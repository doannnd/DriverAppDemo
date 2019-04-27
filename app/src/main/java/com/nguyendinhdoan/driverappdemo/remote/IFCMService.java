package com.nguyendinhdoan.driverappdemo.remote;

import com.nguyendinhdoan.driverappdemo.model.Result;
import com.nguyendinhdoan.driverappdemo.model.Sender;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface IFCMService {

    @Headers({
            "Content-Type:application/json",
            "Authorization:key=AAAAt_H3ne4:APA91bHLL8Jzc5eeivbca-XIrwVnqjLfTICE-oPpfLqYlR5ybGXTHqyR-_XiZx4w92WuhJST14MGHIyMDn0Zw1zXgdFxdLyFNeo5iWdNue5exj5LkGOb_e7u5G2E1lbsk1rUCmBgBI7h"
    })
    @POST("fcm/send")
    Call<Result> sendMessage(@Body Sender body);
}
