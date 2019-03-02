/**
 * Copyright 2019 Charles Coughlin. All rights reserved.
 *  (MIT License)
 */

package chuckcoughlin.bert.tab;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import chuckcoughlin.bert.MainActivity;
import chuckcoughlin.bert.R;
import chuckcoughlin.bert.service.BroadcastObserver;
import chuckcoughlin.bert.service.ActionState;
import chuckcoughlin.bert.service.OrderedAction;
import chuckcoughlin.bert.service.VoiceConstants;

/**
 * This fragment presents a static "cover" with no dynamic content.
 */

public class CoverFragment extends BasicAssistantFragment implements BroadcastObserver {
    private final static String CLSS = "CoverFragment";
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

        ((MainActivity)this.getActivity()).getConnectionStateReceiver().register(this);
        ImageView imageView = view.findViewById(R.id.fragmentCoverImage);
        imageView.setImageResource(R.drawable.recliner);

        bluetoothStatus = view.findViewById(R.id.bluetooth_status);
        socketStatus = view.findViewById(R.id.socket_status);
        voiceStatus = view.findViewById(R.id.voice_status);

        updateToggleButton(bluetoothStatus,ActionState.IDLE);
        updateToggleButton(socketStatus,ActionState.IDLE);
        updateToggleButton(voiceStatus,ActionState.IDLE);
        return view;
    }

    @Override
    public void onDestroyView() {
        ((MainActivity)this.getActivity()).getConnectionStateReceiver().unregister(this);
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
    private void updateToggleButton(final ToggleButton btn,final ActionState state) {
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                if( state.equals(ActionState.IDLE)) {
                    btn.setActivated(false);
                }
                else if( state.equals(ActionState.WAITING)) {
                    btn.setActivated(true);
                    btn.setEnabled(true);
                    btn.setChecked(false);
                }
                else if( state.equals(ActionState.ACTIVE)) {
                    btn.setActivated(true);
                    btn.setEnabled(true);
                    btn.setChecked(true);
                }
                else if( state.equals(ActionState.ERROR)) {
                    btn.setActivated(true);
                    btn.setEnabled(false);
                }
            }
        });
    }
    // ===================== BroadcastObserver =====================
    @Override
    public void broadcastReceived(Intent intent) {
        if( intent.hasCategory(VoiceConstants.CATEGORY_SERVICE_STATE)) {
            ActionState actionState = ActionState.valueOf(intent.getStringExtra(VoiceConstants.KEY_SERVICE_STATE));
            OrderedAction oa = OrderedAction.valueOf(intent.getStringExtra(VoiceConstants.KEY_SERVICE_ACTION));
            if(oa.equals(OrderedAction.BLUETOOTH)) {
                updateToggleButton(bluetoothStatus,actionState);
            }
            if(oa.equals(OrderedAction.SOCKET)) {
                updateToggleButton(socketStatus,actionState);
            }
            else {
                updateToggleButton(voiceStatus,actionState);
            }
        }
    }
}
