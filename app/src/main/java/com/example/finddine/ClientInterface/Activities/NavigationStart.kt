package com.example.finddine.ClientInterface.Activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.databinding.DataBindingUtil
import com.example.finddine.R
import com.example.finddine.databinding.ActivityNavigationStartBinding
import java.util.*
import kotlin.collections.ArrayList

class NavigationStart : AppCompatActivity() {

    private lateinit var binding: ActivityNavigationStartBinding
    private var counter: Int = 0
    private var instructions: ArrayList<String> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_navigation_start)
        val extra: Bundle? = getIntent().getExtras()
        val stall_name = extra?.getString("name")
        binding.directionsText.setText("Directions to " + stall_name)
        setInstructions()

        binding.navigationExitText.setOnClickListener { finish() }

        binding.refreshButton.setOnClickListener {
            counter += 1
            val currentInstruction = getInstruction()
            binding.instructionText.text = currentInstruction
            binding.refreshButton.contentDescription = currentInstruction + ". Double Tap to refresh instruction"
        }
    }

    fun setInstructions() {
        instructions.add("Walk 40m forwards, to the next turn")
        instructions.add("Turn left to your 9 o'clock.")
        instructions.add("Walk 5m forward to stall Wang Jia Ban Mian")
        instructions.add("Turn left")
        instructions.add("You have reached stall Wang Jia Ban Mian")
    }

    fun getInstruction(): String {
        return instructions.get(counter)
    }

}
