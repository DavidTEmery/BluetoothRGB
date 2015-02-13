// David Emery.  2/13/15
// An Android application to control an RGB LED over Bluetooth.

// This example is designed to work between a Bluetooth enabled Android device and a serial Bluetooth
// module communicating with a micro-controller(like an Arduino) which drives an RGB LED at varying
// brightness using PWM.  It has only been tested on an HC-05.

// For this program to work you must already have the 2 devices "paired".
// The program also assumes that the Android device is ONLY paired with the BT device.

package development.david.com.bluetoothrgb;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class Main extends Activity {
    int r, g, b;

    BluetoothAdapter mBluetoothAdapter;
    BluetoothDevice hc05;
    ConnectedThread connectedThread;
    TextView tvMessage;
    // These are the 3 sliders to set the RGB values:
    SeekBar red, green, blue;
    boolean UI = false;

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            byte[] writeBuf = (byte[]) msg.obj;
            int begin = (int)msg.arg1;
            int end = (int)msg.arg2;

            switch(msg.what) {
                case 1:
                    if(writeBuf != null) {
                        String writeMessage = new String(writeBuf);
                        writeMessage = writeMessage.substring(begin, end);

                        tvMessage.setText(writeMessage);
                        Log.d(" MESSAGE: ", writeMessage);
                    }
                    else {
                        tvMessage.setText("No Response");
                    }
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI assignments:
        tvMessage = (TextView) findViewById(R.id.returnMessage);
        red = (SeekBar) findViewById(R.id.red);
        red.setMax(255);
        red.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                r = progress;
                changebrightness();
//                Log.d(" RED:", String.valueOf(progress));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        green =(SeekBar) findViewById(R.id.green);
        green.setMax(255);
        green.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                g = progress;
                changebrightness();
//                Log.d(" GREEN:", String.valueOf(progress));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        blue = (SeekBar) findViewById(R.id.blue);
        blue.setMax(255);
        blue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                b = progress;
                changebrightness();
//                Log.d("BLUE ", String.valueOf(progress));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not have Bluetooth
            Log.d("   ", "Device does not have Bluetooth");
        }
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        // If there are paired devices
        if (pairedDevices.size() > 0) {
            // Phone should only be paired with one device
            for (BluetoothDevice device : pairedDevices) {
                hc05 = device;
                Log.d(" DEVICE: ", device.getName()+"   "+device.getAddress());
            }
        }
        ConnectThread ct = new ConnectThread(hc05);
        ct.start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.my, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void manageConnectedSocket(BluetoothSocket mmSocket){
        connectedThread = new ConnectedThread(mmSocket);
        connectedThread.start();
    }

    // Creates a string from the separate RGB values and delimits it with newlines and commas
    // then writes it to the ConnectedThread object.
    public void changebrightness(){
        String str = String.valueOf('\n')+
                String.valueOf(r)+','+
                String.valueOf(g)+','+
                String.valueOf(b)+
                String.valueOf('\n');
        connectedThread.write(str);
    }

    // Called when the Bluetooth device is connected:
    Runnable enableUI = new Runnable() {
        @Override
        public void run() {
            tvMessage.setText("HC-05 Connected");
            red.setVisibility(View.VISIBLE);
            green.setVisibility(View.VISIBLE);
            blue.setVisibility(View.VISIBLE);
        }
    };

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
            } catch (NullPointerException npe) {
//                Log.d("error", npe.getMessage());
                Log.d("  NPE UUID: ",MY_UUID.toString());
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            mBluetoothAdapter.cancelDiscovery();

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();

                new Handler(Looper.getMainLooper()).post(enableUI);

                UI = true;
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                Log.d("  ", "Cannot connect Socket");
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                }
                return;
            }

            // Do work to manage the connection (in a separate thread)
            manageConnectedSocket(mmSocket);
        }

         // Will cancel an in-progress connection, and close the socket
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int begin = 0;
            int bytes = 0;
            while (true) { // Do forever:
                try {
                    bytes += mmInStream.read(buffer, bytes, buffer.length - bytes);
                    for(int i = begin; i < bytes; i++) {
                        // The message is sent from the InStream once the newline character is read:
                        if(buffer[i] == "\n".getBytes()[0]) {
                            mHandler.obtainMessage(1, begin, i, buffer).sendToTarget();
                            begin = i + 1;
                            if(i == bytes - 1) {
                                bytes = 0;
                                begin = 0;
                            }
                        }
                    }
                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }

        /* Overload of write method to take string */
        public void write(String string) {
            try {
                mmOutStream.write(string.getBytes());
            } catch (IOException e) { }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

} // End of Main Activity