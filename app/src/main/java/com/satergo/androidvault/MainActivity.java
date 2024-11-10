package com.satergo.androidvault;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceManager;

import com.satergo.androidvault.databinding.ActivityMainBinding;
import com.satergo.androidvault.storage.WalletStorage;
import com.satergo.androidvault.ui.MessageDialog;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

	private ActivityMainBinding binding;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		binding = ActivityMainBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		BottomNavigationView navView = findViewById(R.id.nav_view);
		// Passing each menu ID as a set of Ids because each
		// menu should be considered as top level destinations.
		AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
				R.id.navigation_connection, R.id.navigation_wallets, R.id.navigation_settings)
				.build();
		NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
		NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
		NavigationUI.setupWithNavController(binding.navView, navController);

		navView.setSelectedItemId(R.id.navigation_wallets);

		if (WalletStorage.INSTANCE == null) {
			try {
				WalletStorage.INSTANCE = new WalletStorage(getApplicationContext());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		if (!prefs.getBoolean("warned", false)) {
			MessageDialog.newInstance("Disclaimer", "I am not responsible if something happens to your coins or tokens. Uninstall the app if you do not accept this.")
					.show(getSupportFragmentManager(), "warn");
			prefs.edit().putBoolean("warned", true).apply();
		}
	}
}