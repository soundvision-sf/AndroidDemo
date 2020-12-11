package com.soundvision.demo.palette;

public class ColorName
{
    public String name;
    public int code;

    public ColorName(String name, int code) {
        this.name = name;
        this.code = code;
    }

    public ColorName ScaleTo( ColorFormat.ColorRGB black,  ColorFormat.ColorRGB white) {
        ColorFormat.ColorRGB color = new  ColorFormat.ColorRGB(code);

        double scale = white.R - black.R;
        double R = black.R + (color.R / 255 * scale);
        scale = white.G - black.G;
        double G = black.G + (color.G / 255 * scale);
        scale = white.B - black.B;
        double B = black.B + (color.B / 255 * scale);
        color = new  ColorFormat.ColorRGB(R, G, B);
        return new ColorName(name, color.getColor());
    }

}
