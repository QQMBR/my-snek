package com.example.mysnek

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import java.nio.IntBuffer
import java.util.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.collections.ArrayList

class GameRenderer(private val colors: EnumMap<SnekColors, Int>) : GLSurfaceView.Renderer {



    private val grid = GameGrid(SnekSettings.GRID_WIDTH, SnekSettings.GRID_HEIGHT)

    private var created = false

    //the objects that will be drawn by this renderer

    //a square representing the background of the playing field
    private lateinit var gridBackground: Square

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

    private fun queueIfNotCreated(f: () -> Unit) {
        //if onSurfaceCreated hasn't yet been called,
        //we queue the events to be ran upon creation
        //otherwise, we may run the function now
        if (!created) {
            Log.d(TAG, "Queuing some event")
            queuedEvents.add(f)
        }
        else {
            f()
        }
    }

    //TODO are these "safe" functions really safe? -> More testing
    //these functions call their respective functions if onSurface
    //has already been called, otherwise, they are added to
    //a list of queued functions that are executed once the surface
    //has been created
    fun renderTilesSafe(coords: ArrayList<Coords>) {
        queueIfNotCreated {
            renderTiles(coords)
        }
    }

    fun renderAppleSafe(coords: Coords) {
        queueIfNotCreated {
            renderApple(coords)
        }
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
    }


    private fun renderApple(coords: Coords) {
        if (!::apple.isInitialized) {
            apple = lazy {Tile(grid, coords, getColor(SnekColors.APPLE))}
        }
        else {
            apple.value.coords = coords
        }
    }

    override fun onDrawFrame(p0: GL10?) {
        //calculate the projection and view transformation
        Matrix.multiplyMM(mVPMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        //redraw background with color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        gridBackground.draw(mVPMatrix)

        //draw all of the tiles from the list
        tiles.forEach { t -> t.value.draw(mVPMatrix) }

        if (::apple.isInitialized) {
            apple.value.draw(mVPMatrix)
        }
    }

    override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
        //set the camera position
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, -3f, 0f, 0f, 0f, 0f, 1f, 0f)

        //TODO unpack background color array into individual parameters for the background
        GLES20.glClearColor(0.3f, 0.3f, 0.3f, 1.0f)

        //initialize the square representing the background of the grid with the associated color
        gridBackground = Square(getColor(SnekColors.GRID_BACKGROUND))

        created = true
        Log.d(TAG, "Surface Created")

        //run each event that was queued before this surface was created
        queuedEvents.forEach {
            it()
        }
    }

    override fun onSurfaceChanged(p0: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)

        val ratio = width.toFloat() / height.toFloat()
        Log.d(TAG, "New ratio = $ratio")

        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 7f)
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