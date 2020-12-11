package com.soundvision.demo;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.scalefocus.soundvision.ble.BLETransferClient;
import com.scalefocus.soundvision.ble.BLETransferService;
import com.scalefocus.soundvision.ble.data.ColorScanConfiguration;
import com.scalefocus.soundvision.ble.data.DeviceStats;
import com.soundvision.demo.palette.palBase16;
import com.soundvision.demo.ui.ColorView;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

public class ColorCtrlFragment extends Fragment implements BLETransferClient {

    private LinearLayout layoutColors;
    private Button btReset;
    private Button btGet;
    private Button btUpdate;

    private BLETransferService mService = null;

    private palBase16 palette = new palBase16();

    private ColorView colorView;

    private TextView vWhite;
    private TextView vBlack;
    private TextView vGray;
    private TextView vLight;
    private TextView vDark;

    private SeekBar trWhite;
    private SeekBar trBlack;
    private SeekBar trGray;
    private SeekBar trLight;
    private SeekBar trDark;

    private ColorScanConfiguration stats;
    private TextView hueName;
    private SeekBar seekBarCol;
    private boolean trackUpdate = true;


    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_color, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init();

        vWhite = getView().findViewById(R.id.vWhite);
        vBlack = getView().findViewById(R.id.vBlack);
        vGray = getView().findViewById(R.id.vGray);
        vLight = getView().findViewById(R.id.vLight);
        vDark = getView().findViewById(R.id.vDark);

        trWhite = getView().findViewById(R.id.trWhite);
        trBlack = getView().findViewById(R.id.trBlack);
        trGray = getView().findViewById(R.id.trGray);
        trLight = getView().findViewById(R.id.trLight);
        trDark = getView().findViewById(R.id.trDark);

        SeekBar.OnSeekBarChangeListener chL = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int tag = Integer.parseInt((String)seekBar.getTag());
                switch (tag) {
                    case 1:vBlack.setText(""+progress+"%");
                        break;
                    case 2:vWhite.setText(""+progress+"%");
                        break;
                    case 3:vGray.setText(""+progress+"%");
                        break;
                    case 4:vLight.setText(""+progress+"%");
                        break;
                    case 5:vDark.setText(""+progress+"%");
                        break;
                }

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        };

        trWhite.setOnSeekBarChangeListener(chL);
        trBlack.setOnSeekBarChangeListener(chL);
        trGray.setOnSeekBarChangeListener(chL);
        trLight.setOnSeekBarChangeListener(chL);
        trDark.setOnSeekBarChangeListener(chL);

        colorView = getView().findViewById(R.id.colorView);
        hueName = getView().findViewById(R.id.tvHueName);
        trackUpdate = true;
        seekBarCol = getView().findViewById(R.id.seekBarCol);
        seekBarCol.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (trackUpdate || !fromUser) return;
                colorView.setPosition(colorView.getHueIndex(), progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        getView().findViewById(R.id.btColInc).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                seekBarCol.incrementProgressBy(1);
            }
        });

        getView().findViewById(R.id.btColDec).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                seekBarCol.incrementProgressBy(-1);
            }
        });

        trackUpdate = false;

        setColorViewPosition(colorView.getHueIndex());

    }

    public void init()
    {
        layoutColors = getView().findViewById(R.id.layoutColors);
        //initColorBars();

        btReset = getView().findViewById(R.id.btReset);
        btGet = getView().findViewById(R.id.btGet);
        btUpdate  = getView().findViewById(R.id.btUpdate);

        btReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        btGet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                byte[] cmd = {(byte)BLETransferService.BleCommand.GetColorPalette.ordinal()};
                ((BaseActivity)getActivity()).sendCommand(cmd);
            }
        });

        btUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (stats == null) return;
                stats.white = trWhite.getProgress();
                stats.black = trBlack.getProgress();
                stats.gray = trGray.getProgress();
                stats.light = trLight.getProgress();
                stats.dark = trDark.getProgress();
               ((BaseActivity)getActivity()).sendCommand((byte)BLETransferService.BleCommand.SetColorPalette.ordinal(), stats.toData());
            }
        });

        getView().findViewById(R.id.btHuePrev).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setColorViewPosition(colorView.getHueIndex()-1);
            }
        });

        getView().findViewById(R.id.btHueNext).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setColorViewPosition(colorView.getHueIndex()+1);
            }
        });

    }

    private void setColorViewPosition(int index)
    {
        colorView.setHueIndex(index);
        hueName.setText(colorView.getColorName(colorView.getHueIndex()));
        int[] ranges = {0,0,0};
        colorView.getHueRange(colorView.getHueIndex(), ranges);
        if (ranges[1]>ranges[2]) ranges[1] = ranges[1] - 360;
        if (ranges[0]>ranges[1]) ranges[0] = ranges[0] - 360;
        trackUpdate = true;
        seekBarCol.setMin(ranges[0]);
        seekBarCol.setMax(ranges[2]);
        seekBarCol.setProgress(ranges[1]);
        trackUpdate = false;
    }

    private void initColorBars()
    {
        int[] colorsList = palette.getPaletteColors();
        for (int i = 0; i< 1; i++)
        {
            SeekBar bar = new SeekBar(getContext());
            layoutColors.addView(bar);
            ViewGroup.LayoutParams params = bar.getLayoutParams();
            params.width = MATCH_PARENT;
            bar.setLayoutParams(params);
            bar.setTag(new Integer(i));
            bar.setMax(360);
            bar.setMin(0);
            //bar.setThumbTintList(ColorStateList.valueOf(colorsList[i] | 0xff000000));
            int c = colorsList[i];
            bar.setBackgroundColor(Color.rgb((c >> 16) & 0xff, (c >> 8) & 0xff, c & 0xff));

            bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    Integer idx = (Integer)seekBar.getTag();
                    colorView.setPosition(0, progress);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });

        }
    }

    @Override
    public void OnServiceConnect(BLETransferService service) {
        mService = service;
    }

    @Override
    public void OnServiceDisconnect() {

    }

    @Override
    public void OnServiceError() {

    }

    @Override
    public void OnConnect() {

    }

    @Override
    public void OnDisconnect() {

    }

    @Override
    public void OnDiscovery() {

    }

    @Override
    public void OnData(byte[] data) {
        if (getActivity() == null) return;
    }

    @Override
    public void OnStatusInfoChange(DeviceStats stats) {
        if (getActivity() == null) return;
    }

    @Override
    public void OnColorScanConfig(ColorScanConfiguration stats) {
       this.stats = stats;
        trWhite.setProgress(stats.white);
        trBlack.setProgress(stats.black);
        trGray.setProgress(stats.gray);
        trLight.setProgress(stats.light);
        trDark.setProgress(stats.dark);
    }
}