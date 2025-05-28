package com.github.droidworksstudio.launcher.settings

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.github.droidworksstudio.launcher.R
import com.github.droidworksstudio.launcher.utils.UIUtils

class LocationListAdapter(
    private val context: Context,
    private var locations: MutableList<Map<String, String>>,
    private val itemClickListener: OnItemClickListener
) :
    RecyclerView.Adapter<LocationListAdapter.AppViewHolder>() {

    private val uiUtils = UIUtils(context)

    interface OnItemClickListener {
        fun onItemClick(name: String?, latitude: String?, longitude: String?)
    }

    inner class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val listItem: ConstraintLayout = itemView.findViewById(R.id.locationPlace)
        val textView: TextView = listItem.findViewById(R.id.locationName)
        val regionText: TextView = listItem.findViewById(R.id.regionName)

        init {

            listItem.setOnClickListener {
                val position = bindingAdapterPosition
                val name = locations[position]["name"]
                val latitude = locations[position]["latitude"]
                val longitude = locations[position]["longitude"]
                itemClickListener.onItemClick(name, latitude, longitude)

            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.location_item_layout, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val location = locations[position]

        uiUtils.setAppAlignment(holder.textView, null, holder.regionText)

        uiUtils.setAppSize(holder.textView, null, holder.regionText)

        uiUtils.setWeatherSpacing(holder.listItem)

        holder.textView.text = location["name"]
        holder.regionText.text = context.getString(R.string.region_text, location["region"], location["country"])

        holder.textView.visibility = View.VISIBLE
    }

    override fun getItemCount(): Int {
        return locations.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateLocations(newApps: MutableList<Map<String, String>>) {
        locations.clear()
        locations = newApps
        notifyDataSetChanged()
    }
}