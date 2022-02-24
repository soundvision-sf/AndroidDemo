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

            switch (type.toLowerCase())
            {
                case "obstacle":
                {
                    Zone z = new Zone();
                    int FID = 0;
                    try {
                        FID = Integer.valueOf(f.properties.get("fid").toString());
                    } catch (Exception e)
                    {
                        FID = (int)Double.parseDouble(f.properties.get("fid").toString());
                    }
                    z.id = FID;

                    z.name = (String)f.properties.get("name");
                    z.type = Feature.Obstacle;
                    f.properties.put("type", Feature.Obstacle);
                    f.type = Feature.Obstacle;
                    z.uuid = "";
                    z.features.add(f);
                    zones.add(z);
                }
                break;
                case "suite":
                {
                    Zone z = new Zone();
                    int FID = 0;
                    try {
                        FID = Integer.valueOf(f.properties.get("fid").toString());
                    } catch (Exception e)
                    {
                        FID = (int)Double.parseDouble(f.properties.get("fid").toString());
                    }
                    z.id = FID;
                    z.name = (String)f.properties.get("name");
                    z.type = Feature.Suite;
                    f.type = Feature.Suite;
                    z.uuid = "";
                    z.features.add(f);
                    zones.add(z);
                }
                break;
                case "poi":
                {
                    f.type = Feature.POI;
                    POI.add(f);
                }
                break;
                case "room":
                {
                    f.properties.put("type", Feature.Room);
                    Zone z = new Zone();
                    if (f.properties.containsKey("fid"))
                    {
                        try
                        {
                            z.id = (int)((long)f.properties.get("fid"));
                        } catch (Exception e)
                        {
                            z.id = (int)((double)f.properties.get("fid"));
                        }
                    }
                    if (f.properties.containsKey("name"))
                        z.name = (String)f.properties.get("name");
                    else
                    if (f.properties.containsKey("room"))
                        z.name = (String)f.properties.get("room");
                    else
                    if (f.properties.containsKey("alias"))
                        z.name = (String)f.properties.get("alias");

                    z.type = Feature.Room;
                    z.uuid = "";
                    z.features.add(f);
                    zones.add(z);
                }
                break;
                case "route":
                {
                    f.type = Feature.Room;
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
