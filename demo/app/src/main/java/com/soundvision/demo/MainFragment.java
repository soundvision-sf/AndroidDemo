package com.soundvision.demo;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.scalefocus.soundvision.ble.BLETransferService;
import com.scalefocus.soundvision.ble.IBLETransferClient;
import com.scalefocus.soundvision.ble.data.BLEScanAdvertising;
import com.scalefocus.soundvision.ble.data.ColorScanConfiguration;
import com.scalefocus.soundvision.ble.data.DataSegment;
import com.scalefocus.soundvision.ble.data.DeviceStats;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_UP;

public class MainFragment extends Fragment implements IBLETransferClient {

    private static final String TAG = "BLE.DEMO";
    ////////////////////////////  BLE

    private int SVN_DATA_TYPE_JPEG = 0x4A504547;

    final String[] resolutions = new String[]{"160x120", "320x240", "640x480", "1024x768", "1280x720", "1280x960", "1920x1080"};
    final int[] resolution_code_list = new int[]{0x22, 0x11, 0x00, 0x33, 0x44, 0x55, 0x66};

    private BLETransferService mService = null;
    private Button btnGetImage, btnGetColor;
    private Map<String, DataSegment> sessionDataList = new HashMap<>();

    private boolean mBleConnected = false;
    public int last_color_value = 0;

    private TextView tvDist, tvBattery, tvBrightness;
    private TextView tvColor[] = new TextView[8];

    private ImageButton btTop, btRight, btDown, btLeft, btEnter;

    private TextView tvCamera;
    private TextView tvID;
    private TextView tvStatsInfo;

    private ProgressBar mDownloadProgress;
    private TextView tvDataId;
    private TextView tvDataProgress;

    private ImageView imageView;
    private int LED1Progress = 0;
    private byte resolution_code = 00;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    private byte cmd_code = 0;
    Handler mHandler = new Handler();

    class DataSender implements Runnable {
        ArrayList<byte[]> taskList = new ArrayList<byte[]>();
        @Override
        public void run() {
            try {
                if (mBleConnected && (mService != null)) {
                    byte[] t = task();
                    mService.sendCommand(t);
                }
            } finally {

            }
        }

        synchronized private byte[] task()
        {
            if (taskList.size() == 0) return null;
            byte[] ret = taskList.get(0);
            taskList.remove(0);
            return ret;
        }

        synchronized public void send(byte code)
        {
            taskList.add(new byte[]{code});
            mHandler.post(mDataSender);
        }

        synchronized public void send(byte code, byte p1)
        {
            taskList.add(new byte[]{code, p1});
            mHandler.post(mDataSender);
        }

        synchronized public void send(byte code, byte data[])
        {
            ByteBuffer bb = ByteBuffer.allocate(1+data.length);
            bb.put(code);
            bb.put(data);
            taskList.add(bb.array());
            mHandler.post(mDataSender);
        }

        synchronized public void send(byte code, byte p1, byte p2)
        {
            taskList.add(new byte[]{code, p1, p2});
            mHandler.post(mDataSender);
        }

        synchronized public void sendDelayed(byte code, byte p1, byte p2, int delay)
        {
            taskList.add(new byte[]{code, p1, p2});
            mHandler.postDelayed(mDataSender, delay);
        }

        synchronized public void sendDelayed(byte code, int delay)
        {
            taskList.add(new byte[]{code});
            mHandler.postDelayed(mDataSender, delay);
        }

        synchronized public void skip()
        {
            task();
            mHandler.removeCallbacks(mDataSender);
        }

    };

