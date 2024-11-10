package com.satergo.androidvault.utils;

import org.ergoplatform.appkit.Mnemonic;
import org.ergoplatform.sdk.wallet.secrets.DerivationPath;
import org.ergoplatform.sdk.wallet.secrets.ExtendedPublicKey;
import org.ergoplatform.sdk.wallet.secrets.ExtendedSecretKey;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

public class SeedUtils {

	public static byte[] serialize(Mnemonic mnemonic) {
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(b);
		byte[] phraseBytes = ((String) mnemonic.getPhrase().toStringUnsecure()).getBytes(StandardCharsets.UTF_8);
		byte[] passphraseBytes = ((String) mnemonic.getPassword().toStringUnsecure()).getBytes(StandardCharsets.UTF_8);
		try {
			out.writeShort(phraseBytes.length);
			out.write(phraseBytes);
			out.writeShort(passphraseBytes.length);
			out.write(passphraseBytes);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return b.toByteArray();
	}

	public static Mnemonic deserialize(byte[] bytes) {
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		byte[] phraseBytes = new byte[buffer.getShort() & 0xFFFF];
		buffer.get(phraseBytes);
		byte[] passphraseBytes = new byte[buffer.getShort() & 0xFFFF];
		buffer.get(passphraseBytes);
		return Mnemonic.create(new String(phraseBytes, StandardCharsets.UTF_8).toCharArray(), new String(passphraseBytes, StandardCharsets.UTF_8).toCharArray());
	}

	public static byte[] encrypt(Mnemonic mnemonic, char[] password) throws GeneralSecurityException {
		byte[] seedBytes = serialize(mnemonic);
		byte[] iv = Encryption.newest().generateRandom12();
		byte[] salt = Encryption.newest().generateRandom12();
		byte[] encrypted = Encryption.newest().encryptData(iv, Encryption.newest().generateSecretKey(password, salt), seedBytes);
		return Utils.concatenateByteArrays(iv, salt, encrypted);
	}

	public static Mnemonic decrypt(byte[] encrypted, char[] password) throws GeneralSecurityException {
		ByteBuffer buffer = ByteBuffer.wrap(encrypted);
		byte[] iv = new byte[12];
		buffer.get(iv);
		byte[] salt = new byte[12];
		buffer.get(salt);
		byte[] decrypted = Encryption.newest().decryptData(iv, Encryption.newest().generateSecretKey(password, salt), buffer);
		return deserialize(decrypted);
	}

	public static ExtendedPublicKey getParentExtPubKey(Mnemonic mnemonic) {
		ExtendedSecretKey rootSecret = ExtendedSecretKey.deriveMasterKey(mnemonic.toSeed(), false);
		return ((ExtendedSecretKey) rootSecret.derive(DerivationPath.fromEncoded("m/44'/429'/0'/0").get())).publicKey();
	}
}
