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
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.database.FirebaseDatabase;

import static com.fade.sharedclipboard.LoginActivity.RC_SIGN_IN;
import static com.fade.sharedclipboard.MainActivity.APP_SHARED_PREF;

public class SettingsActivity extends AppCompatActivity {


	private static Intent notificationIntent;
	private static PowerManager pm;
	private static String packageName;
	private static Intent fromMainActivity;
	private static SharedPreferences sharedPreferences;
	private static SQLiteDatabase database;
	private Preference linkGooglePref;
	private static FirebaseUser currentUser;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings_activity);

		sharedPreferences = this.getSharedPreferences(APP_SHARED_PREF, MODE_PRIVATE);
		fromMainActivity = getIntent();
		currentUser = FirebaseAuth.getInstance().getCurrentUser();

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
		private String TAG = "SettingsTag";

		@Override
		public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
			setPreferencesFromResource(R.xml.root_preferences, rootKey);

			Preference linkGooglePref = findPreference("google_pref");
			assert linkGooglePref != null;

			for (UserInfo userInfo : currentUser.getProviderData()) {
				if (userInfo.getProviderId().equals(GoogleAuthProvider.PROVIDER_ID)) {
					Log.d(TAG, "User is signed in with Google");
					linkGooglePref.setVisible(false);
					break;
				} else {
					linkGooglePref.setVisible(true);
				}
			}

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

												deleteStatement.bindString(1, c.getString(idIndex));
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

			linkGooglePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
							.requestIdToken(getString(R.string.default_web_client_id))
							.requestEmail()
							.build();

					GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(getContext(), gso);

					Intent signInIntent = mGoogleSignInClient.getSignInIntent();
					preference.setEnabled(false);
					startActivityForResult(signInIntent, RC_SIGN_IN);
					return true;
				}
			});

			Preference deleteAccountPref = findPreference("delete_account_pref");
			assert deleteAccountPref != null;

			deleteAccountPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {

					LayoutInflater inflated = getLayoutInflater();
					final View view = inflated.inflate(R.layout.delete_user_dialog, null);
					final EditText password = view.findViewById(R.id.passwordDialog);
					final EditText email = view.findViewById(R.id.emailDialog);
					final TextView passwordReq = view.findViewById(R.id.passwordDialogReqText);
					final TextView emailReq = view.findViewById(R.id.emailDialogReqText);

					final View.OnClickListener deleteDialogueClick = new View.OnClickListener() {
						@Override
						public void onClick(View view1) {

							passwordReq.setVisibility(View.GONE);
							emailReq.setVisibility(View.GONE);

							if (!email.getText().toString().isEmpty() && !password.getText().toString().isEmpty()) {

								AuthCredential credential = EmailAuthProvider.getCredential(email.getText().toString(), password.getText().toString());

								currentUser.reauthenticate(credential).addOnCompleteListener(new OnCompleteListener<Void>() {
									@Override
									public void onComplete(@NonNull Task<Void> task) {
										if (task.isSuccessful()) {
											FirebaseDatabase.getInstance().getReference().child("users").child(currentUser.getUid()).removeValue();
											currentUser.delete();

											Intent intent = new Intent(getContext(), LoginActivity.class);
											intent.putExtra(MainActivity.MAIN_ACTIVITY_INTENT, true);
											startActivity(intent);

											StartService.killedProg = true;
											getActivity().stopService(new Intent(getContext(), ClipboardListenerService.class));

											getActivity().finish();

										} else {
											try {
												throw task.getException();

											} catch (FirebaseAuthInvalidCredentialsException e) {
												if (e.getErrorCode().equals("ERROR_INVALID_EMAIL")) {
													Toast.makeText(getContext(), getString(R.string.invalid_email_format), Toast.LENGTH_SHORT).show();
													email.requestFocus();
												} else {
													Toast.makeText(getContext(), getString(R.string.incorrect_password), Toast.LENGTH_SHORT).show();
													password.requestFocus();
												}

												Log.i("ERROR", e.getErrorCode());

											} catch (FirebaseAuthInvalidUserException e) {
												email.requestFocus();

											} catch (Exception e) {
												Toast.makeText(getContext(), R.string.login_failed, Toast.LENGTH_SHORT).show();
												Log.w("Failed", "signInWithCredential:failure");
												e.printStackTrace();
											}
											Log.i("ERROR", task.getException().toString());
										}
									}
								});

							} else {
								if (password.getText().toString().isEmpty() && password.getText().toString().isEmpty()) {

									passwordReq.setVisibility(View.VISIBLE);
									emailReq.setVisibility(View.VISIBLE);

								} else if (password.getText().toString().isEmpty()) {
									passwordReq.setVisibility(View.VISIBLE);

								} else if (email.getText().toString().isEmpty()) {
									emailReq.setVisibility(View.VISIBLE);

								}
							}

						}
					};

					final AlertDialog dialog = new AlertDialog.Builder(getContext())
							.setTitle("Delete User")
							.setMessage("To confirm that this user should be deleted, please enter your email and password:")
							.setIcon(R.drawable.warning_bright)
							.setView(view)
							.setPositiveButton(R.string.delete, null)
							.setNegativeButton(R.string.cancel, null)
							.create();

					dialog.setOnShowListener(new DialogInterface.OnShowListener() {
						@Override
						public void onShow(DialogInterface dialogInterface) {
							dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(
									deleteDialogueClick
							);
						}
					});

					dialog.show();

					password.setOnKeyListener(new View.OnKeyListener() {
						@Override
						public boolean onKey(View view, int i, KeyEvent keyEvent) {

							if (keyEvent.getAction() == KeyEvent.ACTION_DOWN && i == KeyEvent.KEYCODE_ENTER) {
								InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(INPUT_METHOD_SERVICE);
								inputMethodManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
								deleteDialogueClick.onClick(dialog.getButton(DialogInterface.BUTTON_POSITIVE));
							}
							return false;
						}
					});

					return true;


				}


			});


		}

		public void onActivityResult(int requestCode, int resultCode, Intent data) {
			super.onActivityResult(requestCode, resultCode, data);

			// Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
			if (requestCode == RC_SIGN_IN) {
				// The Task returned from this call is always completed, no need to attach
				// a listener.
				Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);

				try {
					GoogleSignInAccount account = task.getResult(ApiException.class);

					// Signed in successfully, show authenticated UI.
					if (account != null) {
						AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
						currentUser.linkWithCredential(credential).addOnCompleteListener(getActivity(), new OnCompleteListener<AuthResult>() {
							@Override
							public void onComplete(@NonNull Task<AuthResult> task) {
								if (task.isSuccessful()) {
									Log.d(TAG, "linkWithCredential:success");
									Toast.makeText(getContext(), R.string.link_successful,
											Toast.LENGTH_SHORT).show();
									findPreference("google_pref").setVisible(false);
								} else {
									Log.w(TAG, "linkWithCredential:failure", task.getException());

									new AlertDialog.Builder(getContext())
											.setIcon(R.drawable.warning_bright)
											.setTitle(R.string.link_fail_title)
											.setMessage(R.string.link_fail_message)
											.setPositiveButton(R.string.ok, null)
											.show();
								}
							}
						});
						findPreference("google_pref").setEnabled(true);
					} else {
						findPreference("google_pref").setEnabled(true);
					}
				} catch (ApiException e) {
					// The ApiException status code indicates the detailed failure reason.
					// Please refer to the GoogleSignInStatusCodes class reference for more information.
					Toast.makeText(getContext(), R.string.login_failed, Toast.LENGTH_SHORT).show();
					Log.w("Failed", "signInWithCredential:failure");

					findPreference("google_pref").setEnabled(true);


				}
			}
		}

	}
}