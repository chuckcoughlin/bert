/**
 * Copyright 2022-2023 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.ui.adapter

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * Facilitates data transfer by the Recycler view.
 */
class TextDataViewHolder(v: View) : RecyclerView.ViewHolder(v) {
    private val rowParent: ViewGroup
    val timestampView: TextView
    val sourceView:    TextView
    val messageView:   TextView
    val detailView:    TextView


    /**
     * The ViewGroup is actually a nested LinearLayout holding text views
     * See log_item.xml
     * - timestamp
     * - source
     * - message
     * @param v the current view
     */
    init {
        this.setIsRecyclable(false)
        rowParent = (itemView as ViewGroup).getChildAt(0) as ViewGroup
        timestampView = rowParent.getChildAt(0) as TextView
        sourceView = rowParent.getChildAt(1) as TextView
        messageView = rowParent.getChildAt(2) as TextView
        detailView = (itemView as ViewGroup).getChildAt(1) as TextView
    }
}
