package com.example.mysnek

import android.opengl.Matrix

//assumes unit width and height
class GameGrid(private val cols: Int, private val rows: Int): (Int, Int) -> FloatArray {

    //get the transformation needed to fit an object into the given
    //coordinates of the grid
    override fun invoke(x: Int, y: Int) : FloatArray {
        val scaleM = FloatArray(16).also {
            Matrix.setIdentityM(it, 0)
        }

        val translateM = FloatArray(16).also {
            Matrix.setIdentityM(it, 0)
        }

        //scale the object by factors anti-proportional to the amount of columns and rows of the grid
        Matrix.scaleM(scaleM, 0, 1f/cols, 1f/rows, 1.0f)

        //TODO don't assume object is in the middle of grid
        //calculate the translation needed to move the object into the correct location
        val offset = { max: Int, index: Int ->
            (max-1f)/(2f*max) - index.toFloat()/max.toFloat()
        }

        //translate the object
        Matrix.translateM(translateM, 0, offset(cols, x), offset(rows, y), 0f)


        return FloatArray(16).also {
            Matrix.multiplyMM(it, 0, translateM, 0, scaleM, 0)
        }


        //return scaleM
    }

    companion object {
        const val TAG = "GameGrid"
    }
}