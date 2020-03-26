package com.example.mysnek

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.GestureDetectorCompat
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.ObservableOnSubscribe
import java.util.ArrayList
import java.util.EnumMap
import kotlin.math.absoluteValue

class GameSurfaceView(context: Context, settings: SnekSettings = SnekSettings.default): GLSurfaceView(context) {

    //single parameter constructor uses the default settings
    constructor(context: Context) : this(context, SnekSettings.default)

    //renders tiles representing the snake and apple on a grid of specifiable size
    //takes an enum map of color resources to color different parts of the game
    private val renderer : GameRenderer

    //use OpenGLES 2 and a GameRenderer as renderer
    init {
        Log.d(TAG, "Setting up OpenGL SurfaceView")
        setEGLContextClientVersion(2)

        renderer = {id: Int -> ResourcesCompat.getColor(resources, id, null)}.run {
            GameRenderer(
                EnumMap(mapOf(Pair(SnekColors.BACKGROUND,      this(R.color.colorBackground1)),
                    Pair(SnekColors.HEAD,            this(R.color.colorHead1)),
                    Pair(SnekColors.APPLE,           this(R.color.colorApple1)),
                    Pair(SnekColors.BODY,            this(R.color.colorBody1)),
                    Pair(SnekColors.GRID_BACKGROUND, this(R.color.colorGridBackground1))
                )),
                settings
            )
        }

        setRenderer(renderer)
        Log.d(TAG, "Set renderer")
    }

    //use the renderer thread to set the new coordinates of the box based on a
    //direction; UNUSED: GameModel calculates the coordinates directly, see renderTileAt
    //fun moveBox(dir: GameModel.Direction) = queueEvent {renderer.moveBox(dir)}

    //render the tile at the given coordinates
    //fun renderTileAt(p: Coords) = queueEvent {renderer.renderTileAt(p)}

    fun renderAll(coords: ArrayList<Coords>, apple: Coords) {
        queueEvent {
            renderer.renderAllSafe(coords, apple)
        }
    }
    fun clearTiles() = queueEvent { renderer.clearTiles() }

    fun pauseGame() = queueEvent { renderer.pauseSafe() }.also {Log.d(TAG, "Pausing the game")}
    fun resumeGame() = queueEvent { renderer.resumeSafe() }.also { Log.d(TAG, "Removing overlay") }

    //a gesture detector that can also be used to create a new Observable
    //listens only to fling events and calls onNext each time a fling is registered
    //with the determined direction of the fling
    private val gestureDetector = object
        : GestureDetector.SimpleOnGestureListener(),
          ObservableOnSubscribe<GameModel.GameControl> {

        private var observableEmitter: ObservableEmitter<GameModel.GameControl>? = null

        override fun subscribe(emitter: ObservableEmitter<GameModel.GameControl>) {
            observableEmitter = emitter
        }

        override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
            Log.d(TAG, "Tapped at (${e?.x}, ${e?.y})")

            e?.apply {
                queueEvent {
                    //renderer checks for the pause button having been clicked,
                    //if yes, we send signal to stop the game
                    if (renderer.handleTap(x, y)) {
                        observableEmitter?.onNext(GameModel.Flow.PAUSE)
                    }
                }
            }

            return super.onSingleTapConfirmed(e)
        }
        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent?,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            if (e1 != null && e2 != null) {
                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y

                //call onNext of the emitter with the direction of the largest
                //component of the movement
                observableEmitter?.onNext({
                    if (diffX.absoluteValue > diffY.absoluteValue) {
                        if (diffX > 0) {
                            GameModel.Direction.RIGHT
                        } else {
                            GameModel.Direction.LEFT
                        }
                    } else {
                        if (diffY > 0) {
                            GameModel.Direction.DOWN
                        } else {
                            GameModel.Direction.UP
                        }
                    }
                } ())
            }

            return super.onFling(e1, e2, velocityX, velocityY)
        }
    }

    val screenStream: Observable<GameModel.GameControl> = Observable.create(gestureDetector)

    private val detector = GestureDetectorCompat(context, gestureDetector)

    //TODO override performClick() :)
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        detector.onTouchEvent(event)

        return true
    }

    companion object {
        const val TAG = "GameSurfaceView"
    }
}