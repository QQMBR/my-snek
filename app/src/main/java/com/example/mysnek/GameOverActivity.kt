package com.example.mysnek

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_game_over.*

class GameOverActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_over)

        val score = intent.extras?.getInt("score")

        score?.apply {
            scoreView.text = getString(R.string.point_scored, this)
        }

        replayButton.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }
}
