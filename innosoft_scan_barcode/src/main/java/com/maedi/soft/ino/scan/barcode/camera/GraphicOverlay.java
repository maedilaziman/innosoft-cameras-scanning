package com.maedi.soft.ino.scan.barcode.camera;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.FragmentActivity;
import android.util.AttributeSet;
import android.view.View;

import com.maedi.soft.ino.scan.barcode.R;
import com.maedi.soft.ino.scan.barcode.utils.PathBackgroundAnim;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import timber.log.Timber;

public class GraphicOverlay <T extends GraphicOverlay.Graphic> extends View {
    private final Object mLock = new Object();
    private int mPreviewWidth;
    private float mWidthScaleFactor = 1.0f;
    private int mPreviewHeight;
    private float mHeightScaleFactor = 1.0f;
    private int mFacing = CameraSource.CAMERA_FACING_BACK;
    private Set<T> mGraphics = new HashSet<>();
    private Context mContext;

    //public interface CommGraphicOverlay
    //{
    //    void setDraw(Context context, Canvas canvas);
    //}
    //private CommGraphicOverlay listener;

    /**
     * Base class for a custom graphics object to be rendered within the graphic overlay.  Subclass
     * this and implement the {@link Graphic#draw(Canvas)} method to define the
     * graphics element.  Add instances to the overlay using {@link GraphicOverlay#add(Graphic)}.
     */
    public static abstract class Graphic {
        private GraphicOverlay mOverlay;

        public Graphic(GraphicOverlay overlay) {
            mOverlay = overlay;
        }

        /**
         * Draw the graphic on the supplied canvas.  Drawing should use the following methods to
         * convert to view coordinates for the graphics that are drawn:
         * <ol>
         * <li>{@link Graphic#scaleX(float)} and {@link Graphic#scaleY(float)} adjust the size of
         * the supplied value from the preview scale to the view scale.</li>
         * <li>{@link Graphic#translateX(float)} and {@link Graphic#translateY(float)} adjust the
         * coordinate from the preview's coordinate system to the view coordinate system.</li>
         * </ol>
         *
         * @param canvas drawing canvas
         */
        public abstract void draw(Canvas canvas);

        /**
         * Adjusts a horizontal value of the supplied value from the preview scale to the view
         * scale.
         */
        public float scaleX(float horizontal) {
            return horizontal * mOverlay.mWidthScaleFactor;
        }

        /**
         * Adjusts a vertical value of the supplied value from the preview scale to the view scale.
         */
        public float scaleY(float vertical) {
            return vertical * mOverlay.mHeightScaleFactor;
        }

        /**
         * Adjusts the x coordinate from the preview's coordinate system to the view coordinate
         * system.
         */
        public float translateX(float x) {
            if (mOverlay.mFacing == CameraSource.CAMERA_FACING_FRONT) {
                return mOverlay.getWidth() - scaleX(x);
            } else {
                return scaleX(x);
            }
        }

        /**
         * Adjusts the y coordinate from the preview's coordinate system to the view coordinate
         * system.
         */
        public float translateY(float y) {
            return scaleY(y);
        }

        public void postInvalidate() {
            mOverlay.postInvalidate();
        }
    }

    public GraphicOverlay(Context context) {
        super(context);
        mContext = context;
        initialize();
    }

