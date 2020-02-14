package com.example.mysnek

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject

//TODO does it even really make sense to only observe one type?
class GameViewModel : ViewModel(),
    Observer<SnekData> {

    val liveGameData = MutableLiveData<SnekEvent>()

    val events = PublishSubject.create<GameModel.Direction>()

    private val gameModel = GameModel(events)

    init {

        Log.d(TAG, "Init GameView")
        //events stream contains the input information, apply the game
        //transformation to get the stream of the snake's body
        //GameModel.apply(events).subscribe(this)

        gameModel.snakeData.also {
            it.subscribe(this)
            it.connect()
        }
    }

    override fun onComplete() {
        Log.d(TAG, "Man's done up in this")
        //liveGameData.postValue(Over(arrayListOf()))
    }

    override fun onError(e: Throwable) {
        Log.d(TAG, "Fam can't cop it no more")
    }

    override fun onNext(game: SnekData) {
        when (game) {
            is Over  -> liveGameData.postValue(GameOver(game.body.size - SnekSettings.START_SIZE - 1))
            is Move  -> liveGameData.postValue(UpdateBody(game.body))
            is Apple -> liveGameData.postValue(UpdateApple(game.body, game.apple))
        }
    }

    override fun onSubscribe(d: Disposable) {
        Log.d(TAG, "Get ready for some cringe")
    }

    init {
        Log.d(TAG, "Initialized VM")
    }

    companion object {
        const val TAG = "GameViewModel"
    }
}