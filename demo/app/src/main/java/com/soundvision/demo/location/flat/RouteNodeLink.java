package com.soundvision.demo.location.flat;


public class RouteNodeLink
{
    public int a;
    public int b;

    public RouteNodeLink(int a, int b)
    {
        this.a = a;
        this.b = b;
    }

    public RouteNodeLink()
    {
    }

    public boolean Equals(RouteNodeLink link)
    {
        return (link.a == a && link.b == b) || (link.a == b && link.b == a);
    }

    public boolean Contains(int id)
    {
        return (id == a || id == a);
    }

}
