package com.soundvision.demo.location.ffgeojson;

import java.util.List;

public class GeometryPoint implements IGeometry
{
    public List<Double> coordinates;
    private String Ftype;

    public double distance;

    public GeometryPoint()
    {
        Ftype = Geometry.Point;
          /*  coordinates = new List<Double>();
            coordinates.Add(0);
            coordinates.Add(0);*/
    }
    @Override
    public String type() {
        return Ftype;
    }

    public GeometryPoint(List<Double> coords)
    {
        Ftype = Geometry.Point;
        coordinates = coords;
    }

    public RectangleF bounds()
    {
        FFrect rect = new FFrect(9999999, 9999999, -99999990, -99999990);

        if (rect.Left > coordinates.get(0)) rect.Left = coordinates.get(0);
        if (rect.Top > coordinates.get(1)) rect.Top = coordinates.get(1);
        if (rect.Right < coordinates.get(0)) rect.Right = coordinates.get(0);
        if (rect.Bottom < coordinates.get(1)) rect.Bottom = coordinates.get(1);

        return new RectangleF((float)(rect.Left), (float)(rect.Top), (float)((rect.Right - rect.Left)), ((float)(rect.Bottom - rect.Top)));
    }

    public void Move(double x, double y, double z)
    {
        coordinates.set(0, coordinates.get(0) + x);
        coordinates.set(1, coordinates.get(1) + y);
        if (coordinates.size() > 2)
            coordinates.set(2, coordinates.get(2) + z);
    }



}
