package com.satergo.androidvault.vault;

import java.util.concurrent.CompletableFuture;

public interface VaultWorker {

	CompletableFuture<byte[]> getExtendedPublicKey();
	void signJobReceived(SignJob job);
}
