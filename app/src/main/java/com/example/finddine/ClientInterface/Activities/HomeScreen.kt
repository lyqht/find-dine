package com.example.finddine.ClientInterface.Activities

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.finddine.ClientInterface.StallRecycler.Stall
import com.example.finddine.ClientInterface.StallRecycler.StallAdaptor
import com.example.finddine.DevMenu.MainActivity
import com.example.finddine.DevMenu.MultipleAccessPointRangingResultsActivity
import com.example.finddine.DevMenu.TestActivity
import com.example.finddine.DevMenu.MapsActivity
import com.example.finddine.R
import com.example.finddine.databinding.ActivityHomeScreenBinding

class HomeScreen : AppCompatActivity() {

    private lateinit var binding: ActivityHomeScreenBinding
    private val stalls = ArrayList<Stall>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_home_screen)
        // redirect to DevMenu Button
        val intent = Intent(this, TestActivity::class.java)
        binding.redirectButton.setOnClickListener { startActivity(intent) }


        var mapIntent = Intent(this, MapsActivity::class.java)
        binding.mapButton.setOnClickListener { startActivity(mapIntent) }

        // loading data and using the adaptor to create list item views for the recycler view
        addStalls()
        binding.homeRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.homeRecyclerView.adapter = StallAdaptor(stalls, this)

    }

    // TODO: function to add mock stalls' data
    // To pull google reviews data and store in sqllite / firebase instead.
    fun addStalls() {
        stalls.add(Stall("Wang Jia Ban Mian", "Ban Mian", 4.8, 50))
        stalls.add(Stall("Fishball Story", "Fishball Noodle", 4.5, 20))
        stalls.add(Stall("Ah Tan Wings", "Chicken cutlet rice", 4.5, 50))
        stalls.add(Stall("Wong Kee Noodles", "Wanton Noodles", 4.5, 10))
        stalls.add(Stall("Geylang Prawn Noodles", "Prawn Noodles", 4.5, 120))
    }
}
