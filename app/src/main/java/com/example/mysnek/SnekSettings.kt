package com.example.mysnek

import android.content.SharedPreferences
import android.util.Log
import io.reactivex.Observable
import kotlin.math.max

//TODO write into object with variables set to default settings?
data class SnekSettings(val startSize: Int,
    val gridWidth: Int,
    val gridHeight: Int,
    val initialSnakeSpeed: Long,
    val maxSnakeSpeed: Long,
    val countsToMax: Int,
    val snakeStartingAccel: Float,
    val moveCountDecreaseOnApple: Int,
    val snakeAccelMode: AccelerationMode) {

    fun speedFromCounter(counter: Int): Float = smoothstepScaled(counter.toFloat())

    val slownessFromSnake : (Observable<SnekData>) -> Observable<Long>
        = {upstream -> snakeAccelMode.slowness(upstream, this)}

    private fun smoothstepRaw(x: Float): Float
        = (x * (snakeStartingAccel + x * (3 - 2 * snakeStartingAccel + x * (-2 + snakeStartingAccel)))).also {
        Log.d("SnekSettings", "Raw speed = $it, at $x w acceleration $snakeStartingAccel" )
    }

    private fun smoothstepScaled(x: Float): Float {

        val upperLimit = countsToMax.toFloat()

        return when {
            x < 0 -> 0f
            x > upperLimit -> maxSnakeSpeed.toFloat()
            else -> {
                ((maxSnakeSpeed - initialSnakeSpeed) * smoothstepRaw(x / upperLimit) + initialSnakeSpeed)
                    .also {Log.d("SnekSettings", "Resultant speed $it")}
            }
        }
    }

    fun isValid() = startSize > max(gridWidth, gridHeight)

    enum class AccelerationMode(val slowness: (Observable<SnekData>, SnekSettings) -> Observable<Long>) {
        NONE({ snake, settings ->
            snake.map { (1000f / settings.initialSnakeSpeed).toLong() }
        }),

        APPLE({ upstream, settings ->
            upstream
                .scan(0) { acc, snake ->
                    when (snake) {
                        is Over -> 0
                        is Grow -> acc + 1
                        else -> acc
                    }
                }
                .skip(1)
                .map {
                    (1000f / settings.speedFromCounter(it)).toLong()
                }
        }),

        EACH({ upstream, settings ->
            upstream
                .scan(0) { acc, snake ->
                    when (snake) {
                        is Over -> 0
                        is Grow -> max(0, acc - settings.moveCountDecreaseOnApple)
                        else -> acc + 1
                    }
                }
                .skip(1)
                .map {
                    (1000f / settings.speedFromCounter(it)).toLong()
                }
        });

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
            0f,
            20,
            AccelerationMode.APPLE
        )

        //TODO give the user notice
        fun ifInvalidDefault(settings: SnekSettings): SnekSettings {
            return if (settings.startSize > max(settings.gridWidth, settings.gridHeight)) {
                default
            }
            else {
                settings
            }
        }

        fun fromSharedPreferences(sharedPreferences: SharedPreferences): SnekSettings =
            sharedPreferences.run {

                SnekSettings(
                    getString("start_size", default.startSize.toString())!!.toInt(),
                    getString("grid_width", default.gridWidth.toString())!!.toInt(),
                    getString("grid_height", default.gridHeight.toString())!!.toInt(),
                    getString(
                        "snake_start_slowness",
                        default.initialSnakeSpeed.toString()
                    )!!.toLong(),
                    getString("snake_min_slowness", default.maxSnakeSpeed.toString())!!.toLong(),
                    getString("counts_to_max", default.countsToMax.toString())!!.toInt(),
                    (getInt("starting_accel",
                        default.snakeStartingAccel.toInt()).toFloat() / 10f).also { Log.d("Settings", "Starting acceleration is $it") },
                    getString("accel_mode_each_slowdown", default.moveCountDecreaseOnApple.toString())!!.toInt(),
                    getString("accel_mode", default.snakeAccelMode.toString())!!.toAccelMode()!!
                        .also { Log.d("Settings", "Accel mode $it") }
                )
            }
    }
}
