package com.satergo.androidvault;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.satergo.androidvault.databinding.ActivitySignBinding;
import com.satergo.androidvault.storage.IncorrectPasswordException;
import com.satergo.androidvault.storage.Wallet;
import com.satergo.androidvault.storage.WalletStorage;
import com.satergo.androidvault.ui.MessageDialog;
import com.satergo.androidvault.utils.Utils;

import org.ergoplatform.ErgoBox;
import org.ergoplatform.appkit.Address;
import org.ergoplatform.appkit.NetworkType;
import org.ergoplatform.appkit.OutBox;
import org.ergoplatform.appkit.ReducedTransaction;
import org.ergoplatform.appkit.SignedInput;
import org.ergoplatform.appkit.SignedTransaction;
import org.ergoplatform.sdk.ErgoId;
import org.ergoplatform.sdk.ErgoToken;

import java.math.BigDecimal;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SignActivity extends AppCompatActivity {

	private ActivitySignBinding binding;
	
	public static final int RESULT_SIGNED = 1029;
	public static final int RESULT_DENIED = 1030;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		binding = ActivitySignBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
			Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
			return insets;
		});

		int walletId = getIntent().getIntExtra("walletId", -1);
		Wallet wallet = WalletStorage.INSTANCE.getById(walletId);

		byte[] dataToBeSigned = getIntent().getByteArrayExtra("dataToBeSigned");
		ReducedTransaction reducedTx = Utils.deserializeReducedTx(dataToBeSigned);
		ArrayList<Integer> inputAddressEip3Indexes = getIntent().getIntegerArrayListExtra("inputAddressEip3Indexes");
		Address changeAddress;
		if (getIntent().hasExtra("changeAddressEip3Index")) {
			changeAddress = wallet.deriveAddress(getIntent().getIntExtra("changeAddressEip3Index", -1));
		} else changeAddress = null;

		Set<Address> inputAddresses = inputAddressEip3Indexes.stream().map(wallet::deriveAddress).collect(Collectors.toUnmodifiableSet());
		long outgoingErg = 0;
		Map<ErgoId, Long> outgoingTokens = new HashMap<>();
		// Outputs not going to this wallet
		List<OutBox> outgoingOutputs = reducedTx.getOutputs().stream().filter(o -> {
			Address address = Address.fromErgoTree(o.getErgoTree(), NetworkType.MAINNET);
			return !inputAddresses.contains(address) && !address.equals(changeAddress);
		}).collect(Collectors.toUnmodifiableList());

		for (OutBox output : outgoingOutputs) {
			Address addr = Address.fromErgoTree(output.getErgoTree(), NetworkType.MAINNET);
			StringBuilder outputInfo = new StringBuilder();
			outputInfo.append("To ").append(addr).append(":\n");
			outputInfo.append(Utils.toFullErg(output.getValue())).append(" ERG\n");
			outgoingErg += output.getValue();
			if (!output.getTokens().isEmpty()) outputInfo.append("\n");
			for (ErgoToken token : output.getTokens()) {
				outgoingTokens.merge(token.getId(), token.getValue(), Long::sum);
				outputInfo.append(token.getId()).append(": ").append(token.getValue());
			}
			binding.outputInfo.append(outputInfo);
			binding.outputInfo.append("\n\n");
		}
		if (outgoingOutputs.isEmpty()) {
			binding.outputInfo.append("This wallet");
		}
		binding.coinsSent.setText(Utils.toFullErg(outgoingErg) + " ERG");
		binding.tokensSent.setText(outgoingTokens.entrySet().stream().map(e -> e.getKey() + ": " + e.getValue()).collect(Collectors.joining("\n")));

		binding.accept.setOnClickListener(v -> {
			char[] password = binding.password.getText().toString().toCharArray();
			Utils.ERGO_CLIENT.execute(ctx -> {
				SignedTransaction signedTx;
				try {
					signedTx = Utils.sign(reducedTx, inputAddressEip3Indexes, wallet.decryptSeed(password));
				} catch (GeneralSecurityException e) {
					throw new RuntimeException(e);
				} catch (IncorrectPasswordException e) {
					MessageDialog.newInstance("Error", "Incorrect password")
							.show(getSupportFragmentManager(), "err_incorrect_pw");
					return null;
				}
				List<byte[]> signatures = signedTx.getSignedInputs().stream()
						.map(SignedInput::getProofBytes)
						.collect(Collectors.toUnmodifiableList());
				Intent result = new Intent();
				result.putExtra("serializedSignatures", Utils.serializeSignatures(signatures));
				setResult(RESULT_SIGNED, result);
				finish();
				return null;
			});
		});
		binding.deny.setOnClickListener(v -> {
			setResult(RESULT_DENIED);
			finish();
		});
	}
}