package com.fade.sharedclipboard;

import android.app.ActivityManager;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Gravity;
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
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

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

	/*TODO:
			 UPDATE APP SCREEN INCASE OF ANY ISSUES AND UPDATE NEEDED!
	 */
	//public static final String LAST_INT = "lastInt";
	public static final String LAST_UUID = "lastUUID";
	public static final String main_activity_intent = "MAIN_ACTIVITY";

	public static final float APP_VERSION = (float) 1.0;

	private static FirebaseUser currentUser;
	private static DatabaseReference savedClipRef;
	private static DatabaseReference deletedClipRef;

	private String userEmail;

	private ImageView indicator2;
	private ImageView indicator1;
	private ImageView pushPinImage;
	private ImageView syncImage;
	private EditText editText;
	private ClipboardListenerService clipboardListenerObj = new ClipboardListenerService();
	private ClipboardManager clipboard;
	private Button button;
	private static ArrayList<Long> unixTime = new ArrayList<>();
	private static ArrayList<Integer> syncedBoolean = new ArrayList<>();
	private static ArrayList<String> savedClipID = new ArrayList<>();
	private static ArrayList<String> savedClipTitles = new ArrayList<>();
	private static ArrayList<String> savedClipContents = new ArrayList<>();
	//d for deleted, DeletedArrays
	private static ArrayList<String> dSavedClipTitles = new ArrayList<>();
	private static ArrayList<String> dSavedClipContents = new ArrayList<>();
	private static ArrayList<Long> dUnixTime = new ArrayList<>();
	private static ArrayList<Integer> dSyncedBoolean = new ArrayList<>();
	private static ArrayList<String> dSavedClipID = new ArrayList<>();
	private static CustomListAdapter savedClipAdapter;
	private DrawerLayout drawerLayout;
	private static SQLiteDatabase database;
	private SharedPreferences sharedPreferences;
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


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		currentUser = FirebaseAuth.getInstance().getCurrentUser();

		if (currentUser == null) {
			startActivity(new Intent(MainActivity.this, LoginActivity.class));
			finish();
		} else {

			//openPowerSettings(this);

			userEmail = currentUser.getEmail();
			savedClipRef = FirebaseDatabase.getInstance().getReference().child("users").child(currentUser.getUid()).child("saved clips");
			deletedClipRef = FirebaseDatabase.getInstance().getReference().child("users").child(currentUser.getUid()).child("deleted clips");


			Log.d("ONCREATE RAN", "onCreate: ");

			savedClipTitles.clear();
			savedClipContents.clear();
			savedClipID.clear();
			syncedBoolean.clear();
			unixTime.clear();

			//Deleted Arrays
			dSavedClipTitles.clear();
			dSavedClipContents.clear();
			dSavedClipID.clear();
			dSyncedBoolean.clear();
			dUnixTime.clear();


			//Creates a List adapter, List views use them to set content
			savedClipAdapter = new CustomListAdapter(this, savedClipTitles, syncedBoolean);
			savedClipAdapter.notifyDataSetChanged();

			//Database Initialization and creation
			database = this.openOrCreateDatabase("SAVED CLIPS", MODE_PRIVATE, null);
			sharedPreferences = this.getSharedPreferences("LastUser", MODE_PRIVATE);

			if (updateRequired(this)) {
				Intent intent = new Intent(MainActivity.this, UpdateRequiredActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);

				startActivity(intent);
			}

			//database.execSQL("DROP TABLE IF EXISTS SavedClips");

			//Database Tables
			database.execSQL("CREATE TABLE IF NOT EXISTS SavedClips (id VARCHAR(36) PRIMARY KEY, ClipTitle VARCHAR, ClipContent VARCHAR, UnixTimeLastSynced INT , Synced INT)");
			database.execSQL("CREATE TABLE IF NOT EXISTS DeletedClips (id VARCHAR(36) PRIMARY KEY, ClipTitle VARCHAR, ClipContent VARCHAR, UnixTimeLastSynced INT, Synced INT)");

			//User Check
			if (differentUserLoggedIn() || sqlIsEmpty(database, "SavedClips")) {

				database.execSQL("DELETE FROM SavedClips");
				database.execSQL("DELETE FROM DeletedClips");
				updateFromFirebase();
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

					if (c.moveToFirst()) {
						do {
							dSyncedBoolean.add(0, c.getInt(syncedIndex));
							dUnixTime.add(0, c.getLong(unixTimeIndex));
							dSavedClipID.add(0, c.getString(clipIDIndex));
							dSavedClipContents.add(0, c.getString(clipContentIndex));
							dSavedClipTitles.add(0, c.getString(clipTitleIndex));
						} while (c.moveToNext());
					}

					c.close();
				} catch (Exception e) {
					e.printStackTrace();
					Toast.makeText(MainActivity.this, R.string.reading_storage, Toast.LENGTH_LONG).show();
				}
			}


			//Add clipboard listener
			clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

			clipboard.addPrimaryClipChangedListener(new ClipboardManager.OnPrimaryClipChangedListener() {
				@Override
				public void onPrimaryClipChanged() {
					if (clipboard.hasPrimaryClip()
							&& clipboard.getPrimaryClipDescription().hasMimeType(
							ClipDescription.MIMETYPE_TEXT_PLAIN)) {
						// Get the very first item from the clip.
						ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);

						editText.setText(item.getText());

					}
				}
			});

			//Find Views By Id's
			syncImage = findViewById(R.id.syncImage);
			indicator1 = findViewById(R.id.indicator1);
			indicator2 = findViewById(R.id.indicator2);
			pushPinImage = findViewById(R.id.pushPinImage);
			editText = findViewById(R.id.editbox);
			button = findViewById(R.id.saveButton);
			drawerLayout = findViewById(R.id.drawer_layout);

			/*flickerAnimation1(indicator1);
			flickerAnimation2(indicator2);*/

			//Clipboard Service check and startup
			ClipboardListenerService clipboardService = new ClipboardListenerService();
			Intent serviceIntent = new Intent(this, clipboardService.getClass());

			if (!serviceRunning(clipboardService.getClass())) {
				startService(serviceIntent);
			}

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

			//Put current user in shared preference
			sharedPreferences.edit().putString(LAST_UUID, currentUser.getUid()).apply();
		}
	}


	@Override
	protected void onResume() {
		super.onResume();
		ActivityVisibility.activityResumed();

		Log.d("ONRESUME RAN", "ONRESUME: ");

		findViewById(R.id.dummy).requestFocus();

		if (clipboardListenerObj.getCurrentClip() != null) {
			editText.setText(clipboardListenerObj.getCurrentClip());
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
			//Animation of Indicator & OnClickListener
			/*indicator1.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					indicatorClicked(v);
				}
			});
			indicator2.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					indicatorClicked(v);
				}
			});*/
			/*indicator1.setAlpha(0.5f);
			indicator2.setAlpha(0.5f);
			*//*flickerAnimation1(indicator1);
			flickerAnimation2(indicator2);*/

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
	public static void sync(Long unixTime) {

		//Sync of SavedClips
		SQLiteStatement statement =
				database.compileStatement("UPDATE SavedClips SET UnixTimeLastSynced = ?, Synced = 1 WHERE Synced = 0");
		Log.d("SYNCED", "SYNCED");

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


		//Sync of DeletedClips
		SQLiteStatement deletedStatement =
				database.compileStatement("UPDATE DeletedClips SET UnixTimeLastSynced = ?, Synced = 1 WHERE Synced = 0");

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

		}
		deletedStatement.bindLong(1, unixTime);
		deletedStatement.execute();

	}


	/* ANIMATIONS
	float alphaVal1;
	float alphaVal2;
	Animator.AnimatorListener listener1;
	Animator.AnimatorListener listener2;
	final int ANIM_DURATION = 2000;

	private void flickerAnimation1(final ImageView imageView) {
		alphaVal1 = 0.1f;

		listener1 = new Animator.AnimatorListener() {
			@Override
			public void onAnimationStart(Animator animation) {

			}

			@Override
			public void onAnimationEnd(Animator animation) {
				if (alphaVal1 == 0.1f) {
					alphaVal1 = 0.8f;
					imageView.animate().alpha(alphaVal1).setDuration(ANIM_DURATION).setListener(listener1);
				} else if (alphaVal1 == 0.8f) {
					alphaVal1 = 0f;
					imageView.animate().alpha(alphaVal1).setDuration(ANIM_DURATION).setListener(listener1);
				} else if (alphaVal2 == 0f) {
					indicator1.setOnClickListener(null);
				}
			}

			@Override
			public void onAnimationCancel(Animator animation) {

			}

			@Override
			public void onAnimationRepeat(Animator animation) {

			}
		};

		imageView.animate().alpha(alphaVal1).setDuration(ANIM_DURATION).setListener(listener1);
	}

	private void flickerAnimation2(final ImageView imageView) {
		alphaVal2 = 0.1f;

		listener2 = new Animator.AnimatorListener() {
			@Override
			public void onAnimationStart(Animator animation) {

			}

			@Override
			public void onAnimationEnd(Animator animation) {
				if (alphaVal2 == 0.1f) {
					alphaVal2 = 0.8f;
					imageView.animate().alpha(alphaVal2).setDuration(ANIM_DURATION).setListener(listener2);
				} else if (alphaVal2 == 0.8f) {
					alphaVal2 = 0f;
					imageView.animate().alpha(alphaVal2).setDuration(ANIM_DURATION).setListener(listener2);
				} else if (alphaVal2 == 0f) {
					indicator2.setOnClickListener(null);
				}
			}

			@Override
			public void onAnimationCancel(Animator animation) {

			}

			@Override
			public void onAnimationRepeat(Animator animation) {

			}
		};

		imageView.animate().alpha(alphaVal2).setDuration(ANIM_DURATION).setListener(listener2);
	}*/


	public void indicatorClicked(View view) {
		drawerLayout.openDrawer(GravityCompat.START);
	}

	public void syncButtonClicked(View view) {

		syncRunner = new Utils.UnixTimeDownloader();

		if (!unixTime.isEmpty()) {
			syncRunner.execute("https://worldtimeapi.org/api/timezone/Etc/UTC");
			syncImage.startAnimation(AnimationUtils.loadAnimation(MainActivity.this, R.anim.rotate));
		}
	}

	public void logoutClicked(View view) {
		FirebaseAuth.getInstance().signOut();
		Intent intent = new Intent(MainActivity.this, LoginActivity.class);
		intent.putExtra(main_activity_intent, true);
		sharedPreferences.edit().putString(LAST_UUID, currentUser.getUid()).apply();
		startActivity(intent);

		StartService.killedProg = true;
		stopService(new Intent(MainActivity.this, ClipboardListenerService.class));

		finish();
	}

	private boolean sqlIsEmpty(SQLiteDatabase database, String tableName) {

		Cursor mcursor = database.rawQuery("SELECT count(*) FROM " + tableName, null);
		mcursor.moveToFirst();
		int icount = mcursor.getInt(0);
		mcursor.close();
		return icount <= 0;
	}

	private void updateFromFirebase() {

		try {
			//Update Saved Table
			Query unixTimeOrderedSaved = savedClipRef.orderByChild("unix time");

			unixTimeOrderedSaved.addListenerForSingleValueEvent(new ValueEventListener() {
				@Override
				public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

					SQLiteStatement statement =
							database.compileStatement("INSERT INTO SavedClips (id,ClipTitle,ClipContent,UnixTimeLastSynced,Synced) VALUES (? , ? , ?, ?, 1)");

					for (DataSnapshot savedClip : dataSnapshot.getChildren()) {
						statement.bindString(1, savedClip.getKey());
						statement.bindString(2, savedClip.child("title").getValue().toString());
						statement.bindString(3, savedClip.child("content").getValue().toString());
						statement.bindLong(4, (long) savedClip.child("unix time").getValue());
						statement.execute();

						syncedBoolean.add(0, 1);
						unixTime.add(0, (long) savedClip.child("unix time").getValue());
						savedClipID.add(0, savedClip.getKey());
						savedClipContents.add(0, savedClip.child("content").getValue().toString());
						savedClipTitles.add(0, savedClip.child("title").getValue().toString());

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

					SQLiteStatement statement =
							database.compileStatement("INSERT INTO DeletedClips (id,ClipTitle,ClipContent,UnixTimeLastSynced,Synced) VALUES (? , ? , ?, ?, 1)");

					for (DataSnapshot deletedClips : dataSnapshot.getChildren()) {
						statement.bindString(1, deletedClips.getKey());
						statement.bindString(2, deletedClips.child("title").getValue().toString());
						statement.bindString(3, deletedClips.child("content").getValue().toString());
						statement.bindLong(4, (long) deletedClips.child("unix time").getValue());
						statement.execute();

						dSyncedBoolean.add(0, 1);
						dUnixTime.add(0, (long) deletedClips.child("unix time").getValue());
						dSavedClipID.add(0, deletedClips.getKey());
						dSavedClipContents.add(0, deletedClips.child("content").getValue().toString());
						dSavedClipTitles.add(0, deletedClips.child("title").getValue().toString());

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

	public void profileClicked(View view) {
		Toast toast = Toast.makeText(MainActivity.this, userEmail, Toast.LENGTH_SHORT);
		toast.setGravity(Gravity.TOP, view.getLeft() - view.getWidth() / 2 - toast.getView().getWidth() / 2, view.getBottom());
		toast.show();

	}

	/*private void openPowerSettings(Context context) {
		Intent intent = new Intent();
		String packageName = context.getPackageName();
		PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (pm.isIgnoringBatteryOptimizations(packageName))
				intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
			else {
				intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
				intent.setData(Uri.parse("package:" + packageName));
			}
		}
		context.startActivity(intent);
	}*/

	//TODO: VERY USEFUL CODE FOR REMOVING FOCUS ON EDITTEXT
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
					imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
				}
			}
		}
		return super.dispatchTouchEvent(ev);
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
											database.compileStatement("INSERT INTO DeletedClips (id,ClipTitle,ClipContent,UnixTimeLastSynced,Synced) VALUES (? , ? , ?, 0, 0)");

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


}




