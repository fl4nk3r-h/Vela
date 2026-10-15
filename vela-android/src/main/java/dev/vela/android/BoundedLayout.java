package dev.vela.android;

import android.content.Context;
import android.widget.FrameLayout;

/** Gives all widgets the same max-size and aspect-ratio semantics. Pixel values are resolved by the backend. */
final class BoundedLayout extends FrameLayout {
    int maxWidth = Integer.MAX_VALUE, maxHeight = Integer.MAX_VALUE;
    double aspectRatio;

    BoundedLayout(Context context) { super(context); }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        widthSpec = limit(widthSpec, maxWidth);
        heightSpec = limit(heightSpec, maxHeight);
        if (aspectRatio > 0) {
            int widthMode = MeasureSpec.getMode(widthSpec), heightMode = MeasureSpec.getMode(heightSpec);
            if (widthMode == MeasureSpec.EXACTLY && heightMode != MeasureSpec.EXACTLY) {
                int height = (int) Math.round(MeasureSpec.getSize(widthSpec) / aspectRatio);
                if (heightMode != MeasureSpec.UNSPECIFIED) height = Math.min(height, MeasureSpec.getSize(heightSpec));
                heightSpec = MeasureSpec.makeMeasureSpec(Math.min(maxHeight, height), MeasureSpec.EXACTLY);
            } else if (heightMode == MeasureSpec.EXACTLY && widthMode != MeasureSpec.EXACTLY) {
                int width = (int) Math.round(MeasureSpec.getSize(heightSpec) * aspectRatio);
                if (widthMode != MeasureSpec.UNSPECIFIED) width = Math.min(width, MeasureSpec.getSize(widthSpec));
                widthSpec = MeasureSpec.makeMeasureSpec(Math.min(maxWidth, width), MeasureSpec.EXACTLY);
            }
        }
        super.onMeasure(widthSpec, heightSpec);
    }

    private int limit(int spec, int maximum) {
        int mode = MeasureSpec.getMode(spec), size = MeasureSpec.getSize(spec);
        if (maximum == Integer.MAX_VALUE) return spec;
        if (mode == MeasureSpec.UNSPECIFIED) return MeasureSpec.makeMeasureSpec(maximum, MeasureSpec.AT_MOST);
        return MeasureSpec.makeMeasureSpec(Math.min(size, maximum), mode);
    }
}
