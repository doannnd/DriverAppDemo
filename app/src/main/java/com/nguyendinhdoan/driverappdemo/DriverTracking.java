package com.nguyendinhdoan.driverappdemo;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.animation.LinearInterpolator;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.SquareCap;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.maps.android.PolyUtil;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.nguyendinhdoan.driverappdemo.common.Common;
import com.nguyendinhdoan.driverappdemo.remote.IGoogleAPI;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DriverTracking extends FragmentActivity implements OnMapReadyCallback {

    private static final String TAG = "DriverTracking";

    private GoogleMap mMap;

    double riderLat, riderLng;

    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;

    private Circle riderMarker;
    private Marker driverMarker;

    private Polyline direction;
    private List<LatLng> polyLineList = new ArrayList<>();

    private IGoogleAPI mServices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_tracking);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        fusedLocationProviderClient = new FusedLocationProviderClient(this);
        mServices = Common.getGoogleAPI();

        startLocationUpdates();

        if (getIntent() != null) {
            riderLat = getIntent().getDoubleExtra("lat", -1);
            riderLng = getIntent().getDoubleExtra("lng", -1);
        }

    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

       /* riderMarker = mMap.addCircle(new CircleOptions()
        .center(new LatLng(riderLat, riderLng))
        .radius(10)
        .strokeColor(Color.BLUE)
                .fillColor(0x220000FF)
                .strokeWidth(5.0f));*/
    }

    private void buildLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setSmallestDisplacement(10.0f);
    }

    private void buildLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                Common.mLastLocation = locationResult.getLastLocation();
                Log.d(TAG, "latitude: " + Common.mLastLocation.getLatitude());
                Log.d(TAG, "longitude: " + Common.mLastLocation.getLongitude());
                displayLocation();
            }
        };
    }

    private void startLocationUpdates() {
        Dexter.withActivity(this)
                .withPermissions(Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()) {
                            buildLocationCallback();
                            buildLocationRequest();

                            if (ActivityCompat.checkSelfPermission(DriverTracking.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                                    && ActivityCompat.checkSelfPermission(DriverTracking.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                return;
                            }
                            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
                        }

                        if (report.isAnyPermissionPermanentlyDenied()) {
                            // TODO:
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                })
                .onSameThread()
                .check();
    }

    private void stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    private void displayLocation() {
        if (Common.mLastLocation != null) {
            if (driverMarker != null) {
                driverMarker.remove();
            }
            driverMarker = mMap.addMarker(
                    new MarkerOptions().position(new LatLng(
                            Common.mLastLocation.getLatitude(), Common.mLastLocation.getLongitude()
                    )).title("you").icon(BitmapDescriptorFactory.defaultMarker())
            );

            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(
                    Common.mLastLocation.getLatitude(), Common.mLastLocation.getLongitude()
            ), 15.0f));

            if (direction != null) {
                direction.remove();
            }

            getDirection();
        }
    }

    private void getDirection() {
        LatLng currentPosition = new LatLng(Common.mLastLocation.getLatitude(), Common.mLastLocation.getLongitude());

        String requestApi = null;
        try {
            requestApi = "https://maps.googleapis.com/maps/api/directions/json?" +
                    "mode=drivings&" +
                    "transit_routing_preference=less_driving&"+
                    "origin=" + currentPosition.latitude+","+currentPosition.longitude+"&"+
                    "destination=" + riderLat + "," + riderLng + "&" +
                    "key=" + getString(R.string.google_api_key);

            Log.d(TAG, "URL direction: " + requestApi);

            mServices.getPath(requestApi)
                    .enqueue(new Callback<String>() {
                        @Override
                        public void onResponse(Call<String> call, Response<String> response) {
                            try {

                                JSONObject jsonObject = new JSONObject(response.body());
                                JSONArray jsonArray = jsonObject.getJSONArray("routes");
                                for (int i = 0; i < jsonArray.length(); i++) {
                                    JSONObject route = jsonArray.getJSONObject(i);
                                    JSONObject poly = route.getJSONObject("overview_polyline");
                                    String polyline = poly.getString("points");
                                    polyLineList = PolyUtil.decode(polyline);
                                    Log.d(TAG, "polyline list size" + polyLineList.size());
                                }

                                // adjusting bound
                                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                                for (LatLng latLng : polyLineList) {
                                    builder.include(latLng);
                                }

                                LatLngBounds bounds = builder.build();
                                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 150);
                                mMap.moveCamera(cameraUpdate);

                                PolylineOptions polylineOptions = new PolylineOptions();
                                polylineOptions.color(Color.GRAY);
                                polylineOptions.width(5);
                                polylineOptions.startCap(new SquareCap());
                                polylineOptions.endCap(new SquareCap());
                                polylineOptions.jointType(JointType.ROUND);
                                polylineOptions.addAll(polyLineList);
                                direction = mMap.addPolyline(polylineOptions);

                                // add marker
                                // marker
                                mMap.addMarker(new MarkerOptions()
                                        .position(polyLineList.get(polyLineList.size() - 1))
                                        .title("customer")
                                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                                );

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(Call<String> call, Throwable t) {
                            Toast.makeText(DriverTracking.this, ""+t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        stopLocationUpdates();
    }
}
