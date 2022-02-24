package com.scalefocus.soundvision.ble.data;

public class BLEScanAdvertising extends DataParser {

    private static final int SV_BLE_SCAN_ADV = 0x56444142;

    public String macAddress;
    public int minor;
    public int major;
    public int rssi = 0;
    public int txPower = -59;
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

        /*
        * memcpy(&data.data[0], &chunk, 4);
    memcpy(&data.data[4], addr, 6);
    data.data[BLE_GAP_ADDR_LEN+4] = rssi;//memcpy(&data.data[BLE_GAP_ADDR_LEN], &rssi, 1);

    memcpy(&data.data[BLE_GAP_ADDR_LEN+5], &len, 1);
    memcpy(&data.data[BLE_GAP_ADDR_LEN+6], adv, len);
        *
        * */

        macAddress = readHex(data, pos, 6);

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

        minor = readInt16(manufactureData, 15);
        major = readInt16(manufactureData, 17);
        int txPower = -75;//manufactureData[19] - 255;

        return true;
    }

}
