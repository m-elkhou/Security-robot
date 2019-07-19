package com.example.liveremote;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.usb.UsbManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class Arduino_Server extends Activity {
    private static final String TAG = Arduino_Server.class.getSimpleName();

    private TextView mLogTextView;
    private String[] mLog = new String[20];
    private ScrollView mScrollView;

    private Button remoteButton;
    private Button ipserverButton;

    private int port = 8040;
    private ServerSocket serverSocket;
    private Socket socket;

    private static PowerManager.WakeLock wakeLock = null; // katfrad 3la wa7d Dvices yb9a cha3ale

    public DataOutputStream outStream = null;
    private boolean isSend = false;

    private CamPreview camPreview; // ktjib tssawer mn l camera
    private SurfaceHolder mHolder;
    public Camera mCamera;
    private List<Camera.Size> cameraSize; // cameraSize
    private FrameLayout senderPreview;

    private int displayWidth;
    private int displayHeight;
    private int mWidth = 0;
    private int mHeight = 0;
    private int imageFormat = ImageFormat.JPEG;
    private int currentCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;

    private final IntentFilter intentFilter = new IntentFilter(); // kt3tih wa7d l 5dma w howwa ky9llab 3la les application lli fihom had service
    private WifiP2pManager wifiP2pManager;
    private WifiP2pManager.Channel wifiChannel;
    private WifiP2pInfo p2pInfo;
    private List<WifiP2pDevice> listDevice = new ArrayList();
    private boolean connected = false;

    private static int id = -1;
    private USB usb ;
    private WifiP2pManager.ChannelListener channelListener = new WifiP2pManager.ChannelListener() {
        @Override
        public void onChannelDisconnected() {
            Log.i(TAG, "Channel Disconnected!");
        }
    };

//    // wifi direct peer listener
//    private WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
//        @Override
//        public void onPeersAvailable(WifiP2pDeviceList peerList) {
//
//            // Out with the old, in with the new.
//            listDevice.clear();
//            listDevice.addAll(peerList.getDeviceList());
//
//            if (listDevice.size() == 0) {
//                Log.i(TAG, "No wifi direct devices found!");
//                return;
//            }
//            else
//            {
//                for(WifiP2pDevice p: listDevice)
//                    Log.i(TAG, p.deviceName);
//            }
//        }
//    };

//    /**
//     * BroadcastReceiver
//     */
//    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
//        public void onReceive(Context context, Intent intent) {
//            String action = intent.getAction();
//
////            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
////                //closeDevice();
////            }
////            else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
////                // The peer list has changed!  We should probably do something about that.
////                if (wifiP2pManager != null) {
////                    wifiP2pManager.requestPeers(wifiChannel, peerListListener);
////                }
////                //Log.d(Arduino_Server.TAG, "server WIFI_P2P_PEERS_CHANGED_ACTION");
////            }
//             if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
//                //Log.i(TAG, "server WIFI_P2P_THIS_DEVICE_CHANGED_ACTION");
//                WifiP2pDevice device = (WifiP2pDevice) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
//                final String name = "WiFi id: " + device.deviceName;
//                showLog(name);
//
//                if(device.status == WifiP2pDevice.AVAILABLE) {
//                    connected = false;
//                    wifiP2pManager.discoverPeers(wifiChannel, new WifiP2pManager.ActionListener() {
//                        @Override
//                        public void onSuccess() {
//                            Arduino_Server.this.runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    showLog("WiFi Direct Discovery Initiated");
//                                }
//                            });
//                        }
//
//                        @Override
//                        public void onFailure(int reasonCode) {
//                            //Log.i(TAG, "Discovery Failed : " + reasonCode);
//                            Arduino_Server.this.runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    showLog("WiFi Direct Discovery Failed");
//                                }
//                            });
//                        }
//                    });
//                }
//            }
//        }
//    };


    // camera preview surface
    class CamPreview extends SurfaceView implements SurfaceHolder.Callback {

        public CamPreview(Context context) {
            super(context);

            mHolder = getHolder();// initialize the Surface Holder
            mHolder.addCallback(this); // add the call back to the surface holder
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        // Called once the holder is ready
        public void surfaceCreated(SurfaceHolder holder) {
            // The Surface has been created, acquire the camera and tell it where to draw.
            try {
                mCamera.setPreviewDisplay(holder);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Called when the holder is destroyed
        public void surfaceDestroyed(SurfaceHolder holder) {
            if (outStream != null) {
                try {
                    outStream.writeInt(-1);
                    outStream.close();
                    outStream = null;
                } catch (Exception e) {
                    Log.i(TAG, "outStream " + e);
                }
            }
        }

        // Called when holder has changed
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            try {
                float rat = 0;

                if(mWidth > mHeight)
                    rat = (float)mHeight / (float)mWidth;
                else
                    rat = (float)mWidth / (float)mHeight;

                int w, h;
                if(width > height) {
                    h = height;
                    w = (int)(height / rat);
                }
                else {
                    w = width;
                    h = (int)(width / rat);
                }

                holder.setFixedSize(w, h);

                //Log.i(TAG, "holder: " + width + " " + height);/////////////////////////////////////////////////////////////////////////////////////////////////
                if (mCamera == null)
                    mCamera = Camera.open();// activate the camera
                if (mCamera != null)
                    mCamera.startPreview();// camera frame preview starts when user launches application screen

                Camera.Parameters parameters = mCamera.getParameters();

                Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
                if (display.getRotation() == Surface.ROTATION_0) {
                    parameters.setPreviewSize(mHeight, mWidth);
                    mCamera.setDisplayOrientation(90);
                }
                else if (display.getRotation() == Surface.ROTATION_90) {
                    parameters.setPreviewSize(mWidth, mHeight);
                }
                else if (display.getRotation() == Surface.ROTATION_180) {
                    parameters.setPreviewSize(mHeight, mWidth);
                }
                else if (display.getRotation() == Surface.ROTATION_270) {
                    parameters.setPreviewSize(mWidth, mHeight);
                    mCamera.setDisplayOrientation(180);
                }

                mCamera.setPreviewCallback(new Camera.PreviewCallback() {
                    // Called for each frame previewed
                    public void onPreviewFrame(byte[] data, Camera camera) {
                        System.gc();
                        if(!isSend) {
                            if (outStream != null && connected) {
                                isSend = true;
                                sendImage(data, camera);
                            }
                        }
                    }
                });
                mCamera.setParameters(parameters);// setting the parameters to the camera but this line is not required
            }
            catch (Exception e) {
                Log.i(TAG, "surfaceChanged " + e);
            }
        }
    }

    @SuppressLint("InvalidWakeLockTag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_arduino_server);

        mScrollView = (ScrollView) findViewById(R.id.scroller);
        mLogTextView = (TextView) findViewById(R.id.tvTopText);
        for (int i = 0; i < mLog.length; i++)
            mLog[i] = "";


        ipserverButton = (Button) findViewById(R.id.ip_server_btn);
        ipserverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isConnected(Arduino_Server.this) == 1) {
                    try {
                        if (serverSocket != null) {
                            serverSocket.close();
                        }
                        serverSocket = new ServerSocket(port);

                        new ServerTask().execute(0);

                        remoteButton.setEnabled(false);

                        showLog("IPServer started: " + getLocalIpAddress() + ":" + port);

                    } catch (Exception e) {
                        Log.e(TAG, "" + e);
                    }
                } else {
                    final String sMess = getResources().getString(R.string.no_wifi);
                    Arduino_Server.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showLog(sMess);
                        }
                    });
                }
            }
        });

        remoteButton = (Button) findViewById(R.id.client_btn);
        remoteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Arduino_Server.this, com.example.liveremote.RemoteManager.class);
                startActivity(intent);
                Arduino_Server.this.finish();
            }
        });

        usb = new USB(this);
        usb.Start();

        Display display = getWindowManager().getDefaultDisplay();
        displayWidth = display.getWidth();
        displayHeight = display.getHeight();

        createCamera();

        senderPreview = (FrameLayout) findViewById(R.id.senderPreview);

        camPreview = new CamPreview(this);
        senderPreview.addView(camPreview);

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "ArduinoServerLock");
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumeCamera();
//        registerReceiver(mReceiver, intentFilter);

        wifiP2pManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        wifiChannel = wifiP2pManager.initialize(Arduino_Server.this, getMainLooper(), channelListener);

        wakeLock.acquire();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // unregister the intent filtered actions
