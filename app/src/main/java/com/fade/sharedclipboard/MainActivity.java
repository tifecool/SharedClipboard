package com.fade.sharedclipboard;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

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


		//Toolbar Creation
		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

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
}


