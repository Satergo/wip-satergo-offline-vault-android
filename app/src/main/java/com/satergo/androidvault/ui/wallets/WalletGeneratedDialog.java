package com.satergo.androidvault.ui.wallets;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.satergo.androidvault.R;

public class WalletGeneratedDialog extends DialogFragment {

	public static WalletGeneratedDialog newInstance(String phrase) {
		Bundle bundle = new Bundle();
		bundle.putString("phrase", phrase);
		WalletGeneratedDialog dialog = new WalletGeneratedDialog();
		dialog.setArguments(bundle);
		return dialog;
	}

	public WalletGeneratedDialog() {
		setCancelable(false);
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
		AlertDialog dialog = new AlertDialog.Builder(getContext())
				.setTitle(getString(R.string.walletGenerated))
				.setMessage(getString(R.string.walletGenerationMessage) + "\n\n" + getArguments().getString("phrase"))
				.setPositiveButton(R.string.iHaveSavedIt, (dialog1, which) -> {
				})
				.setCancelable(false)
				.create();
		dialog.setCanceledOnTouchOutside(false);
		return dialog;
	}
}
