package com.example.mysnek

import android.opengl.Matrix
import android.util.Log

class Tile(var x: Int, var y: Int): Square() {

    //TODO expose grid dimensions
    private val scalelessOffset = GameGrid(4, 4)

    //change the coordinates of the box based on a direction
    //UNUSED - see GameSurfaceView
    fun move(direction: GameModel.Direction) {
        Log.d(TAG, "Moving Box")
        when (direction) {
            GameModel.Direction.UP    -> y -= 1
            GameModel.Direction.RIGHT -> x += 1
            GameModel.Direction.DOWN  -> y += 1
            GameModel.Direction.LEFT  -> x -= 1
        }
    }

    override fun draw(mvpMatrix: FloatArray) {
        val scratch = FloatArray(16)

        //calculate the necessary scale and translate operations based on a grid
        val transformMatrix = scalelessOffset(x, y)

        //apply the grid transformations before the projection and view transforms
        Matrix.multiplyMM(scratch, 0, mvpMatrix, 0, transformMatrix, 0)

        //draw the square
        super.draw(scratch)
    }

    companion object {
        const val TAG = "Tile"
    }
}