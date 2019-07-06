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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.List;

import chuckcoughlin.bert.R;
import chuckcoughlin.bert.common.IntentObserver;
import chuckcoughlin.bert.service.DispatchService;
import chuckcoughlin.bert.service.DispatchServiceBinder;
import chuckcoughlin.bert.service.FacilityState;
import chuckcoughlin.bert.service.TieredFacility;
import chuckcoughlin.bert.service.VoiceConstants;

/**
 * This fragment presents a static "cover" with no dynamic content.
 */

public class CoverFragment extends BasicAssistantFragment implements IntentObserver, ServiceConnection {
    private final static String CLSS = "CoverFragment";
    private ToggleButton bluetoothStatus = null;
    private ToggleButton socketStatus = null;
    private ToggleButton voiceStatus = null;
    private DispatchService service = null;

    // Inflate the view. It holds a fixed image of the robot
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        Log.i(CLSS,"onCreateView: ....");
        View view = inflater.inflate(R.layout.fragment_cover, container, false);

        TextView label = view.findViewById(R.id.fragmentCoverText);
        label.setText(getString(R.string.fragmentCoverLabel));
        label.setTextSize(36);

        ImageView imageView = view.findViewById(R.id.fragmentCoverImage);
        imageView.setImageResource(R.drawable.recliner);

        bluetoothStatus = view.findViewById(R.id.bluetooth_status);
        socketStatus = view.findViewById(R.id.socket_status);
        voiceStatus = view.findViewById(R.id.voice_status);
        bluetoothStatus.setClickable(false);  // Not really buttons, just indicators
        socketStatus.setClickable(false);
        voiceStatus.setClickable(false);

        updateToggleButton(bluetoothStatus, FacilityState.IDLE);
        updateToggleButton(socketStatus, FacilityState.IDLE);
        updateToggleButton(voiceStatus, FacilityState.IDLE);
        return view;
    }

    /**
     * Bind to the DispatchService, start speech analyzer and enunciator
     */
    @Override
    public void onStart() {
        super.onStart();
        Intent intent = new Intent(getActivity(), DispatchService.class);
        getActivity().getApplicationContext().bindService(intent, this, Context.BIND_AUTO_CREATE);
    }
    @Override
    public void onResume() {
        super.onResume();
        if( service!=null ) {
            Log.i(CLSS,"onResume: registering as observer");
            service.registerIntentObserver(this);
        }
    }
    @Override
    public void onPause() {
        super.onPause();
        if( service!=null ) {
            Log.i(CLSS,"onPause: unregistering as observer");
            service.unregisterIntentObserver(this);
        }
    }
    @Override
    public void onStop() {
        super.onStop();
        getActivity().getApplicationContext().unbindService(this);
    }
    @Override
    public void onDestroyView() {
        Log.i(CLSS,"onDestroyView: ...");
        super.onDestroyView();
    }

    /**
     * Map current bluetooth action state to ToggleButton icon. Checked in this order ...
     *    gray - active = false
     *    green- checked = true
     *    yellow - checked = false
     *    red - enabled = false
     * @param state
     */
    private void updateToggleButton(final ToggleButton btn,final FacilityState state) {
        Log.i(CLSS,String.format("updateToggleButton:%s %s",btn.getText(),state.name()));
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                btn.setVisibility(View.INVISIBLE);
                if( state.equals(FacilityState.IDLE)) {
                    btn.setChecked(false);
                    btn.setSelected(false);
                }
                else if( state.equals(FacilityState.WAITING)) {
                    btn.setChecked(true);
                    btn.setSelected(false);
                }
                else if( state.equals(FacilityState.ACTIVE)) {
                    btn.setChecked(true);
                    btn.setSelected(true);
                }
                else if( state.equals(FacilityState.ERROR)) {
                    btn.setChecked(false);
                    btn.setSelected(true);
                }
                btn.setVisibility(View.VISIBLE);
            }
        });
    }
    // ===================== IntentObserver =====================
    @Override
    public void initialize(List<Intent> list) {
        for(Intent intent:list) {
            if (intent.hasCategory(VoiceConstants.CATEGORY_FACILITY_STATE)) {
                FacilityState actionState = FacilityState.valueOf(intent.getStringExtra(VoiceConstants.KEY_FACILITY_STATE));
                TieredFacility tf = TieredFacility.valueOf(intent.getStringExtra(VoiceConstants.KEY_TIERED_FACILITY));
                if (tf.equals(TieredFacility.BLUETOOTH)) {
                    updateToggleButton(bluetoothStatus, actionState);
                }
                else if (tf.equals(TieredFacility.SOCKET)) {
                    updateToggleButton(socketStatus, actionState);
                }
                else {
                    updateToggleButton(voiceStatus, actionState);
                }
            }
        }
    }
    @Override
    public void update(Intent intent) {
        if( intent.hasCategory(VoiceConstants.CATEGORY_FACILITY_STATE)) {
            FacilityState actionState = FacilityState.valueOf(intent.getStringExtra(VoiceConstants.KEY_FACILITY_STATE));
            TieredFacility tf = TieredFacility.valueOf(intent.getStringExtra(VoiceConstants.KEY_TIERED_FACILITY));
            if(tf.equals(TieredFacility.BLUETOOTH)) {
                updateToggleButton(bluetoothStatus,actionState);
            }
            else if(tf.equals(TieredFacility.SOCKET)) {
                updateToggleButton(socketStatus,actionState);
            }
            else {
                updateToggleButton(voiceStatus,actionState);
            }
        }
    }
    // =================================== ServiceConnection ===============================
    @Override
    public void onServiceDisconnected(ComponentName name) {
        if( service!=null ) service.unregisterIntentObserver(this);
        service = null;
    }

    // name.getClassName() contains the class of the service.
    @Override
    public void onServiceConnected(ComponentName name, IBinder bndr) {
        DispatchServiceBinder binder = (DispatchServiceBinder) bndr;
        service = binder.getService();
        service.registerIntentObserver(this);
    }
}
