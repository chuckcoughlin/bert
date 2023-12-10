/**
 * Copyright 2022-2023 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.logs

import android.graphics.Color
import android.transition.TransitionManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import chuckcoughlin.bertspeak.R
import chuckcoughlin.bertspeak.common.MessageType
import chuckcoughlin.bertspeak.speech.TextMessage
import java.text.SimpleDateFormat
import java.util.*

/**
 * This class is a link between a RecyclerView and the data backstop.
 * Each element in the list is a string, a text message.
 */
class TextMessageAdapter(msgs: List<TextMessage>) : RecyclerView.Adapter<LogViewHolder>() {
    private val dateFormatter = SimpleDateFormat("HH:mm:ss.SSS")
    private var expandedPosition = -1
    private var recyclerView :RecyclerView? = null
    private var messages: List<TextMessage> = msgs

     override fun onAttachedToRecyclerView(view: RecyclerView) {
         super.onAttachedToRecyclerView(view)
         Log.i(CLSS, String.format("onAttachedToRecyclerView"))
         this.recyclerView = view
    }

    /**
     * Create a new view holder. Inflate the row layout, set the item height.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        Log.i(CLSS, String.format("onCreateViewHolder count = %d", messages.size))
        val inflater: LayoutInflater = LayoutInflater.from(parent.context)
        val shouldAttachToParent = false
        val layout: LinearLayout =
            inflater.inflate(R.layout.log_item, parent, shouldAttachToParent) as LinearLayout
        Log.i(CLSS, String.format("onCreateViewHolder inflation complete"))
        return LogViewHolder(layout)
    }

    /**
     * Set the contents of the holder appropriate to the position visible.
     * Change the views depending on whether or not the item is selected.
     * In an expanded view the message text is on its own line. We add date and source.
     * @param holder the viewholder that should be populated at the given position
     * @param position row that should be updated
     */
    override fun onBindViewHolder(holder: LogViewHolder, pos: Int) {
        var position = pos
        Log.i(CLSS, String.format("onBindViewHolder at %d of %d", position, messages.size))
        if( messages.size == 0 ) {
            Log.w(CLSS, String.format("onBindViewHolder no messsages to bind"))
            return
        }
        if(position>=messages.size) position = messages.size - 1
        val expand = position == expandedPosition
        val msg: TextMessage = messages[position]
        val type: MessageType = msg.messageType
        // The timestamp is always the same
        val timestampView: TextView = holder.timestampView
        val tstamp: Date = msg.timestamp
        val dt = dateFormatter.format(tstamp)
        timestampView.text = dt

        // In expanded mode the source is the type
        val sourceView: TextView = holder.sourceView
        var source: String = msg.messageType.name
        if (expand) {
            sourceView.text = source
            if (type == MessageType.ANS) {
                sourceView.setTextColor(Color.BLUE)
            }
        }
        else {
            // Truncate source to 16 char
            if (source.length > SOURCE_LEN) source = source.substring(0, SOURCE_LEN)
            sourceView.text = source
        }

        // In expanded mode, the message is the source (node-name).
        val messageView: TextView = holder.messageView
        var msgText: String = msg.message.trim { it <= ' ' }
        if (expand) {
            messageView.text = source
        }
        else {
            if (msgText.length > MESSAGE_LEN) msgText = msgText.substring(0, MESSAGE_LEN)
            messageView.text = msgText
            if (type == MessageType.ANS) {
                messageView.setTextColor(Color.BLUE)
            }
        }
        val detailView: TextView = holder.detailView
        val params: ViewGroup.LayoutParams = holder.itemView.layoutParams
        if (expand) {
            detailView.text = msgText
            detailView.visibility = View.VISIBLE
            holder.itemView.isActivated = false
            params.height = LOG_MSG_HEIGHT_EXPANDED
        }
        else {
            detailView.visibility = View.GONE
            holder.itemView.isActivated = true
            params.height = LOG_MSG_HEIGHT
        }
        holder.itemView.layoutParams = params
        holder.itemView.setOnClickListener(View.OnClickListener {
            expandedPosition = if (expand) -1 else position
            if(recyclerView!=null ) {
                TransitionManager.beginDelayedTransition(recyclerView)
                reportDataSetChanged()
            }
        })
    }

    // It is important that the widget and backing manager be in synch
    // with respect to item count.
    override fun getItemCount(): Int {
        Log.i(CLSS, String.format("getItemCount = %d", messages.size))
        return messages.size
    }

    override fun onDetachedFromRecyclerView(view: RecyclerView) {
        super.onDetachedFromRecyclerView(view)
        Log.i(CLSS, String.format("onDetachedFromRecyclerView count = %d", messages.size))
        this.recyclerView = null
    }

    /**
     * Replace the source of messages for this adapter.
     * @param msgs message list, managed externally
     */
    fun resetList(msgs: List<TextMessage>?) {
        if(msgs == null ) {
            messages = listOf<TextMessage>()
        }
        else {
            messages = msgs
        }
        Log.i(CLSS, String.format("resetList new count = %d", messages.size))
        reportDataSetChanged()
    }

    /*
     * The parent class notifyDataSetChanged() method is final.
     * This allows us to get a hook for debugging
     */
    fun reportDataSetChanged() {
        Log.i(CLSS, String.format("reportDataSetChanged count = %d", messages.size))
        //notifyDataSetChanged()
        Log.i(CLSS, String.format("reportDataSetChanged SUCCESS"))
    }

    private val CLSS = "TextMessageAdapter"
    private val MESSAGE_LEN = 45
    private val SOURCE_LEN = 15
    private val LOG_MSG_HEIGHT = 75
    private val LOG_MSG_HEIGHT_EXPANDED = 225

    init {
        messages = msgs
    }
}
