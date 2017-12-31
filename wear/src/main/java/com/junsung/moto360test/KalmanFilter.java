package com.junsung.moto360test;

/**
 * Created by Junsung on 2016. 12. 1..
 */

public class KalmanFilter {
    private double X = 0, P = 1, K;

    KalmanFilter(double initValue) {
        X = initValue;
    }

    private void measurementUpdate(){
        final double Q = 0.00001;
        final double R = 0.001;
        K = (P + Q) / (P + Q + R);
        P = R * (P + Q) / (R + P + Q);
    }

    public double update(double measurement){
        measurementUpdate();
        X = X + (measurement - X) * K;

        return X;
    }
}
