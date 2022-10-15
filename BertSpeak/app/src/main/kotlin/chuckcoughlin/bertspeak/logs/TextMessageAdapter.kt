/**
 * Copyright 2022 Charles Coughlin. All rights reserved.
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
class TextMessageAdapter(msgs: List<TextMessage>) : RecyclerView.Adapter<LogViewHolder?>() {
    private val dateFormatter = SimpleDateFormat("HH:mm:ss.SSS")
    private var expandedPosition = -1
    private lateinit var recyclerView: RecyclerView
    private var messages: List<TextMessage> = msgs

     override fun onAttachedToRecyclerView(view: RecyclerView) {
        recyclerView = view
    }

    /**
     * Create a new view holder. Inflate the row layout, set the item height.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val inflater: LayoutInflater = LayoutInflater.from(parent.getContext())
        val shouldAttachToParent = false
        val layout: LinearLayout =
            inflater.inflate(R.layout.log_item, parent, shouldAttachToParent) as LinearLayout
        return LogViewHolder(layout)
    }

    /**
     * Set the contents of the holder appropriate to the position visible.
     * Change the views depending on whether or not the item is selected.
     * In an expanded view the message text is on its own line. We add date and source.
     * @param holder the viewholder that should be populated at the given position
     * @param position row that should be updated
     */
    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        Log.i(CLSS, String.format("onBindViewHolder at %d of %d", position, messages.size))
        val expand = position == expandedPosition
        val msg: TextMessage = messages[position]
        if (msg == null) {
            Log.w(CLSS, String.format("Null log holder at %d", position))
            return
        }
        val type: MessageType = msg.messageType
        // The timestamp is always the same
        val timestampView: TextView = holder.timestampView
        val tstamp: Date = msg.getTimestamp()
        val dt = dateFormatter.format(tstamp)
        timestampView.setText(dt)

        // In expanded mode the source is the type
        val sourceView: TextView = holder.sourceView
        var source: String = msg.messageType.name
        if (expand) {
            sourceView.setText(source)
            if (type == MessageType.ANS) {
                sourceView.setTextColor(Color.BLUE)
            }
        } else {
            // Truncate source to 16 char
            if (source.length > SOURCE_LEN) source = source.substring(0, SOURCE_LEN)
            sourceView.setText(source)
        }

        // In expanded mode, the message is the source (node-name).
        val messageView: TextView = holder.messageView
        var msgText: String = msg.message.trim { it <= ' ' }
        if (expand) {
            messageView.setText(source)
        } else {
            if (msgText.length > MESSAGE_LEN) msgText = msgText.substring(0, MESSAGE_LEN)
            messageView.setText(msgText)
            if (type == MessageType.ANS) {
                messageView.setTextColor(Color.BLUE)
            }
        }
        val detailView: TextView = holder.detailView
        val params: ViewGroup.LayoutParams = holder.itemView.getLayoutParams()
        if (expand) {
            detailView.setText(msgText)
            detailView.setVisibility(View.VISIBLE)
            holder.itemView.setActivated(false)
            params.height = LOG_MSG_HEIGHT_EXPANDED
        } else {
            detailView.setVisibility(View.GONE)
            holder.itemView.setActivated(true)
            params.height = LOG_MSG_HEIGHT
        }
        holder.itemView.setLayoutParams(params)
        holder.itemView.setOnClickListener(View.OnClickListener {
            expandedPosition = if (expand) -1 else position
            TransitionManager.beginDelayedTransition(recyclerView)
            notifyDataSetChanged()
        })
    }

    // It is important that the widget and backing manager be in synch
    // with respect to item count.
    override fun getItemCount(): Int {
        return messages.size
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        //
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
            messages = msgs!!
        }
        notifyDataSetChanged()
    }

    companion object {
        private val CLSS = TextMessageAdapter::class.java.simpleName
        private const val MESSAGE_LEN = 45
        private const val SOURCE_LEN = 15
        private const val LOG_MSG_HEIGHT = 75
        private const val LOG_MSG_HEIGHT_EXPANDED = 225
    }

    /**
     * Adapter between the recycler and data source for log messages.
     */
    init {
        messages = msgs
    }
}
