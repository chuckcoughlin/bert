/**
 * Copyright 2022-2023 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.tab

import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.TextView
import chuckcoughlin.bertspeak.R
import chuckcoughlin.bertspeak.common.NameValue
import chuckcoughlin.bertspeak.databinding.FragmentSettingsBinding
import chuckcoughlin.bertspeak.db.DatabaseManager
import java.util.Locale

/**
 * Display the current values of settings table and allow
 * editing.
 */
class SettingsFragment (pos:Int): BasicAssistantListFragment(pos) {
    val name : String = CLSS

    // This property is only valid between onCreateView and onDestroyView
    private lateinit var binding: FragmentSettingsBinding

    // Called to have the fragment instantiate its user interface view.
    // Inflate the view for the fragment based on layout XML. Populate
    // the text fields from the database.
    override fun onCreateView(inflater: LayoutInflater,container: ViewGroup?,savedInstanceState: Bundle?): View {
        val nvpairs = DatabaseManager.getSettings()
        val nvarray = nvpairs.toTypedArray()
        Log.i(CLSS, String.format("onCreateView: will display %d name-values", nvarray.size))
        val adapter = SettingsListAdapter(requireContext(), nvarray)
        listAdapter = adapter
        listView.itemsCanFocus = true
        binding = FragmentSettingsBinding.inflate(inflater,container,false)
        binding.fragmentSettingsText.setText(R.string.fragmentSettingsLabel)
        return binding.root
    }

    inner class SettingsListAdapter(context: Context, values: Array<NameValue>) :
        ArrayAdapter<NameValue>( context, R.layout.settings_item, values)   {

        override fun getItemId(position: Int): Long {
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
            editText.hint = nv.hint
            if (nv.name.uppercase(Locale.getDefault()).contains("PASSWORD")) {
                editText.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                editText.setSelection(editText.text.length)
            } else {
                editText.inputType = InputType.TYPE_CLASS_TEXT
            }
            editText.onFocusChangeListener = OnFocusChangeListener { v, hasFocus -> /*
                     * When focus is lost save the entered value both into the current array
                     * and database.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
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
    }

    companion object {
        private const val CLSS = "SettingsFragment"
    }
}
