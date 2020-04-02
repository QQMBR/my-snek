package com.example.mysnek

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import java.nio.IntBuffer
import java.util.Collections
import java.util.EnumMap
import java.util.concurrent.Callable
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.properties.Delegates

class GameRenderer(private val colors: EnumMap<SnekColors, Int>, settings: SnekSettings) : GLSurfaceView.Renderer {
    private val grid = GameGrid(settings.gridWidth, settings.gridHeight)

    private var created = false

    private var activeOverlay: Square? = null
    private lateinit var overlay: Square

    private var screenPixelWidth by Delegates.notNull<Int>()
    private var screenPixelHeight by Delegates.notNull<Int>()

    //a square representing the background of the playing field
    private lateinit var gridBackground: Square

    private val pauseButton by lazy {
        CompositeSquare(
            Pair(Square(getColor(SnekColors.APPLE)),
                FloatArray(16).also {
                    Matrix.multiplyMM(it, 0,
                        translateFloatArray(0.35f, 0f, 0f), 0,
                        scaleFloatArray(sx = 0.3f), 0
                    )
                }
            ),
            Pair(Square(getColor(SnekColors.APPLE)),
                FloatArray(16).also {
                    Matrix.multiplyMM(
                        it, 0,
                        translateFloatArray(-0.3f, 0f, 0f), 0,
                        scaleFloatArray(sx = 0.3f), 0
                    )
                }
            ), each = FloatArray(16).also {
                Matrix.multiplyMM(
                    it, 0,
                    translateFloatArray(screenWidth / 2f - 0.05f, screenHeight / 2f + 0.1f, 0f), 0,
                    scaleFloatArray(0.1f, 0.1f, 0.1f), 0)
            })
    }

    private lateinit var apple: Tile
    private var tiles: ArrayList<Tile> = arrayListOf()