    DataSender mDataSender = new DataSender();

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {


        tvDist = getView().findViewById(R.id.valDistance);

        tvColor[0] = getView().findViewById(R.id.valColor1);
        tvColor[1] = getView().findViewById(R.id.valColor2);
        tvColor[2] = getView().findViewById(R.id.valColor3);
        tvColor[3] = getView().findViewById(R.id.valColor4);

        tvColor[4] = getView().findViewById(R.id.valColor5);
        tvColor[5] = getView().findViewById(R.id.valColor6);
        tvColor[6] = getView().findViewById(R.id.valColor7);
        tvColor[7] = getView().findViewById(R.id.valColor8);

        btTop = getView().findViewById(R.id.btUp);
        btRight = getView().findViewById(R.id.btRight);
        btDown = getView().findViewById(R.id.btDown);
        btLeft = getView().findViewById(R.id.btLeft);
        btEnter = getView().findViewById(R.id.btEnter);
        tvBattery = getView().findViewById(R.id.tvBattery);
        tvBrightness = getView().findViewById(R.id.tvBrightness);

        btTop.setTag(SV_KEY_UP);
        btRight.setTag(SV_KEY_RIGHT);
        btLeft.setTag(SV_KEY_LEFT);
        btDown.setTag(SV_KEY_DOWN);
        btEnter.setTag(SV_KEY_ENTER);

        View.OnTouchListener h = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                int k = ((Integer) v.getTag());
                cmd_code = (byte) k;

                if (mBleConnected && (mService != null)) {
                    switch (event.getAction()) {
                        case ACTION_UP:
                            mDataSender.skip();
                            mDataSender.send((byte)BLETransferService.BleCommand.SendControlCode.ordinal(), cmd_code, (byte) 0);
                            break;
                        case ACTION_DOWN:
                            mDataSender.send((byte)BLETransferService.BleCommand.SendControlCode.ordinal(), cmd_code, (byte) 1);
                            mDataSender.sendDelayed((byte)BLETransferService.BleCommand.SendControlCode.ordinal(), cmd_code, (byte) 2,300);
                            break;
                    }
                }
                return true;
            }

