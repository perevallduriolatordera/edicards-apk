package net.ifeu.library.Devices;

import java.util.ArrayList;
import java.util.Set;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

public class BlueTooth {

	public static boolean IsEnabled() {
		
		try {
			BluetoothAdapter bluetoothAdapter = BluetoothAdapter
				.getDefaultAdapter();
		
			return bluetoothAdapter.isEnabled();
		} catch (Exception e)
		{
			return true;
		}
		
	}

	public static boolean ActivateBlueTooth() {
		
		try {
			BluetoothAdapter bluetoothAdapter = BluetoothAdapter
					.getDefaultAdapter();
	
			boolean enabled = true;
			if (!bluetoothAdapter.isEnabled()) {
				enabled = bluetoothAdapter.enable();
	
			}

			return enabled;
		} catch (Exception e)
		{
			return false;
		}
	}

	public static ArrayList<String> getBluetoothDevicesMacAddress() {
		BluetoothAdapter mBtAdapter;
		mBtAdapter = BluetoothAdapter.getDefaultAdapter();
		Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
		ArrayList<String> macAddress = new ArrayList<>();

		if (pairedDevices.size() > 0) {
			for (BluetoothDevice device : pairedDevices) {

				macAddress.add(device.getAddress());
			}
		}

		return macAddress;
	}

}
