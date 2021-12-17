package com.soundvision.demo.location.internalpositioning;

import android.graphics.PointF;
import android.util.Log;
import android.util.Pair;

import com.soundvision.demo.location.LowPassFilter;
import com.soundvision.demo.location.MeasuredFilter;
import com.soundvision.demo.location.ffgeojson.PointD;
import com.soundvision.demo.location.flat.BeaconProp;
import com.soundvision.demo.location.ibeacon.IBeacon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by galen.georgiev on 8.11.2017 г..
 */

public class InternalPositioning
{
    private static final String TAG = InternalPositioning.class.getSimpleName();

    //private VenueProp mVenue;
    private HashMap<String, BeaconMisses> mBeaconsMisses = new HashMap<>();
    private HashMap<String, BeaconStats> mBeaconsStats = new HashMap<>();

    private InternalProperties mProperties;
    private int mFloor;

    private int nthMeasurement = 0;

    private MeasuredFilter mFilter1 = new MeasuredFilter(-100);
    private MeasuredFilter mFilter2 = new MeasuredFilter(-100);
    private LowPassFilter mLowPassFilterX = new LowPassFilter();
    private LowPassFilter mLowPassFilterY = new LowPassFilter();

    public InternalPositioning()
    {
        mProperties = new InternalProperties();
    }

    private PointF getCoordinateWith(double ax, double ay, double bx, double by, double cx, double cy,
                                     double dA, double dB, double dC) {
        double W, Z, x, y, y2;
        W = dA*dA - dB*dB - ax*ax - ay*ay + bx*bx + by*by;
        Z = dB*dB - dC*dC - bx*bx - by*by + cx*cx + cy*cy;

        x = (W*(cy-by) - Z*(by-ay)) / (2 * ((bx-ax)*(cy-by) - (cx-bx)*(by-ay)));
        y = (W - 2*x*(bx-ax)) / (2*(by-ay));
        //y2 is a second measure of y to mitigate errors
        y2 = (Z - 2*x*(cx-bx)) / (2*(cy-by));

        y = (y + y2) / 2;
        return new PointF((float)x, (float)y);
    }

