package com.maedi.soft.ino.scan.barcode.utils;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.PathMeasure;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class PathBackgroundAnim extends View
{
    Path path;
    Paint paint;
    float length;

    public PathBackgroundAnim(Context context)
    {
        super(context);
    }

    public PathBackgroundAnim(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public PathBackgroundAnim(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
    }

    public void init()
    {
        paint = new Paint();
        paint.setColor(Color.BLUE);
        paint.setStrokeWidth(10);
        paint.setStyle(Paint.Style.FILL);

        path = new Path();
        path.moveTo(200, 500);
        path.lineTo(600, 500);
        path.lineTo(600, 1000);
        path.lineTo(200, 1000);
        path.lineTo(200, 500);

        // Measure the path
        PathMeasure measure = new PathMeasure(path, false);
        length = measure.getLength();

        float[] intervals = new float[]{length, length};

        ObjectAnimator animator = ObjectAnimator.ofFloat(PathBackgroundAnim.this, "phase", 1.0f, 0.0f);
        animator.setDuration(1000);
        animator.start();
    }

    //is called by animtor object
    public void setPhase(float phase)
    {
        Log.d("pathview","setPhase called with:" + String.valueOf(phase));
        paint.setPathEffect(createPathEffect(length, phase, 0.0f));
        invalidate();//will calll onDraw
    }

    private static PathEffect createPathEffect(float pathLength, float phase, float offset)
    {
        return new DashPathEffect(new float[] { pathLength, pathLength },
                Math.max(phase * pathLength, offset));
    }

    @Override
    public void onDraw(Canvas c)
    {
        super.onDraw(c);
        c.drawPath(path, paint);
    }
}