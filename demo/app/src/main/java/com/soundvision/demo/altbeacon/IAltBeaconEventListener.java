package com.soundvision.demo.altbeacon;

import com.soundvision.demo.location.ffgeojson.PointD;

public interface IAltBeaconEventListener {
    void OnLocationUpdate(PointD pt);
}
