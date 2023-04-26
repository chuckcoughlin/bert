/**
 * Copyright 2022 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.tab

import chuckcoughlin.bertspeak.db.DatabaseManager
import android.view.LayoutInflater
import android.view.ViewGroup
import android.os.Bundle
import chuckcoughlin.bertspeak.R
import android.text.InputType
import android.view.View.OnFocusChangeListener
import android.content.*
import android.util.Log
import android.view.View
import android.widget.*
import chuckcoughlin.bertspeak.common.*
import chuckcoughlin.bertspeak.databinding.FragmentCoverBinding
import chuckcoughlin.bertspeak.databinding.FragmentSettingsBinding
import java.util.*

/**
 * Display the current values of global application settings and allow
 * editing.
 */
class SettingsFragment (pageNumber:Int): BasicAssistantListFragment(pageNumber) {
    val name : String

    // This property is only valid between onCreateView and onDestroyView
    private lateinit var binding: FragmentSettingsBinding
    private lateinit var dbManager: DatabaseManager

    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        dbManager = DatabaseManager(requireContext())
        val nvpairs = dbManager.getSettings()
        val nvarray = nvpairs.toTypedArray()
        Log.i(CLSS, String.format("onActivityCreated: will display %d name-values", nvarray.size))
        val adapter = SettingsListAdapter(requireContext(), nvarray)
        setListAdapter(adapter)
        getListView().setItemsCanFocus(true)
    }

    // Called to have the fragment instantiate its user interface view.
    // Inflate the view for the fragment based on layout XML. Populate
    // the text fields from the database.
    override fun onCreateView(inflater: LayoutInflater,container: ViewGroup?,savedInstanceState: Bundle?): View {
        binding = FragmentSettingsBinding.inflate(inflater,container,false)
        binding.fragmentSettingsText.setText(R.string.fragmentSettingsLabel)
        return binding.root
    }

    inner class SettingsListAdapter(context: Context, values: Array<NameValue>) :
        ArrayAdapter<NameValue>( context, R.layout.settings_item, values), ListAdapter {

        override fun getItemId(position: Int): Long {
            return getItem(position).hashCode().toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            // Get the data item for this position
            var convertView = convertView
            val nv = getItem(position)
            // Check if an existing view is being reused, otherwise inflate the view
            if (convertView == null) {
                convertView =
                    LayoutInflater.from(context).inflate(R.layout.settings_item, parent, false)
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
                     * and the databasesetInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
                     */
                if (!hasFocus) {
                    Log.i(CLSS,String.format("SettingsListAdapter.getView.onFocusChange %d = %s",position,
                            (v as EditText).text.toString()))
                    nv.value = v.text.toString()
                    dbManager!!.updateSetting(nv)
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
    init {
        this.name = CLSS
    }
}
