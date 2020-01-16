package com.example.finddine.ClientInterface.Activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.finddine.ClientInterface.StallRecycler.Stall
import com.example.finddine.ClientInterface.StallRecycler.StallAdaptor
import com.example.finddine.DevMenu.TestActivity
import com.example.finddine.DevMenu.MapsActivity
import com.example.finddine.R
import com.example.finddine.databinding.ActivityHomeScreenBinding

class HomeScreen : AppCompatActivity() {

    private lateinit var binding: ActivityHomeScreenBinding
    private var stalls = ArrayList<Stall>()
    private var queryResults = ArrayList<Stall>()

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
        queryResults = stalls
        binding.homeRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.homeRecyclerView.adapter = StallAdaptor(queryResults, this)

        val searchQuery = binding.homeSearchView.query
        binding.homeSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            override fun onQueryTextChange(newText: String): Boolean {
//                Log.d("QueryText Focus Change", "Triggered") // is triggered
                if (searchQuery.length == 0) {
                    queryResults = stalls
                } else if (searchQuery.length > 0) {
                    queryResults = ArrayList()
                    val toAdd = stalls.filter {
                        it.name.toLowerCase().contains(searchQuery) or it.food.toLowerCase().contains(searchQuery)
                    } as ArrayList<Stall>
                    queryResults.addAll(toAdd)

                }

                Log.d("Query Results", queryResults.toString()) // does change results correctly

                // TODO: Better way to update the adapter
                val adapter = StallAdaptor(queryResults, this@HomeScreen)
                binding.homeRecyclerView.adapter = adapter

                return true
            }

            override fun onQueryTextSubmit(query: String?): Boolean {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
        })
    }

    // function currently add mock stalls' data
    // TODO: To pull google reviews data and store in sqllite / firebase instead.
    fun addStalls() {
        stalls.add(Stall("Wang Jia Ban Mian", "Ban Mian", 4.8, 50))
        stalls.add(Stall("Fishball Story", "Fishball Noodle", 4.5, 20))
        stalls.add(Stall("Ah Tan Wings", "Chicken cutlet rice", 4.5, 50))
        stalls.add(Stall("Wong Kee Noodles", "Wanton Noodles", 4.5, 10))
        stalls.add(Stall("Geylang Prawn Noodles", "Prawn Noodles", 4.5, 120))
    }
}
