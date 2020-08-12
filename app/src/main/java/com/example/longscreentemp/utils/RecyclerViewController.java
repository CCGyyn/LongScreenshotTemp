package com.example.longscreentemp.utils;

import android.util.Log;
import android.view.ViewGroup;

import java.lang.reflect.Method;

/**
 * 作者：caigaopeng on 2020/8/12 23:36
 */
class RecyclerViewController extends ScrollController {

    private boolean DEBUG_LONG_SCREENSHOT = true;
    private final String TAG = this.getClass().getSimpleName();

    private Method m1, m2, m3;
    private int leftMaxScrollDistance = 0;
    private int preLeftMaxScrollDistance;
    private boolean selfRecyclerView = false;

    public RecyclerViewController(ViewGroup view, Class clazz, boolean selfRecyclerView) {
        super(view);
        this.selfRecyclerView = selfRecyclerView;
        createMethods(clazz);
    }

    private void createMethods(Class clazz) {
        try {
            m1 = clazz.getDeclaredMethod("computeVerticalScrollRange", null);
            m2 = clazz.getDeclaredMethod("computeVerticalScrollExtent", null);
            m3 = clazz.getDeclaredMethod("computeVerticalScrollOffset", null);
            m1.setAccessible(true);
            m2.setAccessible(true);
            m3.setAccessible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void setScrollDistanceListener() {
        if (!selfRecyclerView) super.setScrollDistanceListener();
    }

    protected void clearScrollDistanceListener() {
        if (!selfRecyclerView) super.clearScrollDistanceListener();
    }

    @Override
    protected int computeCurrentScrollY() {
        int result = 0;
        try {
            result = (Integer)m3.invoke(view);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (DEBUG_LONG_SCREENSHOT) Log.d(TAG, "computeStartScrollY:result = " + result + " selfRecyclerView?" + selfRecyclerView);
        return result;
    }

    @Override
    protected void continueScroll() {
        Log.e(TAG,"RecyclerViewController continueScroll selfRecyclerView:"+selfRecyclerView);
        if (!selfRecyclerView) {
            super.continueScroll();
            return;
        }

        if (!canScrollDown()) {
            setScrollComplete();
            return;
        }
        scrolltimes += 1;
        startScrollY = computeCurrentScrollY();
        lastItemView = view.getChildAt(view.getChildCount() - 1);
        if (lastItemView != null) lastItemPreBottom = lastItemView.getBottom();
        if (DEBUG_LONG_SCREENSHOT) Log.d(TAG, "RecyclerViewController continueScroll: lastItemPreBottom = " + lastItemPreBottom);
        preLeftMaxScrollDistance = leftMaxScrollDistance;
        view.scrollBy(0, onceScrollDistance);
        view.postDelayed(checkScrollDistance, 200);
    }

    @Override
    protected boolean canScrollDown() {
        boolean result = true;
        try {
            int verticalScrollRange = (Integer)m1.invoke(view);
            int verticalScrollExtent = (Integer)m2.invoke(view);
            int maxDistance = verticalScrollRange - verticalScrollExtent;
            int currentScrollDistance = (Integer)m3.invoke(view);
            leftMaxScrollDistance = maxDistance - currentScrollDistance;
            Log.d(TAG, "maxDistance = " + maxDistance + ", leftMaxScrollDistance = " + leftMaxScrollDistance + " scrollY = " + view.getScrollY());
            if (currentScrollDistance >= maxDistance || preLeftMaxScrollDistance == leftMaxScrollDistance) {
                result = false;
                if (DEBUG_LONG_SCREENSHOT) Log.d(TAG, "RecyclerView has reached bottom");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.d(TAG, "canScrollDown:result = " + result);
        return result;
    }

    protected int countMaxScrollDistance() {//剩余可滚动的距离
        try {
            int verticalScrollRange = (Integer)m1.invoke(view);
            int verticalScrollExtent = (Integer)m2.invoke(view);
            int currentScrollDistance = (Integer)m3.invoke(view);
            maxScrollDistance = verticalScrollRange - verticalScrollExtent;
            leftMaxScrollDistance = maxScrollDistance - currentScrollDistance;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return leftMaxScrollDistance;
    }

    protected int fitScrollDistance(int scrollDistance) {
        Log.e(TAG,"RecyclerViewController fitScrollDistance");
        int preLeftMaxScrollDistance = leftMaxScrollDistance;
        //if (!canScrollDown() /*&& scrolltimes == 1 && scrollDistance < leftMaxScrollDistance*/) return preLeftMaxScrollDistance;
        return super.fitScrollDistance(scrollDistance);
    }
}

