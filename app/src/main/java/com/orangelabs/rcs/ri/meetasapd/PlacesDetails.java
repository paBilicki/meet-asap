package com.orangelabs.rcs.ri.meetasapd;
/**
 * Created by Bilu on 2015-09-28.
 */

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.media.Image;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
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
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.meetasapcommon.MeetAsapGpsTracker;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PlacesDetails extends FragmentActivity implements
        OnMapReadyCallback {

    private MeetAsapGpsTracker GPS;
    private GoogleMap meetmap;

    ImageButton backButton, navigateButton, getPathButton;
    TextView pname, prating, paddress, pphone, mdist, idist;

    // place data
    String placeName, placeVicinity, placeRating, placeLat, placeLng, Mdistance, Idistance, placeRef, placePhone, sMode;
    ImageView myModeView, intModeView;

    // strings for prenavigation screen
    String mNature, mCoordinates, mMode, iCoordinates, iMode;

    // variables for update map
    private LatLng myPosition, iPosition;
    Marker myMarker, interMarker, middleMarker, placeMarker;
    int markers = 0;

    // variables for coordinates update
    double iLatitude, iLongitude, mLatitude = 1, mLongitude = 1, pLatitude, pLongitude;

    // variables for displaying a path
    Boolean myTurn = true;
    String mla, mlo, ila, ilo;

    CameraUpdate cu;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.meet_place_details_screen);




        Log.d("meetAsapError", "PlaceDetails - Creating Map..");

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Log.d("meetAsapError",
                "PlaceDetails - Map created! Creating TVs...");
        pname = (TextView) findViewById(R.id.place_name);
        prating = (TextView) findViewById(R.id.place_rating);
        pphone = (TextView) findViewById(R.id.place_phone);
        paddress = (TextView) findViewById(R.id.place_address);
        mdist = (TextView) findViewById(R.id.my_distance);
        idist = (TextView) findViewById(R.id.int_distance);
        myModeView = (ImageView) findViewById(R.id.my_mode);
        intModeView = (ImageView) findViewById(R.id.int_mode);


        Log.d("meetAsapError",
                "PlaceDetails - TVs Created! Creating GPS...");

        GPS = new MeetAsapGpsTracker(PlacesDetails.this);

        Log.d("meetAsapError",
                "PlaceDetails -  - GPS created! Getting extras...");

        placeName = getIntent().getStringExtra("placesName");
        placeVicinity = getIntent().getStringExtra("placesVicinity");
        placeRating = getIntent().getStringExtra("placesRating");
        placeLat = getIntent().getStringExtra("placesLat");
        placeLng = getIntent().getStringExtra("placesLng");
        placePhone = getIntent().getStringExtra("placePhone");
        Mdistance = getIntent().getStringExtra("Mdistance");
        Idistance = getIntent().getStringExtra("Idistance");

        mNature = getIntent().getStringExtra("meetingNature");
        mCoordinates = getIntent().getStringExtra("myCoordinates");
        mMode = getIntent().getStringExtra("myMode");
        iCoordinates = getIntent().getStringExtra("interCoordinates");
        iMode = getIntent().getStringExtra("interMode");
        sMode = getIntent().getStringExtra("sessionMode");

        //for demo
        if (sMode.equals("incoming")) {
            iCoordinates = "48.7334647,-3.4612257";
            mCoordinates = "48.732309, -3.465897";
        } else {
            mCoordinates = "48.7334647,-3.4612257";
            iCoordinates = "48.732309, -3.465897";
        }

        Log.d("meetAsapError", "PlacesDetails - extras taken... Creating Buttons...");

        backButton = (ImageButton) findViewById(R.id.meet_btn_back);
        getPathButton = (ImageButton) findViewById(R.id.meet_btn_path);
        navigateButton = (ImageButton) findViewById(R.id.meet_btn_navigate);


        getPathButton.setOnClickListener(getPathButtonListener);
        navigateButton.setOnClickListener(navigateButtonListener);

        if (placeRating.length() == 0) {
            prating.setVisibility(View.GONE);
        } else {
            prating.setText(placeRating);
        }


        if (placePhone.length() == 0) {
            pphone.setVisibility(View.GONE);
        } else {
            pphone.setText(placePhone);
        }

        pname.setText(placeName);
        paddress.setText(placeVicinity);
        pphone.setText(placePhone);
        mdist.setText(Mdistance);
        idist.setText(Idistance);

        if (mMode.equals("foot")) {
            myModeView.setImageResource(R.drawable.me_walk);
            navigateButton.setImageResource(R.drawable.btn_nav_walk);
        } else if (mMode.equals("bike")) {
            myModeView.setImageResource(R.drawable.me_bike);
            navigateButton.setImageResource(R.drawable.btn_nav_bike);
        } else if (mMode.equals("car")) {
            myModeView.setImageResource(R.drawable.me_car);
            navigateButton.setImageResource(R.drawable.btn_nav_car);
        } else if (mMode.equals("public transport")) {
            myModeView.setImageResource(R.drawable.me_public);
            navigateButton.setImageResource(R.drawable.btn_nav_public);
        }

        if (iMode.equals("foot")) {
            intModeView.setImageResource(R.drawable.int_walk);
        } else if (iMode.equals("bike")) {
            intModeView.setImageResource(R.drawable.int_bike);
        } else if (iMode.equals("car")) {
            intModeView.setImageResource(R.drawable.int_car);
        } else if (iMode.equals("public transport")) {
            intModeView.setImageResource(R.drawable.int_public);
        }

        pLatitude = Double.valueOf(placeLat);
        pLongitude = Double.valueOf(placeLng);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        Log.d("meetAsapError", "PlaceDetails - onMapReady");
        meetmap = map;
        updateMap();
    }

    private void updateMap() {
        Log.d("meetAsapError", "PlaceDetails - updateMap");
        updateMyCoordinates();
        updateInterlocutorsCoordinates();

        if (markers == 1) {
            myMarker.remove();
            interMarker.remove();
            placeMarker.remove();
        }

        myMarker = meetmap.addMarker(new MarkerOptions()
                .position(myPosition)
                .title("I am here")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.me_icon)));


        interMarker = meetmap
                .addMarker(new MarkerOptions()
                        .position(iPosition)
                        .title("My interlocutor is there")
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.int_icon)));

        LatLng placePosition = new LatLng(pLatitude, pLongitude);
        placeMarker = meetmap
                .addMarker(new MarkerOptions()
                        .position(placePosition)
                        .title("Chosen place is here")
                        .icon(BitmapDescriptorFactory
                                .defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));


        double middleLat = (mLatitude + iLatitude) / 2;
        double middleLong = (mLongitude + iLongitude) / 2;
        LatLng middlePosition = new LatLng(middleLat, middleLong);

        markers = 1;

        cu = viewPortAdjust(mLatitude, mLongitude, pLatitude, pLongitude, iLatitude, iLongitude);


        Log.d("meetAsapError", "PlaceDetails - updateMap - myPosition: " + myPosition.toString());
        Log.d("meetAsapError", "PlaceDetails - updateMap - interPosition: " + iPosition.toString());
        Log.d("meetAsapError", "PlaceDetails - updateMap - middlePosition: " + middlePosition.toString());
        Log.d("meetAsapError", "PlaceDetails - updateMap - placePosition: " + placePosition.toString());
    }

    private View.OnClickListener getPathButtonListener = new View.OnClickListener() {
        public void onClick(View v) {
            Log.d("meetAsapError", "PlacesDetails - refreshButtonListener");
            getPath();
            meetmap.animateCamera(cu);
        }
    };
    private View.OnClickListener navigateButtonListener = new View.OnClickListener() {
        public void onClick(View v) {
            Log.d("meetAsapError", "PlacesDetails - navigateButtonListener");
            new GetDirections().execute();

        }
    };

    private void updateInterlocutorsCoordinates() {
        String[] iCoordinatesSplitted = iCoordinates.split(",");

        Log.d("meetAsapError", "PlacesDetails - updateInterlocutorsCoordinates");

        iLatitude = Double.parseDouble(iCoordinatesSplitted[0]);
        iLongitude = Double.parseDouble(iCoordinatesSplitted[1]);

        ila = String.valueOf(iLatitude);
        ilo = String.valueOf(iLongitude);

        iPosition = new LatLng(iLatitude, iLongitude);
    }

    private void updateMyCoordinates() {
        Log.d("meetAsapError", "PlacesDetails - updateMyCoordinates");
        try {
            if (GPS.canGetLocation()) {
//                mLatitude = GPS.getLatitude();
//                mLongitude = GPS.getLongitude();



                String[] mCoordinatesSplitted = mCoordinates.split(",");
                mLatitude = Double.parseDouble(mCoordinatesSplitted[0]);
                mLongitude = Double.parseDouble(mCoordinatesSplitted[1]);


                myPosition = new LatLng(mLatitude, mLongitude);
                mla = String.valueOf(mLatitude);
                mlo = String.valueOf(mLongitude);

                mCoordinates = (mla + ", " + mlo);
            }
        } catch (Exception e) {
            Log.d("meetAsapError",
                    "PlacesDetailes");
        }
    }


    public void getPath() {
        String mMot, iMot;

        if (mMode.equals("car")) {
            mMot = "driving";
        } else if (mMode.equals("walk")) {
            mMot = "walking";
        } else {
            mMot = "walking";
        }

        if (iMode.equals("car")) {
            iMot = "driving";
        } else if (iMode.equals("walk")) {
            iMot = "walking";
        } else {
            iMot = "walking";
        }


        String pathUrl1 = "http://51.254.128.180/pbbilu/www/showplaces.php?myurl=https%3A%2F%2Fmaps.googleapis.com%2Fmaps%2Fapi%2Fdirections%2Fjson%3Forigin%3D" + mla + "%2C" + mlo + "%26destination%3D" + placeLat + "%2C" + placeLng + "%26sensor%3Dfalse%26mode%3D" + mMot + "%26alternatives%3Dfalse%26key%3DAIzaSyCW-ixu6yPyaZQwTkLMYwMKcYXKAHjKrdA";
        String pathUrl2 = "http://51.254.128.180/pbbilu/www/showplaces.php?myurl=https%3A%2F%2Fmaps.googleapis.com%2Fmaps%2Fapi%2Fdirections%2Fjson%3Forigin%3D" + ila + "%2C" + ilo + "%26destination%3D" + placeLat + "%2C" + placeLng + "%26sensor%3Dfalse%26mode%3D" + iMot + "%26alternatives%3Dfalse%26key%3DAIzaSyCW-ixu6yPyaZQwTkLMYwMKcYXKAHjKrdA";

        Log.d("pathBuilder", "pathUrl: " + pathUrl1);

        new connectAsyncTask(pathUrl1).execute();

        new connectAsyncTask(pathUrl2).execute();
    }


    private class connectAsyncTask extends AsyncTask<Void, Void, String> {
        private ProgressDialog progressDialog;
        String url;

        connectAsyncTask(String urlPass) {
            url = urlPass;
        }

        @Override
        protected void onPreExecute() {
            Log.d("pathBuilder", "onPreExecute");
            super.onPreExecute();
            progressDialog = new ProgressDialog(PlacesDetails.this);
            progressDialog.setMessage("Fetching route, Please wait...");
            progressDialog.setIndeterminate(true);
            progressDialog.show();
        }

        @Override
        protected String doInBackground(Void... params) {
            Log.d("pathBuilder", "doInBackground");
            JSONParser jParser = new JSONParser();
            String json = jParser.getJSONFromUrl(url);
            return json;
        }

        @Override
        protected void onPostExecute(String result) {
            Log.d("pathBuilder", "onPostExecute");
            super.onPostExecute(result);
            progressDialog.hide();
            if (result != null) {
                drawPath(result);
            }
        }
    }

    public class JSONParser {

        InputStream is = null;
        JSONObject jObj = null;
        String json = "";

        // constructor
        public JSONParser() {
        }

        public String getJSONFromUrl(String url) {
            Log.d("pathBuilder", "getJSONFromUrl");
            // Making HTTP request
            try {
                // defaultHttpClient
                DefaultHttpClient httpClient = new DefaultHttpClient();
                HttpGet httpMyGet = new HttpGet(url);

                HttpResponse httpResponse = httpClient.execute(httpMyGet);
                HttpEntity httpEntity = httpResponse.getEntity();
                is = httpEntity.getContent();

            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        is, "iso-8859-1"), 8);
                StringBuilder sb = new StringBuilder();
                String line = null;
                while ((line = reader.readLine()) != null) {
                    sb.append(line + "\n");
                }

                json = sb.toString();
                is.close();
            } catch (Exception e) {
                Log.e("Buffer Error", "Error converting result " + e.toString());
            }
            Log.d("pathBuilder", "parsed json: " + json);
            return json;

        }
    }

    public void drawPath(String result) {
        Log.d("pathBuilder", "drawPath");
        try {
            //Tranform the string into a json object
            final JSONObject json = new JSONObject(result.substring(23));

            Log.d("pathBuilder", "json: " + json.toString());

            JSONArray routeArray = json.getJSONArray("routes");
            JSONObject routes = routeArray.getJSONObject(0);
            JSONObject overviewPolylines = routes.getJSONObject("overview_polyline");
            String encodedString = overviewPolylines.getString("points");

            List<LatLng> list = decodePoly(encodedString);

            Log.d("pathBuilder", "it is myTurn: " + myTurn.toString());
            PolylineOptions line;
            if (myTurn) {
                 line = new PolylineOptions()
                        .addAll(list)
                        .width(12)
                        .color(Color.parseColor("#05b1fb"))//Google maps blue color
                        .geodesic(true);
                myTurn = false;
            } else {
                line = new PolylineOptions()
                        .addAll(list)
                        .width(9)
                        .color(Color.parseColor("#00cc00"))//Google maps blue color
                        .geodesic(true);

            }
            meetmap.addPolyline(line);
            updateMap();


        } catch (JSONException e) {

        }
    }

    public CameraUpdate viewPortAdjust(double mla, double mlo, double pla, double plo, double ila, double ilo) {
        double[] lats = {mla, pla, ila};
        double[] lngs = {mlo, plo, ilo};

        Log.d("pathBuilder", "lats: " + lats[0] + ", " + lats[1] + ", " + lats[2]);
        Log.d("pathBuilder", "lngs: " + lngs[0] + ", " + lngs[1] + ", " + lngs[2]);
        Arrays.sort(lats);
        Arrays.sort(lngs);
        Log.d("pathBuilder", "sorted lats: " + lats[0] + ", " + lats[1] + ", " + lats[2]);
        Log.d("pathBuilder", "sorted lngs: " + lngs[0] + ", " + lngs[1] + ", " + lngs[2]);
        double leftToplat = lats[0];
        double leftToplng = lngs[lats.length - 1];

        double rightBottomlat = lats[lngs.length - 1];
        double rightBottomlng = lngs[0];

        LatLng leftTop = new LatLng(leftToplat, leftToplng);
        LatLng rightBottom = new LatLng(rightBottomlat, rightBottomlng);

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(leftTop);
        builder.include(rightBottom);
        LatLngBounds bounds = builder.build();

        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, 200);
        return cu;
    }

    private List<LatLng> decodePoly(String encoded) {
        Log.d("pathBuilder", "decodePoly");
        List<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }

    class GetDirections extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... params) {
            return null;
        }

        protected void onPostExecute(String s) {
            super.onPostExecute(s);

//             Create the URL for Google Maps to get the directions
            String geoUriString = "http://maps.google.com/maps?saddr=" + mCoordinates + "&daddr=" + placeLat + "," + placeLng;
//              Call for Google Maps to open
            Log.d("checking", "geoUriString: " + geoUriString);
            Intent mapCall = new Intent(Intent.ACTION_VIEW, Uri.parse(geoUriString));

            startActivity(mapCall);

        }
    }
}



