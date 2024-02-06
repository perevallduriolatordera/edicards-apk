package net.ifeu.library.Performance;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Debug;

public class MemoryInfo {

    public static double getMemoryUsage(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        double availableMegs = memoryInfo.availMem / 0x100000L;
        return memoryInfo.availMem / (double)memoryInfo.totalMem * 100.0;
    }
}
