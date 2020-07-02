package com.fade.sharedclipboard;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import static com.fade.sharedclipboard.ClipboardListenerService.currentUser;
import static com.fade.sharedclipboard.MainActivity.DONT_SHOW_CHECK;

class Utils {

	static void openPowerSettings(LayoutInflater layoutInflater, final SharedPreferences sharedPreferences, final Context context) {
		final int[] bat_counter = {0};
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

			final View view = layoutInflater.inflate(R.layout.power_settings_dialogue, null);
			final ImageView batImage = view.findViewById(R.id.batImage);
			final CheckBox checkBox = view.findViewById(R.id.checkBox);
			checkBox.setChecked(sharedPreferences.getBoolean(DONT_SHOW_CHECK, false));

			batImage.setImageResource(R.drawable.battery_opti_1);

			AlertDialog.Builder alertBuild = new AlertDialog.Builder(context)
					.setView(view)
					.setTitle(R.string.bat_opti)
					.setNegativeButton(R.string.cancel, null)
					.setPositiveButton(R.string.next, null);

			final AlertDialog alert = alertBuild.create();
			alert.show();

			alert.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					switch (bat_counter[0]) {
						case 0:
							batImage.setImageResource(R.drawable.battery_opti_2);
							bat_counter[0]++;
							break;
						case 1:
							batImage.setImageResource(R.drawable.battery_opti_3_3);
							alert.getButton(DialogInterface.BUTTON_POSITIVE).setText(R.string.done);
							alert.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
								@Override
								public void onClick(View v) {
									Intent intent = new Intent();
									intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
									context.startActivity(intent);
									alert.dismiss();
								}
							});
							bat_counter[0]++;
							break;
						default:
							bat_counter[0] = 0;
							break;
					}
				}
			});

			checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					sharedPreferences.edit().putBoolean(DONT_SHOW_CHECK, isChecked).apply();
					if (isChecked) {
						alert.dismiss();
					}
				}
			});
		}
	}

	public static class UnixTimeDownloader extends AsyncTask<String, Void, String> {

		Long unixTime = null;

		private Long getUnixTime() {
			return unixTime;
		}

		private void setUnixTime(Long unixTime) {
			this.unixTime = unixTime;
		}

		@Override
		protected String doInBackground(String... urls) {
			StringBuilder string = new StringBuilder();

			try {

				URL url = new URL(urls[0]);
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				InputStream is = connection.getInputStream();
				InputStreamReader reader = new InputStreamReader(is);
				int maxBytesToRead = 400; //no of bytes read at once
				char[] currentChars = new char[maxBytesToRead]; // Array holds currently read bytes
				int data = reader.read(currentChars);//Stores bytes in array

				while (data != -1) {
					string.append(currentChars, 0, data);//adds bytes to string, no offset, how many chars to be added
					data = reader.read(currentChars);

					if (string.length() > 600)//Check is particular to this app
						return null;
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return string.toString();
		}

		@Override
		protected void onPostExecute(String jsonData) {

			try {
				JSONObject jsonObject = new JSONObject(jsonData);
				setUnixTime(jsonObject.getLong("unixtime"));

				//Sends UnixTime to a sync method in MainActivity
				MainActivity.sync(getUnixTime());

			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}

	boolean handlePurchase(Purchase purchase, BillingClient billingClient, final Context context) {

		DatabaseReference removeAds = FirebaseDatabase.getInstance().getReference().child("users").child(currentUser.getUid()).child("remove ads");

		removeAds.child("purchaseToken").setValue(purchase.getPurchaseToken());
		removeAds.child("orderTime").setValue(purchase.getPurchaseTime());
		removeAds.child("state").setValue(purchase.getPurchaseState());

		if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {

			ConsumeParams consumeParams =
					ConsumeParams.newBuilder()
							.setPurchaseToken(purchase.getPurchaseToken())
							.build();

			ConsumeResponseListener listener = new ConsumeResponseListener() {
				@Override
				public void onConsumeResponse(@NonNull BillingResult billingResult, @NonNull String purchaseToken) {
					try {
						Toast.makeText(context, R.string.successful_purchase, Toast.LENGTH_SHORT).show();
					}catch (Exception e){
						e.printStackTrace();
					}
				}
			};

			billingClient.consumeAsync(consumeParams, listener);
			return true;
		} else {
			try {
				Toast.makeText(context, R.string.purchase_pending, Toast.LENGTH_SHORT).show();
			} catch (Resources.NotFoundException e) {
				e.printStackTrace();
			}
			return false;
		}
	}

/*
	public Object getJsonValue(String jsonString){
		PageDownloader downloader = new PageDownloader();
		Object object = null;
		try {

			String jsonData = downloader.execute("http://worldtimeapi.org/api/timezone/Etc/UTC").get();
			if(jsonData == null) //Check is particular to this app
				return null;

			JSONObject jsonObject = new JSONObject(jsonData);
			object = jsonObject.getLong(jsonString);

		} catch (ExecutionException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return object;
	}
*/

/*Toast toast = Toast.makeText(MainActivity.this, userEmail, Toast.LENGTH_SHORT);
		toast.setGravity(Gravity.TOP, view.getLeft() - view.getWidth() / 2 - toast.getView().getWidth() / 2, view.getBottom());
		toast.show();*/

	/*private boolean sqlIsEmpty(SQLiteDatabase database, String tableName) {

		Cursor mcursor = database.rawQuery("SELECT count(*) FROM " + tableName, null);
		mcursor.moveToFirst();
		int icount = mcursor.getInt(0);
		mcursor.close();
		return icount <= 0;
	}*/

	//VERY USEFUL CODE FOR REMOVING FOCUS ON EDITTEXT
	/*@Override
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
	}*/

	/*
	public static class PageDownloader extends AsyncTask<String, Void, String> {


		@Override
		protected String doInBackground(String... urls) {
			StringBuilder string = new StringBuilder();

			try {

				URL url = new URL(urls[0]);
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				InputStream is = connection.getInputStream();
				InputStreamReader reader = new InputStreamReader(is);
				int maxBytesToRead = 400; //no of bytes read at once
				char[] currentChars = new char[maxBytesToRead]; // Array holds currently read bytes
				int data = reader.read(currentChars);//Stores bytes in array

				while (data != -1) {
					string.append(currentChars,0,data);//adds bytes to string, no offset, how many chars to be added
					data = reader.read(currentChars);

				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return string.toString();
		}
	}
*/
}


