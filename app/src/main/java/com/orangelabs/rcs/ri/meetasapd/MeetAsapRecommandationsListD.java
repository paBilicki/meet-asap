package com.orangelabs.rcs.ri.meetasapd;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View.OnClickListener;

import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.extension.MultimediaMessagingSession;
import com.gsma.services.rcs.extension.MultimediaMessagingSessionListener;
import com.gsma.services.rcs.extension.MultimediaSession;
import com.orangelabs.rcs.ri.ConnectionManager;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.RI;
import com.orangelabs.rcs.ri.RiApplication;
import com.orangelabs.rcs.ri.extension.messaging.MessagingSessionUtils;
import com.orangelabs.rcs.ri.utils.ContactListAdapter;
import com.orangelabs.rcs.ri.utils.LockAccess;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

public class MeetAsapRecommandationsListD extends Activity {
    String sessionId;
    TextView tvOne, tvTwo;
    private String mServiceId = MessagingSessionUtils.SERVICE_ID, mSessionId;
    private ContactId mMeetContact;
    private ConnectionManager mCnxManager;
    private LockAccess mExitOnce = new LockAccess();
    private final Handler mHandler = new Handler();
    private MultimediaMessagingSession mSession;
    private static final String LOGTAG = LogUtils.getTag(MeetAsapRecommandationsListD.class
            .getSimpleName());
    int counterTest = 0;
    String receivedCounter;
    Button updateBtn, sendBtn;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		/* Set layout */
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        String[] items = {"Pizza Hut", "McDonald", "Quick", "KFC"};
//        setListAdapter(new ArrayAdapter<String>(this, R.layout.recommandations_list, items));
        setContentView(R.layout.just_for_test);
        sessionId = getIntent().getStringExtra("sessionId");
        tvOne = (TextView) findViewById(R.id.test_received);
        tvTwo = (TextView) findViewById(R.id.test_sent);
        updateBtn = (Button)findViewById(R.id.test_btn);
        sendBtn = (Button)findViewById(R.id.test_send_btn);
        Log.d("meetAsapError",
                "MeetAsapReccomandationsListD - setting btn listener");
        updateBtn.setOnClickListener(updateCounterListener);
        sendBtn.setOnClickListener(sendCounterListener);

        tvOne.setText("session id: " + sessionId);
        Log.d("meetAsapError",
                "MeetAsapReccomandationsListD - btn listener set");
        mCnxManager = ConnectionManager.getInstance();
        mCnxManager.startMonitorServices(this, mExitOnce,
                ConnectionManager.RcsServiceName.MULTIMEDIA, ConnectionManager.RcsServiceName.CONTACT);
        try {
            /* Add service listener */

            mSession = mCnxManager.getMultimediaSessionApi().getMessagingSession(sessionId);

//            if (mSession != null) {
                mCnxManager.getMultimediaSessionApi().addEventListener(
                        mServiceListener);
//            }

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
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCnxManager.stopMonitorServices(this);
        if (mCnxManager.isServiceConnected(ConnectionManager.RcsServiceName.MULTIMEDIA)) {
            // Remove listener
            try {
                mCnxManager.getMultimediaSessionApi().removeEventListener(mServiceListener);
            } catch (Exception e) {
                if (LogUtils.isActive) {
                    Log.e(LOGTAG, "Failed to remove listener", e);
                }
            }
        }
    }
    private void sendMyMessage(String msg) {
        try {
            mSession.sendMessage(msg.getBytes());
            Toast.makeText(getBaseContext(),
                    "your msg was sent to the remote contact" + msg,
                    Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Utils.showMessageAndExit(this,
                    getString(R.string.label_api_failed), mExitOnce, e);
        }
    }

    private OnClickListener updateCounterListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            tvOne.setText("I have sent: " + counterTest);
            tvTwo.setText("I have received: " + receivedCounter);
            Toast.makeText(getBaseContext(),
                    "U updated tvs",
                    Toast.LENGTH_SHORT).show();
        }
    };

    private OnClickListener sendCounterListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            String counterTestString;
            counterTest = counterTest +1;
                counterTestString = Integer.toString(counterTest);
                sendMyMessage(counterTestString);
        }
    };
    private MultimediaMessagingSessionListener mServiceListener = new MultimediaMessagingSessionListener() {

        @Override
        public void onStateChanged(ContactId contact, String sessionId,
                                   final MultimediaSession.State state,
                                   MultimediaSession.ReasonCode reasonCode) {
            Log.d("meetAsapError", "MeetAsapRecommandationListD - onStateChanged contact=" + contact
                        + " sessionId=" + sessionId + " state=" + state
                        + " reason=" + reasonCode);

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
                                    MeetAsapRecommandationsListD.this,
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
                Log.d(LOGTAG, "Recommandations list - onMessageReceived contact: " + contact
                        + " sessionId: " + sessionId);
            }
            // Discard event if not for current sessionId
//            if (mSessionId == null || !mSessionId.equals(sessionId)) {
//                return;
//            }
            final String data = new String(content);

            receivedCounter = data;
            Toast.makeText(getBaseContext(),
                    "U have received a message: " + receivedCounter,
                    Toast.LENGTH_SHORT).show();
            mHandler.post(new Runnable() {
                public void run() {
                    Toast.makeText(getBaseContext(),
                            "remote contact sent you his current location!",
                            Toast.LENGTH_SHORT).show();
                }
            });
        }
    };

//    @Override
//    public void setListAdapter(ListAdapter adapter) {
//        ContactListAdapter.createContactListAdapter(this);
//        super.setListAdapter(adapter);
//    }

//    @Override
//    protected void onListItemClick(ListView l, View v, int position, long id) {
//        String counterTestString;
//        switch (position) {
//
//            case 0:
//                counterTest = counterTest +1;
//                counterTestString = Integer.toString(counterTest);
//                sendMyMessage(counterTestString);
//                Toast.makeText(getBaseContext(),
//                        "U have sent a message: " + counterTestString,
//                        Toast.LENGTH_SHORT).show();
//                break;
//            case 1:
//                counterTest = counterTest -1;
//                counterTestString = Integer.toString(counterTest);
//                sendMyMessage(counterTestString);
//                Toast.makeText(getBaseContext(),
//                        "U have sent a message: " + counterTestString,
//                        Toast.LENGTH_SHORT).show();
//                break;
//
//            case 2:
//
//                break;
//
//            case 3:
//                break;
//        }
//        super.onListItemClick(l, v, position, id);
//    }

}
