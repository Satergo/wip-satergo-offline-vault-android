package com.satergo.androidvault.ui.settings;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import com.satergo.androidvault.R;

public class SettingsFragment extends PreferenceFragmentCompat {
	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
		setPreferencesFromResource(R.xml.root_preferences, rootKey);
	}
}
