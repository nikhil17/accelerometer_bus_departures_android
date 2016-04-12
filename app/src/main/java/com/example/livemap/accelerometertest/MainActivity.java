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
import android.location.LocationListener;
import android.location.LocationManager;
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

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;

import java.text.SimpleDateFormat;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements SensorEventListener, ConnectionCallbacks, OnConnectionFailedListener {

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

    private double NOISE_THRESHOLD = 0.5;
    private final float ELAPSED_TIME_THRESHOLD = 3000;//3s
    private int sample_rate = 250; //0.3s
    private final String TIME_FORMAT = "yyyy-MM-dd HH:mm:ss.SS";

    private float JOSTLE_INDEX_UPPER_BOUND = 20.0f;
    private float JOSTLE_INDEX_LOWER_BOUND = 3.0f;


    private TextView currentX, currentY, currentZ, stateTextView, maxX, maxY, maxZ,
            currentTime, currentState, currentIs_moving, currentJostle, currentX1, currentY1, currentZ1;

    private boolean isRecording = false;
    private boolean started_recording = isRecording;

    private boolean isDeparting = false;
    private boolean old_isDeparting = isDeparting;

    private String LOG = "MainActivity-log";
    VectorComputation vc;
    private Location location;
    protected Location mLastLocation;


    protected GoogleApiClient mGoogleApiClient;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        final DatabaseWrapper dbw = new DatabaseWrapper(this);
        vc = new VectorComputation();
        dbw.getWritableDatabase();

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


        // Acquire a reference to the system Location Manager
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // Define a listener that responds to location updates
        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider.
//                makeUseOfNewLocation(location);
                Log.d("latitude", "" + location.getLatitude());
                Log.d("longitude", "" + location.getLongitude());

            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
            }
        };

        Context context = this;
        PackageManager pm = context.getPackageManager();
        int hasPerm = pm.checkPermission(
                Manifest.permission.ACCESS_FINE_LOCATION,
                context.getPackageName());
        if (hasPerm != PackageManager.PERMISSION_GRANTED) {
            // do stuff
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        }

        // Register the listener with the Location Manager to receive location updates


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
                    DBEntry entry = new DBEntry(sdf.format(new Date()), state, isDeparting, sample_rate, NOISE_THRESHOLD, accelerationX, accelerationY, accelerationZ, jostle_index);
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
    }

    //onPause() unregister the accelerometer for stop listening the events
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
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

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    /**
     * Runs when a GoogleApiClient object successfully connects.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        // Provides a simple way of getting a device's location and is well suited for
        // applications that do not require a fine-grained location and that do not need location
        // updates. Gets the best and most recent location currently available, which may be null
        // in rare cases when a location is not available.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    public void requestPermissions(@NonNull String[] permissions, int requestCode)
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for Activity#requestPermissions for more details.
                return;
            }
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            Log.d("longitude -", ""+mLastLocation.getLongitude());
            Log.d("longitude -", "" + mLastLocation.getLatitude());

//
//            mLatitudeText.setText(String.format("%s: %f", mLatitudeLabel,
//                    mLastLocation.getLatitude()));
//            mLongitudeText.setText(String.format("%s: %f", mLongitudeLabel,
//                    mLastLocation.getLongitude()));
        } else {
            Log.d(LOG, "No location detected");
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Log.i(LOG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }


    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i(LOG, "Connection suspended");
        mGoogleApiClient.connect();
    }

}
