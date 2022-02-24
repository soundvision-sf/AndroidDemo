package com.soundvision.demo.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.soundvision.demo.location.ffgeojson.PointD;
import com.soundvision.demo.location.flat.BeaconProp;
import com.soundvision.demo.location.flat.VenueProp;
import com.soundvision.demo.location.ibeacon.IBeacon;

import java.util.List;

public class BeaconViewItem extends View {

    private IBeacon bp;
    private int width;
    private int height;

    TextPaint mTextPaint;
    StaticLayout mStaticLayout;

    public BeaconViewItem(Context context) {
        super(context);
    }

    public BeaconViewItem(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public BeaconViewItem(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setBeacon(IBeacon bp)
    {
        this.bp = bp;
        double distance = Math.pow(10d, ((double) (-55) - (bp.getRssi())) / (10 * 2))*100;

        initLabelView("("+bp.rssiList.getCount()+") scan:"+bp.scanCount+"  mac:"+bp.mac+"  dist:"+distance+"  err:"+bp.getRssiError()+"  rssi:"+bp.getRssi());
        invalidate();
    }

    private void initLabelView(String mText) {
        mTextPaint = new TextPaint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextSize(12 * getResources().getDisplayMetrics().density);
        mTextPaint.setColor(0xFF000000);

        // default to a single line of text
        int width = (int) mTextPaint.measureText(mText);
        mStaticLayout = new StaticLayout(mText, mTextPaint, (int) width, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0, false);

    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.save();
        canvas.translate(getPaddingLeft(), getPaddingTop());



        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(new Rect(0, 0, width, height),paint );
        //canvas.drawPaint(paint);



        mStaticLayout.draw(canvas);
        //canvas.drawText(bp.mac, 5, 5, paint);

        Paint paintLine = new Paint();
        paintLine.setColor(Color.argb(128, 255, 20, 20));
        paintLine.setStyle(Paint.Style.STROKE);
        paintLine.setStrokeWidth(5);

        Paint paintLine2 = new Paint();
        paintLine2.setColor(Color.argb(128, 200, 200, 10));
        paintLine2.setStyle(Paint.Style.STROKE);
        paintLine2.setStrokeWidth(5);

        List<float[]> list = bp.getRSSIList();
        int x = 20;
        int xstep = (width - x) / 25;
        for (int i=0; i<25; i++)
        {
            if (i+1 >= list.size()) break;
            //if (i==0)
            {
                float[] values1 = list.get(i);
                float[] values2 = list.get(i+1);
                float i1 = (Math.abs(values1[0]) / 100.0f) * height;
                float i2 = (Math.abs(values2[0]) / 100.0f) * height;
                canvas.drawLine(x, i1, x+xstep, i2, paintLine);

                i1 = (Math.abs(values1[1]) / 100.0f) * height;
                i2 = (Math.abs(values2[1]) / 100.0f) * height;
                canvas.drawLine(x, i1, x+xstep, i2, paintLine2);

                x += xstep;
            }
        }

        canvas.restore();

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        width = getMeasuredWidth();
        height = getMeasuredHeight();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Tell the parent layout how big this view would like to be
        // but still respect any requirements (measure specs) that are passed down.

        // determine the width
        int width;
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthRequirement = MeasureSpec.getSize(widthMeasureSpec);
        if (widthMode == MeasureSpec.EXACTLY || mStaticLayout == null) {
            width = widthRequirement;
        } else {
            width = mStaticLayout.getWidth() + getPaddingLeft() + getPaddingRight();
            if (widthMode == MeasureSpec.AT_MOST) {
                if (width > widthRequirement) {
                    width = widthRequirement;
                    // too long for a single line so relayout as multiline
                    mStaticLayout = new StaticLayout(bp.mac, mTextPaint, width, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0, false);
                }
            }
        }

        // determine the height
        int height;
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightRequirement = MeasureSpec.getSize(heightMeasureSpec);
        if (heightMode == MeasureSpec.EXACTLY) {
            height = heightRequirement;
        } else {
            height = mStaticLayout.getHeight() + getPaddingTop() + getPaddingBottom();
            if (heightMode == MeasureSpec.AT_MOST) {
                height = Math.min(height, heightRequirement);
            }
        }

        // Required call: set width and height
        setMeasuredDimension(width, height);
    }

}
