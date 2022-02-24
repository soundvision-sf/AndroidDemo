package com.soundvision.demo.location.flat;

import com.soundvision.demo.navigine.PositionSmoother;
import com.soundvision.demo.navigine.RSSISmoother;
import com.soundvision.demo.utils.AverageValue;
import com.soundvision.demo.utils.AverageValueTime;

import java.util.Comparator;
import java.util.List;

public class BeaconProp implements Comparable<BeaconProp>
{
    public double x;
    public double y;
    public double z;
    public String mac;
    public float Frssi;
    public float txPower = 0;
    public double distance;
    public double minDist;
    public double maxDist;

    private RSSISmoother filter = new RSSISmoother(0.8);

    public AverageValueTime rssiList = new AverageValueTime(16, 9000);

    public static class RSSIComparator implements Comparator<BeaconProp> {
        @Override
        public int compare(BeaconProp o1, BeaconProp o2) {
            return Double.compare(o2.getRSSI(), o1.getRSSI());
        }
    }

    public float calcRSSI(float rssi, float txPow)
    {
        txPower = txPow;
        Frssi = filter.smoothRssi(rssi);//  rssiList.add(rssi);
        distance = Math.pow(10d, ((double) (-Math.abs(txPower)) - (Frssi)) / (10 * 2))*100;//(double)(10000 ^ ((-59 - (b.rssi)) / (10000 * 2))) / 100.0;
        return Frssi;
    }

    public List<float[]> getRSSIList()
    {
        return rssiList.getList();
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

    @Override
    public int compareTo(BeaconProp o) {
        return Double.compare(distance, o.distance);
    }
}