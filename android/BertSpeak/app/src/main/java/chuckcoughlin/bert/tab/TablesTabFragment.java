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
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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
 * This fragment displays data from the robot in tabular form. Only
 * one table is displayed at a time and is completely replaced when the
 * next table is read. The table is dynamically sized to fit the data.
 */

public class TablesTabFragment extends BasicAssistantFragment implements ServiceConnection, TextMessageObserver {
    private final static String CLSS = "TablesTabFragment";
    private RecyclerView.LayoutManager layoutManager;
    private TextMessageAdapter adapter;
    private View rootView = null;
    private RecyclerView logMessageView;
    private TextView logView;
    private DispatchService service = null;
    private TextManager textManager = null;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rootView = inflater.inflate(R.layout.fragment_tables_tab, container, false);
        TextView textView = rootView.findViewById(R.id.fragmentTablesText);
        textView.setText(R.string.fragmentTableTabLabel);

        logMessageView = rootView.findViewById(R.id.logs_recycler_view);
        logMessageView.setHasFixedSize(true);   // Refers to the size of the layout.
        LinearLayoutManager layoutManager = new LinearLayoutManager(logMessageView.getContext());
        logMessageView.setLayoutManager(layoutManager);
        adapter = new TextMessageAdapter(new FixedSizeList<>(BertConstants.NUM_LOG_MESSAGES));
        logMessageView.setAdapter(adapter);
        int scrollPosition = layoutManager.findFirstCompletelyVisibleItemPosition();
        logMessageView.scrollToPosition(scrollPosition);

        return rootView;
    }
     // Bind to the DispatchService
    @Override
    public void onStart() {
        super.onStart();
        if( getContext()!=null ) {
            Intent intent = new Intent(getContext().getApplicationContext(), DispatchService.class);
            getContext().getApplicationContext().bindService(intent, this, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(CLSS,"onResume: registering adapter as observer");
        if( service!=null ) service.registerTableViewer(this);
    }
    @Override
    public void onPause() {
        super.onPause();
        Log.i(CLSS, "onPause: unregistering adapter as observer");
        if (service != null) {
            service.unregisterTableViewer(this);
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

    // =================================== ServiceConnection ===============================
    @Override
    public void onServiceDisconnected(ComponentName name) {
        if( service!=null ) service.unregisterTableViewer(this);
        service = null;
        textManager = null;
    }

    // name.getClassName() contains the class of the service.
    @Override
    public void onServiceConnected(ComponentName name, IBinder bndr) {
        DispatchServiceBinder binder = (DispatchServiceBinder) bndr;
        service = binder.getService();
        service.registerTableViewer(this);
    }

    // =================================== TextMessageObserver ===============================
    @Override
    public String getName() { return CLSS; }
    @Override
    public void initialize(TextManager mgr) {
        textManager = mgr;
    }
    @Override
    public void update(TextMessage msg) {
        String text = msg.getMessage();
    }
}
