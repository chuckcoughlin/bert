/**
 * Copyright 2019 Charles Coughlin. All rights reserved.
 *  (MIT License)
 */

package chuckcoughlin.bert.tab;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import chuckcoughlin.bert.MainActivity;
import chuckcoughlin.bert.R;
import chuckcoughlin.bert.service.BroadcastObserver;
import chuckcoughlin.bert.service.FacilityState;
import chuckcoughlin.bert.service.FacilityStateReceiver;
import chuckcoughlin.bert.service.SpokenTextReceiver;
import chuckcoughlin.bert.service.TieredFacility;
import chuckcoughlin.bert.service.VoiceConstants;

/**
 * This fragment presents a static "cover" with no dynamic content.
 */

public class CoverFragment extends BasicAssistantFragment implements BroadcastObserver {
    private final static String CLSS = "CoverFragment";
    private FacilityStateReceiver csr = null;
    private SpokenTextReceiver str      = null;
    ToggleButton bluetoothStatus = null;
    ToggleButton socketStatus = null;
    ToggleButton voiceStatus = null;

    // Inflate the view. It holds a fixed image of the robot
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cover, container, false);

        TextView label = view.findViewById(R.id.fragmentCoverText);
        label.setText(getString(R.string.fragmentCoverLabel));
        label.setTextSize(36);

        ImageView imageView = view.findViewById(R.id.fragmentCoverImage);
        imageView.setImageResource(R.drawable.recliner);

        bluetoothStatus = view.findViewById(R.id.bluetooth_status);
        socketStatus = view.findViewById(R.id.socket_status);
        voiceStatus = view.findViewById(R.id.voice_status);

        updateToggleButton(bluetoothStatus, FacilityState.IDLE);
        updateToggleButton(socketStatus, FacilityState.IDLE);
        updateToggleButton(voiceStatus, FacilityState.IDLE);
        Log.i(CLSS,"onCreateView: ....");
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Register broadcast receivers
        IntentFilter filter = new IntentFilter(VoiceConstants.RECEIVER_FACILITY_STATE);
        filter.addAction(VoiceConstants.RECEIVER_FACILITY_STATE);
        csr = new FacilityStateReceiver();
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(csr,filter);

        filter = new IntentFilter(VoiceConstants.RECEIVER_SPOKEN_TEXT);
        str = new SpokenTextReceiver();
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(str,filter);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i(CLSS,"onPause: unregistering the receivers");
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(csr);
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(str);
    }

    @Override
    public void onDestroyView() {
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
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                if( state.equals(FacilityState.IDLE)) {
                    btn.setActivated(false);
                }
                else if( state.equals(FacilityState.WAITING)) {
                    btn.setActivated(true);
                    btn.setEnabled(true);
                    btn.setChecked(false);
                }
                else if( state.equals(FacilityState.ACTIVE)) {
                    btn.setActivated(true);
                    btn.setEnabled(true);
                    btn.setChecked(true);
                }
                else if( state.equals(FacilityState.ERROR)) {
                    btn.setActivated(true);
                    btn.setEnabled(false);
                }
            }
        });
    }
    // ===================== BroadcastObserver =====================
    @Override
    public void broadcastReceived(Intent intent) {
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
}
