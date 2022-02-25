package com.soundvision.demo.location.flat;


import com.soundvision.demo.location.ffgeojson.Feature;
import com.soundvision.demo.location.ffgeojson.Geometry;
import com.soundvision.demo.location.ffgeojson.GeometryLineString;
import com.soundvision.demo.location.ffgeojson.GeometryPoint;
import com.soundvision.demo.location.ffgeojson.PointD;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class RouteController
{
    Floor floor;
    public RouteController(Floor floor)
    {
        this.floor = floor;
    }

    public List<RouteNode> nodes() {return floor.routeNodes;}
    public List<RouteNodeLink> links() {return floor.routeLinks;}

    public boolean visible = true;

    public RouteNode AddRoute(RouteNode r)
    {
        int ID = nodes().size();
        List<Integer> idsList = new ArrayList<Integer>();
        for (RouteNode rn : nodes())
        {
            if ((rn.coordinate.X == r.coordinate.X) && (rn.coordinate.Y == r.coordinate.Y)) return rn;
            idsList.add(rn.id);
        }

        while (idsList.contains(ID)) ID++;
        r.id = ID;
        nodes().add(r);
        return r;
    }

    public boolean AddLink(RouteNodeLink link)
    {
        for (RouteNodeLink rn : links())
        {
            if (rn.Equals(link)) return false;
        }
        links().add(link);
        return true;
    }

    public PointD GetRoutePt(int id)
    {
        for (RouteNode node : nodes())
        {
            if (node.id == id) return node.coordinate;
        }
        return new PointD(-1, -1);
    }

    class FindGroup
    {
        public int delta;
        public RouteNode node;

        public FindGroup(int delta, RouteNode node)
        {
            this.delta = delta;
            this.node = node;
        }
    }

    public class FindGroupComparator implements Comparator<FindGroup> {
        @Override
        public int compare(FindGroup o1, FindGroup o2) {
            return o2.delta - o1.delta;
        }
    }

    public RouteNode FindRouteByCoords(PointD pt, int delta)
    {
        List <FindGroup> list = new ArrayList<FindGroup>();
        for (RouteNode node : nodes())
        {
            int dx = (int)Math.abs(node.coordinate.X - pt.X);
            int dy = (int)Math.abs(node.coordinate.Y - pt.Y);
            if ((dx < delta) && (dy < delta))
            {
                list.add(new FindGroup(dx+dy, node));
            }
        }
        if (list.size() == 0) return null;
        //List<Point> SortedList = list.OrderBy(o <= o.X).ToList();
        Collections.sort(list, new FindGroupComparator());
        //list.Sort((x, y) => x.delta.CompareTo(y.delta));
        return list.get(0).node;
    }

    public boolean ImportLinks(Feature feature)
    {
        int lastID = -1;

        switch (feature.geometry.type())
        {
            case Geometry.Point:
            {
                GeometryPoint gp = (GeometryPoint)feature.geometry;

                RouteNode r = new RouteNode(new PointD(gp.coordinates.get(0), gp.coordinates.get(1)));
                r = AddRoute(r);
                if (lastID >= 0) AddLink(new RouteNodeLink(lastID, r.id));
                lastID = r.id;
            }
            break;
            case Geometry.LineString:
            {
                GeometryLineString gp = (GeometryLineString)feature.geometry;

                for (List<Double> coords : gp.coordinates)
                {
                    RouteNode r = new RouteNode(new PointD(coords.get(0), coords.get(1)));
                    r = AddRoute(r);
                    if (lastID >= 0) AddLink(new RouteNodeLink(lastID, r.id));
                    lastID = r.id;
                }
            }
            break;
        }
        return true;
    }

    public List<RouteNodeLink> GetLinks(RouteNode node)
    {
        List<RouteNodeLink> nodeLinks = new ArrayList<RouteNodeLink>();
        for (RouteNodeLink rn : links())
        {
            if (rn.Contains(node.id))
                nodeLinks.add(rn);
        }
        return nodeLinks;
    }

}
