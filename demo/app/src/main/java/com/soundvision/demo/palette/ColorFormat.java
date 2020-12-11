package com.soundvision.demo.palette;

import static android.graphics.Color.HSVToColor;

public class ColorFormat {

    private static final int COL_MAX = 255;

    public static class ColorHSL {
        public double H;
        public double L;
        public double S;

        public int value()
        {
          return  (int)H | ( (int)L << 8 ) |  ( (int)S << 16 );
        }

        public ColorHSL()
        {
            H = 0;
            S = 0;
            L = 0;
        }

        public ColorHSL(int h, int s, int l)
        {
            H = h;
            S = s;
            L = l;
        }

        public ColorHSL(int hls)
        {
            H = hls  & 0xff;
            S = (hls >> 8 ) & 0xff;
            L = (hls >> 16 ) & 0xff;
        }

        public static String Name()
        {
            return "";
        }

    }

    public static class ColorRGB {
        public double R;
        public double G;
        public double B;

        public ColorRGB(Integer rgb)
        {
            B = rgb & 0xff;
            G = (rgb >> 8 ) & 0xff;
            R = (rgb >> 16 ) & 0xff;
        }

        public ColorRGB(Integer r, Integer g, Integer b)
        {
            R = r;
            G = g;
            B = b;
        }

        public ColorRGB(double r, double g, double b)
        {
            R = r;
            G = g;
            B = b;
        }

        public int getColor()
        {
            return (int)B | ((int)G << 8) | ((int)R << 16);
        }

        public String getName(ColorName[] from)
        {
            ColorName scanResult = from[0];
            ColorRGB result = new ColorRGB(255, 255, 255);

            for (ColorName c : from) {
                ColorRGB col = new ColorRGB(c.code);
                ColorRGB res = new ColorRGB(Math.abs(R - col.R), Math.abs(G - col.G), Math.abs(B - col.B));
                if (( res.R <= result.R) && ( res.G <= result.G) && ( res.B <= result.B) && (R-G>0 == col.R-col.G>0 ) && (R-B>0 == col.R-col.B > 0) && (G-B>0 == col.G-col.B > 0)) {
                    result = res;
                    scanResult = c;
                }
            }

            return scanResult.name;

        }

        public byte getColIndex(ColorName[] from)
        {
            int ret = 0;
            ColorName scanResult = from[0];
            ColorRGB result = new ColorRGB(255, 255, 255); // MAX DISTANCE
            ColorRGB delta = new ColorRGB(255, 255, 255);
            double resCheck = 1000000.0;

            for (int idx=0; idx<from.length; idx++) {
                ColorName c = from[idx];
                ColorRGB col = new ColorRGB(c.code);
                ColorRGB res = new ColorRGB(Math.abs(R - col.R), Math.abs(G - col.G), Math.abs(B - col.B));

                if (( res.R <= result.R) && ( res.G <= result.G) && ( res.B <= result.B) && (R-G>0 == col.R-col.G>0 ) && (R-B>0 == col.R-col.B > 0) && (G-B>0 == col.G-col.B > 0)) {
                    result = res;
                    ret = idx;
                }
            }

            return (byte)ret;

        }

    }

    public static ColorHSL RGBtoHLS(int col, int HLSMAX)
    {
        return RGBtoHLS(new ColorRGB(col), HLSMAX);
    }

    public static int HLStoRGB(ColorHSL col) {
        float[] hlsArr = {(float)(col.H / 255) * 360, (float)col.L/ 255, (float)col.S/ 255};
        int rgb = HSVToColor(255, hlsArr);
        return rgb;
    }

    public static ColorHSL RGBtoHLS(ColorRGB col, int HLSMAX)
    {

        ColorHSL hls = new ColorHSL();
        int RGBMAX = COL_MAX;
        // calculate lightness /

        double cMax = Math.max(Math.max(col.R,col.G), col.B);
        double cMin = Math.min( Math.min(col.R,col.G), col.B);

        double LL = ( ( (cMax+cMin) * HLSMAX ) + RGBMAX );

        hls.L = ( LL / (2 * RGBMAX) );

        if (cMax == cMin) {              // r=g=b -. achromatic case /
            hls.S = 0;                       // saturation /
            hls.H = HLSMAX*2/3;//UNDEFINED;  // hue /
        } else {                        // chromatic case /
            // saturation /
            if (hls.L <= (HLSMAX/2))
                hls.S = ( ((cMax-cMin)*HLSMAX) + ((cMax+cMin)/2) ) / (cMax+cMin);
            else
                hls.S = (( ((cMax-cMin)*HLSMAX) + ((2*RGBMAX-cMax-cMin)/2) )
                        / (2*RGBMAX-cMax-cMin));

            // hue /
            double Rdelta = ( ((cMax-col.R)*(HLSMAX/6)) + ((cMax-cMin)/2) ) / (cMax-cMin);
            double Gdelta = ( ((cMax-col.G)*(HLSMAX/6)) + ((cMax-cMin)/2) ) / (cMax-cMin);
            double Bdelta = ( ((cMax-col.B)*(HLSMAX/6)) + ((cMax-cMin)/2) ) / (cMax-cMin);

            if (col.R == cMax) {
                hls.H = (Bdelta - Gdelta);
            } else {
                if (col.G == cMax)
                    hls.H = ((HLSMAX/3) + Rdelta - Bdelta);
                else // B = cMax /
                    hls.H = (((2*HLSMAX)/3) + Gdelta - Rdelta);
            }

            if (hls.H < 0) hls.H += HLSMAX;
            if (hls.H > HLSMAX) hls.H -= HLSMAX;
        }
        return hls;
    }



    static double scale255(double n, double lower, double upper) {
        double ret = Math.max(lower, Math.min(n, upper)) - lower;
        return 255 - (ret * 255 / (upper - lower));
    }

    public static float clip255(double n) {
        double ret = Math.max(0, Math.min(n, 255));
        return (float) ret;
    }

}
