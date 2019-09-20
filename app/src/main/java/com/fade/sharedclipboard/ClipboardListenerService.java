package com.fade.sharedclipboard;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

public class ClipboardListenerService extends Service {

	ClipboardManager clipboard;
	ClipboardManager.OnPrimaryClipChangedListener changedListener;

	@Override
	public void onCreate() {
		super.onCreate();
		clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

		changedListener = new ClipboardManager.OnPrimaryClipChangedListener() {
			@Override
			public void onPrimaryClipChanged() {
				if (clipboard.hasPrimaryClip()
						&& clipboard.getPrimaryClipDescription().hasMimeType(
						ClipDescription.MIMETYPE_TEXT_PLAIN)) {
					// Get the very first item from the clip.
					ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);

					Log.i("Clipboard ", item.getText().toString());
					Toast.makeText(ClipboardListenerService.this,item.getText().toString(),Toast.LENGTH_SHORT).show();



				}
			}
		};

		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O)
			startOreoAboveForeground();
		else
			startForeground(1, new Notification());

	}


	@RequiresApi(Build.VERSION_CODES.O)
	private void startOreoAboveForeground()
	{
		String NOTIFICATION_CHANNEL_ID = "Clipboard.notification";
		String channelName = "Clipboard Notification";
		NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_LOW);
		chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

		NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		assert manager != null;
		manager.createNotificationChannel(chan);

		NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
		Notification notification = notificationBuilder.setOngoing(true)
				.setContentTitle("Clipboard running")
				.setPriority(NotificationManager.IMPORTANCE_LOW)
				.setCategory(Notification.CATEGORY_SERVICE)
				.build();
		startForeground(2, notification);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);

		clipboard.addPrimaryClipChangedListener(changedListener);

		return START_STICKY;
	}


	@Override
	public void onDestroy() {
		super.onDestroy();
		clipboard.removePrimaryClipChangedListener(changedListener);

		Intent broadcastIntent = new Intent(this, StartService.class);
		broadcastIntent.setAction("restartservice");

		sendBroadcast(broadcastIntent);
	}



	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
