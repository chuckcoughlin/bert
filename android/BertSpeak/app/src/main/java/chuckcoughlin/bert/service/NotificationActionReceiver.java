package chuckcoughlin.bert.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import chuckcoughlin.bert.R;

/**
 * Receive intents as a result of button presses in the Notification dialog
 */
public class NotificationActionReceiver extends BroadcastReceiver {
    private final static String CLSS = "NotificationActionReceiver";

    /**
     * The user has selected one of the buttons on the VoiceService notification dialog.
     * @param context the voice service
     * @param incoming intent contains the desired action
     */
    @Override
    public void onReceive(Context context, Intent incoming) {

        Log.i(CLSS,"================================================================");

        String action=incoming.getAction();
        Intent intent = new Intent(context, VoiceService.class);
        intent.setAction(action);
        Log.i(CLSS,String.format("onReceive: action = %s",action));
        context.startService(intent);

        //This is used to close the notification tray
        Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        context.sendBroadcast(it);

        if(action.equalsIgnoreCase(context.getString(R.string.notificationStop))) System.exit(0);
    }
}
