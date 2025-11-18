package com.atul.ninja_connect_java;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
//import android.support.annotation.NonNull;
//import android.support.v4.app.ActivityCompat;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class CallReceiver extends BroadcastReceiver {

    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference, dataBaseReferenceUpdateValue, workStatusDataReference;
    String outgoing_state = "";
    Context context;

    String last_status = "";


    @Override
    public void onReceive(Context context, Intent intent) {
//        context = context;

        outgoing_state = intent.getStringExtra(TelecomManager.EXTRA_OUTGOING_CALL_EXTRAS);

        Log.e("check", "onReceive ");

        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference("call_status").child("answer");
        workStatusDataReference = firebaseDatabase.getReference("work_status").child("last_status");


        dataBaseReferenceUpdateValue = firebaseDatabase.getReference();

        workStatusDataReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                last_status = snapshot.getValue(Integer.class).toString();

                if (last_status.equalsIgnoreCase("1")){

                    resetDatabase(context, databaseReference, 0);
                }else {

                    resetDatabase(context, databaseReference, 2);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


//        resetDatabase(context, databaseReference);

        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
            // Incoming call
            String incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
            Log.e("check", "Incoming Number: " + incomingNumber);

            if (incomingNumber != null) {

                fetchNameUsingNumber(context, dataBaseReferenceUpdateValue, incomingNumber);

            }

//            sendPhoneNumberToDataBase(incomingNumber);

            // Add your logic to accept or decline the call
            // For example, you can use the TelephonyManager to end the call
            databaseReference.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    int status = snapshot.getValue(Integer.class);

                    Log.e("check", "status: " + status);
                    if (status == 1) {
                        acceptCall(context);
                    } else if (status == 0) {
                        disconnectCall(context);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });

            //this is for call forwarding and stop call forwarding


            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            try {
                telephonyManager.getClass().getMethod("answerRingingCall").invoke(telephonyManager);
                // To decline the call, you can use the endCall() method
                // telephonyManager.getClass().getMethod("endCall").invoke(telephonyManager);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


    }

    private void fetchNameUsingNumber(Context context, DatabaseReference databaseReference, String number) {

        Context context1 = context;
        String callerName = "";

        /*if (number != null) {
            ContentResolver contentResolver = context1.getContentResolver();

            Cursor cursor = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME},
                    ContactsContract.CommonDataKinds.Phone.NUMBER + " =?",
                    new String[]{number}, number);

            if (cursor != null && cursor.getCount() > 0) {
                cursor.moveToFirst();
                callerName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                Log.e("check", "Name is: " + callerName);
            }
            cursor.close();
        }*/

        if (number != null) {
            callerName = getContactName(context, number);
        }


        Map<String, Object> updateData = new HashMap<>();
        updateData.put("mobile", number);
        updateData.put("username", callerName);

        Log.e("check", "number to send: " + number);

        databaseReference.child("incoming_call_status").updateChildren(updateData)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Log.e("check", "onComplete");
                    }
                });


    }

    public static String getContactName(Context context, String phoneNumber) {

        if (phoneNumber.length() <= 10) {
            phoneNumber = "+91" + phoneNumber;
        }

        ContentResolver cr = context.getContentResolver();
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        Cursor cursor = cr.query(uri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);
        if (cursor == null) {
            return null;
        }
        String contactName = null;
        if (cursor.moveToFirst()) {
            contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
        }

        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }

        return contactName;
    }

    private void resetDatabase(Context context, DatabaseReference databaseReference, int status) {

//        firebaseDatabase.getReference("call_status").child("answer");

        databaseReference.setValue(status)
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


    private void acceptCall(Context context) {

        TelecomManager tm = (TelecomManager) context
                .getSystemService(Context.TELECOM_SERVICE);

        if (tm == null) {
            // whether you want to handle this is up to you really
            throw new NullPointerException("tm == null");
        }

        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ANSWER_PHONE_CALLS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        tm.acceptRingingCall();


    }


    private void disconnectCall(Context context) {

        TelecomManager tm = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
        if (tm != null) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ANSWER_PHONE_CALLS) != PackageManager.PERMISSION_GRANTED) {
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
                    Toast.makeText(context, "Call Disconnected", Toast.LENGTH_SHORT).show();
                }
            }
            // success == true if call was terminated.
        }

    }


}
