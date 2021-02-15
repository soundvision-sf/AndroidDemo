package com.scalefocus.soundvision.ble.data;

public abstract class DataParser {

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    public static int readInt(byte[] arr, int offset)
    {
        return (arr[offset] & 0xFF) | ((arr[offset+1]  & 0xFF) << 8) | ((arr[offset+2]  & 0xFF) << 16) | ((arr[offset+3] & 0xFF) << 24);
    }

    public static int readInt16(byte[] arr, int offset)
    {
        return (arr[offset] & 0xFF) | ((arr[offset+1]  & 0xFF) << 8);
    }

    public static int readInt8(byte[] arr, int offset)
    {
        return (arr[offset] & 0xFF);
    }

    public static void writeInt(byte[] arr, int offset, int data)
    {
        arr[offset]   = (byte)(data & 0xFF);
        arr[offset+1] = (byte)((data >> 8) & 0xFF);
        arr[offset+2] = (byte)((data >> 16) & 0xFF);
        arr[offset+3] = (byte)((data >> 24) & 0xFF);
    }

    public static long readLong(byte[] arr, int offset)
    {
        return (arr[offset] & 0xFF) | ((arr[offset+1]  & 0xFF) << 8) | ((arr[offset+2]  & 0xFF) << 16) | ((arr[offset+3] & 0xFF) << 24) |
          ((arr[offset+4]  & 0xFF) << 28) | ((arr[offset+5]  & 0xFF) << 32) | ((arr[offset+6]  & 0xFF) << 36) | ((arr[offset+7] & 0xFF) << 40);
    }

    public static byte readByte(byte[] arr, int offset)
    {
        return arr[offset];
    }

    public static String readHex(byte[] bytes, int offset, int len) {
        StringBuilder ret = new StringBuilder();
        //char[] hexChars = new char[bytes.length * 3];
        for (int j = len-1; j >= 0; j--) {
            int v = bytes[j + offset] & 0xFF;
            ret.append(HEX_ARRAY[v >>> 4]);
            ret.append(HEX_ARRAY[v & 0x0F]);
            if (j > 0)
                ret.append(':');
        }
        return ret.toString();
    }

    public boolean parse(byte[] data)
    {
        return false;
    }

    public byte[] toData() { return null;}

}
