package com.example.finddine.ClientInterface.Activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.databinding.DataBindingUtil
import com.example.finddine.R
import com.example.finddine.databinding.ActivityNavigationStartBinding
import java.util.*

class NavigationStart : AppCompatActivity(),TextToSpeech.OnInitListener {

    private lateinit var binding: ActivityNavigationStartBinding
    private lateinit var tts: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_navigation_start)
        val extra: Bundle? = getIntent().getExtras()
        val stall_name = extra?.getString("name")
        binding.directionsText.setText("Directions to " + stall_name)
        binding.navigationExitText.setOnClickListener { finish() }

        // uncomment this for textToSpeech
        // textToSpeech voice is spoken before talkback, so there may be repeat of text2speech voice.
//        tts = TextToSpeech(this, this)

    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // set US English as language for tts
            val result = tts!!.setLanguage(Locale.US)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS","The Language specified is not supported!")
            } else {
                speakOut()
            }

        } else {
            Log.e("TTS", "Initilization Failed!")
        }

    }

    private fun speakOut() {
        val text = binding.directionsText.text.toString()
        tts!!.speak(text, TextToSpeech.QUEUE_FLUSH, null,"")
    }

    public override fun onDestroy() {
        // Shutdown TTS
        if (tts != null) {
            tts!!.stop()
            tts!!.shutdown()
        }
        super.onDestroy()
    }
}
