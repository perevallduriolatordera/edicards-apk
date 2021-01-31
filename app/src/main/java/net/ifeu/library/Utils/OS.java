package net.ifeu.library.Utils;

import android.os.Build;

public class OS {
	
	public static double getAndroidVersion() {
		return Build.VERSION.SDK_INT;
	}
	
	public static boolean isOlderVersion() {
		return OS.getAndroidVersion() < 21;
	}
}
