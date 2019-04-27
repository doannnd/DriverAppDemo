package com.nguyendinhdoan.driverappdemo.services;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.nguyendinhdoan.driverappdemo.common.Common;
import com.nguyendinhdoan.driverappdemo.model.Token;

public class MyFirebaseIdServices extends FirebaseMessagingService {

    @Override
    public void onNewToken(String s) {
        super.onNewToken(s);
        updateTokenToServer(s); // when have refresh token , we need update to our real time database
    }

    private void updateTokenToServer(String s) {
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        DatabaseReference tokens = db.getReference(Common.token_tbl);

        Token token = new Token(s);
        if (FirebaseAuth.getInstance().getCurrentUser() != null) { // if already login, must update token
            tokens.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).setValue(token);
        }
    }
}
