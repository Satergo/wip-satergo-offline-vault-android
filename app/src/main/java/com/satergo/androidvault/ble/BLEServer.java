package com.satergo.androidvault.ble;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.content.Context;
import android.os.Build;
import android.os.ParcelUuid;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.satergo.androidvault.vault.VaultService;
import com.welie.blessed.BluetoothCentral;
import com.welie.blessed.BluetoothPeripheralManager;
import com.welie.blessed.BluetoothPeripheralManagerCallback;
import com.welie.blessed.GattStatus;
import com.welie.blessed.ReadResponse;
import com.welie.blessed.ReadResponseInterface;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@SuppressWarnings({"RedundantMethodOverride", "FieldCanBeLocal"})
public class BLEServer {

	public enum Status {
		NOT_ADVERTISING, ADVERTISING, CONNECTED, LOST_CONNECTION
	}

	private static BLEServer INSTANCE;
	private final VaultService service;

	public static boolean exists() {
		return INSTANCE != null;
	}

	@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
	public static synchronized BLEServer getOrCreate(Context context, VaultService vaultService) {
		if (INSTANCE == null) {
			return INSTANCE = new BLEServer(context.getApplicationContext(), vaultService);
		}
		return INSTANCE;
	}

	private final MutableLiveData<Status> status = new MutableLiveData<>();
	private final MutableLiveData<String> statusInfo = new MutableLiveData<>();

	public LiveData<Status> getStatus() { return status; }
	public LiveData<String> getStatusInfo() { return statusInfo; }

	public static BLEServer get() {
		if (INSTANCE == null)
			throw new IllegalStateException();
		return INSTANCE;
	}

	private final List<BLEService> services;
	private final BluetoothPeripheralManager peripheralManager;

	private final BluetoothPeripheralManagerCallback peripheralManagerCallback = new BluetoothPeripheralManagerCallback() {

		@Override
		public @NonNull ReadResponseInterface onCharacteristicRead(@NonNull BluetoothCentral central, @NonNull BluetoothGattCharacteristic ch) {
			for (BLEService service : services) {
				if (service.service().getUuid().equals(ch.getService().getUuid()))
					return service.onCharacteristicRead(central, ch);
			}
			return new ReadResponse(GattStatus.REQUEST_NOT_SUPPORTED, new byte[0]);
		}

		@Override
		public @NotNull GattStatus onCharacteristicWrite(@NotNull BluetoothCentral central, @NotNull BluetoothGattCharacteristic ch, byte @NotNull [] value) {
			for (BLEService service : services) {
				if (service.service().getUuid().equals(ch.getService().getUuid()))
					return service.onCharacteristicWrite(central, ch, value);
			}
			return GattStatus.REQUEST_NOT_SUPPORTED;
		}

		@Override
		public void onCharacteristicWriteCompleted(@NonNull BluetoothCentral bluetoothCentral, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value) {
			super.onCharacteristicWriteCompleted(bluetoothCentral, characteristic, value);
		}

		@Override
		public void onCentralConnected(@NotNull BluetoothCentral central) {
			statusInfo.setValue("Connected to " + central.getName());
			central.createBond();
			stopAdvertising();
			status.setValue(Status.CONNECTED);
		}

		@Override
		public void onCentralDisconnected(@NotNull BluetoothCentral central) {
			statusInfo.setValue("Lost connection");
			status.setValue(Status.LOST_CONNECTION);
		}
	};

	@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
	private BLEServer(Context context, VaultService service) {
		this.service = service;
		BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
		if (bluetoothManager == null) {
			throw new UnsupportedOperationException("Bluetooth not supported");
		}

		BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
		if (!bluetoothAdapter.isMultipleAdvertisementSupported()) {
			throw new UnsupportedOperationException("Does not support advertising");
		}

		// Set the adapter name as this is used when advertising
		bluetoothAdapter.setName(Build.MODEL);

		peripheralManager = new BluetoothPeripheralManager(context, bluetoothManager, peripheralManagerCallback);
		peripheralManager.removeAllServices();

		services = Collections.singletonList(service);
		for (BLEService s : services) {
			peripheralManager.add(s.service());
		}
	}

	public void startAdvertising() {
		statusInfo.setValue("Available for being connected to");
		UUID serviceUuid = service.service().getUuid();
		AdvertiseSettings advertiseSettings = new AdvertiseSettings.Builder()
				.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
				.setConnectable(true)
				.setTimeout(0)
				.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
				.build();

		AdvertiseData advertiseData = new AdvertiseData.Builder()
				.setIncludeTxPowerLevel(true)
				.addServiceUuid(new ParcelUuid(serviceUuid))
				.build();

		AdvertiseData scanResponse = new AdvertiseData.Builder()
				.setIncludeDeviceName(true)
				.build();

		peripheralManager.startAdvertising(advertiseSettings, advertiseData, scanResponse);
		status.setValue(Status.ADVERTISING);
	}

	public void stopAdvertising() {
		peripheralManager.stopAdvertising();
	}

	public void stopServer() {
		peripheralManager.close();
		INSTANCE = null;
	}
}
