package com.soundvision.demo.palette;

public class ColorNode {
    public int rgb;
    public int red, grn, blu;
    public int cnt;

    public int RGB()
    {
        return rgb | 0xff000000;
    }

    public int Count()
    {
        return cnt;
    }

    public ColorNode(int rgb, int cnt) {
        this.rgb = (rgb & 0xFFFFFF);
        this.red = (rgb & 0xFF0000) >> 16;
        this.grn = (rgb & 0xFF00) >> 8;
        this.blu = (rgb & 0xFF);
        this.cnt = cnt;
    }

    public ColorNode(int red, int grn, int blu, int cnt) {
        this.rgb = ((red & 0xff) << 16) | ((grn & 0xff) << 8) | blu & 0xff;
        this.red = red;
        this.grn = grn;
        this.blu = blu;
        this.cnt = cnt;
    }

    public int distanceTo(int red, int grn, int blu) {
        // returns the squared distance between (red, grn, blu)
        // and this this color
        int dr = this.red - red;
        int dg = this.grn - grn;
        int db = this.blu - blu;
        return (dr*dr) + (dg*dg) + db*db;
    }

    public String toString() {
        String s = this.getClass().getSimpleName();
        s = s + " red=" + red + " green=" + grn + " blue=" + blu + " count=" + cnt;
        return s;
    }
}