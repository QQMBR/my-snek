package com.example.mysnek

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.Observer
import androidx.lifecycle.OnLifecycleEvent
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager

class GameFragment : Fragment() {

    private lateinit var settings : SnekSettings

    private var gameSurfaceView : GameSurfaceView? = null

    private val viewModel by lazy {

        //create a ViewModel using the stream of directions from the SurfaceView
        //and settings from shared preferences
        getViewModel { GameViewModel(settings) }
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "OnStart")

        gameSurfaceView?.onResume()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "OnCreate")
        lifecycle.addObserver(object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            fun pause() {
                Log.d(TAG, "Paused from LifecycleObserver")
                viewModel.pauseGame()
            }
        })

        settings = SnekSettings.fromSharedPreferences(
            PreferenceManager.getDefaultSharedPreferences(
                requireActivity()
            )
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        Log.d(TAG, "OnCreateView")

        gameSurfaceView = GameSurfaceView(requireActivity(), settings)
        //only show the surfaceView

        return gameSurfaceView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //connect the ViewModel's subject to the input stream and signal
        //that it may start emitting items
        gameSurfaceView?.screenStream?.subscribe(viewModel.events)

        //observe changes in the live data and send them for rendering the SurfaceView
        //or handle the end of the game
        viewModel.liveGameData.observe(viewLifecycleOwner, Observer {
            //Log.d(TAG, "Observed $it (${it.hashCode()})")
            when (it) {
                is Update  -> {
                    //Log.d(TAG, "Updating tiles $it (${it.hashCode()}), ${it.coords.hashCode()}")
                    gameSurfaceView?.renderAll(it.coords, it.apple)
                }
            }
        })

        viewModel.liveEventData.observe(viewLifecycleOwner, EventObserver {
            //Log.d(TAG, "Observed in event $it")
            when (it) {
                Pause -> gameSurfaceView?.pauseGame()
                Resume -> gameSurfaceView?.resumeGame()
                is GameOver -> gameOver(it.score)
            }
        })
    }

    //TODO properly handle game over
    private fun gameOver(score: Int) {

        //navigate to the game over destination fragment, safely passing the score
        findNavController().navigate(GameFragmentDirections.actionGameFragmentToGameOverFragment(score))
    }

    override fun onStop() {
        gameSurfaceView?.onPause()

        Log.d(TAG, "OnStop")

        super.onStop()
    }

    override fun onPause() {

        super.onPause()
        Log.d(TAG, "OnPause")
    }

    override fun onResume() {
        Log.d(TAG, "OnResume")

        //TODO can we do better than this?
        viewModel.startGame()

        super.onResume()
    }

    override fun onDestroyView() {
        viewModel.liveEventData.removeObservers(viewLifecycleOwner)
        viewModel.liveGameData.removeObservers(viewLifecycleOwner)

        super.onDestroyView()
    }

    companion object {


        const val TAG = "GameFragment"
    }
}
