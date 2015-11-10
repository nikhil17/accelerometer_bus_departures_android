package com.example.livemap.accelerometertest;

/**
 * Created by nikhil on 11/5/15.
 */
public class DBEntry {

    private String time;
    private String state;
    private boolean isMoving;
    private float X_acceleration;
    private float Y_acceleration;
    private float Z_acceleration;

    public DBEntry(String time, String state, boolean isMoving, float X_acceleration, float Y_acceleration, float Z_acceleration){
        this.time = time;
        this.state = state;
        this.isMoving = isMoving;
        this.X_acceleration = X_acceleration;
        this.Y_acceleration = Y_acceleration;
        this.Z_acceleration = Z_acceleration;

    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public boolean isMoving() {
        return isMoving;
    }

    public void setIsMoving(boolean isMoving) {
        this.isMoving = isMoving;
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



//            (Time TEXT primary key not null, State Text not null, isMoving INT not null," +
//                    " X-Acceleration INT not null, Y-Acceleration INT not null, Z-Acceleration INT not null);";


}
