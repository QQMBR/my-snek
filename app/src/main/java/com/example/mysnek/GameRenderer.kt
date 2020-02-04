package com.example.mysnek

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class GameRenderer : GLSurfaceView.Renderer {

    //a square tile that can be drawn
    private lateinit var square: Tile

    private val tiles: ArrayList<Tile> = arrayListOf()

    //necessary matrices used in the OpenGL pipeline
    private val mVPMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)

    //move the tile based on a direction; UNUSED - see GameSurfaceView
    fun moveBox(dir: GameModel.Direction) = if (::square.isInitialized) square.move(dir) else Unit

    //directly change the coordinates of the tile
    fun renderTileAt(p: Coords) {
        if (::square.isInitialized) {
            square.x = p.first
            square.y = p.second
        }
    }

    //copy a list of coordinates over into the x and y
    //coordinates of all tiles in the list
    //TODO add efficiency improvement for when only the first and last tiles have been moved
    //TODO handle case where there are more tiles than coordinates
    fun renderTiles(coords: ArrayList<Coords>) {

        if (tiles.size <= coords.size) {

            var index = 0

            with(tiles.iterator()) {
                forEach {
                    it.x = coords[index].first
                    it.y = coords[index].second

                    index++
                }
            }

            with(coords.takeLast(coords.size - tiles.size).iterator()) {
                forEach { (x, y) ->
                    tiles.add(Tile(x, y))
                }
            }
        }
    }

    override fun onDrawFrame(p0: GL10?) {

        //set the camera position
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, -3f, 0f, 0f, 0f, 0f, 1f, 0f)
        Matrix.multiplyMM(mVPMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        //draw all of the tiles from the list
        tiles.forEach { t -> t.draw(mVPMatrix) }
    }

    override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
        GLES20.glClearColor(0.3f, 0.3f, 0.3f, 1.0f)
        square = Tile(3, 3)
    }

    override fun onSurfaceChanged(p0: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)

        val ratio = width.toFloat() / height.toFloat()

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