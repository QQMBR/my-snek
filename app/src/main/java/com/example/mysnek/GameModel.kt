package com.example.mysnek

import android.util.Log
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Predicate
import io.reactivex.observables.ConnectableObservable
import io.reactivex.rxkotlin.Observables
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class GameModel(gameControl: Observable<GameControl>, private val settings: SnekSettings) {

    private fun speedUp(acc: Int, snake: SnekData) : Int
        = (when (snake) {
        is Over -> 0
        is Apple -> acc / 2
        else -> acc + 1
    }).run { this }

    private val directionsQueue : MutableList<Direction> = mutableListOf()

    private val processGameData: BiFunction<SnekData, GameControl, SnekData>
        = BiFunction { snake, newDir ->
        when (newDir) {
            is Direction -> {
                //modify the snake's body
                snake.run {
                    when(snake) {
                        is Movable -> {
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
            is Flow -> {
                when (newDir) {
                    Flow.PAUSE -> {
                        if (snake is Over) {
                            processMove(snake)
                        }
                        else {
                            snake
                        }
                    }
                    else -> {
                        Log.d(TAG, "Starting game and getting new Move")
                        getFirstApple(settings.startSize, settings.gridWidth, settings.gridHeight)
                    }
                }
            }
            //only Flow and Direction implement interface, so this branch will never be reached
            else -> {
                snake
            }
        }
    }

    private val upstream = gameControl
        .filter { it == Flow.PAUSE || it == Flow.START_GAME}
        .mergeWith(
            gameControl
                //.filter {it != Flow.PAUSE}
                .ofType(Direction::class.java)
                //TODO setting for starting direction
                // " maybe randomize along with starting position
                .scan(Direction.UP) { acc, cur ->
                    if (acc == cur.flip()) {
                        acc
                    }
                    else {
                        cur
                    }
                }
                .skip(1)
                .distinctUntilChanged()
        )
        .distinctUntilChanged { last, current ->
            last is Direction && current is Direction && last == current
        }
        .doOnNext { Log.d(TAG, "Taken $it") }
        .publish()

    private val heartbeat : PublishSubject<Unit> = PublishSubject.create()

    private val queueSubject = PublishSubject.create<QueueOperation>()
    /**
     * on each heartbeat, takes the next value from [directionsQueue] or, if the
     * queue is empty, [upstream]
     * and applies [processGameData] to calculate the next state of the game
     */
    val data : ConnectableObservable<SnekData> = queueSubject
        .scan(Pair(mutableListOf<GameControl>(), Flow.START_GAME as GameControl?)) { acc, op ->
            op.handle(acc)
        }
        //.doOnNext { Log.d(TAG, "Senseless $it") }
        .filter { (_, res) -> res != null}
        .map { (_, res) -> res!!}
        // filter out any Pauses as they are handled by the heartbeat
        .filter { it != Flow.PAUSE }
        // filter out multiple start games after a heartbeat was established
        .distinctUntilChanged { last, current ->
            last == Flow.START_GAME && last == current
        }
        .scan(Move(arrayListOf(), Coords(0, 0)), processGameData)
        .skip(1)
        .doOnNext { Log.d(TAG, "Data ${it.javaClass.simpleName}") }
        .publish()

    interface QueueOperation {
        fun handle(acc: Pair<MutableList<GameControl>, GameControl?>) : Pair<MutableList<GameControl>, GameControl?>
    }

    class QueueWrite(private val toWrite: GameControl) : QueueOperation {
        override fun handle(acc: Pair<MutableList<GameControl>, GameControl?>): Pair<MutableList<GameControl>, GameControl?> {
            return Pair(acc.first.apply { add(toWrite) }, null)
        }
    }

    class QueueRead : QueueOperation {
        override fun handle(acc: Pair<MutableList<GameControl>, GameControl?>): Pair<MutableList<GameControl>, GameControl?> {
            return if (acc.first.size > 0) {
                val first = acc.first.removeAt(0)

                Pair(acc.first, first)
            } else {
                acc
            }
        }
    }


    init {

        heartbeat
            .map {
                Log.d(TAG, "Reading")
                QueueRead()
            }
            .subscribe(queueSubject)

        upstream
            .map {
                Log.d(TAG, "Writing $it")
                QueueWrite(it)
            }
            .subscribe(queueSubject)

        Observables.combineLatest(
            data,
            upstream.distinctUntilChanged { last, _ ->
                !(last == Flow.PAUSE || last == Flow.START_GAME)
            }
                .doOnNext { Log.d(TAG, "Restart after Pause or StartGame") }
        )
            .map { (snake, _) -> snake }
            .slownessFromSnake(settings)
            .switchMap { slowness ->
                Observable
                    .timer(slowness, TimeUnit.MILLISECONDS)
                    .map { Unit }
            }
            .doOnNext { Log.d(TAG, "Heartbeat") }
            .subscribe(heartbeat)

        upstream.connect()
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
            //val newArr = ArrayList(snake.body.map { it.copy() })
            Over(snake.body)
        }
        else {
            when (snake) {
                is Movable  -> {
                    checkApple(snake)
                }
                else -> snake
                //if the game is over, restart the game by resetting the snake to the first apple
                //is Over -> getFirstApple(settings.startSize, settings.gridWidth, settings.gridHeight)
            }
        }
    }

    private val checkApple: (Movable) -> SnekData = { snake ->
        snake.run {
            if (apple == body.first()) {

                var newApple: Coords

                //generate random coordinates in the grid that aren't in the snake's body
                do {
                    newApple = Coords(
                        Random.nextInt(settings.gridWidth),
                        Random.nextInt(settings.gridHeight)
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
            x < 0 || x >= settings.gridWidth || y < 0 || y >= settings.gridHeight }

        //check for duplicates in the body of the snake by converting it into a set that
        //contains no duplicates and then comparing the size, the game is over if the
        //there are duplicates in the body
        (it.body.size != it.body.toSet().size) || isBorderCollision(it.body[0])
    }

    interface GameControl

    //simple enum for all the directions the snake can move in
    enum class Direction : GameControl { UP, DOWN, LEFT, RIGHT }
    enum class Flow : GameControl { PAUSE, /* SHOW_APPLE, */ START_GAME }

    companion object {
        const val TAG = "GameModel"

        //snake starts vertically in length with a random apple
        //position (but not in the snake) and with 0 points
        fun getFirstApple(startSize: Int, width: Int, height: Int) : Move
            = Move(
            body = ArrayList((0 until startSize ).map { Coords(0, it) }),
            apple = Coords(
                Random.nextInt(1, width),
                Random.nextInt(startSize, height)
            )
        )
    }
}