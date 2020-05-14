package com.fade.sharedclipboard;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import static com.fade.sharedclipboard.MainActivity.APP_SHARED_PREF;

public class SettingsActivity extends AppCompatActivity {


	private static Intent notificationIntent;
	private static PowerManager pm;
	private static String packageName;
	private static Intent fromMainActivity;
	private static SharedPreferences sharedPreferences;
	private static SQLiteDatabase database;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings_activity);

		sharedPreferences = this.getSharedPreferences(APP_SHARED_PREF, MODE_PRIVATE);
		fromMainActivity = getIntent();
		FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

		database = this.openOrCreateDatabase(MainActivity.SQL_DATABASE_NAME, MODE_PRIVATE, null);

		if (currentUser == null) {
			startActivity(new Intent(SettingsActivity.this, LoginActivity.class));

			StartService.killedProg = true;
			stopService(new Intent(SettingsActivity.this, ClipboardListenerService.class));

			finish();
		} else {
			pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);

			packageName = getPackageName();

			notificationIntent = new Intent();
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				notificationIntent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
				notificationIntent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
			} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				notificationIntent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
				notificationIntent.putExtra("app_package", getPackageName());
				notificationIntent.putExtra("app_uid", getApplicationInfo().uid);
			} else {
				notificationIntent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
				notificationIntent.addCategory(Intent.CATEGORY_DEFAULT);
				notificationIntent.setData(Uri.parse("package:" + getPackageName()));
			}


			getSupportFragmentManager()
					.beginTransaction()
					.replace(R.id.settings, new SettingsFragment())
					.commit();
			ActionBar actionBar = getSupportActionBar();
			if (actionBar != null) {
				actionBar.setDisplayHomeAsUpEnabled(true);
			}
		}
	}

	public void backClicked(View view) {
		onBackPressed();
	}

	public static class SettingsFragment extends PreferenceFragmentCompat {
		@Override
		public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
			setPreferencesFromResource(R.xml.root_preferences, rootKey);

			Preference loggedInUserPref = findPreference("logged_in_user_pref");
			assert loggedInUserPref != null;
			loggedInUserPref.setSummary(fromMainActivity.getStringExtra(MainActivity.USERS_EMAIL));

			Preference notificationSettingsPref = findPreference("notification_settings");
			assert notificationSettingsPref != null;
			notificationSettingsPref.setIntent(notificationIntent);

			Preference batteryOptimizationPref = findPreference("battery_optimization_pref");
			assert batteryOptimizationPref != null;

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				if (!pm.isIgnoringBatteryOptimizations(packageName)) {
					batteryOptimizationPref.setVisible(true);
				}
			}
			batteryOptimizationPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					Utils.openPowerSettings(getLayoutInflater(), sharedPreferences, getContext());
					return true;
				}
			});

			SwitchPreference alwaysRunningPref = findPreference("always_running_pref");
			assert alwaysRunningPref != null;

			alwaysRunningPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(final Preference preference, Object newValue) {

					if (!(boolean) newValue) {
						new AlertDialog.Builder(getContext())
								.setTitle(R.string.alert_sure)
								.setMessage(R.string.always_running_warning)
								.setIcon(R.drawable.warning_bright)
								.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {

									}
								})
								.setPositiveButton(R.string.accept, new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										SwitchPreference switchPreference = (SwitchPreference) preference;
										switchPreference.setChecked(false);
									}
								}).show();
						return false;
					} else {
						return true;
					}

				}
			});

			Preference deleteAllPref = findPreference("delete_all_pref");
			assert deleteAllPref != null;

			deleteAllPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					try {
						final Cursor c = database.rawQuery("SELECT * FROM DeletedClips WHERE Deleted = 0 ORDER BY UnixTimeLastSynced ASC ", null);
						final int idIndex = c.getColumnIndex("id");

						if (c.moveToFirst()) {
							new AlertDialog.Builder(getContext())
									.setTitle(R.string.alert_sure)
									.setMessage("Are you sure you want to permanently delete all your deleted clips?")
									.setPositiveButton("Delete All", new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface dialog, int which) {

											do {
												SQLiteStatement deleteStatement =
														database.compileStatement("UPDATE DeletedClips SET Deleted = 1 WHERE id = ?");

												deleteStatement.bindString(1,c.getString(idIndex));
												deleteStatement.execute();

												int i = MainActivity.dSavedClipID.indexOf(c.getString(idIndex));

												MainActivity.dSavedClipID.remove(i);
												MainActivity.dSavedClipContents.remove(i);
												MainActivity.dSavedClipTitles.remove(i);
												MainActivity.dSyncedBoolean.remove(i);
												MainActivity.dUnixTime.remove(i);

											} while (c.moveToNext());

											c.close();


										}
									})
									.setNegativeButton(R.string.cancel, null)
									.show();

						} else {
							Toast.makeText(getContext(), "No Deleted Clips", Toast.LENGTH_SHORT).show();
						}

					} catch (Exception e) {
						e.printStackTrace();
					}

					return true;
				}
			});


		}

	}
}