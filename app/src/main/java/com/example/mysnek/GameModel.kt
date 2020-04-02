package com.example.mysnek

import android.util.Log
import io.reactivex.Observable
import io.reactivex.functions.Predicate
import io.reactivex.observables.ConnectableObservable
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class GameModel(gameControl: Observable<GameControl>, private val settings: SnekSettings) {

    /**
     * emits a [Unit] each time the snake should move by reacting to changes in [data]
     */
    private val heartbeat : PublishSubject<Unit> = PublishSubject.create()

    /**
     * used to convey operations that should be executed on a queue of directions that should
     * be consumed on each [heartbeat]
     */
    private val queueSubject = PublishSubject.create<QueueOperation>()

    /**
     * filter the origin game stream to only allow [Flow]s through;
     * the opposite of [directions]
     */
    private val flows = gameControl
        .filter { it == Flow.PAUSE || it == Flow.START_GAME}

    /**
     * take values from [queueSubject], and apply each [QueueOperation] to a queue inside a scan,
     * then filter the result so that only non null second components (which contain the result
     * of the operation) are let through, to these,
     * apply [processGameData] to calculate the next state of the game
     *
     */
    val data : ConnectableObservable<SnekData> = queueSubject
        .scan(Pair(mutableListOf<GameControl>(), null as GameControl?)) { acc, op ->
            op.handle(acc)
        }
        .doOnNext { Log.d(TAG, "Queue and operation result: $it") }
        // filter all non-results and get rid of the rest of the queue
        .filter { (_, res) -> res != null}
        .map { (_, res) -> res!!}

        // filter out any Pauses as they are handled by the heartbeat
        .filter { it != Flow.PAUSE}

        // filter out multiple start games after a heartbeat was established
        .distinctUntilChanged { last, current ->
            last == Flow.START_GAME && last == current
        }
        .scan(Move(arrayListOf(), Coords(0, 0)), ::processGameData)

        //skip the initial value - which is the empty Move
        .skip(1)
        .doOnNext { Log.d(TAG, "Data ${it.javaClass.simpleName}") }
        .publish()

    /**
     * filter to only allow valid directions from the origin game stream through;
     * the opposite of [flows], but here, we apply more filtering to only
     * let directions through that don't collide with the snake, for this reason,
     * we merge with [data], that is filtered to contain the initial facing direction.
     * This data will always be set as the accumulator that is compared against all
     * new directions to determine what is let through
     */
    private val directions = gameControl
        .ofType(Direction::class.java)
        .map { dir -> Pair(dir, false)}
        .mergeWith(
            data
                .ofType(Start::class.java)
                .map { start -> Pair(start.facing, true) }
        )
        .scan(Pair(Direction.UP, false)) { (acc, _), (cur, isStart) ->
            // only ever let the direction through
            if (!isStart) {
                if (acc == cur.flip()) {
                    Pair(acc, false)
                } else {
                    Pair(cur, true)
                }
            }
            else {
                Log.d(TAG, "Set acc to $cur")
                Pair(cur, false)
            }
        }
        .filter { (_, b) -> b}
        .map { (dir, _) -> dir}

    /**
     * combines both [flows] and [directions], additionally filtering out
     * any identical directions that come after each other (without a PAUSE
     * or START_GAME in between)
     */
    private val upstream : ConnectableObservable<GameControl> = Observable
        .merge(flows, directions)
        .distinctUntilChanged { last, current ->
            last is Direction && current is Direction && last == current
        }
        .doOnNext { Log.d(TAG, "Taken $it") }
        .publish()

    /**
     * there are two [QueueOperation]s - [QueueWrite] and [QueueRead],
     * each have a method that transforms a (list of game control, game control) pair
     * the first element is the actual queue of unhandled [GameControl]s and the second
     * is the result of the operation that should be handled in the next cycle
     *
     * henceforth, [QueueWrite] adds an element it contains to the queue and sets the return value
     * to null, while [QueueRead] removes the first element of the queue and sets the return value
     * to this first element
     */
    private interface QueueOperation {
        fun handle(acc: Pair<MutableList<GameControl>, GameControl?>) : Pair<MutableList<GameControl>, GameControl?>
    }

    private class QueueWrite(private val toWrite: GameControl) : QueueOperation {
        override fun handle(acc: Pair<MutableList<GameControl>, GameControl?>): Pair<MutableList<GameControl>, GameControl?> {
            return Pair(acc.first.apply { add(toWrite) }, null)
        }
    }

    private class QueueRead : QueueOperation {
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

        /** heartbeat and upstream send reads and writes, respectively **/
        heartbeat
            .map { QueueRead() }
            .subscribe(queueSubject)

        upstream
            .map { QueueWrite(it) }
            .subscribe(queueSubject)

        /**
         * restart the heartbeat immediately after a PAUSE or a START_GAME
         */
        upstream
            .distinctUntilChanged { last, _ ->
                !(last == Flow.PAUSE || last == Flow.START_GAME)
            }
            .map { Unit.also { Log.d(TAG, "Kickstarted heartbeat without delay")} }
            .subscribe(heartbeat)

        /**
         * "schedule" a new heartbeat after a new data was emitted using the slowness
         * calculated by the new data and the settings
         */
        data
            .filter { it !is Start }
            .slownessFromSnake(settings)
            .concatMap { slowness ->
                Observable
                    .timer(slowness, TimeUnit.MILLISECONDS)
                    .map { Unit }
            }
            .subscribe(heartbeat)

        // TODO does upstream need multicasting?
        // since upstream has multiple users, we multicast it and need to connect to it
        // after everything that uses it has been initialized
        upstream.connect()
    }

    /**
     * the main method used to combine an existing [snake] with a new direction
     * to actually move the snake along, grow it, or even create a new one
     * if [control] is START_GAME
     */
    private fun processGameData(snake: SnekData, control: GameControl) : SnekData
        = when (control) {
        is Direction -> {
            // modify the snake's body
            snake.run {
                when(snake) {
                    is Movable -> {

                        // move the snake
                        body.apply {

                            // snake grows by not removing the tail end of the body
                            if (snake !is Grow) {
                                // remove the last element of the body
                                removeAt(lastIndex)
                            }
                            add(0, moveHead(first(), control))
                        }
                    }
                }

                //check for collisions and apple eating
                processMove(this)
            }
        }
        is Flow -> {
            when (control) {
                Flow.START_GAME -> {
                    Log.d(TAG, "Starting game and getting new Move")

                    // get a new randomly generated starting snake and apple
                    generateNewStart(settings.startSize, settings.gridWidth, settings.gridHeight)
                }

                // PAUSE is filtered out in data, so the following will never be reached
                else -> snake
            }
        }

        // only Flow and Direction implement the GameControl interface, so this branch will also never be reached
        else -> snake
    }

    /**
     * move the tile by destructuring the coordinates of the tile
     * and the vectorized direction and adding the components
     * of the vector to the coordinates
     */
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
                // if the game is over, restart the game by resetting the snake to the first apple
                // is Over -> getFirstApple(settings.startSize, settings.gridWidth, settings.gridHeight)
            }
        }
    }

    private val checkApple: (Movable) -> SnekData = { snake ->
        snake.run {
            if (apple == body.first()) {
                val newApple = generateNewApple(body, settings.gridWidth, settings.gridHeight)

                Log.d(TAG, "New Apple was generated at $newApple")

                Grow(body, newApple)
            } else {
                Move(body, apple)
            }
        }
    }

    private val isGameOver: Predicate<SnekData> = Predicate {
        // check for a collision with the borders
        val isBorderCollision = {(x, y): Coords ->
            x < 0 || x >= settings.gridWidth || y < 0 || y >= settings.gridHeight }

        /**
         * check for duplicates in the body of the snake by converting it into a set that
         * contains no duplicates and then comparing the size, the game is over if the
         * there are duplicates in the body
         */
        (it.body.size != it.body.toSet().size) || isBorderCollision(it.body[0])
    }

    interface GameControl

    //simple enum for all the directions the snake can move in
    enum class Direction : GameControl { UP, DOWN, LEFT, RIGHT }
    enum class Flow : GameControl { PAUSE, START_GAME }

    companion object {
        const val TAG = "GameModel"

        /**
         * generate new starting snake given a [startSize], [width]
         * and [height] that is guaranteed to be valid
         */
        fun generateNewStart(startSize: Int, width: Int, height: Int): Start {
            /**
             * generate a random, but valid facing direction
             *
             * check the starting size to see in which alignments the snake would fit
             */
            val facing = when {
                startSize > width -> Random.nextInt(2).toDirection()
                startSize > height -> Random.nextInt(2, 4).toDirection()
                else -> Random.nextInt(4).toDirection()
            }.also {
                Log.d(TAG, "Snake will be facing $it")
            }

            /**
             * generate a random starting position that is valid given the snake's starting size
             * and facing direction by calculating the valid ranges for the x and y coordinates
             */
            val delta = facing.vectorize()

            val startPos = Coords(
                Random.nextInt(
                    max(0, startSize * delta.x),
                    min(width, width + startSize * delta.x)
                ),
                Random.nextInt(
                    max(0, startSize * delta.y),
                    min(height, height + startSize * delta.y)
                )
            ).also {
                Log.d(TAG, "Generated starting position $it")
            }

            /**
             * build the rest of the snake from the head's coordinates and the facing direction
             */
            val building = facing.flip().vectorize()
            val body = ArrayList((0 until startSize).map { i ->
                Coords(startPos.x + i * building.x, startPos.y + i * building.y)
            })

            /** generate a random, but valid apple that isn't in the snake's body **/
            val apple = generateNewApple(body, width, height)

            return Start(body, apple, facing)
        }

        /**
         * generate a new apple that isn't inside the given [body]
         * or outside the [width] and [height]
         */
        fun generateNewApple(body : ArrayList<Coords>, width: Int, height: Int): Coords {
            var newApple: Coords

            //generate random coordinates in the grid that aren't in the snake's body
            do {
                newApple = Coords(
                    Random.nextInt(width),
                    Random.nextInt(height)
                )
            } while (newApple in body)

            return newApple
        }
    }
}