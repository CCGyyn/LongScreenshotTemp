package com.example.longscreentemp.utils;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

/**
 * 作者：caigaopeng on 2020/8/11 23:51
 */
public abstract  class ScrollController {

    private boolean DEBUG_LONG_SCREENSHOT = true;
    private final String TAG = this.getClass().getSimpleName();

    //the target to scroll and capture it's long view
    protected ViewGroup view;
    //scroll distance of SCROLL_DISTANCE by time of SCROLL_DURATION at once
    protected static final int SCROLL_DISTANCE = 500;
    protected static final int SCROLL_DURATION = 50;
    protected int onceScrollDistance = SCROLL_DISTANCE;
    protected int maxScrollDistance = 0;
    protected int leftScrollDistance = 0;
    //remember scrollBar state before scrolling and resume scrollBar when scrollComplete
    protected boolean scrollBarEnabled = true;
    protected int scrolltimes = 0;

    protected Handler scrollHandler = null;

    protected int startScrollY = 0;
    protected CheckScrollDistanceRunnable checkScrollDistance = new CheckScrollDistanceRunnable();
    protected Runnable scrollTimeoutRunnable = new Runnable() {
        public void run() {
            setScrollComplete();
            sendScrollMessage(0);
        }
    };

    //use to compute last scroll distance
    protected int lastItemPreBottom = 0;
    protected View lastItemView = null;

    public ScrollController(ViewGroup view) {
        this.view = view;
        scrollBarEnabled = view.isVerticalScrollBarEnabled();
        //view.setVerticalScrollBarEnabled(false);
        //setScrollDistanceListener();
    }

    public void setOnceScrollDistance(int distance) {
        if (DEBUG_LONG_SCREENSHOT) Log.d(TAG, "setOnceScrollDistance:onceScrollDistance=" + distance);
        onceScrollDistance = distance;
    }

    protected void setScrollHandler(Handler scrollHandler) {
        this.scrollHandler = scrollHandler;
    }

    protected void startScroll() {//（前面有过截图的操作）点击开始长截图后调用到这里，在SystemUI里面已经截图了一次
        //view.setVerticalScrollBarEnabled(false);
        setScrollDistanceListener();
        maxScrollDistance = countMaxScrollDistance();
        if (DEBUG_LONG_SCREENSHOT) Log.d(TAG, "startScroll:maxScrollDistance = " + maxScrollDistance);
        continueScroll();
    }

    protected void continueScroll() {
        //view.removeCallbacks(scrollTimeoutRunnable);
        if (this.canScrollDown()) {
            scrolltimes += 1;
            startScrollY = computeCurrentScrollY();
            lastItemView = view.getChildAt(view.getChildCount() - 1);
            if (lastItemView != null) lastItemPreBottom = lastItemView.getBottom();
            if (DEBUG_LONG_SCREENSHOT) Log.d(TAG, "ScrollController continueScroll: lastItemPreBottom = " + lastItemPreBottom+"  scrollBy==>onceScrollDistance:"+onceScrollDistance
                    +"  view.getChildCount():"+view.getChildCount());
            view.scrollBy(0, onceScrollDistance);
            //view.scrollTo(0, view.getScrollY() + onceScrollDistance);
            //view.postDelayed(scrollTimeoutRunnable,300);
        } else {
            setScrollComplete();
        }

    }

    protected void sendScrollMessage(int scrollDistance) {
        if (DEBUG_LONG_SCREENSHOT) Log.d(TAG, "sendScrollMessage: scrollDistance=" + scrollDistance, new Throwable());
        if (scrollDistance > 0) {
            Message shotMessage = scrollHandler.obtainMessage(CAPTURE_SCREEN, scrollDistance);
            scrollHandler.sendMessage(shotMessage);
        }
        if (scrollDistance < onceScrollDistance) {
            if (DEBUG_LONG_SCREENSHOT) Log.d(TAG, "should have scroll to bottom");
            Message completeMessage = scrollHandler.obtainMessage(SCROLL_COMPLETE);
            scrollHandler.sendMessage(completeMessage);
        }
    }

    protected int computeCurrentScrollY() {
        return view.getScrollY();
    }

    protected int computeScrollDistance() {
        return computeCurrentScrollY() - startScrollY;
    }

    protected void setScrollComplete() {
        //view.removeCallbacks(scrollTimeoutRunnable);
        view.setVerticalScrollBarEnabled(scrollBarEnabled);
        clearScrollDistanceListener();
        resetOverlayViews();
        //sendScrollMessage(-1);
    }

    //set scroll listener which will compute scrollDistance after scroll once
    protected void setScrollDistanceListener() {
        view.setScrollDistanceRunnable(checkScrollDistance);
    }

    protected void clearScrollDistanceListener() {
        view.setScrollDistanceRunnable(null);
    }

    protected void resetOverlayViews() {
        if (shouldCheckOverlay) {
            if (overlayViews != null && overlayViews.size() > 0) {
                for (int i = 0; i < overlayViews.size(); i++) {
                    View view = overlayViews.get(i);
                    view.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    //whether scrollableView canScrollDown any more
    abstract boolean canScrollDown();

    abstract int countMaxScrollDistance();

    protected int fitScrollDistance(int scrollDistance) {
        if (canScrollDown() /*|| scrollDistance > onceScrollDistance*/) {
            return onceScrollDistance;
        } else {
            if (lastItemView != null) {
                if (DEBUG_LONG_SCREENSHOT) Log.d(TAG, "fitScrollDistance continueScroll: lastItemPreBottom = " + lastItemView.getBottom());
                return lastItemPreBottom - lastItemView.getBottom();
            }
        }

        return scrollDistance;
    }

    protected class CheckScrollDistanceRunnable implements Runnable {

        public CheckScrollDistanceRunnable() {
        }

        @Override
        public void run() {
            int scrollDistance = computeScrollDistance();
            if (lastItemView != null) {
                if (DEBUG_LONG_SCREENSHOT) Log.d(TAG, "run: lastItemPreBottom = " + lastItemPreBottom+"  lastItemView.getBottom():" +lastItemView.getBottom());
                scrollDistance = lastItemPreBottom - lastItemView.getBottom();
            }
            if ((scrollDistance != onceScrollDistance && scrollType != RECYCLEVIEW) || scrollType == SELF_WEBVIEW){
                scrollDistance = fitScrollDistance(scrollDistance);
            }
            //if (scrollType != RECYCLEVIEW) scrollDistance = fitScrollDistance(scrollDistance);
            if (scrollDistance > onceScrollDistance) scrollDistance = onceScrollDistance;
            //if (scrollType == SELF_WEBVIEW && scrollDistance > leftScrollDistance) scrollDistance = leftScrollDistance;
            //if (scrollType == SCROLLVIEW) scrollDistance = fitScrollDistance(scrollDistance);
            Log.d(TAG, "scrollDistance = " + scrollDistance);
            sendScrollMessage(scrollDistance);
        }
    }
}
