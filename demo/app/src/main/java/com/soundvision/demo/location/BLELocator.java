package com.soundvision.demo.location;

import android.content.Context;
import android.util.Log;
import android.util.Pair;

import com.lemmingapex.trilateration.NonLinearLeastSquaresSolver;
import com.lemmingapex.trilateration.TrilaterationFunction;
import com.scalefocus.soundvision.ble.data.BLEScanAdvertising;
import com.soundvision.demo.altbeacon.AltBeaconApp;
import com.soundvision.demo.altbeacon.IAltBeaconEventListener;
import com.soundvision.demo.location.ffgeojson.PointD;
import com.soundvision.demo.location.flat.BeaconProp;
import com.soundvision.demo.location.ibeacon.IBeacon;
import com.soundvision.demo.location.internalpositioning.InternalPositioning;

import org.altbeacon.beacon.Beacon;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class BLELocator {

    /*
    class Beacon {
        String mac;
        int rssi;

        public Beacon(String mac, int rssi) {
            this.mac = mac;
            this.rssi = rssi;
        }
    }
*/
    private List<IBeacon> reports = Collections.synchronizedList( new ArrayList<IBeacon>());

    InternalPositioning mInternalPositioning = new InternalPositioning();

    AltBeaconApp altBeacon;
    public IAltBeaconEventListener listener;
    public List<BeaconProp> beaconsPropListFilter;

    androidx.lifecycle.Observer<Collection<Beacon>> observer = new androidx.lifecycle.Observer<Collection<Beacon>>() {
        @Override
        public void onChanged(Collection<Beacon> beacons) {
            if (listener == null) return;
            if (beacons.size() > 2) {

                List<BeaconProp> latest = new ArrayList<>();

                for (Beacon beacon : beacons) {
                    for (BeaconProp bp : beaconsPropListFilter)
                    {
                        if (bp.mac.toLowerCase(Locale.ROOT).equals(beacon.getBluetoothAddress().toLowerCase(Locale.ROOT).replace(":", ""))) {
                            bp.distance = beacon.getDistance();
                            latest.add(bp);
                            break;
                        }
                    }
                }

                if (latest.size() > 2) {
                    double[][] positions = new double[latest.size()][2];
                    double distances[] = new double[latest.size()];

                    int i = 0;
                    for (BeaconProp b : latest) {
                        distances[i] = b.distance * 100;//// Math.pow(10d, ((double) (-59) - (b.getRSSI())) / (10 * 2))*100;//(double)(10000 ^ ((-59 - (b.rssi)) / (10000 * 2))) / 100.0;
                        positions[i][0] = b.x;
                        positions[i][1] = b.y;
                        i++;
                    }

                    NonLinearLeastSquaresSolver solver = new NonLinearLeastSquaresSolver(new TrilaterationFunction(positions, distances), new LevenbergMarquardtOptimizer());
                    LeastSquaresOptimizer.Optimum optimum = solver.solve();

// the answer
                    double[] calculatedPosition = optimum.getPoint().toArray();

                    if (calculatedPosition != null)
                        listener.OnLocationUpdate(new PointD(calculatedPosition[0], calculatedPosition[1]));
                    else
                        listener.OnLocationUpdate(null);
                }
// error and geometry information
                //RealVector standardDeviation = optimum.getSigma(0);
                //RealMatrix covarianceMatrix = optimum.getCovariances(0);

            }


        }
    };

    public BLELocator(Context ctx) {
        altBeacon = new AltBeaconApp();
        altBeacon.init(ctx, observer);
    }

    public IBeacon getBeacon(String mac)
    {
        synchronized (reports) {
            for (IBeacon b : reports) {
                if (b.mac.equals(mac)) return b;
            }
        }
        return null;
    }

    public BeaconProp findBeaconProp(List<BeaconProp> beacons, String mac)
    {
        synchronized (reports) {
            for (BeaconProp b : beacons) {
                if (b.mac.equals(mac)) return b;
            }
        }
        return null;
    }

    public void addReport(BLEScanAdvertising stats)
    {
        synchronized (reports) {
            IBeacon b = getBeacon(stats.macAddress);
            if (b != null) {
                b.rssi = stats.rssi;
                reports.remove(b);
            } else
                b = IBeacon.fromBLEAdvScan (stats);
            reports.add(0, b);
            Log.i("NED:67", "mac : "+ stats.macAddress+"  rssi : "+stats.rssi+"  count : "+reports.size());
        }
    }

    public PointD getLocation(List<BeaconProp> beaconsPropList)
    {
        List<BeaconProp> latest = new ArrayList<>();
        List<IBeacon> beacons = new ArrayList<>();
        synchronized (reports) {
            for (int i = 0; i < reports.size(); i++) {
                IBeacon b = reports.get(i);
                BeaconProp bp = findBeaconProp(beaconsPropList, b.mac);
                if (bp != null) {
                    beacons.add(b);
                    bp.calcRSSI(b.rssi);
                    bp.distance = Math.pow(10d, ((double) (-59) - (bp.getRSSI())) / (10 * 2))*100;//(double)(10000 ^ ((-59 - (b.rssi)) / (10000 * 2))) / 100.0;
                    latest.add(bp);
                    if (latest.size() > 3) break;
                }
            }
        }

        //mInternalPositioning.getLocationWithBeacons(beacons, beaconsPropList);
        //mInternalPositioning.getLocationWithBeacons(beacons, beaconsPropList);

        boolean internal = false;

        if (internal) {
            return mInternalPositioning.getLocationWithBeacons(beacons, beaconsPropList);
        }

        if (beacons.size() > 2) {

            double[][] positions = new double[beacons.size()][2];
            double distances[] = new double[beacons.size()];

            int i = 0;
            for (BeaconProp b : latest) {
                distances[i] = Math.pow(10d, ((double) (-59) - (b.getRSSI())) / (10 * 2))*100;//(double)(10000 ^ ((-59 - (b.rssi)) / (10000 * 2))) / 100.0;
                positions[i][0] = b.x;
                positions[i][1] = b.y;
                i++;
            }

            NonLinearLeastSquaresSolver solver = new NonLinearLeastSquaresSolver(new TrilaterationFunction(positions, distances), new LevenbergMarquardtOptimizer());
            LeastSquaresOptimizer.Optimum optimum = solver.solve();

// the answer
            double[] calculatedPosition = optimum.getPoint().toArray();

            if (calculatedPosition != null)
                return new PointD(calculatedPosition[0], calculatedPosition[1]);

// error and geometry information
            //RealVector standardDeviation = optimum.getSigma(0);
            //RealMatrix covarianceMatrix = optimum.getCovariances(0);

            }

        return null;

    }

}
