package com.github.shortcutbookmarker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.shortcutbookmarker.models.ChromeShortcut
import com.google.android.material.card.MaterialCardView

class ShortcutAdapter : ListAdapter<ChromeShortcut, ShortcutAdapter.ViewHolder>(DiffCallback()) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleText: TextView = view.findViewById(R.id.titleText)
        val urlText: TextView = view.findViewById(R.id.urlText)
        val domainText: TextView = view.findViewById(R.id.domainText)
        val card: MaterialCardView = view.findViewById(R.id.card)
        val statusIcon: ImageView = view.findViewById(R.id.statusIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shortcut, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val shortcut = getItem(position)
        
        holder.titleText.text = shortcut.title
        holder.urlText.text = shortcut.url
        holder.domainText.text = shortcut.getDomain()
        
        // Update card appearance based on bookmark status
        if (shortcut.isBookmarked) {
            holder.card.strokeColor = holder.itemView.context.getColor(R.color.success_green)
            holder.card.strokeWidth = 4
            holder.statusIcon.visibility = View.VISIBLE
            holder.card.alpha = 0.7f
        } else {
            holder.card.strokeColor = holder.itemView.context.getColor(R.color.border_gray)
            holder.card.strokeWidth = 2
            holder.statusIcon.visibility = View.GONE
            holder.card.alpha = 1.0f
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ChromeShortcut>() {
        override fun areItemsTheSame(oldItem: ChromeShortcut, newItem: ChromeShortcut): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ChromeShortcut, newItem: ChromeShortcut): Boolean {
            return oldItem == newItem
        }
    }
}
