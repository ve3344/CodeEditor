package com.ve.view.utils;

import android.util.Log;

public class EditorException {
    private static final String TAG = EditorException.class.getSimpleName();
    private static final boolean DEBUG = true;


    static public void fail(final String details) {
        if (DEBUG) {
            Log.e(TAG, "fail: " + details);

        }
    }

    static public void logIf(boolean condition, final String details) {

        if (condition) {
            fail(details);
        }
    }
}
