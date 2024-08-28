/**
 * Copyright 2024 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.ui.adapter

import android.content.Context
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View.OnFocusChangeListener
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListAdapter
import android.widget.TextView
import chuckcoughlin.bertspeak.R
import chuckcoughlin.bertspeak.common.NameValue
import chuckcoughlin.bertspeak.db.DatabaseManager
import java.util.Locale

/**
 * This is an adapter for a list of simple text messages.
 */
class TextListAdapter(context: Context, values: Array<String>) :
    ArrayAdapter<String>( context, R.layout.text_item, values),ListAdapter   {

    override fun getItemId(position: Int): Long {
        //Log.i(CLSS, String.format("getItemId: %d",position))
        return getItem(position).hashCode().toLong()
    }

    override fun getView(position: Int, view: View?, parent: ViewGroup): View {
        // Get the data item for this position
        var convertView = view
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView =LayoutInflater.from(context).inflate(R.layout.text_item,
                parent, false)
        }
        // Lookup view for data population
        val textView = convertView!!.findViewById<TextView>(R.id.textView)
        textView.text = getItem(position)
        Log.i(CLSS, String.format("getView: text = %s",getItem(position)))

        // Return the completed view to render on screen
        return convertView
    }

    private val CLSS = "TextListAdapter"
}
