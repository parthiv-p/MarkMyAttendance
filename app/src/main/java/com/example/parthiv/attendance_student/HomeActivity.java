//#TODO: Test with low power advertising
//#TODO: how to see if user has switched on permission for the app
//#TODO: ID SECURE


package com.example.parthiv.attendance_student;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ProgressBar;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import android.location.LocationManager;
import android.view.View;

import android.widget.Button;
import android.widget.Toast;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static android.bluetooth.le.AdvertiseSettings.ADVERTISE_MODE_BALANCED;
import static android.bluetooth.le.AdvertiseSettings.ADVERTISE_MODE_LOW_POWER;
import static android.bluetooth.le.AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM;
import static android.bluetooth.le.AdvertiseSettings.ADVERTISE_TX_POWER_ULTRA_LOW;
import static java.security.AccessController.getContext;


public class HomeActivity extends AppCompatActivity {

    //GENERAL
    private static final int REQUEST_ENABLE_BT = 1;         //for startActivityForResult

    //UI Elements
    private Button attendanceButton;
    private ProgressBar advertiseProgressBar;

    //FOR BLE
    private BluetoothAdapter mBluetoothAdapter;
    private LocationManager manager;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;

    private static final String TAG = HomeActivity.class.getSimpleName();

   // private final static String BASE_UUID = "00000000";  //https://www.bluetooth.com/specifications/assigned-numbers/service-discovery

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        attendanceButton  = (Button) findViewById(R.id.attendanceButton);
        advertiseProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        advertiseProgressBar.getProgressDrawable().setColorFilter(
                Color.RED, android.graphics.PorterDuff.Mode.SRC_IN);
        advertiseProgressBar.setIndeterminate(false);

        BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);


    }
/*
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == REQUEST_ENABLE_BT) {
            if(resultCode == RESULT_OK){
                attendanceButton.setEnabled(true);
            }
        }
    }*/

    @Override
    protected void onStart() {
        super.onStart();
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("BLE needs access to location services, do you want to enable it?")
                    .setCancelable(false)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog, final int id) {
                            startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog, final int id) {
                            dialog.cancel();
                    }
                    });
            final AlertDialog alert = builder.create();
            alert.show();
        }

        if (mBluetoothAdapter != null) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        } else {
            Toast.makeText(this, R.string.BLE_not_supported, Toast.LENGTH_SHORT).show();
        }
    }


    AdvertiseCallback mAdvCallback = new AdvertiseCallback() {
        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);

            switch (errorCode) {
                case ADVERTISE_FAILED_ALREADY_STARTED:
                    break;
                case ADVERTISE_FAILED_DATA_TOO_LARGE:
                    break;
                case ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                    Toast.makeText(HomeActivity.this, "BLE Advertising Not Supported", Toast.LENGTH_SHORT).show();
                    break;
                case ADVERTISE_FAILED_INTERNAL_ERROR:
                    break;
                case ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Toast.makeText(HomeActivity.this, "Advertising Started", Toast.LENGTH_SHORT).show();
            attendanceButton.setEnabled(false);
            attendanceButton.setTextColor(getColor(R.color.errorText));
            new DelayCounter().execute();
        }
    };

    public void advertise (View v) {

        if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER) && mBluetoothAdapter.isEnabled()) {
            mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
            if (mBluetoothLeAdvertiser != null) {

                    AdvertiseSettings mAdvSettings = new AdvertiseSettings.Builder()
                        .setAdvertiseMode(ADVERTISE_MODE_LOW_POWER)
                        .setTxPowerLevel(ADVERTISE_TX_POWER_ULTRA_LOW)
                        .setTimeout(10000)
                        .build();

                    //ParcelUuid serviceDataUuid = ParcelUuid.fromString(BASE_UUID);
                    //Log.e(TAG, serviceDataUuid.toString());
                    byte[] manufactureData = new byte[] {(byte) 0x39, (byte)0x00, (byte)0x13, (byte) 0x01, (byte) 0x00, (byte) 0x06};

                    int manufacturerId = 0x0000;

                    //String android_id = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
                    //Log.e(TAG, android_id);
                    //byte[] android_id_byte = android_id.getBytes(StandardCharsets.UTF_8);

                    AdvertiseData mAdvData = new AdvertiseData.Builder()
                        .addManufacturerData(manufacturerId, manufactureData)
                        .build();

                    mBluetoothLeAdvertiser.startAdvertising(mAdvSettings, mAdvData, mAdvCallback);
            } else {
                Toast.makeText(this, R.string.ble_adv_error, Toast.LENGTH_SHORT).show();
                attendanceButton.setTextColor(getColor(R.color.errorText));
            }
        } else {
            Log.e(TAG,"Permission Error in advertise function");
            Toast.makeText(this, R.string.permission_error, Toast.LENGTH_SHORT).show();
        }
    }

    private class DelayCounter extends AsyncTask<Void, Integer, Void> { //void to execute, integer for progress and void for result
        int delay = 1;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            advertiseProgressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Toast.makeText(HomeActivity.this, R.string.attendance_success, Toast.LENGTH_LONG).show();
            advertiseProgressBar.setVisibility(View.INVISIBLE);
            attendanceButton.setEnabled(true);
            attendanceButton.setTextColor(getColor(R.color.green));
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            advertiseProgressBar.setProgress(delay);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            while(delay < 100){
                try{
                    Thread.sleep(100);                          //alternate would be SystemClock.sleep but this was more challenging
                } catch (InterruptedException e) {
                    Thread.interrupted();
                }
                delay++;
                publishProgress(delay);
            }
            return null;
        }
    }
}

