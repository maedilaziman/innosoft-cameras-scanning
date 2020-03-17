package com.maedi.soft.ino.scan.barcode;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.maedi.soft.ino.base.BuildActivity;
import com.maedi.soft.ino.base.func_interface.ActivityListener;
import com.maedi.soft.ino.base.func_interface.ServicesListener;
import com.maedi.soft.ino.base.store.MapDataParcelable;
import com.maedi.soft.ino.scan.barcode.activity.BarcodeGraphic;
import com.maedi.soft.ino.scan.barcode.activity.BarcodeGraphicTracker;
import com.maedi.soft.ino.scan.barcode.activity.BarcodeTrackerFactory;
import com.maedi.soft.ino.scan.barcode.camera.CameraSource;
import com.maedi.soft.ino.scan.barcode.camera.CameraSourcePreview;
import com.maedi.soft.ino.scan.barcode.camera.GraphicOverlay;
import com.maedi.soft.ino.scan.barcode.camera.PixelCalc;
import com.maedi.soft.ino.scan.barcode.camera.ScreenSize;
import com.maedi.soft.ino.scan.barcode.utils.DataStatic;
import com.maedi.soft.ino.scan.barcode.utils.FuncHelper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

public class MainActivity extends BuildActivity<View> implements ActivityListener<Integer>, ServicesListener, BarcodeGraphicTracker.BarcodeUpdateListener, CameraSource.CommCameraSource {

    private final String TAG = this.getClass().getName()+"- BARCODE_SCANNER - ";

    private FragmentActivity f;

    private static final int RC_HANDLE_GMS = 9001;
    // permission request codes need to be < 256
    private static final int RC_HANDLE_CAMERA_PERM = 2;

    public static final String BarcodeObject = "Barcode";

    private CameraSourcePreview mPreview;

    private GraphicOverlay<BarcodeGraphic> mGraphicOverlay;

    private CameraSource mCameraSource;

    // helper objects for detecting taps and pinches.
    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector gestureDetector;

    private final boolean autoFocus = true;
    private final boolean useFlash = false;

    private Timer timerLaser;
    TimerTask timerTaskLaser;

    @Override
    public int setPermission() {
        return 0;
    }

    @Override
    public boolean setAnalytics() {
        return false;
    }

    @Override
    public int baseContentView() {
        return R.layout.activity_main;
    }

    @Override
    public ActivityListener createListenerForActivity() {
        return this;
    }

    @Override
    public void onCreateActivity(Bundle savedInstanceState) {
        f = this;
        ButterKnife.bind(this);

        mPreview = (CameraSourcePreview) findViewById(R.id.preview);
        mGraphicOverlay = (GraphicOverlay<BarcodeGraphic>) findViewById(R.id.graphicOverlay);
    }

    @Override
    public void onBuildActivityCreated() {
        init();
    }

    private void init()
    {
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            openCameraNow();
        }
        else {
            requestCameraPermission();
        }

