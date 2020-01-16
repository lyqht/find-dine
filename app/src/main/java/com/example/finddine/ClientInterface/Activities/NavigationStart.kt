package com.example.finddine.ClientInterface.Activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.databinding.DataBindingUtil
import com.example.finddine.R
import com.example.finddine.databinding.ActivityNavigationStartBinding
import java.util.*

class NavigationStart : AppCompatActivity() {

    private lateinit var binding: ActivityNavigationStartBinding
    private lateinit var tts: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_navigation_start)
        val extra: Bundle? = getIntent().getExtras()
        val stall_name = extra?.getString("name")
        binding.directionsText.setText("Directions to " + stall_name)
        binding.navigationExitText.setOnClickListener { finish() }
    }

}
