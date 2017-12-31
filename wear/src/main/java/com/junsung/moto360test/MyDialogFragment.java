package com.junsung.moto360test;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.os.Bundle;

import java.util.Set;

public class MyDialogFragment extends DialogFragment {
    private final static int DEVICES_DIALOG = 1;
    private final static int ERROR_DIALOG = 2;

    public static boolean sIsNotConnect = false;



    public MyDialogFragment() {

    }

    public static MyDialogFragment newInstance(int id, String text) {
        MyDialogFragment frag = new MyDialogFragment();
        Bundle args = new Bundle();
        args.putString("content", text);
        args.putInt("id", id);

        frag.setArguments(args);
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String content = getArguments().getString("content");
        int id = getArguments().getInt("id");
        AlertDialog.Builder alertDialogBuilder = null;

        switch(id)
        {
            case DEVICES_DIALOG:
                alertDialogBuilder = new AlertDialog.Builder(getActivity());
                alertDialogBuilder.setTitle("Select device");

                Set<BluetoothDevice> pairedDevices = WearMainActivity.getPairedDevices();
                final BluetoothDevice[] devices = pairedDevices.toArray(new BluetoothDevice[0]);
                String[] items = new String[devices.length+1];
                for (int i=0;i<devices.length;i++) {
                    items[i] = devices[i].getName();
                }
                items[devices.length] = "지금 연결 안함";

                alertDialogBuilder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if(which != devices.length)
                            ((WearMainActivity)WearMainActivity.mContext).doConnect(devices[which]);
                        else
                            sIsNotConnect = true;
                    }
                });
                alertDialogBuilder.setCancelable(false);
                break;


            case ERROR_DIALOG:
                alertDialogBuilder = new AlertDialog.Builder(getActivity());
                alertDialogBuilder.setTitle("ERROR");
                alertDialogBuilder.setMessage(content);
                alertDialogBuilder.setPositiveButton("OK",  new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        ((WearMainActivity)WearMainActivity.mContext).finish();
                    }
                });
                break;


         /*       alertDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        getActivity().finish();
                    }
                });
        */

        }

        return alertDialogBuilder.create();
    }
}