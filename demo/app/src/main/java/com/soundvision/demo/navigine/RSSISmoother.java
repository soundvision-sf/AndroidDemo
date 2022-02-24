package com.soundvision.demo.navigine;

public class RSSISmoother {

    static double MAX_ALPHA = 0.9;
    static double MIN_ALPHA = 0.1;
    static double TIME_THRESHOLD_SEC = 10;

    boolean mWasCalled = false;
    double mSpeedX =   0.0f ;
    double mSpeedY =   0.0f ;
    double mAlpha;
    double mRssi;
    long mLast_ts = -1;

    public RSSISmoother(double smoothing)
    {
        smoothing = Math.min(1.0, Math.max(0.0, smoothing));
        mAlpha = MIN_ALPHA * smoothing + MAX_ALPHA * (1.0 - smoothing);
    }

    public float smoothRssi(double newRssi)
    {
        //if (mPosition == null) mPosition = new Position(pos);
        long pos_ts = System.currentTimeMillis();
        if (pos_ts > mLast_ts)
        {
            double t = (pos_ts - mLast_ts) / 1000.0;
            double a = mAlpha;
            double b = a * a / (2.0 - a);

            double xp =mRssi + mSpeedX * t;
            double xs = xp + a * (newRssi - xp);

            boolean timeIsTooLong = t > TIME_THRESHOLD_SEC;

            if (!mWasCalled || timeIsTooLong)
            {
                mRssi = newRssi;
                mSpeedX = 0.0;
                mSpeedY = 0.0;
                mWasCalled = true;
                return (float)mRssi;
            }

            mRssi = xs;

        }

        return (float)mRssi;
    }

    
}
