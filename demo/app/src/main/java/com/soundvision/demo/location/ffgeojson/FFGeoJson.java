package com.soundvision.demo.location.ffgeojson;

import java.util.ArrayList;
import java.util.List;

public class FFGeoJson {

    //private List<Feature> l_features = new ArrayList<Feature>();

    public String type;
    public String name;
    public Crs crs;
    public List<Feature> features = new ArrayList<Feature>();;//{ get { return l_features; } set { l_features = value; CalcOffset(); } }


    public RectangleF  bounds;



    public void CalcOffset()
    {
        FFrect rect = new FFrect(Double.MIN_VALUE, Double.MAX_VALUE, 0, 0);

        for (Feature feature : features)
        {
            switch (feature.geometry.type()) {
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

        float offsX = 0;
        float offsY = 0;
        bounds = new RectangleF((float)rect.Left+ offsX, (float)rect.Top+ offsY, (float)(rect.Right-rect.Left)+ offsX, (float)offsY + (float)(rect.Bottom-rect.Top));
    }

    public void Move(double x, double y, double z)
    {
        for (Feature feature : features) {
        feature.Move(x, y, z);
    }
    }

}








/*
class GeometryJson
{
    public IEnumerable<IGeometry> Items;
}
*/


/*
public class GeometryItemConverter : Newtonsoft.Json.Converters.CustomCreationConverter<IGeometry>
    {
public override IGeometry Create(Type ObjectType)
        {
        throw new NotImplementedException();
        }

public IGeometry Create(Type ObjectType, JObject jObject)
        {
        var type = (String)jObject.Property("type");

        switch (type)
        {
        case "Point":
        return new GeometryPoint();
        case "LineString":
        return new GeometryLineString();
        case "Polygon":
        return new GeometryPolygon();
        }

        throw new ApplicationException(String.Format("The animal type {0} is not supported!", type));
        }

public override Object ReadJson(JsonReader reader, Type ObjectType, Object existingValue, JsonSerializer serializer)
        {
        // Load JObject from stream 
        JObject jObject = JObject.Load(reader);

        // Create target Object based on JObject 
        var target = Create(ObjectType, jObject);

        // Populate the Object properties 
        serializer.Populate(jObject.CreateReader(), target);

        return target;
        }
        
}
*/