package com.orangelabs.rcs.ri.meetasapd;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.view.View.OnClickListener;

import com.orangelabs.rcs.ri.R;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by Bilu on 2015-09-22.
 */
public class ServerPlacePicker extends Activity {
    TextView tv1, tv2, tv3;
    Button btn1;
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


    String mylatitude = "48.7366184";
    String mylongitude = "-3.464593";
    Location myLocation = new Location ("myLocation");
    String interlatitude = "48.7866184";
    String interlongitude = "-3.414593";
    Location interLocation = new Location ("interLocation");
    String meetingNature = "eating";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.meet_place_picking);
        myLocation.setLatitude(Double.parseDouble(mylatitude));
        myLocation.setLongitude(Double.parseDouble(mylongitude));

        interLocation.setLatitude(Double.parseDouble(interlatitude));
        interLocation.setLongitude(Double.parseDouble(interlongitude));

        getPlaceName();
    }

    private View.OnClickListener updateInfo = new OnClickListener() {
        @Override
        public void onClick(View v) {
            getPlaceName();
        }
    };

    public void getPlaceName() {
        new GetPlaces().execute(mylatitude, mylongitude, interlatitude, interlongitude, meetingNature);
    }

    class GetPlaces extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... params) {
            // Get the latitude address
            String mlatit = params[0];
            String mlongit = params[1];
            String ilatit = params[2];
            String ilongit = params[3];
            String mnature = params[4];
            getResults(mlatit, mlongit, ilatit, ilongit, mnature);
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            distanceMeasuring();
            newIntentStarter();
        }
    }
    public void distanceMeasuring(){
        for(int i=0; i<=placesLocations.size()-1; i++) {
            if ((myLocation.distanceTo(placesLocations.get(i)))> 500){
                Mdistances.add(String.valueOf((myLocation.distanceTo(placesLocations.get(i)))/1000).substring(0,4) + " km");
            }else{
                Mdistances.add(String.valueOf(myLocation.distanceTo(placesLocations.get(i))) + " m");
            }
            if ((interLocation.distanceTo(placesLocations.get(i)))> 500){
                Idistances.add(String.valueOf((interLocation.distanceTo(placesLocations.get(i)))/1000).substring(0,4) + " km");
            }else{
                Idistances.add(String.valueOf(interLocation.distanceTo(placesLocations.get(i))) + " m");
            }


        }
    }

    public void newIntentStarter (){

        Intent newint = new Intent(ServerPlacePicker.this, RecommendationsList.class);

        newint.putStringArrayListExtra("placesNames", (ArrayList<String>) placesNames);
        newint.putStringArrayListExtra("placesVicinities", (ArrayList<String>) placesVicinities);
        newint.putStringArrayListExtra("placesRatings", (ArrayList<String>) placesRatings);
        newint.putStringArrayListExtra("Mdistances", (ArrayList<String>) Mdistances);
        newint.putStringArrayListExtra("Idistances", (ArrayList<String>) Idistances);
        newint.putStringArrayListExtra("placesRefs", (ArrayList<String>) placesRefs);
        newint.putStringArrayListExtra("placesIcons", (ArrayList<String>) placesIcons);

        startActivity(newint);
    }

    protected void getResults(String mlatit, String mlongit, String ilatit, String ilongit, String mNature) {
        Log.d("ServerPlacePicker", "entering getPlaces");
        // Define the uri that is used to get lat and long for our address

        String uri = stringComposer(mlatit,  mlongit, ilatit, ilongit, mNature);
        Log.d("meetAsapError", "ServerPlacePicker - uri set... http getting...");

        HttpGet httpMyGet = new HttpGet(uri);
        Log.d("meetAsapError", "ServerPlacePicker - uri set... http client...");
        HttpClient client = new DefaultHttpClient();

        HttpResponse response;

        StringBuilder stringBuilder = new StringBuilder();

        try {
            Log.d("meetAsapError", "ServerPlacePicker - try response...");
            response = client.execute(httpMyGet);
            Log.d("meetAsapError", "ServerPlacePicker - got response.... try entity");
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

            for(int l=0; l<=places.length()-1; l++){
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
                try{
                placesRatings.add("rating: " + String.valueOf(places.getJSONObject(l).getDouble("rating")));
                } catch (JSONException e){
                    placesRatings.add("");
                }

            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public String stringComposer(String mlatit, String  mlongit, String ilatit, String ilongit, String meetingNature){
        double mla = Double.parseDouble(mlatit);
        double mlo = Double.parseDouble(mlongit);
        double ila = Double.parseDouble(ilatit);
        double ilo = Double.parseDouble(ilongit);

        double lat = (mla + ila)/2;
        double lon = (mlo + ilo)/2;
        String middleLat = String.valueOf(lat);
        String middleLon = String.valueOf(lon);

        String radius = radiusCalculating(mla, mlo, ila, ilo);
        Log.d("meetAsapError", "ServerPlacePicker - middlelat: " + middleLat + ", " + middleLon + ", radius: " + radius);
        String types = typesDefining(meetingNature);

        //with radius
        String uri = "http://51.254.128.180/pbbilu/www/showplaces.php?myurl=https%3A%2F%2Fmaps.googleapis.com%2Fmaps%2Fapi%2Fplace%2Fnearbysearch%2Fjson%3Flocation%3D" + middleLat + "%2C" + middleLon + "%26radius%3D" + radius + "%26sensor%3Dtrue%26types%3D" + types + "%26key%3DAIzaSyCW-ixu6yPyaZQwTkLMYwMKcYXKAHjKrdA";

        //with rankby
        //String uri = "http://51.254.128.180/pbbilu/www/showplaces.php?myurl=https%3A%2F%2Fmaps.googleapis.com%2Fmaps%2Fapi%2Fplace%2Fnearbysearch%2Fjson%3Flocation%3D" + middleLat + "%2C" + middleLon + "%26rankby%3Ddistance%26sensor%3Dtrue%26types%3D" + types + "%26key%3DAIzaSyCW-ixu6yPyaZQwTkLMYwMKcYXKAHjKrdA";

        Log.d("meetAsapError", "ServerPlacePicker - uri: " + uri);
        return uri;
    }

    public String radiusCalculating(double latA, double lngA, double latB, double lngB){
        Location locationA = new Location("point A");

        locationA.setLatitude(latA);
        locationA.setLongitude(lngA);
        Log.d("meetAsapError", "ServerPlacePicker - locA: " + locationA.toString());
        Location locationB = new Location("point B");

        locationB.setLatitude(latB);
        locationB.setLongitude(lngB);
        Log.d("meetAsapError", "ServerPlacePicker - locB: " + locationB.toString());
        float distance = locationA.distanceTo(locationB);
        if (distance > 5000)
            distance = (distance/(5));
        else
            distance = 500;
        Log.d("meetAsapError", "ServerPlacePicker - dist: " + String.valueOf(distance));
        return String.valueOf(distance);
    }

    public String typesDefining(String meetingNature){
        String types= "";
        if (meetingNature.equals("eating")){
            types = "food";
        } if(meetingNature.equals("drinking")){
            types = "bar";
        } if(meetingNature.equals("talking")) {
            types = "park";
        } if(meetingNature.equals("pickingup")) {
            types = "parking";
        }if(meetingNature.equals("shopping")) {
            types = "store";
        }
        return types;
    }

}