            ;
        };

        btTop.setOnTouchListener(h);
        btRight.setOnTouchListener(h);
        btLeft.setOnTouchListener(h);
        btDown.setOnTouchListener(h);
        btEnter.setOnTouchListener(h);

        tvStatsInfo = getView().findViewById(R.id.tvStatsInfo);

        tvCamera = getView().findViewById(R.id.valCamera);
        tvID = getView().findViewById(R.id.valStat4);

        mDownloadProgress = getView().findViewById(R.id.DownloadProgress);
        tvDataId = getView().findViewById(R.id.lbDataID);
        tvDataProgress = getView().findViewById(R.id.lbDataProgress);

        imageView = getView().findViewById(R.id.ivDataImage);

        ((SeekBar) getView().findViewById(R.id.sbLed1Seek)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                LED1Progress = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mBleConnected && (mService != null)) {
                    byte v = (byte) (LED1Progress % 256);
                    mDataSender.send((byte)BLETransferService.BleCommand.SendLedValue.ordinal(), (byte)0, v);
                    //mService.sendCommand(BLETransferService.BleCommand.SendLedValue.ordinal(), new byte[]{0, v}); // first LED idx + value
                }
            }
        });

        // spinner

        Spinner spinner = getView().findViewById(R.id.spinner_res);

        ArrayAdapter<String> adapter0 = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, resolutions);
        adapter0.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
        spinner.setAdapter(adapter0);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (mBleConnected && (mService != null)) {
                    resolution_code = (byte) resolution_code_list[(position % 7)];
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        btnGetImage = getView().findViewById(R.id.btBLEGetImage);
        btnGetImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mBleConnected && (mService != null)) {
                    mDataSender.send((byte)BLETransferService.BleCommand.ChangeResolution.ordinal(), resolution_code);
                    mDataSender.sendDelayed((byte)BLETransferService.BleCommand.GetPicture.ordinal(), 100);
                }
            }
        });

        btnGetColor = getView().findViewById(R.id.btBLEColorScan);
        btnGetColor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mBleConnected && (mService != null)) {
                    mDataSender.send((byte)BLETransferService.BleCommand.GetColor.ordinal());
                }
            }
        });

    }

    private void setConnectionState(boolean connected)
    {
        mBleConnected = connected;

        if (connected) {
            List<BluetoothGattService> list = mService.getSupportedGattServices();
            if (list != null)
                for (BluetoothGattService serv : list)
                {
                    List<BluetoothGattCharacteristic> c = serv.getCharacteristics();
                    if (c != null)
                        for (BluetoothGattCharacteristic ch : c)
                        {
                            List<BluetoothGattDescriptor> desc = ch.getDescriptors();
                            if (desc != null)
                                for (BluetoothGattDescriptor d : desc) {
                                    byte[] val = d.getValue();
                                    if (val!=null && val.length > 1)
                                    {
                                        Log.i("NED", new String(val, StandardCharsets.UTF_8));
                                    }
                                }
                        }
                }
        }
    }

    @Override
    public void OnServiceConnect(BLETransferService service) {
        mService = service;


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



    }

    @Override
    public void OnDisconnect() {

    }

    @Override
    public void OnDiscovery() {
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
    public void OnData(byte[] data) {
        if (getActivity() == null) return;
        final byte[] txValue = data;
        getActivity().runOnUiThread(new Runnable() {
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

                mDownloadProgress = getView().findViewById(R.id.DownloadProgress);
                tvDataId = getView().findViewById(R.id.lbDataID);


                // valid segment
                {
                    //mService.sendCommand(BLETransferService.BleCommand.SegmentDataConfirm.ordinal(), segment.toBytes());
                    mDataSender.send((byte)BLETransferService.BleCommand.SegmentDataConfirm.ordinal(), segment.toBytes());
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

                            if (isValid) {
                                SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US);
                                Date now = new Date();
                                String fileName = formatter.format(now) + "_"+ segment.getSessionId();
                                saveToFile(segment.getData(), fileName );
                            }

                        } catch (Exception e) {
                            Log.w(TAG, "Bitmapfactory fail :(");
                        }
                    }
                    sessionDataList.remove(session);
                }

            }
        });
    }

    private String getResolutionText(int code)
    {
        int i = Math.max(0, Arrays.asList(resolution_code_list).indexOf(code));
        return resolutions[i];
    }

    @Override
    public void OnStatusInfoChange(DeviceStats stats) {
        if (getActivity() == null) return;
        final DeviceStats devstats = stats;
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                tvDist.setText(""+((devstats.distance >> 2) & 0x07FF)+" mm");
                for (int i=0;i<8;i++) {
                    tvColor[i].setText("" + devstats.color_count[i]);
                    tvColor[i].setBackgroundColor(devstats.color[i] | 0xff000000);
                }

                tvCamera.setText(getResolutionText(devstats.camera));

                if (last_color_value != devstats.color[0]) {
                    last_color_value = devstats.color[0];
                    tvStatsInfo.setText("scan color - R:"+Integer.toHexString((last_color_value >> 16) & 0xff)+" , G:"+Integer.toHexString((last_color_value >> 8) & 0xff)+" , B:"+Integer.toHexString((last_color_value) & 0xff));
                }

                int enter_state = Math.max(0, Math.min(255, devstats.buttonMask >> 16));

                int[] keyValues = new int[]{0,0,0,0,enter_state};

                int key = devstats.buttonMask & 0xff;
                int value = (devstats.buttonMask & 0xff00) >> 8;

                switch (key) {
                    case IBLETransferClient.SV_KEY_LEFT:
                        keyValues[0] = value;
                        break;
                    case IBLETransferClient.SV_KEY_RIGHT:
                        keyValues[1] = value;
                        break;
                    case IBLETransferClient.SV_KEY_UP:
                        keyValues[2] = value;
                        break;
                    case IBLETransferClient.SV_KEY_DOWN:
                        keyValues[3] = value;
                        break;

                    case IBLETransferClient.SV_KEY_ENTER:
                        break;
                }

                btTop.setPressed(keyValues[2] > 0);
                btLeft.setPressed(keyValues[0] > 0);
                btRight.setPressed(keyValues[1] > 0);
                btDown.setPressed(keyValues[3] > 0);
                btEnter.setPressed(keyValues[4] > 0);

                tvBattery.setText("l:"+devstats.battery_level+" s:"+devstats.battery_status);
                tvBrightness.setText(""+devstats.brightness);

                tvID.setText("I:"+devstats.compass_inclination +" H:"+devstats.compass_heading +" C:"+devstats.compass_direction);

            }
        });
    }

    @Override
    public void OnColorScanConfig(ColorScanConfiguration stats) {
        //
    }

    @Override
    public void OnBLEAdvScan(BLEScanAdvertising stats) {

    }

}