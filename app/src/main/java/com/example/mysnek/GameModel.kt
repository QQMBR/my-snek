package com.example.mysnek

import android.util.Log
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Predicate
import io.reactivex.observables.ConnectableObservable
import io.reactivex.rxkotlin.withLatestFrom
import io.reactivex.subjects.BehaviorSubject
import java.util.concurrent.TimeUnit
import kotlin.math.exp
import kotlin.random.Random

class GameModel(upstream: Observable<GameControl>) {

    private fun slownessFromLength(length: Int): Long {
        return (exp(length.toDouble()*-SnekSettings.SNAKE_ACCELERATION)*SnekSettings.SNAKE_SLOWNESS_IN_MS).toLong()
    }

    private val processGameData: BiFunction<SnekData, GameControl, SnekData>
            = BiFunction { snake, newDir ->
        when (newDir) {
            is Direction -> {
                //modify the snake's body
                snake.run {
                    when(snake) {
                        is Moveable -> {
                            //move the snake
                            body.apply {
                                //snake grows by not removing the tail end of the body
                                if (snake is Move) {
                                    //remove the last element of the body
                                    removeAt(lastIndex)
                                }
                                add(
                                    0,
                                    moveHead(first(), newDir)
                                )
                            }
                        }
                    }

                    //check for collisions and apple eating
                    processMove(this)
                }
            }
            else -> {
                when (newDir) {
                    Flow.PAUSE -> {
                        //no changes are made to the snake
                        snake
                    }
                    //Flow.REQUEST_APPLE
                    //turn any Move into an Apple containing the same information
                    //the Apples state should be displayed though
                    else -> {
                        //
                        when (snake) {
                            is Move -> Apple(snake.body, snake.apple)
                            else    -> snake
                        }
                    }
                }
            }
        }

    }

    private val score : BehaviorSubject<Int> = BehaviorSubject.createDefault(0)

    //emit the snake's new travelling direction upon each user input
    private val inputIntervalParams : Observable<Triple<GameControl, Long, Long>> = upstream
        //.ofType<Direction>()
        .onlyAllowedControls()
        .withLatestFrom(score) { control: GameControl, latestScore: Int ->
            Triple(control, 0L, slownessFromLength(latestScore))
        }

    //an observable that emits when the snake has a new score and needs to have
    //it's speed recalculated so that it may move faster even when travelling
    //in a straight line
    private val gameIntervalParams : Observable<Triple<GameControl, Long, Long>> = score
        .distinctUntilChanged()
        .withLatestFrom(upstream) { score, latestDirection ->
            Log.d(TAG, "Latest direction is: $latestDirection")
            slownessFromLength(score).let { Triple(latestDirection, it, it) }
        }

    private fun intervalFromDir(dir: Direction, initialDelay: Long, interval: Long) =
        Observable.interval(initialDelay, interval, TimeUnit.MILLISECONDS)
            .map { dir }
            .doOnNext { Log.d(TAG, "Slowness = $interval, direction = $dir") }

    //start the interval that moves the snake along
    //at a certain speed without user input
    private val slowness : Observable<GameControl> = Observable
        .merge(gameIntervalParams, inputIntervalParams)
        .switchMap { (dir, initialDelay, interval) ->
            when (dir) {
                is Direction -> intervalFromDir(dir, initialDelay, interval)
                else -> Observable.just(dir)
            }
        }

    private fun Observable<GameControl>.onlyAllowedControls() : Observable<GameControl> =
        this.distinctUntilChanged { last, current ->
            if (last is Direction && current is Direction) {
                last == current || current == last.flip()
            }
            //otherwise let everything through - a flow is always distinct from a direction
            //and all flows should be let through
            else false
        }
    private fun Observable<Direction>.onlyAllowedDirections() : Observable<Direction> =
        this.distinctUntilChanged { last, current ->
            //always let NONE through
            if (current == Direction.NONE) {
                false
            }
            else {
                last == current || current == last.flip()
            }
        }

    val snakeData : ConnectableObservable<SnekData> = slowness
        .filter { x -> x != Direction.NONE}
        .scan(getFirstApple(), processGameData)
        .doOnNext { otherSnake ->
            Log.d(TAG, "New snake data = $otherSnake")
        }
        .publish()

    init {
        snakeData
            .map { snek -> snek.body.size - SnekSettings.START_SIZE - 1}
            .distinctUntilChanged()
            .doOnNext { x -> Log.d(TAG, "New score = $x")}
            .subscribe(score)
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
    enum class Direction : GameControl { UP, DOWN, LEFT, RIGHT, NONE }
    enum class Flow : GameControl { PAUSE, SHOW_APPLE }

    interface GameControl

    companion object {
        const val TAG = "GameModel"

        //snake starts vertically in length with a random apple
        //position (but not in the snake) and with 0 points
        fun getFirstApple() : Move
                = Move(
                    body = ArrayList((0 until SnekSettings.START_SIZE).map { Coords(0, it) }),
                    apple = Coords(
                    Random.nextInt(1, SnekSettings.GRID_WIDTH),
                    Random.nextInt(SnekSettings.START_SIZE, SnekSettings.GRID_HEIGHT)
                    )
                )
    }
}