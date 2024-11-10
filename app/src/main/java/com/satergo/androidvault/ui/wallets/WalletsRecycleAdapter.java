package com.satergo.androidvault.ui.wallets;

import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.satergo.androidvault.R;
import com.satergo.androidvault.storage.IncorrectPasswordException;
import com.satergo.androidvault.storage.Wallet;
import com.satergo.androidvault.storage.WalletStorage;

import org.ergoplatform.appkit.Mnemonic;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class WalletsRecycleAdapter extends RecyclerView.Adapter<WalletsRecycleAdapter.ViewHolder> {

	private final WalletStorage walletStorage;

	public WalletsRecycleAdapter(WalletStorage walletStorage) {
		this.walletStorage = walletStorage;
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.holder_wallet, parent, false));
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		Wallet wallet = walletStorage.wallets().get(position);
		((TextView) holder.itemView.findViewById(R.id.name)).setText(wallet.name);
		((Button) holder.itemView.findViewById(R.id.viewSeed)).setOnClickListener(v -> {
			EditText password = new EditText(holder.itemView.getContext());
			password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
			new AlertDialog.Builder(holder.itemView.getContext())
					.setTitle(R.string.passwordNeeded)
					.setView(password)
					.setPositiveButton("OK", (dialog, which) -> {
						try {
							Mnemonic mnemonic = wallet.decryptSeed(password.getText().toString().toCharArray());
							new AlertDialog.Builder(holder.itemView.getContext())
									.setMessage(mnemonic.getPhrase().toStringUnsecure().toString())
									.show();
						} catch (GeneralSecurityException e) {
							throw new RuntimeException(e);
						} catch (IncorrectPasswordException e) {
							Toast.makeText(holder.itemView.getContext(), R.string.incorrectPassword, Toast.LENGTH_SHORT).show();
						}
					})
					.setNegativeButton(R.string.cancel, null)
					.show();
		});
		((Button) holder.itemView.findViewById(R.id.delete)).setOnClickListener(v -> {
			new AlertDialog.Builder(holder.itemView.getContext())
					.setTitle(R.string.areYouSure)
					.setMessage(R.string.walletWillBeDeleted)
					.setPositiveButton(R.string.delete, (dialog, which) -> {
						try {
							WalletStorage.INSTANCE.remove(holder.itemView.getContext(), wallet.getId());
							notifyItemRemoved(position);
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					})
					.setNegativeButton(R.string.cancel, null)
					.show();
		});
	}

	@Override
	public int getItemCount() {
		return walletStorage.wallets().size();
	}

	public static class ViewHolder extends RecyclerView.ViewHolder {
		public ViewHolder(@NonNull View itemView) {
			super(itemView);
		}
	}
}
