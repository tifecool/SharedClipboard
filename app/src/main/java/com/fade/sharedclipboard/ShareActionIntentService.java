package com.fade.sharedclipboard;

import android.app.IntentService;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ShareActionIntentService extends IntentService {

	//Service for uploading shared current clip to database
	public static final String SHARE = "com.fade.sharedclipboard.action.SHARE";

	public static final String CURRENT_CLIP_DATA = "com.fade.sharedclipboard.extra.CCD";

	public ShareActionIntentService() {
		super("ShareActionIntentService");
	}


	@Override
	protected void onHandleIntent(Intent intent) {

		if (intent != null) {
			final String action = intent.getAction();
			if (SHARE.equals(action)) {
				final String param1 = intent.getStringExtra(CURRENT_CLIP_DATA);
				handleShareAction(param1);
			}
		}
	}

	private void handleShareAction(String param1) {
		FirebaseDatabase.getInstance().getReference().child("users").child(MainActivity.currentUser.getUid()).child("current clip").setValue(param1, new DatabaseReference.CompletionListener() {
			@Override
			public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
				if (databaseError == null) {
					Toast.makeText(ShareActionIntentService.this, R.string.shared, Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(ShareActionIntentService.this, R.string.failed_to_upload, Toast.LENGTH_SHORT).show();
					System.out.print(databaseError.getDetails());
				}
			}
		});
	}

}
