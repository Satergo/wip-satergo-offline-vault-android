package com.satergo.androidvault.utils;

import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class Encryption {

	private static final Encryption INSTANCE = new Encryption();

	public static Encryption newest() {
		return INSTANCE;
	}

	private Encryption() {}

	public byte[] encryptData(byte[] iv, SecretKey secretKey, byte[] data) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
		Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
		GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iv);

		cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

		// Encrypt the data
		return cipher.doFinal(data);
	}

	public byte[] decryptData(byte[] iv, SecretKey secretKey, ByteBuffer encryptedData) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
		// Get the rest of encrypted data
		byte[] cipherBytes = new byte[encryptedData.remaining()];
		encryptedData.get(cipherBytes);

		Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
		GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iv);

		cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

		// Decrypt the data
		return cipher.doFinal(cipherBytes);
	}

	public SecretKey generateSecretKey(char[] password, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
		KeySpec spec = new PBEKeySpec(password, salt, 100000, 128);
		SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
		byte[] key = secretKeyFactory.generateSecret(spec).getEncoded();
		return new SecretKeySpec(key, "AES");
	}

	public byte[] generateRandom12() {
		byte[] random = new byte[12];
		new SecureRandom().nextBytes(random);
		return random;
	}
}
