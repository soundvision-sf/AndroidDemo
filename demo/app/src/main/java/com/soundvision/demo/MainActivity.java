package com.soundvision.demo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.scalefocus.soundvision.ble.BLETransferService;
import com.scalefocus.soundvision.ble.BLETransferClient;
import com.scalefocus.soundvision.ble.DeviceListActivity;
import com.scalefocus.soundvision.ble.data.DataSegment;
import com.scalefocus.soundvision.ble.data.DeviceStats;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.scalefocus.soundvision.ble.BLETransferService.*;
import static com.scalefocus.soundvision.ble.BLETransferService.BleCommand.GetBleParams;

public class MainActivity extends AppCompatActivity implements BLETransferClient {

    private static final String TAG = "BLE.DEMO";
    ////////////////////////////  BLE

    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;
    private static final int PERMISSION_REQUEST_BACKGROUND_LOCATION = 2;
    private static final int RC_PERMISSION_WRITE_EXTERNAL_STORAGE = 3;


    private static final int REQUEST_SELECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int UART_PROFILE_CONNECTED = 20;
    private static final int UART_PROFILE_DISCONNECTED = 21;

    private int SVN_DATA_TYPE_JPEG = 0x4A504547;

    private ProgressDialog mConnectionProgDialog;

    private BLETransferService mService = null;
    private BluetoothDevice mDevice = null;
    private BluetoothAdapter mBtAdapter = null;
    private Button btnConnectDisconnect;
    private Button btnGetImage, btnGetColor;
    private Map<String, DataSegment> sessionDataList = new HashMap<>();

    private boolean mBleConnected = false;
    private TextView textConnected;
    private ImageView ble_icon;

    private TextView tvDist;
    private TextView tvColor[] = new TextView[8];
    private TextView tvCamera;
    private TextView tvID;

    private ProgressBar mDownloadProgress;
    private TextView tvDataId;
    private TextView tvDataProgress;

    private ImageView imageView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        textConnected = findViewById(R.id.textConnected);
        ble_icon = findViewById(R.id.ble_icon);

        tvDist = findViewById(R.id.valDistance);

        tvColor[0] = findViewById(R.id.valColor1);
        tvColor[1] = findViewById(R.id.valColor2);
        tvColor[2] = findViewById(R.id.valColor3);
        tvColor[3] = findViewById(R.id.valColor4);

        tvColor[4] = findViewById(R.id.valColor5);
        tvColor[5] = findViewById(R.id.valColor6);
        tvColor[6] = findViewById(R.id.valColor7);
        tvColor[7] = findViewById(R.id.valColor8);

        tvCamera = findViewById(R.id.valCamera);
        tvID = findViewById(R.id.valStat4);

        mDownloadProgress = findViewById(R.id.DownloadProgress);
        tvDataId = findViewById(R.id.lbDataID);
        tvDataProgress = findViewById(R.id.lbDataProgress);

        imageView = findViewById(R.id.ivDataImage);

