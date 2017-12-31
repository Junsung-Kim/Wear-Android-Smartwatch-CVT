package com.junsung.moto360test;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class WearMainActivity extends Activity {

    private TextView mTextView; // 텍스트를 출력할 뷰
    private View mLayout; // 배경을 출력할 레이아웃

    // Sensor capture flag
    private boolean mCaptureFlag = false;

    // sensor data capture interval
    private int mCaptureInterval = 100;

    // for MotionSensor
    private MotionSensor mMotionSensor;
    private SensorManager mSensorManager;
    private int mSensorType;
    private String mOption = null;

    // Variable for counting
    public int mCount = 0;

    // Thread handler
    final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

        }
    };

    // bluetooth with pc
    public static Context mContext;
    private final static int DEVICES_DIALOG = 1;
    private final static int ERROR_DIALOG = 2;
    private static ProgressDialog waitDialog;
    private String errorMessage = "";
    private static final String TAG = "BluetoothTask";
    static private BluetoothAdapter bluetoothAdapter;
    static private BluetoothDevice bluetoothDevice = null;
    static private BluetoothSocket bluetoothSocket;
    static private InputStream btIn;
    static private OutputStream btOut;
    public static Activity activity;

    private LogThread mLogThread;

    // radio button
    // RadioButtonSensor
    private RadioButton mRBSAccel;
    private RadioButton mRBSRV;
    private RadioButton mRBSGRV;
    // RadioButtonOption
    private RadioButton mRBONothing;
    private RadioButton mRBOKalman;
    private RadioButton mRBOR2D;

    @Override // Activity
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wear_main);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);
                mLayout = stub.findViewById(R.id.layout);
                mRBSAccel = (RadioButton) stub.findViewById(R.id.wearSensorAccel);
                mRBSRV = (RadioButton) stub.findViewById(R.id.wearSensorRV);
                mRBSGRV = (RadioButton) stub.findViewById(R.id.wearSensorGRV);
                mRBONothing = (RadioButton) stub.findViewById(R.id.wearOptionNothing);
                mRBOKalman = (RadioButton) stub.findViewById(R.id.wearOptionKalman);
                mRBOR2D = (RadioButton) stub.findViewById(R.id.wearOptionR2D);

                // RadioButtonSensor
                mRBSAccel.setOnClickListener(RBOnClickListener);
                mRBSRV.setOnClickListener(RBOnClickListener);
                mRBSGRV.setOnClickListener(RBOnClickListener);

                // RadioButtonOption
                mRBONothing.setOnClickListener(RBOnClickListener);
                mRBOKalman.setOnClickListener(RBOnClickListener);
                mRBOR2D.setOnClickListener(RBOnClickListener);

                // RadioButton init
                mRBSAccel.setChecked(true);
                mRBONothing.setChecked(true);
            }
        });
        viewInit();

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensorType = Sensor.TYPE_ACCELEROMETER;
        mOption = "kalman";
        mMotionSensor = new MotionSensor(mSensorManager, mSensorType, mOption);

        mLogThread = new LogThread();
        mLogThread.start();

        // 액티비티 화면 안꺼지도록
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // bluetooth with PC
        mContext = this;
        activity = this;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            ErrorDialog("This device is not implement Bluetooth.");
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            ErrorDialog("This device is disabled Bluetooth.");
            return;
        }else DeviceDialog();
    }

    // 액티비티가 시작할 때 실행
    @Override // Activity
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMotionSensor.register();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMotionSensor.unregister();
    }

    // 액티비티가 종료될 때 실행
    @Override // Activity
    protected void onStop() {
        // 구글 플레이 서비스 접속 해제
        super.onStop();
    }


    public void applySetting(View view) {
        mMotionSensor.unregister();
        mMotionSensor = null;
        mMotionSensor = new MotionSensor(mSensorManager, mSensorType, mOption);
        mMotionSensor.register();

        // UI 스레드를 실행하여 텍스트 뷰의 값을 수정한다.
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTextView.setText(String.valueOf(mSensorType) +
                " " +
                mOption);
            }
        });
    }

    public void changeCaptureFlag(View view) {
        if (mCaptureFlag) {
            mCaptureFlag = false;
            ((Button)findViewById(R.id.captureBtn)).setText("Start");
        }
        else {
            mCaptureFlag = true;
            ((Button)findViewById(R.id.captureBtn)).setText("Stop");
        }

    }

    public void onExit(View view) {
        moveTaskToBack(true);
        finish();
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    public void sendSensorData(final String msg) {
        if(!MyDialogFragment.sIsNotConnect)
            doSend(msg);
    }

    private class LogThread extends Thread {
        @Override
        public void run() {
            while(true) {
                if(mCaptureFlag) {
                    float[] tmp = new float[3];
                    tmp = mMotionSensor.getResultValues();
                    final String tmpString = String.valueOf(tmp[0]+":::"+tmp[1]+":::"+tmp[2]);
                    sendSensorData(tmpString);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mTextView.setText(tmpString);
                        }
                    });
                }
                try {
                    Thread.sleep(mCaptureInterval);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                mHandler.sendEmptyMessage(0);
            }
        }
    }

    //region about bluetooth
    @Override
    protected void onDestroy() {
        super.onDestroy();
        doClose();

    }
    public void DeviceDialog()
    {
        if (activity.isFinishing()) return;

        FragmentManager fm = WearMainActivity.this.getFragmentManager();
        MyDialogFragment alertDialog = MyDialogFragment.newInstance(DEVICES_DIALOG, "");
        alertDialog.show(fm, "");
    }


    public void ErrorDialog(String text)
    {
        if (activity.isFinishing()) return;

        FragmentManager fm = WearMainActivity.this.getFragmentManager();
        MyDialogFragment alertDialog = MyDialogFragment.newInstance(ERROR_DIALOG, text);
        alertDialog.show(fm, "");
    }


    public void doSetResultText(String text) {
//        editText2.setText(text);
    }

    public static void hideWaitDialog() {
    }

    static public Set<BluetoothDevice> getPairedDevices() {
        return bluetoothAdapter.getBondedDevices();
    }


    public void doConnect(BluetoothDevice device) {
        bluetoothDevice = device;

        //Standard SerialPortService ID
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

        try {

            bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
            bluetoothAdapter.cancelDiscovery();
            new ConnectTask().execute();
        } catch (IOException e) {
            Log.e(TAG, e.toString(), e);
            ErrorDialog(e.toString());
        }
    }


    public void doClose() {
        new CloseTask().execute();
    }


    public void doSend(String data) {
        new SendTask().execute(data);
    }


    private class ConnectTask extends AsyncTask<Void, Void, Object> {
        @Override
        protected void onPreExecute() {

        }

        @Override
        protected Object doInBackground(Void... params) {
            try {
                bluetoothSocket.connect();
                btIn = bluetoothSocket.getInputStream();
                btOut = bluetoothSocket.getOutputStream();
            } catch (Throwable t) {
                Log.e( TAG, "connect? "+ t.getMessage() );
                doClose();
                return t;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Object result) {
            if (result instanceof Throwable) {
                Log.e(TAG,result.toString(),(Throwable)result);
                ErrorDialog(result.toString());
            } else {
                hideWaitDialog();
            }
        }
    }


    private class CloseTask extends AsyncTask<Void, Void, Object> {
        @Override
        protected Object doInBackground(Void... params) {
            try {
                try{btOut.close();}catch(Throwable t){/*ignore*/}
                try{btIn.close();}catch(Throwable t){/*ignore*/}
                bluetoothSocket.close();
            } catch (Throwable t) {
                return t;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Object result) {
            if (result instanceof Throwable) {
                Log.e(TAG,result.toString(),(Throwable)result);
                ErrorDialog(result.toString());
            }
        }
    }


    private class SendTask extends AsyncTask<String, Void, Object> {
        @Override
        protected Object doInBackground(String... params) {
            try {

                btOut.write(params[0].getBytes());
                btOut.flush();

                byte[] buff = new byte[512];
                int len = btIn.read(buff);

                return new String(buff, 0, len);
            } catch (Throwable t) {
                doClose();
                return t;
            }
        }

        @Override
        protected void onPostExecute(Object result) {
            if (result instanceof Exception) {
                Log.e(TAG,result.toString(),(Throwable)result);
                ErrorDialog(result.toString());
            } else {
                doSetResultText(result.toString());
            }
        }
    }
    //endregion

    RadioButton.OnClickListener RBOnClickListener = new RadioButton.OnClickListener() {
        @Override
        public void onClick(View v) {
            String sensorTypeStr = null;

            if(mRBSAccel.isChecked()) {mSensorType = Sensor.TYPE_ACCELEROMETER; sensorTypeStr = "accel";}
            else if(mRBSRV.isChecked()) {mSensorType = Sensor.TYPE_ROTATION_VECTOR; sensorTypeStr = "RV";}
            else if(mRBSGRV.isChecked()) {mSensorType = Sensor.TYPE_GAME_ROTATION_VECTOR; sensorTypeStr = "GRV";}

            if(mRBONothing.isChecked())  {
                mOption = "nothing";
            }
            else if(mRBOKalman.isChecked())  {
                mOption = "kalman";
            }
            else if(mRBOR2D.isChecked()) {
                mOption = "rad2deg";
            }
        }
    };

    private void viewInit() {

    }
}
