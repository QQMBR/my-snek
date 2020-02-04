package com.example.mysnek

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val gameSurfaceView = GameSurfaceView(this)
        setContentView(gameSurfaceView)

        //create a ViewModel using the stream of directions from the SurfaceView
        val viewModel: GameViewModel by viewModels {
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                    return GameViewModel(gameSurfaceView.flingStream) as T
                }
            }
        }

        //observe changes in the live data and send them for rendering the SurfaceView
        viewModel.liveGameData.observe(this, Observer(gameSurfaceView::renderTileAt))
    }

    companion object {
        const val TAG = "MainActivity"
    }
}