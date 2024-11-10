package com.satergo.androidvault.storage;

import android.content.Context;

import androidx.lifecycle.LiveData;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WalletStorage {

	public static WalletStorage INSTANCE;

	private static final String FILE_NAME = "wallets.dat";
	private ArrayList<Wallet> wallets;

	public WalletStorage(Context context) throws IOException {
		if (INSTANCE != null) throw new IllegalStateException();
		this.wallets = new ArrayList<>();
		readIfExists(context);
	}

	private synchronized void readIfExists(Context context) throws IOException {
		try (DataInputStream in = new DataInputStream(context.openFileInput(FILE_NAME))) {
			long version = in.readLong();
			int walletCount = in.readInt();
			ArrayList<Wallet> wallets = new ArrayList<>();
			for (int i = 0; i < walletCount; i++) {
				int byteLength = in.readInt();
				byte[] bytes = new byte[byteLength];
				in.readFully(bytes);
				wallets.add(Wallet.deserialize(new DataInputStream(new ByteArrayInputStream(bytes))));
			}
			this.wallets = wallets;
		} catch (FileNotFoundException ignored) {
		}
	}

	public synchronized void store(Context context) throws IOException {
		try (ByteArrayOutputStream b = new ByteArrayOutputStream();
			 DataOutputStream out = new DataOutputStream(b)) {
			out.writeLong(0);
			out.writeInt(wallets.size());
			for (Wallet wallet : wallets) {
				byte[] bytes = wallet.serialize();
				out.writeInt(bytes.length);
				out.write(bytes);
			}
			try (FileOutputStream fo = context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE)) {
				fo.write(b.toByteArray());
			}
		}
	}

	public void add(Context context, Wallet wallet) throws IOException {
		if (wallet.hasId()) throw new IllegalArgumentException();
		if (wallets.isEmpty()) {
			wallet.setId(1);
		} else {
			int id = wallets.get(wallets.size() - 1).getId();
			while (true) {
				id++;
				int finalId = id;
				if (wallets.stream().noneMatch(w -> w.getId() == finalId)) {
					wallet.setId(id);
					break;
				}
			}
		}
		wallets.add(wallet);
		store(context);
	}

	public void remove(Context context, int id) throws IOException {
		if (wallets.removeIf(w -> w.getId() == id)) {
			store(context);
		} else {
			throw new IllegalArgumentException("No such wallet " + id);
		}
	}

	public List<Wallet> wallets() {
		return Collections.unmodifiableList(wallets);
	}

	public Wallet getById(int walletId) {
		return wallets.stream().filter(w -> w.getId() == walletId).findAny().orElseThrow();
	}
}
