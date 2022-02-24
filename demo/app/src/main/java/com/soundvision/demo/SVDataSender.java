package com.soundvision.demo;

import android.os.Handler;

import com.scalefocus.soundvision.ble.BLETransferService;

import java.nio.ByteBuffer;
import java.util.ArrayList;


 public class SVDataSender implements Runnable {

     Handler mHandler = new Handler();
     private BLETransferService mService = null;
     public boolean mBleConnected = false;

     public SVDataSender(BLETransferService service)
     {
         mService = service;
     }

    ArrayList<byte[]> taskList = new ArrayList<byte[]>();
    @Override
    public void run() {
        try {
            if (mBleConnected && (mService != null)) {
                byte[] t = task();
                mService.sendCommand(t);
            }
        } finally {

        }
    }

    synchronized private byte[] task()
    {
        if (taskList.size() == 0) return null;
        byte[] ret = taskList.get(0);
        taskList.remove(0);
        return ret;
    }

    synchronized public void send(byte code)
    {
        taskList.add(new byte[]{code});
        mHandler.post(this);
    }

    synchronized public void send(byte code, byte p1)
    {
        taskList.add(new byte[]{code, p1});
        mHandler.post(this);
    }

    synchronized public void send(byte code, byte data[])
    {
        ByteBuffer bb = ByteBuffer.allocate(1+data.length);
        bb.put(code);
        bb.put(data);
        taskList.add(bb.array());
        mHandler.post(this);
    }

    synchronized public void send(byte code, byte p1, byte p2)
    {
        taskList.add(new byte[]{code, p1, p2});
        mHandler.post(this);
    }

    synchronized public void sendDelayed(byte code, byte p1, byte p2, int delay)
    {
        taskList.add(new byte[]{code, p1, p2});
        mHandler.postDelayed(this, delay);
    }

    synchronized public void sendDelayed(byte code, int delay)
    {
        taskList.add(new byte[]{code});
        mHandler.postDelayed(this, delay);
    }

    synchronized public void skip()
    {
        task();
        mHandler.removeCallbacks(this);
    }

};