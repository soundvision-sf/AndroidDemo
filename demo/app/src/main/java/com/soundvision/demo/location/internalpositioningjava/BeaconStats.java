package com.soundvision.demo.location.internalpositioningjava;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by galen.georgiev on 8.11.2017 г..
 */

class BeaconStats
{
    public String mac;
    public String minor;
    public String major;
    public double average;
    public int lowest;
    public List<Integer> rssi = new ArrayList<>();
}
