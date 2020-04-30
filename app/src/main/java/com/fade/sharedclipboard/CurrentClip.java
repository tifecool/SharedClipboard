package com.fade.sharedclipboard;

import android.util.Log;

interface CurrentClipListener {
	void onCurrentClipChanged(String currentClip);
}

class CurrentClip{

	private static String currentClip;
	private static CurrentClipListener currentClipListener;

	void setCurrentClipListener(CurrentClipListener currentClipListener) {
		CurrentClip.currentClipListener = currentClipListener;
	}

	static void setCurrentClip(String clip) {

		Log.d("TAG", "setCurrentClip: " + clip);
		if (currentClipListener != null)
			currentClipListener.onCurrentClipChanged(clip);
		currentClip = clip;
	}

	String getCurrentClip() {
		return currentClip;
	}
}
