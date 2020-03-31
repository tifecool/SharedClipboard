package com.fade.sharedclipboard;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.app.Service;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

public class ClipboardListenerService extends Service {

	ClipboardManager clipboard;
	ClipboardManager.OnPrimaryClipChangedListener changedListener;

	public static final String FOREGROUND_CHANNEL = "Clipboard.foreground.notification";
	public static final String COPIED_CHANNEL = "Clipboard.copied.notification";
	private NotificationManager manager;
	private PendingIntent notifyPendingIntent;


	@Override
	public void onCreate() {
		super.onCreate();

		StartService.killedProg = false;
		Intent notifyIntent = new Intent(this, MainActivity.class);
		// Set the Activity to start in a new, empty task
		notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		// Create the PendingIntent
		notifyPendingIntent = PendingIntent.getActivity(
				this, 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);

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
					//Toast.makeText(ClipboardListenerService.this, item.getText().toString(), Toast.LENGTH_SHORT).show();
					setCurrentClip(item.getText().toString());
					if (!ActivityVisibility.isActivityVisible())
						copiedNotification();
				}
			}
		};

		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O)
			startOreoAboveForeground();
		else {

			NotificationCompat.Builder notificationBuilderLowSdk = new NotificationCompat.Builder(this, FOREGROUND_CHANNEL);
			Notification notificationLowSdk = notificationBuilderLowSdk
					.setContentTitle("Clipboard running")
					.setContentText("Running")
					.setContentIntent(notifyPendingIntent)
					.setSmallIcon(R.drawable.demo_icon)
					.build();

			//Prevents Service from dying
			notificationLowSdk.flags = notificationLowSdk.flags | Notification.FLAG_NO_CLEAR;
			startForeground(1, notificationLowSdk);

		}


	}


	@RequiresApi(Build.VERSION_CODES.O)
	private void startOreoAboveForeground() {

		String channelName = "Clipboard Notification";
		NotificationChannel chan = new NotificationChannel(FOREGROUND_CHANNEL, channelName, NotificationManager.IMPORTANCE_LOW);
		chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

		assert manager != null;
		manager.createNotificationChannel(chan);

		NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, FOREGROUND_CHANNEL);
		Notification notification = notificationBuilder.setOngoing(true)
				.setContentTitle("Clipboard running")
				.setPriority(NotificationManager.IMPORTANCE_LOW)
				.setCategory(Notification.CATEGORY_SERVICE)
				.setContentIntent(notifyPendingIntent)
				.setSmallIcon(R.drawable.demo_icon)
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

	public void setCurrentClip(String clip) {
		currentClip = clip;
	}

	public String getCurrentClip() {
		return currentClip;
	}

	private PendingIntent searchAction() {

		String currentClip = getCurrentClip();

		if (currentClip.length() > 3000) {
			currentClip = currentClip.substring(0, 3000);
		}

		Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
		intent.putExtra(SearchManager.QUERY, currentClip);
		intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		Log.d("SEARCH ACTION RAN", "searchAction: " + currentClip);

		return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
	}

	private PendingIntent shareAction() {

		String currentClip = getCurrentClip();

		if (currentClip.length() > 3000) {
			currentClip = currentClip.substring(0, 3000);
		}

		Intent intent = new Intent(this, ShareActionIntentService.class);
		intent.setAction(ShareActionIntentService.SHARE)
				.putExtra(ShareActionIntentService.CURRENT_CLIP_DATA, currentClip);
		Log.d("SHARE RAN", "shareAction: " + currentClip);

		return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
	}

	private void copiedNotification() {

		Notification copiedNotify;

		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {

			String copiedNotificationName = "Copied Notifications";
			NotificationChannel chan1 = new NotificationChannel(COPIED_CHANNEL, copiedNotificationName, NotificationManager.IMPORTANCE_HIGH);
			chan1.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
			manager.createNotificationChannel(chan1);

			String currentClipContent = getCurrentClip();
			boolean above150 = false;

			if (currentClipContent.length() > 150) {
				currentClipContent = currentClipContent.substring(0, 150);
				currentClipContent = currentClipContent.concat("...");
				above150 = true;
			}

			NotificationCompat.Builder copiedNotifyBuilder = new NotificationCompat.Builder(this, COPIED_CHANNEL);
			copiedNotifyBuilder
					.setContentTitle("New Clip")
					.setContentText(currentClipContent)
					.setPriority(NotificationManager.IMPORTANCE_HIGH)
					.setCategory(Notification.CATEGORY_EVENT)
					.setContentIntent(notifyPendingIntent)
					.setSmallIcon(R.drawable.demo_icon);

			if (above150) {
				if (currentClipContent.length() > 3000) {
					copiedNotifyBuilder.setSubText(getString(R.string.text_limit));
				} else {
					copiedNotifyBuilder.setSubText(getString(R.string.search_limit));
				}
			} else {
				copiedNotifyBuilder.addAction(R.drawable.ic_search, getString(R.string.search), searchAction());
			}

			copiedNotify = copiedNotifyBuilder.addAction(R.drawable.ic_share, getString(R.string.share), shareAction()).build();

		} else {

			String currentClipContent = getCurrentClip();
			boolean above150 = false;

			if (currentClipContent.length() > 150) {
				currentClipContent = currentClipContent.substring(0, 150);
				currentClipContent = currentClipContent.concat("...");
				above150 = true;
			}

			NotificationCompat.Builder copiedNotifyBuilder = new NotificationCompat.Builder(this, COPIED_CHANNEL);
			copiedNotifyBuilder
					.setContentTitle(getString(R.string.new_clip))
					.setContentText(currentClipContent)
					.setContentIntent(notifyPendingIntent)
					.setSmallIcon(R.drawable.demo_icon);

			if (above150) {
				if (currentClipContent.length() > 3000) {
					copiedNotifyBuilder.setSubText(getString(R.string.text_limit));
				} else {
					copiedNotifyBuilder.setSubText(getString(R.string.search_limit));
				}
			} else {
				copiedNotifyBuilder.addAction(R.drawable.ic_search, getString(R.string.search), searchAction());
			}
			copiedNotify = copiedNotifyBuilder.addAction(R.drawable.ic_share, getString(R.string.share), shareAction()).build();
		}

		manager.notify(1, copiedNotify);
	}
}
