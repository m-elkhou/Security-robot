package com.example.liveremote;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class RemoteActivity extends Activity {
    private Button leftButton, rightButton, upButton, downButton, switchButton;
    private static final String TAG = "RemoteActivity";

    public static final int UP_START = 1;
    public static final int UP_STOP = 2;
    public static final int DOWN_START = 3;
    public static final int DOWN_STOP = 2;
    public static final int LEFT_START = 4;
    public static final int LEFT_STOP = 5;
    public static final int RIGHT_START = 6;
    public static final int RIGHT_STOP = 5;
    public static final int DISCONNECT = 9;

    private int port = 8040;
    private String ip;
    private Socket clientSocket;
    private DataOutputStream outStream = null;
    private DataInputStream inStream = null;
    private boolean connected = false;
    private ImageView remotePreview;

    private final IntentFilter intentFilter = new IntentFilter();
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;

    private static PowerManager.WakeLock wakeLock = null;

    private boolean isAutomaticStatus= true;
    @SuppressLint({"InvalidWakeLockTag", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remote);

        leftButton = (Button) findViewById(R.id.button_left);
        rightButton = (Button) findViewById(R.id.button_right);
        upButton = (Button) findViewById(R.id.button_forward);
        downButton = (Button) findViewById(R.id.button_backward);
        remotePreview = (ImageView) findViewById(R.id.remotePreview);
        switchButton = findViewById(R.id.enable);

        Intent intent = getIntent();
        if(intent != null) {
            Bundle b = intent.getBundleExtra("b");
            ip = b.getString("ip");
            String id = b.getString("id");
            if(id != null && id.equals("ipConnect")){
                if (isConnected(RemoteActivity.this) == 1) {

                    if (clientSocket != null) {
                        try {
                            clientSocket.close();
                        } catch (Exception ex) {
                            Log.i(TAG, "clientSocket " + ex);
                        }
                    }

                    new ConnectTask().execute();

                    putInfo();
                } else {
                    String sMess = getResources().getString(R.string.no_wifi);
                    Toast.makeText(RemoteActivity.this, sMess, Toast.LENGTH_LONG).show();
                }
            } else{
                finish();
            }
        }

        leftButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    return Button_2_OutStream(LEFT_START);
                }
                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    return Button_2_OutStream(LEFT_STOP);
                }
                return false;
            }
        });

        rightButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    return Button_2_OutStream(RIGHT_START);
                }
                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    return Button_2_OutStream(RIGHT_STOP);
                }
                return false;
            }
        });

        upButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    return Button_2_OutStream(UP_START);
                }
                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    return Button_2_OutStream(UP_STOP);
                }
                return false;
            }
        });

        downButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    return Button_2_OutStream(DOWN_START);
                }
                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    return Button_2_OutStream(DOWN_STOP);
                }
                return false;
            }
        });

        leftButton.setEnabled(false);
        rightButton.setEnabled(false);
        upButton.setEnabled(false);
        downButton.setEnabled(false);
        switchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isAutomaticStatus){
                    isAutomaticStatus = false;
                    Button_2_OutStream(0);
                    switchButton.setBackgroundResource(R.drawable.switch2);
                    leftButton.setEnabled(true);
                    rightButton.setEnabled(true);
                    upButton.setEnabled(true);
                    downButton.setEnabled(true);
                }else{
                    isAutomaticStatus = true;
                    Button_2_OutStream(0);
                    switchButton.setBackgroundResource(R.drawable.switch1);
                    leftButton.setEnabled(false);
                    rightButton.setEnabled(false);
                    upButton.setEnabled(false);
                    downButton.setEnabled(false);
                }
            }
        });

        getInfo();

        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "ArduinoRemoteLock");
    }

    private boolean Button_2_OutStream(int st){
        final int s =st;
        if (connected) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        outStream.writeInt(s);
                    } catch (Exception e) {
                        Log.e(TAG, "" + e);
                    }
                }
            }).start();
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onPause() {
        super.onPause();

        DISCONNECT();
        mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {

            @Override
            public void onFailure(int reasonCode) {
                Log.d(TAG, "WiFi disconnect failed. Reason :" + reasonCode);
            }

            @Override
            public void onSuccess() {
                Log.d(TAG, "WiFi disconnected.");
            }

        });

        connected = false;
        wakeLock.release();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        DISCONNECT();
    }

    private void DISCONNECT(){

        if (clientSocket != null) {
            try {
                outStream.writeInt(DISCONNECT);
                clientSocket.close();
            } catch (Exception ex) {
                Log.i(TAG, "clientSocket " + ex);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        wakeLock.acquire();
    }

    /**
     * Check wifi connection
     *
     * @param context activity context
     * @return boolean
     */
    private int isConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = null;
        int bOK = 0;
        if (connectivityManager != null) {
            try {
                networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                if (networkInfo != null && networkInfo.isConnectedOrConnecting()) {
                    bOK = 1;
                }
            } catch (Exception ex) {
                Log.i(TAG, "getNetworkInfo " + ex);
            }
        }

        if (networkInfo == null) {
            bOK = 0;
        }

        return bOK;
    }

    /**
     * write configure
     */
    private void putInfo() {
        String text = ip;
        SharedPreferences settings = getSharedPreferences("org.landroo.arduino_preferences", MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("ip", text);
        editor.putString("default_port", "" + port);
        editor.commit();
    }

    /**
     * read configure
     */
    private void getInfo() {
        SharedPreferences settings = getSharedPreferences("org.landroo.arduino_preferences", Context.MODE_PRIVATE);
        String ip = settings.getString("ip", "192.168.0.1");

    }

    // ip client task
    private class ConnectTask extends AsyncTask<Integer, Integer, Long> {
        protected Long doInBackground(Integer... data) {
            try {
                // connect to server socket of Ip address
                clientSocket = new Socket(ip, port);
                outStream = new DataOutputStream(clientSocket.getOutputStream());
                inStream = new DataInputStream(clientSocket.getInputStream());
                ClientServiceThread clientThread = new ClientServiceThread(clientSocket, 1);
                clientThread.start();
                connected = true;
            } catch (Exception e) {
                Log.i(TAG, "ConnectTask " + e);
            }

            return (long) 0;
        }
    }

    // image receiver thread
    class ClientServiceThread extends Thread {
        Socket socket;
        public int clientID = -1;
        public boolean running = true;

        ClientServiceThread(Socket soc, int id) {
            socket = soc;
            clientID = id;
        }

        public void run() {

            while (running) {
                try {
                    int len = inStream.readInt();
                    if (len == 1) {
                        RemoteActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.i(TAG, "Connection success!");
                            }
                        });
                    }
                    else if (len > 10 && len < 200000) {
//                    else if (len > 10 ) {
                        byte[] buffer = new byte[len];
                        inStream.readFully(buffer, 0, len);

                        final Bitmap image = BitmapFactory.decodeByteArray(buffer, 0, len);
                        if (image != null) {
                            RemoteActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    remotePreview.setImageBitmap(image);
                                    remotePreview.invalidate();
                                    //Log.i(TAG, "Image shown!");
                                }
                            });
                        }
                    }
                } catch (Exception e) {
                    Log.i(TAG, "ClientServiceThread " + e);
                    running = false;
                }
            }
        }
    }
}
