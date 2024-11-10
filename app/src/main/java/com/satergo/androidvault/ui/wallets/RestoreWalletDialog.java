package com.satergo.androidvault.ui.wallets;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.satergo.androidvault.R;

public class RestoreWalletDialog extends DialogFragment {

	public static final String REQUEST_KEY = "RESTORE_WALLET";

	@NonNull
	@Override
	public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
		EditText name = new EditText(getContext());
		name.setHint(R.string.walletName);
		EditText seedPhrase = new EditText(getContext());
		seedPhrase.setHint(R.string.seedPhrase);
		seedPhrase.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
		EditText password = new EditText(getContext());
		password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
		password.setHint(R.string.encryptionPassword);

		LinearLayout root = new LinearLayout(getContext());
		root.setOrientation(LinearLayout.VERTICAL);
		root.addView(name);
		root.addView(seedPhrase);
		root.addView(password);
		return new AlertDialog.Builder(getContext())
				.setTitle(R.string.restoreWallet)
				.setView(root)
				.setPositiveButton(R.string.restore, (dialogInterface, which) -> {
					if (name.getText().toString().isBlank()) {
						Toast.makeText(getContext(), R.string.walletNameRequired, Toast.LENGTH_SHORT).show();
					} else if (seedPhrase.getText().toString().isBlank()) {
						Toast.makeText(getContext(), R.string.seedPhraseRequired, Toast.LENGTH_SHORT).show();
					} else if (password.getText().toString().isBlank()) {
						Toast.makeText(getContext(), R.string.encryptionPasswordRequired, Toast.LENGTH_SHORT).show();
					} else {
						Bundle bundle = new Bundle();
						bundle.putString("name", name.getText().toString());
						bundle.putString("seedPhrase", seedPhrase.getText().toString());
						bundle.putString("password", password.getText().toString());
						getParentFragmentManager().setFragmentResult(REQUEST_KEY, bundle);
					}
				})
				.setNegativeButton(R.string.cancel, null)
				.create();
	}
}
