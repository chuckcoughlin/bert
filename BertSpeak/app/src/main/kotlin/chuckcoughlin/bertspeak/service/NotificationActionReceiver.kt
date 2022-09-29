package chuckcoughlin.bertspeak.service

import chuckcoughlin.bertspeak.R
import android.content.*
import android.util.Log

/**
 * Receive intents as a result of button presses in the Notification dialog
 */
class NotificationActionReceiver : BroadcastReceiver() {
    /**
     * The user has selected one of the buttons on the DispatchService notification dialog.
     * @param context the voice service
     * @param incoming intent contains the desired action
     */
    override fun onReceive(context: Context, incoming: Intent) {
        Log.i(CLSS, "================================================================")
        val action = incoming.action
        val intent = Intent(context, DispatchService::class.java)
        intent.action = action
        Log.i(CLSS, String.format("onReceive: action = %s", action))
        context.startService(intent)

        //This is used to close the notification tray
        val it = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
        context.sendBroadcast(it)
        assert(action != null)
        if (action.equals(
                context.getString(R.string.notificationStop),
                ignoreCase = true
            )
        ) System.exit(0)
    }

    companion object {
        private const val CLSS = "NotificationActionReceiver"
    }
}