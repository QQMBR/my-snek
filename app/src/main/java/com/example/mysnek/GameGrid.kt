package com.example.mysnek

import android.opengl.Matrix
import kotlin.math.max

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
        val scale = 1f / max(cols, rows)

        Matrix.scaleM(scaleM, 0, scale, scale, 1.0f)

        //TODO don't assume object is in the middle of grid
        //calculate the translation needed to move the object into the correct location
        val offset = { max: Int, index: Int ->
            //(max-1f)/(2f*max) - index.toFloat()/ max(cols, rows)

            ((1f/2f*max*scale)  - (scale / 2f)) - index.toFloat()/ max(cols, rows)
            //-max / 2f + scale / 2f + index.toFloat() * scale
        }

        //translate the object
        Matrix.translateM(translateM, 0, -offset(cols, x), offset(rows, y), 0f)


        return FloatArray(16).also {
            Matrix.multiplyMM(it, 0, translateM, 0, scaleM, 0)
        }


        //return scaleM
    }

    companion object {
        const val TAG = "GameGrid"
    }
}