package com.soundvision.demo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.scalefocus.soundvision.ble.BLETransferService;
import com.scalefocus.soundvision.ble.IBLETransferClient;
import com.scalefocus.soundvision.ble.data.BLEScanAdvertising;
import com.scalefocus.soundvision.ble.data.ColorScanConfiguration;
import com.scalefocus.soundvision.ble.data.DeviceStats;
import com.soundvision.demo.altbeacon.IAltBeaconEventListener;
import com.soundvision.demo.location.BLELocator;
import com.soundvision.demo.location.ffgeojson.FFrect;
import com.soundvision.demo.location.ffgeojson.GeometryDeserializer;
import com.soundvision.demo.location.ffgeojson.IGeometry;
import com.soundvision.demo.location.ffgeojson.NaturalDeserializer;
import com.soundvision.demo.location.ffgeojson.PointD;
import com.soundvision.demo.location.flat.VenueProp;
import com.soundvision.demo.ui.FFView;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.Map;

public class LocationFragment extends Fragment implements IBLETransferClient, IAltBeaconEventListener {

    private FFView ffview;
    private BLELocator locator;
    private float x1, x2;
    private float y1, y2;
    private double mDist = 0.0f;
    private int mode = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.location_fragment, container, false);

        v.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {


                try {
                    if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
                        mode = 0;
                    } else if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_DOWN) {
                        x1 = event.getX();
                        y1 = event.getY();
                        mode = 1;
                    } else if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_MOVE) {
                        if (mode == 1) {
                            PointD pt = ffview.getOffset();
                            pt = pt.Offset(event.getX() - x1, event.getY() - y1);
                            ffview.setOffset(pt);
                            x1 = event.getX();
                            y1 = event.getY();
                        } else if (mode == 2) {
                            x1 = event.getX(0);
                            y1 = event.getY(0);
                            x2 = event.getX(1);
                            y2 = event.getY(1);
                            double nDist = Math.sqrt(((x2 - x1) * (x2 - x1) + ((y2 - y1) * (y2 - y1))));
                            ffview.setScale(ffview.getScale() + ((nDist - mDist) / 5000.0f));
                            mDist = nDist;
                        }
                    } else if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_POINTER_DOWN) {
                        x1 = event.getX(0);
                        y1 = event.getY(0);
                        x2 = event.getX(1);
                        y2 = event.getY(1);
                        mDist = Math.sqrt(((x2 - x1) * (x2 - x1) + ((y2 - y1) * (y2 - y1))));
                        mode = 2;
                    }
                } catch (Exception e)
                {
                    e.printStackTrace();
                }

                return true;
            }
        });
        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        ffview = getView().findViewById(R.id.mapView);
        ImageButton btnCenter = getView().findViewById(R.id.imageButtonCenter);

        btnCenter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ffview.centerUser(true);
            }
        });

        loadImages();

        try {
            //InputStream wStream = getContext().getAssets().open("Infinity8.json");
            InputStream wStream = getContext().getAssets().open("i8.area");
            loadFromJsonString(wStream);

        } catch (IOException e) {
            e.printStackTrace();
        }

        locator = new BLELocator(getContext());
        locator.listener = this;
        locator.beaconsPropListFilter = ffview.selArea.beacons;

        super.onViewCreated(view, savedInstanceState);
    }

    private Bitmap GetIcon(int res)
    {
        //return BitmapFactory.decodeResource(getResources(), res);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        return BitmapFactory.decodeResource(getResources(),  res, options );
    }

    private void loadImages()
    {

        ffview.icons.put("", GetIcon(R.drawable.location));

        ffview.icons.put("Location",GetIcon(R.drawable.location));
        ffview.icons.put("Door",GetIcon(R.drawable.door));
        ffview.icons.put("Beacon",GetIcon(R.drawable.beacon));
        ffview.icons.put("Desk",GetIcon(R.drawable.desk));

        ffview.icons.put("Exit",GetIcon(R.drawable.exit));
        ffview.icons.put("Flowerpot",GetIcon(R.drawable.flowerpot));
        ffview.icons.put("Lift",GetIcon(R.drawable.lift));
        ffview.icons.put("Reception",GetIcon(R.drawable.reception));
        ffview.icons.put("Rest",GetIcon(R.drawable.rest_area));

        ffview.icons.put("Smoking",GetIcon(R.drawable.smoking));
        ffview.icons.put("Stairs",GetIcon(R.drawable.stairs));
        ffview.icons.put("WC female",GetIcon(R.drawable.wc_female));
        ffview.icons.put("WC Male",GetIcon(R.drawable.wc_male));

        ffview.icons.put("ManMark", GetIcon(R.drawable.group78));
        ffview.icons.put("ManMark2", GetIcon(R.drawable.group79));
        ffview.icons.put("RouteDir", GetIcon(R.drawable.group85));

        ffview.iconBeacon = ffview.icons.get("Beacon");// GetIcon(R.drawable.Beacon);
        ffview.iconPOI = ffview.icons.get("Location");// GetIcon(R.drawable.Door);

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
        if (ffview == null) return;
        ffview.setUserDirection(stats.compass_direction);
    }

    @Override
    public void OnColorScanConfig(ColorScanConfiguration stats) {

    }



    @Override
    public void OnBLEAdvScan(BLEScanAdvertising stats) {
        if (ffview == null) return;
        locator.addReport(stats);
        PointD loc = locator.getLocation(ffview.selArea.beacons);
        if (loc != null)
            ffview.setUserLocation(loc, loc != null);
              else
            ffview.setUserValidPos(false);
    }

    private void loadFromJsonString(InputStream inputStream)
    {
        VenueProp[] list = null;
        try {
            GsonBuilder b = new GsonBuilder();
            b.registerTypeAdapter(IGeometry.class, new GeometryDeserializer());
            Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
            b.registerTypeAdapter(mapType, new NaturalDeserializer());

            Gson gson = b.create();
            list = gson.fromJson(new InputStreamReader(inputStream, "UTF-8"), VenueProp[].class);
        } catch (Exception e) {
            e.printStackTrace();
        }

        ffview.Clear();
        FFrect rect = null;
        if (list != null) {
            for (VenueProp area : list) {
                ffview.addArea(area);
                area.buildBeaconList(0, 999);
                rect = area.bounds(rect);
            }
        }

        // center
        double w = rect.Width(); //* ffview.getScale();
        double h = rect.Height(); //* ffview.getScale();
        //ffview.setOffset(new PointD(-( (w/2.0)), (h/2.0)));


    }

    @Override
    public void onStart() {
        super.onStart();


    }

    @Override
    public void OnLocationUpdate(PointD pt) {
        if (ffview == null) return;
        ffview.setUserLocation(pt, pt != null);
    }
}
