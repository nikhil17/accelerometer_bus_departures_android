package com.example.livemap.accelerometertest;

import android.util.Log;

/**
 * Created by nikhil on 11/5/15.
 */
public class DBEntry {

    private String time;
    private String state;
    private boolean isDeparting;
    private int sampleRate;
    private double noiseThreshold;
    private float X_acceleration;
    private float Y_acceleration;
    private float Z_acceleration;
    private float jostle_index;



    private double longitude;
    private double latitude;

    public DBEntry(String time, String state, boolean isDeparting, int sampleRate, double noiseThreshold, double latitude, double longitude, float X_acceleration, float Y_acceleration, float Z_acceleration, float jostle_index){
        this.time = time;
        this.state = state;
        this.isDeparting = isDeparting;
        this.sampleRate = sampleRate;
        this.noiseThreshold = noiseThreshold;
        this.latitude = latitude;
        this.longitude = longitude;
        this.X_acceleration = X_acceleration;
        this.Y_acceleration = Y_acceleration;
        this.Z_acceleration = Z_acceleration;
        this.jostle_index = jostle_index;
//        Log.d("Sample rate :", "" + sampleRate);
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public boolean isDeparting() {
        return isDeparting;
    }

    public void setisDeparting(boolean isDeparting) {
        this.isDeparting = isDeparting;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getLatitude() {
        return latitude;
    }


    public double getNoiseThreshold() {
        return noiseThreshold;
    }
    public float getSampleRate() {
        return sampleRate;
    }

    public float getX_acceleration() {
        return X_acceleration;
    }

    public void setX_acceleration(int x_acceleration) {
        X_acceleration = x_acceleration;
    }

    public float getY_acceleration() {
        return Y_acceleration;
    }

    public void setY_acceleration(int y_acceleration) {
        Y_acceleration = y_acceleration;
    }

    public float getZ_acceleration() {
        return Z_acceleration;
    }

    public void setZ_acceleration(int z_acceleration) {
        Z_acceleration = z_acceleration;
    }

    public float getJostle_index() {
        return jostle_index;
    }



//            (Time TEXT primary key not null, State Text not null, isDeparting INT not null," +
//                    " X-Acceleration INT not null, Y-Acceleration INT not null, Z-Acceleration INT not null);";


}
