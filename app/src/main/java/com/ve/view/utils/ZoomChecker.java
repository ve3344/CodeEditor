package com.ve.view.utils;

import android.view.MotionEvent;

public class ZoomChecker {

    private ZoomListener listener;
    private double distance;
    private double scaleFrom = 1, scaleCurrent = 1;


    private static double calculateDistance(MotionEvent event) {
        float dx = event.getX(0) - event.getX(1);
        float dy = event.getY(0) - event.getY(1);
        return Math.sqrt(dx * dx + dy * dy);
    }

    public boolean checkZoom(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_MOVE || event.getPointerCount() != 2) {
            reset();
            return false;
        }

        double newDistance = calculateDistance(event);
        if (!isZooming()) {
            distance = newDistance;
            //listener.onZoomStart();
            scaleCurrent = scaleFrom;

        } else {
            if (listener != null) {
                double zoom = newDistance / distance;
                scaleCurrent = zoom * scaleFrom;
                listener.onZoom(zoom,scaleCurrent);
            }
        }
        return true;

    }

    public void reset() {
        distance = -1;
        scaleFrom = scaleCurrent;
        //listener.onZoomEnd();

    }

    public void setListener(ZoomListener listener) {
        this.listener = listener;
    }

    public boolean isZooming() {
        return distance > 0;
    }

    public interface ZoomListener {
        void onZoom(double scale,double scaleAll);
    }
}
