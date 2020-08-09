package com.example.longscreentemp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Build;
import android.util.Log;
import android.view.View;

import static com.example.longscreentemp.LongScreenShotContants.DEBUG_LONG_SCREENSHOT;

public class TakeShot {

    private static String TAG = "cgp";
    private boolean isScreenshotError = false;
    int screenBottom;
    int secondBitmapHeight;


    //截得是去掉导航栏的高度（包括状态栏高度，但是状态栏的内容是没有的）
    private Bitmap captureScreen(Context context, View view) {
        Log.e(TAG,"captureScreen view:"+view.getClass().toString());
        disableOverlayViews();
        view.setDrawingCacheEnabled(true);
        Bitmap b1;
        try {
            view.buildDrawingCache();
            b1 = view.getDrawingCache();
        } catch (Exception e) {
            Log.d(TAG, "captureScreen:" + e.getMessage());
            isScreenshotError = true;
            return null;
        }
        Rect frame = new Rect();
        view.getWindowVisibleDisplayFrame(frame);
        int statusBarHeight = frame.top;
        screenBottom = frame.bottom;
        Log.d(TAG, "statusBarHeight = " + statusBarHeight + " naviBarHeight = " + screenBottom);

        int width = context.getResources().getDisplayMetrics().widthPixels;
        int height = context.getResources().getDisplayMetrics().heightPixels;
        int screenHeight = getScreenHeight();//真实高度，包含NavigationBar
        if (overlayCropPosition > 0 && cropPosition > overlayCropPosition) {
            cropPosition = overlayCropPosition;
        }
        if (DEBUG_LONG_SCREENSHOT) {
            Log.d(TAG, "height = " + height + " screenHeight = " + screenHeight + " cropPosition = " + cropPosition
                    + "  b1.getWidth():" + b1.getWidth() + "  b1.getHeight():" + b1.getHeight());
        }
        Bitmap b = Bitmap.createBitmap(b1, 0, 0, width, cropPosition > b1.getHeight() ? b1.getHeight() : cropPosition);//截图不包含导航栏
        secondBitmapHeight = screenHeight - cropPosition;//secondBitmapHeight 就是导航栏高度
        if (DEBUG_LONG_SCREENSHOT) Log.d(TAG, "captureScreen end secondBitmapHeight = " + secondBitmapHeight+"  b1.getHeight:"+ b1.getHeight()+"   b.getHeight:"+ b.getHeight());
        view.destroyDrawingCache();
        return b;
    }
}
