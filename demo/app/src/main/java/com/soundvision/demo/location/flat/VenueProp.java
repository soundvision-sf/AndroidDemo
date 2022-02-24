package com.soundvision.demo.location.flat;

import com.soundvision.demo.location.ffgeojson.FFrect;
import com.soundvision.demo.location.ffgeojson.Feature;
import com.soundvision.demo.location.ffgeojson.GeometryPoint;
import com.soundvision.demo.location.ffgeojson.GeometryPolygon;
import com.soundvision.demo.location.ffgeojson.PointD;
import com.soundvision.demo.location.ffgeojson.RectangleF;

import java.util.ArrayList;
import java.util.List;

public class VenueProp
{
    public List<BeaconProp> beacons = new ArrayList<BeaconProp>();
    public List<Floor> floors = new ArrayList<Floor>();

    public Feature desc = new Feature();
    
    public int id;

    public double lat;

    public double lon;
 
    public double angle;

    public double scale;
     
    public double mapOffset;
  
    public String name;
  
    public int singleRoomMajor;
 
    public String uuid;
    
    public void setMapOffset(double dx, double dy)
    {
        for (Floor floor : floors)
        {
            for (Zone zone : floor.zones)
            for (Feature f : zone.features)
            {
                f.Move(dx, dy, 0);
            }

            for (Feature f : floor.beacons)
            f.Move(dx, dy, 0);
            for (Feature f : floor.POI)
            f.Move(dx, dy, 0);
        }

    }

    public FFrect bounds()
    {
        return bounds(new FFrect(9999999, 9999999, -99999990, -99999990));
    }
    public FFrect bounds(FFrect inRect)
    {
        if (inRect==null) return bounds();
        FFrect rect = inRect;
        for (Floor floor : floors)
        {
            for (Zone zone : floor.zones) {
                RectangleF rc = zone.bounds();
                if (rect.Left > rc.X) rect.Left = rc.X;
                if (rect.Top > rc.Y) rect.Top = rc.Y;
                if (rect.Right < rc.Right()) rect.Right = rc.Right();
                if (rect.Bottom < rc.Bottom()) rect.Bottom = rc.Bottom();
            }
            for (Feature f : floor.beacons)
            {
                RectangleF rc = f.geometry.bounds();
                if (rect.Left > rc.X) rect.Left = rc.X;
                if (rect.Top > rc.Y) rect.Top = rc.Y;
                if (rect.Right < rc.Right()) rect.Right = rc.Right();
                if (rect.Bottom < rc.Bottom()) rect.Bottom = rc.Bottom();
            }
            for (Feature f : floor.POI)
            {
                RectangleF rc = f.geometry.bounds();
                if (rect.Left > rc.X) rect.Left = rc.X;
                if (rect.Top > rc.Y) rect.Top = rc.Y;
                if (rect.Right < rc.Right()) rect.Right = rc.Right();
                if (rect.Bottom < rc.Bottom()) rect.Bottom = rc.Bottom();
            }
        }
        return rect;
    }

    public void setMapScale(double dx, double dy)
    {
        for (Floor floor : floors)
        {
            for (Zone zone : floor.zones)
            for (Feature f : zone.features)
            {
                f.Scale(dx, dy, 0);
            }
            for (Feature f : floor.beacons)
            f.Scale(dx, dy, 0);
            for (Feature f : floor.POI)
            f.Scale(dx, dy, 0);
        }
    }

    public double AngleToDistanceCM(double lat1, double lon1, double lat2, double lon2)
    {  // generally used geo measurement function
        double R = 6378.137; // Radius of earth : KM
        double dLat = lat2 * Math.PI / 180 - lat1 * Math.PI / 180;
        double dLon = lon2 * Math.PI / 180 - lon1 * Math.PI / 180;
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double d = R * c;
        return d * 100000.0; // meters
    }

    public Floor getFloor(String number)
    {
        for (Floor f : floors)
        {
            if (f.number.equals(number)) return f;
        }
        Floor floor = new Floor();
        floor.number = number;
        floors.add(floor);
        return floor;
    }

    private PointD rotatePoint(PointD p, double angle)
    {
        //if (angle == 0.0) return p;
        return new PointD(((Math.cos(angle) * (p.X) - Math.sin(angle) * (p.Y))),
                (((Math.sin(angle) * (p.X) + Math.cos(angle) * (p.Y)))));
    }
    
