package com.soundvision.demo.altbeaconjava;

import com.soundvision.demo.location.ffgeojson.PointD;

public interface IAltBeaconEventListenerJava {
    void OnLocationUpdate(PointD pt);
}
