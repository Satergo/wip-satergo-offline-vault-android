package com.satergo.androidvault.vault;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class SignJob {

	public enum State {
		PENDING, SIGNED, REJECTED
	}

	private List<byte[]> signatures;
	private State state;

	public final byte[] dataToBeSigned;
	public final List<Integer> inputAddressEip3Indexes;
	public final Integer changeAddressIndex;

	public SignJob(byte[] data, List<Integer> inputAddressEip3Indexes, Integer changeAddressIndex) {
		this.dataToBeSigned = data;
		this.inputAddressEip3Indexes = inputAddressEip3Indexes;
		this.changeAddressIndex = changeAddressIndex;
		this.state = State.PENDING;
	}

	public void setSignatures(List<byte[]> signatures) {
		Objects.requireNonNull(signatures, "signatures");
		this.signatures = signatures;
		this.state = State.SIGNED;
		onStateChanged.forEach(Runnable::run);
	}

	public void reject() {
		this.state = State.REJECTED;
		onStateChanged.forEach(Runnable::run);
	}

	public List<byte[]> getSignatures() {
		if (state != State.SIGNED)
			throw new IllegalStateException("Not signed");
		return Collections.unmodifiableList(signatures);
	}

	public State state() {
		return state;
	}

	private final ArrayList<Runnable> onStateChanged = new ArrayList<>();
	public void onStateChanged(Runnable runnable) {
		onStateChanged.add(runnable);
	}

	@NonNull
	@Override
	public String toString() {
		return String.format("SignJob{data=%s, signatures=%s, state=%s}", Arrays.toString(dataToBeSigned), signatures, state);
	}
}
