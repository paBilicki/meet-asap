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
    private String mSessionId, sMode, mMode, mNature,
            mCoordinates, iCoordinates, iMode, receivedSessionMode, rContact = "default";
    TextView modeQuestion, myMode, sessionMode, meetingNature, myCoordinates,
            interCoordinates, interMode, remoteContact;
    Button sendOptionsBtn, startBtn, updateBtn;
    RadioGroup modesList, natureList;
    private double mLatitude = 1, mLongitude = 1;
    JSONObject receivedMsg;
    int msgIndicator = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.meet_options);

        Log.d("meetAsapError", "MeetAsapOptionsC - Taking extras...");

        mMeetContact = getIntent().getParcelableExtra(
                MeetAsapContactsC.EXTRA_CONTACT);
        rContact = mMeetContact.toString();
        sMode = getIntent().getStringExtra(
                MeetAsapContactsC.EXTRA_SESSION_MODE);

        Log.d("meetAsapError", "MeetAsapOptionsD - sessionMode: " + sMode);
        Log.d("meetAsapError",
                "MeetAsapOptionsD - Extras Taken! Creating TVs...");

        remoteContact = (TextView) findViewById(R.id.contact_id);
        sessionMode = (TextView) findViewById(R.id.session_mode);
        myCoordinates = (TextView) findViewById(R.id.my_coord);
        myMode = (TextView) findViewById(R.id.my_mode);
        meetingNature = (TextView) findViewById(R.id.chosen_nature);
        modeQuestion = (TextView) findViewById(R.id.mode_question);
        interCoordinates = (TextView) findViewById(R.id.inter_coord);
        interMode = (TextView) findViewById(R.id.inter_mode);

        Log.d("meetAsapError",
                "MeetAsapOptionsD - TVs created! Creating RadioGroups...");

        modesList = (RadioGroup) findViewById(R.id.transport_mode_group);
        natureList = (RadioGroup) findViewById(R.id.meeting_nature_group);

        if (sMode == null) {
            modeQuestion.setText("Choose your mode of transport");
            natureList.setVisibility(View.GONE);
            //sMode = "incoming";
            sessionMode.setText("Session mode: incoming");
        } else {
            sessionMode.setText("Session mode: " + sMode);
            natureList.setOnCheckedChangeListener(natureListener);
        }

        modesList.setOnCheckedChangeListener(modeListener);

        remoteContact.setText("Contact invited: " + rContact);

        Log.d("meetAsapError",
                "MeetAsapOptionsD - RadioGroups created! Creating Buttons...");

        updateBtn = (Button) findViewById(R.id.update_btn);
        sendOptionsBtn = (Button) findViewById(R.id.send_btn);
        startBtn = (Button) findViewById(R.id.start_btn);

        Log.d("meetAsapError",
                "MeetAsapOptionsD - Buttons created! setting Listeners...");

        updateBtn.setOnClickListener(updateListener);
        sendOptionsBtn.setOnClickListener(sendOptionsListener);
        startBtn.setOnClickListener(startListener);

        Log.d("meetAsapError",
                "MeetAsapOptionsD - Buttons Created! Creating GPS...");

        GPS = new MeetAsapGpsTracker(MeetAsapOptionsC.this);

        Log.d("meetAsapError", "MeetAsapOptionsD - GPS created!");
        mCnxManager = ConnectionManager.getInstance();
        mCnxManager.startMonitorServices(this, mExitOnce,
                RcsServiceName.MULTIMEDIA, RcsServiceName.CONTACT);
        try {
            /* Add service listener */
            mCnxManager.getMultimediaSessionApi().addEventListener(
                    mServiceListener);
            initialiseMessagingSession(getIntent(), sMode);
            Log.d("meetAsapError",
                    "MeetAsapOptionsD - starting initialiseMessagingSession");
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
            mMode = (String) radioButton.getText();
            myMode.setText("Chosen transport mode: " + mMode);
            Log.d("meetAsapError",
                    "MeetAsapOptionsD  - myMode changed to: " + mMode);
        }
    };

    private OnCheckedChangeListener natureListener = new OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            RadioButton radioButton = (RadioButton) findViewById(checkedId);
            mNature = (String) radioButton.getText();
            meetingNature.setText("Chosen meeting's nature: " + mNature);
            Log.d("meetAsapError",
                    "MeetAsapOptionsD  - mNature changed to: " + mNature);
        }
    };
    private OnClickListener updateListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            updateMyCoordinates();
            myCoordinates.setText("My coordinates: " + mCoordinates);

            interCoordinates.setText("Interlocutor's coordinates: " + iCoordinates);
            interMode.setText("Interlocutor's transport mode: " + iMode);
            meetingNature.setText("Chosen meeting's nature: " + mNature);
            Log.d("meetAsapError",
                    "MeetAsapOptionsD - infos updated");


        }
    };
    private OnClickListener sendOptionsListener = new OnClickListener() {
        String message;

        @Override
        public void onClick(View v) {
            if (mCoordinates == null) {
                Toast.makeText(getBaseContext(),
                        "There is nothing to send, firstly update.",
                        Toast.LENGTH_SHORT).show();
            } else {
                try {
                    message = createMyMessage();
                    Log.d("meetAsapError",
                            "MeetAsapOptionsD - creating message");
                    sendMyMessage(message);
                    Log.d("meetAsapError",
                            "MeetAsapOptionsD - sending message: ");

                } catch (JSONException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
        }
    };

    private String createQueryInfos() throws JSONException {
        JSONObject queryInfos = new JSONObject();
        queryInfos.put("sessionMode", sMode);
        queryInfos.put("mymode", mMode);
        queryInfos.put("myCoordinates", mCoordinates);
        queryInfos.put("interMode", iMode);
        queryInfos.put("interCoordinates", iCoordinates);
        queryInfos.put("meetingNature", mNature);
        return queryInfos.toString();
    }

    private OnClickListener startListener = new OnClickListener() {
        @Override
        public void onClick(View v) {

            Intent newint = new Intent(MeetAsapOptionsC.this, MeetAsapPreNavigationC.class);
            newint.putExtra("sessionId", mSessionId);
            newint.putExtra("remoteContact", mMeetContact.toString());
            newint.putExtra("sessionMode", sMode);
            newint.putExtra("meetingNature", mNature);
            newint.putExtra("myCoordinates", mCoordinates);
            newint.putExtra("myMode", mMode);
            newint.putExtra("interCoordinates", iCoordinates);
            newint.putExtra("interMode", iMode);

            startActivity(newint);
        }

    };

    private String createMyMessage() throws JSONException {
        JSONObject jObject = new JSONObject();
        String sender;
        Log.d("meetAsapError",
                "MeetAsapOptionsD - createMessage - sessionMode: " + sMode);

//        if (sMode == null) {
//            sMode = "incoming";
//            sender = "guest";
//        } else {
//            sender = "host";
            jObject.put("meetingNature", mNature);
//        }
//        jObject.put("sender", sender);
        jObject.put("myCoordinates", mCoordinates);
        jObject.put("myMode", mMode);

        String msg = jObject.toString();

        return msg;
    }


    private void sendMyMessage(String msg) {
        try {
            mSession.sendMessage(msg.getBytes());
            Toast.makeText(getBaseContext(),
                    "Information sent to the remote contact",
                    Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Utils.showMessageAndExit(this,
                    getString(R.string.label_api_failed), mExitOnce, e);
        }
    }

    private void updateMyCoordinates() {
        try {
            if (GPS.canGetLocation()) {
                mLatitude = GPS.getLatitude();
                mLongitude = GPS.getLongitude();

                mCoordinates = (String.valueOf(mLatitude) + ", " + String
                        .valueOf(mLongitude));
                Toast.makeText(getBaseContext(),
                        "Information updated", Toast.LENGTH_SHORT)
                        .show();
            }
        } catch (Exception e) {
            Utils.showMessageAndExit(this,
                    getString(R.string.label_api_failed), mExitOnce, e);
        }
    }

    private void initialiseMessagingSession(Intent intent, String sessionMode) {
        MultimediaSessionService sessionApi = mCnxManager
                .getMultimediaSessionApi();
        Log.d("meetAsapError", "MeetAsapOptionsD - session API  retrieved");
        try {
            if (sMode != null && sMode.length() > 0)
                sMode = "outgoing";
            if (sMode == "outgoing") {
                Log.d("meetAsapError",
                        "MeetAsapOptionsD - initialiseMessagingSession - sessionMode: "
                                + sMode);

                // Check if the service is available
                if (!sessionApi.isServiceRegistered()) {
                    Utils.showMessageAndExit(this,
                            getString(R.string.label_service_not_available),
                            mExitOnce);
                    return;
                }

                // Get remote contact
                ContactId mMeetContact = intent
                        .getParcelableExtra(MeetAsapContactsC.EXTRA_CONTACT);
                Log.d("meetAsapError",
                        "MeetAsapOptionsD - remote contact retrieved");
                // Initiate session
                startSession();
                Log.d("meetAsapError",
                        "MeetAsapOptionsD - startSession in progress");

            } else {
                // Incoming session from its Intent
                Log.d("meetAsapError",
                        "MeetAsapOptionsD -  initialiseMessagingSession - sessionMode: "
                                + sMode);

                mSessionId = intent
                        .getStringExtra(MultimediaMessagingSessionIntent.EXTRA_SESSION_ID);
                Log.d("meetAsapError",
                        "MeetAsapOptionsD - initialiseMessagingSession - sessionID retrieved: " + mSessionId);

                // Get the session
                mSession = sessionApi.getMessagingSession(mSessionId);
                Log.d("meetAsapError",
                        "MeetAsapOptionsD - initialiseMessagingSession - sessionApi retrieved");

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
                        "MeetAsapOptionsD - initialiseMessagingSession - remote contact retrieved: " + from);
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
            String sender;
            if (LogUtils.isActive) {
                Log.d(LOGTAG, "onMessageReceived contact: " + contact
                        + " sessionId: " + sessionId);
            }
            // Discard event if not for current sessionId
            if (mSessionId == null || !mSessionId.equals(sessionId)) {
                return;
            }
            final String data = new String(content);
            msgIndicator = 1;

            Log.d("meetAsapError", "MeetAsapOptionsD - onMessageReceived: " + receivedMsg);

            try {
                receivedMsg = new JSONObject(data);
//                sender = receivedMsg.getString("sender");
                iCoordinates = receivedMsg.getString("myCoordinates");
                iMode = receivedMsg.getString("myMode");

//                Log.d("MeetAsapError", "sender: " + sender);

//                if (sender == "host") {
                    mNature = receivedMsg.getString("meetingNature");
//                }

            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            mHandler.post(new Runnable() {
                public void run() {
                    Toast.makeText(getBaseContext(),
                            "New information received",
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
