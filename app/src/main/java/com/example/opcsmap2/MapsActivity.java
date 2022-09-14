package com.example.opcsmap2;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.Navigation;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.opcsmap2.databinding.ActivityMapsBinding;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.maps.android.PolyUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static GoogleMap mMap;
    private ActivityMapsBinding binding;

    Location currentLocation;
    FusedLocationProviderClient fusedLocationProviderClient;
    private static final int REQUEST_CODE = 101;
    public static String TAG = "MapAPp";

    private static int AUTOCOMPLETE_REQUEST_CODE = 1;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

//        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
//        mapFragment.getMapAsync(this);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        getCurrentLocation();
        Places.initialize(getApplicationContext(), "AIzaSyAnBavLX9QyeDE6vkwRXAZsyQhvUMt96AA");
        PlacesClient placesClient = Places.createClient(this);


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_search);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startAuto();
            }



        });


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.

    }

    public void startAuto(){


        // Set the fields to specify which types of place data to
        // return after the user has made a selection.
        List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME);

        // Start the autocomplete intent.
        Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
            .build(this);
        startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE);

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;


        LatLng curLoc = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
        mMap.addMarker(new MarkerOptions().position(curLoc).title("Current Location"));
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(curLoc));
        requestLocationUpdatesFromAndroid();
    }

    //vid

    private  void direction(String destination, String origin){
        mMap.clear();
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        String url = Uri.parse("https://maps.googleapis.com/maps/api/directions/json")
            .buildUpon()
            .appendQueryParameter("destination", destination)
            .appendQueryParameter("origin", origin)
            .appendQueryParameter("mode", "driving")
            .appendQueryParameter("key", "AIzaSyAnBavLX9QyeDE6vkwRXAZsyQhvUMt96AA")
            .toString();
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                ArrayList<LatLng> points = new ArrayList<>();
                try {
                    String status = response.getString("status");
                    if (status.equals("OK")){
                        JSONArray routes = response.getJSONArray("routes");

                        PolylineOptions polylineOptions = null;

                        for (int i = 0; i < routes.length(); i++){
                            points = new ArrayList<>();
                            polylineOptions = new PolylineOptions();
                            JSONArray legs = routes.getJSONObject(i).getJSONArray("legs");

                            for (int j = 0; j < legs.length(); j++){
                                JSONArray steps = legs.getJSONObject(j).getJSONArray("steps");

                                for (int k = 0; k < steps.length(); k ++) {
                                    String polyLine = steps.getJSONObject(k).getJSONObject("polyline").getString("points");
                                    List<LatLng> list = PolyUtil.decode(polyLine);


                                    for (int l = 0; l < list.size(); l++){
                                        LatLng position = new LatLng((list.get(l)).latitude, (list.get(l)).longitude);
                                        points.add(position);

                                    }

                                }

                            }

                            polylineOptions.addAll(points);
                            polylineOptions.width(10);
                            polylineOptions.color(ContextCompat.getColor(MapsActivity.this, R.color.purple_500));
                            polylineOptions.geodesic(true);

                        }
                        mMap.addPolyline(polylineOptions);
                        mMap.addMarker(new MarkerOptions().position(points.get(0)).title("Marker 1"));
                        mMap.addMarker(new MarkerOptions().position(points.get(points.size() - 1)).title("Marker 2"));

                        LatLngBounds bounds = new LatLngBounds.Builder()
                            .include(points.get(0))
                            .include(points.get(points.size() - 1))
                            .build();
                        Point point = new Point();
                        getWindowManager().getDefaultDisplay().getSize(point);
                        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, point.x, 150, 30));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });

        RetryPolicy retryPolicy = new DefaultRetryPolicy(3000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        jsonObjectRequest.setRetryPolicy(retryPolicy);
        requestQueue.add(jsonObjectRequest);

    }
