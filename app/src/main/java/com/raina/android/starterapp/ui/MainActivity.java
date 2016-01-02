package com.raina.android.starterapp.ui;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.alertdialogpro.AlertDialogPro;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.raina.android.starterapp.Model.Data;
import com.raina.android.starterapp.R;
import com.raina.android.starterapp.persist.MobDBHandler;
import com.raina.android.starterapp.persist.UploadService;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

import au.com.bytecode.opencsv.CSVWriter;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener, DatePickerDialog.OnDateSetListener,
                GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    protected static final String TAG = "MobLocationService";

    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    protected final static String REQUESTING_LOCATION_UPDATES_KEY = "requesting-location-updates-key";
    protected final static String LOCATION_KEY = "location-key";
    protected final static String LAST_UPDATED_TIME_STRING_KEY = "last-updated-time-string-key";

    protected GoogleApiClient mGoogleApiClient;

    protected LocationRequest mLocationRequest;

    protected Location mCurrentLocation;

    protected Boolean mRequestingLocationUpdates;

    protected String mLastUpdateTime;

    private MobDBHandler db;

    private UploadService service;

    TextView dateText;
    DatePickerDialog dpd;
    Button button;
    EditText eName, ePhone, eNumber;
    String option, eDate, name, phone,number;
    String imagePath;
    File file;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkNetworkState();

        db = new MobDBHandler(this);
        db.initializeDB();

        String root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
        File myDir = new File(root + "/starterapp");

        if (!myDir.exists()) {
            myDir.mkdirs();
        }

        service = new UploadService(getApplicationContext());
        mRequestingLocationUpdates = false;
        mLastUpdateTime = "";



        try{
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
            createLocationRequest();
        }catch (Exception e){
            e.printStackTrace();
        }

        Spinner spinner = (Spinner) findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.planets_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);

        Calendar now = Calendar.getInstance();
        dpd = DatePickerDialog.newInstance(
                MainActivity.this,
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH)
        );

         dateText = (TextView) findViewById(R.id.input_date);
        button = (Button) findViewById(R.id.btn_date);
        button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                dpd.show(getFragmentManager(), "Datepickerdialog");
            }
        });

        Button camera = (Button) findViewById(R.id.btn_camera);
        camera.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent, 0);
            }
        });

        Button gps = (Button) findViewById(R.id.btn_gps);
        gps.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                Toast.makeText(getApplicationContext(), "Please wait, fetching location...", Toast.LENGTH_SHORT).show();
                mGoogleApiClient.connect();

                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        startUpdatesButtonHandler();
                    }
                }, 10000);

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        Toast.makeText(getApplicationContext(), "lat :" + mCurrentLocation.getLatitude() + " long : " +
                                mCurrentLocation.getLongitude(), Toast.LENGTH_SHORT).show();
                    }
                }, 10000);
            }
        });


        Button save = (Button) findViewById(R.id.btn_save);
        save.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                eName = (EditText)findViewById(R.id.input_name);
                ePhone = (EditText)findViewById(R.id.input_phone);
                eNumber = (EditText)findViewById(R.id.input_number);



                name = eName.getText().toString();
                phone = ePhone.getText().toString();
                number = eNumber.getText().toString();

                service.save(name, phone, eDate, option, number, imagePath,
                        Double.toString(mCurrentLocation.getLatitude()),
                        Double.toString(mCurrentLocation.getLongitude()), true);

                Toast.makeText(getApplicationContext(), "Saved to DB", Toast.LENGTH_SHORT).show();
            }
        });

        Button upload = (Button) findViewById(R.id.btn_upload);
        upload.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                new LongOperation().execute("");
            }
        });

    }

    private AlertDialog.Builder createAlertDialogBuilder() {

        return new AlertDialogPro.Builder(this, R.style.Theme_AlertDialogPro_Material_Light);
    }

    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id) {
        option = parent.getItemAtPosition(pos).toString();
    }

    public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
    }


    @Override
    public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
        String date = "Date: "+dayOfMonth+"/"+(monthOfYear+1)+"/"+year;
        eDate = dayOfMonth+"/"+(monthOfYear+1)+"/"+year;
        dateText.setText(date);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if(data != null) {
            Bitmap bp = (Bitmap) data.getExtras().get("data");
            saveImage(bp);
        }
    }


    private void saveImage(Bitmap finalBitmap) {
        String root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
        File myDir = new File(root + "/starterapp");

        if (!myDir.exists()) {
            myDir.mkdirs();
        }

        Random generator = new Random();
        int n = 10000;
        n = generator.nextInt(n);
        String iname = "Image-" + n + ".jpg";
        File file = new File(myDir, iname);
        if (file.exists())
            file.delete();
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Tell the media scanner about the new file so that it is
        // immediately available to the user.
        MediaScannerConnection.scanFile(this, new String[]{file.toString()}, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        Log.i("ExternalStorage", "Scanned " + path + ":");
                        Log.i("ExternalStorage", "-> uri=" + uri);
                    }
                });


        imagePath = Environment.getExternalStorageDirectory() + "/Pictures/starterapp/" + iname;

        Toast.makeText(getApplicationContext(), imagePath, Toast.LENGTH_SHORT).show();

    }



    /**
     * Updates fields based on data stored in the bundle.
     *
     * @param savedInstanceState The activity state saved in the Bundle.
     */
    private void updateValuesFromBundle(Bundle savedInstanceState) {
        Log.i(TAG, "Updating values from bundle");
        if (savedInstanceState != null) {
            // Update the value of mRequestingLocationUpdates from the Bundle, and make sure that
            // the Start Updates and Stop Updates buttons are correctly enabled or disabled.
            if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean(
                        REQUESTING_LOCATION_UPDATES_KEY);
                setButtonsEnabledState();
            }

            // Update the value of mCurrentLocation from the Bundle and update the UI to show the
            // correct latitude and longitude.
            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
                // Since LOCATION_KEY was found in the Bundle, we can be sure that mCurrentLocation
                // is not null.
                mCurrentLocation = savedInstanceState.getParcelable(LOCATION_KEY);
            }

            // Update the value of mLastUpdateTime from the Bundle and update the UI.
            if (savedInstanceState.keySet().contains(LAST_UPDATED_TIME_STRING_KEY)) {
                mLastUpdateTime = savedInstanceState.getString(LAST_UPDATED_TIME_STRING_KEY);
            }
            updateUI();
        }
    }

    /**
     * Builds a GoogleApiClient. Uses the {@code #addApi} method to request the
     * LocationServices API.
     */
    protected synchronized void buildGoogleApiClient() {
        Log.i(TAG, "Building GoogleApiClient");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        createLocationRequest();
        startUpdatesButtonHandler();
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
    protected void createLocationRequest() {
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
     * Handles the Start Updates button and requests start of location updates. Does nothing if
     * updates have already been requested.
     */
    public void startUpdatesButtonHandler() {
        if (!mRequestingLocationUpdates) {
            mRequestingLocationUpdates = true;
            //setButtonsEnabledState();
            startLocationUpdates();
        }
    }

    /**
     * Handles the Stop Updates button, and requests removal of location updates. Does nothing if
     * updates were not previously requested.
     */
    public void stopUpdatesButtonHandler() {
        if (mRequestingLocationUpdates) {
            mRequestingLocationUpdates = false;
            //setButtonsEnabledState();
            stopLocationUpdates();
        }
    }

    /**
     * Requests location updates from the FusedLocationApi.
     */
    protected void startLocationUpdates() {
        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    /**
     * Ensures that only one button is enabled at any time. The Start Updates button is enabled
     * if the user is not requesting location updates. The Stop Updates button is enabled if the
     * user is requesting location updates.
     */
    private void setButtonsEnabledState() {
//        if (mRequestingLocationUpdates) {
//            mStartUpdatesButton.setEnabled(false);
//            mStopUpdatesButton.setEnabled(true);
//        } else {
//            mStartUpdatesButton.setEnabled(true);
//            mStopUpdatesButton.setEnabled(false);
//        }
    }

    /**
     * Updates the latitude, the longitude, and the last location time in the UI.
     */
    private void updateUI() {

        Log.d(TAG,"lat : "+ mCurrentLocation.getLatitude());
        Log.d(TAG,"lon : "+ mCurrentLocation.getLongitude());
        Log.d(TAG, "timenow : " + mLastUpdateTime);

    }

    /**
     * Removes location updates from the FusedLocationApi.
     */
    protected void stopLocationUpdates() {
        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.

        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    /**
     * Runs when a GoogleApiClient object successfully connects.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "Connected to GoogleApiClient");

        // If the initial location was never previously requested, we use
        // FusedLocationApi.getLastLocation() to get it. If it was previously requested, we store
        // its value in the Bundle and check for it in onCreate(). We
        // do not request it again unless the user specifically requests location updates by pressing
        // the Start Updates button.
        //
        // Because we cache the value of the initial location in the Bundle, it means that if the
        // user launches the activity,
        // moves to a new location, and then changes the device orientation, the original location
        // is displayed as the activity is re-created.



        if (mCurrentLocation == null) {
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
            updateUI();
        }

        // If the user presses the Start Updates button before GoogleApiClient connects, we set
        // mRequestingLocationUpdates to true (see startUpdatesButtonHandler()). Here, we check
        // the value of mRequestingLocationUpdates and if it is true, we start location updates.
        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    /**
     * Callback that fires when the location changes.
     */
    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        updateUI();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopUpdatesButtonHandler();
        mGoogleApiClient.disconnect();
    }


    private class LongOperation extends AsyncTask<String, Void, String> {
        private final ProgressDialog dialog = new ProgressDialog(MainActivity.this);
        @Override
        protected String doInBackground(String... params) {
            File dbFile=getDatabasePath("StarterApp.db");
            MobDBHandler dbhelper = new MobDBHandler(getApplicationContext());
            File exportDir = new File(Environment.getExternalStorageDirectory(), "");
            if (!exportDir.exists())
            {
                exportDir.mkdirs();
            }

            file = new File(exportDir, "starterCSV.csv");
            try
            {
                file.createNewFile();
                CSVWriter csvWrite = new CSVWriter(new FileWriter(file));
                SQLiteDatabase db = dbhelper.getReadableDatabase();
                Cursor curCSV = db.rawQuery("SELECT * FROM info",null);
                csvWrite.writeNext(curCSV.getColumnNames());
                while(curCSV.moveToNext())
                {
                    //Which column you want to exprort
                    String arrStr[] ={curCSV.getString(0),curCSV.getString(1), curCSV.getString(2),
                            curCSV.getString(3),curCSV.getString(4),curCSV.getString(5),curCSV.getString(6),
                            curCSV.getString(7),curCSV.getString(8)};
                    csvWrite.writeNext(arrStr);
                }
                csvWrite.close();
                curCSV.close();
            }
            catch(Exception sqlEx)
            {
                Log.e("MainActivity", sqlEx.getMessage(), sqlEx);
            }
            return "Executed";
        }

        @Override
        protected void onPostExecute(String result) {
            if (this.dialog.isShowing()){
                this.dialog.dismiss();
            }

                Toast.makeText(MainActivity.this, "Export done!", Toast.LENGTH_SHORT).show();

            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.setType("text/plain");
            //emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] {"renga99@gmail.com"});
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "starter app data");
            emailIntent.putExtra(Intent.EXTRA_TEXT, "starter app csv file");
            File root = Environment.getExternalStorageDirectory();
            String pathToMyAttachedFile = file.getAbsolutePath();

            File file = new File(pathToMyAttachedFile);
            if (!file.exists()) {
                Log.d("absolut","absolutepath : "+file.getAbsolutePath());
                return;
            }
            Uri uri = Uri.fromFile(file);
            emailIntent.putExtra(Intent.EXTRA_STREAM, uri);
            startActivity(Intent.createChooser(emailIntent, "Pick an Email provider"));
        }

        @Override
        protected void onPreExecute() {
            this.dialog.setMessage("Exporting database...");
            this.dialog.show();
        }

        @Override
        protected void onProgressUpdate(Void... values) {}
    }


    public void checkNetworkState(){

        Context context = getApplicationContext();
        LocationManager lm = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        boolean network_enabled = false;

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch(Exception ex) {}

        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch(Exception ex) {}

        if(!gps_enabled && !network_enabled) {
            // notify user


            final String[] list = new String[]{"Open location settings"};
            createAlertDialogBuilder()
                    .setIcon(R.drawable.location).
                    setTitle("Please turn on location").
                    setNeutralButton("CANCEL", null)
                    .setItems(list, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            if (which == 0) {
                                Intent myIntent = new Intent( Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                startActivity(myIntent);
                            }
                        }
                    }).show();


        }
    }




}
