package com.soundvision.demo;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import com.kynetics.uf.android.api.UFServiceCommunicationConstants;
import com.kynetics.uf.android.api.UFServiceConfiguration;
import com.kynetics.uf.android.api.UFServiceMessage;
import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.distribute.Distribute;
import com.scalefocus.soundvision.ble.BLETransferService;
import com.scalefocus.soundvision.ble.IBLETransferClient;
import com.scalefocus.soundvision.ble.data.BLEScanAdvertising;
import com.scalefocus.soundvision.ble.data.ColorScanConfiguration;
import com.scalefocus.soundvision.ble.data.DataSegment;
import com.scalefocus.soundvision.ble.data.DeviceStats;
import com.soundvision.demo.dfu.AppCenterUpdateListener;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.content.Intent.FLAG_INCLUDE_STOPPED_PACKAGES;
import static com.kynetics.uf.android.api.UFServiceCommunicationConstants.MSG_AUTHORIZATION_RESPONSE;
import static com.kynetics.uf.android.api.UFServiceCommunicationConstants.SERVICE_DATA_KEY;
import static com.scalefocus.soundvision.ble.BLETransferService.BleCommand.GetBleParams;
import static com.scalefocus.soundvision.ble.BLETransferService.startBLETransferService;

public class BaseActivity extends AppCompatActivity implements IBLETransferClient {

    private static final String TAG = "BLE.DEMO";

    private Button btnColor;

    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;
    private static final int PERMISSION_REQUEST_BACKGROUND_LOCATION = 2;
    private static final int RC_PERMISSION_WRITE_EXTERNAL_STORAGE = 3;

    private static final int REQUEST_SELECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    private TextView textConnected;
    private ImageView ble_icon;
    TextView modeLabel;

    private ProgressDialog mConnectionProgDialog;

    private BLETransferService mService = null;
    private BluetoothDevice mDevice = null;
    private BluetoothAdapter mBtAdapter = null;
    private Button btnConnectDisconnect;
    private Button btnNav;
    private Map<String, DataSegment> sessionDataList = new HashMap<>();

    private boolean mBleConnected = false;

    private float x1,x2;
    static final int MIN_DISTANCE = 150;

    class FragmentItem {
        Fragment fragment;
        String navText;
        IBLETransferClient client;
        public FragmentItem(Fragment fragment, String navText) {
            this.fragment = fragment;
            this.navText = navText;
            client = (IBLETransferClient)fragment;
        }
    }

    private List<FragmentItem> fragmentList = new ArrayList<>();

    private byte colScanMode = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Distribute.setListener(new AppCenterUpdateListener());
        Distribute.setEnabled(true);
        AppCenter.start(getApplication(), "53264e19-b237-4886-b8ed-c71d55435528", Distribute.class);

        textConnected = findViewById(R.id.textConnected2);
        ble_icon = findViewById(R.id.ble_icon2);


        init();

        fragmentList.add( new FragmentItem(new MainFragment(), "Statistics"));
        fragmentList.add( new FragmentItem(new ColorCtrlFragment(), "Colors Ctrl."));
        fragmentList.add( new FragmentItem(new DeviceFirmwareUpgradeFragment(), "FW Upgrade"));
        fragmentList.add( new FragmentItem(new LocationFragment(), "Location"));

