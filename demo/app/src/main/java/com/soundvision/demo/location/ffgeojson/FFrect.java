package com.soundvision.demo.location.ffgeojson;

public class FFrect {

    public FFrect(double l, double t, double r, double b)
    {
        this.Left = l;
        this.Top = t;
        this.Right = r;
        this.Bottom = b;
    }

    public double Left;
    public double Top;
    public double Right;
    public double Bottom;

    public double Width() {return Right - Left; }
    public double Height() {return Bottom - Top; }
}
