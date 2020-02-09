package com.example.mysnek

import android.util.Log
import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.ObservableTransformer
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Predicate
import java.util.concurrent.TimeUnit
import kotlin.random.Random

object GameModel: ObservableTransformer<GameModel.Direction, SnekData> {

    //take a stream of directions and transform it to a stream of the snake's body positions
    //first filter out invalid directions, then start a timer to move in the last direction
    //after a certain time if there is no other user input, then calculate the snake's new
    //position and check for a collision in the body by checking for duplicates in the list
    //of the body coordinates
    override fun apply(upstream: Observable<Direction>): ObservableSource<SnekData> {
        return upstream
            .distinctUntilChanged { last, current ->
                last == current || last == current.flip()
            }
            .switchMap { dir ->
                Observable
                    .interval(0, SnekSettings.SNAKE_SLOWNESS_IN_MS, TimeUnit.MILLISECONDS)
                    .map { dir }
            }
            .scan(Apple(
                body  = ArrayList((0 until SnekSettings.START_SIZE).map {Coords(0, it)}),
                apple = Coords(Random.nextInt(1, SnekSettings.GRID_WIDTH), Random.nextInt(SnekSettings.START_SIZE, SnekSettings.GRID_HEIGHT))
            ), processGameData)
            .takeWhile { it !is Finished}
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

    private val processGameData: BiFunction<SnekData, Direction, SnekData>
            = BiFunction { snake, direction ->
        //modify the snake's body
        snake.run {

            if (snake is Move || snake is Apple) {
                //move the snake
                body.apply {
                    //snake grows by not removing the tail end of the body
                    if (snake is Move) {
                        //remove the last element of the body
                        removeAt(lastIndex)
                    }

                    //add a new head, which is the old head "translated by the movement direction"
                    add(
                        0,
                        moveHead(first(), direction)
                    )
                }
            }

            //check for collisions and apple eating
            processMove(this)
        }
    }

    private val processMove : (SnekData) -> SnekData = { snake ->
        if (isGameOver.test(snake) && snake !is Over) {
            Over(snake.body)
        }
        else {
            when (snake) {
                is Move     -> checkApple(snake)
                is Apple    -> checkApple(snake)
                is Over     -> Finished
                is Finished -> Finished
            }
        }
    }

    private val checkApple: (Moveable) -> SnekData = { snake ->
        snake.run {
            if (apple == body.first()) {

                var newApple: Coords

                //generate random coordinates in the grid that aren't in the snake's body
                do {
                    newApple = Coords(
                        Random.nextInt(SnekSettings.GRID_WIDTH),
                        Random.nextInt(SnekSettings.GRID_HEIGHT)
                    )
                } while (newApple in body)

                Log.d(TAG, "New Apple was generated at $newApple")

                Apple(body, newApple)
            } else {
                Move(body, apple)
            }
        }
    }

    private val isGameOver: Predicate<SnekData> = Predicate {
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