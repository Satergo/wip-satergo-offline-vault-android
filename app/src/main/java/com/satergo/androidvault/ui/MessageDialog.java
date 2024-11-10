package com.satergo.androidvault.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class MessageDialog extends DialogFragment {

	public static MessageDialog newInstance(String title, String message) {
		Bundle bundle = new Bundle();
		bundle.putString("title", title);
		bundle.putString("message", message);
		MessageDialog dialog = new MessageDialog();
		dialog.setArguments(bundle);
		return dialog;
	}

	public MessageDialog() {
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
		return new AlertDialog.Builder(getContext())
				.setTitle(getArguments().getString("title"))
				.setMessage(getArguments().getString("message"))
				.create();
	}
}
