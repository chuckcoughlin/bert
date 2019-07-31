/**
 * Copyright 2017 Charles Coughlin. All rights reserved.
 *  (MIT License)
 */
package chuckcoughlin.bert.logs;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import chuckcoughlin.bert.R;

/**
 * Facilitates data transfer by the Recycler view.
 */
public class LogViewHolder extends RecyclerView.ViewHolder {
    private final ViewGroup rowParent;
    /**
     * The ViewGroup is actually a nested LinearLayout holding text views
     * See log_item.xml
     *    - timestamp
     *    - source
     *    - message
     * @param v the current view
     */
    public LogViewHolder(ViewGroup v) {
        super(v);
        this.setIsRecyclable(false);
        this.rowParent = (ViewGroup) ((ViewGroup)itemView).getChildAt(0);
    }
    public TextView getTimestampView(){ return (TextView) rowParent.getChildAt(0); }
    public TextView getSourceView()   { return (TextView) rowParent.getChildAt(1); }
    public TextView getMessageView()  { return (TextView) rowParent.getChildAt(2); }
    public TextView getDetailView()   { return (TextView) ((ViewGroup)itemView).getChildAt(1);  }
}
