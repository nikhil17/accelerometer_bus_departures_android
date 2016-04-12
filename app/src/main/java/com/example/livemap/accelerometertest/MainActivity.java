package com.example.livemap.accelerometertest;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;


import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements SensorEventListener,
        LocationListener, ConnectionCallbacks, OnConnectionFailedListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;

    private float oldX = 0;
    private float oldY = 0;
    private float oldZ = 0;

    private float accelerationX = 0;
    private float accelerationY = 0;
    private float accelerationZ = 0;

    private long tLastChanged = 0;
    private long elapsedTime = 0;
    private String state = "Constant speed";
    private String start_recording_time;
    private Timer checkAcceleration;
    private float jostle_index;

    private double NOISE_THRESHOLD = 0.3;
    private final float ELAPSED_TIME_THRESHOLD = 3000;//3s
    private int sample_rate = 250; //0.3s
    private final String TIME_FORMAT = "yyyy-MM-dd HH:mm:ss.SS";

    private float JOSTLE_INDEX_UPPER_BOUND = 20.0f;
    private float JOSTLE_INDEX_LOWER_BOUND = 2.5f;


    private TextView currentX, currentY, currentZ, stateTextView, maxX, maxY, maxZ,
            currentTime, currentState, currentIs_moving, currentJostle, currentX1, currentY1, currentZ1;

    private boolean isRecording = false;
    private boolean started_recording = isRecording;

    private boolean isDeparting = false;
    private boolean old_isDeparting = isDeparting;

    private String LOG = "MainActivity-log";
    VectorComputation vc;
    protected static final String TAG = "location-updates-sample";

    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 1000;

    /**
     * The fastest rate for active location updates. Exact. Updates will never be more frequent
     * than this value.
     */
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    // Keys for storing activity state in the Bundle.
    protected final static String REQUESTING_LOCATION_UPDATES_KEY = "requesting-location-updates-key";
    protected final static String LOCATION_KEY = "location-key";
    protected final static String LAST_UPDATED_TIME_STRING_KEY = "last-updated-time-string-key";

    /**
     * Provides the entry point to Google Play services.
     */
    protected GoogleApiClient mGoogleApiClient;

    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    protected LocationRequest mLocationRequest;

    /**
     * Represents a geographical location.
     */
    protected Location mCurrentLocation;

    // UI Widgets.
    protected Button mStartUpdatesButton;
    protected Button mStopUpdatesButton;


    private double latitude;
    private double longitude;

    // Labels.
    protected String mLatitudeLabel;
    protected String mLongitudeLabel;
    protected String mLastUpdateTimeLabel;

    /**
     * Tracks the status of the location updates request. Value changes when the user presses the
     * Start Updates and Stop Updates buttons.
     */
    protected Boolean mRequestingLocationUpdates;

    /**
     * Time when the location was updated represented as a String.
     */
    protected String mLastUpdateTime;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        final DatabaseWrapper dbw = new DatabaseWrapper(this);
        vc = new VectorComputation();
        dbw.getWritableDatabase();

        mRequestingLocationUpdates = true;
        mLastUpdateTime = "";

        // Update values using data stored in the Bundle.
        updateValuesFromBundle(savedInstanceState);

        // Kick off the process of building a GoogleApiClient and requesting the LocationServices
        // API.
        buildGoogleApiClient();

        //Motion sensor
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            // success! we have an accelerometer
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            // fail
        }



        //Query at every ELAPSED_TIME_THRESHOLD ms
        state = "Stop";
        checkAcceleration = new Timer();
        checkAcceleration.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        initializeViews();
                    }
                });

                vc.addVector(new Vector3D(accelerationX, accelerationY, accelerationZ));
                jostle_index = vc.getJostleIndex();
                if (isRecording) {
                    SimpleDateFormat sdf = new SimpleDateFormat(TIME_FORMAT);
                    //Log.d(LOG, ""+ sdf.format(new Date()) + " " + state + " "+ isStopped+ " "+ accelerationX + " "+ accelerationY + " "+ accelerationZ);
                    String dt = sdf.format(new Date());

                    if (!started_recording)
                        start_recording_time = dt;


//                    Log.d(LOG + " jostle index 1:", Float.toString(jostle_index));
                    //create entry and add to local database
                    DBEntry entry = new DBEntry(sdf.format(new Date()), state, isDeparting, sample_rate, NOISE_THRESHOLD, latitude, longitude, accelerationX, accelerationY, accelerationZ, jostle_index);
                    dbw.addDBEntry(entry);
//                    Log.d(LOG+ " jostle index 2:", Float.toString(vc.getJostleIndex()));
                }


                started_recording = true;
            }
        }, 0, sample_rate);

        //Recording button
        final Button startRB = (Button) findViewById(R.id.start_button);
        startRB.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!isRecording) {
                    isRecording = true;

                    startRB.setText("Stop Recording");

                } else {
                    isRecording = false;
                    started_recording = false;
                    startRB.setText("Start Recording");
                    dbw.logRecordings();
//                    vc.printArray();
//                    Log.d(LOG + " jostle index :", Float.toString(vc.getJostleIndex()));
                }

            }
        });

        //Bus state button
        final Button busMB = (Button) findViewById(R.id.busMotion_button);
        busMB.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {

                    isDeparting = true;
                    busMB.setText("Accelerating now");
//                    Log.d(LOG, "isDeparting :" + isDeparting);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
//
                    isDeparting = false;
                    busMB.setText("Not Accelerating now");
//                    Log.d(LOG, "isDeparting :" + isDeparting);
                }
                return true;
            }
        });

        //Clear db button
        final Button clearButton = (Button) findViewById(R.id.clear_button);
        clearButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Clear All")
                        .setMessage("Are you sure you want to clear all data recorded?")
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dbw.clearALL();
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // do nothing
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();

            }
        });


        final Button changeSampleRateButton = (Button) findViewById(R.id.button_sample);
        changeSampleRateButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final EditText input = new EditText(MainActivity.this);
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Change Sample Rate")
                        .setView(input)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    sample_rate = Integer.parseInt(input.getText().toString());
                                } catch (NumberFormatException nf) {
                                    new AlertDialog.Builder(MainActivity.this)
                                            .setTitle("Error")
                                            .setMessage("Bad input passed in")
                                            .setIcon(android.R.drawable.ic_dialog_alert)
                                            .show();
                                }
                                //Log.d(LOG, "sample rate :" + sample_rate);
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();

            }
        });

        final Button changeJIThresholdButton = (Button) findViewById(R.id.button_jiThreshold);
        changeJIThresholdButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final EditText input = new EditText(MainActivity.this);
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Change Jostle Index Threshold");
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                builder.setView(input);
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            JOSTLE_INDEX_LOWER_BOUND = Float.parseFloat(input.getText().toString());
                        } catch (NumberFormatException nf) {
                            new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("Error")
                                    .setMessage("Bad input passed in")
                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                    .show();
                        }
                        Log.d(LOG, "JOSTLE_INDEX_LOWER_BOUND :" + JOSTLE_INDEX_LOWER_BOUND);
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                builder.show();

            }
        });

        final Button changeNoiseThresholdButton = (Button) findViewById(R.id.button_noiseThreshold);
        changeNoiseThresholdButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final EditText input = new EditText(MainActivity.this);
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Change Noise Threshold");
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                builder.setView(input);
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            NOISE_THRESHOLD = Double.parseDouble(input.getText().toString());

                        } catch (NumberFormatException nf) {
                            new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("Error")
                                    .setMessage("Bad input passed in")
                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                    .show();
                        }

                        Log.d(LOG, "NOISE THRESHOLD :" + NOISE_THRESHOLD);
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                builder.show();

            }
        });

    }

    public void initializeViews() {
        currentX = (TextView) findViewById(R.id.currentX);
        currentY = (TextView) findViewById(R.id.currentY);
        currentZ = (TextView) findViewById(R.id.currentZ);
        stateTextView = (TextView) findViewById(R.id.state);
        currentTime = (TextView) findViewById(R.id.value_time);
        currentJostle = (TextView) findViewById(R.id.value_jostle_index);

        currentX.setText(String.format("%.2f", accelerationX));
        currentY.setText(String.format("%.2f", accelerationY));
        currentZ.setText(String.format("%.2f", accelerationZ));
        stateTextView.setText(state);
        currentTime.setText(new Date().toString());
        currentJostle.setText(String.format("%.2f", jostle_index));
    }

    //onResume() register the accelerometer for listening the events
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        if (mGoogleApiClient.isConnected() && mRequestingLocationUpdates) {
            startLocationUpdates();
        }

    }

    //onPause() unregister the accelerometer for stop listening the events
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        if (mGoogleApiClient.isConnected()) {
            stopLocationUpdates();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
//        Log.d(LOG,"Accuracy is :"+ accuracy);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        boolean changed = false;
        // clean current values
//        displayCleanValues();
        // display the current x,y,z accelerometer values

        // get the change of the x,y,z values of the accelerometer


        oldX = accelerationX;
        oldY = accelerationY;
        oldZ = accelerationZ;

        accelerationX = event.values[0];
        accelerationY = event.values[1];
        accelerationZ = event.values[2];

        // if the change is below 2, it is just plain noise
        if (Math.abs(accelerationX) < NOISE_THRESHOLD)
            accelerationX = 0;
        else
            changed = true;
        if (Math.abs(accelerationY) < NOISE_THRESHOLD)
            accelerationY = 0;
        else
            changed = true;
        if (Math.abs(accelerationZ) < NOISE_THRESHOLD)
            accelerationZ = 0;
        else
            changed = false;

//        if (accelerationX == 0 && accelerationY == 0 && accelerationZ == 0){
//            // change this to accelerating from
//            state = "Constant acceleration";
//        }

        if (accelerationX == 0 && accelerationY == 0 && accelerationZ == 0) {
            state = "Constant acceleration";
        } else if (vc.containsZeroVector()) {
//            Log.d("Contains zero vector :", "" + vc.containsZeroVector());
//            Log.d(LOG,"current jostle index :" + jostle_index);
            if (jostle_index > JOSTLE_INDEX_LOWER_BOUND && jostle_index < JOSTLE_INDEX_UPPER_BOUND) {
                state = "Departing";
                Log.d(LOG, "ACCELERATING FROM START RIGHT NOW!!");
//                System.out.println("YOOOOO");
            }
        } else {
            state = "Randomly Accelerating";
        }

    }

    public void displayCleanValues() {
        currentX.setText("0.0");
        currentY.setText("0.0");
        currentZ.setText("0.0");
    }

    // display the current x,y,z accelerometer values
    public void displayCurrentValues() {
        currentX.setText(String.format("%.2f", accelerationX));
        currentY.setText(String.format("%.2f", accelerationY));
        currentZ.setText(String.format("%.2f", accelerationZ));
        stateTextView.setText(state);
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
    public void startUpdatesButtonHandler(View view) {
        if (!mRequestingLocationUpdates) {
            mRequestingLocationUpdates = true;
            setButtonsEnabledState();
            startLocationUpdates();
        }
    }

    /**
     * Handles the Stop Updates button, and requests removal of location updates. Does nothing if
     * updates were not previously requested.
     */
    public void stopUpdatesButtonHandler(View view) {
        if (mRequestingLocationUpdates) {
            mRequestingLocationUpdates = false;
            setButtonsEnabledState();
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
        if (mRequestingLocationUpdates) {
            mStartUpdatesButton.setEnabled(false);
            mStopUpdatesButton.setEnabled(true);
        } else {
            mStartUpdatesButton.setEnabled(true);
            mStopUpdatesButton.setEnabled(false);
        }
    }

    /**
     * Updates the latitude, the longitude, and the last location time in the UI.
     */
    private void updateUI() {
        latitude = mCurrentLocation.getLatitude();
        longitude = mCurrentLocation.getLongitude();
        Log.d(LOG,"updated location:- ");
        Log.d("latitude :", ""+ mCurrentLocation.getLatitude());
        Log.d("longitude :", ""+ mCurrentLocation.getLongitude());
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

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }





    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();

        super.onStop();
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
        Toast.makeText(this, getResources().getString(R.string.location_updated_message),
                Toast.LENGTH_SHORT).show();
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


    /**
     * Stores activity data in the Bundle.
     */
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY, mRequestingLocationUpdates);
        savedInstanceState.putParcelable(LOCATION_KEY, mCurrentLocation);
        savedInstanceState.putString(LAST_UPDATED_TIME_STRING_KEY, mLastUpdateTime);
        super.onSaveInstanceState(savedInstanceState);
    }

}
