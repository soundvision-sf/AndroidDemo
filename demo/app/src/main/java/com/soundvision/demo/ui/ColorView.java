package com.soundvision.demo.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.CalendarContract;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;

import com.soundvision.demo.R;
import com.soundvision.demo.palette.ColorFormat;

import java.nio.ByteBuffer;

public class ColorView extends View {

    private int baseHue[] = new int[255];
    private int y_pad = 8;
    private Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint backPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint fillSelection = new Paint(Paint.ANTI_ALIAS_FLAG);

    private String hueName[] = {"red",  "orange", "yellow",
            "lime", "green", "aqua",
            "blue","indigo",  "violet",
            "purple", "magenta", "rose"};

    private int hueColorMapBase[] = { 15, 47, 72, 81, 165, 195, 242, 262, 272, 293, 329, 346};
    private int hueColorMap[] = hueColorMapBase;
    private int hueIndex = 0;

    private Bitmap hueBk = BitmapFactory.decodeResource(getResources(), R.drawable.hue);

    public ColorView(Context context) {
        super(context);
        init();
    }

    public ColorView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ColorView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void drawZone(Canvas canvas, float angle, int color, boolean focus)
    {
        canvas.setDrawFilter(null);
        float cx = (getWidth()) / 2;
        float cy = (getHeight()) / 2;
        float R = cy - 28;

        float x = cx + (float)(Math.sin(Math.toRadians(angle)) * R);
        float y = cy - (float)(Math.cos(Math.toRadians(angle)) * R);

        linePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        canvas.drawLine(x, y, cx, cy, linePaint);
        final Paint p = new Paint();

        int col = ColorFormat.HLStoRGB(new ColorFormat.ColorHSL((int)(angle / 360 * 255), 200, 200));

        p.setStyle(Paint.Style.FILL_AND_STROKE);
        p.setStrokeJoin(Paint.Join.ROUND);
        if (focus) {
            p.setAntiAlias(true);
            p.setDither(true);
            p.setColor(Color.GRAY);
            p.setStrokeJoin(Paint.Join.ROUND);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeWidth(10);
            canvas.drawCircle(x, y, 20, p);
        }
        p.setColor(col);
        p.setAlpha(255);
        canvas.drawCircle(x, y, 16, p);

    }

    @Override
    public Parcelable onSaveInstanceState() {
        // Force our ancestor class to save its state
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);

        ss.hueIndex = hueIndex;
        ss.hueMap = hueColorMap;

        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());

        hueIndex = ss.hueIndex;
        hueColorMap = ss.hueMap;
    }

    public void setPosition(int zone, int value)
    {
        //int v = Math.min(255, value);
        //fillColor = 0xff000000 | (v) | (v << 8) | (v << 16);
        hueColorMap[zone] = (value + 360) % 360;
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //setBackgroundColor(Color.BLACK);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Rect rect = new Rect(0, 0, hueBk.getWidth(), hueBk.getHeight());

        int x = (getWidth() - getHeight()) / 2;
        final RectF drect = new RectF(x+y_pad, y_pad, x+getHeight()-y_pad, getHeight()-y_pad);

        for (int i=0; i<12; i++)
        {
            if (i == hueIndex) {
                float alen = ((hueColorMap[i] - hueColorMap[((i - 1) + 12) % 12]) + 360) % 360;
                canvas.drawArc(drect, hueColorMap[((i - 1) + 12) % 12]+270, alen, true, fillSelection);
            }
        }

        paint.setColor(Color.GRAY);
        float cx = (getWidth()) / 2;
        float cy = (getHeight()) / 2;
        canvas.drawCircle(cx, cy, cy - 60, paint);
        paint.setAlpha(255);
        canvas.drawBitmap(hueBk, rect, drect, paint);




        for (int i=0; i<12; i++)
        {
            drawZone(canvas, hueColorMap[i], 0, i==hueIndex);
        }

    }

    private void init()
    {
        for (int hue=0; hue<255; hue++)
        {
            baseHue[hue] = ColorFormat.HLStoRGB(new ColorFormat.ColorHSL(hue, 255, 255));
        }
        linePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        linePaint.setColor(0xff202020);
        backPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        backPaint.setColor(0xff000000);
        fillSelection.setStyle(Paint.Style.FILL_AND_STROKE);
        fillSelection.setColor(0x3f808080);
        build();
    }

    public String getColorName(int index)
    {
        return hueName[index];
    }

    public int getHueIndex()
    {
        return hueIndex;
    }

    public int setHueIndex(int newIndex)
    {
        int v = (newIndex + 12) % 12;
        hueIndex = v;
        invalidate();
        return hueIndex;
    }

    public void getHueRange(int index, int[] ranges)
    {
        for (int i=0; i<3; i++) {
            int v = (index + (i-1) + 12) % 12;
            ranges[i] = hueColorMap[v];
        }
    }

    private int incColor(int c, float value)
    {
        int B = Math.min(255, Math.max(0, (int)(value * (c & 0xff))));
        int G = Math.min(255, Math.max(0, (int)(value * ((c >> 8 ) & 0xff))));
        int R = Math.min(255, Math.max(0, (int)(value * ((c >> 16 ) & 0xff))));
        return B | (G << 8) | (R << 16);
    }

    private void build()
    {
/*
        int[] pixels = new int[hueData.getWidth() * hueData.getHeight()];
        for (int x=0; x<255; x++) {
            for (int h=0; h<255; h++) {
                int v = Math.abs(255-(2*h));
                pixels[(h*255) +(x)] = ColorFormat.HLStoRGB(new ColorFormat.ColorHSL(x, h, 255-v));
            }
        }
        hueData.setPixels(pixels, 0, hueData.getWidth(), 0, 0, hueData.getWidth(), hueData.getHeight());

*/
    }

    static class SavedState extends BaseSavedState {
        int hueIndex;
        int[] hueMap;

        /**
         * Constructor called from {@link ProgressBar#onSaveInstanceState()}
         */
        SavedState(Parcelable superState) {
            super(superState);
        }

        /**
         * Constructor called from {@link #CREATOR}
         */
        private SavedState(Parcel in) {
            super(in);
            hueIndex = in.readInt();
            int count = in.readInt();
            hueMap = new int[count];
            in.readIntArray(hueMap);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(hueIndex);
            out.writeInt(hueMap.length);
            out.writeIntArray(hueMap);
        }

        public static final Parcelable.Creator<SavedState> CREATOR
                = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

}
