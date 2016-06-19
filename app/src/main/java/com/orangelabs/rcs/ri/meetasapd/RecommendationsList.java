package com.orangelabs.rcs.ri.meetasapd;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.location.places.Place;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.extension.MultimediaMessagingSession;
import com.gsma.services.rcs.extension.MultimediaMessagingSessionListener;
import com.gsma.services.rcs.extension.MultimediaSession;
import com.orangelabs.rcs.ri.ConnectionManager;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.RiApplication;
import com.orangelabs.rcs.ri.extension.messaging.MessagingSessionUtils;
import com.orangelabs.rcs.ri.utils.LockAccess;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.Utils;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Created by Bilu on 2015-09-25.
 */
public class RecommendationsList extends Activity {
    private ConnectionManager mCnxManager;
    private LockAccess mExitOnce = new LockAccess();
    private final Handler mHandler = new Handler();
    private MultimediaMessagingSession mSession;
    private String mServiceId = MessagingSessionUtils.SERVICE_ID, mSessionId;
    String sessionId;

    ArrayList<String> placesNames;
    ArrayList<String> placesVicinities;
    ArrayList<String> placesRatings;
    ArrayList<String> placesLats;
    ArrayList<String> placesLngs;
    ArrayList<String> Mdistances;
    ArrayList<String> Idistances;
    ArrayList<String> placesRefs;
    ArrayList<String> placesIcons;
    String placePhone, sMode;

    // strings of a chosen place
    String placesName, placesVicinity, placesRating, placesLat, placesLng, Mdistance, Idistance, placesRef;

