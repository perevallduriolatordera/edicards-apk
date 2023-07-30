package net.ifeu.library.Devices;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import net.ifeu.edicards.Application.AppConfig;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class _3G {
	public static boolean IsEnabled(Context context) {
		
	    ConnectivityManager connectivityManager = (ConnectivityManager) context
	            .getSystemService(Context.CONNECTIVITY_SERVICE);
	      
	    return !(connectivityManager.getNetworkInfo(
	            ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED || connectivityManager
	            .getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED);
	}
	
	public static void Activate3G(AppConfig config, boolean enable) throws IllegalArgumentException {

		try {
			final ConnectivityManager conman = (ConnectivityManager) config.getSystemService(Context.CONNECTIVITY_SERVICE);
			final Class<?> conmanClass = Class.forName(conman.getClass().getName());
			final Field iConnectivityManagerField = conmanClass.getDeclaredField("mService");
			iConnectivityManagerField.setAccessible(true);
			final Object iConnectivityManager = iConnectivityManagerField.get(conman);
			final Class<?> iConnectivityManagerClass = Class.forName(iConnectivityManager.getClass().getName());
			final Method setMobileDataEnabledMethod = iConnectivityManagerClass.getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
			setMobileDataEnabledMethod.setAccessible(true);

			
			setMobileDataEnabledMethod.invoke(iConnectivityManager, enable);
		} catch (Exception e) {
			return;
		}

	}
	
}
