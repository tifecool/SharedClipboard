package com.fade.sharedclipboard;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

public class SliderItemFragment extends Fragment {

	private static final String ARG_POSITION = "slider-position";

	@StringRes
	private static final int[] WELCOME_TEXT =
			new int[]{R.string.welcome_to, R.string.empty, R.string.empty, R.string.empty};


	// prepare all page text ids arrays
	@StringRes
	private static final int[] PAGE_TEXT =
			new int[]{
					R.string.first_page_text, R.string.second_page_text, R.string.third_page_text, R.string.fourth_page_text
			};

	// prepare all center images arrays
	@StringRes
	private static final int[] CENTER_IMAGE =
			new int[]{
					R.drawable.transparent, R.drawable.notification, R.drawable.pinned_clips, R.drawable.chrome_extension
			};

	@StringRes
	private static final int[] CENTER_ICON =
			new int[]{
					R.drawable.sc_icon, R.drawable.transparent, R.drawable.transparent, R.drawable.transparent
			};

	@StringRes
	private static final int[] TOP_ICON =
			new int[]{
					R.drawable.transparent, R.drawable.sc_icon, R.drawable.sc_icon, R.drawable.sc_icon
			};


	private int position;

	public SliderItemFragment() {
		// Required empty public constructor
	}

	static SliderItemFragment newInstance(int position) {
		SliderItemFragment fragment = new SliderItemFragment();
		Bundle args = new Bundle();
		args.putInt(ARG_POSITION, position);
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getArguments() != null) {
			position = getArguments().getInt(ARG_POSITION);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		return inflater.inflate(R.layout.fragment_slider_item, container, false);
	}

	@RequiresApi(api = Build.VERSION_CODES.KITKAT)
	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		// set page background
		view.setBackground(getResources().getDrawable(R.drawable.ic_bg_gray));

		ImageView imageView = view.findViewById(R.id.centerIcon);
		TextView welcomeText = view.findViewById(R.id.firstText);
		TextView sharedTitle = view.findViewById(R.id.secondTitle);
		TextView bottomText = view.findViewById(R.id.bottomText);
		ImageView imageView2 = view.findViewById(R.id.centerImage);
		ImageView imageView3 = view.findViewById(R.id.topIcon);


		welcomeText.setText(WELCOME_TEXT[position]);
		sharedTitle.setText(R.string.shared_clipboard);
		bottomText.setText(PAGE_TEXT[position]);
		imageView2.setImageResource(CENTER_IMAGE[position]);
		imageView.setImageResource(CENTER_ICON[position]);
		imageView3.setImageResource(TOP_ICON[position]);

		Animation topToBottom = AnimationUtils.loadAnimation(getContext(), R.anim.image_anim);
		imageView.setAnimation(topToBottom);


	}
}
