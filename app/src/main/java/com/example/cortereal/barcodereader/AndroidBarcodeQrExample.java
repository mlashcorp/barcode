package com.example.cortereal.barcodereader;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.OutputStream;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import java.util.UUID;
import android.content.DialogInterface.OnClickListener;


public class AndroidBarcodeQrExample extends Activity {

    static final String ACTION_SCAN = "com.google.zxing.client.android.SCAN";
    private static final UUID MY_UUID = UUID.fromString("94f39d29-7d6d-437d-973b-fba39e49d4ee");


    // Insert your server's MAC address
    private static String address = "00:1A:7D:DA:71:04";
    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private OutputStream outStream = null;
    private ConnectThread mConnectThread;
    private TransmitThread mTransmitThread;

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        public ConnectThread() {
            ;
        }
        private void establishConnection(){
            Log.d("cloud-debug","\n...In onResume...\n...Attempting client connect...");

            // Set up a pointer to the remote node using it's address.
            Log.d("spinit-cloud","1");
            BluetoothDevice device = btAdapter.getRemoteDevice(address);
            Log.d("spinit-cloud","2");
            // Two things are needed to make a connection:
            //   A MAC address, which we got above.
            //   A Service ID or UUID.  In this case we are using the
            //     UUID for SPP.
            try {
                btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.d("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");
            }
            Log.d("spinit-cloud","3");
            // Discovery is resource intensive.  Make sure it isn't going on
            // when you attempt to connect and pass your message.
            btAdapter.cancelDiscovery();
            Log.d("spinit-cloud", "4");
            // Establish the connection.  This will block until it connects.
            try {
                btSocket.connect();
                Log.d("cloud-debug","\n...Connection established and data link opened...");
            } catch (IOException e) {
                try {
                    btSocket.close();
                } catch (IOException e2) {
                    Log.d("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
                }
            }
            Log.d("spinit-cloud","5");
            // Create a data stream so we can talk to server.
            Log.d("cloud-debug","\n...Sending message to server...");

            try {
                outStream = btSocket.getOutputStream();
            } catch (IOException e) {
                Log.d("Fatal Error", "In onResume() and output stream creation failed:" + e.getMessage() + ".");
            }
        }
        public void run() {
            establishConnection();
        }

        public void cancel() {
           ;
        }
    }

    private class TransmitThread extends Thread {
        private String payload;
        public TransmitThread(String payload) {
            this.payload = payload;
        }

        private void sendBTData(String payload) {
            if (btSocket == null || !btSocket.isConnected()){
                Log.d("spinit-cloud", "no socket connected - establishing connection");
                mConnectThread = new ConnectThread();
                mConnectThread.start();

                //establishConnection();
            }

            byte[] msgBuffer = payload.getBytes();
            try {
                mConnectThread.join();
                outStream.write(msgBuffer);
            } catch (Exception e) {
                String msg = "In sendBTData and an exception occurred during write: " + e.getMessage();
                if (address.equals("00:00:00:00:00:00"))
                    msg = msg + ".\n\nUpdate your server address from 00:00:00:00:00:00 to the correct address on line 37 in the java code";
                msg = msg +  ".\n\nCheck that the SPP UUID: " + MY_UUID.toString() + " exists on server.\n\n";

                Log.d("Fatal Error", msg);
            }
        }

        public void run() {
            Log.d("spinit-cloud","Starting data transmission via thread!");
            this.sendBTData(this.payload);
        }

        public void cancel() {
            ;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //set the main content layout of the Activity
        setContentView(R.layout.activity_main);
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        CheckBTState();
    }


    private void CheckBTState() {
        // Check for Bluetooth support and then check to make sure it is turned on

        // Emulator doesn't support Bluetooth and will return null

        if(btAdapter==null) {
            Log.d("Fatal Error", "Bluetooth Not supported. Aborting.");
        } else {
            if (btAdapter.isEnabled()) {
                Log.d("cloud-debug", "\n...Bluetooth is enabled...");
            } else {
                //Prompt user to turn on Bluetooth
                Intent enableBtIntent = new Intent(btAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }


    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();

        Log.d("cloud-debug","\n...In onPause()...");

        if (outStream != null) {
            try {
                outStream.flush();
            } catch (IOException e) {
                Log.d("Fatal Error", "In onPause() and failed to flush output stream: " + e.getMessage() + ".");
            }


            try {
                btSocket.close();
            } catch (IOException e2) {
                Log.d("Fatal Error", "In onPause() and failed to close socket." + e2.getMessage() + ".");
            }
        }
    }
    //product barcode mode
    public void scanBar(View v) {
        try {
            //start the scanning activity from the com.google.zxing.client.android.SCAN intent
            Intent intent = new Intent(ACTION_SCAN);
            //intent.putExtra("SCAN_MODE", "PRODUCT_MODE");
            startActivityForResult(intent, 0);
        } catch (ActivityNotFoundException anfe) {
            //on catch, show the download dialog
            showDialog(AndroidBarcodeQrExample.this, "No Scanner Found", "Download a scanner code activity?", "Yes", "No").show();
        }
    }


    //alert dialog for downloadDialog
    private static AlertDialog showDialog(final Activity act, CharSequence title, CharSequence message, CharSequence buttonYes, CharSequence buttonNo) {
        AlertDialog.Builder downloadDialog = new AlertDialog.Builder(act);
        downloadDialog.setTitle(title);
        downloadDialog.setMessage(message);
        downloadDialog.setPositiveButton(buttonYes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                Uri uri = Uri.parse("market://search?q=pname:" + "com.google.zxing.client.android");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                try {
                    act.startActivity(intent);
                } catch (ActivityNotFoundException anfe) {

                }
            }
        });
        downloadDialog.setNegativeButton(buttonNo, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        return downloadDialog.show();
    }

    //on ActivityResult method
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == 0) {
            if (resultCode == RESULT_OK) {
                //get the extras that are returned from the intent
                String contents = intent.getStringExtra("SCAN_RESULT");
                String format = intent.getStringExtra("SCAN_RESULT_FORMAT");
                //Toast toast = Toast.makeText(this, "Content:" + contents + " Format:" + format, Toast.LENGTH_LONG);
                //toast.show();
                mTransmitThread = new TransmitThread(contents);
                mTransmitThread.start();


            }
        }
    }


}
