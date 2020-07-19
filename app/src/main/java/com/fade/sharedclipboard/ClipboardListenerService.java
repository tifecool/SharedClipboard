package com.fade.sharedclipboard;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import static androidx.constraintlayout.widget.Constraints.TAG;
import static com.fade.sharedclipboard.MainActivity.APP_SHARED_PREF;

public class ClipboardListenerService extends Service {

	ClipboardManager clipboard;
	ClipboardManager.OnPrimaryClipChangedListener changedListener;

	public static final String FOREGROUND_CHANNEL = "Clipboard.foreground.notification";
	public static final String COPIED_CHANNEL = "Clipboard.copied.notification";
	public static final String ONLINE_CHANNEL = "Clipboard.online.notification";
	private NotificationManager manager;
	private PendingIntent notifyPendingIntent;
	private static Boolean fromDatabase = false;
	private static Boolean fromDevice = false;
	public static FirebaseUser currentUser;
	CurrentClip currentClip;
	SharedPreferences settingsSharedPref;

	@Override
	public void onCreate() {
		super.onCreate();

		currentClip = new CurrentClip();
		/*Context context = this;
		currentClipListener = (CurrentClipListener) context;*/

		currentUser = FirebaseAuth.getInstance().getCurrentUser();

		final SharedPreferences sharedPreferences = this.getSharedPreferences(APP_SHARED_PREF, MODE_PRIVATE);
		settingsSharedPref = PreferenceManager.getDefaultSharedPreferences(this);

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
				try {
					if (clipboard.hasPrimaryClip()) {
						// Get the very first item from the clip.
						ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);

						String string = item.coerceToText(ClipboardListenerService.this).toString();
						CurrentClip.setCurrentClip(string);
						Log.d(TAG, "onPrimaryClipChanged: "+string);

						if (!ActivityVisibility.isActivityVisible() && !fromDatabase && settingsSharedPref.getBoolean("copied_notification_pref", true)) {
							copiedNotification();
							Log.d(TAG, "onPrimaryClipChanged: COPIED");
						}

						fromDatabase = false;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};

		if (currentUser != null) {
			FirebaseDatabase.getInstance().getReference().child("users").child(currentUser.getUid()).child("current clip").addValueEventListener(new ValueEventListener() {
				@Override
				public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
					try {
						CurrentClip.setCurrentClip(dataSnapshot.getValue().toString());
						if (!ActivityVisibility.isActivityVisible() && !fromDevice && settingsSharedPref.getBoolean("database_notification_pref", true))
							currentClipUpdatedNotification();
						fromDevice = false;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				@Override
				public void onCancelled(@NonNull DatabaseError databaseError) {
					CurrentClip.setCurrentClip(sharedPreferences.getString("CURRENT_CLIP", ""));
				}
			});
		}

		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
			startOreoAboveForeground();

			if (currentUser == null) {
				StartService.killedProg = true;
				stopService(new Intent(ClipboardListenerService.this, ClipboardListenerService.class));
			} else {
				StartService.killedProg = false;
			}
		} else {

			NotificationCompat.Builder notificationBuilderLowSdk = new NotificationCompat.Builder(this, FOREGROUND_CHANNEL);
			Notification notificationLowSdk = notificationBuilderLowSdk
					.setContentTitle("Clipboard running")
					.setContentText("Running")
					.setContentIntent(notifyPendingIntent)
					.setSmallIcon(R.drawable.ic_running)
					.build();

			//Prevents Service from dying
			notificationLowSdk.flags = notificationLowSdk.flags | Notification.FLAG_NO_CLEAR;
			startForeground(2, notificationLowSdk);

			if (currentUser == null) {
				StartService.killedProg = true;
				stopService(new Intent(ClipboardListenerService.this, ClipboardListenerService.class));
			} else {
				StartService.killedProg = false;
			}

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
				.setSmallIcon(R.drawable.ic_running)
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

	public void setFromDatabase(boolean bool) {
		fromDatabase = bool;
	}

	public void setFromDevice(boolean bool) {
		fromDevice = bool;
	}


	private PendingIntent searchAction() {

		String currentClip = this.currentClip.getCurrentClip();

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

		String currentClip = this.currentClip.getCurrentClip();

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
			NotificationChannel chan1 = new NotificationChannel(COPIED_CHANNEL, copiedNotificationName, NotificationManager.IMPORTANCE_DEFAULT);
			chan1.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
			manager.createNotificationChannel(chan1);

			String currentClipContent = this.currentClip.getCurrentClip();
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
					.setPriority(NotificationManager.IMPORTANCE_DEFAULT)
					.setContentIntent(notifyPendingIntent)
					.setSmallIcon(R.drawable.ic_notifications);

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

			String currentClipContent = this.currentClip.getCurrentClip();
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
					.setSmallIcon(R.drawable.ic_notifications)
					.setSound(Settings.System.DEFAULT_NOTIFICATION_URI);

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

	private void currentClipUpdatedNotification() {

		Notification onlineNotify;

		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {

			String onlineNotificationName = "Online Notification";
			NotificationChannel chan2 = new NotificationChannel(ONLINE_CHANNEL, onlineNotificationName, NotificationManager.IMPORTANCE_DEFAULT);
			chan2.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
			manager.createNotificationChannel(chan2);

			String currentClipContent = this.currentClip.getCurrentClip();

			if (currentClipContent.length() > 150) {
				currentClipContent = currentClipContent.substring(0, 150);
				currentClipContent = currentClipContent.concat("...");
			}

			NotificationCompat.Builder copiedNotifyBuilder = new NotificationCompat.Builder(this, ONLINE_CHANNEL);
			copiedNotifyBuilder
					.setContentTitle(getString(R.string.new_clip))
					.setContentText(currentClipContent)
					.setPriority(NotificationManager.IMPORTANCE_DEFAULT)
					.setContentIntent(notifyPendingIntent)
					.setSmallIcon(R.drawable.ic_notifications)
					.addAction(R.drawable.ic_search, getString(R.string.copy_text), copyAction());

			onlineNotify = copiedNotifyBuilder.build();
			Log.d(TAG, "currentClipUpdatedNotification: ONLINE NOTIFY");

		} else {

			String currentClipContent = this.currentClip.getCurrentClip();

			if (currentClipContent.length() > 150) {
				currentClipContent = currentClipContent.substring(0, 150);
				currentClipContent = currentClipContent.concat("...");
			}

			NotificationCompat.Builder copiedNotifyBuilder = new NotificationCompat.Builder(this, ONLINE_CHANNEL);
			copiedNotifyBuilder
					.setContentTitle(getString(R.string.new_clip))
					.setContentText(currentClipContent)
					.setContentIntent(notifyPendingIntent)
					.setSmallIcon(R.drawable.ic_notifications)
					.addAction(R.drawable.ic_search, getString(R.string.copy_text), copyAction())
					.setSound(Settings.System.DEFAULT_NOTIFICATION_URI);

			onlineNotify = copiedNotifyBuilder.build();
		}

		manager.notify(3, onlineNotify);

	}

	private PendingIntent copyAction() {

		String currentClip = this.currentClip.getCurrentClip();

		if (currentClip.length() > 3000) {
			currentClip = currentClip.substring(0, 3000);
		}

		Intent intent = new Intent(this, ShareActionIntentService.class);
		intent.setAction(ShareActionIntentService.COPY)
				.putExtra(ShareActionIntentService.CURRENT_CLIP_DATA, currentClip);
		Log.d("COPY RAN", "CopyAction: " + currentClip);

		return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
	}

}



