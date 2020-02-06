package com.example.mysnek

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class GameRenderer : GLSurfaceView.Renderer {

    //a square tile that can be drawn
    private lateinit var square: Square

    private val grid = GameGrid(10, 10)

    private val tiles: ArrayList<Lazy<Tile>> = arrayListOf()
    //private val lazyTiles: Sequence<Tile> = sequenceOf()

    //necessary matrices used in the OpenGL pipeline
    private val mVPMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)

    /*
    //move the tile based on a direction; UNUSED - see GameSurfaceView
    Unit fun moveBox(dir: GameModel.Direction) = if (::square.isInitialized) square.move(dir) else

    //directly change the coordinates of the tile
    fun renderTileAt(p: Coords) {
        if (::square.isInitialized) {
            square.x = p.first
            square.y = p.second
        }
    }

     */

    //copy a list of coordinates over into the x and y
    //coordinates of all tiles in the list
    //TODO add efficiency improvement for when only the first and last tiles have been moved
    //TODO handle case where there are more tiles than coordinates
    fun renderTiles(coords: ArrayList<Coords>) {

        //Log.d(TAG, "Updating tiles $coords")
        if (tiles.count() <= coords.size) {

            var index = 0

            with(tiles.iterator()) {
                forEach {
                    it.value.x = coords[index].first
                    it.value.y = coords[index].second

                    index++
                }
            }

            with(coords.takeLast(coords.size - tiles.size).iterator()) {
                forEach { (x, y) ->
                    //Log.d(TAG, "Adding new tiles")
                    tiles.add(lazy {Tile(grid, x, y)})
                }
            }
        }
    }

    override fun onDrawFrame(p0: GL10?) {
        //calculate the projection and view transformation
        Matrix.multiplyMM(mVPMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        //redraw background with color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        val identity = FloatArray(16).also {Matrix.setIdentityM(it, 0)}
        //draw all of the tiles from the list
        tiles.forEach { t -> t.value.draw(mVPMatrix) }
        //square.draw(mVPMatrix)
    }

    override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
        //set the camera position
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, -3f, 0f, 0f, 0f, 0f, 1f, 0f)

        GLES20.glClearColor(0.3f, 0.3f, 0.3f, 1.0f)
        //square = Tile(3, 3)
        square = Tile(grid, 3, 3)
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
            }
        }

        const val TAG = "GameRenderer"
    }
}