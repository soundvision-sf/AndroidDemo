package com.soundvision.demo.location.flat;


import com.soundvision.demo.location.ffgeojson.Feature;

import java.util.ArrayList;
import java.util.List;

public class Floor
{


    public List<Zone> zones = new ArrayList<Zone>();
    public List<Feature> beacons = new ArrayList<Feature>();
    public List<RouteNode> routeNodes = new ArrayList<RouteNode>();
    public List<RouteNodeLink> routeLinks  = new ArrayList<RouteNodeLink>();
    public List<Feature> POI = new ArrayList<Feature>();



    public int id;
    public double lat;
    public double lon;
    public double x;
    public double y;
    public double z; // 

    public FFFilter filter1;
    public FFFilter filter2;
    public LowPassFilter lowPassFilterX;
    public LowPassFilter lowPassFilterY;

    public int nthMeasurement;
    public String number;
    public String uuid;
    public int widthInCentimeters;
    public int widthInPixels;
    public int heightInPixels;

    public RouteController routeCtrl = new RouteController(this);
    public boolean visible = true;

    public void AddFeature(Feature f)
    {
        if (f.properties.containsKey("type"))
        {
            String type = (String)f.properties.get("type");
            switch (type)
            {
                case "obstacle":
                {
                    Zone z = new Zone();
                    z.id = (int)f.properties.get("fid");
                    z.name = (String)f.properties.get("name");
                    z.type = Feature.Obstacle;
                    z.uuid = "";
                    z.features.add(f);
                    zones.add(z);
                }
                break;
                case "suite":
                {
                    Zone z = new Zone();
                    z.id = (int)((long)f.properties.get("fid"));
                    z.name = (String)f.properties.get("name");
                    z.type = Feature.Suite;
                    z.uuid = "";
                    z.features.add(f);
                    zones.add(z);
                }
                break;
                case "POI":
                {
                    f.type = Feature.POI;
                    POI.add(f);
                }
                break;
                case "room":
                {
                    Zone z = new Zone();
                    z.id = (int)((long)f.properties.get("fid"));
                    z.name = (String)f.properties.get("room");
                    z.type = Feature.Room;
                    z.uuid = "";
                    z.features.add(f);
                    zones.add(z);
                }
                break;
                case "route":
                {
                    routeCtrl.ImportLinks(f);
                }
                break;
                case "beacon":
                {
                    f.type = Feature.Beacon;
                    beacons.add(f);

                }
                break;
            }
        }
    }

}
