package com.example.trash.mapviewproject;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private MapView mMapView;
    private GoogleMap mGoogleMap;
    private GoogleApiClient mGoogleApiClient;
    private Location mLocation;
    private Marker mLocationMarker, placeMarker;
    private LocationRequest mLocationRequest;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;

    private double mLat, mLng;
    private double placeLat, placeLng;

    private boolean firstRun = true;

    private List<HashMap<String, String>> placeData;
    private List<Marker> markerList;

    private static final String MAP_VIEW_BUNDLE_KEY = "AIzaSyCnC0PVJ-dshihBvgv8IM5XgVxME_ZoNKk";

    private static int MY_PERMISSIONS_REQUEST_ACCESS_LOCATION = 4;
    private static int RADIUS = 300;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermissions();

        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY);
        }

        mMapView = findViewById(R.id.map_view);
        mMapView.onCreate(mapViewBundle);
        mMapView.getMapAsync(this);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        markerList = new ArrayList<Marker>();

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                for (final Location location : locationResult.getLocations()) {
                    mLocation = location;

                    if (mLocationMarker != null) {
                        mLocationMarker.remove();
                    }

                    mLat = location.getLatitude();
                    mLng = location.getLongitude();

                    mLocation.setLatitude(mLat);
                    mLocation.setLongitude(mLng);

                    //Place current location marker
                    LatLng latLng = new LatLng(mLat, mLng);

                    Log.v("MY LOCATION", "MY LOCATION: " + mLocation);

//                    Circle circle = mGoogleMap.addCircle(new CircleOptions()
//                            .center(latLng)
//                            .radius(RADIUS).strokeColor(Color.argb(50, 255, 0, 0))
//                            .fillColor(Color.argb(100, 255, 0, 0)));

//        MarkerOptions markerOptions = new MarkerOptions();
//        markerOptions.position(latLng);
//        markerOptions.title("Position");
//        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
//        mLocationMarker = mMap.addMarker(markerOptions);

                    //Move map camera
                    mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                    mGoogleMap.animateCamera(CameraUpdateFactory.zoomTo(16));

                    //Query places with current location
                    StringBuilder stringBuilderValue = new StringBuilder(stringBuilderMethod(location));
                    Log.v("LocationCallBack", "LocationCallBack: " + mLocation.getLatitude() + " " + mLocation.getLongitude());
                    PlacesTask placesTask = new PlacesTask();
                    placesTask.execute(stringBuilderValue.toString());

                    if (mFusedLocationClient != null) {
                        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
                    }

                }
            }
        };
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);

        Bundle mapViewBundle = outState.getBundle(MAP_VIEW_BUNDLE_KEY);
        if (mapViewBundle == null) {
            mapViewBundle = new Bundle();
            outState.putBundle(MAP_VIEW_BUNDLE_KEY, mapViewBundle);
        }
        mMapView.onSaveInstanceState(mapViewBundle);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
        if (mGoogleApiClient != null && mFusedLocationClient != null) {
            requestLocationUpdates();
        } else {
            buildGoogleApiClient();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mMapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mMapView.onStop();
    }

    @Override
    protected void onPause() {
        if (mFusedLocationClient != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
        mMapView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mMapView.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        mGoogleMap.setIndoorEnabled(true);
        UiSettings uiSettings = mGoogleMap.getUiSettings();
        uiSettings.setIndoorLevelPickerEnabled(true);
        uiSettings.setMyLocationButtonEnabled(true);
        uiSettings.setMapToolbarEnabled(true);
        uiSettings.setCompassEnabled(true);
        uiSettings.setZoomControlsEnabled(true);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mGoogleMap.setMyLocationEnabled(true);

        buildGoogleApiClient();
    }

    public StringBuilder stringBuilderMethod(Location mLocation) {
        //current location
        double mLatitude = mLocation.getLatitude();
        double mLongitude = mLocation.getLongitude();

        StringBuilder stringBuilder = new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
        stringBuilder.append("location=").append(mLatitude).append(",").append(mLongitude);
        stringBuilder.append("&keyword=Пятерочка | пятерочка | ПЯТЕРОЧКА | Магнит | магнит | МАГНИТ");
        stringBuilder.append("&language=ru");
        stringBuilder.append("&radius=").append(RADIUS);
        stringBuilder.append("&sensor=true");

        stringBuilder.append("&key=AIzaSyCNa66fP1Lv_Hrk8r0Af7WJum6W7n3wCDA");

        Log.d("Map", "<><>api: " + stringBuilder.toString());

        return stringBuilder;
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        requestLocationUpdates();
    }

    public void requestLocationUpdates() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(final Location location) {

    }

    private class PlacesTask extends AsyncTask<String, Integer, String> {

        String data = null;

        // Invoked by execute() method of this object
        @Override
        protected String doInBackground(String... url) {
            try {
                data = downloadUrl(url[0]);
            } catch (IOException e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        // Executed after the complete execution of doInBackground() method
        @Override
        protected void onPostExecute(final String result) {
            Log.d("results", "<><> result: " + result);
            ParserTask parserTask = new ParserTask();

            // Start parsing the Google places in JSON format
            // Invokes the "doInBackground()" method of the class ParserTask
            parserTask.execute(result);
        }
    }

    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream inputStream = null;
        HttpURLConnection urlConnection = null;

        try {
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            inputStream = urlConnection.getInputStream();

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            StringBuilder stringBuilder = new StringBuilder();

            String line = "";
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }

            data = stringBuilder.toString();

            bufferedReader.close();
        } catch (Exception e) {
            Log.d("Exception", e.toString());
        } finally {
            inputStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    private class ParserTask extends AsyncTask<String, Integer, List<HashMap<String, String>>> {

        JSONObject jsonObject;

        // Invoked by execute() method of this object
        @Override
        protected List<HashMap<String, String>> doInBackground(String... jsonData) {

            List<HashMap<String, String>> places = null;
            PlaceJSON placeJSON = new PlaceJSON();

            try {
                jsonObject = new JSONObject(jsonData[0]);

                places = placeJSON.parse(jsonObject);
            } catch (JSONException e) {
                Log.d("Exception", e.toString());
            }
            return places;
        }

        @Override
        protected void onPostExecute(List<HashMap<String, String>> list) {
            Log.d("Map", "list size: " + list.size());
            // Clears all the existing markers;
            if (!firstRun) {
                mGoogleMap.clear();
            }
            firstRun = false;

            placeData = list;

            onPause();
            onResume();
            markerUpdates();
        }
    }

    void markerUpdates() {
        markerList.clear();
        mGoogleMap.clear();
        mMapView.requestLayout();
        //Creating a market
        MarkerOptions markerOptions = new MarkerOptions();
        Location placeLocation = new Location("point B");
        for (int i = 0; i < placeData.size(); i++) {

            Log.v("MY LOCATION RES", "MY LOCATION: " + mLocation);

            //Getting a place from the Places list
            HashMap<String, String> hashMapPlace = placeData.get(i);

            //Getting Latitude of the place
            placeLat = Double.parseDouble(hashMapPlace.get("lat"));

            //Getting longitude of the place
            placeLng = Double.parseDouble(hashMapPlace.get("lng"));

            //Getting name
            String name = hashMapPlace.get("place_name");

            Log.d("Map", "place name: " + name);

            //Getting vicinity
            String vicinity = hashMapPlace.get("vicinity");

            LatLng latLng = new LatLng(placeLat, placeLng);


            //Setting the position for the market
            markerOptions.position(latLng);

            placeLocation.setLatitude(latLng.latitude);
            placeLocation.setLongitude(latLng.longitude);

            double distance = mLocation.distanceTo(placeLocation);

//            BigDecimal bd = new BigDecimal(distance).setScale(1, RoundingMode.HALF_EVEN);
//            distance = bd.doubleValue();

            if (distance < RADIUS) {
                markerOptions.title(name + " : " + distance + " метров");
                Log.d("Distance", "Distance: " + distance);
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
                //Placed a marker on the touched position
                placeMarker = mGoogleMap.addMarker(markerOptions);
                markerList.add(placeMarker);
                Log.d("Markers", "Markers: " + markerList.size());
            }
        }
    }

    public class PlaceJSON {
        /*
          Receives a JSONObject and returns a list
         */
        public List<HashMap<String, String>> parse(JSONObject jsonObject) {
            JSONArray jsonArrayPlaces = null;
            try {
                jsonArrayPlaces = jsonObject.getJSONArray("results");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return getPlaces(jsonArrayPlaces);
        }

        private List<HashMap<String, String>> getPlaces(JSONArray jsonPlaces) {
            int placesCount = jsonPlaces.length();
            List<HashMap<String, String>> placesList = new ArrayList<HashMap<String, String>>();
            HashMap<String, String> place = null;

            /* Taking each place, parses and adds to list object */
            for (int i = 0; i < placesCount; i++) {
                try {
                    /* Call getPlace with place JSON object to parse the place */
                    place = getPlace((JSONObject) jsonPlaces.get(i));
                    placesList.add(place);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return placesList;
        }

        /*
         * Parsing the Place JSON object
         */
        private HashMap<String, String> getPlace(JSONObject jsonPlace) {
            HashMap<String, String> place = new HashMap<String, String>();
            String placeName = "-RU-";
            String vicinity = "-RU-";
            String latitude = "";
            String longitude = "";
            String reference = "";

            try {
                // Extracting Place name, if available
                if (!jsonPlace.isNull("name")) {
                    placeName = jsonPlace.getString("name");
                }

                // Extracting Place Vicinity, if available
                if (!jsonPlace.isNull("vicinity")) {
                    vicinity = jsonPlace.getString("vicinity");
                }

                latitude = jsonPlace.getJSONObject("geometry").getJSONObject("location").getString("lat");
                longitude = jsonPlace.getJSONObject("geometry").getJSONObject("location").getString("lng");
                reference = jsonPlace.getString("reference");

                place.put("place_name", placeName);
                place.put("vicinity", vicinity);
                place.put("lat", latitude);
                place.put("lng", longitude);
                place.put("reference", reference);

            } catch (JSONException e) {
                e.printStackTrace();
            }
            return place;
        }
    }

    void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                        MY_PERMISSIONS_REQUEST_ACCESS_LOCATION);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            Toast.makeText(this, "Все разрешения уже выданы", Toast.LENGTH_SHORT).show();
        }
    }
}
