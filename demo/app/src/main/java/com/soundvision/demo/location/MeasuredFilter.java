package com.soundvision.demo.location;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by galen.georgiev on 9.11.2017 г..
 */

public class MeasuredFilter implements Parcelable
{
    public double mean;
    public double next;
    public double oldMean;
    public double variance;
    public double gain;
    public double annealingRate; //number of readings over which gain should go from max to steady
    public double annealingStart;
    public double annealingSteady;
    public double numReadings;

    public MeasuredFilter(int mean)
    {
        this.mean = mean;
        this.oldMean = mean;
        this.annealingRate = 10;
        this.annealingStart = 150;
        this.annealingSteady = 50; // SPEED
        this.gain = annealingStart;
        this.variance = 0.5;
        this.next = mean;
        this.numReadings = 0;
    }

    private double evalUnnormalizedGaussian(double mean, double variance, double input)
    {
        // float variance = std * std * 2.0;
        double term = input - mean;
        double unNormalized = Math.exp(-(term * term * 0.2) / variance) / Math.sqrt(Math.PI * variance);

        return unNormalized;
    }

    public double processMeasurement(double measurement)
    {
        double error = measurement - mean;

        variance = (variance + Math.abs(error) * gain) / 2.0;
        if (variance < 20)
            variance = 20;

        double probAtObservation = evalUnnormalizedGaussian(mean, variance, measurement);
        double probAtPrediction =  evalUnnormalizedGaussian(mean, variance, next);

        //we do want to trust everything a little bit... probability shouldn't decay to 0
        if (probAtObservation < 0.00025)
            probAtObservation = 0.00025;

        oldMean = mean;
        mean = (measurement*probAtObservation + next*probAtPrediction)/(probAtPrediction+probAtObservation);

        //predict
        next = mean;

        if (numReadings < annealingRate)
            gain -= (annealingStart - annealingSteady) / annealingRate;

        numReadings ++;

        if (numReadings > annealingRate)
            numReadings = 10000000;

        return next;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeDouble(mean);
        dest.writeDouble(next);
        dest.writeDouble(oldMean);
        dest.writeDouble(variance);
        dest.writeDouble(gain);
        dest.writeDouble(annealingRate);
        dest.writeDouble(annealingStart);
        dest.writeDouble(annealingSteady);
        dest.writeDouble(numReadings);
    }

    protected MeasuredFilter(Parcel in)
    {
        mean = in.readDouble();
        next = in.readDouble();
        oldMean = in.readDouble();
        variance = in.readDouble();
        gain = in.readDouble();
        annealingRate = in.readDouble();
        annealingStart = in.readDouble();
        annealingSteady = in.readDouble();
        numReadings = in.readDouble();
    }

    public static final Creator<MeasuredFilter> CREATOR = new Creator<MeasuredFilter>()
    {
        @Override
        public MeasuredFilter createFromParcel(Parcel source)
        {
            return new MeasuredFilter(source);
        }

        @Override
        public MeasuredFilter[] newArray(int size)
        {
            return new MeasuredFilter[size];
        }
    };
}
