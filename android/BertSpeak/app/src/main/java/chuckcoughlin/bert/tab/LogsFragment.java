/**
 * Copyright 2019 Charles Coughlin. All rights reserved.
 *  (MIT License)
 */

package chuckcoughlin.bert.tab;

import android.content.Intent;
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
import chuckcoughlin.bert.logs.BertLogManager;
import chuckcoughlin.bert.service.BroadcastObserver;
import chuckcoughlin.bert.service.VoiceConstants;


/**
 * This fragment allows perusal of the robot's spoken interactions..
 */

public class LogsFragment extends BasicAssistantFragment implements BroadcastObserver {
    private final static String CLSS = "LogFragment";
    private RecyclerView.LayoutManager layoutManager;
    private LogRecyclerAdapter adapter;
    private View rootView = null;
    private RecyclerView logMessageView;
    private TextView logView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_logs, container, false);
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

        Button button = (Button) rootView.findViewById(R.id.clearButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearButtonClicked();
            }
        });
        BertLogManager.getInstance().freeze();
        button = (Button) rootView.findViewById(R.id.freezeButton);
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
    public void onDestroyView() {
        Log.i(CLSS, "onDestroyView");
        BertLogManager.getInstance().removeObserver(adapter);
        super.onDestroyView();
    }

    //======================================== Button Callbacks ======================================
    //
    public void clearButtonClicked() {
        Log.i(CLSS, "Clear button clicked");
        BertLogManager.getInstance().clear();
    }

    public void freezeButtonClicked() {
        BertLogManager logManager = BertLogManager.getInstance();
        if( logManager.isFrozen() ) {
            logManager.resume();
        }
        else {
            logManager.freeze();
        }
        updateUI();
    }

    private void updateUI() {
        Button button = (Button) rootView.findViewById(R.id.freezeButton);
        if( BertLogManager.getInstance().isFrozen() ) {
            button.setText(R.string.logButtonThaw);
        }
        else {
            button.setText(R.string.logButtonFreeze);
        }
    }

    // ===================== BroadcastObserver =====================
    @Override
    public void broadcastReceived(Intent intent) {
        if( intent.hasCategory(VoiceConstants.CATEGORY_SPOKEN_TEXT)) {

        }
    }
}
