package com.example.mysnek

import android.opengl.Matrix
import android.util.Log
import kotlin.math.max
import kotlin.math.min

class CompositeSquare(private vararg val squares: Pair<Square, FloatArray>, private val each: FloatArray) {
    lateinit var hitbox: Hitbox

    init {
        calculateHitbox()
    }

    fun draw(mvpMatrix: FloatArray) {
        for ((s, t) in squares) {
            s.draw(calculateMatrix(t, mvpMatrix))
        }
    }

    private fun calculateMatrix(individual: FloatArray, vpMatrix: FloatArray): FloatArray {
        val scratch = FloatArray(16)

        Matrix.multiplyMM(scratch, 0, each, 0, individual, 0)

        val scratch2 = FloatArray(16)
        Matrix.multiplyMM(scratch2, 0, vpMatrix, 0, scratch, 0)

        return scratch2
    }

    private fun calculateHitbox() {
        val hitboxes = arrayListOf<Hitbox>()

        for ((s, individual) in squares) {
            val mMatrix = FloatArray(16)
            Matrix.multiplyMM(mMatrix, 0, each, 0, individual, 0)

            mMatrix.apply {
                Log.d("CompositeSquare", "Model matrix: ${toList()}")
            }

            hitboxes.add(s.calculateHitbox(mMatrix).also {
                Log.d("CompositeSquare", "added hitbox: ${it.min}, ${it.max}")
            })
        }

        //TODO single pass, more efficient (do you need to call min and max EVERY time?)
        val min = hitboxes.map { it.min }.fold(hitboxes.first().min, {acc, pair ->
            Pair(min(acc.first, pair.first), min(acc.second, pair.second))
        })

        val max = hitboxes.map { it.max }.fold(hitboxes.first().max, {acc, pair ->
            Pair(max(acc.first, pair.first), max(acc.second, pair.second))
        })

        Log.d("CompositeSquare", "min: $min, max: $max")
        hitbox = scaleHitbox(min, max)
    }

    private fun scaleHitbox(min: Pair<Float, Float>, max: Pair<Float, Float>): Hitbox {
        return Hitbox(
            Pair(min.first * (-ConstSnekSettings.HITBOX_SCALE_FACTOR + 2),
                min.second * (-ConstSnekSettings.HITBOX_SCALE_FACTOR + 2)),
            Pair(max.first * ConstSnekSettings.HITBOX_SCALE_FACTOR, max.second * ConstSnekSettings.HITBOX_SCALE_FACTOR)
        )
    }
}