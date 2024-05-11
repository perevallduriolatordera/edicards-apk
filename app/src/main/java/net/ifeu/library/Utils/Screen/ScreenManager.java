package net.ifeu.library.Utils.Screen;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;

public class ScreenManager {

    public static class ScreenSize {
        int width;
        int height;

        ScreenSize(int width, int height) {
            this.width = width;
            this.height=height;
        }

        public int getWidth() {
            return this.width;
        }

        public int getHeight() {
            return this.height;
        }

    }

    public static ScreenSize getScreenSizeByPercentage(WindowManager windowManager, float percentage) {

        Display display = windowManager.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;

        int targetWidth = (int) (screenWidth * percentage);
        int targetHeight = (int) (screenHeight * percentage);

        return new ScreenSize(targetWidth, targetHeight);
    }

    public static int getViewWidthByLength(Context context, int length, int textSize, int gravity) {
        String mask="";
        for (int i=0; i < length; i++) mask = mask.concat("A");

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        EditText editText = new EditText(context);
        editText.setLayoutParams(params);
        editText.setText(mask);
        editText.setTextSize(textSize);
        editText.setGravity(gravity);
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int wSpec = View.MeasureSpec.makeMeasureSpec(displayMetrics.widthPixels, View.MeasureSpec.AT_MOST);
        int hSpec = View.MeasureSpec.makeMeasureSpec(displayMetrics.heightPixels, View.MeasureSpec.AT_MOST);
        editText.measure(wSpec, hSpec);
        return Math.max(editText.getMeasuredWidth(), editText.getMeasuredHeight());
    }
}


