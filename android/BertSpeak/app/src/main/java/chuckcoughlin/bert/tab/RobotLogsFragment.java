/**
 * Copyright 2019 Charles Coughlin. All rights reserved.
 *  (MIT License)
 */

package chuckcoughlin.bert.tab;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
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
import chuckcoughlin.bert.common.BertConstants;
import chuckcoughlin.bert.common.FixedSizeList;
import chuckcoughlin.bert.logs.TextMessageAdapter;
import chuckcoughlin.bert.service.DispatchService;
import chuckcoughlin.bert.service.DispatchServiceBinder;
import chuckcoughlin.bert.service.TextManager;
import chuckcoughlin.bert.speech.TextMessage;
import chuckcoughlin.bert.speech.TextMessageObserver;


/**
 * This fragment shows log messages originating in the robot.
 */
public class RobotLogsFragment extends BasicAssistantFragment implements ServiceConnection, TextMessageObserver {
    private final static String CLSS = "RobotLogsFragment";
    private TextMessageAdapter adapter;
    private View rootView = null;
    private RecyclerView logMessageView;
    private DispatchService service = null;
    private boolean frozen = false;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if( savedInstanceState!=null ) frozen = savedInstanceState.getBoolean(BertConstants.BUNDLE_FROZEN,false);
        adapter = new TextMessageAdapter(new FixedSizeList<>(BertConstants.NUM_LOG_MESSAGES));
        rootView = inflater.inflate(R.layout.fragment_robot_logs, container, false);
        logMessageView = rootView.findViewById(R.id.logs_recycler_view);
        logMessageView.setHasFixedSize(true);   // Refers to the size of the layout.
        LinearLayoutManager layoutManager = new LinearLayoutManager(logMessageView.getContext());
        logMessageView.setLayoutManager(layoutManager);
        logMessageView.setAdapter(adapter);
        int scrollPosition = layoutManager.findFirstCompletelyVisibleItemPosition();
        logMessageView.scrollToPosition(scrollPosition);


        Button button = rootView.findViewById(R.id.logClearButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearButtonClicked();
            }
        });
        button = rootView.findViewById(R.id.logFreezeButton);
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
        getContext().getApplicationContext().bindService(intent, this, Context.BIND_AUTO_CREATE);
    }
    @Override
    public void onResume() {
        super.onResume();
        if( service!=null ) {
            Log.i(CLSS,"onResume: registering as observer");
            service.getTextManager().registerLogViewer(this);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if( service!=null ) {
            Log.i(CLSS,"onPause: unregistering as observer");
            service.getTextManager().unregisterLogViewer(this);
        }
    }
    @Override
    public void onStop() {
        super.onStop();
        if( getContext()!=null ) getContext().getApplicationContext().unbindService(this);
    }
    @Override
    public void onDestroyView() {
        Log.i(CLSS, "onDestroyView");
        super.onDestroyView();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(BertConstants.BUNDLE_FROZEN,frozen);
    }

    //======================================== Button Callbacks ======================================
    //
    public void clearButtonClicked() {
        Log.i(CLSS, "Clear button clicked");
        if( service!=null ) {
            service.getTextManager().getLogs().clear();
            adapter.notifyDataSetChanged();
        }
    }

    public void freezeButtonClicked() {
        frozen = !frozen;
        if( service!=null ) {
            if( !frozen ) {
                adapter.notifyDataSetChanged();
            }
        }
        updateUI();
    }

    private void updateUI() {
        Button button = rootView.findViewById(R.id.logFreezeButton);
        if( frozen ) {
            button.setText(R.string.buttonThaw);
        }
        else {
            button.setText(R.string.buttonFreeze);
        }
    }
    // =================================== ServiceConnection ===============================
    @Override
    public void onServiceDisconnected(ComponentName name) {
        if( service!=null ) service.getTextManager().unregisterLogViewer(this);
        service = null;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder bndr) {
        DispatchServiceBinder binder = (DispatchServiceBinder) bndr;
        service = binder.getService();
        adapter.resetList(service.getTextManager().getLogs());
        service.getTextManager().registerLogViewer(this);
    }
    // =================================== TextMessageObserver ===============================
    @Override
    public String getName() { return CLSS; }
    @Override
    public void initialize() {
        Log.i(CLSS,"initialize: message list is now ...");
        for(TextMessage m:service.getTextManager().getLogs()) {
            Log.i(CLSS,String.format("initialize: \t%s",m.getMessage()));
        }
        adapter.notifyDataSetChanged();
    }
    @Override
    public void update(TextMessage msg) {
        Log.i(CLSS,String.format("update: message = %s",msg.getMessage()));
        if( !frozen ) {
            adapter.notifyItemInserted(0);
            logMessageView.scrollToPosition(0);
        }
    }
}