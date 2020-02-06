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
fun GameModel.Direction.vectorize(): Pair<Int, Int>
    = when (this) {
        GameModel.Direction.UP    -> Pair( 0, -1)
        GameModel.Direction.LEFT  -> Pair(-1,  0)
        GameModel.Direction.DOWN  -> Pair( 0,  1)
        GameModel.Direction.RIGHT -> Pair( 1,  0)
    }

fun <T> Predicate<T>.not(): Predicate<T> {
    return Predicate {
        this.test(it).not()
    }
}

typealias Coords = Pair<Int, Int>