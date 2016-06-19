package com.orangelabs.rcs.ri.meetasapd;

/**
 * Created by Bilu on 2015-09-25.
 */

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import com.orangelabs.rcs.ri.R;

import org.w3c.dom.Text;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;

class CustomAdapter extends ArrayAdapter<String> {

    ArrayList<String> placesNames;
    ArrayList<String> placesVicinities;
    ArrayList<String> placesRatings;
    ArrayList<String> mdistances;
    ArrayList<String> idistances;
    ArrayList<String> placesIds;
    ArrayList<String> placesIcons;
    String mMode, iMode;

    CustomAdapter(Context context,
                  ArrayList<String> pNames,
                  ArrayList<String> pRatings,
                  ArrayList<String> mDist,
                  ArrayList<String> iDist,
                  ArrayList<String> pIcons,
                  String myMode,
                  String intMode) {
        super(context, R.layout.meet_custom_row, pNames);

        placesNames = pNames;
        placesRatings = pRatings;
        mdistances = mDist;
        idistances = iDist;
        placesIcons = pIcons;
        mMode = myMode;
        iMode = intMode;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater myInflater = LayoutInflater.from(getContext());
        View customView = myInflater.inflate(R.layout.meet_custom_row, parent, false);

        String placeName = placesNames.get(position);
        String placeRating = placesRatings.get(position);
        String mDistance = mdistances.get(position);
        String iDistance = idistances.get(position);
        String placeIcon = placesIcons.get(position);
        TextView nameText = (TextView) customView.findViewById(R.id.tv_name);
        TextView ratingText = (TextView) customView.findViewById(R.id.tv_rating);
        TextView mdistText = (TextView) customView.findViewById(R.id.tv_my_dist);
        TextView idistText = (TextView) customView.findViewById(R.id.tv_int_dist);
        ImageView iconImg = (ImageView) customView.findViewById(R.id.img_icon);
        ImageView mModeIcon = (ImageView) customView.findViewById(R.id.my_mode_tv);
        ImageView iModeIcon = (ImageView) customView.findViewById(R.id.int_mode_tv);

        nameText.setText(placeName);
        if (placeRating.length() == 0) {
            ratingText.setVisibility(View.GONE);
        } else {
            ratingText.setText(placeRating);
        }
        mdistText.setText(mDistance);
        idistText.setText(iDistance);
        Log.d("placeIcon", position + ", " + placeIcon);
        if (placeIcon.contains("bar")) {
            Log.d("bar", position + ", " + placeIcon);
            iconImg.setImageResource(R.drawable.bar);
        } else if (placeIcon.contains("restaurant")) {
            iconImg.setImageResource(R.drawable.restaurant);
        } else if (placeIcon.contains("car_dealer")) {
            iconImg.setImageResource(R.drawable.car_dealer);
        } else if (placeIcon.contains("shopping")) {
            iconImg.setImageResource(R.drawable.shopping);
        } else if (placeIcon.contains("restaurant")) {
            Log.d("restaurant", position + ", " + placeIcon);
            iconImg.setImageResource(R.drawable.restaurant);
        } else if (placeIcon.contains("generic_business")) {
            iconImg.setImageResource(R.drawable.generic_business);
        } else if (placeIcon.contains("wine")) {
            iconImg.setImageResource(R.drawable.wine);
        } else if (placeIcon.contains("aquarium")) {
            iconImg.setImageResource(R.drawable.aquarium);
        } else {
            iconImg.setImageResource(R.drawable.spot);
        }

        if (mMode.contains("walk")) {
            mModeIcon.setImageResource(R.drawable.me_walk);
        } else  if (mMode.contains("bike")) {
            mModeIcon.setImageResource(R.drawable.me_bike);
        }else  if (mMode.contains("car")) {
            mModeIcon.setImageResource(R.drawable.me_car);
        }else  if (mMode.contains("public")) {
            mModeIcon.setImageResource(R.drawable.me_public);
        }

        if (iMode.contains("walk")) {
            iModeIcon.setImageResource(R.drawable.int_walk);
        } else  if (iMode.contains("bike")) {
            iModeIcon.setImageResource(R.drawable.int_bike);
        }else  if (iMode.contains("car")) {
            iModeIcon.setImageResource(R.drawable.int_car);
        }else  if (iMode.contains("public")) {
            iModeIcon.setImageResource(R.drawable.int_public);
        }

        return customView;
    }
}
