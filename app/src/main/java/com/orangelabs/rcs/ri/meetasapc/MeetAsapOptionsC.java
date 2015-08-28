/**
 * @author Piotr Bilicki
 */
package com.orangelabs.rcs.ri.meetasapc;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.RadioGroup.OnCheckedChangeListener;

import com.google.android.gms.maps.model.LatLng;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.contact.RcsContact;
import com.orangelabs.rcs.ri.ConnectionManager;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.ConnectionManager.RcsServiceName;
import com.orangelabs.rcs.ri.meetasapb.MeetAsapContactsB;
import com.orangelabs.rcs.ri.meetasapb.MeetAsapPreNavigationB;
import com.orangelabs.rcs.ri.meetasapc.MeetAsapContactsC;
import com.orangelabs.rcs.ri.meetasapcommon.MeetAsapGpsTracker;
import com.orangelabs.rcs.ri.utils.LockAccess;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.Utils;
import com.gsma.services.rcs.extension.MultimediaMessagingSession;
import com.gsma.services.rcs.extension.MultimediaMessagingSessionIntent;
import com.gsma.services.rcs.extension.MultimediaMessagingSessionListener;
import com.gsma.services.rcs.extension.MultimediaSession;
import com.gsma.services.rcs.extension.MultimediaSessionService;
import com.orangelabs.rcs.ri.RiApplication;
import com.orangelabs.rcs.ri.extension.messaging.MessagingSessionUtils;
import com.orangelabs.rcs.ri.utils.RcsDisplayName;

