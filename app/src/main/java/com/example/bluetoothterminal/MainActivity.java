package com.example.bluetoothterminal;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String Tag = "DEBUG_MA";

    TextView tvMAReceivedMessage;

    Button buttonSendMessage;
    Button buttonBTConnect;

    EditText editTextSentMessage;

    Spinner spinnerBTPairedDevices;

    static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    BluetoothSocket BTSocket = null;
    BluetoothAdapter BTAdapter = null;
    Set<BluetoothDevice> BTPairedDevices = null;
    boolean bBTConnected = false;
    BluetoothDevice BTDevice = null;
    classBTInitDataCommunication cBTInitSendReceive =null;

    static public final int BT_CON_STATUS_NOT_CONNECTED     =0;
    static public final int BT_CON_STATUS_CONNECTING        =1;
    static public final int BT_CON_STATUS_CONNECTED         =2;
    static public final int BT_CON_STATUS_FAILED            =3;
    static public final int BT_CON_STATUS_CONNECTiON_LOST   =4;
    static public int iBTConnectionStatus = BT_CON_STATUS_NOT_CONNECTED;

    static final int BT_STATE_LISTENING            =1;
    static final int BT_STATE_CONNECTING           =2;
    static final int BT_STATE_CONNECTED            =3;
    static final int BT_STATE_CONNECTION_FAILED    =4;
    static final int BT_STATE_MESSAGE_RECEIVED     =5;


    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(Tag, "onCreate-Start");

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
        }

        tvMAReceivedMessage = findViewById(R.id.idMATextViewReceivedMessage);

        spinnerBTPairedDevices = findViewById(R.id.idMASpinnerBTPairedDevices);

        buttonSendMessage = findViewById(R.id.idMAButtonSendData);
        buttonBTConnect = findViewById(R.id.idMAButtonConnect);



        editTextSentMessage = findViewById(R.id.idMAEditTextSendMessage);

        tvMAReceivedMessage.setText("APP Loaded");

        buttonSendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(Tag, "Send Button clicked");

                String sMessage = editTextSentMessage.getText().toString();
                sendMessage(sMessage);
            }
        });

        buttonBTConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(Tag, "Button Click buttonBTConnect");
                if (bBTConnected == false) {
                    if (spinnerBTPairedDevices.getSelectedItemPosition() == 0) {
                        Log.d(Tag, "Please select a device");
                        Toast.makeText(MainActivity.this, "Please select a bluetooth device", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String selectedDevice = spinnerBTPairedDevices.getSelectedItem().toString();
                    Log.d(Tag, "Selected device = " + selectedDevice);

                    for (BluetoothDevice BTDev : BTPairedDevices) {

                        if (selectedDevice.equals(BTDev.getName())) {
                            BTDevice = BTDev;
                            Log.d(Tag, "Selected device UUID = " + BTDevice.getAddress());

                            cBluetoothConnect cBTConnect = new cBluetoothConnect(BTDevice);
                            cBTConnect.start();

                           /* try {
                                Log.d(Tag, "Creating socket, my uuid = " + MY_UUID);
                                BTSocket = BTDevice.createRfcommSocketToServiceRecord(MY_UUID);
                                Log.d(Tag, "Connecting to device");
                                BTSocket.connect();
                                Log.d(Tag, "Connected");
                                buttonBTConnect.setText("Disconnect");
                                bBTConnected = true;
                            } catch (IOException e) {
                                e.printStackTrace();
                                Log.e(Tag, "Exception = " + e.getMessage());
                                bBTConnected = false;
                            }*/
                        }
                    }
                } else {
                    Log.d(Tag, "Disconnecting BTConnection");
                    try {
                        BTSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(Tag, "Exception = " + e.getMessage());
                    }
                    buttonBTConnect.setText("Connect");
                    bBTConnected = false;
                }

            }
        });




        getBTPairedDevices();
        populateSpinnerWithBTPairedDevices();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(Tag, "permission granted");
            } else {
                Log.e(Tag, "Permission denied");
            }

        }
    }

    void getBTPairedDevices() {
        Log.d(Tag, "getBTPairedDevices - Start");
        BTAdapter = BluetoothAdapter.getDefaultAdapter();
        if (BTAdapter == null) {
            Log.e(Tag, "getBTPairedDevices - BTAdapter null");
            editTextSentMessage.setText("\nNo Bluetooth on this device");
            return;
        } else if (!BTAdapter.isEnabled()) {
            Log.e(Tag, "getBTPairedDevices - BT not enable");
            editTextSentMessage.setText("\nPlease turn on Bluetooth");
            return;
        }


        BTPairedDevices = BTAdapter.getBondedDevices();
        Log.d(Tag, "getBTPairedDevices - Paired devices count " + BTPairedDevices.size());

        for (BluetoothDevice BTDevice : BTPairedDevices) {
            Log.d(Tag, BTDevice.getName() + ", " + BTDevice.getAddress());
        }
    }

    void populateSpinnerWithBTPairedDevices() {
        ArrayList<String> alPairedDevices = new ArrayList<>();
        alPairedDevices.add("Select");
        for (BluetoothDevice BTDev : BTPairedDevices) {

            alPairedDevices.add(BTDev.getName());
        }
        final ArrayAdapter<String> aaPairedDevices = new ArrayAdapter<String>(this, androidx.appcompat.R.layout.support_simple_spinner_dropdown_item, alPairedDevices);
        aaPairedDevices.setDropDownViewResource(androidx.appcompat.R.layout.support_simple_spinner_dropdown_item);
        spinnerBTPairedDevices.setAdapter(aaPairedDevices);
    }

    public class cBluetoothConnect extends Thread {
        private BluetoothDevice device;


        public cBluetoothConnect(BluetoothDevice BTDevice) {
            Log.i(Tag, "classBTConnect-start");

            device = BTDevice;
            try {

                BTSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (Exception exp) {
                Log.e(Tag, "classBTConnect-exp" + exp.getMessage());
            }
        }

        public void run() {
            try {

                BTSocket.connect();
                Message message = Message.obtain();
                message.what = BT_STATE_CONNECTED;
                handler.sendMessage(message);


            } catch (IOException e) {
                e.printStackTrace();
                Message message=Message.obtain();
                message.what=BT_STATE_CONNECTION_FAILED;
                handler.sendMessage(message);
            }
        }

    }

    public class classBTInitDataCommunication extends Thread
    {
        private final BluetoothSocket bluetoothSocket;
        private InputStream inputStream =null;
        private OutputStream outputStream=null;

        public classBTInitDataCommunication (BluetoothSocket socket)
        {
            Log.i(Tag, "classBTInitDataCommunication-start");

            bluetoothSocket=socket;


            try {
                inputStream=bluetoothSocket.getInputStream();
                outputStream=bluetoothSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(Tag, "classBTInitDataCommunication-start, exp " + e.getMessage());
            }


        }

        public void run()
        {
            byte[] buffer=new byte[1024];
            int bytes;

            while (BTSocket.isConnected())
            {
                try {
                    bytes = inputStream.read(buffer);
                    handler.obtainMessage(BT_STATE_MESSAGE_RECEIVED,bytes,-1,buffer).sendToTarget();
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(Tag, "BT disconnect from decide end, exp " + e.getMessage());
                    iBTConnectionStatus=BT_CON_STATUS_CONNECTiON_LOST;
                    try {
                        //disconnect bluetooth
                        Log.d(Tag, "Disconnecting BTConnection");
                        if(BTSocket!=null && BTSocket.isConnected())
                        {

                            BTSocket.close();
                        }
                        buttonBTConnect.setText("Connect");
                        bBTConnected = false;
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }

                }
            }
        }

        public void write(byte[] bytes)
        {
            try {
                outputStream.write(bytes);
                Log.d(Tag, "Sending message");
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(Tag, "sending fail = " + e.getMessage());
            }
        }
    }

    Handler handler =new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {

            switch (msg.what)
            {
                case BT_STATE_LISTENING:
                    Log.d(Tag, "BT_STATE_LISTENING");
                    break;
                case BT_STATE_CONNECTING:
                    iBTConnectionStatus = BT_CON_STATUS_CONNECTING;
                    buttonBTConnect.setText("Connecting..");
                    Log.d(Tag, "BT_STATE_CONNECTING");
                    break;
                case BT_STATE_CONNECTED:

                    iBTConnectionStatus = BT_CON_STATUS_CONNECTED;

                    Log.d(Tag, "BT_CON_STATUS_CONNECTED");
                    buttonBTConnect.setText("Disconnect");

                    cBTInitSendReceive = new classBTInitDataCommunication(BTSocket);
                    cBTInitSendReceive.start();

                    bBTConnected = true;
                    break;
                case BT_STATE_CONNECTION_FAILED:
                    iBTConnectionStatus = BT_CON_STATUS_FAILED;
                    Log.d(Tag, "BT_STATE_CONNECTION_FAILED");
                    bBTConnected = false;
                    break;

                case BT_STATE_MESSAGE_RECEIVED:
                    byte[] readBuff= (byte[]) msg.obj;
                    String tempMsg=new String(readBuff,0,msg.arg1);
                    Log.d(Tag, "Message receive ( " + tempMsg.length() + " )  data : " + tempMsg);

                    tvMAReceivedMessage.append(tempMsg);

                    break;

            }
            return true;
        }
    });

    public void sendMessage(String sMessage)
    {
        if( BTSocket!= null && iBTConnectionStatus==BT_CON_STATUS_CONNECTED)
        {
            if(BTSocket.isConnected() )
            {
                try {
                    cBTInitSendReceive.write(sMessage.getBytes());
                    tvMAReceivedMessage.append("\r\n-> " + sMessage);
                }
                catch (Exception exp)
                {
                    Log.e(Tag, "sending = " + exp.getMessage());
                }
            }
        }
        else {
            Toast.makeText(getApplicationContext(), "Please connect to bluetooth", Toast.LENGTH_SHORT).show();
            tvMAReceivedMessage.append("\r\n Not connected to bluetooth");
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(Tag, "onResume-Resume");
        //getBTPairedDevices();
        //populateSpinnerWithBTPairedDevices();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(Tag, "onPause-Start");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(Tag, "onDestroy-Start");
    }
}