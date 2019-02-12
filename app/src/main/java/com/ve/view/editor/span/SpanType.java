package com.ve.view.editor.span;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.support.annotation.ColorInt;
import android.util.Log;


public class SpanType {
    public static final Typeface TF_DEFAULT = Typeface.DEFAULT;
    public static final Typeface TF_BOLD = Typeface.DEFAULT_BOLD;
    public static final Typeface TF_ITALIC = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC);
    public static final Typeface TF_BOLD_ITALIC = Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC);
    @ColorInt
    private int color;
    private Typeface typeface;

    public SpanType(int color) {
        this.color = color;
        this.typeface = TF_DEFAULT;
    }

    public SpanType(int color, Typeface typeface) {
        this.color = color;
        this.typeface = typeface;
    }

    public void onSpan(Paint paint) {
        paint.setColor(color);
        paint.setTypeface(typeface);
    }

    @Override
    public String toString() {
        return String.format("#%h",color);
    }
}