    //necessary matrices used in the OpenGL pipeline
    private val mVPMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)

    /**
     * the inverse of [projectionMatrix] * [viewMatrix]
     * used to take pixel coordinates into world coordinates for checking
     * hitbox collisions for e.g. the pause button
     */
    private var inverseVPMatrix = FloatArray(16)

    private val queuedEvents = Collections.synchronizedList(ArrayList<() -> Unit>())

    //try to retrieve a color from the colors map and convert it to a RGBA FloatArray
    private fun getColor(color: SnekColors) = colors[color]?.colorToRGBAFloatArray()
        ?: DEFAULT_COLOR.also {
            Log.e(TAG, "$color wasn't found in the colors map")
        }

    private val screenHeight = if (settings.gridWidth > settings.gridHeight) settings.gridHeight / settings.gridWidth.toFloat()
        else 1f

    private val screenWidth = if (settings.gridWidth > settings.gridHeight) 1f
        else settings.gridWidth / settings.gridHeight.toFloat()

    private val backgroundScale = scaleFloatArray(sx = screenWidth, sy = screenHeight)

    private fun queueIfNotCreated(f: () -> Unit) {
        //if onSurfaceCreated hasn't yet been called,
        //we queue the events to be ran upon creation
        //otherwise, we may run the function now
        if (!created) {
            Log.d(TAG, "Queuing some event at ${queuedEvents.size}, hashCode: ${f.hashCode()}, thread: ${Thread.currentThread()}")
            queuedEvents.add(f)
        }
        else {
            f()
        }
    }

    fun queueUntilCreated() {
        created = false
    }

    /**
     * @param x x coordinate of the tap in pixels from left
     * @param y y coordinate of the tap in pixels from top
     *
     * starts a [Callable] to calculate the whether the tap
     * is in the pause button's hitbox, if yes, then
     * the pause overlay is set
     *
     * NOTE: this means that the overlay may be set twice,
     *       once by this method and once via the owning
     *       fragment that handles all pauses, including
     *       ones that are due to lifecycle changes
     *
     * @return true if the pause button was hit,
     * false if not or an error was thrown by the [Callable]
     *
     * can be called without queueEvent as it uses a [Callable]
     */
    fun handleTap(x: Float, y: Float): Boolean {
        Log.d(TAG, "Calculating tap coordinates in the world coordinate space")
        return try {
            Callable {
                screenToWorld(x, y).let {
                    Log.d(TAG, "Checking for collision using calculated click value")
                    pauseButton.hitbox.checkCollision(it[0], it[1])
                }
            }.call().also {
                if (it) pause()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Callable on handleTap threw an exception ${e.message} ${e.stackTrace}")
            false
        }
    }

    fun renderAllSafe(coords: ArrayList<Coords>, apple: Coords) {
        Log.d(TAG, "Attempting to queue all")
        queueIfNotCreated {
            renderTiles(coords)
            renderApple(apple)
        }
    }

    fun pauseSafe() {
        queueIfNotCreated {
            pause()
        }
    }

    fun resumeSafe() {
        queueIfNotCreated {
           resume()
        }
    }

    private fun resume() {
        activeOverlay = null
    }

    private fun pause() {
        Log.d(TAG, "Setting an overlay")

        activeOverlay = overlay
    }

    /**
     * @param coords the coordinates of the snake's body tiles
     *
     * updates the internal tiles to match [coords]
     * and set the colors of the respective tiles accordingly
     * TODO optimize with the knowledge that mostly only two body tiles change:
     *  " the first (head) and the last one (as long as no apples are eaten)
     */
    private fun renderTiles(coords: ArrayList<Coords>) {
        Log.d(TAG, "Calling renderTiles")

        synchronized(tiles) {
            //remove elements from tiles until the size matches that of coords'
            with(tiles.iterator()) {
                while (tiles.size > coords.size) {
                    next()
                    remove()
                }
            }

            with(tiles.iterator().withIndex()) {
                forEach { (i, value) ->
                    if (i == 0) {
                        value.changeColor(getColor(SnekColors.HEAD))
                    }
                    if (i < coords.size) {
                        value.coords = coords[i]
                    }
                }
            }

            if (tiles.size < coords.size) {

                val colorHead = tiles.size == 0

                tiles.addAll((coords.slice(tiles.size until coords.size)).map { coords ->
                    Tile(grid, coords, getColor(SnekColors.BODY))
                })

                if (colorHead) {
                    with(tiles.iterator()) {
                        if (hasNext()) {
                            next().changeColor(getColor(SnekColors.HEAD))
                        }
                    }
                }
            }
        }
    }

    private fun renderApple(coords: Coords) {
        // TODO don't check for initialized every time, do this only once at the start of the game
        if (!::apple.isInitialized) {
            apple = Tile(grid, coords, getColor(SnekColors.APPLE))
        }
        else {
            apple.coords = coords
        }
    }

    override fun onDrawFrame(p0: GL10?) {

        //redraw background with color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        gridBackground.draw(mVPMatrix mul backgroundScale)

        //draw all of the tiles from the list
        tiles.forEach { t -> t.draw(mVPMatrix) }

        if (::apple.isInitialized) {
            apple.draw(mVPMatrix)
        }

        //if there is an overlay, draw it
        activeOverlay?.draw(mVPMatrix mul backgroundScale)

        pauseButton.draw(mVPMatrix)
    }

    override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_ONE_MINUS_SRC_ALPHA, GLES20.GL_SRC_ALPHA)

        val backgroundColor = getColor(SnekColors.BACKGROUND)

        GLES20.glClearColor(backgroundColor[0], backgroundColor[1], backgroundColor[2], backgroundColor[3])

        //initialize the square representing the background of the grid with the associated color
        gridBackground = Square(getColor(SnekColors.GRID_BACKGROUND))

        //TODO why need to invert color
        overlay = Square(floatArrayOf(0f, 0f, 0f, 0.8f))

        created = true
        Log.d(TAG, "Surface Created")
    }

    override fun onSurfaceChanged(p0: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)

        val ratio = width.toFloat() / height.toFloat()
        Log.d(TAG, "New ratio = $ratio")

        //TODO okay for all aspect ratios?
        //Matrix.perspectiveM(projectionMatrix, 0, 90f, ratio, 1f, 4f)

        // otherwise try these, w/ fine tuning on the factor
        //val factor = 1f / (ratio + 0.25f)
        //Matrix.orthoM(projectionMatrix, 0, -ratio*factor, ratio*factor, -1f*factor, factor, 1f, 4f)

        val factor = if (ratio > 1) ratio else 1f / (ratio)

        if (ratio < 1) {
            Matrix.orthoM(
                projectionMatrix,
                0,
                -0.65f,
                0.65f,
                -factor * 0.65f,
                factor * 0.65f,
                1f,
                4f
            )
        }
        else {
            Matrix.orthoM(
                projectionMatrix,
                0,
                -0.8f * factor,
                0.8f * factor,
                -0.8f,
                0.8f,
                1f,
                4f
            )

        }

        // set the camera position - same for all aspect ratios
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f)

        //calculate the projection and view transformation
        Matrix.multiplyMM(mVPMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        Matrix.invertM(inverseVPMatrix, 0, mVPMatrix, 0)

        screenPixelWidth = width
        screenPixelHeight = height

        //run each event that was queued before this surface was created
        //TODO look up what synchronized list actually does
        queuedEvents.forEach {
            Log.d(TAG, "Executing event ${it.hashCode()}")
            it()
        }
    }

    /**
     * @param x the
     * calculates the
     */
    private fun screenToWorld(x: Float, y: Float): FloatArray {

        val xNDC = 2 * x / screenPixelWidth - 1
        val yNDC = -(2 * y / screenPixelHeight - 1)

        val nearNDC = floatArrayOf(xNDC, yNDC, -1f, 1f)
        val farNDC = floatArrayOf(xNDC, yNDC, 1f, 1f)

        val nearVertexWorld = FloatArray(4)
        val farVertexWorld = FloatArray(4)

        Log.d(TAG, "Before MM")

        Matrix.multiplyMV(nearVertexWorld, 0, inverseVPMatrix, 0, nearNDC, 0)
        Matrix.multiplyMV(farVertexWorld, 0, inverseVPMatrix, 0, farNDC, 0)

        val rayStart = nearVertexWorld.map {
            it / nearVertexWorld[3]
        }.also { arr ->
            Log.d(TAG, "Near point (in world space): ${arr.map { "$it, " }}")
        }

        //do perspective division on the far vertex while calculating the vector
        //from the near to far vector
        val startToEnd = farVertexWorld.zip(rayStart) { e, s ->
            e / farVertexWorld[3] - s
        }

        //calculate the distance from the near vector to cutting the plane perpendicular
        //to the z axis, intersect it at zCut
        val distance = (- rayStart[2]) / startToEnd[2]

        //calculate the other coordinates at the distance just calculated from the near point
        return rayStart.zip(startToEnd) { s, v ->
            s + distance * v
        }.toFloatArray().also { arr ->
            Log.d(TAG, "Intersected z = 0 at ${arr.map { "$it" }}")
        }
    }

    companion object {

        fun loadShader(type: Int, shaderCode: String): Int {

            return GLES20.glCreateShader(type).also { shader ->
                GLES20.glShaderSource(shader, shaderCode)
                GLES20.glCompileShader(shader)

                val buf = IntBuffer.allocate(1)
                GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, buf)

                if (buf[0] == GLES20.GL_FALSE) {

                    GLES20.glGetShaderiv(shader, GLES20.GL_INFO_LOG_LENGTH, buf)

                    val str: String = GLES20.glGetShaderInfoLog(shader)
                    Log.e(TAG, "Error compiling a shader: $str")
                }
            }
        }

        val log = {str: String, after: String ->
            Log.e(TAG, "OpenGL Error after $after: $str")
        }

        fun checkForErrors(after: String = "") {
            when(GLES20.glGetError()) {
                GLES20.GL_INVALID_OPERATION             -> log("Invalid Operation", after)
                GLES20.GL_INVALID_ENUM                  -> log("Invalid Enum", after)
                GLES20.GL_INVALID_FRAMEBUFFER_OPERATION -> log("Invalid Framebuffer Operation", after)
                GLES20.GL_OUT_OF_MEMORY                 -> log("Out of Memory", after)
                GLES20.GL_INVALID_VALUE                 -> log("Invalid Value", after)
            }
        }

        val DEFAULT_COLOR = floatArrayOf(0.8f, 0.6f, 0.9f, 1.0f)

        const val TAG = "GameRenderer"
    }
}
