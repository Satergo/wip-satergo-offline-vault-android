package com.satergo.androidvault.storage;

import com.satergo.androidvault.utils.SeedUtils;
import com.satergo.androidvault.utils.Utils;

import org.ergoplatform.ErgoAddressEncoder;
import org.ergoplatform.P2PKAddress;
import org.ergoplatform.appkit.Address;
import org.ergoplatform.appkit.Mnemonic;
import org.ergoplatform.appkit.NetworkType;
import org.ergoplatform.sdk.wallet.secrets.ExtendedPublicKey;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Objects;

import javax.crypto.AEADBadTagException;

public final class Wallet {

	private Integer id;
	public final String name;
	public final ExtendedPublicKey parentExtPubKey;
	public final byte[] encryptedSeed;

	public Wallet(Integer id, String name, ExtendedPublicKey parentExtPubKey, byte[] encryptedSeed) {
		this.id = id;
		Objects.requireNonNull(name);
		Objects.requireNonNull(parentExtPubKey);
		Objects.requireNonNull(encryptedSeed);
		this.name = name;
		this.parentExtPubKey = parentExtPubKey;
		this.encryptedSeed = encryptedSeed;
	}

	public boolean hasId() { return id != null; }
	public int getId() { return Objects.requireNonNull(id); }
	public void setId(int id) { this.id = id; }

	public Mnemonic decryptSeed(char[] password) throws GeneralSecurityException, IncorrectPasswordException {
		try {
			return SeedUtils.decrypt(encryptedSeed, password);
		} catch (AEADBadTagException e) {
			throw new IncorrectPasswordException();
		}
	}

	public Address deriveAddress(int index) {
		return new Address(P2PKAddress.apply(parentExtPubKey.child(index).key(), new ErgoAddressEncoder(NetworkType.MAINNET.networkPrefix)));
	}

	public byte[] serialize() {
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(b);
		byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
		try {
			out.writeInt(id);
			out.writeShort(nameBytes.length);
			out.write(nameBytes);
			byte[] extPubKey = Utils.serializeExtPubKey(parentExtPubKey);
			out.writeShort(extPubKey.length);
			out.write(extPubKey);
			out.writeShort(encryptedSeed.length);
			out.write(encryptedSeed);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return b.toByteArray();
	}

	public static Wallet deserialize(DataInputStream in) throws IOException {
		int id = in.readInt();

		byte[] nameBytes = new byte[in.readUnsignedShort()];
		in.readFully(nameBytes);
		String name = new String(nameBytes, StandardCharsets.UTF_8);

		byte[] extPubKeyBytes = new byte[in.readUnsignedShort()];
		in.readFully(extPubKeyBytes);
		ExtendedPublicKey extPubKey = Utils.deserializeExtPubKey(extPubKeyBytes);

		byte[] encryptedSeed = new byte[in.readUnsignedShort()];
		in.readFully(encryptedSeed);

		return new Wallet(id, name, extPubKey, encryptedSeed);
	}
}
