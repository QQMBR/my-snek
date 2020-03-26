package com.example.mysnek

import android.content.SharedPreferences
import kotlin.math.exp

data class SnekSettings(val startSize: Int,
                        val gridWidth: Int,
                        val gridHeight: Int,
                        private val initialSnakeSlowness: Long,
                        private val minSnakeSlowness: Long,
                        private val snakeAcceleration: Double) {
    fun slownessFromLength(length: Int): Long
        = (exp(length.toDouble() * -snakeAcceleration)*initialSnakeSlowness + minSnakeSlowness).toLong()

    companion object {
        val default = SnekSettings(3, 16, 16, 250, 50, 0.05)

        fun fromSharedPreferences(sharedPreferences: SharedPreferences) : SnekSettings
            = sharedPreferences.run {

            SnekSettings(
                getString("start_size", default.startSize.toString())!!.toInt(),
                getString("grid_width", default.gridWidth.toString())!!.toInt(),
                getString("grid_height", default.gridHeight.toString())!!.toInt(),
                getString("snake_start_slowness", default.initialSnakeSlowness.toString())!!.toLong(),
                getString("snake_min_slowness", default.minSnakeSlowness.toString())!!.toLong(),
                getString("snake_exp_factor", default.snakeAcceleration.toString())!!.toDouble()
            )
        }
    }
}
