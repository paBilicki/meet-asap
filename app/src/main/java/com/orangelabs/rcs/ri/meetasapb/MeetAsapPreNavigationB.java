/**
 * @author Piotr Bilicki
 */
package com.orangelabs.rcs.ri.meetasapb;

import java.util.List;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.extension.MultimediaMessagingSession;
import com.gsma.services.rcs.extension.MultimediaMessagingSessionIntent;
import com.gsma.services.rcs.extension.MultimediaMessagingSessionListener;
import com.gsma.services.rcs.extension.MultimediaSession;
import com.gsma.services.rcs.extension.MultimediaSessionService;
import com.orangelabs.rcs.ri.ConnectionManager;
import com.orangelabs.rcs.ri.ConnectionManager.RcsServiceName;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.RiApplication;
import com.orangelabs.rcs.ri.extension.messaging.MessagingSessionUtils;
import com.orangelabs.rcs.ri.meetasapcommon.MeetAsapGpsTracker;
import com.orangelabs.rcs.ri.utils.LockAccess;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.RcsDisplayName;
import com.orangelabs.rcs.ri.utils.Utils;
import com.orangelabs.rcs.ri.meetasapb.MeetAsapContactsB;

/**
 * This shows how to create a simple activity with a map and a marker on the
 * map.
 */
