package net.ifeu.library.Devices;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

public class Screen {

    public static double getX(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        return metrics.widthPixels;
    }

    public static double getY(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        return  metrics.heightPixels;
    }

    public static int getProportionX(Context context, double percentage) {
        double width = Screen.getX(context);
        return (int) (width * (percentage / (double)100));
    }

    public static int getProportionY(Context context, double percentage) {
        double width = Screen.getY(context);
        return (int) (width * (percentage / (double)100));
    }

    public static void toLog(Activity context) {

        String TAG = "SCREEN";
        Display display = context.getWindowManager().getDefaultDisplay();

        // 	display size in pixels
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        Log.i(TAG, "width        = " + width);
        Log.i(TAG, "height       = " + height);

// pixels, dpi
        DisplayMetrics metrics = new DisplayMetrics();
        context.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int heightPixels = metrics.heightPixels;
        int widthPixels = metrics.widthPixels;
        int densityDpi = metrics.densityDpi;
        float xdpi = metrics.xdpi;
        float ydpi = metrics.ydpi;
        Log.i(TAG, "widthPixels  = " + widthPixels);
        Log.i(TAG, "heightPixels = " + heightPixels);
        Log.i(TAG, "densityDpi   = " + densityDpi);
        Log.i(TAG, "xdpi         = " + xdpi);
        Log.i(TAG, "ydpi         = " + ydpi);

// deprecated
        int screenHeight = display.getHeight();
        int screenWidth = display.getWidth();
        Log.i(TAG, "screenHeight = " + screenHeight);
        Log.i(TAG, "screenWidth  = " + screenWidth);
    }
}