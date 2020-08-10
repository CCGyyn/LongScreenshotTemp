package com.example.longscreentemp;

import android.app.Activity;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.AbsListView;
import android.widget.ScrollView;

import com.example.longscreentemp.utils.TestUtil;

import java.lang.reflect.Method;

import androidx.annotation.Nullable;
import androidx.core.view.ScrollingView;


public class Test extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new TestUtil().isSupportLongScreenshot();
    }

    private boolean supportLongScreenShot;

    /**
     * 判断是否能进行长截图
     */
    private void updateSupportState() {
        supportLongScreenShot = !isInMultiWindowMode() && isSupportLongScreenshot();
    }

    private ViewGroup scrollableView = null;
    private Rect scrollViewRact = new Rect();
    private int cropPosition;

    private boolean isSupportLongScreenshot() {
        boolean result = true;
        synchronized (this.getClass()) {
            scrollableView = getScrollableView((ViewGroup) getWindow().getDecorView());
            if (scrollableView != null) {
                scrollableView.getGlobalVisibleRect(scrollViewRact);
                cropPosition = scrollViewRact.bottom;
                if (scrollType != ScrollType.RECYCLEVIEW) {
                    cropPosition -= scrollableView.getPaddingBottom();
                }

                int width = getResources().getDisplayMetrics().widthPixels;
                int height = getResources().getDisplayMetrics().heightPixels;
                if (scrollViewRact.width() < width / 2
                        || scrollViewRact.height() < height / 2) {
                    result = false;
                }
            } else {
                result = false;
            }
        }
        return result;
    }

    /**
     * 决定当前界面是否能够支持长截图
     * @param viewGroup
     * @return
     */
    private ViewGroup getScrollableView(ViewGroup viewGroup){
        int childCount = viewGroup.getChildCount();
        for(int i = childCount - 1;i >= 0;i--) {
            View childView = viewGroup.getChildAt(i);
            if (childView.getVisibility() != View.VISIBLE
                    || !childView.getLocalVisibleRect(new Rect())
                    || !childView.isAttachedToWindow()) {
                continue;
            }
            if(childView instanceof ViewGroup) {
                if (isScrollableView((ViewGroup) childView)) {
                    android.util.Log.e("cgp", "FIND childview=" + childView.getClass().getName());
                    return (ViewGroup) childView;
                }
                android.util.Log.e("cgp", "FIND next for childview=" + childView.getClass().getName());
                ViewGroup view = getScrollableView((ViewGroup) childView);
                if (view != null) {
                    return view;
                }
            }
        }

        return null;
    }


    /**
     * judge whether a decor contains a scrollable view
     * @param viewGroup
     * @return
     */
    private boolean isScrollableView(ViewGroup viewGroup) {
        boolean result = false;
        if (viewGroup instanceof ScrollView) {
            android.util.Log.d("cgp", "viewGroup is ScrollView");
            ScrollView scrollView = (ScrollView) viewGroup;
            View child = scrollView.getChildAt(0);
            //whether scrollView scroll to bottom
            if (child.getMeasuredHeight() <= scrollView.getScrollY() + scrollView.getHeight()) {
                result = false;
                android.util.Log.d("cgp", "ScrollView has reached bottom");
            } else {
                result = true;
                scrollType = ScrollType.SCROLLVIEW;
            }
            android.util.Log.d("cgp", "child.getMeasuredHeight() = " + child.getMeasuredHeight());
            return result;
        } else if (viewGroup instanceof AbsListView) {
            android.util.Log.d("cgp", "viewGroup is ListView");
            AbsListView listView = (AbsListView) viewGroup;
            int count = listView.getCount();
            int childCount = listView.getChildCount();
            android.util.Log.d("cgp", "count = " + count + " getChildCount = " + listView.getChildCount());
            android.util.Log.d("cgp", "LastVisiblePosition = " + listView.getLastVisiblePosition());
            //count > childCount
            //测下listView.getMeasuredHeight()
            if (count > 0) {
                result = true;
            }
            //whether listview scroll to bottom
            //是否已到最后一个
            if (listView.getLastVisiblePosition() >= count - 1 && count > 0) {
                int lastItemBottom = listView.getChildAt(listView.getChildCount() - 1).getBottom();
                int listViewheight = listView.getHeight();
                android.util.Log.d("cgp", "lastItemBottom = " + lastItemBottom);
                android.util.Log.d("cgp", "listView.getHeight() = " + listViewheight);
                if (lastItemBottom <= listViewheight) {
                    result = false;
                    android.util.Log.d("cgp", "ListView has reached bottom");
                }
            }
            if (result) {
                scrollType = ScrollType.LISTVIEW;
            }
            android.util.Log.d("cgp", "count = " + count + " LastVisiblePosition = " + listView.getLastVisiblePosition());
            return result;
        } else if (viewGroup instanceof WebView) {
            android.util.Log.d("cgp", "viewGroup is WebView");
            WebView webView = (WebView) viewGroup;
            int verticalScrollRange = 0;
            int verticalScrollExtent = 1;
            int offset = 2;
            Class clazz = viewGroup.getClass();
            for (;;) {
                String classNamepath = clazz.getSimpleName().toLowerCase();
                if (classNamepath.equals("webview")) {
                    break;
                }
                clazz = clazz.getSuperclass();
            }

            try {
                Method m1 = clazz.getDeclaredMethod("computeVerticalScrollRange", null);
                Method m2 = clazz.getDeclaredMethod("computeVerticalScrollExtent", null);
                Method m3 = clazz.getDeclaredMethod("computeVerticalScrollOffset", null);

                m1.setAccessible(true);
                m2.setAccessible(true);
                m3.setAccessible(true);
                verticalScrollRange = (Integer)m1.invoke(viewGroup);
                verticalScrollExtent = (Integer)m2.invoke(viewGroup);
                offset = (Integer)m3.invoke(viewGroup);
            } catch (Exception e) {
                e.printStackTrace();
            }

            android.util.Log.d("cgp", "verticalScrollRange=" + verticalScrollRange + " verticalScrollExtent=" + verticalScrollExtent + " offset=" + offset);
            if(webView.getContentHeight()*webView.getScale()-(webView.getHeight()+webView.getScrollY())==0){
                android.util.Log.d("cgp", "WebView ContentHeight=" + webView.getContentHeight()*webView.getScale() + " Height=" + webView.getHeight());
                //webview has reached bottom
                result = false;
                android.util.Log.d("cgp", "WebView has reached bottom");
            } else {
                result = true;
                scrollType = ScrollType.WEBVIEW;
            }

            if(verticalScrollRange-verticalScrollExtent-offset <= 0){
                //webview has reached bottom
                result = false;
                android.util.Log.d("cgp", "WebView has reached bottom.");
            } else {
                result = true;
                scrollType = ScrollType.WEBVIEW;
            }

            return result;
        } else {
            if (viewGroup instanceof ScrollingView) {
                ScrollingView scrollingView = (ScrollingView) viewGroup;
            }
        }

        android.util.Log.d("cgp", "isScrollableView:result = " + result);
        return false;
    }

    private ScrollType scrollType;

    enum ScrollType {
        SCROLLVIEW,
        LISTVIEW,
        WEBVIEW,
        RECYCLEVIEW
    }

}


