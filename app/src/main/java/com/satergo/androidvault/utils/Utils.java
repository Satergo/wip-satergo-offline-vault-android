package com.satergo.androidvault.utils;

import org.ergoplatform.appkit.ColdErgoClient;
import org.ergoplatform.appkit.ErgoClient;
import org.ergoplatform.appkit.ErgoProver;
import org.ergoplatform.appkit.ErgoProverBuilder;
import org.ergoplatform.appkit.Mnemonic;
import org.ergoplatform.appkit.NetworkType;
import org.ergoplatform.appkit.Parameters;
import org.ergoplatform.appkit.ReducedTransaction;
import org.ergoplatform.appkit.SignedTransaction;
import org.ergoplatform.sdk.wallet.secrets.ExtendedPublicKey;
import org.ergoplatform.sdk.wallet.secrets.ExtendedPublicKeySerializer;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import sigmastate.serialization.SigmaSerializer;
import sigmastate.utils.SigmaByteReader;
import sigmastate.utils.SigmaByteWriter;

public class Utils {

	public static final ErgoClient ERGO_CLIENT = new ColdErgoClient(NetworkType.MAINNET, Parameters.ColdClientMaxBlockCost, Parameters.ColdClientBlockVersion);

	public static SignedTransaction sign(ReducedTransaction reducedTx, List<Integer> eip3Indexes, Mnemonic mnemonic) {
		return Utils.ERGO_CLIENT.execute(ctx -> {
			ErgoProverBuilder proverBuilder = ctx.newProverBuilder()
					.withMnemonic(mnemonic, false);
			eip3Indexes.forEach(proverBuilder::withEip3Secret);
			return proverBuilder.build().signReduced(reducedTx, 0);
		});
	}

	public static byte[] concatenateByteArrays(byte[]... byteArrays) {
		int length = 0;
		for (byte[] array : byteArrays) {
			length += array.length;
		}
		byte[] result = new byte[length];
		int currentIndex = 0;
		for (byte[] array : byteArrays) {
			System.arraycopy(array, 0, result, currentIndex, array.length);
			currentIndex += array.length;
		}
		return result;
	}


	public static byte[] serializeExtPubKey(ExtendedPublicKey extPubKey) {
		SigmaByteWriter sbw = SigmaSerializer.startWriter();
		ExtendedPublicKeySerializer.serialize(extPubKey, sbw);
		return sbw.toBytes();
	}

	public static ExtendedPublicKey deserializeExtPubKey(byte[] bytes) {
		SigmaByteReader sbr = SigmaSerializer.startReader(bytes, 0);
		return ExtendedPublicKeySerializer.parse(sbr);
	}

	public static byte[] serializeSignatures(List<byte[]> signatures) {
		ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
		DataOutputStream dataOut = new DataOutputStream(byteOut);
		try {
			for (byte[] signature : signatures) {
				dataOut.writeShort(signature.length);
				dataOut.write(signature);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return byteOut.toByteArray();
	}

	public static List<byte[]> deserializeSignatures(byte[] serialized) {
		ArrayList<byte[]> signatures = new ArrayList<>();
		ByteBuffer buffer = ByteBuffer.wrap(serialized);
		while (buffer.position() < buffer.capacity()) {
			int length = buffer.getShort() & 0xFFFF;
			byte[] signature = new byte[length];
			buffer.get(signature);
			signatures.add(signature);
		}
		return Collections.unmodifiableList(signatures);
	}

	public static ReducedTransaction deserializeReducedTx(byte[] bytes) {
		return ERGO_CLIENT.execute(ctx -> ctx.parseReducedTransaction(bytes));
	}

	public static BigDecimal toFullErg(long nanoErg) {
		return BigDecimal.valueOf(nanoErg).movePointLeft(9);
	}
}
