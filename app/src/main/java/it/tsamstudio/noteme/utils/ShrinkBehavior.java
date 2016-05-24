package it.tsamstudio.noteme.utils;

import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;

import com.michaldrabik.tapbarmenulib.TapBarMenu;

import java.util.List;

/**
 * Created by damiano on 23/05/16.
 */
public class ShrinkBehavior extends CoordinatorLayout.Behavior<TapBarMenu> {

    public ShrinkBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, TapBarMenu child, View dependency) {
        return dependency instanceof Snackbar.SnackbarLayout;
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, TapBarMenu child, View dependency) {
        if (dependency instanceof Snackbar.SnackbarLayout) {
            float translationY = dependency.getTranslationY();

            child.setScaleX(translationY / dependency.getWidth());
            child.setScaleY(translationY / dependency.getHeight());

            if (Math.floor(dependency.getTranslationY()) == 0) {
                child.setScaleX(1);
                child.setScaleY(1);
                child.setVisibility(View.GONE);
            }
        }
        return false;
    }

    private float getFabTranslationYForSnackbar(CoordinatorLayout parent,
                                                FloatingActionButton fab) {
        float minOffset = 0;
        final List<View> dependencies = parent.getDependencies(fab);
        for (int i = 0, z = dependencies.size(); i < z; i++) {
            final View view = dependencies.get(i);
            if (view instanceof Snackbar.SnackbarLayout && parent.doViewsOverlap(fab, view)) {
                minOffset = Math.min(minOffset,
                        ViewCompat.getTranslationY(view) - view.getHeight());
            }
        }

        return minOffset;
    }
}
