package com.satergo.androidvault.ui.connection;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;
import com.satergo.androidvault.R;
import com.satergo.androidvault.SignActivity;
import com.satergo.androidvault.storage.WalletStorage;
import com.satergo.androidvault.utils.Utils;
import com.satergo.androidvault.ble.BLEServer;
import com.satergo.androidvault.vault.VaultService;
import com.satergo.androidvault.databinding.FragmentConnectionBinding;
import com.satergo.androidvault.storage.Wallet;
import com.satergo.androidvault.vault.SignJob;
import com.satergo.androidvault.vault.VaultWorker;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

public class ConnectionFragment extends Fragment {

	private FragmentConnectionBinding binding;

	private final ActivityResultLauncher<String> btConnPerm = registerForActivityResult(
			new ActivityResultContracts.RequestPermission(),
			result -> {});

	private final ActivityResultLauncher<String> btAdvPerm = registerForActivityResult(
			new ActivityResultContracts.RequestPermission(),
			result -> {});

	private final ActivityResultLauncher<String> legacyBtPerm = registerForActivityResult(
			new ActivityResultContracts.RequestPermission(),
			result -> {});

	private final ActivityResultLauncher<Intent> enableBluetoothThenLaunch = registerForActivityResult(
			new ActivityResultContracts.StartActivityForResult(),
			result -> {
				launchServer();
				binding.start.setText(R.string.running_);
				binding.start.setEnabled(false);
			}
	);

	private SignJob signJob;
	private final ActivityResultLauncher<Intent> signTransaction = registerForActivityResult(
			new ActivityResultContracts.StartActivityForResult(),
			result -> {
				if (result.getResultCode() == SignActivity.RESULT_DENIED || result.getData() == null) {
					signJob.reject();
				} else if (result.getResultCode() == SignActivity.RESULT_SIGNED) {
					byte[] serializedSignatures = result.getData().getByteArrayExtra("serializedSignatures");
					signJob.setSignatures(Utils.deserializeSignatures(serializedSignatures));
				}
			});

	public View onCreateView(@NonNull LayoutInflater inflater,
							 ViewGroup container, Bundle savedInstanceState) {
		binding = FragmentConnectionBinding.inflate(inflater, container, false);
		View root = binding.getRoot();
		if (BLEServer.exists()) {
			bindToServerState(BLEServer.get());
			if (BLEServer.get().getStatus().getValue() == BLEServer.Status.ADVERTISING) {
				binding.start.setText(R.string.running_);
				binding.start.setEnabled(false);
			}
		}

		binding.start.setOnClickListener(v -> {
			if (BLEServer.exists()) {
				if (BLEServer.get().getStatus().getValue() == BLEServer.Status.LOST_CONNECTION) {
					BLEServer.get().startAdvertising();
				}
			}

			if (WalletStorage.INSTANCE.wallets().isEmpty()) {
				Snackbar.make(v, R.string.needToCreateAWalletFirst, Snackbar.LENGTH_LONG).show();
				return;
			}

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
				boolean connPerm = ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
				boolean advPerm = ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED;
				if (!connPerm || !advPerm) {
					if (!connPerm) btConnPerm.launch(Manifest.permission.BLUETOOTH_CONNECT);
					if (!advPerm) btAdvPerm.launch(Manifest.permission.BLUETOOTH_ADVERTISE);
					Toast.makeText(getContext(), R.string.acceptThenTryAgain, Toast.LENGTH_SHORT).show();
					return;
				}
			} else {
				boolean legacyPerm = ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED;
				if (!legacyPerm) {
					btConnPerm.launch(Manifest.permission.BLUETOOTH);
					Toast.makeText(getContext(), R.string.acceptThenTryAgain, Toast.LENGTH_SHORT).show();
					return;
				}
			}

			if (!getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
				Snackbar.make(v, R.string.bleUnsupported, Snackbar.LENGTH_LONG).show();
				return;
			}

			BluetoothManager bluetoothManager = (BluetoothManager) getContext().getSystemService(Context.BLUETOOTH_SERVICE);
			BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

			if (bluetoothAdapter == null) {
				Snackbar.make(v, R.string.deviceNoBluetooth, Snackbar.LENGTH_LONG).show();
				return;
			}

			if (!bluetoothAdapter.isEnabled()) {
				Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				enableBluetoothThenLaunch.launch(enableBtIntent);
				return;
			}
			launchServer();
			binding.start.setText(R.string.running_);
			binding.start.setEnabled(false);
		});

		return root;
	}

	@SuppressLint("MissingPermission")
	private void launchServer() {
		if (!BLEServer.exists()) {
			Wallet wallet = WalletStorage.INSTANCE.wallets().get(0);
			BLEServer bleServer = BLEServer.getOrCreate(getContext(), new VaultService(new VaultWorker() {
				@Override
				@SuppressLint("NewApi")
				public CompletableFuture<byte[]> getExtendedPublicKey() {
					CompletableFuture<byte[]> future = new CompletableFuture<>();
					getActivity().runOnUiThread(() -> {
						new AlertDialog.Builder(getContext())
								.setTitle(R.string.sendPublicKeyPrompt)
								.setPositiveButton(R.string.yes, (dialog, which) -> {
									future.complete(Utils.serializeExtPubKey(wallet.parentExtPubKey));
								})
								.setNegativeButton(R.string.no, (dialog, which) -> {
									future.complete(null);
								})
								.show();
					});
					return future;
				}

				@Override
				public void signJobReceived(SignJob job) {
					ConnectionFragment.this.signJob = job;
					Intent intent = new Intent(getContext(), SignActivity.class);
					intent.putExtra("walletId", wallet.getId());
					intent.putExtra("dataToBeSigned", job.dataToBeSigned);
					intent.putIntegerArrayListExtra("inputAddressEip3Indexes", new ArrayList<>(job.inputAddressEip3Indexes));
					if (job.changeAddressIndex != null)
						intent.putExtra("changeAddressEip3Index", (int) job.changeAddressIndex);
					signTransaction.launch(intent);
				}
			}));
			bindToServerState(bleServer);
			bleServer.startAdvertising();
		}
	}

	private void bindToServerState(BLEServer bleServer) {
		bleServer.getStatus().observe(getViewLifecycleOwner(), status -> {
			if (status == BLEServer.Status.LOST_CONNECTION) {
				binding.start.setText("Open for connection");
				binding.start.setEnabled(true);
			}
		});
		bleServer.getStatusInfo().observe(getViewLifecycleOwner(), binding.status::setText);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		binding = null;
	}
}