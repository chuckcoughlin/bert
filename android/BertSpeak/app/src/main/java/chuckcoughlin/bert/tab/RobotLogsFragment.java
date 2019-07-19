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
import chuckcoughlin.bert.logs.LogRecyclerAdapter;
import chuckcoughlin.bert.logs.LogViewer;
import chuckcoughlin.bert.service.DispatchService;
import chuckcoughlin.bert.service.DispatchServiceBinder;
import chuckcoughlin.bert.service.TextManager;
import chuckcoughlin.bert.speech.TextMessage;
import chuckcoughlin.bert.speech.TextMessageObserver;


/**
 * This fragment shows log messages originating in the robot.
 */
public class RobotLogsFragment extends BasicAssistantFragment implements LogViewer, ServiceConnection, TextMessageObserver {
    private final static String CLSS = "RobotLogsFragment";
    private LogRecyclerAdapter adapter;
    private View rootView = null;
    private RecyclerView logMessageView;
    private TextView logView;
    private DispatchService service = null;
    private TextManager textManager = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_robot_logs, container, false);
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
        getContext().getApplicationContext().bindService(intent, this, Context.BIND_AUTO_CREATE);
    }
    @Override
    public void onResume() {
        super.onResume();
        if( service!=null ) {
            Log.i(CLSS,"onResume: registering as observer");
            service.registerLogViewer(this);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if( service!=null ) {
            Log.i(CLSS,"onPause: unregistering as observer");
            service.unregisterLogViewer(this);
            textManager = null;
        }
    }
    @Override
    public void onStop() {
        super.onStop();
        getContext().getApplicationContext().unbindService(this);
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
        if( service!=null ) msg = textManager.getLogAtPosition(position);
        return msg;
    }
    public List<TextMessage> getLogs() {
        List<TextMessage> logs = new ArrayList<>();
        if( textManager!=null ) logs = textManager.getLogs();
        return logs;
    }
    // =================================== ServiceConnection ===============================
    @Override
    public void onServiceDisconnected(ComponentName name) {
        if( service!=null ) service.unregisterLogViewer(this);
        service = null;
        textManager = null;
    }

    // name.getClassName() contains the class of the service.
    @Override
    public void onServiceConnected(ComponentName name, IBinder bndr) {
        DispatchServiceBinder binder = (DispatchServiceBinder) bndr;
        service = binder.getService();
        service.registerLogViewer(this);
    }
    // =================================== TextMessageObserver ===============================
    @Override
    public void initialize(TextManager mgr) {
        textManager = mgr;
    }
    @Override
    public void update(TextMessage msg) {
        String text = msg.getMessage();
        adapter.update(msg);
    }
}
