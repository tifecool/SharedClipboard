package com.fade.sharedclipboard;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class LoginActivity extends AppCompatActivity {
	final static int RC_SIGN_IN = 9;

	Intent intent;
	private int signupClickInt;
	private GoogleSignInClient mGoogleSignInClient;
	private FirebaseAuth mAuth;
	private DatabaseReference databaseUsersReference = FirebaseDatabase.getInstance().getReference().child("users");
	private EditText emailText;
	private EditText passwordText;
	private Button signInButton;
	private Button signUpButton;
	boolean programmaticSwitch = false;
	private SignInButton googleButton;
	private EditText confPass;

	private TextView emailReqText;
	private TextView passReqText;
	private TextView confPassReqText;
	private View dividerEmail;
	private View dividerConfPass;
	private View dividerPass;


	//SignInByEmail
	View.OnClickListener signInClick = new View.OnClickListener() {
		@Override
		public void onClick(View v) {

			blankPass(false);
			blankEmail(false);

			if (!emailText.getText().toString().isEmpty() && !passwordText.getText().toString().isEmpty()) {

				disableButtons(true);

				mAuth.signInWithEmailAndPassword(emailText.getText().toString(), passwordText.getText().toString())
						.addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {

							@Override
							public void onComplete(@NonNull Task<AuthResult> task) {
								if (task.isSuccessful()) {
									startActivity(intent);
									finish();
								} else {
									try {
										throw task.getException();

									} catch (FirebaseAuthInvalidCredentialsException e) {
										if (e.getErrorCode().equals("ERROR_INVALID_EMAIL")) {
											Toast.makeText(LoginActivity.this, getString(R.string.invalid_email_format), Toast.LENGTH_SHORT).show();
											emailText.requestFocus();
										} else {
											Toast.makeText(LoginActivity.this, getString(R.string.incorrect_password), Toast.LENGTH_SHORT).show();
											passwordText.requestFocus();
										}

										Log.i("ERROR", e.getErrorCode());

									} catch (FirebaseAuthInvalidUserException e) {
										Toast.makeText(LoginActivity.this, getString(R.string.email_inexistent), Toast.LENGTH_SHORT).show();
										emailText.requestFocus();

									} catch (Exception e) {
										Toast.makeText(LoginActivity.this, R.string.login_failed, Toast.LENGTH_SHORT).show();
										Log.w("Failed", "signInWithCredential:failure");
										e.printStackTrace();
									}
									Log.i("ERROR", task.getException().toString());
								}

								disableButtons(false);
							}
						});
			} else {
				if (passwordText.getText().toString().isEmpty() && emailText.getText().toString().isEmpty()) {

					blankPass(true);
					blankEmail(true);

				} else if (passwordText.getText().toString().isEmpty()) {
					blankPass(true);

				} else if (emailText.getText().toString().isEmpty()) {
					blankEmail(true);

				}
			}
		}
	};


	View.OnClickListener cancelClick = new View.OnClickListener() {
		@Override
		public void onClick(View v) {

			signupClickInt = 0;
			blankPass(false);
			blankEmail(false);
			unMatchedPass(false);

			confPass.setVisibility(View.GONE);

			signInButton.setText(R.string.sign_in);
			signInButton.setOnClickListener(signInClick);

		}
	};
	private SharedPreferences settingsPreferences;
	private Switch switchButton;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);

		signupClickInt = 0;

		dividerConfPass = findViewById(R.id.dividerConfPass);
		confPassReqText = findViewById(R.id.confPassReqText);
		dividerEmail = findViewById(R.id.dividerEmail);
		emailReqText = findViewById(R.id.emailReqText);
		dividerPass = findViewById(R.id.dividerPass);
		passReqText = findViewById(R.id.passReqText);
		switchButton = findViewById(R.id.switch1);

		mAuth = FirebaseAuth.getInstance();
		emailText = findViewById(R.id.emailText);
		passwordText = findViewById(R.id.passwordText);
		signInButton = findViewById(R.id.signIn);
		signUpButton = findViewById(R.id.signUp);
		googleButton = findViewById(R.id.googleButton);
		confPass = findViewById(R.id.confPassText);

		intent = new Intent(this, MainActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);

		signInButton.setOnClickListener(signInClick);


		//SignInWithGoogle
		GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
				.requestIdToken(getString(R.string.default_web_client_id))
				.requestEmail()
				.build();

		mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

		Intent intent = getIntent();
		if (intent.getBooleanExtra(MainActivity.MAIN_ACTIVITY_INTENT, false)) {
			mGoogleSignInClient.signOut();
		}

		GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);

		if (account != null) {
			firebaseAuthWithGoogle(account);
		}

		//Google Button OnclickListener
		googleButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent signInIntent = mGoogleSignInClient.getSignInIntent();
				disableButtons(true);
				startActivityForResult(signInIntent, RC_SIGN_IN);
			}
		});

		//Hides Keyboard when background is clicked
		findViewById(R.id.bgLayout).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
				View focusedView = getCurrentFocus();

				if (focusedView != null) {
					assert inputMethodManager != null;
					inputMethodManager.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
				}
			}
		});


		//When enter is clicked, continues
		passwordText.setOnKeyListener(new View.OnKeyListener() {
			@Override
			public boolean onKey(View view, int i, KeyEvent keyEvent) {

				if (keyEvent.getAction() == KeyEvent.ACTION_DOWN && i == KeyEvent.KEYCODE_ENTER && signupClickInt == 0) {
					InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
					View focusedView = getCurrentFocus();

					if (focusedView != null) {
						assert inputMethodManager != null;
						inputMethodManager.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
					}
					signInClick.onClick(signInButton);
				}
				return false;
			}
		});

		//When enter is clicked, continues
		confPass.setOnKeyListener(new View.OnKeyListener() {
			@Override
			public boolean onKey(View view, int i, KeyEvent keyEvent) {
				if (keyEvent.getAction() == KeyEvent.ACTION_DOWN && i == KeyEvent.KEYCODE_ENTER && signupClickInt == 1) {
					InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
					View focusedView = getCurrentFocus();

					if (focusedView != null) {
						assert inputMethodManager != null;
						inputMethodManager.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
					}
					signUpClicked(null);
				}
				return false;
			}
		});

		settingsPreferences = PreferenceManager.getDefaultSharedPreferences(this);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
			switchButton.setVisibility(View.GONE);
		}else{
			switchButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
					if (!programmaticSwitch) {
						Log.d("TAG", "onCheckedChanged: SWITCHED ");
						if (b) {
							AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
						} else {
							AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
						}
						settingsPreferences.edit().putBoolean("dark_mode", b).apply();
					} else {
						programmaticSwitch = false;
					}

				}
			});
		}


	}



	@Override
	protected void onStart() {
		super.onStart();

		if (mAuth.getCurrentUser() != null) {
			startActivity(intent);
			finish();
		}

		mAuth.signOut();
	}

	private void disableButtons(boolean bool) {
		signUpButton.setEnabled(!bool);
		signInButton.setEnabled(!bool);
		googleButton.setEnabled(!bool);
	}

	//SignUp
	public void signUpClicked(View view) {

		blankPass(false);
		blankEmail(false);
		unMatchedPass(false);

		if (signupClickInt == 0) {
			confPass.setVisibility(View.VISIBLE);
			confPass.setAlpha(0f);
			confPass.animate().alpha(1f).setDuration(1000);

			signInButton.setText(R.string.cancel);

			signInButton.setOnClickListener(cancelClick);
		}


		if (signupClickInt == 1) {

			if (!passwordText.getText().toString().isEmpty() && !emailText.getText().toString().isEmpty()) {
				if (passwordText.getText().toString().equals(confPass.getText().toString())) {

					disableButtons(true);

					mAuth.createUserWithEmailAndPassword(emailText.getText().toString(), passwordText.getText().toString())
							.addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {

								@Override
								public void onComplete(@NonNull Task<AuthResult> task) {
									if (task.isSuccessful()) {

										String userUid = mAuth.getCurrentUser().getUid();
										databaseUsersReference.child(userUid).child("email").setValue(mAuth.getCurrentUser().getEmail());
										startActivity(intent);
										finish();

									} else {
										try {
											throw task.getException();

										} catch (FirebaseAuthUserCollisionException e) {
											Toast.makeText(LoginActivity.this, getString(R.string.email_used), Toast.LENGTH_SHORT).show();
											emailText.requestFocus();

										} catch (FirebaseAuthWeakPasswordException e) {
											Toast.makeText(LoginActivity.this, getString(R.string.short_password), Toast.LENGTH_SHORT).show();
											passwordText.requestFocus();

										} catch (FirebaseAuthInvalidCredentialsException e) {
											Toast.makeText(LoginActivity.this, getString(R.string.invalid_email_format), Toast.LENGTH_SHORT).show();
											emailText.requestFocus();

										} catch (Exception e) {
											Toast.makeText(LoginActivity.this, R.string.signup_failed, Toast.LENGTH_SHORT).show();
											Log.w("Failed", "signInWithCredential:failure");
											e.printStackTrace();
										}
									}

									disableButtons(false);
								}
							});
				} else {
					unMatchedPass(true);
				}
			} else {
				if (passwordText.getText().toString().isEmpty() && emailText.getText().toString().isEmpty()) {

					blankPass(true);
					blankEmail(true);

				} else if (passwordText.getText().toString().isEmpty()) {
					blankPass(true);

				} else if (emailText.getText().toString().isEmpty()) {
					blankEmail(true);

				}

			}
		}

		if (signupClickInt == 0) {
			signupClickInt = 1;
		}

	}


	@Override
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
					firebaseAuthWithGoogle(account);
				} else {
					disableButtons(false);
				}
			} catch (ApiException e) {
				// The ApiException status code indicates the detailed failure reason.
				// Please refer to the GoogleSignInStatusCodes class reference for more information.
				Toast.makeText(LoginActivity.this, R.string.login_failed, Toast.LENGTH_SHORT).show();
				Log.w("Failed", "signInWithCredential:failure", e);

				disableButtons(false);

			}
		}
	}

	private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {

		AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);


		mAuth.signInWithCredential(credential)
				.addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {

					@Override
					public void onComplete(@NonNull Task<AuthResult> task) {
						if (task.isSuccessful()) {
							// Sign in success, update UI with the signed-in user's information
							Log.d("Success", "signInWithCredential:success");

							String userUid = mAuth.getCurrentUser().getUid();
							databaseUsersReference.child(userUid).child("email").setValue(mAuth.getCurrentUser().getEmail());
							startActivity(intent);
							finish();
						} else {
							// If sign in fails, display a message to the user.
							Toast.makeText(LoginActivity.this, R.string.login_failed, Toast.LENGTH_SHORT).show();
							Log.w("Failed", "signInWithCredential:failure", task.getException());

							if (mGoogleSignInClient != null) {
								mGoogleSignInClient.signOut();
							}
						}

						disableButtons(false);
					}
				});
	}

	private void blankPass(boolean blank) {
		if (blank) {
			passReqText.setVisibility(View.VISIBLE);
			dividerPass.setVisibility(View.VISIBLE);
		} else {
			passReqText.setVisibility(View.GONE);
			dividerPass.setVisibility(View.GONE);
		}
	}

	private void blankEmail(boolean blank) {
		if (blank) {
			emailReqText.setVisibility(View.VISIBLE);
			dividerEmail.setVisibility(View.VISIBLE);
		} else {
			emailReqText.setVisibility(View.GONE);
			dividerEmail.setVisibility(View.GONE);
		}
	}

	private void unMatchedPass(boolean unMatched) {
		if (unMatched) {
			confPassReqText.setVisibility(View.VISIBLE);
			dividerConfPass.setVisibility(View.VISIBLE);
		} else {
			confPassReqText.setVisibility(View.GONE);
			dividerConfPass.setVisibility(View.GONE);
		}
	}

}
