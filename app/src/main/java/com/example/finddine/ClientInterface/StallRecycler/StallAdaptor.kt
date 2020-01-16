package com.example.finddine.ClientInterface.StallRecycler

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.finddine.R
import com.example.finddine.R.layout.stall_list_item
import kotlinx.android.synthetic.main.stall_list_item.view.*

class StallAdaptor (val items : ArrayList<Stall>, val context: Context) : RecyclerView.Adapter<StallAdaptor.ViewHolder>() {

    override fun getItemCount(): Int {
        return items.size
    }

    class ViewHolder (view: View) : RecyclerView.ViewHolder(view) {
        val stallName = view.stall_name
        val foodName = view.food_name
        val stallRating = view.stall_rating
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(stall_list_item, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder?.stallName?.text = item.name
        holder?.foodName?.text = item.food
        holder?.stallRating?.text = item.rating.toString() + "\u2605"
        holder?.stallRating?.contentDescription = item.rating.toString() + "stars"
    }

}