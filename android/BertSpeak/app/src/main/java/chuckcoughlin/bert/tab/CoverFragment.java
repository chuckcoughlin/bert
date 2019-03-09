/**
 * Copyright 2019 Charles Coughlin. All rights reserved.
 *  (MIT License)
 */

package chuckcoughlin.bert.tab;

import android.content.Intent;
import android.os.Bundle;
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
import chuckcoughlin.bert.service.FacilityState;
import chuckcoughlin.bert.service.ServiceStatusManager;
import chuckcoughlin.bert.service.TieredFacility;
import chuckcoughlin.bert.service.VoiceConstants;

/**
 * This fragment presents a static "cover" with no dynamic content.
 */

public class CoverFragment extends BasicAssistantFragment implements IntentObserver {
    private final static String CLSS = "CoverFragment";
    ToggleButton bluetoothStatus = null;
    ToggleButton socketStatus = null;
    ToggleButton voiceStatus = null;

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

    @Override
    public void onResume() {
        super.onResume();
        Log.i(CLSS,"onResume: registering as observer");
        ServiceStatusManager.getInstance().register(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i(CLSS,"onPause: unregistering as observer");
        ServiceStatusManager.getInstance().unregister(this);
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
}
