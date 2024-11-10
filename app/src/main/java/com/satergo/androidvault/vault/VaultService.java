package com.satergo.androidvault.vault;

import static android.bluetooth.BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED;
import static android.bluetooth.BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_READ;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import com.satergo.androidvault.BuildConfig;
import com.satergo.androidvault.ble.BLEService;
import com.satergo.androidvault.ble.Id;
import com.satergo.androidvault.utils.Utils;
import com.welie.blessed.BluetoothCentral;
import com.welie.blessed.DeferredReadResponse;
import com.welie.blessed.GattStatus;
import com.welie.blessed.ReadResponse;
import com.welie.blessed.ReadResponseInterface;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

/**
 * The Vault BLE Service
 */
public class VaultService implements BLEService {

	public static final int PROTOCOL_VERSION = 0;

	private final BluetoothGattService service = new BluetoothGattService(Id.SERVICE, BluetoothGattService.SERVICE_TYPE_PRIMARY);
	private final VaultWorker worker;
	private SignJob signJob;

	public VaultService(VaultWorker worker) {
		this.worker = worker;
		service.addCharacteristic(new BluetoothGattCharacteristic(Id.Characteristic.APP_INFO,
				PROPERTY_READ, PERMISSION_READ_ENCRYPTED));

		service.addCharacteristic(new BluetoothGattCharacteristic(Id.Characteristic.EXT_PUB_KEY,
				PROPERTY_READ, PERMISSION_READ_ENCRYPTED));

		service.addCharacteristic(new BluetoothGattCharacteristic(Id.Characteristic.REQUEST_TX_SIGNING,
				PROPERTY_WRITE_NO_RESPONSE, PERMISSION_WRITE_ENCRYPTED));

		service.addCharacteristic(new BluetoothGattCharacteristic(Id.Characteristic.SIGNATURES,
				PROPERTY_READ, PERMISSION_READ_ENCRYPTED));
	}

	@Override
	public BluetoothGattService service() {
		return service;
	}

	private static class ChunkedRead {
		UUID characteristic;
		byte[] data;
		int offset;
		int perChunk;
	}
	// Peer reading data from this service
	private ChunkedRead chunkedRead;
	// Chunked writes from the peer to this service
	private UUID chunkedWriteUuid;
	private ByteBuffer chunkedWrite = null;

