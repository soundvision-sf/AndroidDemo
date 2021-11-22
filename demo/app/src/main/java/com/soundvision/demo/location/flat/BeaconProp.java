package com.soundvision.demo.location.flat;

import com.soundvision.demo.utils.AverageValue;

public class BeaconProp
{
    public double x;
    public double y;
    public double z;
    public String mac;
    public float Frssi;
    public double distance;
    public double minDist;
    public double maxDist;
    AverageValue rssiList = new AverageValue(25);

    public float calcRSSI(float rssi)
    {
        Frssi = rssiList.add(rssi);
        return Frssi;
    }

    public float getRSSI()
    {
        return Frssi;
    }

    public BeaconProp(double x, double y, double z, String mac) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.mac = mac;
    }

    public BeaconProp(double x, double y, String mac) {
        this.x = x;
        this.y = y;
        this.z = 0;
        this.mac = mac;
    }

}