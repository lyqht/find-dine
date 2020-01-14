package com.example.finddine.ClientInterface

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.finddine.DevMenu.MainActivity
import com.example.finddine.R
import kotlinx.android.synthetic.main.home_screen.*

class HomeScreen : AppCompatActivity() {

    private var redirectButton: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home_screen)
        setSupportActionBar(toolbar)

        redirectButton = findViewById(R.id.redirect_button)
        val intent = Intent(this, MainActivity::class.java)

        redirectButton?.setOnClickListener { startActivity(intent) }
    }
}