//    private List<LatLng> decodePoly(String encoded){
//    List<LatLng> poly = new ArrayList<>();
//    int index = 0, len = encoded.length();
//    int lat = 0, lng = 0;
//
//    while (index < len) {
//        int b, shift = 0, result = 0;
//        do {
//            b = encoded.charAt(index++) - 63;
//            result |= (b & 0x1f) << shift;
//            shift += 5;
//        }while  (b >= 0x20);
//        int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
//        lat += dlat;
//
//        shift = 0;
//        result = 0;
//        do {
//            b = encoded.charAt(index++) - 63;
//            result |= (b & 0x1f) << shift;
//            shift += 5;
//        }while(b >  0x20);
//        int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
//        lng += dlng;
//
//        LatLng p = new LatLng((((double) lat / 1E5)),
//            (((double) lng / 1E5)));
//        poly.add(p);
//
//    }
//
//    return poly;
//    }

    //vid

    private void getCurrentLocation() {

        if (
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);
            return;

        }
        Task<Location> task = fusedLocationProviderClient.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    currentLocation = location;
                    Toast.makeText(getApplicationContext(), currentLocation.getLatitude() + "", Toast.LENGTH_LONG).show();
                    SupportMapFragment supportMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_fragment);

                    assert supportMapFragment != null;
                    supportMapFragment.getMapAsync(MapsActivity.this);

//                    AutocompleteSupportFragment autocompleteSupportFragment = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.map_auto);
//                    autocompleteSupportFragment.setTypeFilter(TypeFilter.ESTABLISHMENT);
//
//                    autocompleteSupportFragment.setLocationBias(
//                        RectangularBounds.newInstance(
//                            new LatLng(26.2005208,26.9134699),
//                            new LatLng(-26.2005208,26.9134699)
//                        )
//                    );
//                    autocompleteSupportFragment.setCountries("ZA");
//
//                    autocompleteSupportFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME));
//
//                    autocompleteSupportFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
//                        @Override
//                        public void onError(@NonNull Status status) {
//                            Log.i(TAG, "An Error occurred " + status);
//                        }
//
//                        @Override
//                        public void onPlaceSelected(@NonNull Place place) {
//                            Log.i(TAG, "PLace: " + place.getName() + " " + place.getId());
//                        }
//                    });
                }



            }
        });

    }

    public void requestLocationUpdatesFromAndroid() {
        Log.i("Service", "");
        long UPDATE_INTERVAL = 10 * 1000;
        long FASTEST_INTERVAL = 5 * 1000;

        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        LocationSettingsRequest locationSettingsRequest = builder.build();
//        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
//        settingsClient.checkLocationSettings(locationSettingsRequest);
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
        LocationServices.getFusedLocationProviderClient(this)
            .requestLocationUpdates(
                mLocationRequest,
                periodicLocationCallback,
                Looper.myLooper()
            );
    }

    private static LocationCallback periodicLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {

            LatLng curLoc = new LatLng(locationResult.getLastLocation().getLatitude(), locationResult.getLastLocation().getLongitude());
            mMap.addMarker(new MarkerOptions().position(curLoc).title("Current Location"));
            //mMap.moveCamera(CameraUpdateFactory.newLatLng(curLoc));
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        requestLocationUpdatesFromAndroid();
        switch (REQUEST_CODE) {
            case REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getCurrentLocation();
                }
                break;
        }
    }



    //nav
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Place place = Autocomplete.getPlaceFromIntent(data);
                Log.i(TAG, "Place: " + place.getName() + ", " + place.getId());
                Geocoder geo = new Geocoder(this);
                List<Address> addr = new ArrayList<>() ;
                try {
                    addr = geo.getFromLocationName(place.getName(), 1);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Log.i(TAG, "onActivityResult: " + addr.toString());
                String destination = addr.get(0).getLatitude() + ", " + addr.get(0).getLongitude();
                String origin = currentLocation.getLatitude()+", " + currentLocation.getLongitude();
                direction(destination, origin);
            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                // TODO: Handle the error.
                Status status = Autocomplete.getStatusFromIntent(data);
                Log.i(TAG, status.getStatusMessage());
            } else if (resultCode == RESULT_CANCELED) {
                // The user canceled the operation.
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

}