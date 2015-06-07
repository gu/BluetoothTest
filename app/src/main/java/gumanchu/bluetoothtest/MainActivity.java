package gumanchu.bluetoothtest;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";

    int currentKey, previousKey;
    ControlRunnable controller;
    VideoRunnable video;

    int bytes, size;
    byte[] data;

    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothSocket mBluetoothSocket = null;
    private DataOutputStream mOutStream = null;
    private DataInputStream mInStream = null;


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        CheckBTState();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();

        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(Constants.SERVER_ADDRESS_BLUETOOTH);

        try {
            mBluetoothSocket = device.createRfcommSocketToServiceRecord(Constants.DEVICE_UUID);
        } catch (IOException e) {
            AlertBox("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");
        }

        mBluetoothAdapter.cancelDiscovery();

        try {
            mBluetoothSocket.connect();
        } catch (IOException e) {
            try {
                mBluetoothSocket.close();
            } catch (IOException e2) {
                AlertBox("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
            }
        }

        try {
            mOutStream = new DataOutputStream(mBluetoothSocket.getOutputStream());
            mInStream = new DataInputStream(mBluetoothSocket.getInputStream());
        } catch (IOException e) {
            AlertBox("Fatal Error", "In onResume() and output stream creation failed:" + e.getMessage() + ".");
        }


        currentKey = 0;
        previousKey = 1;
        controller = new ControlRunnable();
//        Thread c = new Thread(controller);
//        c.start();

        video = new VideoRunnable();
//        Thread v = new Thread(video);
//        v.start();


    }

    @Override
    public void onPause() {
        super.onPause();

        if (mOutStream != null) {
            try {
                mOutStream.flush();
            } catch (IOException e) {
                AlertBox("Fatal Error", "In onPause() and failed to flush output stream: " + e.getMessage() + ".");
            }
        }

        try     {
            mBluetoothSocket.close();
        } catch (IOException e2) {
            AlertBox("Fatal Error", "In onPause() and failed to close socket." + e2.getMessage() + ".");
        }
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }



    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_ESCAPE:
                currentKey = keyCode;
                return true;
            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {

        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_ESCAPE:
                currentKey = 0;
                return true;
            default:
                return super.onKeyUp(keyCode, event);
        }

    }

    public class ControlRunnable implements Runnable {
        @Override
        public void run() {

            Looper.prepare();
            Handler controlHandle = new Handler();

            try {
                while (mBluetoothSocket.isConnected() && currentKey != KeyEvent.KEYCODE_ESCAPE) {
                    if (currentKey != previousKey) {
                        mOutStream.writeInt(currentKey);
                        mOutStream.flush();
                    }
                    previousKey = currentKey;
                    controlHandle.postDelayed(controller, 100);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public class VideoRunnable implements Runnable {
        @Override
        public void run() {
            try {
                while (mBluetoothSocket.isConnected()) {
                    bytes = 0;

                    size = mInStream.readInt();
                    data = new byte[size];

                    for (int i = 0; i < size; i += bytes) {
                        bytes = mInStream.read(data, i, size - i);
                    }

                    Log.i(TAG, "BYTES: " + bytes);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }



    private void CheckBTState() {
        // Check for Bluetooth support and then check to make sure it is turned on

        // Emulator doesn't support Bluetooth and will return null
        if(mBluetoothAdapter ==null) {
            AlertBox("Fatal Error", "Bluetooth Not supported. Aborting.");
        } else {
            if (!mBluetoothAdapter.isEnabled()) {
                //Prompt user to turn on Bluetooth
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }

    public void AlertBox( String title, String message ){
        new AlertDialog.Builder(this)
                .setTitle( title )
                .setMessage( message + " Press OK to exit." )
                .setPositiveButton("OK", new OnClickListener() {
                    public void onClick(DialogInterface arg0, int arg1) {
                        finish();
                    }
                }).show();
    }
}
