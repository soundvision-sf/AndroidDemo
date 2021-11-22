package com.soundvision.demo.location.ffgeojson;

public class RectangleF {
    public RectangleF(double l, double t, double width, double height)
    {
        this.X = l;
        this.Y = t;
        this.Width = width;
        this.Height = height;
    }

    public double X;
    public double Y;
    public double Width;
    public double Height;

    public double Right()
    {
        return X+Width;
    }

    public double Bottom()
    {
        return Y+Height;
    }
}
