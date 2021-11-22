package com.soundvision.demo.location.ffgeojson;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Feature
{
    public static final String Room = "Room";
    public static final String Zone = "Zone";
    public static final String POI = "POI";
    public static final String Route = "Route";
    public static final String Beacon = "Beacon";
    public static final String Obstacle = "Obstacle";
    public static final String Suite = "Suite";

    //private Map<String, Object> Fproperties = new HashMap<String, Object>();

    public String type;
    public Map<String, Object> properties = new HashMap<String, Object>();
    public IGeometry geometry;

    public boolean visible = true;

    public Feature()
    {
        type = "feature";
        geometry = new GeometryPolygon();
    }

    public String property(String key)
    {
        if (properties.containsKey(key)) {
            return (String)properties.get(key);
        }
        return "";
    }

    public void Scale(double dx, double dy, double dz)
    {
        switch (geometry.type())
        {
            case Geometry.Point:
            {
                GeometryPoint gp = (GeometryPoint)geometry;

                gp.coordinates.set(0, gp.coordinates.get(0) * dx);
                gp.coordinates.set(1, gp.coordinates.get(1) * dy);
            }
            break;
            case Geometry.LineString:
                GeometryLineString gl = (GeometryLineString)geometry;

                for (List<Double> coords : gl.coordinates)
                {
                    coords.set(0, coords.get(0) * dx);
                    coords.set(1, coords.get(1) * dy);
                }

                break;
            case Geometry.Polygon:
            {
                GeometryPolygon gp = (GeometryPolygon)geometry;

                for (List<List<Double>> coords : gp.coordinates)
                {
                    for (int i = 0; i < coords.size(); i++)
                    {
                        coords.get(i).set(0, coords.get(i).get(0) * dx);
                        coords.get(i).set(1, coords.get(i).get(1) * dy);
                    }
                }

            }
            break;

        }
    }
    public void Move(double x, double y, double z)
    {

        //for (Feature feature : features)
        {
            switch (geometry.type())
            {
                case Geometry.Point:
                {
                    GeometryPoint gp = (GeometryPoint)geometry;

                    gp.coordinates.set(0, gp.coordinates.get(0) + x);
                    gp.coordinates.set(1, gp.coordinates.get(1) + y);
                    if (gp.coordinates.size()>2)
                        gp.coordinates.set(2, gp.coordinates.get(2) + z);
                }
                break;
                case Geometry.LineString:
                    GeometryLineString gl = (GeometryLineString)geometry;

                    for (List<Double> coords : gl.coordinates)
                    {
                        coords.set(0, coords.get(0) + x);
                        coords.set(1, coords.get(1) + y);
                        if (coords.size() > 2)
                            coords.set(2, coords.get(2) + z);
                    }

                    break;
                case Geometry.Polygon:
                {
                    GeometryPolygon gp = (GeometryPolygon)geometry;

                    for (List<List<Double>> coords : gp.coordinates)
                    {
                        for (int i = 0; i < coords.size(); i++)
                        {
                            coords.get(i).set(0, coords.get(i).get(0) + x);
                            coords.get(i).set(1, coords.get(i).get(1) + y);
                            if (coords.size() > 2)
                                coords.get(i).set(2, coords.get(i).get(2) + z);
                        }
                    }

                }
                break;

            }
        }
    }
}

