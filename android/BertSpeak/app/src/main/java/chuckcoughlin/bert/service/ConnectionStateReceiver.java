/**
 * Copyright 2017-2018 Charles Coughlin. All rights reserved.
 *  (MIT License)
 */

package chuckcoughlin.bert.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

/**
 * Define a broadcast receiver that is interested only in the connection state
 */
public class ConnectionStateReceiver extends BroadcastReceiver {
   public ConnectionStateReceiver() {

   }

  @Override
  public void onReceive(Context context, Intent intent) {
    Bundle bundle = intent.getExtras();
    if (bundle != null) {
      //String string = bundle.getString(DownloadService.FILEPATH);

    }
  }

}
