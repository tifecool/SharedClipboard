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

	public final String NOTIFICATION_CHANNEL_ID = "Clipboard.notification";
	private NotificationManager manager;


	@Override
	public void onCreate() {
		super.onCreate();
		clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
		manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		changedListener = new ClipboardManager.OnPrimaryClipChangedListener() {
			@Override
			public void onPrimaryClipChanged() {
				if (clipboard.hasPrimaryClip()
						&& clipboard.getPrimaryClipDescription().hasMimeType(
						ClipDescription.MIMETYPE_TEXT_PLAIN)) {
					// Get the very first item from the clip.
					ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);

					Log.i("Clipboard ", item.getText().toString());
					Toast.makeText(ClipboardListenerService.this, item.getText().toString(), Toast.LENGTH_SHORT).show();
					setCurrentClip(item.getText().toString());


				}
			}
		};

		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O)
			startOreoAboveForeground();
		else {

			//TODO: Notifications for lower versions not displayed properly(But working)
			//		Icon creation
			NotificationCompat.Builder notificationBuilderLowSdk = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
			Notification notificationLowSdk = notificationBuilderLowSdk
					.setContentTitle("Clipboard running")
					.setContentText("Running")
					.build();

			//Prevents Service from dying
			notificationLowSdk.flags = notificationLowSdk.flags | Notification.FLAG_NO_CLEAR;
			startForeground(1, notificationLowSdk);

		}


	}


	@RequiresApi(Build.VERSION_CODES.O)
	private void startOreoAboveForeground() {

		String channelName = "Clipboard Notification";
		NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_LOW);
		chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);


		assert manager != null;
		manager.createNotificationChannel(chan);

		NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
		Notification notification = notificationBuilder.setOngoing(true)
				.setContentTitle("Clipboard running")
				.setPriority(NotificationManager.IMPORTANCE_LOW)
				.setCategory(Notification.CATEGORY_SERVICE)
				.build();

		//Prevents service from dying
		notification.flags = notification.flags | Notification.FLAG_NO_CLEAR;
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

	private static String currentClip;

	public void setCurrentClip(String clip){
		currentClip = clip;
	}

	public String getCurrentClip(){
		return currentClip;
	}
}