        init();
    }

    private void init()
    {
        initBLE();

        permissionSet();

        mConnectionProgDialog = new ProgressDialog(this);
        mConnectionProgDialog.setTitle("Connecting...");
        mConnectionProgDialog.setCancelable(false);

        startBLETransferService(this, this);

        btnConnectDisconnect    = (Button) findViewById(R.id.btnConnectDisconnect);
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
                        Intent newIntent = new Intent(MainActivity.this, DeviceListActivity.class);
                        startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
                    } else {
                        //Disconnect button pressed
                        if (mDevice != null) {
                            mService.disconnect();
                        }
                    }
                }
            }
        });

        btnGetImage    = (Button) findViewById(R.id.btnGetImage);
        btnGetImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    if (mBleConnected && (mDevice != null)) {
                       mService.sendCommand(BleCommand.GetPicture.ordinal(), null);
                    }
            }
        });

        btnGetColor= (Button) findViewById(R.id.btnGetColor);
        btnGetColor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mBleConnected && (mDevice != null)) {
                    mService.sendCommand(BleCommand.GetColor.ordinal(), null);
                }
            }
        });

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

    private void setConnectionState(boolean connected)
    {
        mBleConnected = connected;
        textConnected.setText(connected ? "Connected" : "-");
        ble_icon.setAlpha(connected ? 1.0f : 0.5f);
        btnConnectDisconnect.setText(connected ? "Disconnect" : "Connect");
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        //super.onActivityResult(requestCode, resultCode, data);
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


    /*******************************************************************************************
    *
    * BLETransferClient
    *
    *******************************************************************************************/
    @Override
    public void OnServiceConnect(BLETransferService bleTransferService) {
        mService = bleTransferService;
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
        mConnectionProgDialog.hide();
    }

    @Override
    public void OnDisconnect() {
        mConnectionProgDialog.hide();
    }

    @Override
    public void OnDiscovery() {
        mService.enableTXNotification();
        mService.sendCommand(GetBleParams.ordinal(), null);
        setConnectionState(true);
    }


    private void saveToFile(final byte[] data, String fName)
    {
        File photo=new File("/sdcard/Download", fName+".jpg");
        try {
            FileOutputStream fos=new FileOutputStream(photo.getPath());

            fos.write(data);
            fos.close();
        }
        catch (java.io.IOException e) {
            Log.e("PictureDemo", "Exception in photoCallback", e);
        }
    }

    @Override
    public void OnData(byte[] bytes) {
        final byte[] txValue = bytes;
        runOnUiThread(new Runnable() {
            public void run() {

                String session = DataSegment.UID(txValue);
                DataSegment segment;
                if (sessionDataList.containsKey(session)) {
                    segment = sessionDataList.get(session);
                    segment.parse(txValue);
                    Log.w(TAG, "segment : data"+segment.getCurrentSize());
                    mDownloadProgress.setMax(segment.getLength());
                    mDownloadProgress.setProgress(segment.getCurrentSize());
                    tvDataProgress.setText("" + segment.getCurrentSize()+"/"+segment.getLength());
                } else {
                    segment = new DataSegment(txValue);
                    sessionDataList.put(session, segment);
                    mDownloadProgress.setProgress(0);
                    tvDataId.setText("ses : " + segment.getSessionId());
                }

                mDownloadProgress = findViewById(R.id.DownloadProgress);
                tvDataId = findViewById(R.id.lbDataID);


                // valid segment
                {
                    mService.sendCommand(BleCommand.SegmentDataConfirm.ordinal(), segment.toBytes());
                }

                if (segment.isReady()) {
                    Log.w(TAG, "attempting JPEG decode");
                    mDownloadProgress.setProgress(0);
                    if (SVN_DATA_TYPE_JPEG == segment.getType()) {
                        try {
                            byte[] jpgHeader = new byte[]{-1, -40, -1, -32};
                            byte[] jpgHeaderOld = new byte[]{-1, -40, -1, -2};
                            boolean isValid = false;
                            if (Arrays.equals(jpgHeader, Arrays.copyOfRange(segment.getData(), 0, 4))) {
                                Bitmap bitmap = BitmapFactory.decodeByteArray(segment.getData(), 0, segment.getData().length);
                                imageView.setImageBitmap(bitmap);
                                isValid = true;
                            } else
                            if (Arrays.equals(jpgHeaderOld, Arrays.copyOfRange(segment.getData(), 0, 4))) {
                                Bitmap bitmap = BitmapFactory.decodeByteArray(segment.getData(), 0, segment.getData().length);
                                imageView.setImageBitmap(bitmap);
                                isValid = true;
                            } else {
                                Log.w(TAG, "JPG header missing!! Image data corrupt.");
                            }

                            if (isValid)
                            saveToFile(segment.getData(), "Session_"+segment.getSessionId());

                        } catch (Exception e) {
                            Log.w(TAG, "Bitmapfactory fail :(");
                        }
                    }
                    sessionDataList.remove(session);
                }

            }
        });
    }

    @Override
    public void OnStatusInfoChange(DeviceStats deviceStats) {
        final DeviceStats stats = deviceStats;
        runOnUiThread(new Runnable() {
            public void run() {
                textConnected.setText(stats.macAddress);
                tvDist.setText(""+stats.distance);
                for (int i=0;i<8;i++) {
                    tvColor[i].setText("" + stats.color_count[i]);
                    tvColor[i].setBackgroundColor(stats.color[i] | 0xff000000);
                }
                tvCamera.setText(""+stats.camera);
                tvID.setText("-");
            }
        });
    }
}
