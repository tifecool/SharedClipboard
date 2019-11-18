package com.fade.sharedclipboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

	//TODO:	Setup basic sign up then firebase based.
	Intent intent;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);

		intent = new Intent(this,MainActivity.class);

	}

	public void signUpClicked(View view){
		startActivity(intent);
		finish();
	}

	public void signInClicked(View view){
		startActivity(intent);
		finish();
	}
}
