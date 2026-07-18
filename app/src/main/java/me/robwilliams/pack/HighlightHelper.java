package me.robwilliams.pack;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.view.View;

public class HighlightHelper {

    private static final int HOLD_MS = 400;
    private static final int FADE_OUT_MS = 800;

    public static void highlightView(final View view) {
        if (view == null) return;
        final int highlightColor = view.getContext().getResources().getColor(R.color.highlight);
        view.setBackgroundColor(highlightColor);
        view.postDelayed(new Runnable() {
            @Override
            public void run() {
                ValueAnimator fadeOut = ValueAnimator.ofObject(
                        new ArgbEvaluator(), highlightColor, Color.TRANSPARENT);
                fadeOut.setDuration(FADE_OUT_MS);
                fadeOut.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        view.setBackgroundColor((int) animation.getAnimatedValue());
                    }
                });
                fadeOut.start();
            }
        }, HOLD_MS);
    }
}
