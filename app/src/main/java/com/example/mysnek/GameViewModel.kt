package com.example.mysnek

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject

class GameViewModel : ViewModel(),
    Observer<ArrayList<Coords>> {

    //TODO make sure everything is properly disposed of

    val liveGameData = MutableLiveData<UIEvent>()
    val events = PublishSubject.create<GameModel.Direction>()

    //contains the game logic
    private val model = GameModel()

    init {
        //events stream contains the input information, apply the game
        //transformation to get the stream of the snake's body
        model.apply(events).subscribe(this)
    }

    override fun onComplete() {
        Log.d(TAG, "Man's done up in this")
        liveGameData.postValue(GameOver)
    }

    override fun onError(e: Throwable) {
        Log.d(TAG, "Fam can't cop it no more")
    }

    override fun onNext(coords: ArrayList<Coords>) {
        Log.d(TAG, "Lezzgo da mandem")
        liveGameData.postValue(UpdateSnake(coords))
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