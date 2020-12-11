package com.soundvision.demo.palette;

import android.util.Log;

import java.util.Arrays;

public class ColorPalette {

    static ColorName[] colors;

    private ColorFormat.ColorRGB whiteTrim = new ColorFormat.ColorRGB(0xffffff);
    private ColorFormat.ColorRGB blackTrim = new ColorFormat.ColorRGB(0);

    public static int[] getPaletteColors() {
        int[] ret = new int[colors.length];
        for (int i = 0; i < colors.length; i++) {
            ret[i] = colors[i].code;
        }
        return ret;
    }

    public static ColorName[] getColors() {
        return colors;
    }

    public static  ColorNode[] getColorNodes() {
        ColorNode[] ret = new  ColorNode[colors.length];
        for (int i = 0; i < colors.length; i++) {
            ret[i] = new ColorNode(colors[i].code, 1);
        }
        return ret;
    }

    public void scaleWhite(int whiteTreshold)
    {
        whiteTrim = new ColorFormat.ColorRGB(whiteTreshold);
    }

    public void scaleBlack(int blackTreshold)
    {
        blackTrim = new ColorFormat.ColorRGB(blackTreshold);
    }

    private ColorName[] Scaled()
    {
        ColorName[] scaledPal = new ColorName[colors.length];

        for (int i=0; i<colors.length; i++) {
            scaledPal[i] = colors[i].ScaleTo(blackTrim, whiteTrim);
        }

        return scaledPal;

    }


    class palette {
        String name;
        int code; // ?
        //struct sample* sample;

        public palette(String name, int code) {
            this.name = name;
            this.code = code;
        }
    };

    class palette_color {
        palette pal;
        palette prefix;
    };


    static int sel_col = 0;
    static int GH, GS, GL = 0;
    static palette_color G_desc;

    int HLSMAX = 64;
    
    static int[] hueColorMap = { 15, 47, 72, 81, 165, 195, 242, 262, 272, 293, 329, 346};

    static int blackCode	= 0;
    static int whiteCode	= 3;
    static int grayCode		= 1;
    static int hueStartCode	= 4;

    palette[] pal16 = {
        new palette("black",     0),// &sample_color_black},
        new palette("gray",      1),//  &sample_color_gray),
        new palette("silver",    2),//  &sample_color_silver),
        new palette("white",     3),//  &sample_color_white),

        // Hue range
        new palette("red",       4),//  &sample_color_red), // maroon
        new palette("orange",    5),//  &sample_color_olive), // orange -> brown
        new palette("yellow",    6),//  &sample_color_yellow), // olive

        new palette("lime",      7),//  &sample_color_lime),
        new palette("green",     8),//  &sample_color_green),
        new palette("aqua",      9),//  &sample_color_aqua), // cyan -> teal

        new palette("blue",      10),//  &sample_color_blue), // -> navy
        new palette("indigo",    11),//  &sample_color_blue), // -> navy
        new palette("violet",    12),//  &sample_color_blue), // -> navy

        new palette("purple",    13),//  &sample_color_purple),
        new palette("magenta",   14),//  &sample_color_teal),
        new palette("rose",   	  15),//  &sample_color_fuchsia),

        new palette("maroon",    16),//  &sample_color_maroon), 16
        new palette("brown",     17),//  &sample_color_maroon), 17
        new palette("olive",     18),//  &sample_color_olive) 18
        new palette("teal",      19),//  &sample_color_teal) 19
        new palette("navy",      20),//  &sample_color_navy) 20

    };


    palette[] pal_prefix = {
            new palette("",      0),
            new palette("light",     1), // 21
            new palette("dark",      2)// 22
    };

    static int[][] hueColorCodes = {
        { 16, 17, 18, 8, 19, 19, 20, 20, 20, 13, 14, 15},
        { 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15},
        { 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15}
    };

    static int[][] hueColorPrefixCodes = {
        { 0, 0, 0, 2, 0, 0, 0, 0, 0, 2, 2, 2},
        { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}
    };

    int hueColorCode(int hue)
    {
        int idx = 360 * hue / HLSMAX;
        int ret = 0;
        while (ret < 12 && idx > hueColorMap[ret]) { ret++; }
        return ret % (12);
    }

    int lightColorCode(int L)
    {
        int code = 100 * L / HLSMAX;
        if ((code - 50) > 20) {
            return 2; // light
        }
        else
        if (code < 35) {
            return 0; // dark
        }
        return 1;
    }

    palette_color hueColorDesc(int H, int S, int L)
    {
        palette_color ret = new palette_color();
        ret.pal = pal16[blackCode];
        ret.prefix = null;

        int prefix = 0;
        if (L < 16) {
            ret.pal = pal16[blackCode];
        }
        else
        if (L > 48) {
            ret.pal = pal16[whiteCode];
        }
        else
        {
            if (S < 16) {
                ret.pal = pal16[grayCode];
            }
            else
            {
                int hue_code = hueColorCode(H);
                int light_code = lightColorCode(L);
                int code = hueColorCodes[light_code][hue_code];
                prefix = hueColorPrefixCodes[light_code][hue_code];
                ret.pal = pal16[code];
            }
        }
        ret.prefix = pal_prefix[prefix];
        return ret;
    }

    public byte[][][] extractCodes16() {
        byte[][][] colorCodes = new byte[16][16][16];

        ColorName[] scale = Scaled();

        for (int r = 0; r < 16; r++) {
            for (int g = 0; g < 16; g++) {
                for (int b = 0; b < 16; b++) {
                    int rcomp = ((r*16)+r);
                    int gcomp = (g*16)+g;
                    int bcomp = (b*16)+b;

                    ColorFormat.ColorRGB rgb = new ColorFormat.ColorRGB(rcomp, gcomp, bcomp);
                    ColorFormat.ColorHSL hls = ColorFormat.RGBtoHLS(rgb, HLSMAX);
                    palette_color desc = hueColorDesc((int)hls.H, (int)hls.S, (int)hls.L);

                    colorCodes[r][g][b] = (byte)((desc.prefix.code << 6) | desc.pal.code);//rgb.getColIndex(scale);

                }
            }
        }

        String colList = Arrays.deepToString(colorCodes);
        String[] line = colList.split("],");

        for (String s : line){
            Log.i("ColList = ", s+"],");
        }



        return colorCodes;

    }

    public static void setColors(ColorName[] colors) {
        ColorPalette.colors = colors;
    }
}

