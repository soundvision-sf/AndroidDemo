package com.soundvision.demo;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.scalefocus.soundvision.ble.BLETransferService;
import com.scalefocus.soundvision.ble.IBLETransferClient;
import com.scalefocus.soundvision.ble.data.BLEScanAdvertising;
import com.scalefocus.soundvision.ble.data.ColorScanConfiguration;
import com.scalefocus.soundvision.ble.data.DeviceStats;
import com.soundvision.demo.altbeacon.IAltBeaconEventListener;
import com.soundvision.demo.location.BLELocator;
import com.soundvision.demo.location.ffgeojson.PointD;
import com.soundvision.demo.location.flat.BeaconProp;
import com.soundvision.demo.location.ibeacon.IBeacon;
import com.soundvision.demo.ui.BeaconViewItem;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BeaconScanFragment extends Fragment implements IBLETransferClient, IAltBeaconEventListener {

    private ListView beaconview;
    private View padDlg;

    private TextView rssiView;
    private TextView tvMAC;
    private TextView tvInfo;
    private Spinner spinner;

    private BLELocator locator;
    private BeaconAdapter adapter;
    private float x1, x2;
    private float y1, y2;
    private double mDist = 0.0f;
    private int mode = 0;
    private int selected = -1;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_beacon_list, container, false);

        return v;
    }

    public class BeaconAdapter extends BaseAdapter {
        // View lookup cache
        List<IBeacon> list = new ArrayList<>();
        Context mContext;

        private class ViewHolder {
            //TextView job_name, job_location, date, packages, job_type;
            BeaconViewItem beaconView;
        }

        public BeaconAdapter(Context context) {
            mContext = context;
        }

        private IBeacon getBeacon(String mac)
        {
            for (IBeacon b : list)
            {
                if (b.mac.equals(mac)) return b;
            }
            return null;
        }

        public void setList(List<IBeacon> updateList)
        {
            for (IBeacon b : updateList)
            {
                IBeacon lb = getBeacon(b.mac);
                if (lb == null)
                    list.add(b);

            }
            //this.list = list;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            if (list == null) return 0;
            return list.size();
        }

        @Override
        public Object getItem(int position) {
            return list.get(position);
        }

        public IBeacon getBeacon(int position) {
            return (IBeacon)list.get(position) ;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Get the data item for this position
            final IBeacon beacon = (IBeacon) getItem(position);
            Log.d("Amit2", "reached adapter getView");

            ViewHolder viewHolder; // view lookup cache stored in tag
            if (convertView == null) {
                viewHolder = new ViewHolder();
                LayoutInflater inflater = LayoutInflater.from(mContext);
                convertView = inflater.inflate(R.layout.beacon_view, parent, false);
                viewHolder.beaconView = (BeaconViewItem) convertView.findViewById(R.id.beaconlistview);
                convertView.setTag(viewHolder);
                Log.d("Amit", "set adapter view");
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
                Log.d("Amit", " not set adapter view");
            }
            // Populate the data into the template view using the data object
            viewHolder.beaconView.setBeacon(beacon);
            return convertView;
        }
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        beaconview = getView().findViewById(R.id.beaconlistview);
        ImageButton btnCenter = getView().findViewById(R.id.imageButtonCenter);
        padDlg = getView().findViewById(R.id.padDlg);

        adapter = new BeaconAdapter(getContext());
        beaconview.setAdapter(adapter);

        beaconview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
               if (padDlg.getVisibility() != View.VISIBLE )
               {
                   selected = position;
                   IBeacon b = adapter.getBeacon(position);
                   tvMAC.setText(b.mac);
                   tvInfo.setText("("+b.rssiList.getCount()+") scans:"+b.scanCount);

                   padDlg.setVisibility(View.VISIBLE);
               }
            }
        });

        getView().findViewById(R.id.btClose).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                padDlg.setVisibility(View.INVISIBLE);
                selected = -1;
            }
        });

        getView().findViewById(R.id.btSaveDist).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selected>=0) {
                    IBeacon b = adapter.getBeacon(selected);
                    saveToFile(b);
                }
            }
        });

        rssiView = getView().findViewById(R.id.tv_rssi);
        tvMAC = getView().findViewById(R.id.tvMAC);
        tvInfo = getView().findViewById(R.id.tvInfo);
        spinner = getView().findViewById(R.id.spinnerDIst);


        locator = BLELocator.INSTANCE(getContext());
        locator.listener = this;

        super.onViewCreated(view, savedInstanceState);
    }

    private void saveToFile(IBeacon b)
    {

        try {
            File f=new File("/sdcard/Download", "RSSI-"+b.mac+".table");

            updateFile(f, spinner.getSelectedItem().toString()+"="+b.getRssi(), spinner.getSelectedItem().toString()+"=") ;

        }
        catch (Exception e) {

        }
    }

    private List<String> updateFile(File f, String line, String prefix) {

        List<String> ret = new ArrayList<String>();
        boolean update = true;
        try {
            //File f=new File("/sdcard/Download", b.mac+".rssi");
            InputStream inputStream = new FileInputStream(f);

            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    if (receiveString.startsWith(prefix)) {
                        ret.add(line);
                        update = false;
                    } else
                        ret.add(receiveString);
                }
                inputStream.close();
            }
        }
        catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
        } catch (Exception e) {
            Log.e("login activity", "Can not read file: " + e.toString());
        }

        if (update)
            ret.add(line);

        try {
            FileOutputStream osr = new FileOutputStream(f);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(osr);
            for(String str: ret) {
                outputStreamWriter.write(str+"\n");
            }
            outputStreamWriter.close();
        }
        catch (Exception e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }

        return ret;
    }

    @Override
    public void OnServiceConnect(BLETransferService service) {

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

    }

    @Override
    public void OnStatusInfoChange(DeviceStats stats) {

    }

    @Override
    public void OnColorScanConfig(ColorScanConfiguration stats) {

    }



    @Override
    public void OnBLEAdvScan(BLEScanAdvertising stats) {
        if (beaconview == null) return;
        locator.addReport(stats);
        OnLocationUpdate(new PointD(0, 0));
        /*
        PointD loc = locator.getLocation(ffview.selArea.beacons);
        if (loc != null)
            ffview.setUserLocation(loc, loc != null);
        else
            ffview.setUserValidPos(false);*/
    }

    @Override
    public void onStart() {
        super.onStart();


    }

    @Override
    public void OnLocationUpdate(PointD pt) {
        if (beaconview == null) return;

        if (selected>=0 && padDlg.getVisibility() == View.VISIBLE) {
            IBeacon b = adapter.getBeacon(selected);
            float rssi = b.getRssi();
            rssiView.setText(""+rssi);
            double distance = Math.pow(10d, ((double) (-55) - (rssi)) / (10 * 2))*100;
            tvInfo.setText("("+b.rssiList.getCount()+") scans:"+b.scanCount+" dist:"+distance);
        }

        List<IBeacon> list = locator.getBeaconsList();
        adapter.setList(list);


    }
}