//        unregisterReceiver(mReceiver);

        usb.Stop();

        try {
            if (mCamera != null) {
                // Call stopPreview() to stop updating the preview surface.
                mCamera.stopPreview();

                mCamera.setPreviewCallback(null);
                camPreview.getHolder().removeCallback(camPreview);

                // Important: Call release() to release the camera for use by other
                // applications. Applications should release the camera immediately
                // during onPause() and re-open() it during onResume()).
                mCamera.release();

                mCamera = null;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        if (outStream != null) {
            try {
                outStream.writeInt(-1);
                outStream.close();
                outStream = null;
            } catch (Exception e) {
                Log.i(TAG, "outStream " + e);
            }
        }

        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (Exception ex) {
                Log.i(TAG, "toReceiver " + ex);
            }
        }

        wakeLock.release();

        wifiP2pManager.removeGroup(wifiChannel, new WifiP2pManager.ActionListener() {

            @Override
            public void onFailure(int reasonCode) {
                Log.d(TAG, "WiFi disconnect failed. Reason :" + reasonCode);
            }

            @Override
            public void onSuccess() {
                Log.d(TAG, "WiFi disconnected.");
            }

        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.net_cam_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings)
        {
            Intent PreferenceScreen = new Intent(this, Preferences.class);
            startActivity(PreferenceScreen);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // ip server task
    private class ServerTask extends AsyncTask<Integer, Integer, Long> {
        protected Long doInBackground(Integer... num) {
            try {
                int id = 0;
                ClientServiceThread clientThread = null;
                while (true) {
                    socket = serverSocket.accept();
                    clientThread = new ClientServiceThread(socket, id);
                    clientThread.start();
                    outStream = new DataOutputStream(socket.getOutputStream()); // hal mochkil ////////////////////////////////////////////////////
                    //Log.i(TAG, "New client " + id);
                    final String txt = "New client connected " + id;
                    Arduino_Server.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showLog(new String(txt));
                        }
                    });
                    id++;

                    connected = true;
                }
            } catch (Exception e) {
                Log.i(TAG, "ShowTask " + e);
            }
            //Log.i(TAG, "Exit");

            return (long) 0;
        }
    }

    // ip server thread
    class ClientServiceThread extends Thread {
        Socket socket;
        public int clientID = -1;
        public boolean running = true;

        ClientServiceThread(Socket soc, int id) {
            socket = soc;
            clientID = id;
            // send connection success sign
            try {
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                dos.writeInt(1); // katsift l client . biha kytistiye l conection
            } catch (Exception ex) {
                Log.i(TAG, "" + ex);
            }
        }

        // receive command loop ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        public void run() {
            while (running) {
                try {
                    DataInputStream dis = new DataInputStream(socket.getInputStream());
                    int command = dis.readInt();
                    if (command >= 0 && command < 10) {
                        if (command == RemoteActivity.DISCONNECT) {
                            running = false;
                            return;
                        }else {
                            if (command == 0) {
                                if (usb.isAutomaticStatus) {
                                    usb.isAutomaticStatus = false;
                                } else {
                                    usb.isAutomaticStatus = true;
                                }
                            }
                            final String commandS = "" + command;
                            final String txt = "New command " + command + " from " + clientID + " client";
                            Arduino_Server.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    usb.Send(commandS);
                                    showLog(txt);
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

    /**
     * Get the IP address of the device
     *
     * @return String 192.168.0.1
     */
    public String getLocalIpAddress() {
        String res = "";
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        res += " " + inetAddress.getHostAddress();
                    }
                }
            }
        } catch (Exception ex) {
            Log.i(TAG, "getLocalIpAddress " + ex);
        }
        //Log.i(TAG, res);

        return res;
    }

    /**
     * show info line
     * @param line info
     */
    private void showLog(String line) {

        for (int i = 0; i < mLog.length - 1; i++) {
            mLog[i] = mLog[i + 1];
        }
        mLog[mLog.length - 1] = line;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mLog.length; i++) {
            if (!mLog[i].equals("")) {
                sb.append(mLog[i]);
                sb.append("\n");
            }
        }
        mLogTextView.setText(sb.toString());
        mScrollView.smoothScrollTo(0, mLogTextView.getBottom());
    }

    // is wifi turned on
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
     * create camera preview and enumerate the sizes
     *
     * @return
     */
    private boolean createCamera() {
        try {
            mCamera = Camera.open(); // attempt to get a Camera instance
            if (mCamera == null)
                mCamera = Camera.open(0);
            if (mCamera == null) {
                mCamera = Camera.open(1);
                currentCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
            }
            if (mCamera == null)
                return false;

            // acquire the parameters for the camera
            Camera.Parameters parameters = mCamera.getParameters();

            cameraSize = parameters.getSupportedPictureSizes();
            int w = Integer.MAX_VALUE;
            for (Camera.Size s : cameraSize) {
                if (s.width < w)
                    w = s.width;
                if(s.width < displayWidth / 2)
                    break;
            }

            int i = 0;
            for (Camera.Size s : cameraSize) {
                if (s.width == w)
                    break;
                i++;
            }
            mWidth = cameraSize.get(i).width;
            mHeight = cameraSize.get(i).height;
            parameters.setPreviewSize(mWidth, mHeight);

            mCamera.setPreviewCallback(new Camera.PreviewCallback() {
                // Called for each frame previewed
                public void onPreviewFrame(byte[] data, Camera camera) {
                    System.gc();
                    if(!isSend) {
                        if (outStream != null && connected) {
                            isSend = true;
                            sendImage(data, camera);
                        }
                    }
                }
            });
            mCamera.setParameters(parameters);// setting the parameters to the camera but this line is not required

            imageFormat = parameters.getPreviewFormat();
        } catch (Exception ex) {
            Log.e(TAG, "createCamera " + ex);
        }

        return true;
    }

    // resume camera
    private void resumeCamera() {
        try {
            if (mCamera != null)
                mCamera.startPreview();
            else {
                createCamera();

                senderPreview = (FrameLayout) findViewById(R.id.senderPreview);

                camPreview = new CamPreview(this);
                senderPreview.addView(camPreview);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * send an image through the opened socket to the client
     * @param data  byte array image data
     */
    private void sendImage(byte[] data, Camera camera) {
        try {
            Camera.Size size = camera.getParameters().getPreviewSize();

            YuvImage yuv_image = new YuvImage(data, imageFormat, size.width, size.height, null);
            // all bytes are in YUV format therefore to use the YUV helper functions we are putting in a YUV object

            Rect rect = new Rect(0, 0, size.width, size.height);
            ByteArrayOutputStream output_stream = new ByteArrayOutputStream();

            yuv_image.compressToJpeg(rect, 20, output_stream);
            // image has now been converted to the jpg format and bytes have been written to the output_stream object

            byte[] tmp = output_stream.toByteArray();

            outStream.writeInt(tmp.length);
            outStream.write(tmp);// writing the array to the socket output stream
            outStream.flush();
        } catch (Exception ex) {
            Log.i(TAG, "SendTask " + ex);
        }

        data = null;

        isSend = false;
    }
}