package com.example.mysnek

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject

//TODO does it even really make sense to only observe one type?
class GameViewModel(private val settings: SnekSettings) : ViewModel(),
    Observer<SnekData> {

    private val liveGameData = MutableLiveData<SnekEvent>()

    fun getGameData() = liveGameData

    val liveEventData = MutableLiveData<Event<SingleSnekEvent>>()

    private var isPaused = false
    //private var gameOver = true

    val events = PublishSubject.create<GameModel.GameControl>()

    private val gameModel = GameModel(events, settings)

    private val disposable = events.subscribe {
        if (it == GameModel.Flow.PAUSE) {
            isPaused = true
            Log.d(TAG, "Sending Pause")
            liveEventData.postValue(Event(Pause2))
        }
    }

    init {
        Log.d(TAG, "Init GameView")

        gameModel.snakeData.also {
            it.subscribe(this)
            it.connect()
        }
    }

    fun clearNotHandled() {
        liveEventData.value?.clear()
    }

    /*
    fun startGameIfOver() {
        if (gameOver) {
            //events.onNext(GameModel.Flow.START_GAME)
            gameOver = false
        }
    }
     */

    override fun onComplete() {
        Log.d(TAG, "Man's done up in this")
        //liveGameData.postValue(Over(arrayListOf()))

        endGame(0)
    }

    override fun onError(e: Throwable) {
        Log.d(TAG, "Fam can't cop it no more")

        endGame(0)
    }

    override fun onNext(game: SnekData) {
        //firstGame = false

        Log.d(TAG, "onNext $game")

        if (game is Movable) {
            liveGameData.postValue(Update(game.body, game.apple))
        }
        else if (game is Over) {
            endGame(game.body.size - settings.startSize)
        }

        if (isPaused) {
            liveEventData.postValue(Event(Resume2))
            isPaused = false
        }
    }

    private fun endGame(score: Int) {
        //gameOver = true
        Log.d(TAG, "Posting GameOver")
        liveEventData.postValue(Event(GameOver2(score)))
    }
    override fun onSubscribe(d: Disposable) {
        Log.d(TAG, "Get ready for some cringe")
    }


    init {
        Log.d(TAG, "Initialized VM")
    }

    override fun onCleared() {
        disposable.dispose()

        super.onCleared()
    }
    companion object {
        const val TAG = "GameViewModel"
    }
}