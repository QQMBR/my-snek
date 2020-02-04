package com.example.mysnek

import android.util.Log
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.ViewModel
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable

class GameViewModel(obs: Observable<GameModel.Direction>) : ViewModel() {
    //contains the game logic
    private val model = GameModel()

    init {
        Log.d(TAG, "Initialized VM")
    }

    //create gameData from the game data Observable by turning it first into
    //a Flowable with a backpressure strategy
    val liveGameData by lazy {
        LiveDataReactiveStreams.fromPublisher<ArrayList<Coords>>(
            model.run {
                setInputStream(obs)
                snake.toFlowable(BackpressureStrategy.LATEST)
            }
        )
    }

    companion object {
        const val TAG = "GameViewModel"
    }
}