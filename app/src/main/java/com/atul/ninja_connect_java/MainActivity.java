package com.atul.ninja_connect_java;


import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
//import android.support.annotation.NonNull;
//import android.support.v4.app.ActivityCompat;
//import android.support.v4.content.ContextCompat;
//import android.support.v7.app.AppCompatActivity;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private Button call, disconnect;

    private static final String TAG = "SimSlotUtils";
    private static final int REQUEST_CALL_PHONE = 1;
    private static final String PHONE_NUMBER = "7517433973";

    private static final int PERMISSION_REQUEST_CODE = 100;
    private Button btn_start_service, btn_stop_service;

    String number = "";

    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;


    private String[] permissions = {
            Manifest.permission.CALL_PHONE,
            Manifest.permission.MODIFY_PHONE_STATE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ANSWER_PHONE_CALLS,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.WRITE_CALL_LOG
    };
    Context context;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        startService(new Intent(MainActivity.this, CallBackgroundService.class));
//        Toast.makeText(MainActivity.this, "onCreate", Toast.LENGTH_SHORT).show();

        if (!Settings.canDrawOverlays(this)){
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:"+getPackageName()));
            startActivityForResult(intent, 0);
        }

        btn_start_service = findViewById(R.id.btn_start_service);
        btn_stop_service = findViewById(R.id.btn_stop_service);

        firebaseDatabase = FirebaseDatabase.getInstance();
//        databaseReference = firebaseDatabase.getReference("info").child("number");
        databaseReference = firebaseDatabase.getReference("info");

        context = this;
        checkPermissions();
//        call = findViewById(R.id.bt_call);
//        disconnect = findViewById(R.id.bt_disconnect);
//        call.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                makePhoneCall(PHONE_NUMBER);
//            }
//        });
//        disconnect.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                TelecomManager tm = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
//                if (tm != null) {
//                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ANSWER_PHONE_CALLS) != PackageManager.PERMISSION_GRANTED) {
//                        // TODO: Consider calling
//                        //    ActivityCompat#requestPermissions
//                        // here to request the missing permissions, and then overriding
//                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//                        //                                          int[] grantResults)
//                        // to handle the case where the user grants the permission. See the documentation
//                        // for ActivityCompat#requestPermissions for more details.
//                        return;
//                    }
//                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
//                        boolean success = tm.endCall();
//
//                        if (success) {
//                            Toast.makeText(MainActivity.this, "Call Disconnected", Toast.LENGTH_SHORT).show();
//                        }
//                    }
//                    // success == true if call was terminated.
//                }
//
//            }
//        });


//        getNumberFromFirebase();


        btn_start_service.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startService(new Intent(MainActivity.this, CallBackgroundService.class));
                Toast.makeText(context, "Service Started!", Toast.LENGTH_SHORT).show();
            }
        });

        btn_stop_service.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopService(new Intent(MainActivity.this, CallBackgroundService.class));
                Toast.makeText(context, "Service Stopped!", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void makePhoneCall(String str_number) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CALL_PHONE}, REQUEST_CALL_PHONE);
            } else {
//                String dial = "tel:" + PHONE_NUMBER;
                String dial = "tel:" + str_number;
                startActivity(new Intent(Intent.ACTION_CALL, Uri.parse(dial)));
            }
        } else {
//            String dial = "tel:" + PHONE_NUMBER;
            String dial = "tel:" + str_number;
            startActivity(new Intent(Intent.ACTION_CALL, Uri.parse(dial)));
        }
    }


    private void checkPermissions() {
        // Check if permissions are not granted
        boolean permissionsNeeded = false;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded = true;
                break;
            }
        }


        if (permissionsNeeded) {
            // Request the permissions
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        } else {
            // Permissions already granted
            showToast("Permissions already granted");
        }


    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            // Check if all permissions are granted
            boolean allGranted = true;
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                showToast("Permissions granted successfully");
            } else {
//                showToast("Permissions denied");
            }
        }
    }


    private void getNumberFromFirebase() {


        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

//                long num = snapshot.getValue(Long.class);
//                String num = snapshot.getValue(String.class); // working
//                number = Long.toString(num);

//                Toast.makeText(context, "number is: " + num, Toast.LENGTH_SHORT).show();
//                makePhoneCall(num);
//                Toast.makeText(context, "onDataChange", Toast.LENGTH_SHORT).show();


//                Long str_num = snapshot.child("number").getValue(Long.class);
//                Long str_status = snapshot.child("status").getValue(Long.class);

//                Log.e("check", "str_num: " + str_num + "  str_status: " + str_status);


                readData(snapshot);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

                Toast.makeText(context, "onCancelled", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void readData(DataSnapshot snapshot) {

        Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
        String num = map.get("number").toString();
        String status = map.get("status").toString();
        if (status.equalsIgnoreCase("1")) {
            makePhoneCall(num);
        } else if (status.equalsIgnoreCase("0")) {
            disconnectCall();
        }
    }

    private void disconnectCall() {

        TelecomManager tm = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
        if (tm != null) {
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ANSWER_PHONE_CALLS) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {


                boolean success = tm.endCall();
                if (success) {
                    Toast.makeText(MainActivity.this, "Call Disconnected", Toast.LENGTH_SHORT).show();
                }
            }
            // success == true if call was terminated.
        }

    }


    public void launchApplication(View view) {

        Intent intent = new Intent(Intent.ACTION_MAIN);
//        intent.setPackage("com.example.app"); // Replace with the actual package name
        intent.setPackage("com.google.android.youtube"); // Replace with the actual package name
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // Optional: start a new activity

        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // Handle the exception if the app is not found
            e.printStackTrace();
        }
    }
}