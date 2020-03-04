package com.fade.sharedclipboard;

import android.animation.Animator;
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
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Gravity;
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

public class MainActivity extends AppCompatActivity {

	/*TODO:
	   		 Dialogue box pop up when saved clip clicked
			 UPDATE APP SCREEN INCASE OF ANY ISSUES AND UPDATE NEEDED!
	 */
	//public static final String LAST_INT = "lastInt";
	public static final String LAST_UUID = "lastUUID";
	public static final String main_activity_intent = "MAIN_ACTIVITY";

	private static FirebaseUser currentUser;
	private static DatabaseReference savedClipRef;

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
	private static ArrayList<Integer> deletedBoolean = new ArrayList<>();
	private static ArrayList<Integer> savedClipID = new ArrayList<>();
	//d for dummy
	private static ArrayList<String> dSavedClipTitles = new ArrayList<>();
	private static ArrayList<String> dSavedClipContents = new ArrayList<>();
	private static ArrayList<String> savedClipTitles = new ArrayList<>();
	private static ArrayList<String> savedClipContents = new ArrayList<>();
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
			button.setText(R.string.save);
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


			Log.d("ONCREATE RAN", "onCreate: ");
			dSavedClipTitles.clear();
			dSavedClipContents.clear();
			savedClipTitles.clear();
			savedClipContents.clear();
			savedClipID.clear();
			syncedBoolean.clear();
			unixTime.clear();
			deletedBoolean.clear();


			//Creates a List adapter, List views use them to set content
			savedClipAdapter = new CustomListAdapter(this, dSavedClipTitles, syncedBoolean);
			savedClipAdapter.notifyDataSetChanged();

			//Database Initialization and creation
			database = this.openOrCreateDatabase("SAVED CLIPS", MODE_PRIVATE, null);
			sharedPreferences = this.getSharedPreferences("LastIntUsed", MODE_PRIVATE);

			database.execSQL("DROP TABLE IF EXISTS SavedClips");

			database.execSQL("CREATE TABLE IF NOT EXISTS SavedClips (id INT PRIMARY KEY, ClipTitle VARCHAR, ClipContent VARCHAR, UnixTimeLastSynced INT, Synced INT, Deleted INT)");

