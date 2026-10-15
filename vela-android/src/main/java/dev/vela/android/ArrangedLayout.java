package dev.vela.android;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;

/** Native linear measurement with explicit distributed arrangement, without synthetic child views. */
final class ArrangedLayout extends LinearLayout {
    String arrangement = "start";

    ArrangedLayout(Context context, int orientation) { super(context); setOrientation(orientation); }

    @Override protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (!arrangement.startsWith("space-")) return;
        boolean horizontal = getOrientation() == HORIZONTAL;
        int count = 0, used = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) continue;
            LayoutParams params = (LayoutParams) child.getLayoutParams();
            used += horizontal ? child.getMeasuredWidth() + params.leftMargin + params.rightMargin
                    : child.getMeasuredHeight() + params.topMargin + params.bottomMargin;
            count++;
        }
        if (count == 0) return;
        int available = horizontal ? getWidth() - getPaddingLeft() - getPaddingRight()
                : getHeight() - getPaddingTop() - getPaddingBottom();
        float extra = Math.max(0, available - used);
        float gap = switch (arrangement) {
            case "space-between" -> count > 1 ? extra / (count - 1) : 0;
            case "space-around" -> extra / count;
            case "space-evenly" -> extra / (count + 1);
            default -> 0;
        };
        float cursor = horizontal ? getPaddingLeft() : getPaddingTop();
        if (arrangement.equals("space-around")) cursor += gap / 2;
        if (arrangement.equals("space-evenly")) cursor += gap;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) continue;
            LayoutParams params = (LayoutParams) child.getLayoutParams();
            cursor += horizontal ? params.leftMargin : params.topMargin;
            if (horizontal) {
                int x = Math.round(cursor);
                if (getLayoutDirection() == LAYOUT_DIRECTION_RTL) x = getWidth() - x - child.getMeasuredWidth();
                child.layout(x, child.getTop(), x + child.getMeasuredWidth(), child.getBottom());
            } else {
                int y = Math.round(cursor);
                child.layout(child.getLeft(), y, child.getRight(), y + child.getMeasuredHeight());
            }
            cursor += (horizontal ? child.getMeasuredWidth() + params.rightMargin
                    : child.getMeasuredHeight() + params.bottomMargin) + gap;
        }
    }
}
