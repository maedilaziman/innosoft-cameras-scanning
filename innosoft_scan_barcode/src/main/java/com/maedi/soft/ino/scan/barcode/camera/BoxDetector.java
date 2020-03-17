package com.maedi.soft.ino.scan.barcode.camera;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.util.SparseArray;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;

import java.io.ByteArrayOutputStream;

import timber.log.Timber;

public class BoxDetector extends Detector {

    private Detector mDelegate;
    private int mBoxWidth, mBoxHeight;

    public BoxDetector(Detector delegate, int boxWidth, int boxHeight) {
        mDelegate = delegate;
        mBoxWidth = boxWidth;
        mBoxHeight = boxHeight;
    }

    @Override
    public SparseArray detect(Frame frame) {
        int width = frame.getMetadata().getWidth();
        int height = frame.getMetadata().getHeight();

        Timber.d("START_BOX_SCAN - "+GraphicOverlay.startXPosScan +" | "+ GraphicOverlay.startYPosScan +" | "+ GraphicOverlay.widthPosScan +" | "+ GraphicOverlay.heightPosScan);
        YuvImage yuvImage = new YuvImage(frame.getGrayscaleImageData().array(), ImageFormat.NV21, width, height, null);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(GraphicOverlay.startXPosScan, GraphicOverlay.startYPosScan, GraphicOverlay.widthPosScan, GraphicOverlay.heightPosScan), 100, byteArrayOutputStream);
        byte[] jpegArray = byteArrayOutputStream.toByteArray();
        Bitmap bitmap = BitmapFactory.decodeByteArray(jpegArray, 0, jpegArray.length);

        Frame croppedFrame =
                new Frame.Builder()
                        .setBitmap(bitmap)
                        .setRotation(frame.getMetadata().getRotation())
                        .build();

        return mDelegate.detect(croppedFrame);
    }

    @Override
    public boolean isOperational() {
        return mDelegate.isOperational();
    }

    @Override
    public boolean setFocus(int id) {
        return mDelegate.setFocus(id);
    }
}
