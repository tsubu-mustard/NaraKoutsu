package jp.gr.java_conf.umemilab.narakoutsu;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;

public class Orientation {
	static void lockOrientation(Activity a) {
		Configuration config = a.getResources().getConfiguration();
		switch(config.orientation) {
			case Configuration.ORIENTATION_LANDSCAPE:
				a.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
				break;
			case Configuration.ORIENTATION_PORTRAIT:
				a.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
				break;			
			default:
				;
		}
	}
	
	static void unlockOrientation(Activity a) {
		a.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
	}
}
