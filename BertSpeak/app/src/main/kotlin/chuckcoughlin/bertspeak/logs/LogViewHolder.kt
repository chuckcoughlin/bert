/**
 * Copyright 2022 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.logs

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * Facilitates data transfer by the Recycler view.
 */
class LogViewHolder(v: View) : RecyclerView.ViewHolder(v) {
    private val rowParent: ViewGroup
    val timestampView: TextView
        get() = rowParent.getChildAt(0) as TextView
    val sourceView: TextView
        get() = rowParent.getChildAt(1) as TextView
    val messageView: TextView
        get() = rowParent.getChildAt(2) as TextView
    val detailView: TextView
        get() = (itemView as ViewGroup).getChildAt(1) as TextView

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
    }
}
