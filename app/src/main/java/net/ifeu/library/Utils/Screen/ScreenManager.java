package net.ifeu.library.Utils.Screen;

import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

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
}


