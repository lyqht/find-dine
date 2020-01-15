package com.example.finddine.ClientInterface.Activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.example.finddine.DevMenu.MainActivity
import com.example.finddine.DevMenu.MultipleAccessPointRangingResultsActivity
import com.example.finddine.R
import com.example.finddine.databinding.ActivityHomeScreenBinding

class HomeScreen : AppCompatActivity() {

    private lateinit var binding: ActivityHomeScreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_home_screen)

        val intent = Intent(this, MainActivity::class.java)
        binding.redirectButton.setOnClickListener { startActivity(intent) }

        val intent2 = Intent(this, MultipleAccessPointRangingResultsActivity::class.java)
        binding.redirectButton2.setOnClickListener { startActivity(intent2) }
    }
}
