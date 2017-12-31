package com.junsung.moto360test;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

/**
 * Created by Junsung on 2016. 12. 1..
 *
 * example
 * (1) use rotation vector sensor, with degree
 * MotionSensor m = new MotionSensor(mSensorManager, Sensor.TYPE_ROTATION_VECTOR, "rad2deg");
 * (2) use rotation vector sensor, with radian
 * MotionSensor m = new MotionSensor(mSensorManager, Sensor.TYPE_ROTATION_VECTOR, "nothing");
 * (3) use accelerometer, with kalman filter
 * MotionSensor m = new MotionSensor(mSensorManager, Sensor.TYPE_ACCELEROMETER, "kalman");
 * (4) use accelerometer, without kalman filter
 * MotionSensor m = new MotionSensor(mSensorManager, Sensor.TYPE_ACCELEROMETER, "nothing");
 */

public class MotionSensor implements SensorEventListener{

    private SensorManager mSensorManager;
    private Sensor mSensor;
    private String option;

    private float[] mResultValues = new float[3];

    // Kalman filter
    private float[] mAxis = new float[3];
    private KalmanFilter[] mKalmanFilter = new KalmanFilter[3];

    // accelerometer low-pass filter
    private float[] mGravityData = new float[3];


    public MotionSensor(SensorManager sensorManager, int sensor_type, String option) {
        mSensorManager = sensorManager;
        mSensor = sensorManager.getDefaultSensor(sensor_type);
        this.option = option;

        for(int i = 0 ; i < 3; i++)
            mKalmanFilter[i] = new KalmanFilter(0.0f);
    }

    /**
     * call in onResume, MainActivity of wear
     */
    public void register() {
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_UI);
    }

    /**
     * call in onPause, MainActivity of wear
     */
    public void unregister() {
        mSensorManager.unregisterListener(this);
    }

    public float[] getResultValues() {
        return mResultValues;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        switch (event.sensor.getType()) {
            case Sensor.TYPE_ROTATION_VECTOR:
            case Sensor.TYPE_GAME_ROTATION_VECTOR:
                float[] rotationMatrix = new float[16];
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
                mResultValues = determineOrientation(rotationMatrix);
                break;
            case Sensor.TYPE_ACCELEROMETER:
                final float alpha = 0.8f;
                for(int i = 0 ; i < 3; i++) {
                    mGravityData[i] = alpha * mGravityData[i] + (1 - alpha) * event.values[i];
                    mResultValues[i] = event.values[i] - mGravityData[i];
                }
                break;
            default:
                Log.d("onSensorChanged", event.sensor.getType() + " is not available");
        }

        switch (option) {
            case "rad2deg":
                for(int i = 0 ; i < 3; i++)
                    mResultValues[i] = (int)Math.toDegrees(mResultValues[i]);
                break;
            case "kalman":
                for(int i = 0 ; i < 3; i++)
                    mResultValues[i] = (float)mKalmanFilter[i].update(mResultValues[i]);
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private float[] determineOrientation(float[] rotationMatrix) {
        float[] orientationValues = new float[3];
        SensorManager.getOrientation(rotationMatrix, orientationValues);

        return orientationValues;
    }
}