public class MeetAsapOptionsC extends Activity {
	public final static String EXTRA_TRANSPORT_MODE = "chosenMode";
	public final static String EXTRA_SESSION_MODE = "incoming";
	public final static String EXTRA_SESSION_ID = "session_id";
	private String mServiceId = MessagingSessionUtils.SERVICE_ID;
	private ContactId mMeetContact;
	private ConnectionManager mCnxManager;
	private LockAccess mExitOnce = new LockAccess();
	private final Handler mHandler = new Handler();
	private MultimediaMessagingSession mSession;
	private static final String LOGTAG = LogUtils.getTag(MeetAsapOptionsC.class
			.getSimpleName());
	private MeetAsapGpsTracker GPS;
	private String mSessionId, sessionMode, chosenMode, meetingNature,
			myCoordinates, interCoordinates, iMode;
	RcsContact remoteContact;
	TextView modeQuestion, contactId, myMode, smode, mNature, myCoord,
			interCoord, interMode;
	Button sendOptions, start, update;
	RadioGroup modesList, natureList;
	private double myLatitude = 1, myLongitude = 1, receivedLatitude = 0,
			receivedLongitude = 0;
	JSONObject receivedMsg;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.meet_options);

		Log.d("meetAsapError", "MeetAsapOptionsC - Taking extras...");

		mMeetContact = getIntent().getParcelableExtra(
				MeetAsapContactsC.EXTRA_CONTACT);
		sessionMode = getIntent().getStringExtra(
				MeetAsapContactsC.EXTRA_SESSION_MODE);

		Log.d("meetAsapError", "MeetAsapOptionsC - sessionMode: " + sessionMode);
		Log.d("meetAsapError",
				"MeetAsapOptionsC - Extras Taken! Creating TVs...");

		contactId = (TextView) findViewById(R.id.contact_id);
		smode = (TextView) findViewById(R.id.session_mode);

		myCoord = (TextView) findViewById(R.id.my_coord);
		myMode = (TextView) findViewById(R.id.my_mode);
		mNature = (TextView) findViewById(R.id.chosen_nature);
		modeQuestion = (TextView) findViewById(R.id.mode_question);
		interCoord = (TextView) findViewById(R.id.inter_coord);
		interMode = (TextView) findViewById(R.id.inter_mode);

		Log.d("meetAsapError",
				"MeetAsapOptionsC - TVs created! Creating RadioGroups...");

		modesList = (RadioGroup) findViewById(R.id.transport_mode_group);
		natureList = (RadioGroup) findViewById(R.id.meeting_nature_group);

		if (sessionMode == null) {
			// sessionMode = "incoming";
			modeQuestion.setText("Choose your mode of transport");
			natureList.setVisibility(View.GONE);
			smode.setText("session mode: incoming");
		} else {
			smode.setText("session mode: outgoing");
		}

		modesList.setOnCheckedChangeListener(modeListener);
		natureList.setOnCheckedChangeListener(natureListener);

		contactId.setText("contact invited: " + mMeetContact.toString());

		Log.d("meetAsapError",
				"MeetAsapOptionsC - RadioGroups created! Creating Buttons...");

		update = (Button) findViewById(R.id.update_btn);
		sendOptions = (Button) findViewById(R.id.send_btn);
		start = (Button) findViewById(R.id.start_btn);

		Log.d("meetAsapError",
				"MeetAsapOptionsC - Buttons created! setting Listeners...");

		update.setOnClickListener(updateListener);
		sendOptions.setOnClickListener(sendOptionsListener);
		start.setOnClickListener(startListener);

		Log.d("meetAsapError",
				"MeetAsapOptionsC - Buttons Created! Creating GPS...");

		GPS = new MeetAsapGpsTracker(MeetAsapOptionsC.this);

		Log.d("meetAsapError", "MeetAsapOptionsC - GPS created!");
		mCnxManager = ConnectionManager.getInstance();
		mCnxManager.startMonitorServices(this, mExitOnce,
				RcsServiceName.MULTIMEDIA, RcsServiceName.CONTACT);
		try {
			/* Add service listener */
			mCnxManager.getMultimediaSessionApi().addEventListener(
					mServiceListener);
			initialiseMessagingSession(getIntent(), sessionMode);
			Log.d("meetAsapError",
					"MeetAsapOptionsC - starting initialiseMessagingSession");
		} catch (RcsServiceException e) {
			if (LogUtils.isActive) {
				Log.e(LOGTAG, "Failed to add listener", e);
			}
			Utils.showMessageAndExit(this,
					getString(R.string.label_api_failed), mExitOnce);
		}

	}

	private OnCheckedChangeListener modeListener = new OnCheckedChangeListener() {

		@Override
		public void onCheckedChanged(RadioGroup group, int checkedId) {
			RadioButton radioButton = (RadioButton) findViewById(checkedId);
			chosenMode = (String) radioButton.getText();
			myMode.setText("chosen transport mode: " + chosenMode);
		}
	};

	private OnCheckedChangeListener natureListener = new OnCheckedChangeListener() {

		@Override
		public void onCheckedChanged(RadioGroup group, int checkedId) {
			RadioButton radioButton = (RadioButton) findViewById(checkedId);
			meetingNature = (String) radioButton.getText();
			mNature.setText("chosen meeting's nature: " + meetingNature);
		}
	};
	private OnClickListener updateListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			updateMyCoordinates();
			myCoord.setText("your coordinates: " + myCoordinates);
			interCoord.setText("interlocutor's coordinates: " + interCoordinates);
			interMode.setText("interlocutor's transport mode: " + iMode);
		}
	};
	private OnClickListener sendOptionsListener = new OnClickListener() {
		String message;
		@Override
		public void onClick(View v) {
			if (myCoordinates == null) {
				Toast.makeText(getBaseContext(),
						"you have nothing to send! Firstly update!",
						Toast.LENGTH_SHORT).show();
			} else {
				// sendMyGeoloc(myCoordinates);
				try {
					message = createMyMessage();
					sendMyMessage(message);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		}
	};

	private OnClickListener startListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			Toast.makeText(getBaseContext(), "recommandations not ready :)",
					Toast.LENGTH_SHORT).show();

			 //String sessionMode = "outgoing";
			
			 Intent newint = new Intent(MeetAsapOptionsC.this,
			 MeetAsapPreNavigationC.class);
//			 newint.putExtra(MeetAsapContactsC.EXTRA_CONTACT,
//			 (Parcelable) remoteContact.getContactId());
//			 newint.putExtra(EXTRA_TRANSPORT_MODE, chosenMode);
//			 newint.putExtra(EXTRA_SESSION_MODE, sessionMode);
			 startActivity(newint);
		}

	};

	private String createMyMessage() throws JSONException {
		// if (sessionMode == null){
		//
		// }else{
		//
		// }
		// msg = sessionMode;
		// msg = myCoordinates;
		// msg = meetingNature;
		// msg = chosenMode;

		JSONObject jObject = new JSONObject();

		jObject.put("interCoordinates", myCoordinates);
		jObject.put("interMode", chosenMode);

		String msg = jObject.toString();
		return msg;
	}
	
	
	private void sendMyMessage(String msg) {
		try {
			mSession.sendMessage(msg.getBytes());
			Toast.makeText(getBaseContext(),
					"your msg was sent to the remote contact",
					Toast.LENGTH_SHORT).show();
		} catch (Exception e) {
			Utils.showMessageAndExit(this,
					getString(R.string.label_api_failed), mExitOnce, e);
		}
	}
	private void updateMyCoordinates() {
		try {
			if (GPS.canGetLocation()) {
				myLatitude = GPS.getLatitude();
				myLongitude = GPS.getLongitude();

				myCoordinates = (String.valueOf(myLatitude) + ", " + String
						.valueOf(myLongitude));
				Toast.makeText(getBaseContext(),
						"your coordinates were updated", Toast.LENGTH_SHORT)
						.show();
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
		Log.d("meetAsapError", "MeetAsapOptionsC - session API  retrieved");
		try {
			if (sessionMode != null && sessionMode.length() > 0)
				sessionMode = "outgoing";
			if (sessionMode == "outgoing") {
				Log.d("meetAsapError",
						"MeetAsapOptionsC -  Outgoing sessionMode: "
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
						"MeetAsapOptionsC - remote contact retrieved");
				// Initiate session
				startSession();
				Log.d("meetAsapError",
						"MeetAsapOptionsC - startSession in progress");

			} else {
				// Incoming session from its Intent
				Log.d("meetAsapError",
						"MeetAsapOptionsC - Incoming sessionMode: "
								+ sessionMode);

				// mSessionId =
				// intent.getStringExtra(meetAsapPreNavigation.EXTRA_SESSION_ID);
				mSessionId = intent
						.getStringExtra(MultimediaMessagingSessionIntent.EXTRA_SESSION_ID);
				Log.d("meetAsapError",
						"MeetAsapOptionsC - sessionID retrieved: " + mSessionId);

				// Get the session
				mSession = sessionApi.getMessagingSession(mSessionId);
				Log.d("meetAsapError",
						"MeetAsapOptionsC - sessionApi retrieved");

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
						"MeetAsapOptionsC -  remote contact retrieved: " + from);
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
								MeetAsapOptionsC.this,
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
			Log.d("meetAsapError", "MeetAsapOptionsC - received string: "
					+ content + " splitting...");

			
			try {
				receivedMsg  = new JSONObject(data);
				interCoordinates = receivedMsg.getString("interCoordinates");
				iMode = receivedMsg.getString("interMode");
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

//			String[] receivedCoorString = data.split(",");
//
//			Log.d("meetAsapError",
//					"MeetAsapOptionsC - String splitted! parsing to double...");
//
//			receivedLatitude = Double.parseDouble(receivedCoorString[0]);
//			receivedLongitude = Double.parseDouble(receivedCoorString[1]);
//
//			Log.d("meetAsapError", "MeetAsapOptionsC - Parsed! "
//					+ receivedLatitude + ", " + receivedLongitude
//					+ " creating the LatLng...");

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
