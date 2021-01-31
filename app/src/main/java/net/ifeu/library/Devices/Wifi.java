package net.ifeu.library.Devices;

import android.content.Context;
import android.net.wifi.WifiManager;

public class Wifi {

	public static boolean IsEnabled(Context context) {
		WifiManager wifiManager = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);

		return wifiManager.isWifiEnabled();

	}

	public static void ActivateWifi(Context context) {
		WifiManager wifiManager = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);

		if (!IsEnabled(context))
			wifiManager.setWifiEnabled(true);
	}
	
	public static void DeactivateWifi(Context context) {
		WifiManager wifiManager = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);

		if (!IsEnabled(context))
			wifiManager.setWifiEnabled(false);
	}
}
