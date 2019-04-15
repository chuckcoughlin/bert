/**
 * Copyright 2019 Charles Coughlin. All rights reserved.
 *  (MIT License)
 */

package chuckcoughlin.bert.tab;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import chuckcoughlin.bert.R;
import chuckcoughlin.bert.logs.LogRecyclerAdapter;
import chuckcoughlin.bert.speech.SpokenTextManager;


/**
 * This fragment shows messages logged by the dispatcher application in the robot..
 */

public class DispatcherLogsFragment extends BasicAssistantFragment  {
    private final static String CLSS = "LogFragment";
    private RecyclerView.LayoutManager layoutManager;
    private LogRecyclerAdapter adapter;
    private View rootView = null;
    private RecyclerView logMessageView;
    private TextView logView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_dispatcher_logs, container, false);
        TextView textView = rootView.findViewById(R.id.fragmentLogsText);
        textView.setText(R.string.fragmentLogsLabel);

        logMessageView = rootView.findViewById(R.id.logs_recycler_view);
        logMessageView.setHasFixedSize(true);   // Refers to the size of the layout.
        LinearLayoutManager layoutManager = new LinearLayoutManager(logMessageView.getContext());
        logMessageView.setLayoutManager(layoutManager);
        adapter = new LogRecyclerAdapter();
        logMessageView.setAdapter(adapter);
        int scrollPosition = layoutManager.findFirstCompletelyVisibleItemPosition();
        logMessageView.scrollToPosition(scrollPosition);

        Button button = rootView.findViewById(R.id.clearButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearButtonClicked();
            }
        });
        button = rootView.findViewById(R.id.freezeButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                freezeButtonClicked();
            }
        });
        updateUI();

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(CLSS,"onResume: registering adapter as observer");
        SpokenTextManager.getInstance().register(adapter);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i(CLSS,"onPause: unregistering adapter as observer");
        SpokenTextManager.getInstance().unregister(adapter);
    }

    @Override
    public void onDestroyView() {
        Log.i(CLSS, "onDestroyView");
        super.onDestroyView();
    }

    //======================================== Button Callbacks ======================================
    //
    public void clearButtonClicked() {
        Log.i(CLSS, "Clear button clicked");
        SpokenTextManager.getInstance().clear();
    }

    /**
     * The Freeze button has purely local control.
     */
    public void freezeButtonClicked() {
        boolean frozen = adapter.isFrozen();
        adapter.setFrozen(!frozen);
        updateUI();
    }

    private void updateUI() {
        Button button = rootView.findViewById(R.id.freezeButton);
        if( adapter.isFrozen() ) {
            button.setText(R.string.logButtonThaw);
        }
        else {
            button.setText(R.string.logButtonFreeze);
        }
    }

}
