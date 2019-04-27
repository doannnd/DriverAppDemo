package com.nguyendinhdoan.driverappdemo.common;

import android.location.Location;

import com.nguyendinhdoan.driverappdemo.remote.FCMClient;
import com.nguyendinhdoan.driverappdemo.remote.IFCMService;
import com.nguyendinhdoan.driverappdemo.remote.IGoogleAPI;
import com.nguyendinhdoan.driverappdemo.remote.RetrofitClient;

public class Common {

    public static Location mLastLocation;

    public static final String driver_tbl = "driversLocation";
    public static final String user_driver_tbl = "driversTable";
    public static final String user_rider_tbl = "usersTable";
    public static final String pickup_request_tbl = "PickupRequest";
    public static final String token_tbl = "tokens";

    public static final String baseURL = "https://maps.googleapis.com";
    public static final String fcmFURL = "https://fcm.googleapis.com";

    public static IGoogleAPI getGoogleAPI() {
        return RetrofitClient.getClient(baseURL).create(IGoogleAPI.class);
    }

    public static IFCMService getFCMService() {
        return FCMClient.getClient(fcmFURL).create(IFCMService.class);
    }
}