    public List<PointD> calculateZone(List<IBeacon> beacons, List<BeaconProp> mVenueBeacons)
    {
        if (beacons == null || beacons.isEmpty())
        {
            Log.e(TAG, "calculateZone, no beacons detected");
            return null;
        }

        Log.i(TAG, "calculateZone, detected beacons count = " + beacons.size());

        long timestamp = System.currentTimeMillis();

        for (IBeacon beacon : beacons)
        {
            if (Math.abs(beacon.getRssi()) <= 1)
                continue;

            String mac = beacon.mac;//String.valueOf(beacon.getMinor());
            BeaconMisses beaconMisses = mBeaconsMisses.get(mac);
            BeaconStats beaconStats = mBeaconsStats.get(mac);

            if (beaconMisses != null)
            {
                int lowPassRSSI = Math.abs(beacon.getRssi());

                beaconStats.rssi.add(0, lowPassRSSI);

                if (beaconStats.rssi.size() > 10)
                    beaconStats.rssi.remove(beaconStats.rssi.size() - 1);

                //NOTE: it seems like we need to have a beacon three times in a row if there were many misses (> 3) before,
                // .. before we can consider it valid.
                if (beaconMisses.misses > 1)
                {
                    int misses = beaconMisses.misses;
                    beaconMisses.misses = Math.min(3, misses - 1);
                    beaconMisses.timestamp = timestamp;
                    beaconStats.average = -1.0;
                }
                else
                {
                    beaconMisses.misses = 0;
                    beaconMisses.timestamp = timestamp;
                    List<Integer> rssis = mBeaconsStats.get(mac).rssi;
                    mProperties.lowSearchTill = rssis.size() < mProperties.lowestSearch ? rssis.size() : mProperties.lowestSearch;

                    int lowest = 0;
                    for (int i = 0; i < mProperties.lowSearchTill; ++ i)
                    {
                        int rssi = rssis.get(i);
                        if (rssi == 0)
                            continue;

                        if (i == 0)
                            lowest = rssi;
                        else if (rssi < lowest)
                            lowest = rssi;
                    }

                    if (lowest == 0) beaconStats.lowest = -1;
                    else beaconStats.lowest = lowest;

                    mProperties.averageUntil = rssis.size() < mProperties.averageOver ? rssis.size() : mProperties.averageOver;
                    int total = 0;
                    int misses = 0;
                    int lastValue = 0;
                    for (int i = 0; i < mProperties.averageUntil; ++ i)
                    {
                        int rssi = rssis.get(i);

                        if (rssi == 0)
                        {
                            total += lastValue;
                            misses ++;
                        }
                        else
                        {
                            total += rssi;
                            lastValue = rssi;
                        }
                    }

                    if (misses < 2)
                        beaconStats.average = Math.round((double) total / (double) mProperties.averageUntil);
                    else
                        beaconStats.average = -1.0;
                }
            }
            else
            {
                BeaconMisses tempBeaconMisses = new BeaconMisses();
                tempBeaconMisses.misses = 0;
                tempBeaconMisses.timestamp = timestamp;
                mBeaconsMisses.put(mac, tempBeaconMisses);

                BeaconStats tempBeaconStats = new BeaconStats();
                tempBeaconStats.minor = String.valueOf(beacon.getMinor());
                tempBeaconStats.major = String.valueOf(beacon.getMajor());

                //Log.i("NED", "min : 0x"+Integer.toHexString(tempBeaconStats.minor)+"  maj : 0x"+Integer.toHexString( tempBeaconStats.major ));

                tempBeaconStats.average = Math.abs(beacon.getRssi());
                tempBeaconStats.lowest = Math.abs(beacon.getRssi());
                tempBeaconStats.rssi.add(Math.abs(beacon.getRssi()));
                tempBeaconStats.mac = mac;
                mBeaconsStats.put(mac, tempBeaconStats);
            }
        }

        // NOTE: we don't change average if there was a miss.
        for (HashMap.Entry<String, BeaconMisses> entry : mBeaconsMisses.entrySet())
        {
            String key = entry.getKey();
            BeaconMisses beaconMisses = entry.getValue();

            if (beaconMisses.timestamp == timestamp)
                continue;

            BeaconStats beaconStats = mBeaconsStats.get(key);

            beaconStats.rssi.add(0, 0);
            if (beaconStats.rssi.size() > 10)
                beaconStats.rssi.remove(beaconStats.rssi.size() - 1);

            int numMisses = 1 + beaconMisses.misses;
            beaconMisses.misses = numMisses;
            beaconMisses.timestamp = timestamp;

            if (numMisses > 1 || numMisses >= mProperties.lowestSearch)
                beaconStats.lowest = -1;
        }
        mBeaconsMisses.clear();

        List<String> sortedRSSIs = getKeysSortedByAverage();

        HashMap<String, BeaconCoordinates> venueBeacons = new HashMap<>();
        for (BeaconProp beacon : mVenueBeacons)
        {
         /*   double x = beacon.get(0);
            double y = beacon.get(1);
            double z = beacon.get(2);
            String minor = String.valueOf(beacon.get(3));*/
            // TODO
            venueBeacons.put(beacon.mac, new BeaconCoordinates(beacon.x, beacon.y, beacon.z));
        }

        mFloor = 8;
        for (String mac : sortedRSSIs)
        {
            BeaconStats beaconStats = mBeaconsStats.get(mac);
            if (beaconStats != null)
            {
                mFloor = Integer.parseInt(beaconStats.major);
                break;
            }
        }

        List<String> finalSorted = new ArrayList<>();
        String floorStr = String.valueOf(mFloor);
        for (String mac : sortedRSSIs)
        {
            if (finalSorted.size() >= 3)
                break;

            if (venueBeacons.get(mac) == null)
                continue;

            String major = mBeaconsStats.get(mac).major;
            //NOTE: it seems like we don't use beacons from other floors for triangulation
            if (floorStr.equals(major))
            {
                if (finalSorted.size() == 2)
                {
                    double xB1 = venueBeacons.get(finalSorted.get(0)).x;
                    double yB1 = venueBeacons.get(finalSorted.get(0)).y;
                    double xB2 = venueBeacons.get(finalSorted.get(1)).x;
                    double yB2 = venueBeacons.get(finalSorted.get(1)).y;
                    double xBi = venueBeacons.get(mac).x;
                    double yBi = venueBeacons.get(mac).y;

                    double determinant = calculateDeterminant(xB1, yB1, xB2, yB2, xBi, yBi);
                    double distB1B2 = calculateDistance(xB1, yB1, xB2, yB2);
                    double distB1Bi = calculateDistance(xB1, yB1, xBi, yBi);
                    double distB2Bi = calculateDistance(xB2, yB2, xBi, yBi);
                    //TODO: B: what is this testing? it seems like the idea was to test whether they are not on the same line
                    // .. but it doesn't seem like it's doing what it's supposed to be doing.
                    double testValue = (((distB2Bi*distB2Bi)-(distB1Bi*distB1Bi)+(distB1B2*distB1B2))/(-2.0*distB1B2));

                    //if (determinant != 0)
                    if (testValue <= 0)
                        finalSorted.add(mac);
                }
                //NOTE: since we don't override the average value when there's a miss, it seems like we will take into account
                // .. even beacons for which there was a miss previously... though, that's ok...
                else finalSorted.add(mac);
            }
        }

        if (finalSorted.size() < 3)
        {
            Log.e(TAG, "No three beacons found, which not lie on one line");
            return null;
        }

        List<BeaconData> beaconsData = new ArrayList<>();
        for (String mac : finalSorted)
        {
            double x = venueBeacons.get(mac).x;
            double y = venueBeacons.get(mac).y;
            double z = venueBeacons.get(mac).z;

            //////BEACON SIGNAL STRENGTH OFFSET//CHANGE ACCORDING TO SIGNAL INCREASEORDEACREASE////
            /////********THIS INFO COULD ALSO GO INTO JSON AND THEN BE UNIQUE FOR EACH VENUE/////////////
            int beaconRssiOffset = 0;

            int beaconRssi = (int) mBeaconsStats.get(mac).average + beaconRssiOffset;

            double minEst = mProperties.rssis[beaconRssi][0];
            double maxEst = mProperties.rssis[beaconRssi][1];

            double minDist, maxDist;

            //Calculates minimum planar distance of phone from beacons
            if(minEst <= z) minDist = z / 2.0;
            else minDist = Math.sqrt((minEst * minEst) - (z * z));

            //Calculates maximum planar distance of phone from beacon
            if(maxEst <= z) maxDist = (z / 2) + 2;
            else maxDist = Math.sqrt((maxEst * maxEst) - (z * z));

            beaconsData.add(new BeaconData(x, y, z, minDist, maxDist));
        }

        return getTriangulationZone(beaconsData);
    }

