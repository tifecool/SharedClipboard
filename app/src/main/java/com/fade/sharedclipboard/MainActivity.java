package com.fade.sharedclipboard;

import android.animation.Animator;
import android.app.ActivityManager;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

	/*TODO:  Make SQL Storage of SavedClips
			 Dialogue box pop up when saved clip clicked
			 Multi select when long pressed
			 UPDATE APP SCREEN INCASE OF ANY ISSUES AND UPDATE NEEDED!
	 */


	private ImageView indicator2;
	private ImageView indicator1;
	private ImageView pushPinImage;
	private EditText editText;
	private ClipboardListenerService clipboardListenerObj = new ClipboardListenerService();
	private ClipboardManager clipboard;
	private Button button;
	private ArrayList<String> savedClipTitles = new ArrayList<>();
	private ArrayList<String> savedClipContents = new ArrayList<>();
	private ArrayAdapter<String> savedClipTitleAdapter;
	private DrawerLayout drawerLayout;


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
		ListView navList = findViewById(R.id.navListView);

		//Creates a List adapter, List views use them to set content
		savedClipTitleAdapter = new CustomListAdapter(this, savedClipTitles);

		//Sets adapter
		navList.setAdapter(savedClipTitleAdapter);

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


			//Title Shorten
			if (clipContent.length() > 125) {
				String shortTit = clipContent.substring(0, 125);
				shortTit = shortTit.concat("...");
				savedClipTitles.add(shortTit);
			} else {
				savedClipTitles.add(clipContent);
			}

			savedClipContents.add(clipContent);
			savedClipTitleAdapter.notifyDataSetChanged();
			Log.i("Lenght of String", "" + clipContent.length());


			button.setText(R.string.saved);
			pushPinImage.setVisibility(View.GONE);


			countDownTimer.start();
		}else{
			Toast.makeText(MainActivity.this,R.string.empty_editbox,Toast.LENGTH_SHORT).show();
		}

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

}