    // strings for prenavigation screen
    String mNature, mCoordinates, mMode, iCoordinates, iMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.meet_recommandations_list);

        placesNames = getIntent().getStringArrayListExtra("placesNames");
        placesVicinities = getIntent().getStringArrayListExtra("placesVicinities");
        placesRatings = getIntent().getStringArrayListExtra("placesRatings");
        placesLats = getIntent().getStringArrayListExtra("placesLats");
        placesLngs = getIntent().getStringArrayListExtra("placesLngs");
        Mdistances = getIntent().getStringArrayListExtra("Mdistances");
        Idistances = getIntent().getStringArrayListExtra("Idistances");
        placesRefs = getIntent().getStringArrayListExtra("placesRefs");
        placesIcons = getIntent().getStringArrayListExtra("placesIcons");
        sMode = getIntent().getStringExtra("sMode");
        sessionId = getIntent().getStringExtra("sessionId");
        Log.d("meetAsapError", "RecommendationsList: onCreate ");

        mNature = getIntent().getStringExtra("meetingNature");
        mCoordinates = getIntent().getStringExtra("myCoordinates");
        mMode = getIntent().getStringExtra("myMode");
        iCoordinates = getIntent().getStringExtra("interCoordinates");
        iMode = getIntent().getStringExtra("interMode");


        ListAdapter myAdapter = new CustomAdapter(this, placesNames, placesRatings, Mdistances, Idistances, placesIcons, mMode, iMode);
        ListView myListView = (ListView) findViewById(R.id.recList);
        myListView.setAdapter(myAdapter);

        myListView.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                        placesName = placesNames.get(position);
                        placesVicinity = placesVicinities.get(position);
                        placesRating = placesRatings.get(position);
                        placesLat = placesLats.get(position);
                        placesLng = placesLngs.get(position);
                        Mdistance = Mdistances.get(position);
                        Idistance = Idistances.get(position);
                        placesRef = placesRefs.get(position);

                        if (sMode.equals("outgoing")) {
                            Log.d("meetAsapError", "RecommendationsList - Host chosen the place nr: " + position);
                            sendMyMessage(String.valueOf(position));
                            getDetails();
                        } else {
                            Log.d("meetAsapError", "RecommendationsList - Guest clicked on the place nr: " + position);
                            Toast.makeText(RecommendationsList.this, placesName, Toast.LENGTH_SHORT).show();
                            sendMyMessage(String.valueOf(position));
                        }
                    }
                }
        );


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
                    "RecommendationsList - starting initialiseMessagingSession");
        } catch (RcsServiceException e) {
            if (LogUtils.isActive) {
                Log.e("meetAsapError", "RecommendationsList - Failed to add listener", e);
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
                    Log.e("meetAsapError", "RecommendationsList - Failed to remove listener", e);
                }
            }
        }
    }

    private void sendMyMessage(String msg) {
        try {
            mSession.sendMessage(msg.getBytes());
        } catch (Exception e) {
            Utils.showMessageAndExit(this, "meetAsapError", mExitOnce, e);
        }
    }

    public void getDetails() {
        new GetDetails().execute(placesRef);
    }

    class GetDetails extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... params) {
            // Get the latitude address
            String placesRef = params[0];
            getDetails(placesRef);

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            newIntentStarter();
        }

        public void newIntentStarter() {
            Log.d("meetAsapError",
                    "RecommendationsList - GetDetails - newIntentStarter");

            Intent newint = new Intent(RecommendationsList.this, PlacesDetails.class);

            newint.putExtra("placesName", placesName);
            newint.putExtra("placesVicinity", placesVicinity);
            newint.putExtra("placesRating", placesRating);
            newint.putExtra("placesLat", placesLat);
            newint.putExtra("placesLng", placesLng);
            newint.putExtra("Mdistance", Mdistance);
            newint.putExtra("Idistance", Idistance);
            newint.putExtra("placesRef", placesRef);
            newint.putExtra("placePhone", placePhone);

            newint.putExtra("sessionMode", sMode);
            newint.putExtra("meetingNature", mNature);
            newint.putExtra("myCoordinates", mCoordinates);
            newint.putExtra("myMode", mMode);
            newint.putExtra("interCoordinates", iCoordinates);
            newint.putExtra("interMode", iMode);

            startActivity(newint);
        }

        protected void getDetails(String placesRef) {
            Log.d("meetAsapError",
                    "RecommendationsList - GetDetails -  entering getDetails function");
            // Define the uri that is used to get lat and long for our address

            String uri = stringComposer(placesRef);
            Log.d("meetAsapError",
                    "RecommendationsList - GetDetails - uri set... http getting...");

            HttpGet httpMyGet = new HttpGet(uri);
            Log.d("meetAsapError",
                    "RecommendationsList - GetDetails - uri set... http client...");
            HttpClient client = new DefaultHttpClient();

            HttpResponse response;

            StringBuilder stringBuilder = new StringBuilder();

            try {
                Log.d("meetAsapError",
                        "RecommendationsList - GetDetails - trying to get response");
                response = client.execute(httpMyGet);

                Log.d("meetAsapError",
                        "RecommendationsList - GetDetails - got response.... try entity");
                HttpEntity entity = response.getEntity();

                InputStream stream = entity.getContent();

                Log.d("meetAsapError",
                        "RecommendationsList - GetDetails - building string");
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
            Log.d("meetAsapError",
                    "RecommendationsList - GetDetails - getting phone number");
            try {
                String results = stringBuilder.toString().substring(23);
                jsonObject = new JSONObject(results);

                JSONObject details = jsonObject.getJSONObject("result");

                try {
                    placePhone = (details.getString("international_phone_number"));
                } catch (JSONException e) {
                    placePhone = "";
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

        public String stringComposer(String placeRef) {

            String uri = "http://51.254.128.180/pbbilu/www/showplaces.php?myurl=https%3A%2F%2Fmaps.googleapis.com%2Fmaps%2Fapi%2Fplace%2Fdetails%2Fjson%3Freference%3D" + placeRef + "%26key%3DAIzaSyCW-ixu6yPyaZQwTkLMYwMKcYXKAHjKrdA";
            Log.d("meetAsapError", "RecommendationsList - GetDetails - uri: " + uri);
            return uri;
        }

    }

    private MultimediaMessagingSessionListener mServiceListener = new MultimediaMessagingSessionListener() {

        @Override
        public void onStateChanged(ContactId contact, String sessionId,
                                   final MultimediaSession.State state,
                                   MultimediaSession.ReasonCode reasonCode) {
            Log.d("meetAsapError", "RecommendationsList - GetDetails - onStateChanged contact=" + contact
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
                                    RecommendationsList.this,
                                    getString(R.string.label_session_failed,
                                            _reasonCode), mExitOnce);
                            break;

                        default:
                            if (LogUtils.isActive) {
                                Log.d("meetAsapError",
                                        "RecommendationsList - onStateChanged "
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
                Log.d("meetAsapError", "RecommendationsList - onMessageReceived contact: " + contact
                        + " sessionId: " + sessionId);
            }
            final String data = new String(content);

            mHandler.post(new Runnable() {
                public void run() {
                    int clickedPosition = Integer.valueOf(data);
                    if (sMode.equals("outgoing")) {
                        Toast.makeText(getBaseContext(),
                                "Your interlocutor clicked on the place nr: " + (clickedPosition + 1),
                                Toast.LENGTH_LONG).show();
                        Log.d("meetAsapError", "RecommendationsList - Interlocutor sent nr : " + data);
                    } else {

                        Toast.makeText(getBaseContext(),
                                "Host has chosen position nr: " + (clickedPosition +1 ),
                                Toast.LENGTH_LONG).show();

                        Log.d("meetAsapError", "RecommendationsList - Host sent nr : " + data);

                        placesName = placesNames.get(clickedPosition);
                        placesVicinity = placesVicinities.get(clickedPosition);
                        placesRating = placesRatings.get(clickedPosition);
                        placesLat = placesLats.get(clickedPosition);
                        placesLng = placesLngs.get(clickedPosition);
                        Mdistance = Mdistances.get(clickedPosition);
                        Idistance = Idistances.get(clickedPosition);
                        placesRef = placesRefs.get(clickedPosition);

                        getDetails();
                    }
                }
            });
        }


    };
}