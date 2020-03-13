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

    var isPaused = false

    val events = PublishSubject.create<GameModel.GameControl>()

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

    //request an apple to be shown
    fun requestApple() {
        events.onNext(GameModel.Flow.SHOW_APPLE)
    }

    //send signal to pause the game (i.e. not move the snake)
    fun pauseGame() {
        events.onNext(GameModel.Flow.PAUSE)
        liveGameData.postValue(Pause)
        isPaused = true
    }

    override fun onComplete() {
        Log.d(TAG, "Man's done up in this")
        //liveGameData.postValue(Over(arrayListOf()))
    }

    override fun onError(e: Throwable) {
        Log.d(TAG, "Fam can't cop it no more")
    }

    override fun onNext(game: SnekData) {
        val inner = when (game) {
            is Over  -> GameOver(game.body.size - SnekSettings.START_SIZE - 1)
            is Move  -> UpdateBody(game.body)
            is Apple -> UpdateApple(game.body, game.apple)
            else -> null
        }

        //all that needs to be done is to determine
        //what we post to the activity
        inner?.also {
            liveGameData.postValue(
                //if the game was paused we
                //also send signal to resume the game
                if (isPaused) {
                    Resume(it)
                }
                else {
                    it
                }
            )
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