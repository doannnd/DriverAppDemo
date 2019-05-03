package com.nguyendinhdoan.driverappdemo;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.SquareCap;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.maps.android.PolyUtil;
import com.nguyendinhdoan.driverappdemo.common.Common;
import com.nguyendinhdoan.driverappdemo.model.Notification;
import com.nguyendinhdoan.driverappdemo.model.Result;
import com.nguyendinhdoan.driverappdemo.model.Sender;
import com.nguyendinhdoan.driverappdemo.model.Token;
import com.nguyendinhdoan.driverappdemo.remote.IFCMService;
import com.nguyendinhdoan.driverappdemo.remote.IGoogleAPI;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CustommerCall extends AppCompatActivity {

    private static final String TAG = "CustommerCall";
    TextView txtTime, txtAddress, txtDistance;
    Button btnCancel, btnAccept;

    MediaPlayer mediaPlayer;

    IGoogleAPI mServices;
    IFCMService mIfcmService;

    String customerId;
    double lat;
    double lng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custommer_call);

        mServices = Common.getGoogleAPI();
        mIfcmService = Common.getFCMService();

        // init view
        txtAddress = findViewById(R.id.txtAddress);
        txtTime = findViewById(R.id.txtTime);
        txtDistance = findViewById(R.id.txtDistance);

        btnCancel = findViewById(R.id.btnDecline);
        btnAccept = findViewById(R.id.btnAccept);

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!TextUtils.isEmpty(customerId)) {
                    cancelBooking(customerId);
                }
            }
        });

        btnAccept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CustommerCall.this, DriverTracking.class);
                // send customer location to new activity
                intent.putExtra("lat", lat);
                intent.putExtra("lng", lng);

                startActivity(intent);
                finish();
            }
        });

        // media
        mediaPlayer = MediaPlayer.create(this, R.raw.ringtone);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();

        if (getIntent() != null) {
            lat = getIntent().getDoubleExtra("lat", -1.0);
            lng = getIntent().getDoubleExtra("lng", -1.0);
            customerId = getIntent().getStringExtra("customer");

            // just copy getDirection from welcome
            getDirection(lat, lng);
        }
    }

    private void cancelBooking(String customerId) {
        DatabaseReference tokens = FirebaseDatabase.getInstance().getReference(Common.token_tbl);

        tokens.orderByKey().equalTo(customerId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                            Token token = postSnapshot.getValue(Token.class); // get token object from database with key

                            Notification notification = new Notification("notice", "driver cancel request of you"); // we send to driver app and we will deserialize it again
                            Sender content = new Sender(notification, token.getToken()); // send notification to token

                            mIfcmService.sendMessage(content)
                                    .enqueue(new Callback<Result>() {
                                        @Override
                                        public void onResponse(Call<Result> call, Response<Result> response) {
                                            if (response.isSuccessful()) {
                                                Log.d(TAG, "onResponse: messagei id: " + response.body().toString());
                                                Toast.makeText(CustommerCall.this, "cancel", Toast.LENGTH_SHORT).show();
                                                finish();
                                            } else {
                                                Toast.makeText(CustommerCall.this, "failed", Toast.LENGTH_SHORT).show();
                                            }
                                        }

                                        @Override
                                        public void onFailure(Call<Result> call, Throwable t) {
                                            Log.e(TAG, "ERROR: " + t.getMessage());
                                        }
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void getDirection(double lat, double lng) {
        Log.d(TAG, "LAST LOCATION " + Common.mLastLocation.getLatitude());
        Log.d(TAG, "LNG: " + Common.mLastLocation.getLongitude());
        Log.d(TAG, "lat: " + lat);
        Log.d(TAG, "lon" + lng);
        String requestApi = null;
        try {
            requestApi = "https://maps.googleapis.com/maps/api/directions/json?" +
                    "mode=drivings&" +
                    "transit_routing_preference=less_driving&" +
                    "origin=" + Common.mLastLocation.getLatitude() + "," + Common.mLastLocation.getLongitude() + "&" +
                    "destination=" + lat + "," + lng + "&" +
                    "key=" + getString(R.string.google_api_key);

            Log.d(TAG, "URL direction: " + requestApi);

            mServices.getPath(requestApi)
                    .enqueue(new Callback<String>() {
                        @Override
                        public void onResponse(Call<String> call, Response<String> response) {
                            try {
                                JSONObject jsonObject = new JSONObject(response.body());
                                JSONArray routes = jsonObject.getJSONArray("routes");

                                // after when get routes, just get first element of routes
                                JSONObject object = routes.getJSONObject(0);
                                // after get first element, just need get array with name "legs"
                                JSONArray legs = object.getJSONArray("legs");
                                // get the first element of arrays
                                JSONObject legsObject = legs.getJSONObject(0);

                                // now get distance
                                JSONObject distance = legsObject.getJSONObject("distance");
                                txtDistance.setText(distance.getString("text"));
                                Log.d(TAG, "distance: " + distance);

                                // get time
                                JSONObject time = legsObject.getJSONObject("duration");
                                txtTime.setText(time.getString("text"));
                                Log.d(TAG, "duration: " + time);

                                // get address
                                String address = legsObject.getString("end_address");
                                txtAddress.setText(address);
                                Log.d(TAG, "address: " + address);


                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(Call<String> call, Throwable t) {
                            Toast.makeText(CustommerCall.this, "" + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onStop() {
        mediaPlayer.release();
        super.onStop();
    }

    @Override
    protected void onPause() {
        mediaPlayer.release();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mediaPlayer.start();
    }
}
