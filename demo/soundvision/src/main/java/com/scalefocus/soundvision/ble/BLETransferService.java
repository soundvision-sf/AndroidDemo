package com.scalefocus.soundvision.ble;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.view.View;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.scalefocus.soundvision.ble.data.BLEScanAdvertising;
import com.scalefocus.soundvision.ble.data.ColorScanConfiguration;
import com.scalefocus.soundvision.ble.data.DeviceStats;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BLETransferService extends Service {

    public enum BleCommand {
        NoCommand,
        SegmentDataConfirm,
        SegmentDataCancel,
        GetPicture,
        GetColor,
        ChangeResolution,
        ChangePhy,
        GetBleParams,
        GetColorPalette,
        SetColorPalette,
        SendControlCode,
        SendLedValue,
        SetStateMode,
        SetVolume,
        SetMute,
        PlayAudio,
        BleScan,
    };

    public final static String SoundVisionDeviceName = "SoundVision";
    private final static String TAG = BLETransferService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;
    private Map<UUID, String> DISInfo = new HashMap<>();
    private boolean DISLoaded = false;

    private static IBLETransferClient mClient;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "com.scalefocus.soundvision.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.scalefocus.soundvision.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.scalefocus.soundvision.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.scalefocus.soundvision.ACTION_DATA_AVAILABLE";
    public final static String ACTION_IMG_INFO_AVAILABLE =
            "com.scalefocus.soundvision.ACTION_IMG_INFO_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.scalefocus.soundvision.EXTRA_DATA";
    public final static String DEVICE_DOES_NOT_SUPPORT_IMAGE_TRANSFER =
            "com.scalefocus.soundvision.DEVICE_DOES_NOT_SUPPORT_IMAGE_TRANSFER";

    public static final UUID TX_POWER_UUID = UUID.fromString("00001804-0000-1000-8000-00805f9b34fb");
    public static final UUID TX_POWER_LEVEL_UUID = UUID.fromString("00002a07-0000-1000-8000-00805f9b34fb");
    public static final UUID CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    public static final UUID FIRMWARE_REVISON_UUID = UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb");
    public static final UUID DIS_UUID = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");

    public static UUID DIS_MANUF_UUID = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb"); //ScaleFocus
    public static UUID DIS_MODEL_UUID = UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb");
    public static UUID DIS_SERIAL_UUID = UUID.fromString("00002a25-0000-1000-8000-00805f9b34fb"); //9a938084a36037a4
    public static UUID DIS_HWREV_UUID = UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb"); //v0.0
    public static UUID DIS_HWREV_STRING_UUID = UUID.fromString("00002a27-0000-1000-8000-00805f9b34fb");// : MK2.3 - Hardware Revision String
    public static UUID DIS_SWREV_UUID = UUID.fromString("00002a28-0000-1000-8000-00805f9b34fb"); //e2eff8d694a50b296662f5affce80b79299f0785

    public static final UUID IMAGE_TRANSFER_SERVICE_UUID = UUID.fromString("f86b0001-2eae-4bb0-af49-db1d0322d289");
        public static final UUID RX_CHAR_UUID       = UUID.fromString("f86b0002-2eae-4bb0-af49-db1d0322d289");
        public static final UUID TX_CHAR_UUID       = UUID.fromString("f86b0003-2eae-4bb0-af49-db1d0322d289");
        public static final UUID IMG_INFO_CHAR_UUID = UUID.fromString("f86b0004-2eae-4bb0-af49-db1d0322d289");

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_GATT_CONNECTED);
        intentFilter.addAction(ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(ACTION_DATA_AVAILABLE);
        intentFilter.addAction(ACTION_IMG_INFO_AVAILABLE);
        intentFilter.addAction(DEVICE_DOES_NOT_SUPPORT_IMAGE_TRANSFER);
        return intentFilter;
    }

    public static void startBLETransferService(Context context, IBLETransferClient clientInferface) {
        mClient = clientInferface;
        Intent bindIntent = new Intent(context, BLETransferService.class);
        context.bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
        LocalBroadcastManager.getInstance(context).registerReceiver(UARTStatusChangeReceiver, makeGattUpdateIntentFilter());
    }

    public static void stopBLETransferService(Context context)
    {
        mClient = null;

        try {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(UARTStatusChangeReceiver);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        }
        context.unbindService(mServiceConnection);
    }

    private static final BroadcastReceiver UARTStatusChangeReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null || mClient == null) return;

            final Intent mIntent = intent;

            switch (action) {
                case ACTION_GATT_CONNECTED: mClient.OnConnect();
                    break;

                case ACTION_GATT_DISCONNECTED: mClient.OnDisconnect();
                    break;

                case ACTION_GATT_SERVICES_DISCOVERED:
                    mClient.OnDiscovery();
                    break;

                case ACTION_DATA_AVAILABLE:
                    final byte[] txValue = intent.getByteArrayExtra(BLETransferService.EXTRA_DATA);
                    if (DeviceStats.match(txValue)) {
                        final DeviceStats stats = new DeviceStats(txValue);
                        mClient.OnStatusInfoChange(stats);
                    } else
                    if (ColorScanConfiguration.match(txValue)) {
                        final ColorScanConfiguration stats = new ColorScanConfiguration(txValue);
                        mClient.OnColorScanConfig(stats);
                    } else
                    if (BLEScanAdvertising.match(txValue)) {
                        final BLEScanAdvertising stats = new BLEScanAdvertising(txValue);
                        mClient.OnBLEAdvScan(stats);
                        //Log.i("BLE:6", stats.macAddress+"   - rssi : "+stats.rssi);

                    }


                    else {
                        mClient.OnData(txValue);
                    }

                    break;
            }
        }
    };

    //UART service connected/disconnected
    private static ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
            BLETransferService mService = ((BLETransferService.LocalBinder) rawBinder).getService();
            Log.d(TAG, "onServiceConnected mService= " + mService);
            if (mClient == null) return;
            if (!mService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                mClient.OnServiceError();
            }
            mClient.OnServiceConnect(mService);
        }

        public void onServiceDisconnected(ComponentName classname) {
            if (mClient == null) return;
            mClient.OnServiceDisconnect();
        }
    };

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "mBluetoothGatt = " + mBluetoothGatt );
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);



            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            if(IMG_INFO_CHAR_UUID.equals(characteristic.getUuid())) {
                broadcastUpdate(ACTION_IMG_INFO_AVAILABLE, characteristic);
            }
            else {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status){
            Log.w(TAG, "OnCharWrite");
        }

        private void printVal(BluetoothGattCharacteristic d)
        {
            byte[] val = d.getValue();
            if (val!=null && val.length > 1)
            {
                Log.i("NED", new String(val, StandardCharsets.UTF_8));
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.w(TAG, "OnDescWrite!!!");
/*
            BluetoothGattService disService = mBluetoothGatt.getService(DIS_UUID);
            if (disService != null)
            {
                List<BluetoothGattCharacteristic> c = disService.getCharacteristics();
                if (c != null)
                    for (BluetoothGattCharacteristic ch : c)
                    {
                        readCharacteristic(ch);
                    }
            }
*/
            if(TX_CHAR_UUID.equals(descriptor.getCharacteristic().getUuid())) {
                // When the first notification is set we can set the second
                BluetoothGattService ImageTransferService = mBluetoothGatt.getService(IMAGE_TRANSFER_SERVICE_UUID);
                BluetoothGattCharacteristic ImgInfoChar = ImageTransferService.getCharacteristic(IMG_INFO_CHAR_UUID);
                if (ImgInfoChar == null) {
                    showMessage("Img Info characteristic not found!");
                    broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_IMAGE_TRANSFER);
                    return;
                }
                mBluetoothGatt.setCharacteristicNotification(ImgInfoChar, true);

                BluetoothGattDescriptor descriptor2 = ImgInfoChar.getDescriptor(CCCD);
                descriptor2.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                mBluetoothGatt.writeDescriptor(descriptor2);


                ///////////////////////



            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            Log.w(TAG, "MTU changed: " + String.valueOf(mtu));
        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void printVal(BluetoothGattCharacteristic d)
    {
        byte[] val = d.getValue();
        if (val!=null)
        {
            Log.i("NED", d.getUuid().toString() +" : "+ new String(val, StandardCharsets.UTF_8));
        }
    }

    private void GetNextDISCharacteristic()
    {
        BluetoothGattService disService = mBluetoothGatt.getService(DIS_UUID);
        if (disService != null)
        {
            List<BluetoothGattCharacteristic> c = disService.getCharacteristics();
            if (c != null) {
                for (BluetoothGattCharacteristic ch : c) {
                    if (ch.getValue() == null) {
                        readCharacteristic(ch);
                        return;
                    }
                }
                DISLoaded = true;
            }
        }
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        UUID uuid = characteristic.getUuid();
        if (TX_CHAR_UUID.equals(uuid)) {
            intent.putExtra(EXTRA_DATA, characteristic.getValue());
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        } else if(IMG_INFO_CHAR_UUID.equals(uuid)) {
            intent.putExtra(EXTRA_DATA, characteristic.getValue());
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }  else if(DIS_MANUF_UUID.equals(uuid)
                || DIS_MODEL_UUID.equals(uuid)
                || DIS_SERIAL_UUID.equals(uuid)
                || DIS_HWREV_UUID.equals(uuid)
                || DIS_HWREV_STRING_UUID.equals(uuid)
                || DIS_SWREV_UUID.equals(uuid)
        ) {

            printVal(characteristic);
            DISInfo.put(uuid, new String(characteristic.getValue(), StandardCharsets.UTF_8));
        }
        else {
            printVal(characteristic);
        }

        //if (!DISLoaded)
          //  GetNextDISCharacteristic();


    }

    public class LocalBinder extends Binder {
        public BLETransferService getService() {
            return BLETransferService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }

        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, true, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    public boolean isConnected(){
        return (mConnectionState == STATE_CONNECTED);
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
        DISLoaded = false;
        DISInfo.clear();
        // mBluetoothGatt.close();
    }

    public void requestMtu(int mtu){
        Log.i(TAG, "Requesting 247 byte MTU");
        mBluetoothGatt.requestMtu(mtu);
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        Log.w(TAG, "mBluetoothGatt closed");
        mBluetoothDeviceAddress = null;
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    public List<BluetoothDevice> getBondedDevicesByName(String deviceName) {
        List<BluetoothDevice> ret = new ArrayList<>();
        if (mBluetoothAdapter != null) {
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

                for (BluetoothDevice device : pairedDevices) {
                    String devName = device.getName();
                    if (devName.equals(deviceName))
                    {
                        ret.add(device);
                    }
                }
        }
        return ret;
    }
    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null || characteristic == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Enable TXNotification
     *
     * @return
     */
    public void enableTXNotification()
    {
        Log.w(TAG, "enable TX not.");
        BluetoothGattService ImageTransferService = mBluetoothGatt.getService(IMAGE_TRANSFER_SERVICE_UUID);
        if (ImageTransferService == null) {
            showMessage("Rx service not found!");
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_IMAGE_TRANSFER);
            return;
        }

        BluetoothGattCharacteristic TxChar = ImageTransferService.getCharacteristic(TX_CHAR_UUID);
        if (TxChar == null) {
            showMessage("Tx characteristic not found!");
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_IMAGE_TRANSFER);
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(TxChar, true);

        BluetoothGattDescriptor descriptor = TxChar.getDescriptor(CCCD);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mBluetoothGatt.writeDescriptor(descriptor);


    }

    public void writeRXCharacteristic(byte[] value)
    {
        BluetoothGattService RxService = mBluetoothGatt.getService(IMAGE_TRANSFER_SERVICE_UUID);
        if (RxService == null) {
            showMessage("Rx service not found!");
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_IMAGE_TRANSFER);
            return;
        }
        BluetoothGattCharacteristic RxChar = RxService.getCharacteristic(RX_CHAR_UUID);
        if (RxChar == null) {
            showMessage("Rx characteristic not found!");
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_IMAGE_TRANSFER);
            return;
        }
        RxChar.setValue(value);
        boolean status = mBluetoothGatt.writeCharacteristic(RxChar);

        Log.d(TAG, "write TXchar - status=" + status);
    }

    public void sendCommand(int command, byte []data) {
        byte []pckData;
        if(data == null) {
            pckData = new byte[1];
            pckData[0] = (byte)command;
        }
        else {
            pckData = new byte[1 + data.length];
            pckData[0] = (byte)command;
            for(int i = 0; i < data.length; i++) pckData[i + 1] = data[i];
        }

        writeRXCharacteristic(pckData);
    }

    public void sendCommand(final byte []data) {
        writeRXCharacteristic(data);
    }

    private void showMessage(String msg) {
        Log.e(TAG, msg);
    }
    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }
}