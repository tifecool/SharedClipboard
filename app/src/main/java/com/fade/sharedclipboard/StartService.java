package com.fade.sharedclipboard;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class StartService extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {

			Log.i("Receiver", "Recieved");
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				context.startForegroundService(new Intent(context, ClipboardListenerService.class));
			} else {
				context.startService(new Intent(context, ClipboardListenerService.class));
			}

		}

}
