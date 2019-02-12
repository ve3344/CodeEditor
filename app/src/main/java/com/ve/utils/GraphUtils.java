package com.ve.utils;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

public class GraphUtils {
    public static int alphaColor(int alpha,int color){
        return  Color.argb(alpha, Color.red(color),Color.green(color),Color.blue(color));
    }
    public static int alphaColor(float alpha,int color){
        return  Color.argb((int)(alpha* Color.alpha(color)), Color.red(color),Color.green(color),Color.blue(color));
    }
    public static void drawCenterText(Canvas canvas, char c, RectF rect, Paint paint) {
        Paint.FontMetrics fontMetrics = paint.getFontMetrics();
        float top = fontMetrics.top;
        float bottom = fontMetrics.bottom;
        int baseLineY = (int) (rect.centerY() - top / 2 - bottom / 2);
        canvas.drawText(new char[]{c},0,1, rect.centerX(), baseLineY, paint);
    }
    public static void drawCenterText(Canvas canvas, String text, RectF rect, Paint paint) {
        Paint.FontMetrics fontMetrics = paint.getFontMetrics();
        float top = fontMetrics.top;
        float bottom = fontMetrics.bottom;
        int baseLineY = (int) (rect.centerY() - top / 2 - bottom / 2);
        canvas.drawText(text,0,text.length(), rect.centerX(), baseLineY, paint);
    }

    public static int grandColor(int start, int end, float per) {
        int red = (int) (Color.red(start) + (Color.red(end) - Color.red(start)) * per);
        int blue = (int) (Color.blue(start) + (Color.blue(end) - Color.blue(start)) * per);
        int green = (int) (Color.green(start) + (Color.green(end) - Color.green(start)) * per);
        return Color.rgb(red, green, blue);
    }

}