public class MeetAsapPreNavigationB extends FragmentActivity implements
		OnMapReadyCallback {

	TextView meetTextView, meetMyMode, meetShow, meetReceived, meetLocation;
	Button refreshButton, sendButton;

	private LatLng myPosition, interlocutorPosition, middlePosition;
	private double myLatitude = 1, myLongitude = 1, receivedLatitude = 0,
			receivedLongitude = 0;
	private MeetAsapGpsTracker GPS;
	private GoogleMap meetmap;

	private String mServiceId = MessagingSessionUtils.SERVICE_ID;
	private ContactId mMeetContact;
	private ConnectionManager mCnxManager;
	private LockAccess mExitOnce = new LockAccess();
	private final Handler mHandler = new Handler();
	private static final String LOGTAG = LogUtils
			.getTag(MeetAsapPreNavigationB.class.getSimpleName());
	private String mSessionId, myCoordinates, sessionMode;
	private MultimediaMessagingSession mSession;
	public final static String EXTRA_SESSION_ID = "session_id";
	private int markers = 0;
	Marker myMarker, interlocutorsMarker, middleMarker;
	Polyline myPolyline, interPolyline;
	double testlat, testlongit, testaccu;
	int receivedIndicator = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.meet_prenavigation_screen);

		Log.d("meetAsapError", "MeetAsapPreNavigationB - Creating Map..");

		SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
				.findFragmentById(R.id.map);
		mapFragment.getMapAsync(this);

		Log.d("meetAsapError",
				"MeetAsapPreNavigationB - Map created! Creating TVs...");
		meetTextView = (TextView) findViewById(R.id.meet_remote);
		meetShow = (TextView) findViewById(R.id.meet_my);
		meetLocation = (TextView) findViewById(R.id.meet_location);
		meetReceived = (TextView) findViewById(R.id.meet_received);

		Log.d("meetAsapError",
				"MeetAsapPreNavigationB - TVs created! Creating Buttons...");
		refreshButton = (Button) findViewById(R.id.meet_refresh);
		sendButton = (Button) findViewById(R.id.meet_send);

		refreshButton.setOnClickListener(refreshButtonListener);
		sendButton.setOnClickListener(sendButtonListener);

		Log.d("meetAsapError",
				"MeetAsapPreNavigationB - Buttons Created! Creating GPS...");

		GPS = new MeetAsapGpsTracker(MeetAsapPreNavigationB.this);

		Log.d("meetAsapError",
				"MeetAsapPreNavigationB -  - GPS created! Getting extras...");

		mMeetContact = getIntent().getParcelableExtra(
				MeetAsapContactsB.EXTRA_CONTACT);
		sessionMode = getIntent().getStringExtra(
				MeetAsapContactsB.EXTRA_SESSION_MODE);

		Log.d("meetAsapError", "MeetAsapPreNavigationB - extras taken");

		meetTextView.setText("Remote Contact: " + mMeetContact.toString());

		mCnxManager = ConnectionManager.getInstance();
		mCnxManager.startMonitorServices(this, mExitOnce,
				RcsServiceName.MULTIMEDIA, RcsServiceName.CONTACT);
		try {
			/* Add service listener */
			mCnxManager.getMultimediaSessionApi().addEventListener(
					mServiceListener);
			initialiseMessagingSession(getIntent(), sessionMode);
			Log.d("meetAsapError",
					"MeetAsapPreNavigationB - starting initialiseMessagingSession");
		} catch (RcsServiceException e) {
			if (LogUtils.isActive) {
				Log.e(LOGTAG, "Failed to add listener", e);
			}
			Utils.showMessageAndExit(this,
					getString(R.string.label_api_failed), mExitOnce);
		}
	}

	@Override
	public void onMapReady(GoogleMap map) {
		meetmap = map;
		meetmap.setMyLocationEnabled(true);
	}

	private void updateMap() {
		try {
			if (markers == 1) {
				myMarker.remove();
				interlocutorsMarker.remove();
				middleMarker.remove();
				myPolyline.remove();
				interPolyline.remove();
			}

			myMarker = meetmap.addMarker(new MarkerOptions()
					.position(myPosition)
					.title("I am here")
					.icon(BitmapDescriptorFactory
							.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
		if (receivedIndicator == 1){
			interlocutorPosition = new LatLng(receivedLatitude,
					receivedLongitude);
			interlocutorsMarker = meetmap
					.addMarker(new MarkerOptions()
					.position(interlocutorPosition)
					.title("My interlocutor is there")
					.icon(BitmapDescriptorFactory
							.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
			
			double middleLat = (myLatitude + receivedLatitude) / 2;
			double middleLong = (myLongitude + receivedLongitude) / 2;
			LatLng middlePosition = new LatLng(middleLat, middleLong);
			middleMarker = meetmap.addMarker(new MarkerOptions()
					.position(middlePosition)
					.title("Our meeting point is here")
					.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));

			PolylineOptions myLineOptions = new PolylineOptions()
					.add(myPosition)
					.add(middlePosition)
					.color(Color.BLUE);
			
			PolylineOptions interLineOptions = new PolylineOptions()
			.add(interlocutorPosition)
			.add(middlePosition)
			.color(Color.GREEN);

			myPolyline = meetmap.addPolyline(myLineOptions);
			interPolyline = meetmap.addPolyline(interLineOptions);
			
			markers = 1;

			CameraUpdate center = CameraUpdateFactory.newLatLng(middlePosition);
			CameraUpdate zoom = CameraUpdateFactory.zoomTo(15);
			

			meetmap.moveCamera(center);
			meetmap.animateCamera(zoom);
//			fixZoom();
		}
		
		} catch (Exception e) {
			Utils.showMessageAndExit(this,
					getString(R.string.label_api_failed), mExitOnce, e);
		}
	}
	
//	private void fixZoom() {
//	    List<LatLng> points = null;
//	    points.add(myPosition);
//	    points.add(middlePosition);
//	    points.add(interlocutorPosition);
//
//	    LatLngBounds.Builder bc = new LatLngBounds.Builder();
//
//	    for (LatLng item : points) {
//	        bc.include(item);
//	    }
//	   meetmap.moveCamera(CameraUpdateFactory.newLatLngBounds(bc.build(), 50));
//	}
	private void updateMyCoordinates() {
		try {
			if (GPS.canGetLocation()) {
				myLatitude = GPS.getLatitude();
				myLongitude = GPS.getLongitude();
				myPosition = new LatLng(myLatitude, myLongitude);

				myCoordinates = (String.valueOf(myLatitude) + ", " + String
						.valueOf(myLongitude));
				Toast.makeText(getBaseContext(),
						"your coordinates were updated", Toast.LENGTH_SHORT)
						.show();

				// if (sessionMode == "outgoing") {
				// testlat = meetmap.getMyLocation().getAltitude();
				// testlongit = meetmap.getMyLocation().getLongitude();
				// testaccu = meetmap.getMyLocation().getAccuracy();
				// }
				//
			}
		} catch (Exception e) {
			Utils.showMessageAndExit(this,
					getString(R.string.label_api_failed), mExitOnce, e);
		}
	}

	private void sendMyGeoloc(String coordinates) {
		try {
			mSession.sendMessage(coordinates.toString().getBytes());
			Toast.makeText(getBaseContext(),
					"your coordinates were sent to the remote contact",
					Toast.LENGTH_SHORT).show();
		} catch (Exception e) {
			Utils.showMessageAndExit(this,
					getString(R.string.label_api_failed), mExitOnce, e);
		}
	}

	private void initialiseMessagingSession(Intent intent, String sessionMode) {
		MultimediaSessionService sessionApi = mCnxManager
				.getMultimediaSessionApi();
		Log.d("meetAsapError",
				"MeetAsapPreNavigationB - session API  retrieved");
		try {
			if (sessionMode != null && sessionMode.length() > 0)
				sessionMode = "outgoing";
			if (sessionMode == "outgoing") {
				Log.d("meetAsapError",
						"MeetAsapPreNavigationB -  Outgoing sessionMode: "
								+ sessionMode);

				// Check if the service is available
				if (!sessionApi.isServiceRegistered()) {
					Utils.showMessageAndExit(this,
							getString(R.string.label_service_not_available),
							mExitOnce);
					return;
				}

				// Get remote contact
				ContactId mMeetContact = intent
						.getParcelableExtra(MeetAsapContactsB.EXTRA_CONTACT);
				Log.d("meetAsapError",
						"MeetAsapPreNavigationB - remote contact retrieved");
				// Initiate session
				startSession();
				Log.d("meetAsapError",
						"MeetAsapPreNavigationB - startSession in progress");

			} else {
				// Incoming session from its Intent
				Log.d("meetAsapError",
						"MeetAsapPreNavigationB - Incoming sessionMode: "
								+ sessionMode);

				// mSessionId =
				// intent.getStringExtra(meetAsapPreNavigation.EXTRA_SESSION_ID);
				mSessionId = intent
						.getStringExtra(MultimediaMessagingSessionIntent.EXTRA_SESSION_ID);
				Log.d("meetAsapError",
						"MeetAsapPreNavigationB - sessionID retrieved: "
								+ mSessionId);

				// Get the session
				mSession = sessionApi.getMessagingSession(mSessionId);
				Log.d("meetAsapError",
						"MeetAsapPreNavigationB - sessionApi retrieved");

				if (mSession == null) {
					// Session not found or expired
					Utils.showMessageAndExit(this,
							getString(R.string.label_session_has_expired),
							mExitOnce);
					return;
				}

				// Get remote contact
				mMeetContact = mSession.getRemoteContact();

				String from = RcsDisplayName.getInstance(this).getDisplayName(
						mMeetContact);
				Log.d("meetAsapError",
						"MeetAsapPreNavigationB -  remote contact retrieved: "
								+ from);
				// Manual accept

				// Auto accept
				acceptInvitation();
			}

		} catch (Exception e) {
			Utils.showMessageAndExit(this,
					getString(R.string.label_invitation_failed), mExitOnce, e);
			return;
		}

	}

	private OnClickListener refreshButtonListener = new OnClickListener() {
		public void onClick(View v) {
			updateMyCoordinates();
			meetLocation.setText("location: " + testlat + "," + testlongit
					+ " acc: " + testaccu);
			meetShow.setText("your coordinates: " + myCoordinates);
			String interlocutorsCoordinates = String.valueOf(receivedLatitude)
					+ ", " + String.valueOf(receivedLongitude);
			if (interlocutorsCoordinates != null) {
				meetReceived.setText("interlocutor's coordinates: "
						+ interlocutorsCoordinates);

				updateMap();
			}

		}
	};
	private OnClickListener sendButtonListener = new OnClickListener() {
		public void onClick(View v) {
			if (myCoordinates == null) {
				Toast.makeText(getBaseContext(),
						"you have nothing to send! Firstly update!",
						Toast.LENGTH_SHORT).show();
			}

			else {
				sendMyGeoloc(myCoordinates);
			}

		}
	};

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
								MeetAsapPreNavigationB.this,
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
			Log.d("meetAsapError", "MeetAsapPreNavigationB - received string: "
					+ content + " splitting...");

			String[] receivedCoorString = data.split(",");

			Log.d("meetAsapError",
					"MeetAsapPreNavigationB - String splitted! parsing to double...");

			receivedLatitude = Double.parseDouble(receivedCoorString[0]);
			receivedLongitude = Double.parseDouble(receivedCoorString[1]);
			// interlocutorPosition = new LatLng(receivedLatitude,
			// receivedLongitude);

			Log.d("meetAsapError", "MeetAsapPreNavigationB - Parsed! "
					+ receivedLatitude + ", " + receivedLongitude
					+ " creating the LatLng...");

			interlocutorPosition = new LatLng(receivedLatitude,
					receivedLongitude);
			receivedIndicator = 1;
			Log.d("meetAsapError",
					"MeetAsapPreNavigationB - Created LatLng! Adding the marker...");

			mHandler.post(new Runnable() {
				public void run() {
					Toast.makeText(getBaseContext(),
							"remote contact sent you his current location!",
							Toast.LENGTH_SHORT).show();
				}
			});
		}
	};

	// Accept invitation
	private void acceptInvitation() {
		try {
			// Accept the invitation
			mSession.acceptInvitation();
		} catch (Exception e) {
			Utils.showMessageAndExit(this,
					getString(R.string.label_invitation_failed), mExitOnce, e);
		}
	}

	// Reject invitation
	private void rejectInvitation() {
		try {
			// Reject the invitation
			mSession.rejectInvitation();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Accept button listener
	private OnClickListener acceptBtnListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			acceptInvitation();

		}
	};

	// Reject button listener
	private OnClickListener declineBtnListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			// Reject invitation
			rejectInvitation();

			// Exit activity
			finish();

		}
	};

	private void startSession() {
		// Initiate the chat session in background
		try {
			// Initiate session
			mSession = mCnxManager.getMultimediaSessionApi()
					.initiateMessagingSession(mServiceId, mMeetContact);
			mSessionId = mSession.getSessionId();
		} catch (Exception e) {
			Utils.showMessageAndExit(this,
					getString(R.string.label_invitation_failed), mExitOnce, e);
			return;
		}
	}

	// Quit the session
	private void quitSession() {
		// Stop session
		if (mSession != null) {
			try {
				mSession.abortSession();
			} catch (Exception e) {
				e.printStackTrace();
			}
			mSession = null;
		}

		// Exit activity
		finish();
	}
}
