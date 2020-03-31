package com.example.mysnek

import android.content.SharedPreferences

//TODO write into object with variables set to default settings
data class SnekSettings(val startSize: Int,
                        val gridWidth: Int,
                        val gridHeight: Int,
                        val initialSnakeSpeed: Long,
                        val maxSnakeSpeed: Long,
                        val countsToMax: Int,
                        val snakeAccelMode: AccelerationMode) {
    /*
    fun slownessFromLength(length: Int): Long
        = (exp(length.toDouble() * -snakeAcceleration) * 1000 / initialSnakeSpeed + 1000 / maxSnakeSpeed).toLong()
     */


    fun speedFromCounter(counter: Int): Float = smoothStepScaled(counter.toFloat())

    private fun smoothStepScaled(x: Float): Float {

        val upperLimit = countsToMax.toFloat()
        val x1 = x / upperLimit

        return when {
            x < 0 -> {
                initialSnakeSpeed.toFloat()
            }
            x > upperLimit -> {
                maxSnakeSpeed.toFloat()
            }
            else -> {
                (maxSnakeSpeed - initialSnakeSpeed) * x1 * x1 * x1 * (x1 * ( 6 * x1 - 15) + 10) + initialSnakeSpeed
            }
        }
    }

    enum class AccelerationMode{
        NONE, APPLE, EACH;

        override fun toString(): String {
            return when (this) {
                NONE -> "None"
                APPLE -> "Apple"
                EACH -> "Each"
            }
        }

        companion object {
            fun fromString(str: String): AccelerationMode? =
                when (str) {
                    "None" -> NONE
                    "Apple" -> APPLE
                    "Each" -> EACH
                    else -> null
                }

        }

    }
    companion object {
        val default = SnekSettings(
            3,
            16,
            16,
            3,
            15,
            40,
            AccelerationMode.APPLE
        )

        fun fromSharedPreferences(sharedPreferences: SharedPreferences) : SnekSettings
            = sharedPreferences.run {

            SnekSettings(
                getString("start_size", default.startSize.toString())!!.toInt(),
                getString("grid_width", default.gridWidth.toString())!!.toInt(),
                getString("grid_height", default.gridHeight.toString())!!.toInt(),
                getString("snake_start_slowness", default.initialSnakeSpeed.toString())!!.toLong(),
                getString("snake_min_slowness", default.maxSnakeSpeed.toString())!!.toLong(),
                getString("counts_to_max", default.countsToMax.toString())!!.toInt(),
                getString("snake_accel_mode", default.snakeAccelMode.toString())!!.toAccelMode()!!
            )
        }
    }
}
