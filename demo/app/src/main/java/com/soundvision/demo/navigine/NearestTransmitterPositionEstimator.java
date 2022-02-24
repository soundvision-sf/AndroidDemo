package com.soundvision.demo.navigine;

import com.soundvision.demo.location.flat.BeaconProp;
import com.soundvision.demo.location.flat.RouteController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NearestTransmitterPositionEstimator {
    static double A = -82.0;
    static double B =  3.0;

    PositionSmoother m_smoother;
    Map<String, BeaconProp> m_transmitters = new HashMap<>();
    //List<BeaconProp> getRegisteredTransmittersMeasurements(
     //   const List<BeaconProp>& radioMsr);

    
    public NearestTransmitterPositionEstimator(List<BeaconProp> transmitters)
    {
        m_smoother = new PositionSmoother(0.9);
        for (BeaconProp t : transmitters)
            m_transmitters.put(t.mac, t);
    }

    public List<BeaconProp> getRegisteredTransmittersMeasurements(
         List<BeaconProp> radioMsr)
    {
        List<BeaconProp> registeredMeasurements = new ArrayList<>();
        for (BeaconProp  msr : radioMsr)
        if (m_transmitters.containsKey(msr.mac) )
            registeredMeasurements.add(msr);
        return registeredMeasurements;
    }


    public Position calculatePosition(List<BeaconProp> inputMeasurements)
    {
        Position position = new Position();
        position.isEmpty = true;
        List<BeaconProp> BeaconProps = getRegisteredTransmittersMeasurements(inputMeasurements);
        if (BeaconProps.size() == 0)
            return position;


        Collections.sort(BeaconProps, new BeaconProp.RSSIComparator());

       // auto nearestTx = std::max_element(BeaconProps.begin(), BeaconProps.end(),
       // [](BeaconProp msr1, BeaconProp msr2) {return msr1.rssi < msr2.rssi; });

        String nearestTxId = BeaconProps.get(0).mac;
        double nearestTxRssi = BeaconProps.get(0).getRSSI();
        if (!m_transmitters.containsKey(nearestTxId))
            return position;
        else
        {
            BeaconProp t = m_transmitters.get(nearestTxId);
            position.x = t.x;
            position.y = t.y;
            position.precision = Math.sqrt(Math.exp((A - nearestTxRssi) / B)) + 1.0;
            position.isEmpty = false;
            position.ts = BeaconProps.get(BeaconProps.size()-1).rssiList.getTs(true);
            return m_smoother.smoothPosition(position);
        }
    }

}
