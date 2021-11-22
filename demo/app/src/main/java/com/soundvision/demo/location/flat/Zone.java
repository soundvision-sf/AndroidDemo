package com.soundvision.demo.location.flat;


import com.soundvision.demo.location.ffgeojson.FFrect;
import com.soundvision.demo.location.ffgeojson.Feature;
import com.soundvision.demo.location.ffgeojson.Geometry;
import com.soundvision.demo.location.ffgeojson.GeometryLineString;
import com.soundvision.demo.location.ffgeojson.GeometryPoint;
import com.soundvision.demo.location.ffgeojson.GeometryPolygon;
import com.soundvision.demo.location.ffgeojson.RectangleF;

import java.util.ArrayList;
import java.util.List;

public class Zone
{

    public int id;
    public String name;
    public String type = Feature.Room;
    public String uuid;

    public boolean visible = true;

    public List<Feature> features = new ArrayList<Feature>();
    public RectangleF bounds()
    {
        return CalcOffset();
    }
  
    private RectangleF CalcOffset()
    {
        FFrect rect = new FFrect(9999999, 9999999, -9999999, -9999999);

        for (Feature feature : features)
        {
            switch (feature.geometry.type())
            {
                case Geometry.Point:
                {
                    GeometryPoint gp = (GeometryPoint)feature.geometry;

                    if (rect.Left > gp.coordinates.get(0)) rect.Left = gp.coordinates.get(0);
                    if (rect.Top > gp.coordinates.get(1)) rect.Top = gp.coordinates.get(1);
                    if (rect.Right < gp.coordinates.get(0)) rect.Right = gp.coordinates.get(0);
                    if (rect.Bottom < gp.coordinates.get(1)) rect.Bottom = gp.coordinates.get(1);
                }
                break;
                case Geometry.LineString:
                    GeometryLineString gl = (GeometryLineString)feature.geometry;

                    for (List<Double> coords : gl.coordinates)
                {

                    //PointF[] pt = new PointF[coords.Length];
                    //for (int i = 0; i < coords.Count; i++)
                    {
                        if (rect.Left > coords.get(0)) rect.Left = coords.get(0);
                        if (rect.Top > coords.get(1)) rect.Top = coords.get(1);
                        if (rect.Right < coords.get(0)) rect.Right = coords.get(0);
                        if (rect.Bottom < coords.get(1)) rect.Bottom = coords.get(1);
                    }
                }

                break;
                case Geometry.Polygon:
                {
                    GeometryPolygon gp = (GeometryPolygon)feature.geometry;

                    for (List<List<Double>> coords : gp.coordinates)
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

                }
                break;
            }

        }

        Double offsX = 0.0;
        Double offsY = 0.0;
        return new RectangleF((float)(rect.Left + offsX), (float)(rect.Top + offsY), (float)((rect.Right - rect.Left) + offsX), (float)(offsY + (float)(rect.Bottom - rect.Top)));
    }

}