        gestureDetector = new GestureDetector(this, new CaptureGestureListener());
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());

        int screen_width = ScreenSize.instance(f).getWidth();
        int screen_height = ScreenSize.instance(f).getHeight();
        String rasio = FuncHelper.ratio(screen_width, screen_height);

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        int scTop = ScreenSize.instance(f).getHeight()/4;
        layoutParams.setMargins(0, scTop, 0, 0);//l,t,r,b
        mPreview.setLayoutParams(layoutParams);

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                mGraphicOverlay.smoothAnimateCreateLaser(mGraphicOverlay.getTopLaser(), mGraphicOverlay.getBottomLaser(), durationLaser);
            }
        }, 400);
        startTimerLaser();
    }

    private void startCameraSource() throws SecurityException {
        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource, mGraphicOverlay);
            } catch (IOException e) {
                Timber.d(TAG+"Unable to start camera source."+e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    private void openCameraNow()
    {
        createCameraSource(autoFocus, useFlash);
    }


    @SuppressLint("InlinedApi")
    private void createCameraSource(boolean autoFocus, boolean useFlash) {
        Context context = getApplicationContext();

        // A barcode detector is created to track barcodes.  An associated multi-processor instance
        // is set to receive the barcode detection results, track the barcodes, and maintain
        // graphics for each barcode on screen.  The factory is used by the multi-processor to
        // create a separate tracker instance for each barcode.
        BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(context).build();
        //DisplayMetrics dm = getResources().getDisplayMetrics();
        //int height = dm.heightPixels;
        //int width = dm.widthPixels;
        //BoxDetector bx = new BoxDetector(barcodeDetector, height, width);
        BarcodeTrackerFactory barcodeFactory = new BarcodeTrackerFactory(mGraphicOverlay, this);
        barcodeDetector.setProcessor(new MultiProcessor.Builder<>(barcodeFactory).build());
        //bx.setProcessor(new MultiProcessor.Builder<>(barcodeFactory).build());
        barcodeFactory.createLine();

        if (!barcodeDetector.isOperational()) {
            // Note: The first time that an app using the barcode or face API is installed on a
            // device, GMS will download a native libraries to the device in order to do detection.
            // Usually this completes before the app is run for the first time.  But if that
            // download has not yet completed, then the above call will not detect any barcodes
            // and/or faces.
            //
            // isOperational() can be used to check if the required native libraries are currently
            // available.  The detectors will automatically become operational once the library
            // downloads complete on device.
            Timber.d(TAG+" Detector dependencies are not yet available.");

            // Check for low storage.  If there is low storage, the native library will not be
            // downloaded, so detection will not become operational.
            IntentFilter lowstorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean hasLowStorage = registerReceiver(null, lowstorageFilter) != null;

            if (hasLowStorage) {
                Toast.makeText(this, R.string.low_storage_error, Toast.LENGTH_LONG).show();
                Timber.d(TAG+getString(R.string.low_storage_error));
            }
        }

        // Creates and starts the camera.  Note that this uses a higher resolution in comparison
        // to other detection examples to enable the barcode detector to detect small barcodes
        // at long distances.
        //Timber.d(TAG+"HeightCameraView = "+scanWidth+" <> "+scanHeight);

        int[] prevSize = calcScreenAvg();
        CameraSource.Builder builder = new CameraSource.Builder(getApplicationContext(), barcodeDetector)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedPreviewSize(prevSize[0], prevSize[1])//1650x1880//1440x2560//1920x1080.
                .setRequestedFps(15.0f);
        Timber.d(TAG+"Camera OPEN!");
        // make sure that auto focus is an available option
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            builder = builder.setFocusMode(
                    autoFocus ? Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE : null);
        }

        mCameraSource = builder
                .setFlashMode(useFlash ? Camera.Parameters.FLASH_MODE_TORCH : null)
                .setCommCameraSource(this).build();
    }

    private int[] calcScreenAvg()
    {
        int screen_width = ScreenSize.instance(f).getWidth();
        int screen_height = ScreenSize.instance(f).getHeight();
        String rasio = FuncHelper.ratio(screen_width, screen_height);
        Timber.d(TAG+"SCREEN_CALC_WIDTH - screen_width = "+screen_width +" : "+ screen_height);
        int avg = screen_height/screen_width;
        Timber.d(TAG+"SCREEN_CALC_WIDTH - average = "+avg);

        int widthPreviewSize = 1440;
        int heightPreviewSize = 2560;

        if(avg == 1)
        {
            if(screen_width == 1080 &&
                    screen_height == 1920)
            {
                widthPreviewSize = 1650;
                heightPreviewSize = 1880;
            }
            else if(screen_width == 1440 &&
                    screen_height == 2560)
            {
                widthPreviewSize = 1280;
                heightPreviewSize = 2560;
            }
        }
        else if(avg == 2)
        {
            //widthPreviewSize = 1650;
            //heightPreviewSize = 1880;

            widthPreviewSize = 1650;
            heightPreviewSize = 2960;
        }
        return new int[]{widthPreviewSize, heightPreviewSize};
    }

    private void requestCameraPermission() {
        Timber.d(TAG+"Camera permission is not granted. Requesting permission");
        if (FuncHelper.hasAPI_LEVEL24_ANDROID_7_Above())
            FuncHelper.CameraPermission_API_LEVEL24_ANDROID_7_Above(f);
        else FuncHelper.CameraPermission(f);

        /*
        final String[] permissions = new String[]{Manifest.permission.CAMERA};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
            return;
        }

        final Activity thisActivity = this;

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(thisActivity, permissions, RC_HANDLE_CAMERA_PERM);
            }
        };
        */
    }

    private boolean onTap(float rawX, float rawY) {
        // Find tap point in preview frame coordinates.
        int[] location = new int[2];
        mGraphicOverlay.getLocationOnScreen(location);
        float x = (rawX - location[0]) / mGraphicOverlay.getWidthScaleFactor();
        float y = (rawY - location[1]) / mGraphicOverlay.getHeightScaleFactor();

        // Find the barcode whose center is closest to the tapped point.
        Barcode best = null;
        float bestDistance = Float.MAX_VALUE;
        for (BarcodeGraphic graphic : mGraphicOverlay.getGraphics()) {
            Barcode barcode = graphic.getBarcode();
            if (barcode.getBoundingBox().contains((int) x, (int) y)) {
                // Exact hit, no need to keep looking.
                best = barcode;
                break;
            }
            float dx = x - barcode.getBoundingBox().centerX();
            float dy = y - barcode.getBoundingBox().centerY();
            float distance = (dx * dx) + (dy * dy);  // actually squared distance
            if (distance < bestDistance) {
                best = barcode;
                bestDistance = distance;
            }
        }

        if (best != null) {
            Intent data = new Intent();
            data.putExtra(BarcodeObject, best);
            setResult(CommonStatusCodes.SUCCESS, data);
            finish();
            return true;
        }
        return false;
    }

    private Timer timerWait;

    @Override
    public void onBarcodeDetected(final Barcode barcode) {
        Timber.d(TAG+"BARCODE_DETECTED = "+barcode.displayValue);

        String message = new String(barcode.displayValue);
        if(message.startsWith("00"))message = message.substring(2, message.length());
        else if(message.startsWith("0"))message = message.substring(1, message.length());

        Timber.d(TAG + "Data_Message - " + message);
        //showSuccessPair();
        final String msg = message;

    }

    //private int darkVal = 0;
    @Override
    public void backgroundLight(int isDark) {

        //if(isDark == 0)
        //{
        //    darkVal++;
        //    if(darkVal == 4)
        //    {
        //        darkVal = 0;
        //        errorInfo.setVisibility(View.VISIBLE);
        //        errorInfo.setText("Point your camera at the barcode. Move Closer to the barcode. Move to a lighter area.");
        //    }
        //}
        //else
        //{
        //    errorInfo.setVisibility(View.GONE);
        //    errorInfo.setText("");
        //}
    }

    private class CaptureGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            return onTap(e.getRawX(), e.getRawY()) || super.onSingleTapConfirmed(e);
        }
    }

    private class ScaleListener implements ScaleGestureDetector.OnScaleGestureListener {

        /**
         * Responds to scaling events for a gesture in progress.
         * Reported by pointer motion.
         *
         * @param detector The detector reporting the event - use this to
         *                 retrieve extended info about event state.
         * @return Whether or not the detector should consider this event
         * as handled. If an event was not handled, the detector
         * will continue to accumulate movement until an event is
         * handled. This can be useful if an application, for example,
         * only wants to update scaling factors if the change is
         * greater than 0.01.
         */
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            return false;
        }

        /**
         * Responds to the beginning of a scaling gesture. Reported by
         * new pointers going down.
         *
         * @param detector The detector reporting the event - use this to
         *                 retrieve extended info about event state.
         * @return Whether or not the detector should continue recognizing
         * this gesture. For example, if a gesture is beginning
         * with a focal point outside of a region where it makes
         * sense, onScaleBegin() may return false to ignore the
         * rest of the gesture.
         */
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return true;
        }

        /**
         * Responds to the end of a scale gesture. Reported by existing
         * pointers going up.
         * <p/>
         * Once a scale has ended, {@link ScaleGestureDetector#getFocusX()}
         * and {@link ScaleGestureDetector#getFocusY()} will return focal point
         * of the pointers remaining on the screen.
         *
         * @param detector The detector reporting the event - use this to
         *                 retrieve extended info about event state.
         */
        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            mCameraSource.doZoom(detector.getScaleFactor());
        }
    }

    @Override
    public void onActivityResume() {
        startTimerLaser();
        startCameraSource();
    }

    @Override
    public void onActivityPause() {
        stoptimerTaskLaser();
        if (mPreview != null) {
            mPreview.stop();
        }
    }

    @Override
    public void onActivityStop() {
        stoptimerTaskLaser();
    }

    @Override
    public void onActivityDestroy() {
        stoptimerTaskLaser();
        if (mPreview != null) {
            mPreview.release();
        }
    }

    @Override
    public void onActivityKeyDown(int keyCode, KeyEvent event) {

    }

    @Override
    public void onActivityFinish() {

    }

    @Override
    public void onActivityRestart() {

    }

    @Override
    public void onActivitySaveInstanceState(Bundle outState) {

    }

    @Override
    public void onActivityRestoreInstanceState(Bundle savedInstanceState) {

    }

    @Override
    public void onActivityRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode != DataStatic.PERMISSION_REQUEST_ACCESS_CAMERA_ABOVE6) {
            Log.d(TAG, "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Timber.d(TAG+"Camera permission granted - initialize the camera source");
            // we have permission, so create the camerasource
            openCameraNow();
            return;
        }

        Timber.d(TAG+"Permission not granted: results len = " + grantResults.length +
                " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

        finish();
    }

    @Override
    public void onActivityMResult(int requestCode, int resultCode, Intent data) {

    }

    @Override
    public void setAnimationOnOpenActivity(Integer firstAnim, Integer secondAnim) {
        overridePendingTransition(firstAnim, secondAnim);
    }

    @Override
    public void setAnimationOnCloseActivity(Integer firstAnim, Integer secondAnim) {
        overridePendingTransition(firstAnim, secondAnim);
    }

    @Override
    public View setViewTreeObserverActivity() {
        return null;
    }

    @Override
    public void getViewTreeObserverActivity() {
    }

    @Override
    public Intent setResultIntent() {
        return null;
    }

    @Override
    public String getTagDataIntentFromActivity() {
        return null;
    }

    @Override
    public void getMapDataIntentFromActivity(MapDataParcelable parcleable) {
    }

    @Override
    public MapDataParcelable setMapDataIntentToNextActivity(MapDataParcelable parcleable) {
        return null;
    }

    @Override
    public void successPostGetData(Object data) {
        Timber.d(TAG+"successPostGetData - " + data.toString());
    }

    @Override
    public void errorPostGetData(Object data) {
        Timber.d(TAG+"errorPostGetData - " + data.toString());
    }

    @Override
    public boolean verifyDataNonNullOrZero(boolean isDataHasNullOrZero) {
        return false;
    }

    private final int durationLaser = 3000;

    private void startTimerLaser()
    {
        stoptimerTaskLaser();
        timerLaser = new Timer();
        timerTaskLaser = new TimerTask() {
            @Override
            public void run() {
                f.runOnUiThread(new Runnable() {
                    public void run() {
                        mGraphicOverlay.smoothAnimateCreateLaser(mGraphicOverlay.getTopLaser(), mGraphicOverlay.getBottomLaser(), durationLaser);
                        //mGraphicOverlay.changeColorAnimation(durationLaser);
                    }
                });
            }
        };
        //schedule the timer, to wake up every 1 second
        timerLaser.schedule(timerTaskLaser, 0, durationLaser); //
    }

    private void stoptimerTaskLaser() {
        //stop the timer, if it's not already null
        if (timerLaser != null) {
            timerLaser.cancel();
            timerLaser = null;
            Timber.d(TAG+"TimerLaser finish / interupt");
        }
    }

}
