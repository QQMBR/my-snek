package com.example.mysnek

import android.util.Log
import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.ObservableTransformer
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Predicate
import java.util.concurrent.TimeUnit
import kotlin.random.Random

object GameModel: ObservableTransformer<GameModel.Direction, GameData> {

    //take a stream of directions and transform it to a stream of the snake's body positions
    //first filter out invalid directions, then start a timer to move in the last direction
    //after a certain time if there is no other user input, then calculate the snake's new
    //position and check for a collision in the body by checking for duplicates in the list
    //of the body coordinates
    override fun apply(upstream: Observable<Direction>): ObservableSource<GameData> {
        return upstream
            .distinctUntilChanged { last, current ->
                last == current || last == current.flip()
            }
            .switchMap { dir ->
                Observable
                    .interval(0, SnekSettings.SNAKE_SLOWNESS_IN_MS, TimeUnit.MILLISECONDS)
                    .map { dir }
            }
            .scan(GameData(
                body   = ArrayList((0 until SnekSettings.START_SIZE).map { Coords(0, it) }),
                apple  = Coords(Random.nextInt(1, SnekSettings.GRID_WIDTH), Random.nextInt(SnekSettings.START_SIZE, SnekSettings.GRID_HEIGHT)),
                points = 0
            ), processGame)
            .takeWhile(isGameOver.not())
            .doOnNext { p -> Log.d(TAG, "New value in snake $p") }
    }

    //move the tile by destructuring the coordinates of the tile
    //and the vectorized direction and adding the components
    //of the vector to the coordinates
    private val moveHead
            = { (x, y): Coords, dir: Direction ->
        { (dx, dy): Coords ->
            Coords(x + dx, y + dy)
        } (dir.vectorize())
    }

    private fun speedUp(p: Pair<Long, Direction>, dir: Direction): Pair<Long, Direction> {
        return Pair(p.first - 10, dir)
    }
    private val processGame: BiFunction<GameData, Direction, GameData>
            = BiFunction { snake, direction ->
        //modify the snake's body
        snake.apply {
            body.apply {
                //snake grows by not removing the tail end of the body
                if (!grow) {
                    //remove the last element of the body
                    removeAt(lastIndex)
                }
                else {
                    grow = false
                }

                //add a new head, which is the old head "translated by the movement direction"
                add(
                    0,
                    moveHead(first(), direction)
                )
            }

            //reassign the apple if a new one is required
            apple = apple.let {
                //check whether the head's coordinates are same as the apple's
                //if yes, the apple is eaten
                if (it == body.first()) {
                    //increment the points counter
                    points++

                    var newApple: Coords

                    //generate random coordinates in the grid that aren't in the snake's body
                    do {
                        newApple = Coords(Random.nextInt(SnekSettings.GRID_WIDTH),
                                          Random.nextInt(SnekSettings.GRID_HEIGHT))
                    } while (newApple in body)

                    //set the grow flag so that the snake get's one tile longer in the
                    //next step
                    grow = true

                    Log.d(TAG, "New Apple was generated at $newApple")
                    newApple
                }
                else
                    it
            }
        }
    }

    private val isGameOver: Predicate<GameData> = Predicate {
        //check for a collision with the imaginary borders
        val isBorderCollision = {(x, y): Coords ->
            x < 0 || x >= SnekSettings.GRID_WIDTH || y < 0 || y >= SnekSettings.GRID_HEIGHT }

        //check for duplicates in the body of the snake by converting it into a set that
        //contains no duplicates and then comparing the size, the game is over if the
        //there are duplicates in the body
        (it.body.size != it.body.toSet().size) || isBorderCollision(it.body[0])
    }

    //simple enum for all the directions the snake can move in
    enum class Direction { UP, DOWN, LEFT, RIGHT }

    const val TAG = "GameModel"

    //snake starts vertically in length with a random apple
    //position (but not in the snake) and with 0 points
    val startData = GameData(
        body   = ArrayList((0 until SnekSettings.START_SIZE).map { Coords(0, it) }),
        apple  = Coords(Random.nextInt(1, SnekSettings.GRID_WIDTH), Random.nextInt(SnekSettings.START_SIZE, SnekSettings.GRID_HEIGHT)),
        points = 0
    )
}