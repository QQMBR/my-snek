package com.example.mysnek

import io.reactivex.functions.Predicate

//get the opposite direction
fun GameModel.Direction.flip()
    = when (this) {
        GameModel.Direction.UP    -> GameModel.Direction.DOWN
        GameModel.Direction.LEFT  -> GameModel.Direction.RIGHT
        GameModel.Direction.DOWN  -> GameModel.Direction.UP
        GameModel.Direction.RIGHT -> GameModel.Direction.LEFT
    }

//turn the direction into a unit vector representing
//the direction of the movement in screen coordinate space
fun GameModel.Direction.vectorize(): Coords
    = when (this) {
        GameModel.Direction.UP    -> Coords( 0, -1)
        GameModel.Direction.LEFT  -> Coords(-1,  0)
        GameModel.Direction.DOWN  -> Coords( 0,  1)
        GameModel.Direction.RIGHT -> Coords( 1,  0)
    }

fun <T> Predicate<T>.not(): Predicate<T> {
    return Predicate {
        this.test(it).not()
    }
}

fun Int.colorToRGBAFloatArray(): FloatArray {
    val maskToRange = { x: Int, n : Int -> ((this and x) shr n) / 255f }

    return floatArrayOf(
        maskToRange(0x00FF0000, 16), //R
        maskToRange(0x0000FF00, 8), //G
        maskToRange(0x000000FF, 0), //B
        maskToRange(0xFF000000.toInt(), 24)  //A
    )
}

fun <T, R> pair(t: T, r: R) = Pair(t, r)