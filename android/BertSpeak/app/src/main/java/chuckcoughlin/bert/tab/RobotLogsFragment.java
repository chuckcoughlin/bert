/**
 * Copyright 2019 Charles Coughlin. All rights reserved.
 *  (MIT License)
 */

package chuckcoughlin.bert.tab;

import android.content.Context;
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

import java.util.ArrayList;
import java.util.List;

import chuckcoughlin.bert.R;
import chuckcoughlin.bert.logs.LogRecyclerAdapter;
import chuckcoughlin.bert.logs.LogViewer;
import chuckcoughlin.bert.service.DispatchService;
import chuckcoughlin.bert.service.DispatchServiceConnection;
import chuckcoughlin.bert.speech.SpokenTextManager;
import chuckcoughlin.bert.speech.TextMessage;


/**
 * This fragment shows log messages originating in the robot.
 */

public class RobotLogsFragment extends BasicAssistantFragment implements LogViewer {
    private final static String CLSS = "RobotLogsFragment";
    private DispatchServiceConnection serviceConnection = null;
    private LogRecyclerAdapter adapter;
    private View rootView = null;
    private RecyclerView logMessageView;
    private TextView logView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.serviceConnection = new DispatchServiceConnection();
        rootView = inflater.inflate(R.layout.fragment_dispatcher_logs, container, false);
        TextView textView = rootView.findViewById(R.id.fragmentLogsText);
        textView.setText(R.string.fragmentLogsLabel);

        logMessageView = rootView.findViewById(R.id.logs_recycler_view);
        logMessageView.setHasFixedSize(true);   // Refers to the size of the layout.
        LinearLayoutManager layoutManager = new LinearLayoutManager(logMessageView.getContext());
        logMessageView.setLayoutManager(layoutManager);
        adapter = new LogRecyclerAdapter(this);
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
    // Bind to the DispatchService
    @Override
    public void onStart() {
        super.onStart();
        Intent intent = new Intent(getContext().getApplicationContext(), DispatchService.class);
        getContext().getApplicationContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }
    @Override
    public void onResume() {
        super.onResume();
        Log.i(CLSS,"onResume: registering adapter as observer");
        if( serviceConnection.isBound() ) serviceConnection.getService().registerTextObserver(adapter);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i(CLSS,"onPause: unregistering adapter as observer");
        if( serviceConnection.isBound() ) serviceConnection.getService().unregisterTextObserver(adapter);
    }
    @Override
    public void onStop() {
        super.onStop();
        getContext().getApplicationContext().unbindService(serviceConnection);
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
    //======================================== LogViewer ======================================
    public TextMessage getLogAtPosition(int position) {
        TextMessage msg = null;
        if( serviceConnection.isBound() ) msg = serviceConnection.getService().getLogAtPosition(position);
        return msg;
    }
    public List<TextMessage> getLogs() {
        List<TextMessage> logs = new ArrayList<>();
        if( serviceConnection.isBound() ) logs = serviceConnection.getService().getLogs();
        return logs;
    }
}
