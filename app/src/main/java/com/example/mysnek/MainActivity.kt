package com.example.mysnek

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.Observer
import androidx.lifecycle.OnLifecycleEvent

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "Created")
        super.onCreate(savedInstanceState)

        val gameSurfaceView = GameSurfaceView(this)
        setContentView(gameSurfaceView)

        supportActionBar?.hide()

        //create a ViewModel using the stream of directions from the SurfaceView
        val viewModel: GameViewModel by viewModels()

        //connect the ViewModel's subject to the input stream and signal
        //that it may start emitting items
        gameSurfaceView.flingStream.subscribe(viewModel.events)

        gameSurfaceView.flingStream.connect()

        //observe changes in the live data and send them for rendering the SurfaceView
        //or handle the end of the game
        viewModel.liveGameData.observe(this, Observer {

            //a function that recursively calls itself if the data is enclosed in a
            //resume
            fun handleData(data: SnekEvent) {
                when (data) {
                    is GameOver    -> gameOver(data.score)
                    is UpdateBody  -> gameSurfaceView.renderTiles(data.coords)
                    is UpdateApple -> {
                        gameSurfaceView.renderTiles(data.coords)
                        gameSurfaceView.renderApple(data.newApple)
                    }
                    is Pause -> {
                        gameSurfaceView.pauseGame()
                    }
                    is Resume -> {
                        Log.d(TAG, "Resuming")
                        gameSurfaceView.resumeGame()
                        handleData(data.newEvent)
                    }
                }
            }

            handleData(it)
        })

        lifecycle.addObserver(object : LifecycleObserver {

            @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            fun pause() {
                Log.d(TAG, "Paused from LifecycleObserver")
                viewModel.pauseGame()
            }
        })

        viewModel.requestApple()
    }

    private fun gameOver(score: Int) {
        //TODO properly handle game over
        //destroy the ViewModel as it only handles a single game
        viewModelStore.clear()

        //go back to the starting activity
        startActivity(Intent(this, GameOverActivity::class.java).putExtra("score", score))
    }

    override fun onDestroy() {
        Log.d(TAG, "Destroyed")
        super.onDestroy()
    }

    override fun onPause() {
        Log.d(TAG, "Paused")
        super.onPause()
    }

    override fun onStart() {
        Log.d(TAG, "Start")


        super.onStart()
    }

    override fun onRestart() {
        Log.d(TAG, "Restart")
        super.onRestart()
    }

    companion object {
        const val TAG = "MainActivity"
    }

}