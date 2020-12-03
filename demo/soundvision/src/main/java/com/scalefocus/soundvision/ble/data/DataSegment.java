package com.scalefocus.soundvision.ble.data;

import android.util.Log;

public class DataSegment extends DataParser{

    private int sessionId;
    private int type;
    private int length;
    private int readSize = 0;
    private byte[] segmentData;
    private boolean ready;

    public DataSegment(byte[] data)
    {
        parse(data);
        //Log.w("BLE_DS", "new Data segment : "+sessionId+" type:"+type);
    }

    public static String UID(byte[] data)
    {
        if (data == null || data.length == 0) return "";
        return readInt(data, 0)+"_"+readInt(data, 4);
    }

    public int getSessionId()
    {
        return sessionId;
    }

    public boolean isReady()
    {
        return ready;
    }

    public int getLength()
    {
        return length;
    }

    public byte[] getData()
    {
        return segmentData;
    }

    public int getCurrentSize()
    {
        return readSize;
    }

    public int getType()
    {
        return type;
    }

    public byte[] toBytes()
    {
        byte[] data = new byte[8];
        writeInt(data, 0, sessionId);
        writeInt(data, 4, type);
        return data;
    }

    @Override
    public boolean parse(byte[] data)
    {
        sessionId = readInt(data, 0);
        type = readInt(data, 4);
        int full_size = readInt(data, 8);
        int offset = readInt(data, 12);
        int segment_length = readInt(data, 16);
        if (readSize == 0) {
            length = full_size;
            segmentData = new byte[length];
        }
        if (offset + segment_length > full_size) {
            return false;
        }
        System.arraycopy(data, 20, this.segmentData, offset, segment_length);
        readSize += segment_length;
        ready = readSize ==  length;
        return true;
    }


}
