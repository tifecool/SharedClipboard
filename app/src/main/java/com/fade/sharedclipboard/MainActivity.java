package com.fade.sharedclipboard;

import android.app.ActivityManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.PowerManager;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.preference.PreferenceManager;

import com.android.billingclient.api.Purchase;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

	public static final float APP_VERSION = (float) 1.0;

	public static final String LAST_UUID = "lastUUID";
	public static final String MAIN_ACTIVITY_INTENT = "MAIN_ACTIVITY";
	public static final String DONT_SHOW_CHECK = "DONT_SHOW_CHECK";
	public static final String USERS_EMAIL = "users_email";
	public static final String APP_SHARED_PREF = "LastUser";
	public static final String SQL_DATABASE_NAME = "SAVED CLIPS";
	public static final String FIRST_LAUNCH = "FIRST_LAUNCH";
	public static final String PURCHASED_EXTRA = "PURCHASED_EXTRA";
	public static final String PENDING_EXTRA = "PENDING_EXTRA";
	public static final String PURCHASE_TOKEN_EXTRA = "PURCHASE_TOKEN_EXTRA";

	private static FirebaseUser currentUser;
	private static DatabaseReference savedClipRef;
	private static DatabaseReference deletedClipRef;

	private String userEmail;

	private ImageView pushPinImage;
	private ImageView syncImage;
	private EditText editText;
	private ClipboardManager clipboard;
	private Button button;
	public static ArrayList<Long> unixTime = new ArrayList<>();
	public static ArrayList<Integer> syncedBoolean = new ArrayList<>();
	public static ArrayList<String> savedClipID = new ArrayList<>();
	public static ArrayList<String> savedClipTitles = new ArrayList<>();
	public static ArrayList<String> savedClipContents = new ArrayList<>();
	//d for deleted, DeletedArrays
	public static ArrayList<String> dSavedClipTitles = new ArrayList<>();
	public static ArrayList<String> dSavedClipContents = new ArrayList<>();
	public static ArrayList<Long> dUnixTime = new ArrayList<>();
	public static ArrayList<Integer> dSyncedBoolean = new ArrayList<>();
	public static ArrayList<String> dSavedClipID = new ArrayList<>();
	public static ArrayList<Integer> dDeleted = new ArrayList<>();
	private static NavListAdapter savedClipAdapter;
	private DrawerLayout drawerLayout;
	private static SQLiteDatabase database;
	private SharedPreferences sharedPreferences;
	private SharedPreferences settingsPreferences;
	private Utils.UnixTimeDownloader syncRunner;

	private CountDownTimer countDownTimer = new CountDownTimer(2000, 2000) {

		@Override
		public void onTick(long millisUntilFinished) {

		}

		@Override
		public void onFinish() {
			button.setText(R.string.save_f);
			pushPinImage.setVisibility(View.VISIBLE);
		}
	};
	private ListView navList;
	private CurrentClip currentClip;
	private LinearLayout navLinear;
	private AdView bannerAd;
	private boolean purchasedExtra = true;
	private boolean pendingExtra = false;
	private String purchaseToken;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		//Ad Initialization
		MobileAds.initialize(this);
		bannerAd = findViewById(R.id.adBanner);
		navLinear = findViewById(R.id.navLinear);
		navLinear.removeView(bannerAd);

		sharedPreferences = this.getSharedPreferences(APP_SHARED_PREF, MODE_PRIVATE);

		//Find Views By Id's
		syncImage = findViewById(R.id.syncImage);
		pushPinImage = findViewById(R.id.pushPinImage);
		editText = findViewById(R.id.editbox);
		button = findViewById(R.id.saveButton);
		drawerLayout = findViewById(R.id.drawer_layout);

		clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

		currentUser = FirebaseAuth.getInstance().getCurrentUser();

		if (sharedPreferences.getBoolean(FIRST_LAUNCH, true)) {
			startActivity(new Intent(MainActivity.this, IntroActivity.class));
			finish();
		} else {

			if (currentUser == null) {
				startActivity(new Intent(MainActivity.this, LoginActivity.class));

				StartService.killedProg = true;
				stopService(new Intent(MainActivity.this, ClipboardListenerService.class));

				finish();
			} else {

				currentClip = new CurrentClip();
				currentClip.setCurrentClipListener(new CurrentClipListener() {
					@Override
					public void onCurrentClipChanged(String currentClip) {
						editText.setText(currentClip);
					}
				});

				//Database Initialization and creation
				database = this.openOrCreateDatabase(SQL_DATABASE_NAME, MODE_PRIVATE, null);
				settingsPreferences = PreferenceManager.getDefaultSharedPreferences(this);

				if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
					Toast.makeText(this, "External Clipboard Listening Disabled on Android 10.", Toast.LENGTH_SHORT).show();
					settingsPreferences.edit().putBoolean("copied_notification_pref", false).apply();
				}

				String packageName = this.getPackageName();
				PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
				assert pm != null;

				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
					if (!pm.isIgnoringBatteryOptimizations(packageName) && !sharedPreferences.getBoolean(DONT_SHOW_CHECK, false))
						Utils.openPowerSettings(getLayoutInflater(), sharedPreferences, this);
				}

				//Firebase Database References
				userEmail = currentUser.getEmail();
				savedClipRef = FirebaseDatabase.getInstance().getReference().child("users").child(currentUser.getUid()).child("saved clips");
				deletedClipRef = FirebaseDatabase.getInstance().getReference().child("users").child(currentUser.getUid()).child("deleted clips");
				DatabaseReference removeAds = FirebaseDatabase.getInstance().getReference().child("users").child(currentUser.getUid()).child("remove ads");

				//Clearing of Arrays
				savedClipTitles.clear();
				savedClipContents.clear();
				savedClipID.clear();
				syncedBoolean.clear();
				unixTime.clear();

				dSavedClipTitles.clear();
				dSavedClipContents.clear();
				dSavedClipID.clear();
				dSyncedBoolean.clear();
				dUnixTime.clear();
				dDeleted.clear();

				//Creates a List adapter, List views use them to set content
				savedClipAdapter = new NavListAdapter(this, savedClipTitles, syncedBoolean);
				savedClipAdapter.notifyDataSetChanged();

			/*database.execSQL("DROP TABLE IF EXISTS SavedClips");
			database.execSQL("DROP TABLE IF EXISTS DeletedClips");*/

				//Database Tables
				database.execSQL("CREATE TABLE IF NOT EXISTS SavedClips (id VARCHAR(36) PRIMARY KEY, ClipTitle VARCHAR, ClipContent VARCHAR, UnixTimeLastSynced INT , Synced INT)");
				database.execSQL("CREATE TABLE IF NOT EXISTS DeletedClips (id VARCHAR(36) PRIMARY KEY, ClipTitle VARCHAR, ClipContent VARCHAR, UnixTimeLastSynced INT, Synced INT, Deleted INT)");

				//User Check
				if (differentUserLoggedIn()) {

					database.execSQL("DELETE FROM SavedClips");
					database.execSQL("DELETE FROM DeletedClips");

				} else {

					//Populate saved Arrays from SQL
					try {
						Cursor c = database.rawQuery("SELECT * FROM SavedClips ORDER BY UnixTimeLastSynced ASC", null);

						int unixTimeIndex = c.getColumnIndex("UnixTimeLastSynced");
						int syncedIndex = c.getColumnIndex("Synced");
						int clipIDIndex = c.getColumnIndex("id");
						int clipTitleIndex = c.getColumnIndex("ClipTitle");
						int clipContentIndex = c.getColumnIndex("ClipContent");

						if (c.moveToFirst()) {
							do {
								syncedBoolean.add(0, c.getInt(syncedIndex));
								unixTime.add(0, c.getLong(unixTimeIndex));
								savedClipID.add(0, c.getString(clipIDIndex));
								savedClipContents.add(0, c.getString(clipContentIndex));
								savedClipTitles.add(0, c.getString(clipTitleIndex));
							} while (c.moveToNext());
						}

						savedClipAdapter.notifyDataSetChanged();
						c.close();
					} catch (Exception e) {
						e.printStackTrace();
						Toast.makeText(MainActivity.this, R.string.reading_storage, Toast.LENGTH_LONG).show();
					}

					//Populate Deleted Arrays from SQL
					try {
						Cursor c = database.rawQuery("SELECT * FROM DeletedClips ORDER BY UnixTimeLastSynced ASC", null);

						int unixTimeIndex = c.getColumnIndex("UnixTimeLastSynced");
						int syncedIndex = c.getColumnIndex("Synced");
						int clipIDIndex = c.getColumnIndex("id");
						int clipTitleIndex = c.getColumnIndex("ClipTitle");
						int clipContentIndex = c.getColumnIndex("ClipContent");
						int deletedIndex = c.getColumnIndex("Deleted");

						if (c.moveToFirst()) {
							do {
								dSyncedBoolean.add(0, c.getInt(syncedIndex));
								dUnixTime.add(0, c.getLong(unixTimeIndex));
								dSavedClipID.add(0, c.getString(clipIDIndex));
								dSavedClipContents.add(0, c.getString(clipContentIndex));
								dSavedClipTitles.add(0, c.getString(clipTitleIndex));
								dDeleted.add(0, deletedIndex);
							} while (c.moveToNext());
						}

						c.close();
					} catch (Exception e) {
						e.printStackTrace();
						Toast.makeText(MainActivity.this, R.string.reading_storage, Toast.LENGTH_LONG).show();
					}
				}

				//Online Database Querying
				updateFromFirebase();

				//List Creation
				navList = findViewById(R.id.navListView);

				//Sets adapter
				navList.setAdapter(savedClipAdapter);

				//MultiSelection
				navList.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
				navList.setMultiChoiceModeListener(multiListener);

				//When an Item is clicked run below
				navList.setOnItemClickListener(
						new AdapterView.OnItemClickListener() {
							@Override
							public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
								//Gets String from Item in position i that was clicked
								String currentClip = savedClipContents.get(i);

								clipboard.setPrimaryClip(ClipData.newPlainText(currentClip, currentClip));
								//Creates a toast pop up
								Toast.makeText(MainActivity.this, R.string.copied, Toast.LENGTH_SHORT).show();

							}
						}
				);

				removeAds.addValueEventListener(new ValueEventListener() {
					@Override
					public void onDataChange(@NonNull DataSnapshot snapshot) {
						if (snapshot.hasChild("state")) {
							if (Integer.parseInt(snapshot.child("state").getValue().toString()) == Purchase.PurchaseState.PURCHASED) {
								purchasedExtra = true;
								navLinear.removeView(bannerAd);
								Log.d("removeAds", "onDataChange: Purchased");
							} else if (Integer.parseInt(snapshot.child("state").getValue().toString()) == Purchase.PurchaseState.PENDING) {
								Log.d("removeAds", "onDataChange: Pending");

								pendingExtra = true;
								purchasedExtra = false;
								purchaseToken = snapshot.child("purchaseToken").getValue().toString();

								try {
									navLinear.addView(bannerAd);
									AdRequest adRequest = new AdRequest.Builder().build();
									bannerAd.loadAd(adRequest);
									purchasedExtra = false;
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						} else {
							purchasedExtra = false;
							pendingExtra = false;

							try {
								navLinear.addView(bannerAd);
								AdRequest adRequest = new AdRequest.Builder().build();
								bannerAd.loadAd(adRequest);
								purchasedExtra = false;
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}

					@Override
					public void onCancelled(@NonNull DatabaseError error) {

					}

				});

				//Put current user in shared preference
				sharedPreferences.edit().putString(LAST_UUID, currentUser.getUid()).apply();
			}
		}
	}

	@Override
	public void onConfigurationChanged(@NonNull Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		int nightModeFlags = newConfig.uiMode &
				Configuration.UI_MODE_NIGHT_MASK;

		switch (nightModeFlags) {
			case Configuration.UI_MODE_NIGHT_YES:

			case Configuration.UI_MODE_NIGHT_UNDEFINED:

			case Configuration.UI_MODE_NIGHT_NO:
				this.recreate();
				break;

		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		ActivityVisibility.activityResumed();

		//Clipboard Service check and startup
		ClipboardListenerService clipboardService = new ClipboardListenerService();
		Intent serviceIntent = new Intent(this, clipboardService.getClass());

		if (!serviceRunning(clipboardService.getClass())) {
			startService(serviceIntent);
		}


		if (updateRequired(this)) {
			Intent intent = new Intent(MainActivity.this, UpdateRequiredActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);

			StartService.killedProg = true;
			stopService(new Intent(MainActivity.this, ClipboardListenerService.class));

			startActivity(intent);
		}

		Log.d("ONRESUME RAN", "ONRESUME: ");

		findViewById(R.id.dummy).requestFocus();

		if (currentClip.getCurrentClip() != null) {
			editText.setText(currentClip.getCurrentClip());
			sharedPreferences.edit().putString("CURRENT_CLIP", currentClip.getCurrentClip()).apply();
		}

		syncButtonClicked(findViewById(R.id.syncImage));

	}

	@Override
	protected void onStop() {
		super.onStop();

		if (!settingsPreferences.getBoolean("always_running_pref", true) && !toSettings) {
			StartService.killedProg = true;
			stopService(new Intent(MainActivity.this, ClipboardListenerService.class));
		} else {
			toSettings = false;
		}

	}

	private boolean serviceRunning(Class<?> serviceClass) {
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

		for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (serviceClass.getName().equals(service.service.getClassName())) {
				Log.i("Service status", "Running");
				return true;
			}
		}
		Log.i("Service status", "Not running");
		return false;
	}


	public void saveButtonClicked(View view) {
		String clipContent = editText.getText().toString();

		if (!clipContent.isEmpty()) {

			//Adding of content to SQL
			SQLiteStatement statement =
					database.compileStatement("INSERT INTO SavedClips (id,ClipTitle,ClipContent,UnixTimeLastSynced,Synced) VALUES (? , ? , ?, 0, 0)");

			//Title Shorten
			if (clipContent.length() > 125) {
				String shortTit = clipContent.substring(0, 125);
				shortTit = shortTit.concat("...");
				savedClipTitles.add(0, shortTit);
				statement.bindString(2, shortTit);
			} else {
				savedClipTitles.add(0, clipContent);
				statement.bindString(2, clipContent);
			}

			String id = UUID.randomUUID().toString();

			savedClipID.add(0, id);
			statement.bindString(1, id);

			//sharedPreferences.edit().putInt(LAST_INT, sharedPreferences.getInt(LAST_INT, defaultValue) + 1).apply();

			savedClipContents.add(0, clipContent);
			statement.bindString(3, clipContent);

			unixTime.add(0, (long) 0);
			syncedBoolean.add(0, 0);


			statement.execute();
			syncRunner = new Utils.UnixTimeDownloader();
			syncRunner.execute("https://worldtimeapi.org/api/timezone/Etc/UTC");
			Log.d("SYNCED", "NOt Synced ");

			savedClipAdapter.notifyDataSetChanged();

			button.setText(R.string.saved);
			pushPinImage.setVisibility(View.GONE);

			countDownTimer.start();
		} else {
			Toast.makeText(MainActivity.this, R.string.empty_editbox, Toast.LENGTH_SHORT).show();
		}

	}

	//Uploading to Firebase Database
	public static void sync(final Long unixTime) {

		//Sync of SavedClips
		final SQLiteStatement statement =
				database.compileStatement("UPDATE SavedClips SET UnixTimeLastSynced = ?, Synced = 1 WHERE Synced = 0");
		Log.d("SYNCED", "SYNCED");

//		final ArrayList<String> onlineClipIDs = new ArrayList<>();

		savedClipRef.addListenerForSingleValueEvent(new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

				while (MainActivity.unixTime.contains((long) 0)) {

					int i = MainActivity.unixTime.indexOf((long) 0);


					MainActivity.unixTime.add(i, unixTime);
					MainActivity.unixTime.remove(i + 1);

					DatabaseReference currentClip = savedClipRef.child(savedClipID.get(i));

					currentClip.child("content").setValue(savedClipContents.get(i));
					currentClip.child("title").setValue(savedClipTitles.get(i));
					currentClip.child("unix time").setValue(MainActivity.unixTime.get(i));

					syncedBoolean.add(i, 1);
					syncedBoolean.remove(i + 1);


				}

				statement.bindLong(1, unixTime);
				statement.execute();
				savedClipAdapter.notifyDataSetChanged();

			}

			@Override
			public void onCancelled(@NonNull DatabaseError databaseError) {

			}
		});


		//Sync of DeletedClips
		final SQLiteStatement deletedStatement =
				database.compileStatement("UPDATE DeletedClips SET UnixTimeLastSynced = ?, Synced = 1 WHERE Synced = 0");


		final ArrayList<String> dOnlineClipIDs = new ArrayList<>();

		deletedClipRef.addListenerForSingleValueEvent(new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

				for (DataSnapshot deletedClip : dataSnapshot.getChildren()) {
					dOnlineClipIDs.add(deletedClip.getKey());
				}


				while (dUnixTime.contains((long) 0)) {
					final int i = dUnixTime.indexOf((long) 0);

					dUnixTime.add(i, unixTime);
					dUnixTime.remove(i + 1);

					savedClipRef.addListenerForSingleValueEvent(new ValueEventListener() {
						@Override
						public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
							if (dataSnapshot.hasChild(dSavedClipID.get(i))) {
								savedClipRef.child(dSavedClipID.get(i)).removeValue();
							}
						}

						@Override
						public void onCancelled(@NonNull DatabaseError databaseError) {

						}
					});


					DatabaseReference currentClip = deletedClipRef.child(dSavedClipID.get(i));
					currentClip.child("content").setValue(dSavedClipContents.get(i));
					currentClip.child("title").setValue(dSavedClipTitles.get(i));
					currentClip.child("unix time").setValue(dUnixTime.get(i));

					dSyncedBoolean.add(i, 1);
					dSyncedBoolean.remove(i + 1);

					deletedStatement.bindLong(1, unixTime);
					deletedStatement.execute();

				}


			}

			@Override
			public void onCancelled(@NonNull DatabaseError databaseError) {

			}
		});
	}

	public void indicatorClicked(View view) {
		drawerLayout.openDrawer(GravityCompat.START);
	}

	public void syncButtonClicked(View view) {

		syncRunner = new Utils.UnixTimeDownloader();

		updateFromFirebase();
		syncRunner.execute("https://worldtimeapi.org/api/timezone/Etc/UTC");
		syncImage.startAnimation(AnimationUtils.loadAnimation(MainActivity.this, R.anim.rotate));

	}

	public void logoutClicked(View view) {
		FirebaseAuth.getInstance().signOut();
		Intent intent = new Intent(MainActivity.this, LoginActivity.class);
		intent.putExtra(MAIN_ACTIVITY_INTENT, true);
		sharedPreferences.edit().putString(LAST_UUID, currentUser.getUid()).apply();
		startActivity(intent);

		StartService.killedProg = true;
		stopService(new Intent(MainActivity.this, ClipboardListenerService.class));

		finish();
	}

	private void updateFromFirebase() {

		//IDs of Permanently deleted clips
		final ArrayList<String> deletedIDs = new ArrayList<>();
		deletedIDs.clear();
		try {
			Cursor c = database.rawQuery("SELECT * FROM DeletedClips WHERE Deleted = 1 ORDER BY UnixTimeLastSynced ASC", null);
			int idIndex = c.getColumnIndex("id");

			if (c.moveToFirst()) {
				do {
					deletedIDs.add(c.getString(idIndex));
				} while (c.moveToNext());
			}
			c.close();


			//Update Saved Table
			Query unixTimeOrderedSaved = savedClipRef.orderByChild("unix time");

			unixTimeOrderedSaved.addListenerForSingleValueEvent(new ValueEventListener() {
				@Override
				public void onDataChange(@NonNull DataSnapshot dataSnapshot) {


					for (DataSnapshot savedClip : dataSnapshot.getChildren()) {

						if (!savedClipID.contains(savedClip.getKey())) {

							if (dSavedClipID.contains(savedClip.getKey())) {
								int i = dSavedClipID.indexOf(savedClip.getKey());

								if (dUnixTime.get(i) < (long) savedClip.child("unix time").getValue() && dSyncedBoolean.get(i) != 0) {

									dSavedClipID.remove(i);
									dSavedClipContents.remove(i);
									dSavedClipTitles.remove(i);
									dUnixTime.remove(i);
									dSyncedBoolean.remove(i);
									dDeleted.remove(i);
								} else {
									savedClipRef.child(dSavedClipID.get(i)).removeValue();

									DatabaseReference currentClip = deletedClipRef.child(dSavedClipID.get(i));
									currentClip.child("content").setValue(dSavedClipContents.get(i));
									currentClip.child("title").setValue(dSavedClipTitles.get(i));
									currentClip.child("unix time").setValue(dUnixTime.get(i));
									continue;
								}
							}
							SQLiteStatement statement =
									database.compileStatement("INSERT INTO SavedClips (id,ClipTitle,ClipContent,UnixTimeLastSynced,Synced) VALUES (? , ? , ?, ?, 1)");

							statement.bindString(1, savedClip.getKey());
							statement.bindString(2, savedClip.child("title").getValue().toString());
							statement.bindString(3, savedClip.child("content").getValue().toString());
							statement.bindLong(4, (long) savedClip.child("unix time").getValue());
							statement.execute();

							int i;
							if (!unixTime.isEmpty()) {
								i = binarySearch(unixTime, unixTime.size() - 1, 0, (long) savedClip.child("unix time").getValue());
							} else {
								i = 0;
							}

							syncedBoolean.add(i, 1);
							unixTime.add(i, (long) savedClip.child("unix time").getValue());
							savedClipID.add(i, savedClip.getKey());
							savedClipContents.add(i, savedClip.child("content").getValue().toString());
							savedClipTitles.add(i, savedClip.child("title").getValue().toString());


						} else {
							int i = savedClipID.indexOf(savedClip.getKey());

							//Outcome should not be possible
							if (unixTime.get(i) > (long) savedClip.child("unix time").getValue()) {
								Log.d("Bug", "Outcome should not be possible");

								savedClipRef.child(savedClipID.get(i)).child("unix time").setValue(unixTime.get(i));
								savedClipRef.child(savedClipID.get(i)).child("title").setValue(savedClipTitles.get(i));
								savedClipRef.child(savedClipID.get(i)).child("content").setValue(savedClipContents.get(i));

							} else if (unixTime.get(i) < (long) savedClip.child("unix time").getValue()) {
								if (unixTime.get(i) == 0) {
									String id = UUID.randomUUID().toString();

									SQLiteStatement updateStatement =
											database.compileStatement("UPDATE SavedClips SET id = ? WHERE id = ?");

									updateStatement.bindString(1, id);
									updateStatement.bindString(2, savedClipID.get(i));
									updateStatement.execute();
									savedClipID.remove(i);
									savedClipID.add(i, id);

									SQLiteStatement statement =
											database.compileStatement("INSERT INTO SavedClips (id,ClipTitle,ClipContent,UnixTimeLastSynced,Synced) VALUES (? , ? , ?, ?, 1)");

									statement.bindString(1, savedClip.getKey());
									statement.bindString(2, savedClip.child("title").getValue().toString());
									statement.bindString(3, savedClip.child("content").getValue().toString());
									statement.bindLong(4, (long) savedClip.child("unix time").getValue());
									statement.execute();

									syncedBoolean.add(i + 1, 1);
									unixTime.add(i + 1, (long) savedClip.child("unix time").getValue());
									savedClipID.add(i + 1, savedClip.getKey());
									savedClipContents.add(i + 1, savedClip.child("content").getValue().toString());
									savedClipTitles.add(i + 1, savedClip.child("title").getValue().toString());
								} else {

									SQLiteStatement updateStatement =
											database.compileStatement("UPDATE SavedClips SET ClipTitle = ?,ClipContent = ?,UnixTimeLastSynced = ?,Synced = 1 WHERE id = ?");

									updateStatement.bindString(1, savedClip.child("title").getValue().toString());
									updateStatement.bindString(2, savedClip.child("content").getValue().toString());
									updateStatement.bindLong(3, (long) savedClip.child("unix time").getValue());
									updateStatement.bindString(4, savedClipID.get(i));
									updateStatement.execute();

									unixTime.remove(i);
									savedClipTitles.remove(i);
									savedClipContents.remove(i);

									unixTime.add(i, (long) savedClip.child("unix time").getValue());
									savedClipContents.add(i, savedClip.child("content").getValue().toString());
									savedClipTitles.add(i, savedClip.child("title").getValue().toString());
								}
							}
						}

					}

					savedClipAdapter.notifyDataSetChanged();


				}

				@Override
				public void onCancelled(@NonNull DatabaseError databaseError) {

				}
			});

			//Update Deleted Table
			Query unixTimeOrderedDeleted = deletedClipRef.orderByChild("unix time");

			unixTimeOrderedDeleted.addListenerForSingleValueEvent(new ValueEventListener() {
				@Override
				public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

					//Deletion of Permanently Deleted Clips
					for (String deletedId : deletedIDs) {
						if (dataSnapshot.hasChild(deletedId)) {
							deletedClipRef.child(deletedId).removeValue();
						}
						SQLiteStatement deleteStatement = database.compileStatement("DELETE FROM DeletedClips WHERE id = ?");
						deleteStatement.bindString(1, deletedId);
						deleteStatement.execute();
					}


					for (DataSnapshot deletedClip : dataSnapshot.getChildren()) {

						if (!dSavedClipID.contains(deletedClip.getKey())) {

							if (savedClipID.contains(deletedClip.getKey())) {
								int i = savedClipID.indexOf(deletedClip.getKey());

								if (unixTime.get(i) < (long) deletedClip.child("unix time").getValue() && syncedBoolean.get(i) != 0) {

									savedClipID.remove(i);
									savedClipContents.remove(i);
									savedClipTitles.remove(i);
									unixTime.remove(i);
									syncedBoolean.remove(i);

								} else {
									deletedClipRef.child(savedClipID.get(i)).removeValue();

									DatabaseReference currentClip = savedClipRef.child(savedClipID.get(i));
									currentClip.child("content").setValue(savedClipContents.get(i));
									currentClip.child("title").setValue(savedClipTitles.get(i));
									currentClip.child("unix time").setValue(unixTime.get(i));
									continue;
								}
							}

							if (deletedIDs.contains(deletedClip.getKey())) {
								continue;
							}

							SQLiteStatement statement =
									database.compileStatement("INSERT INTO DeletedClips (id,ClipTitle,ClipContent,UnixTimeLastSynced,Synced,Deleted) VALUES (? , ? , ?, ?, 1, 0)");

							statement.bindString(1, deletedClip.getKey());
							statement.bindString(2, deletedClip.child("title").getValue().toString());
							statement.bindString(3, deletedClip.child("content").getValue().toString());
							statement.bindLong(4, (long) deletedClip.child("unix time").getValue());
							statement.execute();

							int i;
							if (!dUnixTime.isEmpty()) {
								i = binarySearch(dUnixTime, dUnixTime.size() - 1, 0, (long) deletedClip.child("unix time").getValue());
							} else {
								i = 0;
							}

							dSyncedBoolean.add(i, 1);
							dDeleted.add(i, 0);
							dUnixTime.add(i, (long) deletedClip.child("unix time").getValue());
							dSavedClipID.add(i, deletedClip.getKey());
							dSavedClipContents.add(i, deletedClip.child("content").getValue().toString());
							dSavedClipTitles.add(i, deletedClip.child("title").getValue().toString());

						} else {
							int i = dSavedClipID.indexOf(deletedClip.getKey());

							//Outcome should not be possible
							if (dUnixTime.get(i) > (long) deletedClip.child("unix time").getValue()) {
								Log.d("Bug", "Outcome should not be possible");

								deletedClipRef.child(dSavedClipID.get(i)).child("unix time").setValue(dUnixTime.get(i));
								deletedClipRef.child(dSavedClipID.get(i)).child("title").setValue(dSavedClipTitles.get(i));
								deletedClipRef.child(dSavedClipID.get(i)).child("content").setValue(dSavedClipContents.get(i));

							} else if (dUnixTime.get(i) < (long) deletedClip.child("unix time").getValue()) {
								if (dUnixTime.get(i) == 0) {
									String id = UUID.randomUUID().toString();

									SQLiteStatement updateStatement =
											database.compileStatement("UPDATE DeletedClips SET id = ? WHERE id = ?");

									updateStatement.bindString(1, id);
									updateStatement.bindString(2, dSavedClipID.get(i));
									updateStatement.execute();
									dSavedClipID.remove(i);
									dSavedClipID.add(i, id);

									SQLiteStatement statement =
											database.compileStatement("INSERT INTO DeletedClips (id,ClipTitle,ClipContent,UnixTimeLastSynced,Synced,Deleted) VALUES (? , ? , ?, ?, 1, 0)");

									statement.bindString(1, deletedClip.getKey());
									statement.bindString(2, deletedClip.child("title").getValue().toString());
									statement.bindString(3, deletedClip.child("content").getValue().toString());
									statement.bindLong(4, (long) deletedClip.child("unix time").getValue());
									statement.execute();

									dSyncedBoolean.add(i + 1, 1);
									dDeleted.add(i + 1, 0);
									dUnixTime.add(i + 1, (long) deletedClip.child("unix time").getValue());
									dSavedClipID.add(i + 1, deletedClip.getKey());
									dSavedClipContents.add(i + 1, deletedClip.child("content").getValue().toString());
									dSavedClipTitles.add(i + 1, deletedClip.child("title").getValue().toString());
								} else {

									SQLiteStatement updateStatement =
											database.compileStatement("UPDATE DeletedClips SET ClipTitle = ?,ClipContent = ?,UnixTimeLastSynced = ?,Synced = 1,Deleted = 0 WHERE id = ?");

									updateStatement.bindString(1, deletedClip.child("title").getValue().toString());
									updateStatement.bindString(2, deletedClip.child("content").getValue().toString());
									updateStatement.bindLong(3, (long) deletedClip.child("unix time").getValue());
									updateStatement.bindString(4, dSavedClipID.get(i));
									updateStatement.execute();

									dUnixTime.remove(i);
									dSavedClipTitles.remove(i);
									dSavedClipContents.remove(i);

									dUnixTime.add(i, (long) deletedClip.child("unix time").getValue());
									dSavedClipContents.add(i, deletedClip.child("content").getValue().toString());
									dSavedClipTitles.add(i, deletedClip.child("title").getValue().toString());
								}
							}
						}
					}


				}

				@Override
				public void onCancelled(@NonNull DatabaseError databaseError) {

				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private boolean differentUserLoggedIn() {
		return !sharedPreferences.getString(LAST_UUID, "").equals(currentUser.getUid());
	}

	@Override
	protected void onDestroy() {
		Log.d("ONDESTROY RAN", "ONDESTROY: ");
		super.onDestroy();

	}

	boolean toSettings = false;

	public void settingsClicked(View view) {
		Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
		toSettings = true;
		intent.putExtra(PURCHASED_EXTRA, purchasedExtra);
		intent.putExtra(USERS_EMAIL, userEmail);
		intent.putExtra(PENDING_EXTRA, pendingExtra);
		intent.putExtra(PURCHASE_TOKEN_EXTRA, purchaseToken);
		startActivity(intent);
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.P){
			finish();
		}
	}

	//VERY USEFUL CODE FOR REMOVING FOCUS ON EDITTEXT
	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		if (ev.getAction() == MotionEvent.ACTION_DOWN) {
			View v = getCurrentFocus();
			if (v instanceof EditText) {
				Rect outRect = new Rect();
				v.getGlobalVisibleRect(outRect);
				if (!outRect.contains((int) ev.getRawX(), (int) ev.getRawY())) {
					Log.d("focus", "touchevent");
					v.clearFocus();
					InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					assert imm != null;
					imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
				}
			}
		}
		return super.dispatchTouchEvent(ev);
	}

	public void copyClicked(View view) {
		//Gets String from Item in position i that was clicked
		String text = editText.getText().toString();

		if (!text.isEmpty()) {
			clipboard.setPrimaryClip(ClipData.newPlainText(text, text));
			//Creates a toast pop up
			Toast.makeText(MainActivity.this, R.string.copied, Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(MainActivity.this, R.string.empty_editbox, Toast.LENGTH_SHORT).show();
		}

	}

	public void shareClicked(View view) {
		if (!editText.getText().toString().isEmpty()) {
			final Button btn = (Button) view;
			btn.setEnabled(false);
			btn.setText("");
			findViewById(R.id.progressBar).setVisibility(View.VISIBLE);

			FirebaseDatabase.getInstance().getReference().child("users").child(currentUser.getUid()).child("current clip").setValue(editText.getText().toString()).addOnCompleteListener(new OnCompleteListener<Void>() {

				@Override
				public void onComplete(@NonNull Task<Void> task) {
					if (task.isSuccessful()) {
						Log.d("Clip Uploaded", "onComplete: Uploaded Clip");
					} else {
						try {
							throw task.getException();
						} catch (Exception e) {
							Toast.makeText(MainActivity.this, R.string.failed_to_upload, Toast.LENGTH_SHORT).show();
							Log.d("Exception", e.toString());
						}
					}

					btn.setEnabled(true);
					btn.setText(R.string.share);
					findViewById(R.id.progressBar).setVisibility(View.GONE);
				}
			});
		} else {
			Toast.makeText(MainActivity.this, R.string.empty_editbox, Toast.LENGTH_SHORT).show();
		}
	}

	//MultiChoiceModeListener for listView
	AbsListView.MultiChoiceModeListener multiListener = new AbsListView.MultiChoiceModeListener() {

		@Override
		public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
			// Capture total checked items
			final int checkedCount = navList.getCheckedItemCount();
			// Set the CAB title according to total checked items
			mode.setTitle(checkedCount + " Selected");
			// Calls toggleSelection method from ListViewAdapter Class
			savedClipAdapter.toggleSelection(position);

			if (checkedCount > 1) {
				findViewById(R.id.edit).setVisibility(View.GONE);
			} else {
				findViewById(R.id.edit).setVisibility(View.VISIBLE);
			}

		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			mode.getMenuInflater().inflate(R.menu.listview_multiselect_menu, menu);
			drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN);

			return true;
		}

		@Override
		public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {

			//Code for deleting
			switch (item.getItemId()) {
				case R.id.delete:

					new AlertDialog.Builder(MainActivity.this)
							.setTitle(R.string.delete_conf)
							.setMessage(R.string.delete_conf_message)
							.setNegativeButton(R.string.cancel, null)
							.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {

									SQLiteStatement deleteStatement =
											database.compileStatement("DELETE FROM SavedClips WHERE id = ?");
									SQLiteStatement insertStatement =
											database.compileStatement("INSERT INTO DeletedClips (id,ClipTitle,ClipContent,UnixTimeLastSynced,Synced,Deleted) VALUES (? , ? , ?, 0, 0,0)");

									// Calls getSelectedIds method from ListViewAdapter Class
									SparseBooleanArray selected = savedClipAdapter.getSelectedIds();
									// Captures all selected ids with a loop
									for (int i = (selected.size() - 1); i >= 0; i--) {
										if (selected.valueAt(i)) {
											// Remove selected items following the ids
											//savedClipAdapter.remove(selected.keyAt(i));
											int s = selected.keyAt(i);

											//Added to deleted table and arrays
											dSavedClipID.add(savedClipID.get(s));
											insertStatement.bindString(1, savedClipID.get(s));
											dSavedClipTitles.add(savedClipTitles.get(s));
											insertStatement.bindString(2, savedClipTitles.get(s));
											dSavedClipContents.add(savedClipContents.get(s));
											insertStatement.bindString(3, savedClipContents.get(s));
											dUnixTime.add((long) 0);
											dSyncedBoolean.add(0);
											dDeleted.add(0);
											insertStatement.execute();

											deleteStatement.bindString(1, savedClipID.get(s));
											deleteStatement.execute();

											savedClipTitles.remove(s);
											savedClipContents.remove(s);
											savedClipID.remove(s);
											syncedBoolean.remove(s);
											unixTime.remove(s);

											syncRunner = new Utils.UnixTimeDownloader();
											syncRunner.execute("https://worldtimeapi.org/api/timezone/Etc/UTC");

										}
									}
									// Close CAB
									mode.finish();
								}
							}).show();
					return true;

				case R.id.edit:

					final int i = savedClipAdapter.getSelectedIds().keyAt(0);

					LayoutInflater inflated = getLayoutInflater();
					final View view = inflated.inflate(R.layout.custom_dialogue, null);
					final EditText dialogueText = view.findViewById(R.id.dialogueEditBox);
					dialogueText.setText(savedClipContents.get(i));

					new AlertDialog.Builder(MainActivity.this)
							.setView(view)
							.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {

									String clipContent = dialogueText.getText().toString();

									if (!clipContent.isEmpty()) {
										SQLiteStatement statement =
												database.compileStatement("UPDATE SavedClips SET ClipTitle = ?,ClipContent = ?,UnixTimeLastSynced = 0,Synced = 0 WHERE id = ?");

										statement.bindString(3, savedClipID.get(i));

										if (clipContent.length() > 125) {
											String shortTit = clipContent.substring(0, 125);
											shortTit = shortTit.concat("...");
											savedClipTitles.add(i, shortTit);
											savedClipTitles.remove(i + 1);
											statement.bindString(1, shortTit);
										} else {
											savedClipTitles.add(i, clipContent);
											savedClipTitles.remove(i + 1);
											statement.bindString(1, clipContent);
										}

										savedClipContents.add(i, clipContent);
										savedClipContents.remove(i + 1);
										statement.bindString(2, clipContent);

										unixTime.add(i, (long) 0);
										unixTime.remove(i + 1);

										syncedBoolean.add(i, 0);
										syncedBoolean.remove(i + 1);

										savedClipAdapter.notifyDataSetChanged();
										statement.execute();

										syncRunner = new Utils.UnixTimeDownloader();
										syncRunner.execute("https://worldtimeapi.org/api/timezone/Etc/UTC");

										// Close CAB
										mode.finish();

									} else {
										Toast.makeText(MainActivity.this, R.string.empty_editbox, Toast.LENGTH_SHORT).show();
										mode.finish();
									}
								}
							})
							.setNegativeButton(R.string.cancel, null)
							.show();

				default:
					return false;

			}
		}


		@Override
		public void onDestroyActionMode(ActionMode mode) {
			savedClipAdapter.removeSelection();
			drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false;
		}
	};

	@Override
	protected void onPause() {
		super.onPause();
		ActivityVisibility.activityPaused();
	}

	private int required = 2;

	public boolean updateRequired(Context context) {

		sharedPreferences = context.getSharedPreferences("LastUser", MODE_PRIVATE);
		Log.d("Update Check", "Checked");

		FirebaseDatabase.getInstance().getReference().child("requiredUpdate").child("forVersion").addListenerForSingleValueEvent(new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

				if (APP_VERSION <= Float.parseFloat(dataSnapshot.getValue().toString())) {
					required = 1;
				} else {
					required = 0;
				}
				sharedPreferences.edit().putFloat("updateVersion", Float.parseFloat(dataSnapshot.getValue().toString())).apply();

			}

			@Override
			public void onCancelled(@NonNull DatabaseError databaseError) {
				required = 2;
			}
		});

		if (required == 1) {
			return true;
		} else if (required == 0) {
			return false;
		} else {
			return APP_VERSION <= sharedPreferences.getFloat("updateVersion", 0);
		}

	}

	//Searches for lowest number compared to x in array
	//Returns position of lowest value in array compared to x in array
	//r - right l - left, x value
	int binarySearch(ArrayList<Long> arr, int r, int l, long x) {
		if (r > l) {
			//ints are rounded down
			int mid = l + ((r - l) / 2);

			// If the element is present at the middle itself
			if (arr.get(mid) == x)
				return mid;

			// If x is smaller than value at mid, then lowest value can only be present in right subarray
			if (arr.get(mid) > x) {
				return binarySearch(arr, r, mid + 1, x);
			} else if (mid == 0) {
				return mid;
			} else {
				// Else the lowest value can only be present in left subarray
				return binarySearch(arr, mid - 1, l, x);
			}

		} else {
			if (arr.get(r) > x) {
				return r + 1;
			} else {
				return r;
			}
		}
	}


}




