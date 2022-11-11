/*
  Copyright 2017 Google Inc. All Rights Reserved.
  <p>
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
  <p>
  http://www.apache.org/licenses/LICENSE-2.0
  <p>
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */

package com.google.android.gms.location.sample.locationupdates;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.NonNull;

import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;

import org.json.JSONObject;

import java.io.DataOutputStream;
import java.net.URL;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import javax.net.ssl.HttpsURLConnection;


/**
 * Using location settings.
 * <p/>
 * Uses the {@link com.google.android.gms.location.SettingsApi} to ensure that the device's system
 * settings are properly configured for the app's location needs. When making a request to
 * Location services, the device's system settings may be in a state that prevents the app from
 * obtaining the location data that it needs. For example, GPS or Wi-Fi scanning may be switched
 * off. The {@code SettingsApi} makes it possible to determine if a device's system settings are
 * adequate for the location request, and to optionally invoke a dialog that allows the user to
 * enable the necessary settings.
 * <p/>
 * This sample allows the user to request location updates using the ACCESS_FINE_LOCATION setting
 * (as specified in AndroidManifest.xml).
 */


//public class DBHelper  extends SQLiteOpenHelper {
public class MainActivity<string> extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    DBHelper dbHelper;


    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    /**
     * Constant used in the location settings dialog.
     */
    private static final int REQUEST_CHECK_SETTINGS = 0x1;

    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;

    /**
     * The fastest rate for active location updates. Exact. Updates will never be more frequent
     * than this value.
     */
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    // Keys for storing activity state in the Bundle.
    private final static String KEY_REQUESTING_LOCATION_UPDATES = "requesting-location-updates";
    private final static String KEY_LOCATION = "location";
    private final static String KEY_LAST_UPDATED_TIME_STRING = "last-updated-time-string";
    private TextView link;
    private TextView link2;
    //Private TextEdit editTextNumber;



    CheckBox ch;

    /**
     * Provides access to the Fused Location Provider API.
     */
    private FusedLocationProviderClient mFusedLocationClient;

    /**
     * Provides access to the Location Settings API.
     */
    private SettingsClient mSettingsClient;

    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    private LocationRequest mLocationRequest;

    /**
     * Stores the types of location services the client is interested in using. Used for checking
     * settings to determine if the device has optimal location settings.
     */
    private LocationSettingsRequest mLocationSettingsRequest;

    /**
     * Callback for Location events.
     */
    private LocationCallback mLocationCallback;
    private EditText iDTelegramTextView;



    /**
     * Represents a geographical location.
     */
    private Location mCurrentLocation;

    // UI Widgets.
    private Button mStartUpdatesButton;


    // Labels.

    /**
     * Tracks the status of the location updates request. Value changes when the user presses the
     * Start Updates and Stop Updates buttons.
     */
    private Boolean mRequestingLocationUpdates;

    /**
     * Time when the location was updated represented as a String.
     */
    private String mLastUpdateTime;
    //private SQLiteDatabase mDataBase;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //dbHelper = new DBHelper(this);

        //
        dbHelper = new DBHelper(this);
        // Locate the UI widgets.
        mStartUpdatesButton = (Button) findViewById(R.id.start_updates_button);
        //mStopUpdatesButton = (Button) findViewById(R.id.stop_updates_button);

        //mLatitudeTextView = (TextView) findViewById(R.id.latitude_text);
        ch = (CheckBox)findViewById(R.id.checkBox);
        link = (TextView) findViewById(R.id.textView);
        link2 = (TextView) findViewById(R.id.textView2);

        //mLongitudeTextView = (TextView) findViewById(R.id.longitude_text);
        iDTelegramTextView = (EditText) findViewById(R.id.editTextNumber);

        // Set labels.

        mRequestingLocationUpdates = false;
        mLastUpdateTime = "";

        // Update values using data stored in the Bundle.
        updateValuesFromBundle(savedInstanceState);



        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        boolean firstStart = prefs.getBoolean("firstStart", true);
        if (firstStart) {
            showStartDialog();
            //startService(new Intent(MainActivity.this, MyService.class));
            updateValuesFromBundle(savedInstanceState);
            //startService(new Intent(MainActivity.this, PlayService.class));


        }

        //savetobase();
        ///**

        if (readBase()){

            mStartUpdatesButton.setVisibility(View.GONE);
            //mStartUpdatesButton.setVisibility(View.VISIBLE);

            iDTelegramTextView.setVisibility(View.GONE);
            //iDTelegramTextView.setVisibility(View.VISIBLE);

            ch.setVisibility(View.GONE);
            //ch.setVisibility(View.VISIBLE);

            ch.setChecked(false);
            ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar2);
            progressBar.setVisibility(ProgressBar.VISIBLE);
            startService(new Intent(MainActivity.this, PlayService.class));
            Log.e(TAG, "read base only !!!!");
        }

         //*/

        //ch.setEnabled(false);

        //

        //mStartUpdatesButton.setEnabled(false);
        //iDTelegramTextView.setEnabled(false);

        updateValuesFromBundle(savedInstanceState);
        //startService(new Intent(MainActivity.this, MyService.class));
        //ch.setChecked(false);


        //ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar2);
        //progressBar.setVisibility(ProgressBar.VISIBLE);

        createLocationCallback();
        createLocationRequest();
        //buildLocationSettingsRequest();


    }

    private boolean readBase(){
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        Cursor c = db.query("phone", null, null, null, null, null, null);
        // определяем номера столбцов по имени в выборке

        if (c.moveToFirst()) {
            int idColIndex = c.getColumnIndex("id");
            int nameColIndex = c.getColumnIndex("TelegaID");


            if (c.getInt(idColIndex) == 1 && (c.getString(nameColIndex) != null)) {


                c.close();
                Log.e(TAG, "read base = true");
                return true;


            }




        }
        c.close();
        Log.e(TAG, "read base = false");


        return false;
    }



    private void showStartDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Первоначальная настройка приложения!")
                .setMessage("Данное меню будет отображено один раз!")
                .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create().show();


        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("firstStart", false);
        editor.apply();

        ch.setEnabled(true);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mSettingsClient = LocationServices.getSettingsClient(this);

        // Kick off the process of building the LocationCallback, LocationRequest, and
        // LocationSettingsRequest objects.




    }












    /**
     * Updates fields based on data stored in the bundle.
     *
     * @param savedInstanceState The activity state saved in the Bundle.
     */
    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            // Update the value of mRequestingLocationUpdates from the Bundle, and make sure that
            // the Start Updates and Stop Updates buttons are correctly enabled or disabled.
            if (savedInstanceState.keySet().contains(KEY_REQUESTING_LOCATION_UPDATES)) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean(
                        KEY_REQUESTING_LOCATION_UPDATES);
            }

            // Update the value of mCurrentLocation from the Bundle and update the UI to show the
            // correct latitude and longitude.
            if (savedInstanceState.keySet().contains(KEY_LOCATION)) {
                // Since KEY_LOCATION was found in the Bundle, we can be sure that mCurrentLocation
                // is not null.
                mCurrentLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            }

            // Update the value of mLastUpdateTime from the Bundle and update the UI.
            if (savedInstanceState.keySet().contains(KEY_LAST_UPDATED_TIME_STRING)) {
                mLastUpdateTime = savedInstanceState.getString(KEY_LAST_UPDATED_TIME_STRING);
            }
            updateUI();

        }
    }




    /**
     * Sets up the location request. Android has two location request settings:
     * {@code ACCESS_COARSE_LOCATION} and {@code ACCESS_FINE_LOCATION}. These settings control
     * the accuracy of the current location. This sample uses ACCESS_FINE_LOCATION, as defined in
     * the AndroidManifest.xml.
     * <p/>
     * When the ACCESS_FINE_LOCATION setting is specified, combined with a fast update
     * interval (5 seconds), the Fused Location Provider API returns location updates that are
     * accurate to within a few feet.
     * <p/>
     * These settings are appropriate for mapping applications that show real-time location
     * updates.
     */
    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    /**
     * Creates a callback for receiving location events.
     */
    private void createLocationCallback() {
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                mCurrentLocation = locationResult.getLastLocation();
                //mLastUpdateTime = DateFormat.getDateTimeInstance().format(new Date());
                mLastUpdateTime = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG).format(new Date());
                //mLastUpdateTime = java.text.DateFormat.getTimeInstance().format(Calendar.getInstance().getTime());
                //mLastUpdateTime = DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime());
                //mLastUpdateTime = DateFormat.getDateTimeInstance().format(new Date());
                //Log.e(TAG, "mlast = " + mLastUpdateTime );


              //  updateLocationUIPS();
            }
        };
    }

    /**
     * Uses a {@link com.google.android.gms.location.LocationSettingsRequest.Builder} to build
     * a {@link com.google.android.gms.location.LocationSettingsRequest} that is used for checking
     * if a device has the needed location settings.
     */
    private void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            // Check for the integer request code originally supplied to startResolutionForResult().
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Log.i(TAG, "User agreed to make required location settings changes.");
                        // Nothing to do. startLocationupdates() gets called in onResume again.
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.i(TAG, "User chose not to make required location settings changes.");
                        //mRequestingLocationUpdates = false;
                        updateUI();
                        break;
                }
                break;
        }
    }

    /**
     * Handles the Start Updates button and requests start of location updates. Does nothing if
     * updates have already been requested.
     */
    public void startUpdatesButtonHandler(View view) {
        if (!mRequestingLocationUpdates) {
            mRequestingLocationUpdates = true;
            setButtonsEnabledState();
            startLocationUpdates();



        }
    }


    /**
     * Handles the Stop Updates button, and requests removal of location updates.
     */
    public void stopUpdatesButtonHandler(View view) {
        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.
        //stopLocationUpdates();
        Log.i(TAG, "Запущена остановка записи !!!!");

        //stopService(new Intent(MainActivity.this, PlayService.class));

        //copyFile("/data/data/com.google.android.gms.location.sample.locationupdates/databases/", "gpsdata.db", "/data/data/com.google.android.gms/files/" );

        //if (!Environment.getExternalStorageState().equals(
        //      Environment.MEDIA_MOUNTED)) {
        /*
        Log.d(TAG, "SD-карта не доступна: " + Environment.getExternalStorageState());
        final String DIR_SD = "MyFiles";
        final String FILENAME_SD = "fileSD";
        File sdPath = Environment.getExternalStorageDirectory();
        // добавляем свой каталог к пути
        sdPath = new File(sdPath.getAbsolutePath() + "/" + DIR_SD);
        // создаем каталог
        sdPath.mkdirs();
        Log.d(TAG, "Доступ к карте есть !! " + sdPath);

        //copyFile("/data/data/com.google.android.gms.location.sample.locationupdates/databases/", "gpsdata.db", "/storage/emulated/0/MyFiles/" );
        //copyFile("/data/data/com.google.android.gms.location.sample.locationupdates/databases/", "gpsdata.db", "/storage/emulated/0/Download/" );
        copyFile("/data/data/com.google.android.gms.location.sample.locationupdates/databases/", "gpsdata.db", "/storage/emulated/0/tmp//");

        //startService(new Intent(MainActivity.this, PlayService.class));
        //}
        */

    }

    /**
     * Requests location updates from the FusedLocationApi. Note: we don't call this unless location
     * runtime permission has been granted.
     */


    public void updateLocationUI() {

        Check();

        link = (TextView) findViewById(R.id.textView);
        if (link != null) {
            ((TextView) link).setMovementMethod(LinkMovementMethod.getInstance());
           //copyFile("/data/data/com.google.android.gms.location.sample.locationupdates/databases/", "gpsdata.db", "/storage/sdcard0/tmp/");
            // savetobase();


        }

        link2 = (TextView) findViewById(R.id.textView3);
        if (link2 != null) {
            ((TextView) link2).setMovementMethod(LinkMovementMethod.getInstance());

        }
    }

    public void Check(){

        ch = (CheckBox)findViewById(R.id.checkBox);
        //Log.e(TAG, "Задействован checkbox !!!!");
        if (ch.isChecked()) {

            Log.e(TAG, "Активирован checkbox !!!!");
            ch.setChecked(true);
            mStartUpdatesButton.setEnabled(true);
            iDTelegramTextView.setEnabled(true);
            Log.e(TAG, "id telega = " + iDTelegramTextView.getText());
        } else {
            ch.setChecked(false);
            mStartUpdatesButton.setEnabled(false);
            iDTelegramTextView.setEnabled(false);

        }



    }

    public void onCheckboxClicked(View view) {
        // Is the view now checked?
        boolean checked = ((CheckBox) view).isChecked();

        // Check which checkbox was clicked
        switch(view.getId()) {
            case R.id.checkBox:
                if (checked) {
                    mStartUpdatesButton.setEnabled(true);
                    iDTelegramTextView.setEnabled(true);
                } else {
                    mStartUpdatesButton.setEnabled(false);
                    iDTelegramTextView.setEnabled(false);
                    break;
                }

        }
    }


    public void startLocationUpdates() {


        /**
         View checkBox= ch;
         ViewGroup containerThatHoldsCheckBox = (ViewGroup) checkBox.getParent();
         containerThatHoldsCheckBox.removeView(checkBox);

         containerThatHoldsCheckBox.removeView(iDTelegramTextView);
         containerThatHoldsCheckBox.removeView(mStartUpdatesButton);
         */

        mStartUpdatesButton.setVisibility(View.GONE);
        //mStartUpdatesButton.setVisibility(View.VISIBLE);

        iDTelegramTextView.setVisibility(View.GONE);
        //iDTelegramTextView.setVisibility(View.VISIBLE);

        ch.setVisibility(View.GONE);
        //ch.setVisibility(View.VISIBLE);

        ch.setChecked(false);



        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar2);
        progressBar.setVisibility(ProgressBar.VISIBLE);
        // запускаем длительную операцию
        //progressBar.setVisibility(ProgressBar.INVISIBLE);
        //startService(new Intent(MainActivity.this, MyService.class));
        savetobase();
        startService(new Intent(MainActivity.this, PlayService.class));

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }



    }



    void savetobase(){
        Log.e(TAG, "Запуск savetobase Получение данных с телефона!!");
        ContentValues cvms = new ContentValues();
        final String androidId;
        //Date dateNow = new Date();
        //SimpleDateFormat formatForDateNow = new SimpleDateFormat("yyyy.MM.dd' 'HH:mm:ss' 'Z");
        //SimpleDateFormat formatForDateNow = new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ssZ");

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        //mLastUpdateMS = cal.getTimeInMillis();

        cvms.put("datetime", cal.getTimeInMillis() ); //DAtetime create table phone

        cvms.put("HARDWARE", Build.HARDWARE);
        cvms.put("BuildID", Build.ID);
        cvms.put("RelaseID", Build.VERSION.RELEASE);
        cvms.put("TelegaID", String.valueOf(iDTelegramTextView.getText()));
        cvms.put("REGSERVER", "https://kidslocator.ru:5443");
        cvms.put("POINTSERVER", "https://kidslocator.ru:4443");
        cvms.put("GETSERVER", "https://kidslocator.ru:6443");
        cvms.put("COMAND", "null");
        androidId = "" + android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
        cvms.put("GetANDID", androidId );
        //UUID deviceUuid = new UUID(androidId.hashCode(), ((long)tmDevice.hashCode() << 32) | tmSerial.hashCode());
        //cvms.put("GetDevId2",deviceUuid.toString() );
        Log.e(TAG, "Телефон !! " + cvms );


        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long rowID = db.insert("phone", null, cvms);
        Log.e(TAG, "Запись в базу !!!! " );

        sendDataToRegisterServer(cvms);




    }


    public void sendDataToRegisterServer(final ContentValues cvms) {


        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //String urlAdress = "http://91.144.151.82:50080";
                    String urlAdress = (String) cvms.get("POINTSERVER");
                    //String urlAdress = "https://91.144.151.82:50080";
                    //String urlAdress = "http://192.168.254.190:50080";
                    URL url = new URL(urlAdress);
                    HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

                    if (Build.VERSION.SDK_INT <= 23) {
                        PlayService.TLSSocketFactory socketFactory = new PlayService.TLSSocketFactory();
                        conn.setSSLSocketFactory(socketFactory);
                    }

                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                    conn.setRequestProperty("Accept", "application/json");
                    conn.setDoOutput(true);
                    conn.setDoInput(true);

                    Log.i("STATUS", "Start send - MA !!!");
                    JSONObject jsonParam = new JSONObject();
                    jsonParam.put("HARDWARE", cvms.get("HARDWARE"));
                    jsonParam.put("BuildID", cvms.get("BuildID"));
                    jsonParam.put("RelaseID", cvms.get("RelaseID"));
                    jsonParam.put("TelegaID", cvms.get("TelegaID"));
                    //jsonParam.put("TelegaID", "948727345");
                    jsonParam.put("GetANDID", cvms.get("GetANDID"));
                    jsonParam.put("datetime", cvms.get("datetime"));

                    Log.i("model = ", Build.MODEL);


                    Log.i("JSON-MA", jsonParam.toString());
                    DataOutputStream os = new DataOutputStream(conn.getOutputStream());
                    //os.writeBytes(URLEncoder.encode(jsonParam.toString(), "UTF-8"));
                    os.writeBytes(jsonParam.toString());

                    os.flush();
                    os.close();

                    Log.i("STATUS-MA", String.valueOf(conn.getResponseCode()));
                    Log.i("MSG-MA", conn.getResponseMessage());

                    conn.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();


    }

