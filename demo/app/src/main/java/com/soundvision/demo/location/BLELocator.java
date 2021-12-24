package com.soundvision.demo.location;

import android.content.Context;
import android.util.Log;

import com.lemmingapex.trilateration.NonLinearLeastSquaresSolver;
import com.lemmingapex.trilateration.TrilaterationFunction;
import com.scalefocus.soundvision.ble.data.BLEScanAdvertising;
import com.soundvision.demo.altbeacon.AltBeaconApp;
import com.soundvision.demo.altbeacon.CustomRangeNotifier;
import com.soundvision.demo.altbeacon.IAltBeaconEventListener;
import com.soundvision.demo.location.ffgeojson.PointD;
import com.soundvision.demo.location.flat.BeaconProp;
import com.soundvision.demo.location.ibeacon.IBeacon;
import com.soundvision.demo.location.internalpositioningjava.InternalPositioning;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.Region;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

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
    private List<IBeacon> reports = Collections.synchronizedList(new ArrayList<>());

    InternalPositioning mInternalPositioning = new InternalPositioning();

    AltBeaconApp altBeacon;
    public IAltBeaconEventListener listener;
    public List<BeaconProp> beaconsPropListFilter;


    CustomRangeNotifier observer = new CustomRangeNotifier() {

        @Override
        public void didRangeBeaconsInRegion(@Nullable Collection<Beacon> beacons, @Nullable Region region) {

            if (listener == null) return;

            //added by me
            //-->
            List<IBeacon> newList = new ArrayList<IBeacon>();
            Log.d("HELLO", "Ranged: " + beacons.size() + " beacons");
            for (Beacon beacon : beacons) {

                for (BeaconProp bp : beaconsPropListFilter) {
                    if (macCheckForAltBeacon(bp.mac, beacon)) {

                        //TODO: delete this boolean for real beacons only.
                        boolean saveMacAddress = beacon.getId1().toString().equalsIgnoreCase("d546df97-4757-47ef-be09-3e2dcbdd0c77");
                        newList.add(IBeacon.fromAltBeacon(beacon, saveMacAddress));
                    }
                }
            }
            boolean internal = true;

            if (internal) {
                PointD point = mInternalPositioning.getLocationWithBeacons(newList, beaconsPropListFilter);
                if (point != null) {
                    listener.onLocationUpdate(point);
                }
                return;
            }
            //<--

            if (beacons.size() > 2) {

                List<BeaconProp> latest = new ArrayList<>();

                for (Beacon beacon : beacons) {
                    for (BeaconProp bp : beaconsPropListFilter)
                    {
                        if (macCheckForAltBeacon(bp.mac, beacon)) {
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
                    Log.d("HELLO", "onChanged(): position=" + calculatedPosition[0] + ", " + calculatedPosition[1]);

                    if (calculatedPosition != null)
                        listener.onLocationUpdate(new PointD(calculatedPosition[0], calculatedPosition[1]));
                    else
                        listener.onLocationUpdate(null);
                }
// error and geometry information
                //RealVector standardDeviation = optimum.getSigma(0);
                //RealMatrix covarianceMatrix = optimum.getCovariances(0);

            }


        }
    };

    //TODO: change this function
    private boolean macCheckForAltBeacon(String mac, Beacon altBeacon) {
        return mac.equalsIgnoreCase(altBeacon.getIdentifiers().get(0).toString())
            || mac.equalsIgnoreCase(altBeacon.getBluetoothAddress().replace(":", ""));
    }

    public BLELocator(Context ctx) {
        altBeacon = AltBeaconApp.Companion.getInstance();
        altBeacon.init(ctx, observer);
    }

    public void onDestroy(Context ctx) {
        altBeacon.onDestroy(ctx);
    }

    public IBeacon getBeacon(String mac) {
        synchronized (reports) {
            for (IBeacon b : reports) {
                if (b.mac.equals(mac)) return b;
            }
        }
        return null;
    }

    public BeaconProp findBeaconProp(List<BeaconProp> beacons, String mac) {

        synchronized (reports) {
            for (BeaconProp b : beacons) {
                if (b.mac.equals(mac)) return b;
            }
        }
        return null;
    }

    public void addReport(BLEScanAdvertising stats) {

        synchronized (reports) {
            IBeacon b = getBeacon(stats.macAddress);
            if (b != null) {
                b.rssi = stats.rssi;
                reports.remove(b);
            } else
                b = IBeacon.fromBLEAdvScan(stats);
            reports.add(0, b);
            Log.i("NED:67", "mac : " + stats.macAddress + "  rssi : " + stats.rssi + "  count : " + reports.size());
        }
    }

    public PointD getLocation(List<BeaconProp> beaconsPropList) {
        List<BeaconProp> latest = new ArrayList<>();
        List<IBeacon> beacons = new ArrayList<>();
        synchronized (reports) {
            for (int i = 0; i < reports.size(); i++) {
                IBeacon b = reports.get(i);
                BeaconProp bp = findBeaconProp(beaconsPropList, b.mac);
                if (bp != null) {
                    beacons.add(b);
                    bp.calcRSSI(b.rssi);
                    //TODO: why is this -59, it should be different depending on beacon?
                    bp.distance = Math.pow(10d, ((double) (-59) - (bp.getRSSI())) / (10 * 2)) * 100;//(double)(10000 ^ ((-59 - (b.rssi)) / (10000 * 2))) / 100.0;
                    latest.add(bp);
                    //TODO: it seems like three most resent ones are taken, instead of three closest ones maybe?
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
                distances[i] = Math.pow(10d, ((double) (-59) - (b.getRSSI())) / (10 * 2)) * 100;//(double)(10000 ^ ((-59 - (b.rssi)) / (10000 * 2))) / 100.0;
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
