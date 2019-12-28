package com.fade.sharedclipboard;

import android.animation.Animator;
import android.app.ActivityManager;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

	/*TODO:
	   		 Dialogue box pop up when saved clip clicked
			 Multi select when long pressed
			 UPDATE APP SCREEN INCASE OF ANY ISSUES AND UPDATE NEEDED!
	 */
	//public static final String LAST_INT = "lastInt";
	public static final String LAST_UUID = "lastUUID";

	private static FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
	private static DatabaseReference savedClipRef = FirebaseDatabase.getInstance().getReference().child("users").child(currentUser.getUid()).child("saved clips");

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
	private static ArrayList<Integer> savedClipID = new ArrayList<>();
	private static ArrayList<String> savedClipTitles = new ArrayList<>();
	private static ArrayList<String> savedClipContents = new ArrayList<>();
	private static ArrayAdapter savedClipAdapter;
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


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		//Database Initialization and creation
		database = this.openOrCreateDatabase("SAVED CLIPS", MODE_PRIVATE, null);
		sharedPreferences = this.getSharedPreferences("LastIntUsed", MODE_PRIVATE);

		//database.execSQL("DROP TABLE IF EXISTS SavedClips");

		database.execSQL("CREATE TABLE IF NOT EXISTS SavedClips (id INT PRIMARY KEY, ClipTitle VARCHAR, ClipContent VARCHAR, UnixTimeLastSynced INT, Synced INT)");

		if (differentUserLoggedIn() || sqlIsEmpty(database, "SavedClips")) {
			database.execSQL("DELETE FROM SavedClips");
			updateSqlFromFirebase();
		}

		try {
			Cursor c = database.rawQuery("SELECT * FROM SavedClips", null);

			int unixTimeIndex = c.getColumnIndex("UnixTimeLastSynced");
			int syncedIndex = c.getColumnIndex("Synced");
			int clipIDIndex = c.getColumnIndex("id");
			int clipTitleIndex = c.getColumnIndex("ClipTitle");
			int clipContentIndex = c.getColumnIndex("ClipContent");

			if (c.moveToFirst()) {
				do {
					syncedBoolean.add(0, c.getInt(syncedIndex));
					unixTime.add(0, c.getLong(unixTimeIndex));
					savedClipID.add(0, c.getInt(clipIDIndex));
					savedClipContents.add(0, c.getString(clipContentIndex));
					savedClipTitles.add(0, c.getString(clipTitleIndex));
				} while (c.moveToNext());
			}

			c.close();
		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(MainActivity.this, R.string.reading_storage, Toast.LENGTH_LONG).show();
		}


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

		syncImage = findViewById(R.id.syncImage);
		indicator1 = findViewById(R.id.indicator1);
		indicator2 = findViewById(R.id.indicator2);
		pushPinImage = findViewById(R.id.pushPinImage);
		editText = findViewById(R.id.editbox);
		button = findViewById(R.id.saveButton);
		drawerLayout = findViewById(R.id.drawer_layout);

		//DRAWERListener
		drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
			@Override
			public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {

			}

			@Override
			public void onDrawerOpened(@NonNull View drawerView) {
				InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
				inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
			}

			@Override
			public void onDrawerClosed(@NonNull View drawerView) {
				InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
				inputMethodManager.showSoftInput(editText, 0);
			}

			@Override
			public void onDrawerStateChanged(int newState) {

			}
		});


		flickerAnimation1(indicator1);
		flickerAnimation2(indicator2);

		//Clipboard Service check and startup
		ClipboardListenerService clipboardService = new ClipboardListenerService();
		Intent serviceIntent = new Intent(this, clipboardService.getClass());

		if (!serviceRunning(clipboardService.getClass())) {
			startService(serviceIntent);
		}

		//List Creation
		ListView navList = findViewById(R.id.navListView);

		//Creates a List adapter, List views use them to set content
		savedClipAdapter = new CustomListAdapter(this, savedClipTitles, syncedBoolean);

		//Sets adapter
		navList.setAdapter(savedClipAdapter);

		//When an Item is clicked run below
		navList.setOnItemClickListener(
				new AdapterView.OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
						//Gets String from Item in position i that was clicked
						String toToaster = savedClipContents.get(i);

						//Creates a toast pop up
						Toast.makeText(MainActivity.this, toToaster, Toast.LENGTH_SHORT).show();

					}
				}
		);

		sharedPreferences.edit().putString(LAST_UUID,currentUser.getUid()).apply();
	}


	@Override
	protected void onResume() {
		super.onResume();

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

		InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
		inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);

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
					database.compileStatement("INSERT INTO SavedClips (id,ClipTitle,ClipContent,UnixTimeLastSynced,Synced) VALUES (? , ? , ?, ?, ?)");

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

			int id = savedClipID.size() > 0 ? savedClipID.get(savedClipID.size() - 1) + 1 : 1;
			//int id = sharedPreferences.getInt(LAST_INT, defaultValue);

			savedClipID.add(0, id);
			statement.bindLong(1, id);

			//sharedPreferences.edit().putInt(LAST_INT, sharedPreferences.getInt(LAST_INT, defaultValue) + 1).apply();

			savedClipContents.add(0, clipContent);
			statement.bindString(3, clipContent);

			unixTime.add(0, (long) 0);
			statement.bindLong(4, 0);
			syncedBoolean.add(0, 0);
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
//TODO
			DatabaseReference currentClip = savedClipRef.child(savedClipID.get(i).toString());
			currentClip.child("content").setValue(savedClipContents.get(i));
			currentClip.child("title").setValue(savedClipTitles.get(i));
			currentClip.child("unix time").setValue(MainActivity.unixTime.get(i));

			syncedBoolean.add(i, 1);
			syncedBoolean.remove(i + 1);


		}

		/*for(int i = MainActivity.unixTime.size() - 1; i >= 0 && MainActivity.unixTime.get(i) == 0; i--){
			MainActivity.unixTime.add(i,unixTime);
			MainActivity.unixTime.remove(i+1);

			syncedBoolean.add(i,1);
			syncedBoolean.remove(i+1);
		}*/

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
		InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
		inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
	}

	public void syncButtonClicked(View view) {

		InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
		inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);

		syncRunner = new Utils.UnixTimeDownloader();

		if (!unixTime.isEmpty()) {
			syncRunner.execute("http://worldtimeapi.org/api/timezone/Etc/UTC");
			syncImage.startAnimation(AnimationUtils.loadAnimation(MainActivity.this, R.anim.rotate));
		}
	}

	public void logoutClicked(View view) {
		FirebaseAuth.getInstance().signOut();
		startActivity(new Intent(MainActivity.this, LoginActivity.class));
		finish();
	}

	private boolean sqlIsEmpty(SQLiteDatabase database, String tableName) {

		Cursor mcursor = database.rawQuery("SELECT count(*) FROM " + tableName, null);
		mcursor.moveToFirst();
		int icount = mcursor.getInt(0);
		mcursor.close();
		return icount <= 0;
	}

	private void updateSqlFromFirebase(){
		savedClipRef.addChildEventListener(new ChildEventListener() {
			@Override
			public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

				SQLiteStatement statement =
						database.compileStatement("INSERT INTO SavedClips (id,ClipTitle,ClipContent,UnixTimeLastSynced,Synced) VALUES (? , ? , ?, ?, 1)");

				statement.bindLong(1,Long.parseLong(dataSnapshot.getKey()));
				statement.bindString(2,dataSnapshot.child("title").getValue().toString());
				statement.bindString(3,dataSnapshot.child("content").getValue().toString());
				statement.bindString(4,dataSnapshot.child("unix time").getValue().toString());

				statement.execute();
			}

			@Override
			public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

			}

			@Override
			public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

			}

			@Override
			public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

			}

			@Override
			public void onCancelled(@NonNull DatabaseError databaseError) {

			}
		});
	}

	private boolean differentUserLoggedIn(){
		return sharedPreferences.getString(LAST_UUID,"").equals(currentUser.getUid());
	}
}



