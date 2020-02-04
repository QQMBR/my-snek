package com.example.mysnek

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.core.view.GestureDetectorCompat
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.ObservableOnSubscribe
import kotlin.math.absoluteValue

class GameSurfaceView(context: Context): GLSurfaceView(context) {

    //renders a single tile on a grid with specifiable size
    private val renderer = GameRenderer()

    //use the renderer thread to set the new coordinates of the box based on a
    //direction; UNUSED: GameModel calculates the coordinates directly, see renderTileAt
    fun moveBox(dir: GameModel.Direction) = queueEvent {renderer.moveBox(dir)}

    //render the tile at the given coordinates
    fun renderTileAt(p: Coords) = queueEvent {renderer.renderTileAt(p)}

    fun renderTiles(coords: ArrayList<Coords>) = queueEvent {renderer.renderTiles(coords)}

    //a gesture detector that can also be used to create a new Observable
    //listens only to fling events and calls onNext each time a fling is registered
    //with the determined direction of the fling
    private val gestureDetector = object
        : GestureDetector.SimpleOnGestureListener(),
          ObservableOnSubscribe<GameModel.Direction> {

        private var observableEmitter: ObservableEmitter<GameModel.Direction>? = null

        override fun subscribe(emitter: ObservableEmitter<GameModel.Direction>) {
            observableEmitter = emitter
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

    val flingStream: Observable<GameModel.Direction> = Observable.create(gestureDetector)

    private val detector = GestureDetectorCompat(context, gestureDetector)

    //TODO override performClick() :)
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        detector.onTouchEvent(event)

        return true
    }

    //use OpenGLES 2 and a GameRenderer as renderer
    init {
        setEGLContextClientVersion(2)

        setRenderer(renderer)
    }

    companion object {
        const val TAG = "GameSurfaceView"
    }
}