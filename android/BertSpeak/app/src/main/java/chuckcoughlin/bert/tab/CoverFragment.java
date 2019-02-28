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

import chuckcoughlin.bert.R;
import chuckcoughlin.bert.service.BroadcastObserver;
import chuckcoughlin.bert.service.VoiceConstants;

/**
 * This fragment presents a static "cover" with no dynamic content.
 */

public class CoverFragment extends BasicAssistantFragment implements BroadcastObserver {
    private final static String CLSS = "CoverFragment";

    // Inflate the view. It holds a fixed image of the robot
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cover, container, false);
        TextView label = view.findViewById(R.id.fragmentCoverText);
        label.setText(getString(R.string.fragmentCoverLabel));
        label.setTextSize(36);

        ImageView imageView = view.findViewById(R.id.fragmentCoverImage);
        imageView.setImageResource(R.drawable.recliner);
        return view;
    }

    // ===================== BroadcastObserver =====================
    @Override
    public void broadcastReceived(Intent intent) {
        if( intent.hasCategory(VoiceConstants.CATEGORY_SERVICE_STATE)) {

        }
    }
}
