package com.example.kohseukim.ambuside;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.api.Context;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.SetOptions;
import com.google.maps.android.ui.IconGenerator;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.android.gms.maps.model.JointType.DEFAULT;
import static com.google.android.gms.maps.model.JointType.ROUND;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final String TAG = "Main";
    private GoogleMap mMap;
    ArrayList<LatLng> MarkerPoints;
    Location mLastLocation;
    Marker mCurrLocationMarker;
    LocationRequest mLocationRequest;
    FusedLocationProviderClient mFusedLocationClient;
    boolean change = false;
    Polyline polylineFinal;
    FirebaseFirestore db;
    private FirebaseAuth mAuth;
    PolylineOptions lineOptions = null;
    private Button btn;
    private Button Signout;
    List<Polyline> allpolylines = new ArrayList<Polyline>();
    boolean same = true;
    List<Location> coord = new ArrayList<>();
    List<LatLng> destination = new ArrayList<>();
    boolean isFirstTime = true;


/// IMPORTANT !!
    // API key has been disabled. Please change the API key to your own in order to successfully run the app.
    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);


        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission();


        }
        // Initializing
        MarkerPoints = new ArrayList<>();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        //buildGoogleApiClient();

        //get instance of firebasefirestore
        db = FirebaseFirestore.getInstance();

        //get instance of user authentication
        mAuth = FirebaseAuth.getInstance();

        //update firebase
        //update "isActive" field as true for the specific signed in user.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        Map<String, Boolean> newAmbu = new HashMap<>();
        newAmbu.put("isActive", true);
        db.collection("ambulances").document(currentUser.getUid()).set(newAmbu,SetOptions.merge());


        //allow user to sign out by clicking the sign out button
        //return to Login activity once user signs out
        Signout = findViewById(R.id.SignOutButton);
        Signout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                Toast.makeText(MapsActivity.this, "Signed Out", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MapsActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */

    //callback method to constantly update the current location of the user
    LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {

            final List<Location> locationList = locationResult.getLocations();
            if (locationList.size() > 0) {
                //The last location in the list is the newest
                final Location location = locationList.get(locationList.size() - 1);

                //list of current locations from every callback
                coord.add(location);
                Log.i("MapsActivity", "Location: " + location.getLatitude() + " " + location.getLongitude());


                Log.i("Test", "test: " + coord.size());

                //current location
                mLastLocation = location;


                if (mCurrLocationMarker != null) {
                    mCurrLocationMarker.remove();
                }

                //user location is constantly updated on firebase as the ambulance moves
                FirebaseUser currentUser = mAuth.getCurrentUser();
                Map<String, Object> newL = new HashMap<>();
                newL.put("currentLocation", new GeoPoint(location.getLatitude(), location.getLongitude()));
                db.collection("ambulances").document(currentUser.getUid()).set(newL, SetOptions.merge());



                //Place current location marker of ambulance
                final LatLng here = new LatLng(coord.get(0).getLatitude(), coord.get(0).getLongitude());
                final LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

                //if user has clicked on a destination marker and the polyline has been drawn, centre the camera position to the ambulance current location
                if(allpolylines.size() > 0) {
                    CameraPosition cameraPosition = new CameraPosition.Builder().target(latLng).zoom(13).build();
                    mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                }
                else{

                    //position camera to current location when user first opens MapsActivity
                    if (mMap != null)
                        if (mMap != null)
                            if (isFirstTime) {
                                CameraUpdate center =
                                        CameraUpdateFactory.newLatLng(here);
                                CameraUpdate zoom = CameraUpdateFactory.zoomTo(13);
                                mMap.moveCamera(center);
                                mMap.animateCamera(zoom);
                                isFirstTime = false;
                            }

                }
                mMap.getUiSettings().setZoomControlsEnabled(true);


                //invisible button on MapsActivity UI
                btn = findViewById(R.id.InvisibleButton);


                //draw a new polyline every time the invisible button is clicked
                btn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                                    //origin is set to the current location of the ambulance
                                    LatLng origin = new LatLng(location.getLatitude(), location.getLongitude());
                                    try{
                                    LatLng dest = destination.get(0);
                                        String url = getUrl(origin, dest);
                                        Log.d("onMapClick", url.toString());
                                        FetchUrl FetchUrl = new FetchUrl();

                                        // Start downloading json data from Google Directions API
                                        FetchUrl.execute(url);



                                    }catch (IndexOutOfBoundsException e){

                                    }


                        }


                    });


                try{
                    float[] distance = new float[1];

                    //calculate distance between current location of two callbacks to check whether the ambulance is moving
                    Location.distanceBetween(coord.get(coord.size()-2).getLatitude(),coord.get(coord.size()-2).getLongitude(),coord.get(coord.size()-1).getLatitude(), coord.get(coord.size()-1).getLongitude(), distance);
                    Log.i("Dis", "dis" + distance[0]);

                    //if ambulance is moving, we constantly perform the button click
                    if(distance[0] > 2){

                        btn.performClick();

                    }
                }catch (ArrayIndexOutOfBoundsException e){

                }

            }
        }


    };

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        //location callback interval is set to 1 second
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000); // one second. number must be in milliseconds
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                //Location Permission already granted
                mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                mMap.setMyLocationEnabled(true);


            } else {
                //Request Location Permission
                checkLocationPermission();
            }
        }
        else {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
            mMap.setMyLocationEnabled(true);


        }

        //add markers at hospital locations.
        //markers of all hospitals in singapore to be added in future works

        IconGenerator iconGenerator = new IconGenerator(this);

        addIcon(iconGenerator, "Changi General Hospital", new LatLng(1.3405, 103.9496));

        addIcon(iconGenerator, "Parkway East Hospital", new LatLng(1.3150, 103.9088));


        iconGenerator.setBackground(getResources().getDrawable(R.drawable.hospitalmarker));


        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                //marker.showInfoWindow();

                if(change == false) {

                    LatLng origin = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                    LatLng dest = marker.getPosition();

                    // dest = position of chosen marker
                    if(destination.size() != 1){
                        destination.add(dest);
                    }
                    else
                        //clear previous destination and set new destination
                        destination.set(0, dest);



                    String url = getUrl(origin, dest);
                    Log.d("onMapClick", url.toString());
                    FetchUrl FetchUrl = new FetchUrl();

                    // Start downloading json data from Google Directions API
                    FetchUrl.execute(url);
                    change = true;
                    return true;

                }
                else {
                    //change destination when user clicks a different marker
                    change = true;
                    polylineFinal.remove();
                    LatLng origin = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                    LatLng dest = marker.getPosition();
                    if(destination.size() != 1){
                        destination.add(dest);
                    }
                    else
                        destination.set(0, dest);
                    String url = getUrl(origin, dest);
                    Log.d("onMapClick", url.toString());
                    FetchUrl FetchUrl = new FetchUrl();

                    // Start downloading json data from Google Directions API
                    FetchUrl.execute(url);


                    return true;
                }

            }
        });


    }

    private String getUrl(LatLng origin, LatLng dest) {

        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;

        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;


        // Sensor enabled
        String sensor = "sensor=false";

        String key = "key=AIzaSyDmN5MP07wzzwDBrBXeDbSlmWuNpB_l1lw";


        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + sensor + "&" + key;


        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;


        return url;
    }

    /**
     * A method to download json data from url
     */
    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();
            Log.d("downloadUrl", data.toString());
            br.close();

        } catch (Exception e) {
            Log.d("Exception", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    public GoogleMap getMap() {

        return mMap;
    }

    // Fetches data from url passed
    private class FetchUrl extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service
            String data = "";

            try {
                // Fetching the data from web service
                data = downloadUrl(url[0]);
                Log.d("Background Task data", data.toString());
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();

            // Invokes the thread for parsing the JSON data
            parserTask.execute(result);

        }
    }

    /**
     * A class to parse the Google Places in JSON format
     */
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                Log.d("ParserTask",jsonData[0].toString());
                DataParser parser = new DataParser();
                Log.d("ParserTask", parser.toString());

                // Starts parsing data
                routes = parser.parse(jObject);
                Log.d("ParserTask","Executing routes");
                Log.d("ParserTask",routes.toString());

            } catch (Exception e) {
                Log.d("ParserTask",e.toString());
                e.printStackTrace();
            }
            return routes;
        }

        // Executes in UI thread, after the parsing process
        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points;
            ArrayList<GeoPoint> list = new ArrayList<>();


            // Traversing through all the routes
            for (int i = 0; i < result.size(); i++) {
                points = new ArrayList<>();
                lineOptions = new PolylineOptions();

                // Fetching i-th route
                List<HashMap<String, String>> path = result.get(i);

                // Fetching all the points in i-th route
                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);
                    list.add(new GeoPoint(lat,lng));

                    //add all the coordinates to be drawn out to points
                    points.add(position);
                }

                 //Adding all the points in the route to LineOptions
                lineOptions.addAll(points);
                lineOptions.width(20);
                lineOptions.color(Color.BLUE);
                lineOptions.jointType(DEFAULT);


                //update the route to be taken by the ambulance to firebase
                FirebaseUser currentUser = mAuth.getCurrentUser();

                Map<String, Object> newL = new HashMap<>();
                newL.put("route", list);
                db.collection("ambulances").document(currentUser.getUid()).set(newL, SetOptions.merge());



                Log.d("onPostExecute","onPostExecute lineoptions decoded");

            }

            // Drawing polyline in the Google Map for the i-th route
            if(lineOptions != null) {

                //draw the new polyline before removing the previous one to prevent the line from blinking every time a new one is drawn
                polylineFinal = mMap.addPolyline(lineOptions);
                if(allpolylines.size() > 1 ){
                    for ( Polyline l: allpolylines){
                        l.remove();
                    }
                }

                //add the newest polyline
                allpolylines.add(polylineFinal);
                Log.i("POLY", "Size: " + allpolylines.size());
            }
            else {
                Log.d("onPostExecute","without Polylines drawn");
            }
        }
    }

    @Override
    public void onPause() {
        Log.i(TAG, "onPause");
        super.onPause();


        //stop location updates when Activity is no longer active
        if (mFusedLocationClient != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
    }



    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;  // i actually don't know what is this for
    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {


            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle("Location Permission Needed")
                        .setMessage("This app needs the Location permission, please accept to use location functionality")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(MapsActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION );
                            }
                        })
                        .create()
                        .show();


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION );
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ActivityCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                        mMap.setMyLocationEnabled(true);
                    }

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    //Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request. in this case, none
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
    }

    @Override
    protected void onDestroy() {
        FirebaseUser user = mAuth.getCurrentUser();
        Log.i(TAG, "onDestroy");
        super.onDestroy();
    }

    private void addIcon(IconGenerator iconFactory, String text, LatLng position) {
        MarkerOptions markerOptions = new MarkerOptions().
                icon(BitmapDescriptorFactory.fromBitmap(iconFactory.makeIcon(text))).
                position(position).
                anchor(iconFactory.getAnchorU(), iconFactory.getAnchorV());

        getMap().addMarker(markerOptions);
    }



    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
    }





}


