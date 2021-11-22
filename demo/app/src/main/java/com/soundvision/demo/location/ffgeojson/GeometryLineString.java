package com.soundvision.demo.location.ffgeojson;

import java.util.List;

public
class GeometryLineString implements IGeometry
{
    public List<List<Double>> coordinates;
    private String Ftype;

    public GeometryLineString()
    {
        Ftype = Geometry.LineString;
    }

    public GeometryLineString(List<List<Double>> coords)
    {
        Ftype = Geometry.LineString;
        coordinates = coords;
    }

    @Override
    public String type() {
        return Ftype;
    }

    public RectangleF bounds()
    {
        FFrect rect = new FFrect(9999999, 9999999, -99999990, -99999990);
        for (List<Double> coords : coordinates)
        {
            //for (int i = 0; i < coords.size(); i++)
            {

                if (rect.Left > coords.get(0)) rect.Left = coords.get(0);
                if (rect.Top > coords.get(1)) rect.Top = coords.get(1);
                if (rect.Right < coords.get(0)) rect.Right = coords.get(0);
                if (rect.Bottom < coords.get(1)) rect.Bottom = coords.get(1);
            }
        }
        return new RectangleF((float)(rect.Left), (float)(rect.Top), (float)((rect.Right - rect.Left)), ((float)(rect.Bottom - rect.Top)));
    }
    
    public void Move(double x, double y, double z)
    {
        if (coordinates != null)
            for (List<Double> coords : coordinates)
            {
                coords.set(0, coords.get(0) + x);
                coords.set(1, coords.get(1) + y);
                if (coords.size() > 2)
                    coords.set(2, coords.get(2) + z);
            }

    }

}

