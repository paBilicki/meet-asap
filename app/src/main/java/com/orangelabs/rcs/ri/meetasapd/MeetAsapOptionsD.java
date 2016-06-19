/**
 * @author Piotr Bilicki
 */
package com.orangelabs.rcs.ri.meetasapd;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.RadioGroup.OnCheckedChangeListener;

import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.contact.ContactId;
import com.orangelabs.rcs.ri.ConnectionManager;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.ConnectionManager.RcsServiceName;
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

import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MeetAsapOptionsD extends Activity {
    List<String> placesNames = new ArrayList<String>();
    List<String> placesVicinities = new ArrayList<String>();
    List<String> placesRatings = new ArrayList<String>();
    List<String> placesLats = new ArrayList<String>();
    List<String> placesLngs = new ArrayList<String>();
    List<Location> placesLocations = new ArrayList<Location>();
    List<String> Mdistances = new ArrayList<String>();
    List<String> Idistances = new ArrayList<String>();
    List<String> placesRefs = new ArrayList<String>();
    List<String> placesIcons = new ArrayList<String>();

    Boolean informationComplete = false;

    public final static String EXTRA_TRANSPORT_MODE = "chosenMode";
    public final static String EXTRA_SESSION_MODE = "incoming";
    public final static String EXTRA_SESSION_ID = "session_id";
    private static final String LOGTAG = LogUtils.getTag(MeetAsapOptionsD.class
            .getSimpleName());
    private String mServiceId = MessagingSessionUtils.SERVICE_ID, mSessionId;
    private ContactId mMeetContact;
    private ConnectionManager mCnxManager;
    private LockAccess mExitOnce = new LockAccess();
    private final Handler mHandler = new Handler();
    private MultimediaMessagingSession mSession;

    private Dialog mProgressDialog;
    private MeetAsapGpsTracker GPS;
    private String sMode, mMode = "", iMode = "", mNature = "", mCoordinates = "", iCoordinates = "",
            rContact = "default", msg = "";
    TextView modeQuestion, myMode, meetingNature, interMode, distanceTv;
    ImageButton listBtn;
    RadioGroup modesList, natureList;
    private double mLatitude = 1, mLongitude = 1;
    JSONObject receivedMsg;

    String distanceBetweenUsers;
    String goToList = "no";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.meet_options);

        Log.d("meetAsapError", "MeetAsapOptionsD - Taking extras...");

        mMeetContact = getIntent().getParcelableExtra(
                MeetAsapContactsD.EXTRA_CONTACT);
        rContact = mMeetContact.toString();
        sMode = getIntent().getStringExtra(
                MeetAsapContactsD.EXTRA_SESSION_MODE);

        Log.d("meetAsapError", "MeetAsapOptionsD - sessionMode: " + sMode);

        Log.d("meetAsapError",
                "MeetAsapOptionsD - Extras Taken! Creating layout...");

        myMode = (TextView) findViewById(R.id.my_mode);
        meetingNature = (TextView) findViewById(R.id.chosen_nature);
        modeQuestion = (TextView) findViewById(R.id.mode_question);
        interMode = (TextView) findViewById(R.id.inter_mode);
        distanceTv = (TextView) findViewById(R.id.distance_tv);

        listBtn = (ImageButton) findViewById(R.id.list_btn);
        modesList = (RadioGroup) findViewById(R.id.transport_mode_group);
        natureList = (RadioGroup) findViewById(R.id.meeting_nature_group);

        if (sMode == null) {
            modeQuestion.setText("Choose your mode of transport");
            natureList.setVisibility(View.GONE);
            listBtn.setVisibility(View.GONE);
            sMode = "incoming";
            iCoordinates = "48.7334647,-3.4612257";
            mCoordinates = "48.732309, -3.465897";
        } else {
            natureList.setOnCheckedChangeListener(natureListener);
            mCoordinates = "48.7334647,-3.4612257";

            iCoordinates = "48.732309, -3.465897";
        }


        modesList.setOnCheckedChangeListener(modeListener);
        listBtn.setOnClickListener(listListener);

        Log.d("meetAsapError",
                "OptionsD - Layout created!, GPS...");

        GPS = new MeetAsapGpsTracker(MeetAsapOptionsD.this);
        //updateMyCoordinates();

        Log.d("meetAsapError", "OptionsD - GPS created!");


        mCnxManager = ConnectionManager.getInstance();
        mCnxManager.startMonitorServices(this, mExitOnce,
                RcsServiceName.MULTIMEDIA, RcsServiceName.CONTACT);
        try {
            /* Add service listener */
            mCnxManager.getMultimediaSessionApi().addEventListener(
                    mServiceListener);
            initialiseMessagingSession(getIntent(), sMode);
            Log.d("meetAsapError",
                    "OptionsD - starting initialiseMessagingSession");
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
        if (mCnxManager.isServiceConnected(RcsServiceName.MULTIMEDIA)) {
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

    private OnCheckedChangeListener modeListener = new OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            RadioButton radioButton = (RadioButton) findViewById(checkedId);
            mMode = (String) radioButton.getText();
            myMode.setText("Chosen transport mode: " + mMode);
            Log.d("meetAsapError",
                    "OptionsD  - myMode changed to: " + mMode);

            try {
                msg = createMyMessage();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            sendMyMessage(msg);
        }
    };

    private OnCheckedChangeListener natureListener = new OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            RadioButton radioButton = (RadioButton) findViewById(checkedId);
            mNature = (String) radioButton.getText();
            meetingNature.setText("Chosen meeting's nature: " + mNature);
            Log.d("meetAsapError",
                    "OptionsD  - mNature changed to: " + mNature);
            String msg = "error";
            try {
                msg = createMyMessage();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            sendMyMessage(msg);
        }
    };


    private android.view.View.OnClickListener listListener = new android.view.View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (informationComplete) {
                getPlaceName();
                finish();
                goToList = "yes";
                String msg = "error";
                try {
                    msg = createMyMessage();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                sendMyMessage(msg);
            } else {
                Toast.makeText(getBaseContext(),
                        "Context Information is not complete",
                        Toast.LENGTH_SHORT).show();
            }

        }

    };

    private String createMyMessage() throws JSONException {
        JSONObject jObject = new JSONObject();
        Log.d("meetAsapError",
                "OptionsD - createMessage - sessionMode: " + sMode);

        jObject.put("meetingNature", mNature);
        jObject.put("myCoordinates", mCoordinates);
        jObject.put("myMode", mMode);
        jObject.put("goToList", goToList);

        String msg = jObject.toString();

        return msg;
    }

    private void sendMyMessage(String msg) {
        try {
            mSession.sendMessage(msg.getBytes());
            Log.d("meetAsapError", "OptionsD - sendMyMessage: " + msg);
        } catch (Exception e) {
            Utils.showMessageAndExit(this,
                    getString(R.string.label_api_failed), mExitOnce, e);
        }
        if (iCoordinates.length() == 0 || iMode.length() == 0 || mNature.length() == 0 || mCoordinates.length() == 0 || mMode.length() == 0) {
        } else {
            informationComplete = true;
            Toast.makeText(getBaseContext(), "The context information is full, you can display the list of recommendations", Toast.LENGTH_SHORT).show();
            if (sMode.equals("outgoing")) {
                listBtn.setImageResource(R.drawable.btn_list_yes);
            }
        }
    }

    private void updateMyCoordinates() {
        try {
            if (GPS.canGetLocation()) {
                mLatitude = GPS.getLatitude();
                mLongitude = GPS.getLongitude();

                mCoordinates = (String.valueOf(mLatitude) + ", " + String
                        .valueOf(mLongitude));

                Log.d("meetAsapError", "OptionsD - updateMyCoordinates: " + mCoordinates);
            }
        } catch (Exception e) {
            Utils.showMessageAndExit(this,
                    getString(R.string.label_api_failed), mExitOnce, e);
        }
    }

    private void initialiseMessagingSession(Intent intent, String sessionMode) {
        MultimediaSessionService sessionApi = mCnxManager
                .getMultimediaSessionApi();
        Log.d("meetAsapError", "OptionsD - session API  retrieved");
        try {

            if (sessionMode.equals("outgoing")) {
                Log.d("meetAsapError",
                        "OptionsD - initialiseMessagingSession - sessionMode: "
                                + sessionMode + "sMode: " + sMode);

                // Check if the service is available
                if (!sessionApi.isServiceRegistered()) {
                    Utils.showMessageAndExit(this,
                            getString(R.string.label_service_not_available),
                            mExitOnce);
                    return;
                }

                // Get remote contact
                ContactId mMeetContact = intent
                        .getParcelableExtra(MeetAsapContactsD.EXTRA_CONTACT);
                Log.d("meetAsapError",
                        "OptionsD - remote contact retrieved");
                // Initiate session
                startSession();
                Log.d("meetAsapError",
                        "OptionsD - startSession in progress");

            } else {
                // Incoming session from its Intent
                Log.d("meetAsapError",
                        "OptionsD - initialiseMessagingSession - sessionMode: "
                                + sessionMode + "sMode: " + sMode);

                mSessionId = intent
                        .getStringExtra(MultimediaMessagingSessionIntent.EXTRA_SESSION_ID);
                Log.d("meetAsapError",
                        "OptionsD - initialiseMessagingSession - sessionID retrieved: " + mSessionId);

                // Get the session
                mSession = sessionApi.getMessagingSession(mSessionId);
                Log.d("meetAsapError",
                        "OptionsD - initialiseMessagingSession - sessionApi retrieved");

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
                        "OptionsD - initialiseMessagingSession - remote contact retrieved: " + from);

                // Manual accept
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Meet ASAP session");
                builder.setMessage(getString(R.string.label_mm_from_id, from, mServiceId));
                builder.setCancelable(false);
                builder.setIcon(R.drawable.ri_notif_meet_icon);
                builder.setPositiveButton(getString(R.string.label_accept), acceptBtnListener);
                builder.setNegativeButton(getString(R.string.label_decline), declineBtnListener);
                builder.show();

                // Auto accept
                //acceptInvitation();
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
                            /* Session is established: hide progress dialog */
                            hideProgressDialog();
                            break;

                        case ABORTED:
                            /* Session is aborted: hide progress dialog then exit. */
                            hideProgressDialog();
                            Utils.showMessageAndExit(MeetAsapOptionsD.this,
                                    getString(R.string.label_session_aborted, _reasonCode),
                                    mExitOnce);
                            break;

                        case REJECTED:
                            /* Session is rejected: hide progress dialog then exit. */
                            hideProgressDialog();
                            Utils.showMessageAndExit(MeetAsapOptionsD.this,
                                    getString(R.string.label_session_rejected, _reasonCode),
                                    mExitOnce);
                            break;

                        case FAILED:
                            /* Session failed: hide progress dialog then exit. */
                            hideProgressDialog();
                            Utils.showMessageAndExit(MeetAsapOptionsD.this,
                                    getString(R.string.label_session_failed, _reasonCode),
                                    mExitOnce);
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
                Log.d(LOGTAG, "OptionsD - onMessageReceived contact: " + contact + " sessionId: " + sessionId);
            }
            // Discard event if not for current sessionId
            if (mSessionId == null || !mSessionId.equals(sessionId)) {
                return;
            }
            final String data = new String(content);

            try {
                receivedMsg = new JSONObject(data);
                //for demo
//                try {
//                    iCoordinates = receivedMsg.getString("myCoordinates");
//                } catch (JSONException e) {
//                    Log.d("meetAsapError", "OptionsD - (no iCoordinates), onMessageReceived: " + receivedMsg);
//                }
                try {
                    iMode = receivedMsg.getString("myMode");
                } catch (JSONException e) {
                    Log.d("meetAsapError", "OptionsD - (no iMode), onMessageReceived: " + receivedMsg);
                }
                try {
                    mNature = receivedMsg.getString("meetingNature");
                } catch (JSONException e) {
                    Log.d("meetAsapError", "OptionsD - (no mNature), onMessageReceived: " + receivedMsg);
                }
                try {
                    goToList = receivedMsg.getString("goToList");
                } catch (JSONException e) {
                    Log.d("meetAsapError", "OptionsD - (no mNature), onMessageReceived: " + receivedMsg);
                }
            } catch (JSONException e) {
                Log.d("meetAsapError", "OptionsD - (json problem), onMessageReceived: " + receivedMsg);
            }

            mHandler.post(new Runnable() {
                public void run() {

                    String[] mCoor = mCoordinates.split(",");
                    String[] iCoor = iCoordinates.split(",");
                    distanceMeasuring(mCoor[0], mCoor[1], iCoor[0], iCoor[1], "users");
                    distanceTv.setText("Distance to interlocutor: " + distanceBetweenUsers);

                    interMode.setText("Interlocutor's transport mode: " + iMode);
                    meetingNature.setText("Chosen meeting's nature: " + mNature);

                    if (iCoordinates.length() == 0 || iMode.length() == 0 || mNature.length() == 0 || mCoordinates.length() == 0 || mMode.length() == 0) {

                    } else {
                        if (sMode.equals("outgoing")) {
                            informationComplete = true;
                            listBtn.setImageResource(R.drawable.btn_list_yes);
                        }

                    }

                    if (sMode.equals("incoming") && goToList.equals("yes")) {
                        getPlaceName();
                        finish();
                    }
                }
            });
        }
    };

    private void acceptInvitation() {
        try {
            // Accept the invitation
            mSession.acceptInvitation();
            Log.d("meetAsapError", "OptionsD - acceptInvitation");
        } catch (Exception e) {
            Utils.showMessageAndExit(this,
                    getString(R.string.label_invitation_failed), mExitOnce, e);
        }
    }

    private void rejectInvitation() {
        try {
            // Reject the invitation
            mSession.rejectInvitation();
            Log.d("meetAsapError", "OptionsD - rejectInvitation");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private OnClickListener acceptBtnListener = new OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            Log.d("meetAsapError", "OptionsD - acceptBtnListener");
            acceptInvitation();
        }
    };

    private OnClickListener declineBtnListener = new OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            Log.d("meetAsapError", "OptionsD - declineBtnListener");
            rejectInvitation();
            /* Exit activity */
            finish();
        }
    };

    private void startSession() {
        // Initiate the chat session in background
        try {
            // Initiate session
            mSession = mCnxManager.getMultimediaSessionApi().initiateMessagingSession(mServiceId, mMeetContact);
            mSessionId = mSession.getSessionId();
            Log.d("meetAsapError", "Options: startSession - msessionid: " + mSessionId);
        } catch (Exception e) {
            Utils.showMessageAndExit(this,
                    getString(R.string.label_invitation_failed), mExitOnce, e);
            return;
        }
        showProgressDialog();
    }

    private void showProgressDialog() {
        mProgressDialog = Utils.showProgressDialog(MeetAsapOptionsD.this,
                "Waiting for interlocutor's acceptation");
        mProgressDialog.setOnCancelListener(new OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                Toast.makeText(MeetAsapOptionsD.this,
                        getString(R.string.label_session_canceled), Toast.LENGTH_SHORT).show();
                quitSession();
            }
        });
    }

    private void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

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

    public void getPlaceName() {
        String[] mCoor = mCoordinates.split(",");
        String[] iCoor = iCoordinates.split(",");
        new GetPlaces().execute(mCoor[0], mCoor[1], iCoor[0], iCoor[1], mNature);
    }

    class GetPlaces extends AsyncTask<String, String, String> {
        String mlatit, mlongit, ilatit, ilongit, mnature;

        @Override
        protected String doInBackground(String... params) {
            // Get the latitude address
            mlatit = params[0];
            mlongit = params[1];
            ilatit = params[2];
            ilongit = params[3];
            mnature = params[4];
            getResults(mlatit, mlongit, ilatit, ilongit, mnature);
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            distanceMeasuring(mlatit, mlongit, ilatit, ilongit, "places");
            newIntentStarter();
        }
    }

    public void distanceMeasuring(String mlatit, String mlongit, String ilatit, String ilongit, String what) {
        Location myLocation = new Location("myLocation");
        myLocation.setLatitude(Double.parseDouble(mlatit));
        myLocation.setLongitude(Double.parseDouble(mlongit));

        Location interLocation = new Location("interLocation");
        interLocation.setLatitude(Double.parseDouble(ilatit));
        interLocation.setLongitude(Double.parseDouble(ilongit));

        if (what.equals("users")) {
            if ((myLocation.distanceTo(interLocation)) > 500) {
                distanceBetweenUsers = String.valueOf(myLocation.distanceTo(interLocation) / 1000).substring(0, 4) + " km";
            } else {
                distanceBetweenUsers = String.valueOf(myLocation.distanceTo(interLocation)).substring(0, 3) + " m";
            }

        } else {
            for (int i = 0; i <= placesLocations.size() - 1; i++) {
                if ((myLocation.distanceTo(placesLocations.get(i))) > 500) {
                    Mdistances.add(String.valueOf((myLocation.distanceTo(placesLocations.get(i))) / 1000).substring(0, 4) + " km");
                } else {
                    Mdistances.add(String.valueOf(myLocation.distanceTo(placesLocations.get(i))).substring(0, 3) + " m");
                }
                if ((interLocation.distanceTo(placesLocations.get(i))) > 500) {
                    Idistances.add(String.valueOf((interLocation.distanceTo(placesLocations.get(i))) / 1000).substring(0, 4) + " km");
                } else {
                    Idistances.add(String.valueOf(interLocation.distanceTo(placesLocations.get(i))).substring(0, 3) + " m");
                }
            }
        }
    }

    public void newIntentStarter() {

        Intent newint = new Intent(MeetAsapOptionsD.this, RecommendationsList.class);

        newint.putStringArrayListExtra("placesNames", (ArrayList<String>) placesNames);
        newint.putStringArrayListExtra("placesVicinities", (ArrayList<String>) placesVicinities);
        newint.putStringArrayListExtra("placesRatings", (ArrayList<String>) placesRatings);
        newint.putStringArrayListExtra("placesLats", (ArrayList<String>) placesLats);
        newint.putStringArrayListExtra("placesLngs", (ArrayList<String>) placesLngs);
        newint.putStringArrayListExtra("Mdistances", (ArrayList<String>) Mdistances);
        newint.putStringArrayListExtra("Idistances", (ArrayList<String>) Idistances);
        newint.putStringArrayListExtra("placesRefs", (ArrayList<String>) placesRefs);
        newint.putStringArrayListExtra("placesIcons", (ArrayList<String>) placesIcons);

        newint.putExtra("sMode", sMode);
        newint.putExtra("sessionId", mSessionId);
        newint.putExtra("meetingNature", mNature);
        newint.putExtra("myCoordinates", mCoordinates);
        newint.putExtra("myMode", mMode);
        newint.putExtra("interCoordinates", iCoordinates);
        newint.putExtra("interMode", iMode);


        Log.d("meetAsapError", "OptionsD: newintentstarter");
        startActivity(newint);
    }

    protected void getResults(String mlatit, String mlongit, String ilatit, String ilongit, String mNature) {
        Log.d("ServerPlacePicker", "entering getPlaces");
        // Define the uri that is used to get lat and long for our address

        String uri = stringComposer(mlatit, mlongit, ilatit, ilongit, mNature);
        Log.d("meetAsapError", "OptionsD - uri set... http getting...");

        HttpGet httpMyGet = new HttpGet(uri);
        Log.d("meetAsapError", "OptionsD - uri set... http client...");
        HttpClient client = new DefaultHttpClient();

        HttpResponse response;

        StringBuilder stringBuilder = new StringBuilder();

        try {
            Log.d("meetAsapError", "OptionsD - try response...");
            response = client.execute(httpMyGet);
            Log.d("meetAsapError", "OptionsD - got response.... try entity");
            HttpEntity entity = response.getEntity();

            InputStream stream = entity.getContent();
            int byteData;

            while ((byteData = stream.read()) != -1) {
                stringBuilder.append((char) byteData);
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        JSONObject jsonObject;
        try {
            String results = stringBuilder.toString().substring(23);
            jsonObject = new JSONObject(results);

            Log.d("json", results);
            Log.d("json", "length " + ((JSONArray) jsonObject.get("results")).length());

            JSONArray places = jsonObject.getJSONArray("results");

            for (int l = 0; l <= places.length() - 1; l++) {
                Log.d("ServerPlacePicker", "element nr :" + (l + 1) + " out of: " + places.length());
                placesRefs.add(places.getJSONObject(l).getString("reference"));
                placesIcons.add(places.getJSONObject(l).getString("icon"));
                placesNames.add(places.getJSONObject(l).getString("name"));
                placesVicinities.add(places.getJSONObject(l).getString("vicinity"));
                placesLats.add(String.valueOf(places.getJSONObject(l).getJSONObject("geometry").getJSONObject("location")
                        .getDouble("lat")));
                placesLngs.add(String.valueOf(places.getJSONObject(l).getJSONObject("geometry").getJSONObject("location")
                        .getDouble("lng")));
                Location tempLoc = new Location("tempLoc");

                tempLoc.setLatitude(places.getJSONObject(l).getJSONObject("geometry").getJSONObject("location")
                        .getDouble("lat"));
                tempLoc.setLongitude(places.getJSONObject(l).getJSONObject("geometry").getJSONObject("location")
                        .getDouble("lng"));
                placesLocations.add(tempLoc);
                try {
                    placesRatings.add("rating: " + String.valueOf(places.getJSONObject(l).getDouble("rating")));
                } catch (JSONException e) {
                    placesRatings.add("");
                }

            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public String stringComposer(String mlatit, String mlongit, String ilatit, String ilongit, String meetingNature) {
        double mla = Double.parseDouble(mlatit);
        double mlo = Double.parseDouble(mlongit);
        double ila = Double.parseDouble(ilatit);
        double ilo = Double.parseDouble(ilongit);

        double lat = (mla + ila) / 2;
        double lon = (mlo + ilo) / 2;
        String middleLat = String.valueOf(lat);
        String middleLon = String.valueOf(lon);

        String radius = radiusCalculating(mla, mlo, ila, ilo);
        Log.d("meetAsapError", "OptionsD - middlelat: " + middleLat + ", " + middleLon + ", radius: " + radius);
        String types = typesDefining(meetingNature);

        //with radius
        //String uri = "http://51.254.128.180/pbbilu/www/showplaces.php?myurl=https%3A%2F%2Fmaps.googleapis.com%2Fmaps%2Fapi%2Fplace%2Fnearbysearch%2Fjson%3Flocation%3D" + middleLat + "%2C" + middleLon + "%26radius%3D" + radius + "%26sensor%3Dtrue%26types%3D" + types + "%26key%3DAIzaSyCW-ixu6yPyaZQwTkLMYwMKcYXKAHjKrdA";

        //with rankby
        String uri = "http://51.254.128.180/pbbilu/www/showplaces.php?myurl=https%3A%2F%2Fmaps.googleapis.com%2Fmaps%2Fapi%2Fplace%2Fnearbysearch%2Fjson%3Flocation%3D" + middleLat + "%2C" + middleLon + "%26rankby%3Ddistance%26sensor%3Dtrue%26types%3D" + types + "%26key%3DAIzaSyCW-ixu6yPyaZQwTkLMYwMKcYXKAHjKrdA";

        Log.d("meetAsapError", "OptionsD - uri: " + uri);
        return uri;
    }

    public String radiusCalculating(double latA, double lngA, double latB, double lngB) {
        Location locationA = new Location("point A");

        locationA.setLatitude(latA);
        locationA.setLongitude(lngA);
        Log.d("meetAsapError", "OptionsD - locA: " + locationA.toString());
        Location locationB = new Location("point B");

        locationB.setLatitude(latB);
        locationB.setLongitude(lngB);
        Log.d("meetAsapError", "OptionsD - locB: " + locationB.toString());
        float distance = locationA.distanceTo(locationB);
        if (distance > 5000)
            distance = (distance / (5));
        else
            distance = 500;
        Log.d("meetAsapError", "OptionsD - dist: " + String.valueOf(distance));
        return String.valueOf(distance);
    }

    public String typesDefining(String meetingNature) {
        String types = "";
        if (meetingNature.equals("eating")) {
            types = "food";
        } else if (meetingNature.equals("drinking")) {
            types = "bar";
        } else if (meetingNature.equals("talking")) {
            types = "park";
        } else if (meetingNature.equals("picking up")) {
            types = "parking";
        } else if (meetingNature.equals("shopping")) {
            types = "store";
        } else if (meetingNature.equals("party")) {
            types = "night_club%7Cbar";
        } else if (meetingNature.equals("entertainment")) {
            types = "zoo%7Cbar%7Camusement_park%7Caquarium%7Cart_gallery%7Cmovie_theater";
        }
        return types;
    }

}
