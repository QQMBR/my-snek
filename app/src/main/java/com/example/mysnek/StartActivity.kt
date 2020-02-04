package com.example.mysnek

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_start.*

//pretty much empty entry point of the application
//press a button to move to the MainActivity
class StartActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        button.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }
}
