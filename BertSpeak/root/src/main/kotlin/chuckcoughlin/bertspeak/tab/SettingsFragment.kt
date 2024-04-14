/**
 * Copyright 2022-2024 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.tab

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import chuckcoughlin.bertspeak.R
import chuckcoughlin.bertspeak.databinding.FragmentSettingsBinding
import chuckcoughlin.bertspeak.db.DatabaseManager
import chuckcoughlin.bertspeak.ui.adapter.SettingsListAdapter

/**
 * Display the current values of settings table and allow
 * editing.
 */
class SettingsFragment (pos:Int): BasicAssistantListFragment(pos) {
    val name : String

    // Called to have the fragment instantiate its user interface view.
    // Inflate the view for the fragment based on layout XML. Populate
    // the text fields from the database.
    override fun onCreateView(inflater: LayoutInflater,container: ViewGroup?,savedInstanceState: Bundle?): View {
        val nvpairs = DatabaseManager.getSettings()
        val nvarray = nvpairs.toTypedArray()
        val listAdapter = SettingsListAdapter(requireContext(), nvarray)
        Log.i(CLSS, String.format("onCreateView: will display %d name-values", listAdapter.count))
        setListAdapter(listAdapter)

        val binding = FragmentSettingsBinding.inflate(inflater,container,false)
        binding.fragmentSettingsText.setText(R.string.fragmentSettingsLabel)
        binding.list.itemsCanFocus = true
        return binding.root
    }



    private val CLSS = "SettingsFragment"

    init {
        name = CLSS
    }
}
