package com.scalefocus.soundvision.ble.data;

import android.util.Log;

public class DeviceStats extends DataParser{

    private static final int SV_DEVICE_STATS   =    0x82736455;

    public int proto_version;
    public int distance;
    public int color[] = new int[8];
    public int color_count[] = new int[8];
    public int camera;
    public int buttonMask;
    public int ledMask;
    public String macAddress;

    public int data_seed;

    public int compass_direction; // 16
    public int compass_inclination; // 16
    public int compass_heading; // 16
    public int compass_status; // 8

    public int battery_level; // 8
    public int battery_status; // 8

    public int brightness;

    boolean isValid = false;

    public DeviceStats(byte[] data)
    {
        parse(data);
    }

    public static boolean match(byte[] data)
    {
        if (data == null || data.length == 0 || DataParser.readInt(data, 0) != SV_DEVICE_STATS) return false;
        return true;
    }

    @Override
    public boolean parse(byte[] data)
    {
        if (!match(data)) return false;
        int pos = 4; // skip session

        proto_version = readInt(data, pos); pos += 4;
        distance = readInt(data, pos); pos += 4;
        for (int i=0; i<8; i++) {
            color[i] = readInt(data, pos); pos += 4;
        }
        for (int i=0; i<8; i++) {
            color_count[i] = readInt(data, pos); pos += 4;
        }
        camera = readInt(data, pos);  pos += 4;
        buttonMask = readInt(data, pos);  pos += 4;
        //Log.i("NLS:332", "buttonMask:"+buttonMask);
        ledMask = readInt(data, pos);  pos += 4;
        macAddress = readHex(data, pos, 6);  pos += 8; // 6 bytes + 2 align

        data_seed = readInt(data, pos); pos += 4; // skip seed
        //Log.i("NLS:332", "data_seed:"+data_seed);

        battery_level = readInt8(data, pos);  pos += 1;
        battery_status = readInt8(data, pos);  pos += 1;
        pos += 2; // align

        compass_inclination = readInt16(data, pos);  pos += 2;
        compass_direction = readInt16(data, pos);  pos += 2;
        compass_heading = readInt16(data, pos);  pos += 2;
        compass_status = readInt8(data, pos);  pos += 1;
        pos += 5; // align

        brightness = readInt(data, pos);  pos += 4;

        return true;
    }

}
