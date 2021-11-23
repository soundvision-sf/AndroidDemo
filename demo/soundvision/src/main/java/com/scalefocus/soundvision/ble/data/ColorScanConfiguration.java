package com.scalefocus.soundvision.ble.data;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class ColorScanConfiguration extends DataParser{

    private static final int SV_COLOR_SCAN_CONFIG   =    0x83676578;

    public int black;
    public int white;
    public int gray;
    public int light;
    public int dark;
    public int[] hueMap = new int[12];

    public ColorScanConfiguration(byte[] data)
    {
        parse(data);
    }

    public static boolean match(byte[] data)
    {
        if (data == null || data.length == 0 || DataParser.readInt(data, 0) != SV_COLOR_SCAN_CONFIG) return false;
        return true;
    }

    @Override
    public boolean parse(byte[] data)
    {
        if (!match(data)) return false;
        int pos = 4; // skip id
        black = readInt(data, pos); pos += 4;
        white = readInt(data, pos); pos += 4;
        gray = readInt(data, pos); pos += 4;
        light = readInt(data, pos); pos += 4;
        dark = readInt(data, pos); pos += 4;
        for (int i=0; i<6; i++) { // skip padding
            pos += 4;
        }
        for (int i=0; i<12; i++) {
            hueMap[i] = readInt(data, pos); pos += 4;
        }
        return true;
    }

    @Override
    public byte[] toData() {
        int[] ret = new int[24+6];

        ret[0] = Integer.reverseBytes(SV_COLOR_SCAN_CONFIG);
        ret[1] = Integer.reverseBytes(black);
        ret[2] = Integer.reverseBytes(white);
        ret[3] = Integer.reverseBytes(gray);
        ret[4] = Integer.reverseBytes(light);
        ret[5] = Integer.reverseBytes(dark);

        for (int i=0; i<6; i++) {
            ret[i+6] = 0x11223344;
        }

        for (int i=0; i<12; i++) {
            ret[i+12] = Integer.reverseBytes(hueMap[i]);
        }

        ByteBuffer byteBuffer = ByteBuffer.allocate(ret.length * 4);
        IntBuffer intBuffer = byteBuffer.asIntBuffer();
        intBuffer.put(ret);

        return byteBuffer.array();
    }

}
