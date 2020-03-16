package com.fade.sharedclipboard;

class ActivityVisibility {
	static boolean isActivityVisible() {
		return activityVisible;
	}

	static void activityResumed() {
		activityVisible = true;
	}

	static void activityPaused() {
		activityVisible = false;
	}

	private static boolean activityVisible;
}
