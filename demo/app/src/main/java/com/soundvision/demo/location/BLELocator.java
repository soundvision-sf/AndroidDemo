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
import com.soundvision.demo.navigine.NearestTransmitterPositionEstimator;
import com.soundvision.demo.navigine.Position;
import com.soundvision.demo.navigine.PositionSmoother;
import com.soundvision.demo.ui.FFView;

import org.altbeacon.beacon.Beacon;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
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

    NearestTransmitterPositionEstimator navigine;

    AltBeaconApp altBeacon;
    public IAltBeaconEventListener listener;
    public List<BeaconProp> beaconsPropListFilter;

    PositionSmoother m_smoother = new PositionSmoother(0.99999);

    public int txPower = 75;

    public int checkCount = 0;

    public boolean exportFlag = false;

    androidx.lifecycle.Observer<Collection<Beacon>> observer = new androidx.lifecycle.Observer<Collection<Beacon>>() {
        @Override
        public void onChanged(Collection<Beacon> beacons) {
            if (listener == null) return;
            addReport(beacons);
            listener.OnLocationUpdate(new PointD(0, 0));
            /*

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
*/

        }
    };

    private static BLELocator locator;

    public static BLELocator INSTANCE(Context ctx)
    {
        if (locator == null)
        locator = new BLELocator(ctx);
        return locator;
    }

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
                if (b.mac.toLowerCase().equals(mac.toLowerCase())) return b;
            }
        }
        return null;
    }

    private void removeOldest()
    {

        long Tmax = 10000;
        long T = System.currentTimeMillis();
        synchronized (reports) {
            int i = 0;
            while (i<reports.size()) {
               if (T - reports.get(i).lastReportTime > Tmax ) {
                   reports.remove(i);
               } else i++;
            }
            Log.i("NED:67", "  count : "+reports.size());
        }
    }

    public void addReport(Collection<Beacon> beacons)
    {
        synchronized (reports) {
            for (Beacon beacon : beacons) {
                IBeacon b = getBeacon(beacon.getBluetoothAddress().toUpperCase().replace(":", ""));
                if (b != null) {
                    b.newRssi(beacon.getRssi());
                    reports.remove(b);
                } else
                    b = IBeacon.fromBLEBeacon(beacon);
                reports.add(0, b);
                //Log.i("NED:67", "mac : "+ b.mac+"  rssi : "+b.getRssi()+"  count : "+reports.size());
                removeOldest();
            }

            checkCount++;

        }
    }

    public void addReport(BLEScanAdvertising stats)
    {
        if (stats.rssi< -90) return;
        if (Math.abs(stats.rssi)<10)
        {
            return;
        }
        synchronized (reports) {

            IBeacon b = getBeacon(stats.macAddress);

            if (b != null) {
                b.newRssi( stats.rssi);
                reports.remove(b);
            } else
                b = IBeacon.fromBLEAdvScan (stats);
            reports.add(0, b);
            removeOldest();
            //Log.i("NED:67", "mac : "+ stats.macAddress+"  rssi : "+stats.rssi+"  count : "+reports.size());
            checkCount++;
        }
    }

    public List<IBeacon> getBeaconsList()
    {
        List<IBeacon> beacons = new ArrayList<>();
        synchronized (reports) {
            for (int i = 0; i < reports.size(); i++) {
                IBeacon b = reports.get(i);
                beacons.add(b);

            }
        }

        return beacons;

    }


    private void saveToFile(BeaconProp b, double dist)
    {

        try {
            File f=new File("/sdcard/Download", "RSSI-"+b.mac+".map");
            String dStr = new DecimalFormat("#.##").format(dist);
            updateFile(f, dStr+"="+b.getRSSI(), dStr+"=") ;

        }
        catch (Exception e) {

        }
    }

    private List<String> updateFile(File f, String line, String prefix) {

        List<String> ret = new ArrayList<String>();
        boolean update = true;
        try {
            //File f=new File("/sdcard/Download", b.mac+".rssi");
            InputStream inputStream = new FileInputStream(f);

            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    if (receiveString.startsWith(prefix)) {
                        ret.add(line);
                        update = false;
                    } else
                        ret.add(receiveString);
                }
                inputStream.close();
            }
        }
        catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
        } catch (Exception e) {
            Log.e("login activity", "Can not read file: " + e.toString());
        }

        if (update)
            ret.add(line);

        try {
            FileOutputStream osr = new FileOutputStream(f);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(osr);
            for(String str: ret) {
                outputStreamWriter.write(str+"\n");
            }
            outputStreamWriter.close();
        }
        catch (Exception e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }

        return ret;
    }

    public PointD getLocation(FFView ffview, List<BeaconProp> latest)
    {
        if (ffview.selArea == null) return null;


        List<BeaconProp> beaconsPropList = ffview.selArea.beacons;
        if (navigine == null)
            navigine = new NearestTransmitterPositionEstimator(beaconsPropList);

        int BECONS_FOR_CHECK = 10;
        List<IBeacon> beacons = new ArrayList<>();
        synchronized (reports) {
            for (int i = 0; i < reports.size(); i++) {
                IBeacon b = reports.get(i);
                BeaconProp bp = findBeaconProp(beaconsPropList, b.mac);
                if (bp != null) {
                    beacons.add(b);
                    bp.calcRSSI(b.getRssi(), -55/*b.getTxPower()*/);
                    //bp.distance = Math.pow(10d, ((double) (-txPower) - (bp.getRSSI())) / (10 * 2))*100;//(double)(10000 ^ ((-59 - (b.rssi)) / (10000 * 2))) / 100.0;
                    latest.add(bp);
                    //if (i>=checkCount) break;
                }
            }
            checkCount = 0;
        }


        if (exportFlag) {
            exportFlag = false;
            PointD ofs = ffview.getMapCenter();
            for (BeaconProp b : latest) {
                double mapDist = Math.sqrt(((b.x - ofs.X) * (b.x - ofs.X)) + ((b.y - ofs.Y) * (b.y - ofs.Y)));
                saveToFile(b, mapDist);
            }
        }

        boolean internal = false;

        if (internal) {
            return mInternalPositioning.getLocationWithBeacons(beacons, beaconsPropList);
        }


        //Position p = navigine.calculatePosition(latest);
/*
        if (!p.isEmpty) {
            return new PointD(p.x, p.y);
        }
*/

        if (latest.size() > 2)
        {

            int checkCount = Math.min(BECONS_FOR_CHECK, beacons.size());

            Collections.sort(latest);

            double[][] positions = new double[checkCount][3];
            double distances[] = new double[checkCount];

            int i = 0;
            for (BeaconProp b : latest) {
                distances[i] = b.distance;// Math.pow(10d, ((double) (-75) - (b.getRSSI())) / (10 * 2))*100;//(double)(10000 ^ ((-59 - (b.rssi)) / (10000 * 2))) / 100.0;
                positions[i][0] = b.x;
                positions[i][1] = b.y;
                positions[i][2] = 280;
                i++;
                if (i>=checkCount) break;
            }

            NonLinearLeastSquaresSolver solver = new NonLinearLeastSquaresSolver(new TrilaterationFunction(positions, distances), new LevenbergMarquardtOptimizer());

          try {
              LeastSquaresOptimizer.Optimum optimum = solver.solve();
              double[] calculatedPosition = optimum.getPoint().toArray();


/*
              LeastSquaresOptimizer.Optimum optimum = solver.solve();
              double[] centroid = optimum.getPoint().toArray();

              double errorRadius = 0;
              boolean errorCalc = false;

              // Error and geometry information
              try {
                  //Create new array without the altitude. Including altitude causes a
                  //SingularMatrixException as it cannot invert the matrix.
                  double[][] err_positions = new double[calculatedPosition.length][2];
                  i = 0;
                  while (i < calculatedPosition.le()) {

                      err_positions[i] = new double[] { positions[i][0], positions[i][1] };
                      i++;
                  }
                  trilaterationFunction = new TrilaterationFunction(err_positions, distances);
                  solver = new NonLinearLeastSquaresSolver(trilaterationFunction, new LevenbergMarquardtOptimizer());

                  optimum = solver.solve();
                  RealVector standardDeviation = optimum.getSigma(0);
                  //RealMatrix covarianceMatrix = optimum.getCovariances(0);

                  errorRadius = ((standardDeviation.getEntry(0) + standardDeviation.getEntry(1)) / 2) * 100;
                  errorCalc = true;

              } catch (Exception ex) {
                  errorRadius = 0;
                  errorCalc = false;
              }

              return new Position(WebMercator.yToLatitude(optimum.getPoint().toArray()[0]),
                      WebMercator.xToLongitude(centroid[1]), centroid[2], errorRadius, errorCalc);
              */
              if (calculatedPosition != null) {

                  Position p = new Position();
                  p.isEmpty = false;
                  p.x = calculatedPosition[0];
                  p.y = calculatedPosition[1];
                  p.ts = System.currentTimeMillis();
                  Position ret = m_smoother.smoothPosition(p);

                  return new PointD(ret.x, ret.y);

                  //return new PointD(calculatedPosition[0], calculatedPosition[1]);
              }
          } catch (Exception e)
          {
              e.printStackTrace();
          }

// the answer


// error and geometry information
            //RealVector standardDeviation = optimum.getSigma(0);
            //RealMatrix covarianceMatrix = optimum.getCovariances(0);

        }

        return null;

    }

}
