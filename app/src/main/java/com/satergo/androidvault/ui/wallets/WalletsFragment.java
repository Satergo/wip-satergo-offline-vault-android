package com.satergo.androidvault.ui.wallets;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.satergo.androidvault.utils.SeedUtils;
import com.satergo.androidvault.databinding.FragmentWalletsBinding;
import com.satergo.androidvault.storage.Wallet;
import com.satergo.androidvault.storage.WalletStorage;

import org.ergoplatform.appkit.Mnemonic;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class WalletsFragment extends Fragment {

	private FragmentWalletsBinding binding;

	public View onCreateView(@NonNull LayoutInflater inflater,
							 ViewGroup container, Bundle savedInstanceState) {

		binding = FragmentWalletsBinding.inflate(inflater, container, false);
		View root = binding.getRoot();

		binding.wallets.setLayoutManager(new LinearLayoutManager(getContext()));
		binding.wallets.setAdapter(new WalletsRecycleAdapter(WalletStorage.INSTANCE));

		getParentFragmentManager().setFragmentResultListener(CreateWalletDialog.REQUEST_KEY, this, (requestKey, result) -> {
			String name = result.getString("name"), password = result.getString("password");
			char[] phrase = Mnemonic.generateEnglishMnemonic().toCharArray();
			Mnemonic mnemonic = Mnemonic.create(phrase, new char[0]);
			try {
				Wallet wallet = new Wallet(null, name, SeedUtils.getParentExtPubKey(mnemonic), SeedUtils.encrypt(mnemonic, password.toCharArray()));
				WalletStorage.INSTANCE.add(getContext(), wallet);
				Toast.makeText(getContext(), "Wallet generated. Please view the seed phrase and write it down.", Toast.LENGTH_LONG).show();
				binding.wallets.getAdapter().notifyDataSetChanged();
			} catch (IOException | GeneralSecurityException e) {
				throw new RuntimeException(e);
			}
		});
		binding.add.setOnClickListener(v -> {
			new CreateWalletDialog()
					.show(getParentFragmentManager(), "create");
		});

		getParentFragmentManager().setFragmentResultListener(RestoreWalletDialog.REQUEST_KEY, this, (requestKey, result) -> {
			String name = result.getString("name"), seedPhrase = result.getString("seedPhrase"), password = result.getString("password");
			Mnemonic mnemonic = Mnemonic.create(seedPhrase.toCharArray(), new char[0]);
			try {
				Wallet wallet = new Wallet(null, name, SeedUtils.getParentExtPubKey(mnemonic), SeedUtils.encrypt(mnemonic, password.toCharArray()));
				WalletStorage.INSTANCE.add(getContext(), wallet);
				Toast.makeText(getContext(), "Wallet restored.", Toast.LENGTH_LONG).show();
				binding.wallets.getAdapter().notifyDataSetChanged();
			} catch (IOException | GeneralSecurityException e) {
				throw new RuntimeException(e);
			}
		});
		binding.restore.setOnClickListener(v -> {
			new RestoreWalletDialog()
					.show(getParentFragmentManager(), "restore");
		});


		return root;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		binding = null;
	}
}