        switchTo(fragmentList.get(0));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        /*
        switch(event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                x1 = event.getX();
                break;
            case MotionEvent.ACTION_UP:
                x2 = event.getX();
                float deltaX = x2 - x1;
                if (Math.abs(deltaX) > MIN_DISTANCE)
                {
                    switchToNext(deltaX > 0 ? 1 : -1);
                }
                break;
        }*/
        return super.onTouchEvent(event);
    }

    private void permissionSet()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                //
            } else {
                if (!this.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            PERMISSION_REQUEST_FINE_LOCATION);
                }
                else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons.  Please go to Settings -> Applications -> Permissions and grant location access to this app.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }

            }

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, RC_PERMISSION_WRITE_EXTERNAL_STORAGE);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, RC_PERMISSION_WRITE_EXTERNAL_STORAGE);
            }
        }

    }
    private void switchTo(FragmentItem item)
    {
        if (findViewById(R.id.include) != null) {
            modeLabel.setText(item.navText);
            btnNav.setTag(fragmentList.indexOf(item));
            item.fragment.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.include, item.fragment).commit();
        }

    }

    private void connectToDevice()
    {
        /*
        if (mService != null)
        {
            List<BluetoothDevice> boundedList = mService.getBondedDevicesByName(BLETransferService.SoundVisionDeviceName);
            if (boundedList.size() == 1) {
                mService.connect(boundedList.get(0).getAddress());
                return;
            }
        }
*/
        Intent newIntent = new Intent(BaseActivity.this, DeviceListActivity.class);
        startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);

    }

    private void init()
    {

        initBLE();

        permissionSet();

        mConnectionProgDialog = new ProgressDialog(this);
        mConnectionProgDialog.setTitle("Connecting...");
        mConnectionProgDialog.setCancelable(false);

        startBLETransferService(this, this);

        btnConnectDisconnect    = (Button) findViewById(R.id.btBTConnect);
        btnConnectDisconnect.setText("Connect");
        btnConnectDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mBtAdapter.isEnabled()) {
                    Log.i(TAG, "onClick - BT not enabled yet");
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                } else {
                    if (!mBleConnected) {
                        connectToDevice();
                    } else {
                        //Disconnect button pressed
                        if (mDevice != null) {
                            mService.disconnect();
                        }
                    }
                }
            }
        });

        modeLabel = findViewById(R.id.tbCurrentMode);

        btnNav = findViewById(R.id.btnNext);
        btnNav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchToNext(1);
                //int lastIndex = ((Integer)btnNav.getTag() + 1) % fragmentList.size();
                //switchTo(fragmentList.get(lastIndex));
            }
        });

        Button btnPrev = findViewById(R.id.btnPrev);
        btnPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchToNext(-1);
                //int lastIndex = ((Integer)btnNav.getTag() + fragmentList.size() - 1) % fragmentList.size();
               // switchTo(fragmentList.get(lastIndex));
            }
        });
    }

    private void switchToNext(int dir)
    {
        int lastIndex = ((Integer)btnNav.getTag() + fragmentList.size() + dir) % fragmentList.size();
        switchTo(fragmentList.get(lastIndex));
    }

    private void initBLE()
    {
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBtAdapter = bluetoothManager.getAdapter();

        if (mBtAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
    }

    private void setConnectionState(boolean connected)
    {
        mBleConnected = connected;
        textConnected.setText(connected ? "Connected" : "-");
        ble_icon.setAlpha(connected ? 1.0f : 0.5f);
        btnConnectDisconnect.setText(connected ? "Disconnect" : "Connect");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_SELECT_DEVICE:
                //When the DeviceListActivity return, with the selected device address
                if (resultCode == Activity.RESULT_OK && data != null) {
                    String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
                    mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);

                    Log.d(TAG, "... onActivityResultdevice.address==" + mDevice + "mserviceValue" + mService);
                    //((TextView) findViewById(R.id.deviceName)).setText(mDevice.getName()+ " - connecting");
                    mService.connect(deviceAddress);

                    mConnectionProgDialog.show();
                }
                break;

            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, "Bluetooth has turned on ", Toast.LENGTH_SHORT).show();

                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, "Problem in BT Turning ON ", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;

            default:
                Log.e(TAG, "wrong request code");
                break;
        }
    }

    public void updatePalette(byte[][][] pall)
    {
        //
    }

    public void sendCommand(byte[] cmd)
    {
        if (mBleConnected && (mService != null)) {
            mService.sendCommand(cmd);
        }
    }

    public void sendCommand(byte cmd, byte[] data)
    {
        if (mBleConnected && (mService != null)) {
            ByteBuffer bb = ByteBuffer.allocate(1+data.length);
            bb.put(cmd);
            bb.put(data);
            mService.sendCommand(bb.array());
        }
    }

    /*******************************************************************************************
     *
     * BLETransferClient
     *
     *******************************************************************************************/
    @Override
    public void OnServiceConnect(BLETransferService bleTransferService) {
        mService = bleTransferService;
        for (FragmentItem f : fragmentList) f.client.OnServiceConnect(bleTransferService);
    }

    @Override
    public void OnServiceDisconnect() {
        mService = null;
    }

    @Override
    public void OnServiceError() {
        mService = null;
    }

    @Override
    public void OnConnect() {
        setConnectionState(true);
        mConnectionProgDialog.hide();
        for (FragmentItem f : fragmentList)
            if (f.fragment instanceof DeviceConnectionEventListener) {
                ((DeviceConnectionEventListener)f.fragment).OnDeviceConnect(mDevice);
            }
    }

    @Override
    public void OnDisconnect() {
        mConnectionProgDialog.hide();
        setConnectionState(false);
        for (FragmentItem f : fragmentList)
            if (f instanceof DeviceConnectionEventListener) {
                ((DeviceConnectionEventListener)f).OnDeviceDisconnect();
            }
    }

    @Override
    public void OnDiscovery() {
        mService.enableTXNotification();
        mService.sendCommand(GetBleParams.ordinal(), null);
        for (FragmentItem f : fragmentList) f.client.OnDiscovery();
    }

    @Override
    public void OnData(byte[] bytes) {
        for (FragmentItem f : fragmentList) f.client.OnData(bytes);
    }

    @Override
    public void OnStatusInfoChange(DeviceStats deviceStats) {

        final DeviceStats devstats = deviceStats;
        runOnUiThread(new Runnable() {
            public void run() {
                textConnected.setText(devstats.macAddress);
            }
        });

        for (FragmentItem f : fragmentList) f.client.OnStatusInfoChange(deviceStats);
    }

    @Override
    public void OnColorScanConfig(ColorScanConfiguration stats) {
        for (FragmentItem f : fragmentList) f.client.OnColorScanConfig(stats);
    }

    @Override
    public void OnBLEAdvScan(BLEScanAdvertising stats) {
        for (FragmentItem f : fragmentList) f.client.OnBLEAdvScan(stats);
    }


    private Messenger mHawkBitService;
    private boolean mIsBound = false;

    private ServiceConnection mHawkBitConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            mHawkBitService = new Messenger(service);

            Toast.makeText(BaseActivity.this, R.string.connected,
                    Toast.LENGTH_SHORT).show();
            try {
                Message msg = Message.obtain(null,
                        UFServiceCommunicationConstants.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mHawkBitService.send(msg);
            } catch (RemoteException e) {
                Toast.makeText(BaseActivity.this, "service communication error",
                        Toast.LENGTH_SHORT).show();
            }
            mIsBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }

        @Override
        public void onBindingDied(ComponentName name) {

        }

        @Override
        public void onNullBinding(ComponentName name) {

        }

    };

    void doBindService() {
        final Intent intent = new Intent(UFServiceCommunicationConstants.SERVICE_ACTION);
        intent.setPackage(UFServiceCommunicationConstants.SERVICE_PACKAGE_NAME);
        intent.setFlags(FLAG_INCLUDE_STOPPED_PACKAGES);
        final boolean serviceExist = bindService(intent, mHawkBitConnection, Context.BIND_AUTO_CREATE);
        if(!serviceExist){
            Toast.makeText(getApplicationContext(), "UpdateFactoryService not found",Toast.LENGTH_LONG).show();
            unbindService(mHawkBitConnection);
            this.finish();
        }
    }

    private void ConfigureService()
    {
        HashMap<String,String> targetAttributes = new HashMap<>(2);
        targetAttributes.put("attribute1","attributeValue1");
        targetAttributes.put("attribute2","attributeValue2");
        final Bundle data = new Bundle();
        data.putSerializable(SERVICE_DATA_KEY, UFServiceConfiguration.builder()
                .withControllerId("controllerId")
                .withTenant("tenant")
                .withUrl("https://personal.updatefactory.io")
                .withTargetAttributes(targetAttributes)
                .build());

        Message msg = Message.obtain(null,
                UFServiceCommunicationConstants.MSG_CONFIGURE_SERVICE);
        msg.replyTo = mMessenger;
        msg.setData(data);
    }

    private void sendPermissionResponse(boolean response){
        Message msg = Message.obtain(null, MSG_AUTHORIZATION_RESPONSE);
        msg.getData().putBoolean(SERVICE_DATA_KEY, response);
        try {
            mHawkBitService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    Messenger mMessenger = new Messenger(new HandlerReplyMsg());
    class HandlerReplyMsg extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            String recdMessage = msg.obj.toString(); //msg received from service
            Toast.makeText(BaseActivity.this, "Response Fetched", Toast.LENGTH_LONG).show();

            final Serializable serializable = msg.getData().getSerializable(SERVICE_DATA_KEY);
            if(!(serializable instanceof UFServiceConfiguration)) {
                UFServiceMessage messageObj = (UFServiceMessage)serializable;
            }

        }
    }

}