    private List<String> getKeysSortedByAverage()
    {
        List<Map.Entry<String, BeaconStats>> list = new LinkedList(mBeaconsStats.entrySet());

        Collections.sort(list, new Comparator<Map.Entry<String, BeaconStats>>()
        {
            public int compare(Map.Entry<String, BeaconStats> o1, Map.Entry<String, BeaconStats> o2)
            {
                if (o1.getValue().average > o2.getValue().average)
                    return 1;

                if (o1.getValue().average < o2.getValue().average)
                    return -1;

                return 0;
            }
        });

        List<String> sortedKeys = new ArrayList<>();
        for (Map.Entry<String, BeaconStats> entry : list)
            sortedKeys.add(entry.getKey());

        return sortedKeys;
    }

    private double calculateDeterminant(double x1, double y1, double x2, double y2, double x3, double y3)
    {
        return (x1 * y2) + (x2 * y3) + (x3 * y1) - (x1 - y3) - (x3 * y2) - (x2 * y1);
    }

    private List<PointD> getTriangulationZone(List<BeaconData> beaconsData)
    {
        ///CORNER BEACON WITH SMALLEST RSSI PREFERENCE///////
        if (beaconsData.get(0).maxDist == 0)
        {
            PointD point = new PointD(beaconsData.get(0).coordinates.x, beaconsData.get(0).coordinates.y);
            return new ArrayList<>(Arrays.asList(point, point, point, point));
        }

        Pair<Double, Double> b01 = triangulateBeacons(beaconsData.get(0), beaconsData.get(1));
        Pair<Double, Double> b02 = triangulateBeacons(beaconsData.get(0), beaconsData.get(2));

        PointD b0b1min = perpendicularIntersectionOnLine(beaconsData.get(0), beaconsData.get(1), b01.first);
        PointD b0b1max = perpendicularIntersectionOnLine(beaconsData.get(0), beaconsData.get(1), b01.second);
        PointD b0b2min = perpendicularIntersectionOnLine(beaconsData.get(0), beaconsData.get(2), b02.first);
        PointD b0b2max = perpendicularIntersectionOnLine(beaconsData.get(0), beaconsData.get(2), b02.second);

        PointD pointMinMin = perpendicularLineIntersections(beaconsData.get(0), beaconsData.get(1), beaconsData.get(2),
                b0b1min, b0b2min);
        PointD pointMinMax = perpendicularLineIntersections(beaconsData.get(0), beaconsData.get(1), beaconsData.get(2),
                b0b1min, b0b2max);
        PointD pointMaxMin = perpendicularLineIntersections(beaconsData.get(0), beaconsData.get(1), beaconsData.get(2),
                b0b1max, b0b2min);
        PointD pointMaxMax = perpendicularLineIntersections(beaconsData.get(0), beaconsData.get(1), beaconsData.get(2),
                b0b1max, b0b2max);

        return new ArrayList<>(Arrays.asList(pointMinMin, pointMinMax, pointMaxMin, pointMaxMax));
    }

