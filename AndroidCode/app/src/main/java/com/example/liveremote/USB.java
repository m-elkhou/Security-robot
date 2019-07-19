package com.example.liveremote;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.DataOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class USB {
    public final String ACTION_USB_PERMISSION = "com.hariharan.arduinousb.USB_PERMISSION";
    private Context context;
    private UsbManager usbManager;
    private UsbDevice usbDevice;
    private UsbSerialDevice usbSerialDevice;
    private UsbDeviceConnection usbDeviceConnection;

    private UsbSerialInterface.UsbReadCallback usbReadCallback;

    private BroadcastReceiver broadcastReceiver ;

    public boolean isAutomaticStatus = true;

    public USB( Context con){
        this.context=con;
        usbManager = (UsbManager) context.getSystemService(context.USB_SERVICE);
        usbReadCallback = new UsbSerialInterface.UsbReadCallback() {
            //Defining a Callback which triggers whenever data is read.
            @Override
            public void onReceivedData(byte[] bytes) {
                String data = null;
                try {

                    data = ""+new String(bytes, "UTF-8");
//                    data.concat("/n");
                    if(data != null && !data.equals("")) {
                        ReceivedData(data);
                    }
                } catch (Exception e) {
                    Toast.makeText(context, "\n! Faild to recieve Data from USB !!!!!!!!!!! \n"+e, Toast.LENGTH_SHORT).show();
                }
            }
        };

        broadcastReceiver = new BroadcastReceiver() {
            //Broadcast Receiver to automatically start and stop the Serial usbDeviceConnection.
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                    boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                    if (granted) {
                        usbDeviceConnection = usbManager.openDevice(usbDevice);
                        usbSerialDevice = UsbSerialDevice.createUsbSerialDevice(usbDevice, usbDeviceConnection);
                        if (usbSerialDevice != null) {
                            if (usbSerialDevice.open()) { //Set Serial Connection Parameters.
                                usbSerialDevice.setBaudRate(9600);
                                usbSerialDevice.setDataBits(UsbSerialInterface.DATA_BITS_8);
                                usbSerialDevice.setStopBits(UsbSerialInterface.STOP_BITS_1);
                                usbSerialDevice.setParity(UsbSerialInterface.PARITY_NONE);
                                usbSerialDevice.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                                usbSerialDevice.read(usbReadCallback);
                                Toast.makeText(context, "Serial Connection Opened!\n", Toast.LENGTH_SHORT).show();

                            } else {
                                Log.d("SERIAL", "PORT NOT OPEN");
                            }
                        } else {
                            Log.d("SERIAL", "PORT IS NULL");
                        }
                    } else {
                        Log.d("SERIAL", "PERM NOT GRANTED");
                    }
                } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                    Start();
                } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                    Stop();
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        context.registerReceiver(broadcastReceiver, filter);
    }

    public void Start() {

        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                usbDevice = entry.getValue();
                int deviceVID = usbDevice.getVendorId();
                if (deviceVID == 0x2341)//Arduino Vendor ID
                {
                    PendingIntent pi = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
                    usbManager.requestPermission(usbDevice, pi);
                    keep = false;
                } else {
                    usbDeviceConnection = null;
                    usbDevice = null;
                }

                if (!keep)
                    break;
            }
        }
        Send("besmi LAh");
    }

    /// bax ktsufat les donner l arduino
    public void Send(String comande) {
        try {
            usbSerialDevice.write(comande.getBytes());    /// bax ktsufat les donner l arduino
        } catch (Exception e) {
            System.err.println("USB Ereuer !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! "+ e);
            Toast.makeText(context, "USB Ereuer !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! "+ e, Toast.LENGTH_SHORT).show();
        }
    }

    public void Stop() {
        if (usbSerialDevice != null) {
            usbSerialDevice.close();
            usbSerialDevice = null;
        }
        Toast.makeText(context, "\nSerial Connection Closed! \n", Toast.LENGTH_SHORT).show();
    }

    private void ReceivedData(String cmd){
        final String data = cmd;
        Activity activity = (Activity) context;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, "Command : \n"+data, Toast.LENGTH_SHORT).show();
                if (data.equals("C")){
                    Send("2"); // 2 - up/down stop
                    isAutomaticStatus=false;
                }
                else if (data.equals("R")){

                    Send("4"); //- left start
                    // dely to slep 100 ms. We ned thred for that!!!!
                    new Handler().postDelayed(
                            new Runnable() {
                                public void run() {
                                    Send("5"); //- left/right stop
                                }
                            }, 100);
                }

                else if (data.equals("L")) {
                    Send("6"); //6 - right start
                    // dely to slep 100 ms. We ned thred for that!!!!
                    new Handler().postDelayed(
                            new Runnable() {
                                public void run() {
                                    Send("5"); //- left/right stop
                                }
                            }, 100);
                }else if (data.equals("N")) {
                    Send("4"); //- left start
                    Send("3"); // 3 - down start
                    // dely to slep 100 ms. We ned thred for that!!!!
                    new Handler().postDelayed(
                            new Runnable() {
                                public void run() {
                                    Send("9"); //- all stop
                                }
                            }, 100);
                }else if (data.equals("O")) {
                    Send("6"); //6 - right start
                    Send("3"); // 3 - down start
                    // dely to slep 100 ms. We ned thred for that!!!!
                    new Handler().postDelayed(
                            new Runnable() {
                                public void run() {
                                    Send("9"); //- all stop
                                }
                            }, 100);
                }else if (data.equals("P")) {
                    Send("3"); // 3 - down start
                    // dely to slep 100 ms. We ned thred for that!!!!
                    new Handler().postDelayed(
                            new Runnable() {
                                public void run() {
                                    Send("9"); //- all stop
                                }
                            }, 100);
                }

                else if (data.equals("M")) {
                    Notification.sendNotification(context, " Worning !!", "Movment Detected");
                    Send("F"); //  bzzer & Pil start
                    new Handler().postDelayed(
                            new Runnable() {
                                public void run() {
                                    Send("G"); // bzzer & Pil stop
                                }
                            }, 1000);
                }
                else if (data.equals("F")) {
                    String ip;
                    try {
                        ip = InetAddress.getLocalHost().getHostAddress();
                    }catch (Exception e){
                        ip="fire dangeres";
                    }
                    Notification.sendNotification(context, " Worning !! Fire dangeres", "Robot ip : "+ip);
                    Send("F"); //  bzzer & Pil start
                    new Handler().postDelayed(
                            new Runnable() {
                                public void run() {
                                    Send("G"); // bzzer & Pil stop
                                    new Handler().postDelayed(
                                            new Runnable() {
                                                public void run() {
                                                    Send("H");
                                                }
                                            }, 2000);
                                }
                            }, 1000);
                }
            }
        });
    }

}

