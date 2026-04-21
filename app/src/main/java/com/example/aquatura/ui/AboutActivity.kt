package com.example.aquatura.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.aquatura.R
import android.widget.Button

class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        // Set up the back button
        val backButton = findViewById<Button>(R.id.btn_back)
        backButton.setOnClickListener {
            finish()
        }
    }
}
