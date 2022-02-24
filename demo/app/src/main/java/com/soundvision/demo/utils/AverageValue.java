package com.soundvision.demo.utils;

import java.util.ArrayList;
import java.util.List;

public class AverageValue {

    private float[] list;
    private float[] average;
    private double sum;
    private int pos = 0;
    private int length = 0;

    public AverageValue(int size)
    {
        setLength(size);
    }

    public void reset() {
        sum = 0;
        pos = 0;
        length = 0;
    }

    public int getCount() {
        return length;
    }

    private void setLength(int len) {
        list = new float[len];
        average = new float[len];
        reset();
    }

    public float add(float v)
    {
        float newVal = v;
        if (isReady()) {
            sum -= list[pos];
/*
            float dx = newVal - ((float)(sum / length));
            if (dx>2) {
                newVal = (float)(sum / length) + 2;
            } else
            if (dx<-2)  {
                newVal = (float)(sum / length) - 2;
            }*/
        }
        else
            length++;



        list[pos] = newVal;
        sum += newVal;
        average[pos] = (float)(sum / length);
        pos++;
        if (pos>=list.length) pos = 0;

        return (float)(sum / length);
    }

    public float max()
    {
        float max = Float.MIN_VALUE;
        for (float f:list) {
            max = Math.max(f, max);
        }
        return max;
    }

    public boolean isReady()
    {
        return length == list.length;
    }

    public List<float[]> getList()
    {
        List<float[]> ret = new ArrayList<float[]>();
        int p = pos;
        for (int i=0; i<length; i++)
        {
            int idx = (p-1) - i;
            if (idx>=list.length) idx = idx - list.length;
            if (idx<0) idx = idx + list.length;
            float[] v = {list[idx], average[idx]};
            ret.add(v);
        }
        return ret;
    }

    public float getValue()
    {
        return (float)(sum / length);
    }

}
