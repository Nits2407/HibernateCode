package com.example.nits.hibernateproject.ble;

public class Command_Gernerator {

	public final static int SETTING_DATETIME = 0x1;

	public final static int ERROR_SETTING_DATETIME = SETTING_DATETIME + 0x80;

	public final static int GETTING_DATETIME = 0x41;

	public final static int ERROR_GETTING_DATETIME = GETTING_DATETIME + 0x80;

	public final static int CONFIGURE_MAC_TABLE = 0x10;

	public final static int ERROR_CONFIGURE_MAC_TABLE = CONFIGURE_MAC_TABLE + 0x80;

	public final static int GETTING_MAC_TABLE = 0x11;

	public final static int ERROR_GETTING_MAC_TABLE = GETTING_MAC_TABLE + 0x80;

	public final static int GETTING_CONNECTED_DEVICE_MAC = 0x22;

	public final static int ERROR_GETTING_CONNECTED_DEVICE_MAC = GETTING_CONNECTED_DEVICE_MAC + 0x80;

	public final static int GETTING_FIRMWARE_VERSION = 0x27;

	public final static int ERROR_GETTING_FIRMWARE_VERSION = GETTING_FIRMWARE_VERSION + 0x80;

	public static void calculateCRC8_16(byte[] data) {

		for (int i = 0; i < 15; i++) {

			data[15] += data[i];

		}

	}

	public static void calculateCRC8_20(byte[] data) {

		for (int i = 0; i < 19; i++) {

			data[19] += data[i];

		}

	}
}
