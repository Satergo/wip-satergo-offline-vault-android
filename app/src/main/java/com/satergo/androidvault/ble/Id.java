package com.satergo.androidvault.ble;

import java.util.UUID;

public interface Id {
	UUID SERVICE = UUID.fromString("fb5c5415-fa44-4c3c-a9bb-9f913f2de7dc");

	interface Characteristic {
		UUID APP_INFO = UUID.fromString("7fb8924e-baf8-4227-a0d9-52e34aef6c4a");

		UUID EXT_PUB_KEY = UUID.fromString("3cd9898b-c684-4407-a830-08f71a40303a");

		UUID REQUEST_TX_SIGNING = UUID.fromString("07ccb789-fdcc-4d77-ba0a-7ed711dc3a6d");
		UUID SIGNATURES = UUID.fromString("408e8d2a-bf45-4c7b-a890-ff82a9840cb3");
	}
}