    private Pair<Double, Double> triangulateBeacons(BeaconData beacon1, BeaconData beacon2)
    {
        double b1x = beacon1.coordinates.x;
        double b1y = beacon1.coordinates.y;
        double b1min = beacon1.minDist;
        double b1max = beacon1.maxDist;

        double b2x = beacon2.coordinates.x;
        double b2y = beacon2.coordinates.y;
        double b2min = beacon2.minDist;
        double b2max = beacon2.maxDist;

        if (b1max < b1min)
            b1max = b1min;

        if (b2max < b2min)
            b2max = b1min;

        double distBeacons = calculateDistance(b1x, b1y, b2x, b2y);
        double minDist, maxDist;

        if (b1min + b2max < distBeacons)
            minDist = b1min;
        else if (b1min + distBeacons < b2max)
            minDist = -1.0 * b1min;
        else if (b2max + distBeacons < b1min)
            minDist = distBeacons + b2max;
        else
            minDist = ((b1min * b1min) - (b2max * b2max) + (distBeacons * distBeacons)) / (2.0 * distBeacons);

        if (b1max + b2min < distBeacons)
            maxDist = b1max;
        else if (b1max + distBeacons < b2min)
            maxDist = -1.0 * b1max;
        else if (b2min + distBeacons < b1max)
            maxDist = distBeacons + b2min;
        else
            maxDist = ((b1max * b1max) - (b2min * b2min) + (distBeacons * distBeacons)) / (2.0 * distBeacons);

        return new Pair<>(minDist, maxDist);
    }

    private PointD perpendicularIntersectionOnLine(BeaconData beacon1, BeaconData beacon2, double distance)
    {
        double b1x = beacon1.coordinates.x;
        double b1y = beacon1.coordinates.y;
        double b2x = beacon2.coordinates.x;
        double b2y = beacon2.coordinates.y;
        double beaconsDist = calculateDistance(b1x, b1y, b2x, b2y);

        double x = (((b2x - b1x) / beaconsDist) * distance) + b1x;
        double y = (((b2y - b1y) / beaconsDist) * distance) + b1y;

        return new PointD(x, y);
    }

    private PointD perpendicularLineIntersections(BeaconData beacon1, BeaconData beacon2, BeaconData beacon3,
                                                 PointD p12, PointD p13)
    {
        double b1x = beacon1.coordinates.x;
        double b1y = beacon1.coordinates.y;
        double b2x = beacon2.coordinates.x;
        double b2y = beacon2.coordinates.y;
        double b3x = beacon3.coordinates.x;
        double b3y = beacon3.coordinates.y;
        double b1b2x = p12.X;
        double b1b2y = p12.Y;
        double b1b3x = p13.X;
        double b1b3y = p13.Y;

        double retX, retY;
        retX = ((b2y-b1y)*((b1b3y)*(b3y-b1y)+(b3x-b1x)*(b1b3x))-(b3y-b1y)*(b1b2y*(b2y-b1y)+(b2x-b1x)*b1b2x))/((b2y-b1y)*(b3x-b1x)-(b3y-b1y)*(b2x-b1x));

        if(b2y == b1y)
            retY = b1b3y+((b3x-b1x)/(b3y-b1y))*b1b3x-((b3x-b1x)/(b3y-b1y))*retX;
        else if (b3y == b1y)
            retY = b1b2y+((b2x-b1x)/(b2y-b1y))*b1b2x-((b2x-b1x)/(b2y-b1y))*retX;
        else
            retY = b1b2y+((b2x-b1x)/(b2y-b1y))*b1b2x-((b2x-b1x)/(b2y-b1y))*retX;

        return new PointD(retX, retY);
    }


