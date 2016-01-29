package com.example.livemap.accelerometertest;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import java.text.SimpleDateFormat;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Date;
import java.util.logging.Handler;

public class MainActivity extends Activity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;

    private float deltaXMax = 0;
    private float deltaYMax = 0;
    private float deltaZMax = 0;

    private float accelerationX = 0;
    private float accelerationY = 0;
    private float accelerationZ = 0;

    private long tLastChanged = 0;
    private long elapsedTime = 0;
    private String state;
    private String start_recording_time;
    private Timer checkAcceleration;
    private float jostle_index;

    private final double NOISE_THRESHOLD = 0.5;
    private final float ELAPSED_TIME_THRESHOLD = 3000;//3s
    private final long CHECK_RATE = 200; //0.2s
    private final String TIME_FORMAT = "yyyy-MM-dd HH:mm:ss.SS";


    private TextView currentX, currentY, currentZ, stateTextView, maxX, maxY, maxZ,
            currentTime, currentState, currentIs_moving, currentJostle, currentX1, currentY1, currentZ1;

    private boolean isRecording = false;
    private boolean started_recording = isRecording;

    private boolean isMoving = false;
    private boolean old_isMoving = isMoving;

    private String LOG = "MainActivity-log";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        final DatabaseWrapper dbw = new DatabaseWrapper(this);
        final VectorComputation vc = new VectorComputation();
        dbw.getWritableDatabase();

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
                elapsedTime = System.currentTimeMillis() - tLastChanged;
                if (elapsedTime > ELAPSED_TIME_THRESHOLD) {
                    state = "Constant  speed";
                } else {
                    state = "Accelerate";
                }
                if(isRecording){
                    SimpleDateFormat sdf = new SimpleDateFormat(TIME_FORMAT);
                    //Log.d(LOG, ""+ sdf.format(new Date()) + " " + state + " "+ isStopped+ " "+ accelerationX + " "+ accelerationY + " "+ accelerationZ);
                    String dt = sdf.format(new Date());

                    if (!started_recording)
                        start_recording_time = dt;

                    vc.addVector(new Vector3D(accelerationX, accelerationY, accelerationZ));
                    jostle_index = vc.getJostleIndex();
//                    Log.d(LOG + " jostle index 1:", Float.toString(jostle_index));
                    //create entry and add to local database
                    DBEntry entry = new DBEntry(sdf.format(new Date()),state, isMoving, accelerationX,accelerationY,accelerationZ,jostle_index);
                    dbw.addDBEntry(entry);
//                    Log.d(LOG+ " jostle index 2:", Float.toString(vc.getJostleIndex()));
                }
                started_recording = true;
            }
        }, 0, CHECK_RATE);

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
        busMB.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (isMoving) {
                    isMoving = true;
                    busMB.setText("Bus Is Moving");
                } else {
                    isMoving = false;
                    busMB.setText("Bus Is Stopped");
                }
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
        currentJostle.setText(String.format("%.2f",jostle_index));
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

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        boolean changed = false;
        // clean current values
//        displayCleanValues();
        // display the current x,y,z accelerometer values

        // get the change of the x,y,z values of the accelerometer
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
        if(Math.abs(accelerationZ) < NOISE_THRESHOLD)
            accelerationZ = 0;
        else
            changed = true;
        if(changed)
            tLastChanged = System.currentTimeMillis();

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

}
