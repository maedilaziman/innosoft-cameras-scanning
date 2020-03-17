/*
 * Copyright (c) 8/27/2019. Maedi Laziman, Email: maedilaziman@gmail.com.
 */

package com.maedi.soft.ino.scan.barcode.camera;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;

public class PixelCalc {

    public static int DpToPixel(float dp, Context context)
    {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float px = dp * (metrics.densityDpi / 160f);
        return (int)px;
    }

    public static int PixelToDP(Context context, int pixel)
    {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) ((pixel * scale) + 0.5f);
    }
}