    public GraphicOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        initialize();
    }

    public GraphicOverlay(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        initialize();
    }

    /**
     * Removes all graphics from the overlay.
     */
    public void clear() {
        synchronized (mLock) {
            mGraphics.clear();
        }
        postInvalidate();
    }

    /**
     * Adds a graphic to the overlay.
     */
    public void add(T graphic) {
        synchronized (mLock) {
            mGraphics.add(graphic);
        }
        postInvalidate();
    }

    /**
     * Removes a graphic from the overlay.
     */
    public void remove(T graphic) {
        synchronized (mLock) {
            mGraphics.remove(graphic);
        }
        postInvalidate();
    }

    /**
     * Returns a copy (as a list) of the set of all active graphics.
     * @return list of all active graphics.
     */
    public List<T> getGraphics() {
        synchronized (mLock) {
            return new Vector(mGraphics);
        }
    }

    /**
     * Returns the horizontal scale factor.
     */
    public float getWidthScaleFactor() {
        return mWidthScaleFactor;
    }

    /**
     * Returns the vertical scale factor.
     */
    public float getHeightScaleFactor() {
        return mHeightScaleFactor;
    }

    /**
     * Sets the camera attributes for size and facing direction, which informs how to transform
     * image coordinates later.
     */
    public void setCameraInfo(int previewWidth, int previewHeight, int facing) {
        synchronized (mLock) {
            mPreviewWidth = previewWidth;
            mPreviewHeight = previewHeight;
            mFacing = facing;
        }
        postInvalidate();
    }

    /**
     * Draws the overlay with its associated graphic objects.
     */

    private Paint paint, paintBorder;
    private Path path;

    private void initialize() {
        this.setWillNotDraw(false);
        paint = new Paint();
        path = new Path();
        paint.setAntiAlias(true);
        paintBorder = new Paint();
        paintBorder.setAntiAlias(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        //listener.setDraw(mContext, canvas);
        //int mc = PixelCalc.DpToPixel(mContext.getResources().getDimension(R.dimen.Camera_Margin_Left), mContext)*2;

        int screen_width = ScreenSize.instance(mContext).getWidth();
        int screen_height = ScreenSize.instance(mContext).getHeight();
        String rasio = ratio(screen_width, screen_height);
        Timber.d("OpenCameraSource_RASIO_IS - "+rasio +" | "+ screen_width +" | "+ screen_height);

        int mrgLeftWindow = PixelCalc.DpToPixel(22.5f, mContext);
        int w = getWidth();// - mc;
        //int h = PixelCalc.DpToPixel(mContext.getResources().getDimension(R.dimen.Camera_Height), mContext);
        int h = PixelCalc.DpToPixel(220, mContext);
        if(rasio.equalsIgnoreCase("16:9") &&
                screen_width == 1440 && screen_height == 2560)
        {
            h = PixelCalc.DpToPixel(180, mContext);
        }

        Timber.d("OpenCameraSource_START_WIDTH_IMAGE -> "+w +" | "+ h);
        widthImageOri = w;
        heightImageOri = h;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(16);
        paint.setColor(mContext.getResources().getColor(R.color.border_def1));

        centerVert = h/4;//ScreenSize.instance(mContext).getHeight()/3;
        int leftMargin = w/9;//ScreenSize.instance(mContext).getWidth()/6;
        int rightMargin = 7*leftMargin;
        leftMargin = leftMargin*2;
        int distanceHeight = (centerVert * 2)-30;

        int rw = 30;
        int rw2 = 50;
        int rw3 = distanceHeight;
        rw4 = rw3+rw+20;
        int rw5 = 28;

        drawCurveSide(centerVert, leftMargin, rightMargin, distanceHeight, rw, rw2, rw3, rw4, rw5, canvas);

        paint.setStrokeWidth(5);
        paint.setColor(mContext.getResources().getColor(R.color.gray_1));//();
        drawCurveSide(centerVert, leftMargin, rightMargin, distanceHeight, rw, rw2, rw3, rw4, rw5, canvas);

        Paint paintLiveBcg = new Paint();
        Path pathLiveBcg = new Path();
        paintLiveBcg.setStyle(Paint.Style.FILL);
        //colorLiveAnim = colorLiveAnim == 0 ? mContext.getResources().getColor(R.color.smooth_blue_transp_0) : colorLiveAnim;
        //paintLiveBcg.setColor(colorLiveAnim);
        paintLiveBcg.setColor(mContext.getResources().getColor(R.color.smooth_blue_opc1));
        pathLiveBcg.moveTo(leftMargin-20, centerVert);
        pathLiveBcg.lineTo(rightMargin+20, centerVert);
        topLiveAnim = topLiveAnim < centerVert ? centerVert : topLiveAnim;
        pathLiveBcg.lineTo(rightMargin+20, topLiveAnim);
        pathLiveBcg.lineTo(leftMargin-20, topLiveAnim);
        pathLiveBcg.lineTo(leftMargin-20, centerVert);
        canvas.drawPath(pathLiveBcg, paintLiveBcg);

        /*
        FragmentActivity f = (FragmentActivity) mContext;
        Timer timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                f.runOnUiThread(new Runnable() {
                    public void run() {
                        topLiveAnim += centerVert+200;
                        Timber.d("OpenCameraSource_RUN_ANIMATE_LASER -> topLiveAnim = "+topLiveAnim);
                        invalidate();
                    }
                });
            }
        };
        //schedule the timer, to wake up every 1 second
        timer.schedule(timerTask, 0, 200); //
        */

        //smoothAnimateChange(centerVert, centerVert+rw4, 1000, canvas, paintLiveBcg, pathLiveBcg, leftMargin, rightMargin, centerVert);

        //PathBackgroundAnim pathBackgroundAnim = new PathBackgroundAnim(mContext);
        //pathBackgroundAnim.init();
        //pathBackgroundAnim.setPhase(1);
        /*
        paint = new Paint();
        path = new Path();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(mContext.getResources().getColor(R.color.border_def1_transp));
        path.moveTo(rightMargin+20, 0);
        path.lineTo(rightMargin+20, centerVert-5);
        path.lineTo(leftMargin-20, centerVert-5);
        path.lineTo(leftMargin-20, h);
        path.lineTo(0, h);
        path.lineTo(0, 0);
        path.lineTo(rightMargin+20, 0);
        path.lineTo(rightMargin+20, centerVert-5);
        canvas.drawPath(path, paint);
        */

        /*
        path.moveTo(rightMargin+20, 0);
        path.lineTo(rightMargin+20, centerVert-5);
        path.lineTo(leftMargin-20, centerVert-5);
        path.lineTo(leftMargin-20, h);
        path.lineTo(0, h);
        path.lineTo(0, 0);
        path.lineTo(rightMargin+20, 0);
        path.lineTo(rightMargin+20, centerVert-5);
        canvas.drawPath(path, paint);
        */
        /*
        paint = new Paint();
        path = new Path();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(mContext.getResources().getColor(R.color.border_def1_transp));
        path.moveTo(leftMargin-20, centerVert+rw4+5);
        path.lineTo(leftMargin-20, h);
        path.lineTo(w, h);
        path.lineTo(w, 0);
        path.lineTo(rightMargin+20, 0);
        path.lineTo(rightMargin+20, centerVert+rw4+5);
        canvas.drawPath(path, paint);
        */

        /*
        path.moveTo(leftMargin-20, centerVert+rw4+5);
        path.lineTo(leftMargin-20, h);
        path.lineTo(w, h);
        path.lineTo(w, 0);
        path.lineTo(rightMargin+20, 0);
        path.lineTo(rightMargin+20, centerVert+rw4+5);
        canvas.drawPath(path, paint);
        */

        paint = new Paint();
        path = new Path();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(mContext.getResources().getColor(R.color.white_1));
        /*
        int mx = mrgLeftWindow + nt;
        int my = mrgLeftWindow + 30;
        int my2 = mrgLeftWindow + 60;
        path.moveTo(mx, 0);
        path.lineTo(mrgLeftWindow, 0);
        path.lineTo(mrgLeftWindow, nt);
        path.quadTo(mrgLeftWindow, my, my2, 0);
         */
        int nn = 90;
        int sx = 60;
        int mx = mrgLeftWindow+nn;
        int my = 30;
        int my2 = mrgLeftWindow+sx;
        int wdWithMrgLeftWindow = w - mrgLeftWindow;
        path.moveTo(mx, 0);
        path.lineTo(mrgLeftWindow, 0);
        path.lineTo(mrgLeftWindow, nn);
        path.quadTo(mrgLeftWindow, my, my2, 0);

        path.moveTo(wdWithMrgLeftWindow-nn, 0);
        path.lineTo(wdWithMrgLeftWindow, 0);
        path.lineTo(wdWithMrgLeftWindow, nn);
        path.quadTo(wdWithMrgLeftWindow, my, wdWithMrgLeftWindow-sx, 0);

        path.moveTo(mx, h);
        path.lineTo(mrgLeftWindow, h);
        path.lineTo(mrgLeftWindow, h-nn);
        path.quadTo(mrgLeftWindow, h-my, my2, h);

        path.moveTo(wdWithMrgLeftWindow-nn, h);
        path.lineTo(wdWithMrgLeftWindow, h);
        path.lineTo(wdWithMrgLeftWindow, h-nn);
        path.quadTo(wdWithMrgLeftWindow, h-my, wdWithMrgLeftWindow-sx, h);

        canvas.drawPath(path, paint);

        int pl2 = 1;
        int nwnd = mrgLeftWindow+pl2;
        path.moveTo(0, 0);
        path.lineTo(nwnd, 0);
        path.lineTo(nwnd, h);
        path.lineTo(0, h);
        path.lineTo(0, 0);

        int nrWnd = w-nwnd;
        path.moveTo(w, 0);
        path.lineTo(nrWnd, 0);
        path.lineTo(nrWnd, h);
        path.lineTo(w, h);
        path.lineTo(w, 0);
        canvas.drawPath(path, paint);

        int hgCamOri = getHeight();
        path.moveTo(mx, h);
        path.lineTo(w, h);
        path.lineTo(w, hgCamOri);
        path.lineTo(0, hgCamOri);
        path.lineTo(0, h);

        canvas.drawPath(path, paint);

        Timber.d("WIDTH_AREA_SCAN - "+widthPosScan +" | "+ heightPosScan);
        synchronized (mLock) {
            if ((mPreviewWidth != 0) && (mPreviewHeight != 0)) {
                mWidthScaleFactor = (float) canvas.getWidth() / (float) mPreviewWidth;
                mHeightScaleFactor = (float) canvas.getHeight() / (float) mPreviewHeight;
            }

            for (Graphic graphic : mGraphics) {
                graphic.draw(canvas);
            }
        }
    }

    public int setAnimValLaser(int val)
    {
        topLiveAnim = topLiveAnim+val;
        if(topLiveAnim >= centerVert &&
                topLiveAnim <= (centerVert+rw4))

        {
            Timber.d("OpenCameraSource_ - topLiveAnim = " + topLiveAnim);
            invalidate();
        }
        else
        {
           if(topLiveAnim > (centerVert+rw4))
           {
               topLiveAnim = centerVert;
               invalidate();
           }
        }
        return topLiveAnim;
    }

    public int getTopLaser()
    {
        Timber.d("OpenCameraSource_ - centerVert = " + centerVert);
        return centerVert;
    }

    public int getBottomLaser()
    {
        int r = centerVert+rw4;
        Timber.d("OpenCameraSource_ - centerVert+rw4 = " + r);
        return r;
    }

    private int centerVert, rw4;
    private int topLiveAnim;
    private int colorLiveAnim = 0;
    public void smoothAnimateCreateLaser(int from, int to, int duration)
    {
        ValueAnimator varl = ValueAnimator.ofInt(from, to);
        varl.setDuration(duration);
        varl.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                topLiveAnim = (Integer) animation.getAnimatedValue();
                Timber.d("OpenCameraSource_ - topLiveAnim = " + topLiveAnim);
                invalidate();
            }
        });
        varl.start();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void changeColorAnimation(int duration)
    {
        int colorFrom = mContext.getResources().getColor(R.color.smooth_blue_transp_0);
        int colorTo = mContext.getResources().getColor(R.color.smooth_blue_opc1);
        ValueAnimator colorAnimation = ValueAnimator.ofArgb(colorFrom, colorTo);
        colorAnimation.setDuration(duration); // milliseconds
        colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                colorLiveAnim = (int) animator.getAnimatedValue();
                invalidate();
            }

        });
        colorAnimation.start();
    }

    private void drawCurveSide(
            int centerVert,
            int leftMargin,
            int rightMargin,
            int distanceHeight,
            int rw,
            int rw2,
            int rw3,
            int rw4,
            int rw5,
            Canvas canvas)
    {
        path.reset();
        path.moveTo(leftMargin+rw, centerVert);
        path.lineTo(leftMargin, centerVert);
        path.quadTo(leftMargin-20, centerVert, leftMargin-20, centerVert+30);
        path.lineTo(leftMargin-20, centerVert+rw2);

        startXPosScan = leftMargin;
        startYPosScan = centerVert;

        path.moveTo(rightMargin-rw, centerVert);
        path.lineTo(rightMargin, centerVert);
        path.quadTo(rightMargin+20, centerVert, rightMargin+20, centerVert+30);
        path.lineTo(rightMargin+20, centerVert+rw2);

        path.moveTo(leftMargin-20, centerVert+rw3);
        path.lineTo(leftMargin-20, centerVert+rw3+rw);
        path.quadTo(leftMargin-20, centerVert+rw4, leftMargin, centerVert+rw4);
        path.lineTo(leftMargin+rw5, centerVert+rw4);

        heightPosScan = centerVert+rw3+rw;

        path.moveTo(rightMargin+20, centerVert+rw3);
        path.lineTo(rightMargin+20, centerVert+rw3+rw);
        path.quadTo(rightMargin+20, centerVert+rw4, rightMargin, centerVert+rw4);
        path.lineTo(rightMargin-rw5, centerVert+rw4);

        widthPosScan = (rightMargin+20)-leftMargin;

        canvas.drawPath(path, paint);
    }

    private static int gcd(int p, int q) {
        if (q == 0) return p;
        else return gcd(q, p % q);
    }

    private String ratio(int a, int b) {
        final int gcd = gcd(a,b);
        if(a > b) {
            return showRatio(a/gcd, b/gcd);
        } else {
            return showRatio(b/gcd, a/gcd);
        }
    }

    private String showRatio(int a, int b) {
        String res = a +":"+ b;
        Timber.d("SHOW_RATIO - "+res);
        return res;
    }

    public static int startXPosScan = 0;
    public static int startYPosScan = 0;
    public static int widthPosScan = 0;
    public static int heightPosScan = 0;

    public static int widthImageOri = 0;
    public static int heightImageOri = 0;
}