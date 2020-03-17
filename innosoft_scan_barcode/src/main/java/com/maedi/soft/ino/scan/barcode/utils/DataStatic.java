package com.maedi.soft.ino.scan.barcode.utils;

import android.Manifest;

public class DataStatic {

    public static final int PERMISSION_REQUEST_ACCESS_CAMERA_ABOVE6 = 17;

    public static final String[] GALLERY_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
}
