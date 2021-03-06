/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */

package xjunz.tool.werecord.ui.transition;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.transition.Transition;
import android.transition.TransitionValues;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import org.jetbrains.annotations.NotNull;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class Elevate extends Transition {

    private static final String PROP_Z = "xjunz:elevate:z";

    public Elevate() {
    }

    public Elevate(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void captureStartValues(@NotNull TransitionValues transitionValues) {
        transitionValues.values.put(PROP_Z, transitionValues.view.getElevation());
    }

    @Override
    public void captureEndValues(@NotNull TransitionValues transitionValues) {
        transitionValues.values.put(PROP_Z, transitionValues.view.getElevation());
    }

    @Override
    public Animator createAnimator(ViewGroup sceneRoot, TransitionValues startValues, TransitionValues endValues) {
        if (startValues == null || endValues == null) {
            return null;
        }
        float startZ = (float) startValues.values.get(PROP_Z);
        float endZ = (float) endValues.values.get(PROP_Z);
        return ObjectAnimator.ofFloat(endValues.view, View.Z, startZ, endZ);
    }
}
