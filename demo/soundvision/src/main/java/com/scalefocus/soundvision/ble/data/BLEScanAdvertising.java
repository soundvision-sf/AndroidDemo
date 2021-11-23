package com.scalefocus.soundvision.ble.data;

public class BLEScanAdvertising extends DataParser {

    private static final int SV_BLE_SCAN_ADV = 0x56444142;

    public String macAddress;
    public int minor;
    public int major;
    public int rssi = 0;
    public byte[] manufactureData = null;

    public BLEScanAdvertising(byte[] data)
    {
        parse(data);
    }

    public static boolean match(byte[] data)
    {
        if (data == null || data.length == 0 || DataParser.readInt(data, 0) != SV_BLE_SCAN_ADV) return false;
        return true;
    }

    @Override
    public boolean parse(byte[] data)
    {
        if (!match(data)) return false;
        int pos = 4; // skip id

        macAddress = readHex(data, pos, 6);
        minor = readInt16(data, pos + 4);
        major = 8;//
        pos += 6;
        rssi = readInt8(data, pos);  pos += 1;
        int len = readInt8(data, pos);  pos += 1; // MUST BE 25

        //minor = readInt16(data, pos + 18);//.replace(":", "");
        //major = readInt16(data, pos + 20);//.replace(":", "");

        if (minor + major == 0) {

        }

        if (len < 64) {
            manufactureData = new byte[len];
            for (int i=0; i<len; i++) {
                manufactureData[i] = (byte)readInt8(data, pos + i);
            }
        }
        return true;
    }

}
