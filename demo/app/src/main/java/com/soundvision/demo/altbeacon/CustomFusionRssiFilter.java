package com.soundvision.demo.altbeacon;

import android.os.SystemClock;

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

import org.altbeacon.beacon.logging.LogManager;
import org.altbeacon.beacon.service.RssiFilter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

/**
 * Calculate a RSSI value on base of an arbitrary list of measured RSSI values
 * The list is clipped by a certain length at start and end and the average
 * is calculate by simple arithmetic average
 */
public class CustomFusionRssiFilter implements RssiFilter {

    private static final String TAG = "CustomFusionRssiFilter";
    public static final long DEFAULT_SAMPLE_EXPIRATION_MILLISECONDS = 20000; /* 20 seconds */
    private static long sampleExpirationMilliseconds = DEFAULT_SAMPLE_EXPIRATION_MILLISECONDS;
    private ArrayList<Measurement> mMeasurements = new ArrayList<Measurement>();
    private KalmanFilter filter = new KalmanFilter(0.008, 4);

    @Override
    public void addMeasurement(Integer rssi) {
        filter.filter(rssi);
        Measurement measurement = new Measurement();
        measurement.rssi = rssi;
        measurement.timestamp = SystemClock.elapsedRealtime();
        mMeasurements.add(measurement);
    }

    @Override
    public boolean noMeasurementsAvailable() {
        return mMeasurements.size() == 0;
    }


    @Override
    public int getMeasurementCount() { return mMeasurements.size(); }

    @Override
    public double calculateRssi() {
        refreshMeasurements();
        int size = mMeasurements.size();

        //-->
        int mean = 0;
        for (int i=0; i<size; i++) {
            mean += mMeasurements.get(i).rssi;
        }
        mean /= size;
        double variance = 0.0, sD = 0.0;
        for (int i =0; i<size; i++) {
            variance += ((mMeasurements.get(i).rssi - mean) * (mMeasurements.get(i).rssi - mean));
        }
        variance /= (size-1);
        sD = Math.sqrt(variance);
        //<--

        int startIndex = 0;
        int endIndex = size -1;
        if (size > 2) {
            startIndex = size/10+1;
            endIndex = size-size/10-2;
        }

        double sum = 0;
        for (int i = startIndex; i <= endIndex; i++) {
            sum += mMeasurements.get(i).rssi;
        }
        double runningAverage = sum/(endIndex-startIndex+1);

        LogManager.d(TAG, "HELLO Running average mRssi based on %s measurements: %s",
            size, runningAverage);
        return runningAverage;
    }

    private synchronized void refreshMeasurements() {
        ArrayList<Measurement> newMeasurements = new ArrayList<Measurement>();
        Iterator<Measurement> iterator = mMeasurements.iterator();
        while (iterator.hasNext()) {
            Measurement measurement = iterator.next();
            if (SystemClock.elapsedRealtime() - measurement.timestamp < sampleExpirationMilliseconds ) {
                newMeasurements.add(measurement);
            }
        }
        mMeasurements = newMeasurements;
        Collections.sort(mMeasurements);
    }

    private class Measurement implements Comparable<Measurement> {
        Integer rssi;
        long timestamp;
        @Override
        public int compareTo(Measurement arg0) {
            return rssi.compareTo(arg0.rssi);
        }
    }

    public static void setSampleExpirationMilliseconds(long newSampleExpirationMilliseconds) {
        sampleExpirationMilliseconds = newSampleExpirationMilliseconds;
    }

    @RestrictTo(Scope.TESTS)
    static long getSampleExpirationMilliseconds() {
        return sampleExpirationMilliseconds;
    }
}
