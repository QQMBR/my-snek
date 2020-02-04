package com.example.mysnek

import android.util.Log
import io.reactivex.Observable
import java.util.concurrent.TimeUnit

class GameModel {

    //stream of the snake's movement
    private var movement : Observable<Direction> = Observable.empty()

    //stream of the snake's head's position
    lateinit var snake : Observable<Coords>

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
                    .interval(0, 1000, TimeUnit.MILLISECONDS)
                    .map { dir }
            }

        snake = movement
            .scan(Pair(0, 0)) { (x, y), dir ->
                //add the vectorized direction to the current coordinates
                //by destructuring the pair in a lambda
                { (dx, dy): Coords ->
                    Pair(x + dx, y + dy)
                }(dir.vectorize())
            }
            .doOnNext { p -> Log.d(TAG, "New value in snake $p") }

        return movement
    }

    //simple enum for all the directions the snake can move in
    enum class Direction { UP, DOWN, LEFT, RIGHT }

    companion object {
        const val TAG = "GameModel"
    }
}