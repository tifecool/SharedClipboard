package com.fade.sharedclipboard;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import static com.fade.sharedclipboard.MainActivity.APP_SHARED_PREF;

public class IntroActivity extends AppCompatActivity {

	private ViewPager viewPager;
	private Button button;
	private SliderPagerAdapter adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

/*    // making activity full screen
    if (Build.VERSION.SDK_INT >= 21) {
      getWindow().getDecorView()
          .setSystemUiVisibility(
              View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }*/
		setContentView(R.layout.activity_intro);

		SharedPreferences sharedPreferences = this.getSharedPreferences(APP_SHARED_PREF, MODE_PRIVATE);
		sharedPreferences.edit().putBoolean(MainActivity.FIRST_LAUNCH, false).apply();

		viewPager = findViewById(R.id.pagerIntroSlider);
		TabLayout tabLayout = findViewById(R.id.tabs);
		button = findViewById(R.id.button);

		// init slider pager adapter
		adapter = new SliderPagerAdapter(getSupportFragmentManager(),
				FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);

		// set adapter
		viewPager.setAdapter(adapter);

		// set dot indicators
		tabLayout.setupWithViewPager(viewPager);

		// make status bar transparent
		changeStatusBarColor();

		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (viewPager.getCurrentItem() < adapter.getCount()) {
					viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
				}
			}
		});


		viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

			}

			@Override
			public void onPageSelected(int position) {
				if (position == adapter.getCount() - 1) {
					button.setText(R.string.get_started);
					button.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							Intent intent = new Intent(IntroActivity.this, MainActivity.class);
							intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

							startActivity(intent);
							finish();
						}
					});
				} else {
					button.setText(R.string.next);
					button.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View view) {
							if (viewPager.getCurrentItem() < adapter.getCount()) {
								viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
							}
						}
					});
				}
			}

			@Override
			public void onPageScrollStateChanged(int state) {

			}
		});
	}


	private void changeStatusBarColor() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			Window window = getWindow();
			window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			window.setStatusBarColor(Color.TRANSPARENT);
		}
	}
}
