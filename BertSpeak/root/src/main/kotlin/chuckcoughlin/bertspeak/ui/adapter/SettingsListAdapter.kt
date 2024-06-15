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
 * This is a specialized adapter for entries in the database settings table.
 */
class SettingsListAdapter(context: Context, values: Array<NameValue>) :
    ArrayAdapter<NameValue>( context, R.layout.settings_item, values),ListAdapter   {

    override fun getItemId(position: Int): Long {
        Log.i(CLSS, String.format("getItemId: %d",position))
        return getItem(position).hashCode().toLong()
    }

    override fun getView(position: Int, view: View?, parent: ViewGroup): View {
        // Get the data item for this position
        var convertView = view
        val nv = getItem(position)
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView =LayoutInflater.from(context).inflate(R.layout.settings_item,
                parent, false)
        }
        // Lookup view for data population
        val nameView = convertView!!.findViewById<TextView>(R.id.settingsNameView)
        val editText = convertView.findViewById<EditText>(R.id.settingsEditView)
        assert(nv != null)
        nameView.text = nv!!.name
        editText.setText(nv.value)
        Log.i(CLSS, String.format("getView: editText %s = %s",nv.name,nv.value))
        editText.hint = nv.hint
        if (nv.name.uppercase(Locale.getDefault()).contains("PASSWORD")) {
            editText.inputType =
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            editText.setSelection(editText.text.length)
        }
        else if (nv.name.uppercase(Locale.getDefault()).contains("VOLUME")) {
            editText.inputType = InputType.TYPE_CLASS_NUMBER
        }
        else {
            editText.inputType = InputType.TYPE_CLASS_TEXT
        }
        editText.onFocusChangeListener = OnFocusChangeListener { v, hasFocus ->
            /*
             * When focus is lost save the entered value both into the current array
             * and database.
             */
            if (!hasFocus) {
                Log.i(CLSS,String.format("SettingsListAdapter.getView.onFocusChange %d = %s",position,(v as EditText).text.toString()))
                nv.value = v.text.toString()
                DatabaseManager.updateSetting(nv)
            }
        }

        //Log.i(CLSS,String.format("SettingsListAdapter.getView set %s = %s",nv.getName(),nv.getValue()));
        // Return the completed view to render on screen
        return convertView
    }

    private val CLSS = "SettingsListAdapter"
}
