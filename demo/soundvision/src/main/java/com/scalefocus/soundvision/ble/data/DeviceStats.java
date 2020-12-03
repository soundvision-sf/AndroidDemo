package com.scalefocus.soundvision.ble.data;

public class DeviceStats extends DataParser{

    private static final int SV_DEVICE_STATS   =    0x82736455;

    public int distance;
    public int color[] = new int[8];
    public int color_count[] = new int[8];
    public int camera;
    public int buttonMask;
    public int ledMask;
    public String macAddress;

    public int battery_level;

    public float inclination_angle;
    public int heading_angle;
    public int compass_direction;

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
        int pos = 8; // skip session
        distance = readInt(data, pos); pos += 4;
        for (int i=0; i<8; i++) {
            color[i] = readInt(data, pos); pos += 4;
        }
        for (int i=0; i<8; i++) {
            color_count[i] = readInt(data, pos); pos += 4;
        }
        camera = readInt(data, pos);  pos += 4;
        buttonMask = readInt(data, pos);  pos += 4;
        ledMask = readInt(data, pos);  pos += 4;
        macAddress = readHex(data, pos, 6);  pos += 6;

        pos += 4; // skip seed

        battery_level = readInt(data, pos);  pos += 4;

        inclination_angle = (float)readInt(data, pos);  pos += 4;
        heading_angle = readInt(data, pos);  pos += 4;
        compass_direction = readInt(data, pos);  pos += 4;

        return true;
    }

}