    public PointD calculateCoord(List<PointD> zonePoints)
    {
        if (zonePoints == null || zonePoints.size() != 4)
        {
            Log.e("TAG", "ZonePoints is null or ZonePoints.size() != 4");
            return null;
        }
/*
        // Calculate Constants
        double areaWidthPxl = widthInPixels;
        double areaWidthCm = widthInCentimeters;
        double scaling = areaWidthCm / areaWidthPxl;
        double mapImageWidthInUnits = (widthInPixels * scaling) / 5.0;
        double mapImageHeightInUnits = (heightInPixels * scaling) / 5.0;

        if (!checkPointValidity(zonePoints))
        {
            Log.e(TAG, "checkPointValidity == false");
            return null;
        }
*/
        PointD p1 = zonePoints.get(0);
        PointD p2 = zonePoints.get(1);
        PointD p3 = zonePoints.get(2);
        PointD p4 = zonePoints.get(3);

        double px = (p1.X + p2.X + p3.X + p4.X) / 4.0;
        double py = (p1.Y + p2.Y + p3.Y + p4.Y) / 4.0;

        if (nthMeasurement == 0)
        {
            mFilter1.mean = (p4.X + p2.X) / 2.0;
            mFilter2.mean = (p4.Y + p2.Y) / 2.0;
            mLowPassFilterX.z = mFilter1.mean;
            mLowPassFilterY.z = mFilter2.mean;
        }
        nthMeasurement ++;

        PointD coordinate = new PointD();
        coordinate.X = mFilter1.processMeasurement(px);
        coordinate.Y = mFilter2.processMeasurement(py);
        //coordinate = processCoordinate(coordinate);


        return coordinate;
    }

    public PointD calculateLocationByZone(List<PointD> zonePoints)
    {
        /*
        Coordinate coordinate = floor.calculateXYWithZone(zonePoints);
        if (coordinate == null)
            return null;

        // Build Location
        Location location = new Location();
        location.venue = mVenue;
        location.floor = floor;
        location.coordinate = coordinate;
        location.date = Calendar.getInstance();
*/
        return calculateCoord(zonePoints);
    }

    public PointD getLocationWithBeacons(List<IBeacon> beacons, List<BeaconProp> mVenueBeacons)
    {
        if (beacons.isEmpty())
            return null;

        PointD location = null;

        List<IBeacon> beaconList = new ArrayList<>(beacons);
        Collections.sort(beaconList, new Comparator<IBeacon>()
        {
            @Override
            public int compare(IBeacon beacon1, IBeacon beacon2)
            {
                return beacon2.getRssi() - beacon1.getRssi();
            }
        });
/*
        IBeacon strongestBeacon = null;
        for (IBeacon beacon : beaconList)
        {
            if (beacon.getAccuracy() >= 0)
            {
                strongestBeacon = beacon;
                break;
            }
        }*/

        //if (mVenue.singleRoomMajor != null && mVenue.singleRoomMajor.equals(strongestBeacon.getMajor()))
          //  location = calculateSingleRoomLocationWithBeacon(strongestBeacon);

        if (location == null)
        {
            List<PointD> calculatedPoints = calculateZone(beaconList, mVenueBeacons);
            location = calculateLocationByZone(calculatedPoints);
        }

        return location;
    }

    /*
    private Location calculateSingleRoomLocationWithBeacon(IBeacon beacon)
    {
        Location location = null;
        Floor currentFloor = null;
        Room currentRoom = null;

        for (Floor floor : mVenue.floors)
        {
            for (Room room : floor.rooms1)
            {
                if (room.minor != null && room.minor.equals(beacon.getMinor()))
                {
                    if (room.rssi != null)
                    {
                        if (beacon.getRssi() >= room.rssi)
                        {
                            currentRoom = room;
                            currentFloor = floor;
                        }
                    }
                    else
                    {
                        currentRoom = room;
                        currentFloor = floor;
                    }
                    break;
                }
            }
        }

        if (currentRoom != null)
        {
            location = new Location();
            location.venue = mVenue;
            location.floor = currentFloor;
            location.room = currentRoom;
            location.coordinate = currentRoom.coordinate;
            location.date = Calendar.getInstance();
        }

        return location;
    }
*/
    /*
    private Floor getCurrentFloor()
    {
        Log.i(TAG, "getCurrentFloor");

        Floor floorToReturn = null;
        for (Floor floor : mVenue.floors)
        {
            boolean isFloorFound = false;
            for (List<Integer> major : floor.majors)
            {
                if (major == null || major.isEmpty())
                    continue;

                if (mFloor == major.get(0))
                {
                    floorToReturn = floor;
                    isFloorFound = true;
                }
            }

            if (!isFloorFound)
                floor.resetIndoorLocationFilters();
        }

        return floorToReturn;
    }
*/
    private double calculateDistance(double x1, double y1, double x2, double y2)
    {
        double deltaX = x2 - x1;
        double deltaY = y2 - y1;

        return Math.sqrt((deltaX * deltaX) + (deltaY * deltaY));
    }

    private double calculateDistance(double x1, double y1, double z1, double x2, double y2, double z2)
    {
        double deltaX = x2 - x1;
        double deltaY = y2 - y1;
        double deltaZ = z2 - z1;

        return Math.sqrt((deltaX * deltaX) + (deltaY * deltaY) + (deltaZ * deltaZ));
    }

}
