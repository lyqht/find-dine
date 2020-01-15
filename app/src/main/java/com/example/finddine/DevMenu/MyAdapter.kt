package com.example.finddine.DevMenu

import android.net.wifi.ScanResult
import androidx.recyclerview.widget.RecyclerView
import android.widget.TextView

import android.view.LayoutInflater
import android.view.View
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import android.view.ViewGroup
import com.example.finddine.R


class MyAdapter() : RecyclerView.Adapter<ViewHolder>() {
    constructor (list: MutableList<ScanResult>, scanResultClickListener: ScanResultClickListener) : this() {
        mWifiAccessPointsWithRtt = list
        sScanResultClickListener = scanResultClickListener
    }

    private val HEADER_POSITION = 0

    private val TYPE_HEADER = 0
    private val TYPE_ITEM = 1

    private var sScanResultClickListener: ScanResultClickListener? = null
    private var mWifiAccessPointsWithRtt: MutableList<ScanResult> = mutableListOf()


    class ViewHolderHeader(view: View) : RecyclerView.ViewHolder(view)

    inner class ViewHolderItem(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
        var mSsidTextView: TextView
        var mBssidTextView: TextView

        init {
            view.setOnClickListener(this)
            mSsidTextView = view.findViewById(R.id.ssid_text_view)
            mBssidTextView = view.findViewById(R.id.bssid_text_view)
        }

        override fun onClick(view: View) {
            sScanResultClickListener?.onScanResultItemClick(getItem(adapterPosition))
        }
    }

    fun swapData(list: List<ScanResult>) {

        // Always clear with any update, as even an empty list means no WifiRtt devices were found.
        mWifiAccessPointsWithRtt.clear()

        if (list != null && list.size > 0) {
            mWifiAccessPointsWithRtt.addAll(list)
        }

        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val viewHolder: ViewHolder

        if (viewType == TYPE_HEADER) {
            viewHolder = ViewHolderHeader(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.rtt_recycler_row_header, parent, false)
            )

        } else if (viewType == TYPE_ITEM) {
            viewHolder = ViewHolderItem(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.rtt_recycler_row_item, parent, false)
            )
        } else {
            throw RuntimeException("$viewType isn't a valid view type.")
        }

        return viewHolder
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        if (viewHolder is ViewHolderHeader) {
            // No updates need to be made to header view (defaults remain same).

        } else if (viewHolder is ViewHolderItem) {
            val currentScanResult = getItem(position)

            viewHolder.mSsidTextView.text = currentScanResult.SSID
            viewHolder.mBssidTextView.text = currentScanResult.BSSID

        } else {
            throw RuntimeException("$viewHolder isn't a valid view holder.")
        }
    }


    /*
     * Because we added a header item to the list, we need to decrement the position by one to get
     * the proper place in the list.
     */
    private fun getItem(position: Int): ScanResult {
        return mWifiAccessPointsWithRtt.get(position - 1)
    }

    // Returns size of list plus the header item (adds extra item).
    override fun getItemCount(): Int {
        return mWifiAccessPointsWithRtt.size + 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == HEADER_POSITION) {
            TYPE_HEADER
        } else {
            TYPE_ITEM
        }
    }

    // Used to inform the class containing the RecyclerView that one of the ScanResult items in the
    // list was clicked.
    interface ScanResultClickListener {
        fun onScanResultItemClick(scanResult: ScanResult?)
    }
}