/**
 * @author Piotr Bilicki
 */
package com.orangelabs.rcs.ri.meetasapc;

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
import com.gsma.services.rcs.contact.ContactId;
import com.orangelabs.rcs.ri.ConnectionManager;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.extension.messaging.MessagingSessionUtils;
import com.orangelabs.rcs.ri.meetasapcommon.MeetAsapGpsTracker;
import com.orangelabs.rcs.ri.utils.LockAccess;

import com.orangelabs.rcs.ri.utils.Utils;


/**
 * This shows how to create a simple activity with a map and a marker on the
 * map.
 */
public class MeetAsapPreNavigationC extends FragmentActivity implements
        OnMapReadyCallback {

    private LatLng myPosition, iPosition;
    private MeetAsapGpsTracker GPS;
    private GoogleMap meetmap;
    Marker myMarker, interMarker, middleMarker;
    Polyline myPolyline;

    private String mServiceId = MessagingSessionUtils.SERVICE_ID;
    private ContactId mMeetContact;
    private ConnectionManager mCnxManager;
    private LockAccess mExitOnce = new LockAccess();

    Button refreshButton, sendButton;
    TextView sessionId, remoteContact, sessionMode, myCoordinates, myMode, meetingNature, interCoordinates, interMode;
    String sMode, mMode, iMode, mCoordinates, iCoordinates, mNature, rContact, sId;
    double iLatitude, iLongitude, mLatitude = 1, mLongitude = 1;
    int markers = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.meet_prenavigation_screen_c);

        Log.d("meetAsapError", "MeetAsapPreNavigationB - Creating Map..");

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Log.d("meetAsapError",
                "MeetAsapPreNavigationB - Map created! Creating TVs...");
        sessionId = (TextView) findViewById(R.id.session_id);
        remoteContact = (TextView) findViewById(R.id.remote_contact);
        sessionMode = (TextView) findViewById(R.id.session_mode);
        meetingNature = (TextView) findViewById(R.id.chosen_nature);
        myCoordinates = (TextView) findViewById(R.id.my_coord);
        myMode = (TextView) findViewById(R.id.my_mode);
        interCoordinates = (TextView) findViewById(R.id.inter_coord);
        interMode = (TextView) findViewById(R.id.inter_mode);

        Log.d("meetAsapError",
                "MeetAsapPreNavigationB - TVs created! Creating Buttons...");
        refreshButton = (Button) findViewById(R.id.meet_refresh);
        sendButton = (Button) findViewById(R.id.meet_send);
        sendButton.setVisibility(View.GONE);
        refreshButton.setOnClickListener(refreshButtonListener);
        sendButton.setOnClickListener(sendButtonListener);

        Log.d("meetAsapError",
                "MeetAsapPreNavigationB - Buttons Created! Creating GPS...");

        GPS = new MeetAsapGpsTracker(MeetAsapPreNavigationC.this);

        Log.d("meetAsapError",
                "MeetAsapPreNavigationB -  - GPS created! Getting extras...");

        //mMeetContact = getIntent().getParcelableExtra(MeetAsapContactsD.EXTRA_CONTACT);
        sId = getIntent().getStringExtra("sessionId");
        rContact = getIntent().getStringExtra("remoteContact");
        sMode = getIntent().getStringExtra("sessionMode");
        mNature = getIntent().getStringExtra("meetingNature");
        mCoordinates = getIntent().getStringExtra("myCoordinates");
        mMode = getIntent().getStringExtra("myMode");
        iCoordinates = getIntent().getStringExtra("interCoordinates");
        iMode = getIntent().getStringExtra("interMode");

        Log.d("meetAsapError", "MeetAsapPreNavigationD - extras taken");

        sessionId.setText("Session ID: " + sId);
        sessionId.setVisibility(View.GONE);
        remoteContact.setText("Remote Contact: " + rContact);
        if (sMode== null)
            sessionMode.setText("session mode: incoming");
        else
            sessionMode.setText("Session mode: " + sMode);
        meetingNature.setText("Meeting nature: " + mNature);
        myCoordinates.setText("My coordinates: " + mCoordinates);
        myMode.setText("My mode of transport: " + mMode);
        interCoordinates.setText("Interlocutor's coordinates: " + iCoordinates);
        interMode.setText("Interlocutor's mode of transport: " + iMode);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        meetmap = map;
        //meetmap.setMyLocationEnabled(true);
    }

    private void updateMap() {
        if (markers == 1) {
            myMarker.remove();
            interMarker.remove();
            middleMarker.remove();
            myPolyline.remove();
        }
        myMarker = meetmap.addMarker(new MarkerOptions()
                .position(myPosition)
                .title("I am here")
                .icon(BitmapDescriptorFactory
                        .defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));

        updateInterlocutorsCoordinates();
        interMarker = meetmap
                .addMarker(new MarkerOptions()
                        .position(iPosition)
                        .title("My interlocutor is there")
                        .icon(BitmapDescriptorFactory
                                .defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

        double middleLat = (mLatitude + iLatitude) / 2;
        double middleLong = (mLongitude + iLongitude) / 2;
        LatLng middlePosition = new LatLng(middleLat, middleLong);
        middleMarker = meetmap.addMarker(new MarkerOptions()
                .position(middlePosition)
                .title("Our meeting point is here")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));

        PolylineOptions myLineOptions = new PolylineOptions()
                .add(myPosition)
                .add(middlePosition)
                .color(Color.BLUE);

        myPolyline = meetmap.addPolyline(myLineOptions);

        markers = 1;
        CameraUpdate center = CameraUpdateFactory.newLatLng(middlePosition);
        CameraUpdate zoom = CameraUpdateFactory.zoomTo(14);
        meetmap.moveCamera(center);
        meetmap.animateCamera(zoom);
    }

    private void updateInterlocutorsCoordinates(){
        String[] iCoordinatesSplitted = iCoordinates.split(",");

        Log.d("meetAsapError",
                "MeetAsapPreNavigationB - String splitted! parsing to double...");

        iLatitude = Double.parseDouble(iCoordinatesSplitted[0]);
        iLongitude = Double.parseDouble(iCoordinatesSplitted[1]);
        iPosition = new LatLng(iLatitude, iLongitude);
    }
    private void updateMyCoordinates() {
        try {
            if (GPS.canGetLocation()) {
                mLatitude = GPS.getLatitude();
                mLongitude = GPS.getLongitude();
                myPosition = new LatLng(mLatitude, mLongitude);

                mCoordinates = (String.valueOf(mLatitude) + ", " + String
                        .valueOf(mLongitude));
                Toast.makeText(getBaseContext(),
                        "Your coordinates updated", Toast.LENGTH_SHORT)
                        .show();
            }
        } catch (Exception e) {
            Utils.showMessageAndExit(this,
                    getString(R.string.label_api_failed), mExitOnce, e);
        }
    }

    private OnClickListener refreshButtonListener = new OnClickListener() {
        public void onClick(View v) {
            updateMyCoordinates();
            updateMap();
            myCoordinates.setText("My coordinates: " + mCoordinates);
        }
    };
    private OnClickListener sendButtonListener = new OnClickListener() {
        public void onClick(View v) {
            Toast.makeText(getBaseContext(),
                    "You have nothing to send! Firstly update!",
                    Toast.LENGTH_SHORT).show();

        }
    };
}
