package com.ve.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.ViewGroup;

public class AnimUtils {
    private static final String TAG = AnimUtils.class.getSimpleName();
    public static void fadeOut(View view, boolean gone) {
        if (view != null) {
            view.animate().alpha(0).setDuration(400).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    view.setVisibility(gone ? View.GONE : View.INVISIBLE);
                }
            }).start();
        }
    }

    public static void fadeIn(View view) {
        if (view != null) {
            view.setVisibility(View.VISIBLE);
            view.animate().alpha(0).setDuration(400).start();
        }
    }
    public static void scaleUpOut(View view, boolean gone) {
        if (view != null) {

            ValueAnimator valueAnimator=ValueAnimator.ofInt(view.getHeight(),0);
            valueAnimator.addUpdateListener(animation -> {
                Integer value= (Integer) animation.getAnimatedValue();
                ViewGroup.LayoutParams mparams=view.getLayoutParams();
                mparams.height= value;
                view.setLayoutParams(mparams);

            });
            valueAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    view.setVisibility(gone ? View.GONE : View.INVISIBLE);
                }
            });
            valueAnimator.setDuration(600);
            valueAnimator.start();
        }
    }

    public static void scaleUpIn(View view) {
        if (view != null) {
            view.setVisibility(View.VISIBLE);
            ValueAnimator valueAnimator=ValueAnimator.ofInt(0,view.getMeasuredHeight());
            valueAnimator.addUpdateListener(animation -> {
                Integer value= (Integer) animation.getAnimatedValue();
                ViewGroup.LayoutParams mparams=view.getLayoutParams();
                mparams.height= value;
                view.setLayoutParams(mparams);

            });
            valueAnimator.setDuration(600);
            valueAnimator.start();
        }
    }

}
