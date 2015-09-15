package com.orangelabs.rcs.ri.meetasapcommon;

import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.meetasapa.MeetAsapContactsA;
import com.orangelabs.rcs.ri.meetasapb.MeetAsapContactsB;
import com.orangelabs.rcs.ri.meetasapc.MeetAsapContactsC;
import com.orangelabs.rcs.ri.meetasapd.MeetAsapContactsD;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

public class MeetAsapSplashScreen extends Activity {
	public final static String MEET_ASAP_VERSION = "C";
	private static int SPLASH_SCREEN_DELAY = 2500;
	Intent intent;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.meet_splash);

		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				if (MeetAsapSplashScreen.MEET_ASAP_VERSION == "A") {
					intent = new Intent(MeetAsapSplashScreen.this,
							MeetAsapContactsA.class);
					Log.d("meetAsapError", "versioning SplashScreen: " + MeetAsapSplashScreen.MEET_ASAP_VERSION );
				} else if (MeetAsapSplashScreen.MEET_ASAP_VERSION == "B") {
					intent = new Intent(MeetAsapSplashScreen.this,
							MeetAsapContactsB.class);
					Log.d("meetAsapError", "versioning SplashScreen: " + MeetAsapSplashScreen.MEET_ASAP_VERSION );
				} else if (MeetAsapSplashScreen.MEET_ASAP_VERSION == "C") {
					intent = new Intent(MeetAsapSplashScreen.this,
							MeetAsapContactsC.class);
					Log.d("meetAsapError", "versioning SplashScreen: " + MeetAsapSplashScreen.MEET_ASAP_VERSION );
				} else if (MeetAsapSplashScreen.MEET_ASAP_VERSION == "D") {
					intent = new Intent(MeetAsapSplashScreen.this,
							MeetAsapContactsD.class);
					Log.d("meetAsapError", "versioning SplashScreen: " + MeetAsapSplashScreen.MEET_ASAP_VERSION );
				} else {
					Log.d("meetAsapError", "versioning doesn't work - SplashScreen");
				}

				startActivity(intent);

				// Kills this Activity
				finish();
			}
		}, SPLASH_SCREEN_DELAY);
	}
}