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

    val liveGameData = MutableLiveData<SnekEvent>()
    val liveEventData = MutableLiveData<Event<SingleSnekEvent>>()

    private var isPaused = false
    private var needToStart = true

    val events = PublishSubject.create<GameModel.GameControl>()

    private val gameModel : GameModel

    private val disposable = events.subscribe {
        if (it == GameModel.Flow.PAUSE) {
            isPaused = true
            Log.d(TAG, "Sending Pause")
            liveEventData.postValue(Event(Pause))
        }
    }

    init {
        Log.d(TAG, "Init GameView")

        gameModel = GameModel(events, settings)

        gameModel.data.also {
            it.subscribe(this)
            it.connect()
        }
    }

    fun startGame() {
        if (needToStart) {
            events.onNext(GameModel.Flow.START_GAME)
            needToStart = false
        }
    }


    fun pauseGame() {
        Log.d(TAG, "Pausing game")
        events.onNext(GameModel.Flow.PAUSE)
    }

    override fun onComplete() {
        Log.d(TAG, "Man's done up in this")

        endGame(-1)
    }

    override fun onError(e: Throwable) {
        Log.d(TAG, "Fam can't cop it no more")

        endGame(-2)
    }

    override fun onNext(game: SnekData) {
        Log.d(TAG, "onNext $game")

        if (game is Movable) {
            liveGameData.postValue(Update(game.body, game.apple))
        }
        else if (game is Over) {
            endGame(game.body.size - settings.startSize)
        }

        if (isPaused) {
            liveEventData.postValue(Event(Resume))
            isPaused = false
        }
    }

    private fun endGame(score: Int) {
        needToStart = true
        Log.d(TAG, "Posting GameOver")
        liveEventData.postValue(Event(GameOver(score)))
    }
    override fun onSubscribe(d: Disposable) {
        Log.d(TAG, "Get ready for some cringe")
    }

    override fun onCleared() {
        disposable.dispose()

        super.onCleared()
    }
    companion object {
        const val TAG = "GameViewModel"
    }
}