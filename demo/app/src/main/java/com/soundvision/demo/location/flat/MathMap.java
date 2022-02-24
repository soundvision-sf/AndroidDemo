package com.soundvision.demo.location.flat;

import com.soundvision.demo.location.ffgeojson.PointD;

public class MathMap {
    public static double EARTH_RAD_CM = 637813700;

    public static  double toRad(double x)
    {
        return (x * Math.PI) / 180.0;
    }

    public static double toDeg(double r)
    {
        return (r * 180.0) / Math.PI;
    }

    public static PointD computePixel(double lon1, double lat1, double lon2, double lat2)
    {
        double x = getDistanceDirection(lon1, lat1, lon2, lat1);
        double y = getDistanceDirection(lon1, lat1, lon1, lat2);
        return new PointD(x, -y);
    }

    private static double getDistanceDirection(double lon1, double lat1, double lon2, double lat2)
    {
        double dLat = toRad(lat2 - lat1);
        double dLong = toRad(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) *
                        Math.sin(dLong / 2) * Math.sin(dLong / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double d = EARTH_RAD_CM * c;
        if (dLong < 0 || dLat < 0)
        {
            return -d;
        }
        return d; // returns the distance in cm
    }

    public static PointD computeOffset(double lon, double lat, PointD offsetCM)
    {
        double dist = Math.sqrt((offsetCM.X * offsetCM.X) + (offsetCM.Y * offsetCM.Y));
        //double head = Math.Asin();

        double distance = dist / EARTH_RAD_CM;
        double heading = (Math.PI / 2) + Math.atan2(offsetCM.Y, offsetCM.X);// rad(head);

        double fromLat = toRad(lat);//latitude
        double fromLng = toRad(lon);//longitude
        double cosDistance = Math.cos(distance);
        double sinDistance = Math.sin(distance);
        double sinFromLat = Math.sin(fromLat);
        double cosFromLat = Math.cos(fromLat);
        double sinLat = cosDistance * sinFromLat + sinDistance * cosFromLat * Math.cos(heading);
        double dLng = Math.atan2(
                sinDistance * cosFromLat * Math.sin(heading),
                cosDistance - sinFromLat * sinLat);

        return new PointD(toDeg(fromLng + dLng), toDeg(Math.asin(sinLat)));
    }

    public static PointD computeOffset(double lon, double lat, double x, double y)
    {
        double dist = Math.sqrt((x * x) + (y * y));

        double distance = dist / EARTH_RAD_CM;
        double heading = (Math.PI / 2) + Math.atan2(y, x);

        double fromLat = toRad(lat);//latitude
        double fromLng = toRad(lon);//longitude
        double cosDistance = Math.cos(distance);
        double sinDistance = Math.sin(distance);
        double sinFromLat = Math.sin(fromLat);
        double cosFromLat = Math.cos(fromLat);
        double sinLat = cosDistance * sinFromLat + sinDistance * cosFromLat * Math.cos(heading);
        double dLng = Math.atan2(
                sinDistance * cosFromLat * Math.sin(heading),
                cosDistance - sinFromLat * sinLat);

        return new PointD(toDeg(fromLng + dLng), toDeg(Math.asin(sinLat)));
    }

    //
    ///Low precision
    //

    public static  PointD computePixelLo(double lonStart, double latStart, double lon, double lat)
    {
        double x = (lat - latStart) * 11111100d;
        double y = (lon - lonStart) * (11111100d * Math.cos(toRad(latStart)));
        return new PointD(y, -x);
    }

}
