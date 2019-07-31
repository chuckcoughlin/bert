/**
 * Copyright 2017 Charles Coughlin. All rights reserved.
 *  (MIT License)
 */
package chuckcoughlin.bert.logs;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.support.transition.TransitionManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import chuckcoughlin.bert.R;
import chuckcoughlin.bert.common.MessageType;
import chuckcoughlin.bert.speech.TextMessage;


/**
 * This class is a link between a RecyclerView and the data backstop.
 * Each element in the list is a string, a text message.
 */

public class TextMessageAdapter extends RecyclerView.Adapter<LogViewHolder> {
    private static final String CLSS = TextMessageAdapter.class.getSimpleName();
    private static final int MESSAGE_LEN = 45;
    private static final int SOURCE_LEN = 15;
    private static final int LOG_MSG_HEIGHT = 75;
    private static final int LOG_MSG_HEIGHT_EXPANDED = 225;
    private SimpleDateFormat dateFormatter = new SimpleDateFormat("HH:mm:ss.SSS");
    private int expandedPosition = -1;
    private RecyclerView recyclerView = null;
    private final List<TextMessage> messages;

    /**
     * Adapter between the recycler and data source for log messages.
     */
    public TextMessageAdapter(List<TextMessage> msgs) {
        this.messages = msgs;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView view) {
        this.recyclerView = view;
    }

    /**
     * Create a new view holder. Inflate the row layout, set the item height.
     */
    @Override
    public LogViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        boolean shouldAttachToParent = false;

        LinearLayout layout = (LinearLayout)inflater.inflate(R.layout.log_item,parent,shouldAttachToParent);
        LogViewHolder holder = new LogViewHolder(layout);
        return holder;
    }

    /**
     * Set the contents of the holder appropriate to the position visible.
     * Change the views depending on whether or not the item is selected.
     * In an expanded view the message text is on its own line. We add date and source.
     * @param holder the viewholder that should be populated at the given position
     * @param position row that should be updated
     */
    @Override
    public void onBindViewHolder(LogViewHolder holder, int position) {
        Log.i(CLSS,String.format("onBindViewHolder at %d of %d",position,messages.size()));
        boolean expand = (position==expandedPosition);
        TextMessage msg = messages.get(position);
        if( msg==null ) {
            Log.w(CLSS,String.format("Null log holder at %d",position));
            return;
        }

        MessageType type = msg.getMessageType();
        // The timestamp is always the same
        TextView timestampView  = holder.getTimestampView();
        Date tstamp = msg.getTimestamp();
        String dt = dateFormatter.format(tstamp);
        timestampView.setText(dt);

        // In expanded mode the source is the type
        TextView sourceView  = holder.getSourceView();
        String source = msg.getMessageType().name();
        if( expand ) {
            sourceView.setText(source);
            if( type.equals(MessageType.ANS)) {
                sourceView.setTextColor(Color.BLUE);
            }
        }
        else {
            // Truncate source to 16 char
            if( source.length()>SOURCE_LEN) source = source.substring(0,SOURCE_LEN);
            sourceView.setText(source);
        }

        // In expanded mode, the message is the source (node-name).
        TextView messageView  = holder.getMessageView();
        String msgText = msg.getMessage().trim();
        if( expand ) {
            messageView.setText(source);
        }
        else {
            if( msgText.length()>MESSAGE_LEN) msgText = msgText.substring(0,MESSAGE_LEN);
            messageView.setText(msgText);
            if( type.equals(MessageType.ANS)) {
                messageView.setTextColor(Color.BLUE);
            }
        }

        TextView detailView = holder.getDetailView();
        ViewGroup.LayoutParams params = holder.itemView.getLayoutParams();
        if( expand ) {
            detailView.setText(msgText);
            detailView.setVisibility(View.VISIBLE);
            holder.itemView.setActivated(false);
            params.height=LOG_MSG_HEIGHT_EXPANDED;
        }
        else {
            detailView.setVisibility(View.GONE);
            holder.itemView.setActivated(true);
            params.height=LOG_MSG_HEIGHT;
        }
        holder.itemView.setLayoutParams(params);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                expandedPosition = expand ? -1:position;
                TransitionManager.beginDelayedTransition(recyclerView);
                notifyDataSetChanged();
            }
        });
    }


    // It is important that the widget and backing manager be in synch
    // with respect to item count.
    @Override
    public int getItemCount() {return messages.size(); }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        this.recyclerView = null;
    }
}