package com.example.mysnek

import android.opengl.Matrix
import android.util.Log

class Tile(private val scalelessOffset: GameGrid, var coords: Coords, color: FloatArray): Square(color) {

    //change the coordinates of the box based on a direction
    //UNUSED - see GameSurfaceView
    fun move(direction: GameModel.Direction) {
        Log.d(TAG, "Moving Box")
        when (direction) {
            GameModel.Direction.UP    -> coords.y -= 1
            GameModel.Direction.RIGHT -> coords.x += 1
            GameModel.Direction.DOWN  -> coords.y += 1
            GameModel.Direction.LEFT  -> coords.x -= 1
        }
    }

    override fun draw(mvpMatrix: FloatArray) {
        val scratch = FloatArray(16)

        //calculate the necessary scale and translate operations based on a grid
        val transformMatrix = scalelessOffset(coords.x, coords.y)

        //apply the grid transformations before the projection and view transforms
        Matrix.multiplyMM(scratch, 0, mvpMatrix, 0, transformMatrix, 0)

        //draw the square
        super.draw(scratch)
    }

    companion object {
        const val TAG = "Tile"
    }
}