    public void convertAngleToCM()
    {
        double x = Double.MAX_VALUE;
        double y = Double.MAX_VALUE;

        double rotX = Double.MIN_VALUE;
        double rotY = Double.MIN_VALUE;


        for (Floor floor : floors)
        {
            for (Zone z : floor.zones)
            {
                for (Feature f : z.features)
                {
                    GeometryPolygon gp = (GeometryPolygon)f.geometry;
                    for (List<List<Double>> coords : gp.coordinates)
                    {
                        for (int i = 0; i < coords.size(); i++)
                        {
                            if (coords.get(i).get(0) < x)
                            {
                                x = Math.min(x, coords.get(i).get(0));
                                y = Math.min(y, coords.get(i).get(1));

                                int idxRot = (i + 1) % coords.size();

                                rotX = coords.get(idxRot).get(0);
                                rotY = coords.get(idxRot).get(1);

                            }
                        }
                    }
                }
            }

        }

        lat = y;
        lon = x;

        PointD rotC = MathMap.computePixel(x, y, rotX, rotY);

        double angleRot = -Math.atan2(rotC.Y, rotC.X);

        if (angle != 0.0)
        {
            angleRot = -angle;
        }

        double grad = MathMap.toDeg(angleRot);


        if (grad != 0 && Math.abs(grad) != 90)
        {
            angle = -angleRot;
        }


        if (desc != null)
        {
            GeometryPolygon gp = (GeometryPolygon)desc.geometry;
            if (gp.coordinates != null)
                for (List<List<Double>> coords : gp.coordinates)
            {
                for (int i = 0; i < coords.size(); i++)
                {
                    PointD p = rotatePoint(MathMap.computePixel(x, y, coords.get(i).get(0), coords.get(i).get(1)), angleRot);
                    coords.get(i).set(0, p.X);// getDistance(x, y, coords[i][0], y);
                    coords.get(i).set(1, p.Y);// -getDistance(x, y, x, coords[i][1]);
                }
            }

/*
            for (Feature f : exits)
            {
                GeometryPoint gpt = (GeometryPoint)f.geometry;
                PointD ep = rotatePoint(MathMap.computePixel(x, y, gpt.coordinates[0], gpt.coordinates[1]), angleRot);
                gpt.coordinates[0] = ep.X;//getDistance(x, y, gp.coordinates[0], y);
                gpt.coordinates[1] = ep.Y;//-getDistance(x, y, x, gp.coordinates[1]);

            }
*/
        }


        for (Floor floor : floors)
        {
            for (Zone z : floor.zones)
            {
                for (Feature f : z.features)
                {
                    GeometryPolygon gp = (GeometryPolygon)f.geometry;
                    for (List<List<Double>> coords : gp.coordinates)
                    {
                        for (int i = 0; i < coords.size(); i++)
                        {
                            PointD p = rotatePoint(MathMap.computePixel(x, y, coords.get(i).get(0), coords.get(i).get(1)), angleRot);//getPixelScale(x, y, coords[i][0], coords[i][1]);
                            coords.get(i).set(0, p.X);// getDistance(x, y, coords[i][0], y);
                            coords.get(i).set(1, p.Y);// -getDistance(x, y, x, coords[i][1]);
                        }
                    }
                }
            }

            for (Feature f : floor.POI)
            {
                GeometryPoint gp = (GeometryPoint)f.geometry;
                PointD p = rotatePoint(MathMap.computePixel(x, y, gp.coordinates.get(0), gp.coordinates.get(1)), angleRot);
                gp.coordinates.set(0, p.X);//getDistance(x, y, gp.coordinates[0], y);
                gp.coordinates.set(1, p.Y);//-getDistance(x, y, x, gp.coordinates[1]);

            }

            for (Feature f : floor.beacons)
            {
                GeometryPoint gp = (GeometryPoint)f.geometry;
                PointD p = rotatePoint(MathMap.computePixel(x, y, gp.coordinates.get(0), gp.coordinates.get(1)), angleRot);
                gp.coordinates.set(0, p.X);//getDistance(x, y, gp.coordinates[0], y);
                gp.coordinates.set(1, p.Y);// -getDistance(x, y, x, gp.coordinates[1]);

            }

            for (RouteNode rn : floor.routeNodes)
            {
                PointD p = rotatePoint(MathMap.computePixel(x, y, rn.coordinate.X, rn.coordinate.Y), angleRot);
                rn.coordinate = new PointD(p.X, p.Y);//getDistance(x, y, rn.coordinate.X, y);
                //rn.coordinate.Y = p.Y;//-getDistance(x, y, x, rn.coordinate.Y);
            }

        }

    }

