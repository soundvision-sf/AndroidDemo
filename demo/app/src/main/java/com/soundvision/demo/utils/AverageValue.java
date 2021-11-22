package com.soundvision.demo.utils;

public class AverageValue {

    private float[] list;
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

    private void setLength(int len) {
        list = new float[len];
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



        list[pos++] = newVal;
        if (pos>=list.length) pos = 0;
        sum += newVal;
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

    public float getValue()
    {
        return (float)(sum / length);
    }

}
