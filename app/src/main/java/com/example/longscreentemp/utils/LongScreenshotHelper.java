package com.example.longscreentemp.utils;

import android.app.Activity;
import android.graphics.Rect;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.AbsListView;
import android.widget.ScrollView;

//import androidx.core.view.ScrollingView;


import java.lang.reflect.Method;


/**
 * {@hide}
 */
public class LongScreenshotHelper {

    //如果都没有istop为true的话 就看top的包名

    private boolean DEBUG_LONG_SCREENSHOT = true;

    private final String TAG = this.getClass().getSimpleName();
    private ViewGroup scrollableView = null;
    private Rect scrollViewRact = new Rect();
    private int cropPosition;
    private Class recyclerViewClass;
    private boolean isSelfRecyclerView = true;

    public ViewGroup getScrollableView() {
        return scrollableView;
    }

    public boolean isSupportLongScreenshot(Activity activity) {
        boolean result = true;
        synchronized (this.getClass()) {
            scrollableView = getScrollableView((ViewGroup) activity.getWindow().getDecorView());
            if (scrollableView != null) {
                scrollableView.getGlobalVisibleRect(scrollViewRact);
                cropPosition = scrollViewRact.bottom;
                if (scrollType != ScrollType.RECYCLEVIEW) {
                    cropPosition -= scrollableView.getPaddingBottom();
                }

                int width = activity.getResources().getDisplayMetrics().widthPixels;
                int height = activity.getResources().getDisplayMetrics().heightPixels;
                if (scrollViewRact.width() < width / 2
                        || scrollViewRact.height() < height / 2) {
                    result = false;
                }
            } else {
                result = false;
            }
        }
        Log.d(TAG, "isSupportLongScreenshot:result = " + result);
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
                    Log.e(TAG, "FIND childview=" + childView.getClass().getName());
                    return (ViewGroup) childView;
                }
                Log.e(TAG, "FIND next for childview=" + childView.getClass().getName());
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
            Log.d(TAG, "viewGroup is ScrollView");
            ScrollView scrollView = (ScrollView) viewGroup;
            View child = scrollView.getChildAt(0);
            //whether scrollView scroll to bottom
            if (child.getMeasuredHeight() <= scrollView.getScrollY() + scrollView.getHeight()) {
                result = false;
                Log.d(TAG, "ScrollView has reached bottom");
            } else {
                result = true;
                scrollType = ScrollType.SCROLLVIEW;
            }
            Log.d(TAG, "child.getMeasuredHeight() = " + child.getMeasuredHeight());
            return result;
        } else if (viewGroup instanceof AbsListView) {
            Log.d(TAG, "viewGroup is ListView");
            AbsListView listView = (AbsListView) viewGroup;
            int count = listView.getCount();
            int childCount = listView.getChildCount();
            Log.d(TAG, "count = " + count + " getChildCount = " + listView.getChildCount());
            Log.d(TAG, "LastVisiblePosition = " + listView.getLastVisiblePosition());
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
                Log.d(TAG, "lastItemBottom = " + lastItemBottom);
                Log.d(TAG, "listView.getHeight() = " + listViewheight);
                if (lastItemBottom <= listViewheight) {
                    result = false;
                    Log.d(TAG, "ListView has reached bottom");
                }
            }
            if (result) {
                scrollType = ScrollType.LISTVIEW;
            }
            Log.d(TAG, "count = " + count + " LastVisiblePosition = " + listView.getLastVisiblePosition());
            return result;
        } else if (viewGroup instanceof WebView) {
            Log.d(TAG, "viewGroup is WebView");
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

            Log.d(TAG, "verticalScrollRange=" + verticalScrollRange + " verticalScrollExtent=" + verticalScrollExtent + " offset=" + offset);
            if(webView.getContentHeight()*webView.getScale()-(webView.getHeight()+webView.getScrollY())==0){
                Log.d(TAG, "WebView ContentHeight=" + webView.getContentHeight()*webView.getScale() + " Height=" + webView.getHeight());
                //webview has reached bottom
                result = false;
                Log.d(TAG, "WebView has reached bottom");
            } else {
                result = true;
                scrollType = ScrollType.WEBVIEW;
            }

            if(verticalScrollRange-verticalScrollExtent-offset <= 0){
                //webview has reached bottom
                result = false;
                Log.d(TAG, "WebView has reached bottom.");
            } else {
                result = true;
                scrollType = ScrollType.WEBVIEW;
            }

            return result;
        } else {
            int scrollableViewIndex = checkWhetherScrollableView(viewGroup);
            if (scrollableViewIndex == 1) {
                if (DEBUG_LONG_SCREENSHOT) Log.d(TAG, "viewGroup is self-defined listview");
                try {
                    Method mGetCount = viewGroup.getClass().getMethod("getCount", null);
                    Method mGetLastVisiblePosition = viewGroup.getClass().getMethod("getLastVisiblePosition", null);
                    int count = (Integer)mGetCount.invoke(viewGroup);
                    int lastVisiblePosition = (Integer)mGetLastVisiblePosition.invoke(viewGroup);
                    if (DEBUG_LONG_SCREENSHOT) Log.d(TAG, "LastVisiblePosition = " + lastVisiblePosition + " count = " + count);
                    if (count > 0) result = true;
                    //whether listview scroll to bottom
                    if (lastVisiblePosition >= count - 1 && count > 0) {
                        int lastItemBottom = viewGroup.getChildAt(viewGroup.getChildCount() - 1).getBottom();
                        int listViewHeight = viewGroup.getHeight();
                        if (DEBUG_LONG_SCREENSHOT) Log.d(TAG, "lastItemBottom = " + lastItemBottom);
                        if (DEBUG_LONG_SCREENSHOT) Log.d(TAG, "listViewHeight = " + listViewHeight);
                        if (lastItemBottom <= listViewHeight) {
                            result = false;
                            if (DEBUG_LONG_SCREENSHOT) Log.d(TAG, "ListView has reached bottom");
                        }
                    }
                    if (result) {
                        scrollType = ScrollType.SELF_DEFINED_LISTVIEW;
                    }
                    //if (DEBUG_LONG_SCREENSHOT) Log.d(TAG, "count = " + count + " LastVisiblePosition = " + listView.getLastVisiblePosition());
                    return result;
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }

            } else if (scrollableViewIndex == 2) {
                try {
                    String className = viewGroup.getClass().getName();
                    if (DEBUG_LONG_SCREENSHOT) Log.d(TAG, "viewGroup = " + className+"  viewGroup.getClass():"+viewGroup.getClass().toString());
                    Log.d(TAG, "viewGroup is self-RecyclerView");
                    Method m1 = recyclerViewClass.getDeclaredMethod("computeVerticalScrollRange", null);
                    Method m2 = recyclerViewClass.getDeclaredMethod("computeVerticalScrollExtent", null);
                    Method m3 = recyclerViewClass.getDeclaredMethod("computeVerticalScrollOffset", null);

                    m1.setAccessible(true);
                    m2.setAccessible(true);
                    m3.setAccessible(true);
                    int verticalScrollRange = (Integer)m1.invoke(viewGroup);
                    int verticalScrollExtent = (Integer)m2.invoke(viewGroup);
                    int maxScrollDistance = verticalScrollRange - verticalScrollExtent;
                    int currentScrollDistance = (Integer)m3.invoke(viewGroup);
                    Log.d(TAG, "maxScrollDistance = " + maxScrollDistance + ", currentScrollDistance = " + currentScrollDistance);
                    if (currentScrollDistance >= maxScrollDistance) {
                        result = false;
                        if (DEBUG_LONG_SCREENSHOT) Log.d(TAG, "RecyclerView has reached bottom");
                    } else {
                        result = true;
                        scrollType = ScrollType.RECYCLEVIEW;
                    }
                } catch(Exception e) {
                    e.printStackTrace();
                    return false;
                }
                return result;
            } else if (scrollableViewIndex == 3) {
                try {
                    String className = viewGroup.getClass().getName();
                    if (DEBUG_LONG_SCREENSHOT) Log.d(TAG, "viewGroup = " + className);
                    Log.d(TAG, "viewGroup is self-WebView");
                    Method m1 = viewGroup.getClass().getMethod("getContentHeight", null);
                    Method m2 = viewGroup.getClass().getMethod("getScale", null);
                    m1.setAccessible(true);
                    m2.setAccessible(true);
                    int contentHeight = (Integer)m1.invoke(viewGroup);
                    float scale = (Float)m2.invoke(viewGroup);
                    int currentScrollDistance = viewGroup.getScrollY();
                    float leftScrollDistance = contentHeight * scale - viewGroup.getHeight() - currentScrollDistance;

                    Log.d(TAG, "leftScrollDistance = " + leftScrollDistance );
                    if (contentHeight * scale - viewGroup.getHeight() - currentScrollDistance <= 1) {
                        result = false;
                        if (DEBUG_LONG_SCREENSHOT) Log.d(TAG, "WebView has reached bottom");
                    } else {
                        result = true;
                        scrollType = ScrollType.SELF_WEBVIEW;
                    }
                } catch(Exception e) {
                    e.printStackTrace();
                    return false;
                }
                return result;
            } else if (scrollableViewIndex == 4) {
                View child = viewGroup.getChildAt(0);
                //whether scrollView scroll to bottom
                if (child.getMeasuredHeight() <= viewGroup.getScrollY() + viewGroup.getHeight()) {
                    result = false;
                    if (DEBUG_LONG_SCREENSHOT) Log.d(TAG, "ScrollView has reached bottom");
                } else {
                    result = true;
                    scrollType = ScrollType.SCROLLVIEW;
                }
                if (DEBUG_LONG_SCREENSHOT) Log.d(TAG, "child.getMeasuredHeight() = " + child.getMeasuredHeight());
                return result;
            }
        }

        Log.d(TAG, "isScrollableView:result = " + result);
        return result;
    }

    private int checkWhetherScrollableView(ViewGroup viewGroup) {
        if (isNotContainsView(viewGroup, "listview")
                && isNotContainsView(viewGroup, "recyclerview")
                && isNotContainsView(viewGroup, "webview")
                && isNotContainsView(viewGroup, "scrollview")){
            return 0;
        }

        int res = 0;
        Class clazz = viewGroup.getClass();
        for (;;) {
            String classNamepath = clazz.getName().toLowerCase();
            int index = classNamepath.lastIndexOf(".");
            if (DEBUG_LONG_SCREENSHOT) Log.d(TAG, "classNamepath = " + classNamepath + " index = " + classNamepath.lastIndexOf(".")+"  clazz:"+clazz.toString());
            String className = classNamepath.substring(index + 1);
            if (DEBUG_LONG_SCREENSHOT) Log.d(TAG, "className = " + className);
            if (className.equals("listview")) {
                res = 1;
                break;
            } else if (className.equals("abslistview")) {
                res = 1;
                break;
            } else if (className.equals("viewgroup")) {
                break;
            } else if (className.equals("adapterview")) {
                res = 1;
                break;
            } else if (className.equals("recyclerview")) {
                res = 2;
                recyclerViewClass = clazz;
                if (classNamepath.equals("android.support.v7.widget.recyclerview")) isSelfRecyclerView = false;
                break;
            } else if (className.equals("webview")) {
                res = 3;
                break;
            } else if (className.equals("scrollview")) {
                res = 4;
                break;
            }
            clazz = clazz.getSuperclass();
        }

        return res;
    }

    private boolean isNotContainsView(ViewGroup viewGroup, String className) {
        return (!viewGroup.getClass().getName().toLowerCase().contains(className)
                && !viewGroup.getClass().getSuperclass().getName().toLowerCase().contains(className)
                && !viewGroup.getClass().getSuperclass().getSuperclass().getName().toLowerCase().contains(className));
    }

    private ScrollType scrollType;

    enum ScrollType {
        SCROLLVIEW,
        LISTVIEW,
        WEBVIEW,
        RECYCLEVIEW,
        SELF_DEFINED_LISTVIEW,
        SELF_WEBVIEW
    }
}
