package com.fade.sharedclipboard;

import android.os.AsyncTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

class Utils {

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

	public static class UnixTimeDownloader extends AsyncTask<String, Void, String> {

		Long unixTime = null;

		private Long getUnixTime(){
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
					string.append(currentChars,0,data);//adds bytes to string, no offset, how many chars to be added
					data = reader.read(currentChars);

					if(string.length()> 600)//Check is particular to this app
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
}


