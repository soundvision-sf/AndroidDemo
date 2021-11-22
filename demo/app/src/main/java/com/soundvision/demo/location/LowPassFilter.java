package com.soundvision.demo.location;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by galen.georgiev on 9.11.2017 г..
 */

public class LowPassFilter implements Parcelable
{
    public double a;
    public double b;
    public double z;

    public LowPassFilter()
    {
        a = 0.92;
        b = 1.0 - a;
        z = 0;
    }

    public double process(double input)
    {
        z = (input * b) + (z * a);
        return z;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeDouble(a);
        dest.writeDouble(b);
        dest.writeDouble(z);
    }

    protected LowPassFilter(Parcel in)
    {
        a = in.readDouble();
        b = in.readDouble();
        z = in.readDouble();
    }

    public static final Creator<LowPassFilter> CREATOR = new Creator<LowPassFilter>()
    {
        @Override
        public LowPassFilter createFromParcel(Parcel source)
        {
            return new LowPassFilter(source);
        }

        @Override
        public LowPassFilter[] newArray(int size)
        {
            return new LowPassFilter[size];
        }
    };
}
