package com.fade.sharedclipboard;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class UpdateRequiredActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_update_required);

		MainActivity object = new MainActivity();

		if(!object.updateRequired(this)){
			Intent intent = new Intent(UpdateRequiredActivity.this, MainActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);

			startActivity(intent);
		}
	}
}