	@Override
	public ReadResponseInterface onCharacteristicRead(BluetoothCentral central, BluetoothGattCharacteristic ch) {
		if (chunkedRead != null && chunkedRead.characteristic.equals(ch.getUuid())) {
			byte[] chunk = Arrays.copyOfRange(chunkedRead.data, chunkedRead.offset, chunkedRead.offset + chunkedRead.perChunk);
			chunkedRead.offset += chunkedRead.perChunk;
			if (chunkedRead.offset >= chunkedRead.data.length)
				chunkedRead = null;
			return new ReadResponse(GattStatus.SUCCESS, chunk);
		}
		if (ch.getUuid().equals(Id.Characteristic.APP_INFO)) {
			ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
			DataOutputStream dataOut = new DataOutputStream(byteOut);
			try {
				dataOut.writeInt(PROTOCOL_VERSION);
				dataOut.writeInt(BuildConfig.VERSION_CODE);
				dataOut.writeUTF(BuildConfig.VERSION_NAME);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			return new ReadResponse(GattStatus.SUCCESS, byteOut.toByteArray());
		}
		if (ch.getUuid().equals(Id.Characteristic.EXT_PUB_KEY)) {
			DeferredReadResponse response = new DeferredReadResponse();
			worker.getExtendedPublicKey().handle((bytes, throwable) -> {
				if (bytes == null)
					response.respond(GattStatus.READ_NOT_PERMITTED, new byte[0]);
				else response.respond(GattStatus.SUCCESS, bytes);
				return null;
			});
			return response;
		}
		if (ch.getUuid().equals(Id.Characteristic.SIGNATURES)) {
			if (signJob == null)
				return new ReadResponse(GattStatus.WRONG_STATE, new byte[0]);
			if (signJob.state() == SignJob.State.PENDING) {
				DeferredReadResponse response = new DeferredReadResponse();
				SignJob s = signJob;
				s.onStateChanged(() -> {
					if (s.state() == SignJob.State.SIGNED) {
						ReadResponse initialResponse = initialSignatureRead();
						response.respond(initialResponse.status, initialResponse.value);
					} else if (s.state() == SignJob.State.REJECTED) {
						response.respond(GattStatus.READ_NOT_PERMITTED, new byte[0]);
					}
				});
				return response;
			}
			if (signJob.state() == SignJob.State.REJECTED)
				return new ReadResponse(GattStatus.READ_NOT_PERMITTED, new byte[0]);
			if (signJob.state() == SignJob.State.SIGNED)
				return initialSignatureRead();
		}
		return new ReadResponse(GattStatus.REQUEST_NOT_SUPPORTED, new byte[0]);
	}

	private ReadResponse initialSignatureRead() {
		byte[] data = Utils.serializeSignatures(signJob.getSignatures());
		signJob = null;
		if (data.length > 510) {
			ChunkedRead cr = new ChunkedRead();
			cr.characteristic = Id.Characteristic.SIGNATURES;
			cr.data = data;
			cr.offset = 510;
			cr.perChunk = 512;
		}
		// initial / single response
		return new ReadResponse(GattStatus.SUCCESS, ByteBuffer.allocate(data.length + 2)
				.putShort((short) data.length)
				.put(data)
				.array());
	}

	@Override
	public @NotNull GattStatus onCharacteristicWrite(@NotNull BluetoothCentral central, @NotNull BluetoothGattCharacteristic ch, byte @NotNull [] value) {
		if (chunkedWriteUuid != null && ch.getUuid().equals(chunkedWriteUuid)) {
			chunkedWrite.put(value);
			if (chunkedWrite.position() == chunkedWrite.capacity()) {
				byte[] data = chunkedWrite.array();
				chunkedWriteUuid = null;
				chunkedWrite = null;
				return onCharacteristicFullyWritten(central, ch, data);
			}
			return GattStatus.SUCCESS;
		}
		if (ch.getUuid().equals(Id.Characteristic.REQUEST_TX_SIGNING)) {
			if (signJob != null && signJob.state() == SignJob.State.PENDING)
				return GattStatus.BUSY;
			ByteBuffer byteBuffer = ByteBuffer.wrap(value);
			int dataLength = Short.toUnsignedInt(byteBuffer.getShort());
			if (dataLength <= 510) {
				return onCharacteristicFullyWritten(central, ch, Arrays.copyOfRange(value, 2, value.length));
			} else {
				chunkedWriteUuid = ch.getUuid();
				chunkedWrite = ByteBuffer.allocate(dataLength).put(byteBuffer);
			}
			return GattStatus.SUCCESS;
		}
		return GattStatus.REQUEST_NOT_SUPPORTED;
	}

	private @NotNull GattStatus onCharacteristicFullyWritten(@NotNull BluetoothCentral central, @NotNull BluetoothGattCharacteristic ch, byte @NotNull [] value) {
		if (ch.getUuid().equals(Id.Characteristic.REQUEST_TX_SIGNING)) {
			ByteBuffer buffer = ByteBuffer.wrap(value);
			int inputIdxCount = buffer.getShort() & 0xFFFF;
			ArrayList<Integer> inputIndexes = new ArrayList<>();
			for (int i = 0; i < inputIdxCount; i++) {
				inputIndexes.add(buffer.getInt());
			}
			boolean hasChangeAddress = buffer.get() == 1;
			Integer changeAddressIndex = hasChangeAddress ? buffer.getInt() : null;
			byte[] data = new byte[buffer.remaining()];
			buffer.get(data);
			signJob = new SignJob(data, inputIndexes, changeAddressIndex);
			worker.signJobReceived(signJob);
			return GattStatus.SUCCESS;
		}
		return GattStatus.REQUEST_NOT_SUPPORTED;
	}
}
