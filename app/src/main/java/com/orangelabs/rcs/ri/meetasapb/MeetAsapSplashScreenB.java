package com.orangelabs.rcs.ri.meetasapb;


import com.orangelabs.rcs.ri.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class MeetAsapSplashScreenB extends Activity {

	private static int SPLASH_SCREEN_DELAY = 2500;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.meet_splash);

		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				Intent intent = new Intent(MeetAsapSplashScreenB.this, MeetAsapContactsB.class);
				startActivity(intent);

				// Kills this Activity
				finish();
			}
		}, SPLASH_SCREEN_DELAY);
	}
}