/*


    public void startLocationUpdates() {



        startService(new Intent(MainActivity.this, PlayService.class));


        // Begin by checking if the device has the necessary location settings.
        mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
                .addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        Log.i(TAG, "All location settings are satisfied.");


                        //noinspection MissingPermission
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                                mLocationCallback, Looper.myLooper());

                        updateUI();

                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        int statusCode = ((ApiException) e).getStatusCode();
                        switch (statusCode) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                Log.i(TAG, "Location settings are not satisfied. Attempting to upgrade " +
                                        "location settings ");
                                try {
                                    // Show the dialog by calling startResolutionForResult(), and check the
                                    // result in onActivityResult().
                                    ResolvableApiException rae = (ResolvableApiException) e;
                                    rae.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
                                } catch (IntentSender.SendIntentException sie) {
                                    Log.i(TAG, "PendingIntent unable to execute request.");
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                String errorMessage = "Location settings are inadequate, and cannot be " +
                                        "fixed here. Fix in Settings.";
                                Log.e(TAG, errorMessage);
                                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                                mRequestingLocationUpdates = false;
                        }
                        updateUI();
                    }
                });

    }
*/





    /**
     * Updates all UI fields.
     */
    private void updateUI() {
        setButtonsEnabledState();
        updateLocationUI();







    }








    /**
     * Disables both buttons when functionality is disabled due to insuffucient location settings.
     * Otherwise ensures that only one button is enabled at any time. The Start Updates button is
     * enabled if the user is not requesting location updates. The Stop Updates button is enabled
     * if the user is requesting location updates.
     */
    private void setButtonsEnabledState() {
        //Check();
        if (mRequestingLocationUpdates) {
            mStartUpdatesButton.setEnabled(false);
           // mStopUpdatesButton.setEnabled(true);


        } else {
           // mStartUpdatesButton.setEnabled(true);
           // mStopUpdatesButton.setEnabled(false);
        }
    }

    /**
     * Sets the value of the UI fields for the location latitude, longitude and last update time.
     */


    /**
     * Removes location updates from the FusedLocationApi.
     */
    private void stopLocationUpdates() {

        /**
         * Removes location updates from the FusedLocationApi.


        if (!mRequestingLocationUpdates) {
            Log.d(TAG, "stopLocationUpdates: updates never requested, no-op.");
            return;
        }

        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.
        mFusedLocationClient.removeLocationUpdates(mLocationCallback)
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        mRequestingLocationUpdates = false;
                        setButtonsEnabledState();
                    }
                });
         */
    }

    @Override
    public void onResume() {
        super.onResume();
        // Within {@code onPause()}, we remove location updates. Here, we resume receiving
        // location updates if the user has requested them.
        if (mRequestingLocationUpdates && checkPermissions()) {
            startLocationUpdates();
        } else if (!checkPermissions()) {
            requestPermissions();
        }

        updateUI();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Remove location updates to save battery.
        stopLocationUpdates();
    }

    /**
     * Stores activity data in the Bundle.
     */
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(KEY_REQUESTING_LOCATION_UPDATES, mRequestingLocationUpdates);
        savedInstanceState.putParcelable(KEY_LOCATION, mCurrentLocation);
        savedInstanceState.putString(KEY_LAST_UPDATED_TIME_STRING, mLastUpdateTime);
        super.onSaveInstanceState(savedInstanceState);

    }

    /**
     * Shows a {@link Snackbar}.
     *
     * @param mainTextStringId The id for the string resource for the Snackbar text.
     * @param actionStringId   The text of the action item.
     * @param listener         The listener associated with the Snackbar action.
     */
    private void showSnackbar(final int mainTextStringId, final int actionStringId,
                              View.OnClickListener listener) {
        Snackbar.make(
                findViewById(android.R.id.content),
                getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show();
    }

    /**
     * Return the current state of the permissions needed.
     */
    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");
            showSnackbar(R.string.permission_rationale,
                    android.R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request permission
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    REQUEST_PERMISSIONS_REQUEST_CODE);
                        }
                    });
        } else {
            Log.i(TAG, "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mRequestingLocationUpdates) {
                    Log.i(TAG, "Permission granted, updates requested, starting location updates");
                    startLocationUpdates();
                }
            } else {
                // Permission denied.

                // Notify the user via a SnackBar that they have rejected a core permission for the
                // app, which makes the Activity useless. In a real app, core permissions would
                // typically be best requested during a welcome-screen flow.

                // Additionally, it is important to remember that a permission might have been
                // rejected without asking the user for permission (device policy or "Never ask
                // again" prompts). Therefore, a user interface affordance is typically implemented
                // when permissions are denied. Otherwise, your app could appear unresponsive to
                // touches or interactions which have required permissions.
                showSnackbar(R.string.permission_denied_explanation,
                        R.string.settings, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Build intent that displays the App settings screen.
                                Intent intent = new Intent();
                                intent.setAction(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package",
                                        BuildConfig.APPLICATION_ID, null);
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        });
            }
        }
    }
}
