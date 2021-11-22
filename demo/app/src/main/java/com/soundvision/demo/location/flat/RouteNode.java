package com.soundvision.demo.location.flat;


import com.soundvision.demo.location.ffgeojson.PointD;

public class RouteNode
        {
public int id;
public PointD coordinate;

public RouteNode()
        {
        id = -1;
        }

public RouteNode(int id, PointD coordinate)
        {
        this.id = id;
        this.coordinate = coordinate;
        }

public RouteNode(PointD coordinate)
        {
        this.id = -1;
        this.coordinate = coordinate;
        }

}