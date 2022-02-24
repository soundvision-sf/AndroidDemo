package com.soundvision.demo.navigine;

public class Position {
    public double x         = 0.0;
    public double y         = 0.0;
    public double precision = 0.0;
    public boolean isEmpty     = true;
    public long ts     = -1;

    public Position()
    {

    }

    public Position(Position pos)
    {
        x = pos.x;
        y = pos.y;
        precision = pos.precision;
        isEmpty = pos.isEmpty;
        ts = pos.ts;
    }

}
