package com.fade.sharedclipboard;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ShareActionIntentService extends IntentService {

	//Service for uploading shared current clip to database
	//And copy current clip from database into phone clipboard
	public static final String SHARE = "com.fade.sharedclipboard.action.SHARE";
	public static final String COPY = "com.fade.sharedclipboard.action.COPY";

	public static final String CURRENT_CLIP_DATA = "com.fade.sharedclipboard.extra.CCD";

	Handler mHandler;
	Context mContext;

	public ShareActionIntentService() {
		super("ShareActionIntentService");
	}

	@Override
	public void onCreate() {
		super.onCreate();

		mHandler = new Handler(getMainLooper());
		mContext = this;

	}

	private class DisplayToast implements Runnable {
		String mText;

		DisplayToast(String text) {
			mText = text;
		}

		public void run() {
			Toast.makeText(mContext, mText, Toast.LENGTH_SHORT).show();
		}
	}


	@Override
	protected void onHandleIntent(Intent intent) {

		if (intent != null) {
			final String action = intent.getAction();
			final String param1 = intent.getStringExtra(CURRENT_CLIP_DATA);

			assert action != null;
			switch (action) {
				case SHARE:
					handleShareAction(param1);
					break;

				case COPY:
					handleCopyAction(param1);
			}


			/*if (SHARE.equals(action)) {
				final String param1 = intent.getStringExtra(CURRENT_CLIP_DATA);
				handleShareAction(param1);
			}*/
		}
	}

	private void handleCopyAction(String param1) {
		ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

		ClipboardListenerService obj = new ClipboardListenerService();
		obj.setFromDatabase(true);

		clipboard.setPrimaryClip(ClipData.newPlainText(param1, param1));
		//Creates a toast pop up
		mHandler.post(new DisplayToast(getString(R.string.copied)));
		((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(1);


	}

	private void handleShareAction(String param1) {

		ClipboardListenerService obj = new ClipboardListenerService();
		obj.setFromDevice(true);

		try {
			FirebaseDatabase.getInstance().getReference().child("users").child(ClipboardListenerService.currentUser.getUid()).child("current clip").setValue(param1, new DatabaseReference.CompletionListener() {
				@Override
				public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
					if (databaseError == null) {
						mHandler.post(new DisplayToast(getString(R.string.shared)));
					} else {
						mHandler.post(new DisplayToast(getString(R.string.failed_to_upload)));
						System.out.print(databaseError.getDetails());
					}
					((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(1);

				}
			});
		} catch (Exception e) {
			e.printStackTrace();
			mHandler.post(new DisplayToast(getString(R.string.failed_to_upload)));
		}
	}

}
