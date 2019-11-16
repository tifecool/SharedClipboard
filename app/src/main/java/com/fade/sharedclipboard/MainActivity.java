package com.fade.sharedclipboard;

import android.animation.Animator;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

	private ImageView indicator2;
	private ImageView indicator1;
	private ImageView pushPinImage;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		//TODO: MAKE INDICATOR 1 PLEASE DUMBASS
		indicator1 = findViewById(R.id.indicator1);
		indicator2 = findViewById(R.id.indicator2);
		pushPinImage = findViewById(R.id.pushPinImage);

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
		//holder is a List of strings
		ArrayList<String> holder = new ArrayList<>();

		//Adds List item #
		for(int i =0; i < 22 ; ++i){
			holder.add("List Item " + i);
		}

		//Creates a List adapter, List views use them to set content
		ListAdapter listAdapted = new CustomListAdapter(this, holder);

		//Sets adapter
		navList.setAdapter(listAdapted);

		//When an Item is clicked run below
		navList.setOnItemClickListener(
				new AdapterView.OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
						//Gets String from Item in position i that was clicked
						String toToaster = String.valueOf(adapterView.getItemAtPosition(i));

						//Creates a toast pop up
						Toast.makeText(MainActivity.this, toToaster, Toast.LENGTH_SHORT).show();

					}
				}
		);

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

	public void saveButtonClicked(View view){
		//TODO: ADD TIMER THAT SWITCHES BACK AFTER A SECOND

		indicator1.setAlpha(0.5f);
		indicator2.setAlpha(0.5f);
		flickerAnimation1(indicator1);
		flickerAnimation2(indicator2);

		Button button = (Button) view;

		button.setText("SAVED!");
		pushPinImage.setVisibility(View.GONE);
	}

	float alphaVal1;
	float alphaVal2;
	Animator.AnimatorListener listener1;
	Animator.AnimatorListener listener2;
	final int ANIM_DURATION = 2000;

	private void flickerAnimation1(final ImageView imageView){
		alphaVal1 = 0.1f;

		listener1 = new Animator.AnimatorListener() {
			@Override
			public void onAnimationStart(Animator animation) {

			}

			@Override
			public void onAnimationEnd(Animator animation) {
				if(alphaVal1 == 0.1f){
					alphaVal1 = 0.8f;
					imageView.animate().alpha(alphaVal1).setDuration(ANIM_DURATION).setListener(listener1);
				}else if(alphaVal1 == 0.8f){
					alphaVal1 = 0f;
					imageView.animate().alpha(alphaVal1).setDuration(ANIM_DURATION);
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

	private void flickerAnimation2(final ImageView imageView){
		alphaVal2 = 0.1f;

		listener2 = new Animator.AnimatorListener() {
			@Override
			public void onAnimationStart(Animator animation) {

			}

			@Override
			public void onAnimationEnd(Animator animation) {
				if(alphaVal2 == 0.1f){
					alphaVal2 = 0.8f;
					imageView.animate().alpha(alphaVal2).setDuration(ANIM_DURATION).setListener(listener2);
				}else if(alphaVal2 == 0.8f){
					alphaVal2 = 0f;
					imageView.animate().alpha(alphaVal2).setDuration(ANIM_DURATION);
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
}


