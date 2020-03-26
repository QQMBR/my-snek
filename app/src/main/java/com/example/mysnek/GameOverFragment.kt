package com.example.mysnek

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.android.synthetic.main.fragment_game_over.*

class GameOverFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {


        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_game_over, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val safeArgs: GameOverFragmentArgs by navArgs()

        scoreView.text = getString(R.string.point_scored, safeArgs.score)

        replayButton.setOnClickListener {
            findNavController().navigate(GameOverFragmentDirections.actionGameOverFragmentToGameFragment())
        }

        menuButton.setOnClickListener {
            findNavController().navigate(GameOverFragmentDirections.actionGameOverFragmentToStartFragment())
        }
    }
}
