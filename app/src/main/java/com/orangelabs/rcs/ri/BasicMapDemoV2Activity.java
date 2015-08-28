/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.orangelabs.rcs.ri;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.gsma.services.rcs.contact.RcsContact;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.R.id;
import com.orangelabs.rcs.ri.R.layout;

/**
 * This shows how to create a simple activity with a map and a marker on the
 * map.
 */
public class BasicMapDemoV2Activity extends FragmentActivity implements
		OnMapReadyCallback {
	TextView myTextView;
	LatLng fromPosition;
	LatLng toPosition;
	double myLat;
	double myLong;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.geoloc_display_v2);

		SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
				.findFragmentById(R.id.map);
		mapFragment.getMapAsync(this);

		Bundle bundle = getIntent().getParcelableExtra("bundle");
		fromPosition = bundle.getParcelable("from_position");
		toPosition = bundle.getParcelable("to_position");

		Button meetButton = (Button) findViewById(R.id.meet_button);
		meetButton.setOnClickListener(meetButtonListener);
		myTextView = (TextView) findViewById(R.id.meet_textview);
	}

	@Override
	public void onMapReady(GoogleMap map) {
		// Instantiates a new CircleOptions object and defines the center and
		// radius
		CircleOptions circleOptions = new CircleOptions()
				.center(new LatLng(48.7565942, -3.4526495)).radius(100)
				.fillColor(0x5531aa07).strokeColor(0x7531aa07);
		Circle circle = map.addCircle(circleOptions);

		CircleOptions circleOptions2 = new CircleOptions()
				.center(new LatLng(48.7565942, -3.4526495)).radius(50)
				.fillColor(0xff31aa07).strokeColor(0xffffff);
		Circle circle2 = map.addCircle(circleOptions2);

		map.addMarker(new MarkerOptions().position(fromPosition).title("fromPosition"));
		map.addMarker(new MarkerOptions().position(toPosition).title("toPosition"));
		map.setMyLocationEnabled(true);
 
        myLat = fromPosition.latitude;
        myLong = fromPosition.longitude;
        // Get longitude of the current location 
        
        //myLong = myLocation.getLongitude(); 
 
        // Create a LatLng object for the current location 
        // LatLng myLoc = new LatLng(myLat, myLong); 


		CameraUpdate center = CameraUpdateFactory.newLatLng(fromPosition);
		CameraUpdate zoom = CameraUpdateFactory.zoomTo(15);

		map.moveCamera(center);
		map.animateCamera(zoom);

	}

	private OnClickListener meetButtonListener = new OnClickListener() {
		public void onClick(View v) {
			myTextView.setText("clicked");

		}
	};

}