    public void convertAngleToCM2()
    {
        double x = Double.MAX_VALUE;
        double y = Double.MAX_VALUE;

        for (Floor floor : floors)
        {
            for (Zone z : floor.zones)
            {
                for (Feature f : z.features)
                {
                    GeometryPolygon gp = (GeometryPolygon)f.geometry;
                    for (List<List<Double>> coords : gp.coordinates)
                    {
                        for (int i = 0; i < coords.size(); i++)
                        {
                            x = Math.min(x, coords.get(i).get(0));
                            y = Math.min(y, coords.get(i).get(1));
                        }
                    }
                }
            }

            for (Zone z : floor.zones)
            {
                for (Feature f : z.features)
                {
                    GeometryPolygon gp = (GeometryPolygon)f.geometry;
                    for (List<List<Double>> coords : gp.coordinates)
                    {
                        for (int i = 0; i < coords.size(); i++)
                        {
                            coords.get(i).set(0, AngleToDistanceCM(x, y, coords.get(i).get(0), y));
                            coords.get(i).set(1, -AngleToDistanceCM(x, y, x, coords.get(i).get(1)));
                        }
                    }
                }
            }

            for (Feature f : floor.POI)
            {
                GeometryPoint gp = (GeometryPoint)f.geometry;
                gp.coordinates.set(0, AngleToDistanceCM(x, y, gp.coordinates.get(0), y));
                gp.coordinates.set(1, -AngleToDistanceCM(x, y, x, gp.coordinates.get(1)));
            }

            for (Feature f : floor.beacons)
            {
                GeometryPoint gp = (GeometryPoint)f.geometry;
                gp.coordinates.set(0, AngleToDistanceCM(x, y, gp.coordinates.get(0), y));
                gp.coordinates.set(1, -AngleToDistanceCM(x, y, x, gp.coordinates.get(1)));
            }

            for (RouteNode rn : floor.routeNodes)
            {
                rn.coordinate.X = AngleToDistanceCM(x, y, rn.coordinate.X, y);
                rn.coordinate.Y = -AngleToDistanceCM(x, y, x, rn.coordinate.Y);
            }

        }
    }

    public void importFeature(Feature f)
    {
        if (f == null) return;
        if (f.properties.containsKey("floor"))
        {
            Floor floor = getFloor((String)f.properties.get("floor"));
            floor.AddFeature(f);
        }
        else {
            if (f.properties.containsKey("type") && f.properties.get("type").equals("building"))
            {
                desc = f;
                angle = (double)f.properties.get("angle");
            }
           /* if (f.properties.ContainsKey("type") && f.properties["type"].Equals("building"))
            {
                FDesc = f;
                id = f.properties.ContainsKey("fid") ? Convert.ToInt32( f.properties["fid"]) : 0;
                lon = f.properties.ContainsKey("lon") ? Convert.ToDouble(f.properties["lon"]) : 0;
                lat = f.properties.ContainsKey("lat") ? Convert.ToDouble(f.properties["lat"]) : 0;
                angle = f.properties.ContainsKey("angle") ? Convert.ToDouble(f.properties["angle"]) : 0;
                name = f.properties.ContainsKey("name") ? Convert.ToString(f.properties["name"]) : "";
            }*/
            else
            if (f.properties.containsKey("type") && f.properties.get("type").equals("floor exit"))
            {
                f.type = Feature.Exit;
                //FExit.Add(f);
            }
        }
    }

    public void buildBeaconList(int floorStart, int floorEnd)
    {
        beacons.clear();
        for (Floor floor : floors)
        {
            //if (floor.number >= floorStart && floor.number <= floorEnd)
            {
                for (Feature f : floor.beacons) {
                    GeometryPoint gp = (GeometryPoint) f.geometry;
                    if (f.properties.containsKey("fid")) {
                        String id = (String) f.properties.get("fid");
                        beacons.add(new BeaconProp(gp.coordinates.get(0), gp.coordinates.get(1), id));
                    }
                }
            }

        }
    }

    public void updateBeaconProps(List<BeaconProp> beacons)
    {
        for (Floor floor : floors)
        {
            //if (floor.number >= floorStart && floor.number <= floorEnd)
            {
                for (Feature f : floor.beacons) {
                    GeometryPoint gp = (GeometryPoint) f.geometry;
                    if (f.properties.containsKey("fid")) {
                        String id = (String) f.properties.get("fid");

                        for (BeaconProp b : beacons)
                        {
                            if (b.mac.equals(id))
                            {
                                gp.distance = b.distance;
                                break;
                            }
                        }
                        //beacons.add(new BeaconProp(gp.coordinates.get(0), gp.coordinates.get(1), id));

                    }
                }
            }

        }
    }

}