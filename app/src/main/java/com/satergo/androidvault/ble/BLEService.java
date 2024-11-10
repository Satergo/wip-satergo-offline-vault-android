package com.satergo.androidvault.ble;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import com.welie.blessed.BluetoothCentral;
import com.welie.blessed.GattStatus;
import com.welie.blessed.ReadResponseInterface;

import org.jetbrains.annotations.NotNull;

public interface BLEService {

	BluetoothGattService service();
	ReadResponseInterface onCharacteristicRead(BluetoothCentral central, BluetoothGattCharacteristic characteristic);
	default @NotNull GattStatus onCharacteristicWrite(@NotNull BluetoothCentral central, @NotNull BluetoothGattCharacteristic characteristic, byte @NotNull [] value) {
		return GattStatus.REQUEST_NOT_SUPPORTED;
	}
}
