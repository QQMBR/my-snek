package com.example.mysnek

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer

class MainActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val gameSurfaceView = GameSurfaceView(this)
        setContentView(gameSurfaceView)

        supportActionBar?.hide()

        //create a ViewModel using the stream of directions from the SurfaceView
        val viewModel: GameViewModel by viewModels()
        /*
        disposable = gameSurfaceView.flingStream.subscribeBy (
            onNext = {Log.d(TAG, "onNext")},
            onComplete = {Log.d(TAG, "onComplete")},
            onError = {Log.d(TAG, "onError")}
        )
        */

        //connect the ViewModel's subject to the input stream and signal
        //that it may start emitting items
        gameSurfaceView.flingStream.subscribe(viewModel.events)
        gameSurfaceView.flingStream.connect()

        //observe changes in the live data and send them for rendering the SurfaceView
        //or handle the end of the game
        viewModel.liveGameData.observe(this, Observer {
            when (it) {
                is GameOver    -> gameOver()
                is UpdateSnake -> gameSurfaceView.renderTiles(it.coords)
            }
        })
    }

    private fun gameOver() {
        //TODO properly handle game over
        //go back to the starting activity
        startActivity(Intent(this, StartActivity::class.java))
    }

    companion object {
        const val TAG = "MainActivity"
    }
}