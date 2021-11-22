package com.soundvision.demo.location.ffgeojson;

public interface IGeometry
{
    String type();
    void Move(double x, double y, double z);
    RectangleF bounds();
}