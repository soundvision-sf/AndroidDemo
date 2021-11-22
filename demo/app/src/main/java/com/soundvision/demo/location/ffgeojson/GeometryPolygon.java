package com.soundvision.demo.location.ffgeojson;

import java.util.List;

public class GeometryPolygon implements IGeometry
{
    public List<List<List<Double>>> coordinates;
    private String Ftype;

    public GeometryPolygon()
    {
        Ftype = Geometry.Polygon;
    }

    public GeometryPolygon(List<List<List<Double>>> coords)
    {
        Ftype = Geometry.Polygon;
        coordinates = coords;
    }

    public RectangleF bounds()
    {
        FFrect rect = new FFrect(9999999, 9999999, -99999990, -99999990);
        for (List<List<Double>> coords : coordinates)
        {
            for (int i = 0; i < coords.size(); i++)
            {

                if (i == 0)
                {
                    rect.Left = coords.get(i).get(0);
                    rect.Top = coords.get(i).get(1);
                    rect.Right = coords.get(i).get(0);
                    rect.Bottom = coords.get(i).get(1);
                }

                if (rect.Left > coords.get(i).get(0)) rect.Left = coords.get(i).get(0);
                if (rect.Top > coords.get(i).get(1)) rect.Top = coords.get(i).get(1);
                if (rect.Right < coords.get(i).get(0)) rect.Right = coords.get(i).get(0);
                if (rect.Bottom < coords.get(i).get(1)) rect.Bottom = coords.get(i).get(1);
            }
        }
        return new RectangleF((float)(rect.Left), (float)(rect.Top), (float)((rect.Right - rect.Left)), ((float)(rect.Bottom - rect.Top)));
    }

    @Override
    public String type() {
        return Ftype;
    }

    public boolean inside(PointD point)
    {
        double x = point.X;
        double y = point.Y;

        boolean inside = false;

        for (List<List<Double>> geom : coordinates)
        {
            int i = 0;
            for (int j = geom.size() - 1; i < geom.size(); j = i++)
            {
                double xi = geom.get(i).get(0);
                double yi = geom.get(i).get(1);
                double xj = geom.get(j).get(0);
                double yj = geom.get(j).get(1);

                boolean intersect = ((yi > y) != (yj > y))
                        && (x < (xj - xi) * (y - yi) / (yj - yi) + xi);
                if (intersect) inside = !inside;
            }
        }
        return inside;
    }

    public void Move(double x, double y, double z)
    {

        for (List<List<Double>> coords : coordinates)
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

}