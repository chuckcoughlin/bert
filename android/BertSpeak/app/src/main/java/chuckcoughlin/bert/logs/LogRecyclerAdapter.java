/**
 * Copyright 2017 Charles Coughlin. All rights reserved.
 *  (MIT License)
 */
package chuckcoughlin.bert.logs;

import android.app.Activity;
import android.content.Context;
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
import chuckcoughlin.bert.speech.SpokenTextManager;
import chuckcoughlin.bert.speech.TextMessage;
import chuckcoughlin.bert.speech.TextMessageObserver;


/**
 * This a link between a RecyclerView and the data backstop.
 * Each element in the list is a string, a text message.
 *
 * Registration with the SpokenTextManager is handled by the parent process.
 */

public class LogRecyclerAdapter extends RecyclerView.Adapter<LogViewHolder> implements TextMessageObserver {
    private static final String CLSS = LogRecyclerAdapter.class.getSimpleName();
    private static final int MESSAGE_LEN = 45;
    private static final int SOURCE_LEN = 15;
    private static final int LOG_MSG_HEIGHT = 75;
    private static final int LOG_MSG_HEIGHT_EXPANDED = 225;
    private SimpleDateFormat dateFormatter = new SimpleDateFormat("HH:mm:ss.SSS");
    private int expandedPosition = -1;
    private final LogViewer viewer;
    private RecyclerView recyclerView = null;
    private Context context = null;
    private boolean frozen;

    /**
     * Adapter between the recycler and data source for log messages
     */
    public LogRecyclerAdapter(LogViewer v) {
        this.viewer = v;
        this.frozen = false;
    }

    public boolean isFrozen() { return this.frozen; }
    public void setFrozen(boolean flag) { this.frozen = flag; }

    @Override
    public void onAttachedToRecyclerView(RecyclerView view) {
        this.recyclerView = view;
    }
    @Override
    public LogViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        boolean shouldAttachToParent = false;

        // create a new view - set the item height.
        LinearLayout layout = (LinearLayout)inflater.inflate(R.layout.log_item,parent,shouldAttachToParent);
        LogViewHolder holder = new LogViewHolder(layout);
        return holder;
    }

    /**
     * Change the views depending on whether or not the item is selected.
     * In an expanded view the message text is on its own line. We add date and source.
     * @param holder the viewholder that should be updated at the given position
     * @param position row that should be updated
     */
    @Override
    public void onBindViewHolder(LogViewHolder holder, int position) {
        Log.i(CLSS,String.format("onBindViewHolder at %d",position));
        boolean expand = (position==expandedPosition);
        TextMessage msg = viewer.getLogAtPosition(position);  // Checks index bounds
        if( msg==null ) {
            Log.w(CLSS,String.format("Null log holder at %d",position));
            return;
        }
        // The timestamp is always the same
        TextView timestampView  = holder.getTimestampView();
        Date tstamp = msg.getTimestamp();
        String dt = dateFormatter.format(tstamp);
        timestampView.setText(dt);

        // In expanded mode the source is the level
        TextView sourceView  = holder.getSourceView();
        String source = msg.getMessageType().name();
        if( expand ) {
            sourceView.setText(source);
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
    public int getItemCount() {return viewer.getLogs().size(); }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        this.recyclerView = null;
        this.context = null;
    }


    // ===================== TextMessageObserver =====================
    @Override
    /**
     * We have just registered as an observer. Now catch up.
     * @param list of messages being the most recent retained by the manager.
     */
    public void initialize(final List<TextMessage> list) {
        Log.i(CLSS,"initialze with message list");
        notifyDataSetChanged();
    }

    /**
     * A new message has arrived
     * @param msg the new message
     */
    public void update(final TextMessage msg) {
        Log.i(CLSS,String.format("new message: %s",msg.getMessage()));
        if( context!=null ) {
            Activity activity = (Activity)context;
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    synchronized(LogRecyclerAdapter.this) {
                        try {
                            final int size = getItemCount();
                            notifyItemRangeInserted(size - 1, 1);
                        }
                        catch(Exception ex) {
                            Log.w(CLSS,String.format("Exception adding to log (%s)",ex.getLocalizedMessage()));
                        }
                    }
                }
            });
        }
    }
}