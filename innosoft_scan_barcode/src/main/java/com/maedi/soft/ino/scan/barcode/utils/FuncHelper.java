package com.maedi.soft.ino.scan.barcode.utils;

import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;

import pub.devrel.easypermissions.EasyPermissions;
import timber.log.Timber;

public class FuncHelper {

    public static int gcd(int p, int q) {
        if (q == 0) return p;
        else return gcd(q, p % q);
    }

    public static String ratio(int a, int b) {
        final int gcd = gcd(a,b);
        if(a > b) {
            return showRatio(a/gcd, b/gcd);
        } else {
            return showRatio(b/gcd, a/gcd);
        }
    }

    public static String showRatio(int a, int b) {
        String res = a +":"+ b;
        Timber.d("SHOW_RATIO - "+res);
        return res;
    }

    public static boolean hasAPI_LEVEL24_ANDROID_7_Above() {
        return Build.VERSION.SDK_INT >= 24; //API Level 7
    }

    public static void CameraPermission_API_LEVEL24_ANDROID_7_Above(FragmentActivity f){

        EasyPermissions.requestPermissions(f, "CAMERA",
                DataStatic.PERMISSION_REQUEST_ACCESS_CAMERA_ABOVE6, DataStatic.GALLERY_PERMISSIONS);
    }

    public static void CameraPermission(FragmentActivity f) {
        if (ContextCompat.checkSelfPermission(f, DataStatic.GALLERY_PERMISSIONS[0]) != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(f, DataStatic.GALLERY_PERMISSIONS[0])) {

                ActivityCompat.requestPermissions(f,
                        DataStatic.GALLERY_PERMISSIONS,
                        DataStatic.PERMISSION_REQUEST_ACCESS_CAMERA_ABOVE6);

            } else {

                ActivityCompat.requestPermissions(f,
                        DataStatic.GALLERY_PERMISSIONS,
                        DataStatic.PERMISSION_REQUEST_ACCESS_CAMERA_ABOVE6);
            }
        }
    }
}
