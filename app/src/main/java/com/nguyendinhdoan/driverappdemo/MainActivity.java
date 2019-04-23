package com.nguyendinhdoan.driverappdemo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.nguyendinhdoan.driverappdemo.model.Driver;

import dmax.dialog.SpotsDialog;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class MainActivity extends AppCompatActivity {

    private RelativeLayout rootLayout;
    private Button signInButton;
    private Button registerButton;

    private FirebaseAuth auth;
    private FirebaseDatabase db;
    private DatabaseReference driversTable;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser driver = FirebaseAuth.getInstance().getCurrentUser();
        if (driver != null) {
            Intent intentWelcome = new Intent(MainActivity.this, WelcomeActivity.class);
            startActivity(intentWelcome);
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/Arkhip_font.ttf")
                .setFontAttrId(R.attr.fontPath)
                .build());

        setContentView(R.layout.activity_main);

        // init firebase auth
        auth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance();
        driversTable = db.getReference("drivers");

        // initView
        rootLayout = findViewById(R.id.root_layout);
        signInButton = findViewById(R.id.sign_in_button);
        registerButton = findViewById(R.id.register_button);

        // events
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRegisterDialog();
            }
        });
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLoginDialog();
            }
        });
    }

    private void showRegisterDialog() {
        AlertDialog.Builder registerDialog = new AlertDialog.Builder(this);
        registerDialog.setTitle(getString(R.string.register_dialog_title));
        registerDialog.setMessage(getString(R.string.register_dialog_message));

        // inflate layout
        LayoutInflater layoutInflater = LayoutInflater.from(this);
        View registerView = layoutInflater.inflate(R.layout.layout_register, null);

        // reference view
        final TextInputEditText emailEditText = registerView.findViewById(R.id.email_edit_text);
        final TextInputEditText passwordEditText = registerView.findViewById(R.id.password_edit_text);
        final TextInputEditText nameEditText = registerView.findViewById(R.id.name_edit_text);
        final TextInputEditText phoneEditText = registerView.findViewById(R.id.phone_edit_text);

        registerDialog.setView(registerView);

        // set button
        registerDialog.setPositiveButton(getString(R.string.sign_up_button_text), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();

                final String email = emailEditText.getText().toString();
                final String password = passwordEditText.getText().toString();
                final String name = nameEditText.getText().toString();
                final String phone = phoneEditText.getText().toString();

                // validate
                if (TextUtils.isEmpty(email)) {
                    Snackbar.make(rootLayout, "Please enter email address", Snackbar.LENGTH_LONG).show();
                    return;
                }

                if (TextUtils.isEmpty(password)) {
                    Snackbar.make(rootLayout, "Please enter password", Snackbar.LENGTH_LONG).show();
                    return;
                }

                if (password.length() < 6) {
                    Snackbar.make(rootLayout, "Password too short", Snackbar.LENGTH_LONG).show();
                    return;
                }

                if (TextUtils.isEmpty(name)) {
                    Snackbar.make(rootLayout, "Please enter name", Snackbar.LENGTH_LONG).show();
                    return;
                }

                if (TextUtils.isEmpty(phone)) {
                    Snackbar.make(rootLayout, "Please enter phone number", Snackbar.LENGTH_LONG).show();
                    return;
                }

                // register new user
                auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    // save driver to db
                                    Driver driver = new Driver();
                                    driver.setEmail(email);
                                    driver.setPassword(password);
                                    driver.setName(name);
                                    driver.setPhone(phone);

                                    String driverId = auth.getCurrentUser().getUid();
                                    // save
                                    driversTable.child(driverId).setValue(driver)
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if (task.isSuccessful()) {
                                                        Snackbar.make(rootLayout, "Register successful", Snackbar.LENGTH_LONG).show();
                                                    } else {
                                                        Snackbar.make(rootLayout, "Register failed", Snackbar.LENGTH_LONG).show();
                                                    }
                                                }
                                            });


                                } else {
                                    Snackbar.make(rootLayout, "Have error occur", Snackbar.LENGTH_LONG).show();
                                }
                            }
                        });

            }
        });

        registerDialog.setNegativeButton(getString(R.string.cancel_button_text), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        registerDialog.show();
    }

    private void showLoginDialog() {
        AlertDialog.Builder loginDialog = new AlertDialog.Builder(this);
        loginDialog.setTitle(getString(R.string.login_dialog_title));
        loginDialog.setMessage(getString(R.string.login_dialog_message));

        // inflate layout
        LayoutInflater layoutInflater = LayoutInflater.from(this);
        View loginView = layoutInflater.inflate(R.layout.layout_login, null);

        // reference view
        final TextInputEditText emailEditText = loginView.findViewById(R.id.email_edit_text);
        final TextInputEditText passwordEditText = loginView.findViewById(R.id.password_edit_text);

        loginDialog.setView(loginView);

        // set button
        loginDialog.setPositiveButton(getString(R.string.sign_in_button_text), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                // disable button sign in if processing
                signInButton.setEnabled(false);

                final String email = emailEditText.getText().toString();
                final String password = passwordEditText.getText().toString();

                // validate
                if (TextUtils.isEmpty(email)) {
                    Snackbar.make(rootLayout, "Please enter email address", Snackbar.LENGTH_LONG).show();
                    return;
                }

                if (TextUtils.isEmpty(password)) {
                    Snackbar.make(rootLayout, "Please enter password", Snackbar.LENGTH_LONG).show();
                    return;
                }

                if (password.length() < 6) {
                    Snackbar.make(rootLayout, "Password too short", Snackbar.LENGTH_LONG).show();
                    return;
                }

                final AlertDialog waitingDialog = new SpotsDialog.Builder().setContext(MainActivity.this).build();
                waitingDialog.show();
                // login user
                auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            waitingDialog.dismiss();
                            Intent intentWelcome = new Intent(MainActivity.this, WelcomeActivity.class);
                            startActivity(intentWelcome);
                            finish();
                        } else {
                            waitingDialog.dismiss();
                            Snackbar.make(rootLayout, "Failed: " + task.getException().getMessage(),
                                    Snackbar.LENGTH_LONG).show();
                            // active sign in button
                            signInButton.setEnabled(true);
                        }
                    }
                });

            }
        });

        loginDialog.setNegativeButton(getString(R.string.cancel_button_text), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        loginDialog.show();
    }
}