			if (differentUserLoggedIn() || sqlIsEmpty(database, "SavedClips")) {

				database.execSQL("DELETE FROM SavedClips");
				updateFromFirebase();
			} else {

				//Populate Arrays from SQL
				try {
					Cursor c = database.rawQuery("SELECT * FROM SavedClips", null);


					int deletedIndex = c.getColumnIndex("Deleted");
					int unixTimeIndex = c.getColumnIndex("UnixTimeLastSynced");
					int syncedIndex = c.getColumnIndex("Synced");
					int clipIDIndex = c.getColumnIndex("id");
					int clipTitleIndex = c.getColumnIndex("ClipTitle");
					int clipContentIndex = c.getColumnIndex("ClipContent");

					if (c.moveToFirst()) {
						do {
							deletedBoolean.add(0, c.getInt(deletedIndex));
							syncedBoolean.add(0, c.getInt(syncedIndex));
							unixTime.add(0, c.getLong(unixTimeIndex));
							savedClipID.add(0, c.getInt(clipIDIndex));
							dSavedClipContents.add(0, c.getString(clipContentIndex));
							dSavedClipTitles.add(0, c.getString(clipTitleIndex));
						} while (c.moveToNext());
					}

					savedClipTitles.addAll(dSavedClipTitles);
					savedClipContents.addAll(dSavedClipContents);
					ArrayList<Integer> dDeletedBoolean = new ArrayList<>(deletedBoolean);
					//If Clip Deleted
					while (dDeletedBoolean.contains(1)) {
						int i = dDeletedBoolean.indexOf(1);

						dSavedClipContents.remove(i);
						dSavedClipTitles.remove(i);
						dDeletedBoolean.remove(i);

					}
					dDeletedBoolean.clear();

					savedClipAdapter.notifyDataSetChanged();
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

			flickerAnimation1(indicator1);
			flickerAnimation2(indicator2);

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
							String toToaster = dSavedClipContents.get(i);

							//Creates a toast pop up
							Toast.makeText(MainActivity.this, toToaster, Toast.LENGTH_SHORT).show();

						}
					}
			);


			sharedPreferences.edit().putString(LAST_UUID, currentUser.getUid()).apply();
		}
	}


	@Override
	protected void onResume() {
		super.onResume();

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
			indicator1.setOnClickListener(new View.OnClickListener() {
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
			});
			indicator1.setAlpha(0.5f);
			indicator2.setAlpha(0.5f);
			flickerAnimation1(indicator1);
			flickerAnimation2(indicator2);

			//Adding of content to SQL
			SQLiteStatement statement =
					database.compileStatement("INSERT INTO SavedClips (id,ClipTitle,ClipContent,UnixTimeLastSynced,Synced,Deleted) VALUES (? , ? , ?, ?, ?, ?)");

			//Title Shorten
			if (clipContent.length() > 125) {
				String shortTit = clipContent.substring(0, 125);
				shortTit = shortTit.concat("...");
				dSavedClipTitles.add(0, shortTit);
				savedClipTitles.add(0, shortTit);
				statement.bindString(2, shortTit);
			} else {
				dSavedClipTitles.add(0, clipContent);
				savedClipTitles.add(0, clipContent);
				statement.bindString(2, clipContent);
			}

			int id = savedClipID.size() > 0 ? savedClipID.get(0) + 1 : 1;
			//int id = sharedPreferences.getInt(LAST_INT, defaultValue);

			savedClipID.add(0, id);
			statement.bindLong(1, id);

			//sharedPreferences.edit().putInt(LAST_INT, sharedPreferences.getInt(LAST_INT, defaultValue) + 1).apply();

			dSavedClipContents.add(0, clipContent);
			savedClipContents.add(0, clipContent);
			statement.bindString(3, clipContent);

			unixTime.add(0, (long) 0);
			statement.bindLong(4, 0);
			syncedBoolean.add(0, 0);
			statement.bindLong(5, 0);
			deletedBoolean.add(0, 0);
			statement.bindLong(5, 0);

			statement.execute();
			syncRunner = new Utils.UnixTimeDownloader();
			syncRunner.execute("http://worldtimeapi.org/api/timezone/Etc/UTC");
			Log.d("SYNCED", "NOt Synced ");

			savedClipAdapter.notifyDataSetChanged();

			button.setText(R.string.saved);
			pushPinImage.setVisibility(View.GONE);

			countDownTimer.start();
		} else {
			Toast.makeText(MainActivity.this, R.string.empty_editbox, Toast.LENGTH_SHORT).show();
		}

	}


	public static void sync(Long unixTime) {
		SQLiteStatement statement =
				database.compileStatement("UPDATE SavedClips SET UnixTimeLastSynced = ?, Synced = 1 WHERE Synced = 0");
		Log.d("SYNCED", "SYNCED");


		while (MainActivity.unixTime.contains((long) 0)) {
			int i = MainActivity.unixTime.indexOf((long) 0);

			//Uploading to Firebase Database


			MainActivity.unixTime.add(i, unixTime);
			MainActivity.unixTime.remove(i + 1);

			DatabaseReference currentClip = savedClipRef.child(savedClipID.get(i).toString());
			currentClip.child("content").setValue(savedClipContents.get(i));
			currentClip.child("title").setValue(savedClipTitles.get(i));
			currentClip.child("unix time").setValue(MainActivity.unixTime.get(i));
			currentClip.child("deleted").setValue(deletedBoolean.get(i));

			syncedBoolean.add(i, 1);
			syncedBoolean.remove(i + 1);


		}

		statement.bindLong(1, unixTime);
		statement.execute();
		savedClipAdapter.notifyDataSetChanged();
	}


	// ANIMATIONS
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
	}


	public void indicatorClicked(View view) {
		drawerLayout.openDrawer(GravityCompat.START);
	}

	public void syncButtonClicked(View view) {

		syncRunner = new Utils.UnixTimeDownloader();

		if (!unixTime.isEmpty()) {
			syncRunner.execute("http://worldtimeapi.org/api/timezone/Etc/UTC");
			syncImage.startAnimation(AnimationUtils.loadAnimation(MainActivity.this, R.anim.rotate));
		}
	}

	public void logoutClicked(View view) {
		FirebaseAuth.getInstance().signOut();
		Intent intent = new Intent(MainActivity.this, LoginActivity.class);
		intent.putExtra(main_activity_intent, true);
		sharedPreferences.edit().putString(LAST_UUID, currentUser.getUid()).apply();
		startActivity(intent);

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

		Query idOrdered = savedClipRef.orderByKey();

		idOrdered.addListenerForSingleValueEvent(new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

				SQLiteStatement statement =
						database.compileStatement("INSERT INTO SavedClips (id,ClipTitle,ClipContent,UnixTimeLastSynced,Synced,Deleted) VALUES (? , ? , ?, ?, 1,?)");

				for (DataSnapshot savedClips : dataSnapshot.getChildren()) {
					statement.bindLong(1, Long.parseLong(savedClips.getKey()));
					statement.bindString(2, savedClips.child("title").getValue().toString());
					statement.bindString(3, savedClips.child("content").getValue().toString());
					statement.bindLong(4, (long) savedClips.child("unix time").getValue());
					statement.bindLong(5, (long) savedClips.child("deleted").getValue());
					statement.execute();

					syncedBoolean.add(0, 1);
					unixTime.add(0, (long) savedClips.child("unix time").getValue());
					savedClipID.add(0, Integer.parseInt(savedClips.getKey()));
					dSavedClipContents.add(0, savedClips.child("content").getValue().toString());
					dSavedClipTitles.add(0, savedClips.child("title").getValue().toString());
					deletedBoolean.add(0, Integer.parseInt(savedClips.child("deleted").getValue().toString()));

				}

				savedClipTitles.addAll(dSavedClipTitles);
				savedClipContents.addAll(dSavedClipContents);
				ArrayList<Integer> dDeletedBoolean = new ArrayList<>(deletedBoolean);
				//If Clip Deleted
				System.out.println(dSavedClipContents.size());
				while (dDeletedBoolean.contains(1)) {
					int i = dDeletedBoolean.indexOf(1);

					dSavedClipContents.remove(i);
					dSavedClipTitles.remove(i);
					dDeletedBoolean.remove(i);

				}
				dDeletedBoolean.clear();
				savedClipAdapter.notifyDataSetChanged();


			}

			@Override
			public void onCancelled(@NonNull DatabaseError databaseError) {

			}
		});

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

	private void openPowerSettings(Context context) {
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
	}

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
			Toast.makeText(MainActivity.this, R.string.empty_editbox_share, Toast.LENGTH_SHORT).show();
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
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			mode.getMenuInflater().inflate(R.menu.listview_multiselect_menu, menu);
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

									SQLiteStatement statement =
											database.compileStatement("UPDATE SavedClips SET UnixTimeLastSynced = 0, Synced = 0, Deleted = 1 WHERE id = ?");

									// Calls getSelectedIds method from ListViewAdapter Class
									SparseBooleanArray selected = savedClipAdapter.getSelectedIds();
									// Captures all selected ids with a loop
									for (int i = (selected.size() - 1); i >= 0; i--) {
										if (selected.valueAt(i)) {
											// Remove selected items following the ids
											//savedClipAdapter.remove(selected.keyAt(i));
											int s = selected.keyAt(i);

											statement.bindLong(1,savedClipID.get(s));
											statement.execute();

											dSavedClipTitles.remove(s);
											dSavedClipContents.remove(s);

											syncedBoolean.add(s, 0);
											syncedBoolean.remove(s + 1);

											unixTime.add(s, (long) 0);
											unixTime.remove(s + 1);

											deletedBoolean.add(s, 1);
											deletedBoolean.remove(s + 1);

											syncRunner = new Utils.UnixTimeDownloader();
											syncRunner.execute("http://worldtimeapi.org/api/timezone/Etc/UTC");

										}
									}
									// Close CAB
									mode.finish();
								}
							}).show();
					return true;
				default:
					return false;

			}
		}


		@Override
		public void onDestroyActionMode(ActionMode mode) {
			savedClipAdapter.removeSelection();
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false;
		}
	};
}



