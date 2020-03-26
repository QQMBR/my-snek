package com.example.mysnek

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import java.nio.IntBuffer
import java.util.EnumMap
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.properties.Delegates

class GameRenderer(private val colors: EnumMap<SnekColors, Int>, settings: SnekSettings) : GLSurfaceView.Renderer {
    private val grid = GameGrid(settings.gridWidth, settings.gridHeight)

    private var created = false

    private var overlay: Square? = null

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

    //the tiles for the apple and entire snake, constrained into a tile of the game grid
    //lazy so that the initializing code is always called when there is already an
    //OpenGL instance (assuming this was the issue causing nothing to be
    //                 rendered after screen rotation)
    //TODO does lazy fix all issues with screen turns?
    private lateinit var apple: Lazy<Tile>
    private val tiles: ArrayList<Lazy<Tile>> = arrayListOf()

    //necessary matrices used in the OpenGL pipeline
    private val mVPMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)

    private val queuedEvents = ArrayList<() -> Unit>()
    //try to retrieve a color from the colors map and convert it to a RGBA FloatArray
    private fun getColor(color: SnekColors) = colors[color]?.colorToRGBAFloatArray()
        ?: DEFAULT_COLOR.also {
            Log.e(TAG, "$color wasn't found in the colors map")
        }

    private var cameraX = 0f
    private var cameraY = 0f
    private var deltaX = 0f
    private var deltaY = 0f

    private val screenHeight = if (settings.gridWidth > settings.gridHeight) settings.gridHeight / settings.gridWidth.toFloat()
        else 1f

    private val screenWidth = if (settings.gridWidth > settings.gridHeight) 1f
        else settings.gridWidth / settings.gridHeight.toFloat()

    private val backGroundScale = scaleFloatArray(sx = screenWidth, sy = screenHeight)

    private fun queueIfNotCreated(f: () -> Unit) {
        //if onSurfaceCreated hasn't yet been called,
        //we queue the events to be ran upon creation
        //otherwise, we may run the function now
        if (!created) {
            Log.d(TAG, "Queuing some event at ${queuedEvents.size}")
            queuedEvents.add(f)
        }
        else {
            f()
        }
    }

    fun handleTap(x: Float, y: Float): Boolean {
        return screenToWorld(mVPMatrix, screenPixelWidth, screenPixelHeight, x, y, 0f).let {
            pauseButton.hitbox.checkCollision(it[0], it[1])
        }
    }

    //TODO are these "safe" functions really safe? -> More testing
    //these functions call their respective functions if onSurface
    //has already been called, otherwise, they are added to
    //a list of queued functions that are executed once the surface
    //has been created
    fun renderTilesSafe(coords: ArrayList<Coords>) {

        Log.d(TAG, "Queuing if not created")
        queueIfNotCreated {
            Log.d(TAG, "Rendering ${coords.map {"$it"}}")
            renderTiles(coords)
        }
    }

    fun renderAppleSafe(coords: Coords) {
        queueIfNotCreated {
            renderApple(coords)
        }
    }

    fun renderAllSafe(coords: ArrayList<Coords>, apple: Coords) {
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
        overlay = null
    }

    private fun pause() {
        Log.d(TAG, "Setting an overlay")
        //TODO why need to invert color
        overlay = Square(floatArrayOf(0f, 0f, 0f, 0.4f))
    }

    //copy a list of coordinates over into the x and y
    //coordinates of all tiles in the list
    //TODO add efficiency improvement for when only the first and last tiles have been moved
    //TODO handle case where there are more tiles than coordinates
    private fun renderTiles(coords: ArrayList<Coords>) {
        if (tiles.size <= coords.size) {
            with(tiles.iterator().withIndex()) {
                forEach { (index, tile) ->

                    //change the color of the head only
                    if (index == 0) {
                        tile.value.changeColor(getColor(SnekColors.HEAD))
                    }

                    tile.value.coords = coords[index]
                }
            }

            with(coords.takeLast(coords.size - tiles.size).iterator().withIndex()) {
                forEach { (index, pair) ->
                    //Log.d(TAG, "Adding new tiles")

                    //lazily create a new tile with the body color (or default if color isn't found)
                    //so that the initializing code is called later
                    @Suppress("ComplexRedundantLet")
                    tiles.add(
                        (if (tiles.size == 0 && index == 0) SnekColors.HEAD else SnekColors.BODY).let {
                            lazy {
                                Tile(grid, pair, getColor(it))
                            }
                        }
                    )
                }
            }
        }
        else {
            Log.d(TAG, "Tiles wasn't smaller than coords ${tiles.size} vs ${coords.size}")
        }
    }

    private fun renderApple(coords: Coords) {
        if (!::apple.isInitialized) {
            apple = lazy {Tile(grid, coords, getColor(SnekColors.APPLE))}
        }
        else {
            apple.value.coords = coords
        }
    }

    fun panCamera(direction: GameModel.Direction) {
        queueIfNotCreated {
            val panSpeed = 0.006f
            when (direction) {
                GameModel.Direction.LEFT -> {
                    deltaX = -panSpeed
                    deltaY = 0f
                }
                GameModel.Direction.RIGHT -> {
                    deltaX = panSpeed
                    deltaY = 0f
                }
                GameModel.Direction.UP -> {
                    deltaX = 0f
                    deltaY = panSpeed
                }
                GameModel.Direction.DOWN -> {
                    deltaX = 0f
                    deltaY = -panSpeed
                }
            }
        }
    }

    override fun onDrawFrame(p0: GL10?) {
        //cameraX += deltaX
        //cameraY += deltaY

        //set the camera position
        Matrix.setLookAtM(viewMatrix, 0, cameraX, cameraY, 4f, 0f, 0f, 0f, 0f, 1f, 0f)

        //calculate the projection and view transformation
        Matrix.multiplyMM(mVPMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        //redraw background with color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)


        gridBackground.draw(mVPMatrix mul backGroundScale)

        //draw all of the tiles from the list
        tiles.forEach { t -> t.value.draw(mVPMatrix) }

        if (::apple.isInitialized) {
            apple.value.draw(mVPMatrix)
        }

        //if there is an overlay, draw it
        overlay?.draw(mVPMatrix mul backGroundScale)

        pauseButton.draw(mVPMatrix)
    }

    fun clearTiles() {
        Log.d(TAG, "Queuing clear tiles")
        queueIfNotCreated {
            Log.d(TAG, "Clearing tiles")
            tiles.clear()
        }
    }

    override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_ONE_MINUS_SRC_ALPHA, GLES20.GL_SRC_ALPHA)

        //TODO unpack background color array into individual parameters for the background
        GLES20.glClearColor(0.3f, 0.3f, 0.3f, 1.0f)

        //initialize the square representing the background of the grid with the associated color
        gridBackground = Square(getColor(SnekColors.GRID_BACKGROUND))

        created = true
        Log.d(TAG, "Surface Created")
    }

    override fun onSurfaceChanged(p0: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)

        val ratio = width.toFloat() / height.toFloat()
        Log.d(TAG, "New ratio = $ratio")

        Matrix.orthoM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 7f)
        //Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 7f)

        screenPixelWidth = width
        screenPixelHeight = height

        //run each event that was queued before this surface was created
        queuedEvents.forEach {
            Log.d(TAG, "Executing events")
            it()
        }
    }

    companion object {
        val screenToWorld: (FloatArray, Int, Int, Float, Float, Float) -> FloatArray = {mvpMatrix, width, height, x, y, zCut ->

            val xNDC = 2 * x / width - 1
            val yNDC = -(2 * y / height - 1)

            val nearNDC = floatArrayOf(xNDC, yNDC, -1f, 1f)
            val farNDC = floatArrayOf(xNDC, yNDC, 1f, 1f)

            val inverseVP= FloatArray(16).also {
                Matrix.invertM(it, 0, mvpMatrix, 0)
            }

            val nearVertexWorld = FloatArray(4)
            val farVertexWorld = FloatArray(4)

            Matrix.multiplyMV(nearVertexWorld, 0, inverseVP, 0, nearNDC, 0)
            Matrix.multiplyMV(farVertexWorld, 0, inverseVP, 0, farNDC, 0)

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
            val distance = (zCut - rayStart[2]) / startToEnd[2]

            //calculate the other coordinates at the distance just calculated from the near point
            rayStart.zip(startToEnd) { s, v ->
                s + distance * v
            }.toFloatArray().also { arr ->
                Log.d(TAG, "Intersected $zCut at ${arr.map { "$it" }}")
            }
        }

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
