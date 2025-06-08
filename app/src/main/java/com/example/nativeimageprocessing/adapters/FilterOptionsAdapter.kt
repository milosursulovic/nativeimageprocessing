package com.example.nativeimageprocessing.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.nativeimageprocessing.FilterOption

class FilterOptionsAdapter(
    private val options: List<FilterOption>,
    private val listener: (FilterOption) -> Unit
) : RecyclerView.Adapter<FilterOptionsAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val optionText: TextView = view.findViewById(android.R.id.text1)

        init {
            view.setOnClickListener {
                listener(options[adapterPosition])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)

        val params = view.layoutParams
        params.width = 200  // fixed width for horizontal scrolling
        view.layoutParams = params

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.optionText.text = options[position].displayName
    }

    override fun getItemCount() = options.size
}
