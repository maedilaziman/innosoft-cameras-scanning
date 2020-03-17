package com.maedi.soft.ino.scan.barcode.camera;

import android.content.Context;
import android.graphics.Point;
import android.view.Display;
import android.view.WindowManager;

public class ScreenSize {

    private WindowManager wm;
    private Display display;
    private Point size;
    private int width;
    private int height;
    private static ScreenSize screenSize;

    public static ScreenSize instance(Context context){
        if(null == screenSize)
        {
            screenSize = new ScreenSize(context);
        }
        return screenSize;
    }

    private ScreenSize(Context context)
    {
        size = new Point();
        wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        display = wm.getDefaultDisplay();
    }

    public int getWidth(){
        if(android.os.Build.VERSION.SDK_INT >= 13)
        {
            display.getRealSize(size);
            width = size.x;
        }else {
            width = display.getWidth();
        }
        return width;
    }

    public int getHeight(){
        if(android.os.Build.VERSION.SDK_INT >= 13)
        {
            display.getRealSize(size);
            height = size.y;
        }else {
            height = display.getHeight();
        }
        return height;
    }
}
