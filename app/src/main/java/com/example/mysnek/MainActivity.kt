package com.example.mysnek

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.reactivex.disposables.Disposable

class MainActivity : AppCompatActivity() {

    private lateinit var disposable: Disposable
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val gameSurfaceView = GameSurfaceView(this)
        setContentView(gameSurfaceView)

        supportActionBar?.hide()

        //create a ViewModel using the stream of directions from the SurfaceView
        val viewModel: GameViewModel by viewModels {
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                    return GameViewModel(gameSurfaceView.flingStream) as T
                }
            }
        }

        /*
        disposable = gameSurfaceView.flingStream.subscribeBy (
            onNext = {Log.d(TAG, "onNext")},
            onComplete = {Log.d(TAG, "onComplete")},
            onError = {Log.d(TAG, "onError")}
        )
        */

        //gameSurfaceView.flingStream.connect()
        //viewModel.setFlingStream(gameSurfaceView.flingStream)
        //viewModel.reconnect()

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

    override fun onDestroy() {
        //disposable.dispose()

        super.onDestroy()
    }
    companion object {
        const val TAG = "MainActivity"
    }
}