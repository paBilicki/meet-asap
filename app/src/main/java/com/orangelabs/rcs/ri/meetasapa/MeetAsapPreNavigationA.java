/**
 * List of RCS contacts who are online (i.e. registered)
 * 
 * @author Piotr Bilicki
 */
package com.orangelabs.rcs.ri.meetasapa;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.extension.MultimediaMessagingSession;
import com.gsma.services.rcs.extension.MultimediaMessagingSessionListener;
import com.gsma.services.rcs.extension.MultimediaSession;
import com.orangelabs.rcs.ri.ConnectionManager;
import com.orangelabs.rcs.ri.ConnectionManager.RcsServiceName;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.RiApplication;
import com.orangelabs.rcs.ri.extension.messaging.MessagingSessionUtils;
import com.orangelabs.rcs.ri.meetasapcommon.MeetAsapGpsTracker;
import com.orangelabs.rcs.ri.utils.LockAccess;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * This shows how to create a simple activity with a map and a marker on the
 * map.
 */
public class MeetAsapPreNavigationA extends FragmentActivity implements
		OnMapReadyCallback {
	TextView meetTextView, meetShow, meetReceived;
	Button refreshButton, sendButton;
	private ContactId mMeetContact;
	LatLng myPosition;
	double myLat, myLong, latitude = 1, longitude = 1;
	MeetAsapGpsTracker GPS;

	private ConnectionManager mCnxManager;
	private LockAccess mExitOnce = new LockAccess();
	private final Handler mHandler = new Handler();
	private static final String LOGTAG = LogUtils
			.getTag(MeetAsapPreNavigationA.class.getSimpleName());
	private String mSessionId;
	private MultimediaMessagingSession mSession;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.meet_prenavigation_screen);

		SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
				.findFragmentById(R.id.map);
		mapFragment.getMapAsync(this);

		refreshButton = (Button) findViewById(R.id.meet_refresh);
		sendButton = (Button) findViewById(R.id.meet_send);

		refreshButton.setOnClickListener(refreshButtonListener);
		sendButton.setOnClickListener(sendButtonListener);

		meetTextView = (TextView) findViewById(R.id.meet_remote);
		meetShow = (TextView) findViewById(R.id.meet_my);
		meetReceived = (TextView) findViewById(R.id.meet_received);
		
		Log.d("meetAsapError", "trying to create gps");
		GPS = new MeetAsapGpsTracker(MeetAsapPreNavigationA.this);
		if (GPS.canGetLocation()) {
			latitude = GPS.getLatitude();
			longitude = GPS.getLongitude();
			}
		Log.d("meetAsapError", "gps created");
		myPosition = new LatLng(latitude, longitude);
		

		Log.d("meetAsapError", "getting extras");


//			String mode = getIntent().getStringExtra(
//					MeetAsapOptionsA.EXTRA_TRANSPORT_MODE);

			mMeetContact = getIntent().getParcelableExtra(
					MeetAsapContactsA.EXTRA_CONTACT);
			
			meetTextView.setText("Remote Contact: " + mMeetContact.toString());
			Log.d("meetAsapError", "extras taken");
			
			mCnxManager = ConnectionManager.getInstance();
			mCnxManager.startMonitorServices(this, mExitOnce,
					RcsServiceName.MULTIMEDIA, RcsServiceName.CONTACT);
			try {
				/* Add service listener */
				mCnxManager.getMultimediaSessionApi().addEventListener(
						mServiceListener);
				initialiseMessagingSession(mMeetContact);
			} catch (RcsServiceException e) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "Failed to add listener", e);
				}
				Utils.showMessageAndExit(this,
						getString(R.string.label_api_failed), mExitOnce);
			}
		
	}

	private void initialiseMessagingSession(ContactId meetContact) {
		// Initiate the chat session in background
		try {
			// Initiate session
			mSession = mCnxManager.getMultimediaSessionApi()
					.initiateMessagingSession(MessagingSessionUtils.SERVICE_ID,
							meetContact);
			mSessionId = mSession.getSessionId();
		} catch (Exception e) {
			Utils.showMessageAndExit(this,
					getString(R.string.label_invitation_failed), mExitOnce, e);
			return;
		}
	}

	@Override
	public void onMapReady(GoogleMap map) {

		map.setMyLocationEnabled(true);

	}

	private OnClickListener refreshButtonListener = new OnClickListener() {
		public void onClick(View v) {
			if (GPS.canGetLocation()) {
				latitude = GPS.getLatitude();
				longitude = GPS.getLongitude();
			}
			meetShow.setText("your coordinates: " + String.valueOf(latitude) + ", " + String.valueOf(longitude));
			Toast.makeText(getBaseContext(), "your coordinates were updated",
					Toast.LENGTH_SHORT).show();

		}
	};
	private OnClickListener sendButtonListener = new OnClickListener() {
		public void onClick(View v) {

			if (GPS.canGetLocation()) {
				latitude = GPS.getLatitude();
				longitude = GPS.getLongitude();
			}
			String myCoordinates = (String.valueOf(latitude) + ", " + String
					.valueOf(longitude));
			meetShow.setText("your coordinates: " + myCoordinates);
			sendMyGeoloc(myCoordinates);
			Toast.makeText(getBaseContext(), "your coordinates were sent to the remote contact",
					Toast.LENGTH_SHORT).show();
		}
	};

	private void sendMyGeoloc(String coordinates) {
		try {
			mSession.sendMessage(coordinates.toString().getBytes());
		} catch (Exception e) {
			Utils.showMessageAndExit(this,
					getString(R.string.label_api_failed), mExitOnce, e);
		}
	}

	private MultimediaMessagingSessionListener mServiceListener = new MultimediaMessagingSessionListener() {

		@Override
		public void onStateChanged(ContactId contact, String sessionId,
				final MultimediaSession.State state,
				MultimediaSession.ReasonCode reasonCode) {
			if (LogUtils.isActive) {
				Log.d(LOGTAG, "onStateChanged contact=" + contact
						+ " sessionId=" + sessionId + " state=" + state
						+ " reason=" + reasonCode);
			}
			// Discard event if not for current sessionId
			if (mSessionId == null || !mSessionId.equals(sessionId)) {
				return;
			}
			final String _reasonCode = RiApplication.sMultimediaReasonCodes[reasonCode
					.toInt()];
			mHandler.post(new Runnable() {
				public void run() {
					switch (state) {
					case STARTED:
						break;

					case ABORTED:
						break;

					case REJECTED:
						break;

					case FAILED:
						Utils.showMessageAndExit(
								MeetAsapPreNavigationA.this,
								getString(R.string.label_session_failed,
										_reasonCode), mExitOnce);
						break;

					default:
						if (LogUtils.isActive) {
							Log.d(LOGTAG,
									"onStateChanged "
											+ getString(
													R.string.label_mms_state_changed,
													RiApplication.sMultimediaStates[state
															.toInt()],
													_reasonCode));
						}
					}
				}
			});
		}

		@Override
		public void onMessageReceived(ContactId contact, String sessionId,
				byte[] content) {
			if (LogUtils.isActive) {
				Log.d(LOGTAG, "onMessageReceived contact=" + contact
						+ " sessionId=" + sessionId);
			}
			// Discard event if not for current sessionId
			if (mSessionId == null || !mSessionId.equals(sessionId)) {
				return;
			}
			final String data = new String(content);

			mHandler.post(new Runnable() {
				public void run() {
					meetReceived.setText("you have received data from remote contact: " + data);
					
				}
			});
		}
	};
}
