package com.soundvision.demo.utils;

import java.util.ArrayList;
import java.util.List;

public class AverageValueTime {

    class AverageStatRec {
        public float last;
        public float average;
        public long time;

        public AverageStatRec(float last, float average, long time) {
            this.last = last;
            this.average = average;
            this.time = time;
        }
    }

    public int getCount()
    {
        return items.size();
    }

    public List<AverageStatRec> items;

    private double sum;
    private int maxLength = 0;
    private long timeFrame = 0;

    private float min;
    private float max;

    public AverageValueTime(int size, long timeFrame)
    {
        setLength(size);
        this.timeFrame = timeFrame;
    }

    public void reset() {
        sum = 0;
        items.clear();
    }

    private void setLength(int len) {
        items = new ArrayList<>();
        maxLength = len;
        reset();
    }

    public float getErr()
    {
        return max - min;
    }

    public long getTs(boolean last)
    {
        if (items.size() == 0) return -1;
        if (last)
            return items.get(0).time;
            else
            return items.get(items.size()-1).time;
    }

    public float add(float v)
    {
        float newVal = v;

        long T = System.currentTimeMillis();

        while (items.size()>1 && (items.size()>=maxLength || (T- items.get(0).time > timeFrame))) {
            sum -= items.get(0).last;
            items.remove(0);
        }

        sum = newVal;

        min = newVal;
        max = newVal;

        //long s = 0;
        for (int i=0; i<items.size(); i++)
        {
            float val = items.get(i).last;
            sum += val;
            min = Math.min(min, val);
            max = Math.max(max, val);
        }

        AverageStatRec rec = new AverageStatRec(newVal, (float)(sum / (items.size()+1)), T);
        items.add(rec);

        return rec.average;
    }

    public List<float[]> getList()
    {
        List<float[]> ret = new ArrayList<float[]>();

        for (int i=0; i<items.size(); i++)
        {
            float[] v = {items.get(i).last, items.get(i).average};
            ret.add(v);
        }
        return ret;
    }

    public float max()
    {
        float max = Float.MIN_VALUE;
        for (AverageStatRec f:items) {
            max = Math.max(f.last, max);
        }
        return max;
    }

    public float getValue()
    {
        return (float)(sum / items.size());
    }

}
