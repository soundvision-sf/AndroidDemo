package com.soundvision.demo.navigine;

public class PositionSmoother {

    static double MAX_ALPHA = 0.9;
    static double MIN_ALPHA = 0.1;
    static double TIME_THRESHOLD_SEC = 10;

    boolean mWasCalled = false;
    double mSpeedX =   0.0f ;
    double mSpeedY =   0.0f ;
    double mAlpha;
    Position mPosition;

    public PositionSmoother(double smoothing)
    {
        smoothing = Math.min(1.0, Math.max(0.0, smoothing));
        mAlpha = MIN_ALPHA * smoothing + MAX_ALPHA * (1.0 - smoothing);
    }

    public Position smoothPosition(Position pos)
    {
        if (mPosition == null) mPosition = new Position(pos);
        if (pos.ts > mPosition.ts)
        {
            double t = (pos.ts - mPosition.ts) / 1000.0;
            double a = mAlpha;
            double b = a * a / (2.0 - a);

            double xp = mPosition.x + mSpeedX * t;
            double vxp = mSpeedX + (b / t) * (pos.x - xp);
            double xs = xp + a * (pos.x - xp);

            double yp = mPosition.y + mSpeedY * t;
            double vyp = mSpeedY + (b / t) * (pos.y - yp);
            double ys = yp + a * (pos.y - yp);

            boolean timeIsTooLong = t > TIME_THRESHOLD_SEC;

            if (!mWasCalled || timeIsTooLong)
            {
                mPosition = new Position(pos);
                mSpeedX = 0.0;
                mSpeedY = 0.0;
                mWasCalled = true;
                return pos;
            }

            mSpeedX = vxp;
            mSpeedY = vyp;

            pos.x = xs;
            pos.y = ys;

            mPosition = new Position(pos);
        }

        return mPosition;
    }



}
