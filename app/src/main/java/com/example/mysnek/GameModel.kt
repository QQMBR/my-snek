package com.example.mysnek

import android.util.Log
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import java.util.concurrent.TimeUnit

class GameModel {

    //stream of the snake's movement
    private var movement: Observable<Direction> = Observable.empty()

    //stream of the snake's head's position
    lateinit var snake: Observable<ArrayList<Coords>>

    //TODO do this in constructor
    //pass an observable stream of input directions and initialize streams
    fun setInputStream(observable: Observable<Direction>): Observable<Direction> {
        movement = observable
            .startWith(Direction.UP)
            .distinctUntilChanged { last, current ->
                last == current || last == current.flip()
            }
            .switchMap { dir ->
                Observable
                    .interval(0, 600, TimeUnit.MILLISECONDS)
                    .map { dir }
            }

        snake = movement
            .scan(ArrayList((0..7).map { Pair(0, it) }), moveSnake)
            .doOnNext { p -> Log.d(TAG, "New value in snake $p") }

        return movement
    }

    //move the tile by destructuring the coordinates of the tile
    //and the vectorized direction and adding the components
    //of the vector to the coordinates
    private val moveHead = { (x, y): Coords, dir: Direction ->
        { (dx, dy): Coords ->
            Pair(x + dx, y + dy)
        }(dir.vectorize())
    }

    private val moveSnake: BiFunction<ArrayList<Coords>, Direction, ArrayList<Coords>> = BiFunction { body, direction ->
        body.apply {
            //remove the last element of the body
            removeAt(lastIndex)

            //add a new head, which is the old head "translated by the direction"
            add(
                0,
                moveHead(body.first(), direction)
            )
        }
    }

    //simple enum for all the directions the snake can move in
    enum class Direction { UP, DOWN, LEFT, RIGHT }

    companion object {
        const val TAG = "GameModel"
    }
}