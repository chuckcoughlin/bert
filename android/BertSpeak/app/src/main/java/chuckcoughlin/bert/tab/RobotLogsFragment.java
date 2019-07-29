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
    private TextView logView;
    private DispatchService service = null;
    private TextManager textManager = null;
    private boolean frozen = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if( savedInstanceState!=null ) frozen = savedInstanceState.getBoolean(BertConstants.BUNDLE_FROZEN,false);
        rootView = inflater.inflate(R.layout.fragment_robot_logs, container, false);
        logMessageView = rootView.findViewById(R.id.logs_recycler_view);
        logMessageView.setHasFixedSize(true);   // Refers to the size of the layout.
        LinearLayoutManager layoutManager = new LinearLayoutManager(logMessageView.getContext());
        logMessageView.setLayoutManager(layoutManager);
        adapter = new TextMessageAdapter(new FixedSizeList<>(BertConstants.NUM_LOG_MESSAGES));
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

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if( savedInstanceState!=null) this.frozen = savedInstanceState.getBoolean(BertConstants.BUNDLE_FROZEN);
    }

    //======================================== Button Callbacks ======================================
    //
    public void clearButtonClicked() {
        Log.i(CLSS, "Clear button clicked");
        textManager.getLogs().clear();
        adapter.notifyDataSetChanged();
    }

    /**
     * The Freeze button has purely local control.
     */
    public void freezeButtonClicked() {
        frozen = !frozen;
        if( !frozen ) {
            initialize(textManager);
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
        if( service!=null ) service.unregisterLogViewer(this);
        service = null;
        textManager = null;
    }

    // name.getClassName() contains the class of the service.
    @Override
    public void onServiceConnected(ComponentName name, IBinder bndr) {
        DispatchServiceBinder binder = (DispatchServiceBinder) bndr;
        service = binder.getService();
        //service.registerLogViewer(this);
    }
    // =================================== TextMessageObserver ===============================
    @Override
    public String getName() { return CLSS; }
    @Override
    public void initialize(TextManager mgr) {
        textManager = mgr;
        adapter.initialize(textManager.getLogs());
        Log.i(CLSS,String.format("initialize: message list is now ..."));
        for(TextMessage m:mgr.getLogs()) {
            Log.i(CLSS,String.format("initialize: \t%s",m.getMessage()));
        }
        adapter.notifyDataSetChanged();
    }
    @Override
    public void update(TextMessage msg) {
        Log.i(CLSS,String.format("update: message = %s",msg.getMessage()));
        if( !frozen ) {
            try {
                adapter.notifyItemInserted(0);
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        logMessageView.scrollToPosition(0);
                    }
                });
            } catch (IllegalStateException ignore) {}
        }
    }
}