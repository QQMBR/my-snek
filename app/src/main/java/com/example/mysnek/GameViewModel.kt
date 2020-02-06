package com.example.mysnek

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import io.reactivex.observables.ConnectableObservable
import io.reactivex.subjects.PublishSubject

class GameViewModel(private val obs: ConnectableObservable<GameModel.Direction>) : ViewModel(),
    Observer<ArrayList<Coords>> {

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

    fun setFlingStream(input: ConnectableObservable<GameModel.Direction>) {
        //model.setInputStream(input)
        val disposable = input.subscribe {
            Log.d(TAG, "Received fling")
        }

        input.connect()
    }

    fun reconnect() {
        val disposable = obs.subscribe {
            Log.d(TAG, "Received fling")
        }

        obs.connect()
    }

    init {
        Log.d(TAG, "Initialized VM")
    }



    //create gameData from the game data Observable by turning it first into
    //a Flowable with a backpressure strategy
    /*
    val liveGameData by lazy {
        LiveDataReactiveStreams.fromPublisher<ArrayList<Coords>>(

                //snake.toFlowable(BackpressureStrategy.LATEST)
        )
    }
     */


    companion object {
        const val TAG = "GameViewModel"
    }
}