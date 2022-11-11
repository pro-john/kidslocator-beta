package com.google.android.gms.location.sample.locationupdates;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;


import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;


public class PlayService extends Service {

    private static final String TAG = MainActivity.class.getSimpleName();
    private String mLastUpdateTime;
    private String mLastUpdateTimeTZ;
    public static final String CHANNEL_ID = "PlayServiceChannel";
    long mLastUpdateMS;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Toast.makeText(this, "Служба создана",
                Toast.LENGTH_SHORT).show();
        updateLocationUIPS();
        //super.onCreate(savedInstanceState);
        dbHelper = new DBHelper(this);
        mRequestingLocationUpdates = false;
        mLastUpdateTime = "";
        mLastUpdateMS = 0;
        mLastUpdateTimeTZ = "";
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mSettingsClient = LocationServices.getSettingsClient(this);
        createLocationCallback();
        createLocationRequest();
        buildLocationSettingsRequest();
        startLocationUpdatesPS();

    }

    DBHelper dbHelper;
    /**
     * Code used in requesting runtime permissions.
     */
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;
    private static int PERIOD = 18; // число деленое на 6 = минуты
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

    /**
     * Represents a geographical location.
     */
    private Location mCurrentLocation;

    // UI Widgets.

    /**
     * Tracks the status of the location updates request. Value changes when the user presses the
     * Start Updates and Stop Updates buttons.
     */
    private Boolean mRequestingLocationUpdates;

    /**
     * Time when the location was updated represented as a String.
     */
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
            updateUIPS();

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
    private void updateUIPS() {
        updateLocationUIPS();
    }


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

                Date dateNow = new Date();

                //SimpleDateFormat formatForDateNow = new SimpleDateFormat("yyyy.MM.dd' 'HH:mm:ss' 'Z"); //original
                //SimpleDateFormat formatForDateNow = new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ssXXX");
                //SimpleDateFormat formatForDateNow = new SimpleDateFormat("x");

                String ioio;

                if (Build.VERSION.SDK_INT <= 23) {
                      ioio = "ZZZ";
                } else {
                    ioio = "zzz";
                }
                SimpleDateFormat formatForDateNow = new SimpleDateFormat(ioio);


                mLastUpdateTimeTZ = formatForDateNow.format(dateNow);

                final SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                f.setTimeZone(TimeZone.getTimeZone("UTC"));
                Log.e(TAG,"time UTC + localtime = " + f.format(new Date()) + "---" + mLastUpdateTimeTZ );

                Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(mLastUpdateTimeTZ));
                mLastUpdateMS = cal.getTimeInMillis();


                mLastUpdateTime = f.format(new Date()) + mLastUpdateTimeTZ;

                updateLocationUIPS();
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

    //@Override

    /**
     * Sets the value of the UI fields for the location latitude, longitude and last update time.
     */
    public void updateLocationUIPS() {

        Log.e(TAG, "Запуск службы PS!!!");
        //Log.e(TAG, "mCurrentLocation = " + mCurrentLocation.toString());
        if (mCurrentLocation != null) {
            Log.e(TAG, "--------------------------");
            Date currentTime = Calendar.getInstance().getTime();

            Log.d(TAG, mLastUpdateTime + " milesecond= " + currentTime);
            Log.d(TAG, String.format(Locale.ENGLISH, "%f", mCurrentLocation.getLongitude()));
            Log.d(TAG, String.format(Locale.ENGLISH, "%f", mCurrentLocation.getLatitude()));

            Log.e(TAG, "--------------------------");
            ContentValues cv = new ContentValues();
            cv.put("datetime",  mLastUpdateMS );
            cv.put("longitude", mCurrentLocation.getLongitude());
            cv.put("latitude",  mCurrentLocation.getLatitude());
            cv.put("batLevel", getBatteryLevel());
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo info = wifiManager.getConnectionInfo();
            String ssid = info.getSSID();
            String result = ssid.replaceAll("\"", "");
            Log.e(TAG, "WI-FI no edit: " + result);
            if (result == "<unknown ssid>") {
                //result = "00" + mLastUpdateTimeTZ;
                Log.e(TAG, "WI-FI: " + result);
            }
            int ooo;
            ooo = result.length();
            if (ooo >= 10 ){

                result =  ooo + result + mLastUpdateTimeTZ;

            } else {
                result =  "0" + ooo + result + mLastUpdateTimeTZ;

            }

            Log.e(TAG, "dlinna= " + ooo);






            cv.put("wifissd", result );


            SQLiteDatabase db = dbHelper.getWritableDatabase();
            // SQLiteDatabase db = dbHelper.getWritableDatabase();
            //SQLiteDatabase db = dbHelper();
            String deviceID = Build.SERIAL;
            Log.e(TAG, "device_ID2  = " + deviceID);

            long rowID = db.insert("gps", null, cv);


            cv.put("idphone", getTelegaIdFromBase());
            //Log.e(TAG, "ид записался 333 ???" + cv );

            //Log.e(TAG, "типа записалось в базу !!!!" + db );
            //Log.d(TAG, "row inserted, ID = " + rowID + cv );
            Log.e(TAG, "Battery  = " + getBatteryLevel());

            PERIOD--;
            Log.e(TAG, "period  = " + PERIOD);
            if (PERIOD == 0) {
                Log.e(TAG, " Прошло 3 минуты !!!!");
                PERIOD = 18;
                //startService(new Intent(MainActivity.this, MyService.class));
                //startService(new Intent(String.valueOf(PlayService.class)));
                //startService(new Intent(String.valueOf(MyService.class)));

                Log.e(TAG, "Запущен сервис !!!!!");


            }


            //cv.put("battery", getBatteryLevel());


            //sendDataToServer(cv);
            sendDataToServer2(cv);


        }
        Log.e(TAG, "Нет данных для отправки PS!!!");

    }


    public int getTelegaIdFromBase() {
        int tttt67 = 0;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor c = db.query("phone", null, null, null, null, null, null);
        
        if (c.moveToFirst()) {
            int idColIndex = c.getColumnIndex("id");
            int nameColIndex = c.getColumnIndex("TelegaID");
            Log.e(TAG, "telegaid from database = " + c.getString(nameColIndex));
            tttt67 = Integer.parseInt(c.getString(nameColIndex));
            //tttt67 = (c.getString(nameColIndex));
            Log.e(TAG, "telegaid int  = " + tttt67);
                    
        }

    return tttt67;

    }

    public String getPointServerFromBase() {
        String tttt67 = "";
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor c = db.query("phone", null, null, null, null, null, null);

        if (c.moveToFirst()) {
            int idColIndex = c.getColumnIndex("id");
            int nameColIndex = c.getColumnIndex("POINTSERVER");
            Log.e(TAG, "pointserver from database = " + c.getString(nameColIndex));
            tttt67 = c.getString(nameColIndex) ;

        }

        return tttt67;

    }




    public String getHashFromBase() {
        String tttt67 = "";
        String rrr = "";
        String uuuu = "";
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor c = db.query("phone", null, null, null, null, null, null);

        if (c.moveToFirst()) {
            int idColIndex = c.getColumnIndex("id");
            int nameColIndex = c.getColumnIndex("GetANDID");
            Log.e(TAG, "HASH from database = " + c.getString(nameColIndex));
            tttt67 = c.getString(nameColIndex) ;

            //String androidIdtest = "" + Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

            //UUID deviceUuid = new UUID(androidId.hashCode(), ((long)tmDevice.hashCode() << 32) | tmSerial.hashCode());
            //cvms.put("GetDevId2",deviceUuid.toString() );

            rrr = UUID.randomUUID().toString();
            uuuu = rrr.substring(0, 6);
            Log.e(TAG,"GetnewANDID= " + tttt67  + " - " + uuuu + " - " + rrr);

        }

        return tttt67;

    }




    public int getBatteryLevel() {
        Intent batteryIntent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        float rrrrr;
        Log.i("STATUS level = ", String.valueOf(level));
        Log.i("STATUS scale = ", String.valueOf(scale));
        // Error checking that probably isn't needed but I added just in case.
        if (level == -1 || scale == -1) {
            return 50;
        }
        rrrrr = ((float) level / (float) scale) * 100.0f;
        level = (int) (rrrrr);
        if (level >= 100 ) {
            Log.i("Заряд 3 символа !!! ", String.valueOf(level));

        } else {
            Log.i("Заряд 2 символа !!! ", String.valueOf(level));

        }

        return level;


    }




    public void sendDataToServer2(final ContentValues cv) {

        //Log.i("STATUS", "Start send ty http://91.144.151.82:50080 ");


        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String urlAdress = getPointServerFromBase();
                    //String urlAdress = "http://kidslocator.ru:4443";
                    //String urlAdress = "http://10.10.10.50:4443";


                    URL url = new URL(urlAdress);
                    HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

                    if (Build.VERSION.SDK_INT <= 23) {
                        TLSSocketFactory socketFactory = new TLSSocketFactory();
                        conn.setSSLSocketFactory(socketFactory);
                    }
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                    conn.setRequestProperty("Accept", "application/json");
                    conn.setDoOutput(true);
                    conn.setDoInput(true);

                    Log.i("STATUS", "Start send - PS2 !!!");
                    JSONObject jsonParam = new JSONObject();
                    jsonParam.put("idphone", cv.get("idphone"));

                    jsonParam.put("datetime", mLastUpdateMS);

                    //jsonParam.put("datetime", new Date(System.currentTimeMillis()));
                    //jsonParam.put("datetimetz", mLastUpdateTimeTZ);
                    jsonParam.put("longitude", cv.get("longitude"));
                    jsonParam.put("latitude", cv.get("latitude"));
                    jsonParam.put("batLevel", cv.get("batLevel"));
                    jsonParam.put("wifissd", cv.get("wifissd"));
                    jsonParam.put("android", getHashFromBase());


                    Log.i("JSON", jsonParam.toString());
                    DataOutputStream os = new DataOutputStream(conn.getOutputStream());
                    //os.writeBytes(URLEncoder.encode(jsonParam.toString(), "UTF-8"));
                    os.writeBytes(jsonParam.toString());

                    os.flush();
                    os.close();

                    Log.i("STATUS", String.valueOf(conn.getResponseCode()));
                    Log.i("MSG", conn.getResponseMessage());

                    conn.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();


    }

    public static class TLSSocketFactory extends SSLSocketFactory {

        private SSLSocketFactory internalSSLSocketFactory;

        public TLSSocketFactory() throws KeyManagementException, NoSuchAlgorithmException {
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, null, null);
            internalSSLSocketFactory = context.getSocketFactory();
        }

        @Override
        public String[] getDefaultCipherSuites() {
            return internalSSLSocketFactory.getDefaultCipherSuites();
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return internalSSLSocketFactory.getSupportedCipherSuites();
        }

        @Override
        public Socket createSocket() throws IOException {
            return enableTLSOnSocket(internalSSLSocketFactory.createSocket());
        }

        @Override
        public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
            return enableTLSOnSocket(internalSSLSocketFactory.createSocket(s, host, port, autoClose));
        }

        @Override
        public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
            return enableTLSOnSocket(internalSSLSocketFactory.createSocket(host, port));
        }

        @Override
        public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException, UnknownHostException {
            return enableTLSOnSocket(internalSSLSocketFactory.createSocket(host, port, localHost, localPort));
        }

        @Override
        public Socket createSocket(InetAddress host, int port) throws IOException {
            return enableTLSOnSocket(internalSSLSocketFactory.createSocket(host, port));
        }

        @Override
        public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
            return enableTLSOnSocket(internalSSLSocketFactory.createSocket(address, port, localAddress, localPort));
        }

        private Socket enableTLSOnSocket(Socket socket) {
            if(socket != null && (socket instanceof SSLSocket)) {
                ((SSLSocket)socket).setEnabledProtocols(new String[] {"TLSv1.1", "TLSv1.2"});
            }
            return socket;
        }
    }



    public void startLocationUpdatesPS() {


        // Begin by checking if the device has the necessary location settings.
        mSettingsClient.checkLocationSettings(mLocationSettingsRequest);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
        updateUIPS();

    }



    /**
     * Removes location updates from the FusedLocationApi.
     */

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
     //   /**
        String input = intent.getStringExtra("inputExtra");
        createNotificationChannel();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Kids Locator Service")
                .setContentText(input)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1, notification);
        //initiateBackgroundWork(intent, flags)

     //   */

       // Toast.makeText(this, "Служба запущена",
       //         Toast.LENGTH_SHORT).show();
       // mPlayer.start();
        updateLocationUIPS();

        return super.onStartCommand(intent, flags, startId);
        //return Service.START_STICKY();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(this, "Служба остановлена",
                Toast.LENGTH_SHORT).show();
        
    }

   // /**
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }
    //  */
}