package com.soundvision.demo.location.ffgeojson;

import android.graphics.PointF;

public class PointD
{
    public PointD(double x, double y)
    {
        this.X = x;
        this.Y = y;
    }

    public PointD()
    {
    }

    public static PointF[] toFloatArr(PointD[] arr)
    {
        PointF[] ret = new PointF[arr.length];
        for (int i=0;i<arr.length; i++)
        {
            ret[i] = arr[i].toPointF();
        }
        return ret;
    }

    public PointF toPointF()
    {
        return new PointF((float) X, (float) Y);
    }

    public PointD Offset(PointD d)
    {
        return new PointD(X + d.X, Y + d.Y);
    }

    public PointD Offset(float dX, float dY)
    {
        return new PointD(X + dX, Y + dY);
    }

    public PointD Clone()
    {
        return new PointD(X, Y);
    }

    public double X;
    public double Y;
}
