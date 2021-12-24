package com.soundvision.demo.location.internalpositioningjava;

/**
 * Created by galen.georgiev on 9.11.2017 г..
 */

class BeaconData
{
    public BeaconCoordinates coordinates;
    public double minDist;
    public double maxDist;

    public BeaconData(double x, double y, double z, double minDist, double maxDist)
    {
        this.coordinates = new BeaconCoordinates(x, y, z);
        this.minDist = minDist;
        this.maxDist = maxDist;
    }
}
