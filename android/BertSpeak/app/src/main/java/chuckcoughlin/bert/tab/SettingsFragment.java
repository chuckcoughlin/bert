/**
 * Copyright 2019 Charles Coughlin. All rights reserved.
 *  (MIT License)
 */

package chuckcoughlin.bert.tab;

import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.util.List;

import chuckcoughlin.bert.R;
import chuckcoughlin.bert.db.BertDbManager;
import chuckcoughlin.bert.common.NameValue;

/**
 * Display the current values of global application settings and allow
 * editing.
 */
public class SettingsFragment extends BasicAssistantListFragment  {
    private final static String CLSS = "SettingsFragment";

    public SettingsFragment() {
        super();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        List<NameValue> nvpairs = BertDbManager.getInstance().getSettings();
        NameValue [] nvarray = nvpairs.toArray(new NameValue[nvpairs.size()]);
        Log.i(CLSS,String.format("onActivityCreated: will display %d name-values",nvarray.length));
        SettingsListAdapter adapter = new SettingsListAdapter(getContext(),nvarray);
        setListAdapter(adapter);
        getListView().setItemsCanFocus(true);
    }

    // Called to have the fragment instantiate its user interface view.
    // Inflate the view for the fragment based on layout XML. Populate
    // the text fields from the database.
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View contentView = inflater.inflate(R.layout.fragment_settings, container, false);
        TextView textView = contentView.findViewById(R.id.fragmentSettingsText);
        textView.setText(R.string.fragmentSettingsLabel);
        return contentView;
    }


    public class SettingsListAdapter extends ArrayAdapter<NameValue> implements ListAdapter {

        public SettingsListAdapter(Context context, NameValue[] values) {
            super(context,R.layout.settings_item, values);
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).hashCode();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Get the data item for this position
            NameValue nv = getItem(position);
            // Check if an existing view is being reused, otherwise inflate the view
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.settings_item, parent, false);
            }
            // Lookup view for data population
            TextView nameView = convertView.findViewById(R.id.settingsNameView);
            EditText editText = convertView.findViewById(R.id.settingsEditView);
            // Populate the data into the template view using the data object
            //Log.i(CLSS,String.format("adapter.getView setting %s = %s",nv.getName(),nv.getValue()));
            nameView.setText(nv.getName());
            editText.setText(nv.getValue());
            editText.setHint(nv.getHint());
            if(nv.getName().toUpperCase().contains("PASSWORD")) {
                editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                editText.setSelection(editText.getText().length());
            }
            else {
                editText.setInputType(InputType.TYPE_CLASS_TEXT);
            }
            editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    /*
                     * When focus is lost save the entered value both into the current array
                     * and the databasesetInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
                     */
                    if (!hasFocus) {
                        Log.i(CLSS,String.format("SettingsListAdapter.getView.onFocusChange %d = %s",position,((EditText) v).getText().toString()));
                        nv.setValue(((EditText)v).getText().toString());
                        BertDbManager.getInstance().updateSetting(nv);
                    }
                }
            });

            //Log.i(CLSS,String.format("SettingsListAdapter.getView set %s = %s",nv.getName(),nv.getValue()));
            // Return the completed view to render on screen
            return convertView;
        }
    }

}
