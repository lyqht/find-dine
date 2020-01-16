package com.example.finddine.ClientInterface.Activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.example.finddine.R
import com.example.finddine.databinding.ActivityNavigationStartBinding

class NavigationStart : AppCompatActivity() {

    private lateinit var binding: ActivityNavigationStartBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_navigation_start)
        getSupportActionBar()?.hide()
        val extra: Bundle? = getIntent().getExtras()
        val stall_name = extra?.getString("name")
        binding.directionsText.setText("Directions to " + stall_name)
        binding.navigationExitText.setOnClickListener { finish() }
    }
}
