/*
 * Copyright (c) 2018, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package no.nordicsemi.android.blinky.profile;

import static no.nordicsemi.android.blinky.profile.data.Command.BREAK_BOND;
import static no.nordicsemi.android.blinky.profile.data.Command.COMPLETE_BOND;
import static no.nordicsemi.android.blinky.profile.data.Command.MEASURE_ECG_REALTIME;
import static no.nordicsemi.android.blinky.profile.data.Command.MEASURE_HRV_REALTIME;
import static no.nordicsemi.android.blinky.profile.data.Command.SYNC_FAST;
import static no.nordicsemi.android.blinky.profile.data.Command.KEEP_CONNECTION;
import static no.nordicsemi.android.blinky.profile.data.Command.REQUEST_BOND;
import static no.nordicsemi.android.blinky.profile.data.Command.SYNC_TIME_AGE;
import static no.nordicsemi.android.blinky.profile.data.Command.VALIDATE_BOND;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import no.nordicsemi.android.ble.data.Data;
import no.nordicsemi.android.ble.livedata.ObservableBleManager;
import no.nordicsemi.android.blinky.BuildConfig;
import no.nordicsemi.android.blinky.profile.callback.BlinkyButtonDataCallback;
import no.nordicsemi.android.blinky.profile.callback.BlinkyLedDataCallback;
import no.nordicsemi.android.blinky.profile.data.BlinkyLED;
import no.nordicsemi.android.blinky.profile.data.Command;
import no.nordicsemi.android.log.LogContract;
import no.nordicsemi.android.log.LogSession;
import no.nordicsemi.android.log.Logger;

public class BlinkyManager extends ObservableBleManager {

	//	/** Nordic Blinky Service UUID. */
	//	public final static UUID LBS_UUID_SERVICE = UUID.fromString("00001523-1212-efde-1523-785feabcd123");
	//	/** BUTTON characteristic UUID. */
	//	private final static UUID LBS_UUID_BUTTON_CHAR = UUID.fromString("00001524-1212-efde-1523-785feabcd123");
	//	/** LED characteristic UUID. */
	//	private final static UUID LBS_UUID_LED_CHAR = UUID.fromString("00001525-1212-efde-1523-785feabcd123");

	//	public static final UUID UART_SERVICE_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
	//	private static final UUID UART_RX_CHARACTERISTIC_UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E");
	//	private static final UUID UART_TX_CHARACTERISTIC_UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E");

	//	private static final UUID BATTERY_SERVICE_UUID = UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb");
	//	private static final UUID BATTERY_LEVEL_CHARACTERISTIC_UUID = UUID.fromString("00002A19-0000-1000-8000-00805f9b34fb");

	// Nordic UART Service
	public static final UUID NORDIC_UART_SERVICE = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
	// RX Characteristic
	private static final UUID RX_CHARACTERISTIC = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E");
	// TX Characteristic
	private static final UUID TX_CHARACTERISTIC = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E");

	public Object storeddata;

	private final MutableLiveData<Boolean> ledState = new MutableLiveData<>();
	private final MutableLiveData<Boolean> buttonState = new MutableLiveData<>();

	private BluetoothGattCharacteristic buttonCharacteristic, ledCharacteristic;
	private LogSession logSession;
	private boolean supported;
	private boolean ledOn;
	private static final String TAG = "BlinkyManager";
	private List<Short> shortList = new ArrayList<>();
	public boolean isSupported(){return supported;}

	public BlinkyManager(@NonNull final Context context) {
		super(context);
	}

	public final LiveData<Boolean> getLedState() {
		return ledState;
	}

	public final LiveData<Boolean> getButtonState() {
		return buttonState;
	}

	@NonNull
	@Override
	protected BleManagerGattCallback getGattCallback() {

		return new BlinkyBleManagerGattCallback();
	}

	/**
	 * Sets the log session to be used for low level logging.
	 * @param session the session, or null, if nRF Logger is not installed.
	 */
	public void setLogger(@Nullable final LogSession session) {
		logSession = session;
	}

	@Override
	public void log(final int priority, @NonNull final String message) {
		if (BuildConfig.DEBUG) {
			Log.println(priority, "BlinkyManager", message);
		}
		// The priority is a Log.X constant, while the Logger accepts it's log levels.
		Logger.log(logSession, LogContract.Log.Level.fromPriority(priority), message);
	}

	@Override
	protected boolean shouldClearCacheWhenDisconnected() {
		return !supported;
	}

	/**
	 * The Button callback will be notified when a notification from Button characteristic
	 * has been received, or its data was read.
	 * <p>
	 * If the data received are valid (single byte equal to 0x00 or 0x01), the
	 * {@link BlinkyButtonDataCallback#onButtonStateChanged} will be called.
	 * Otherwise, the {@link BlinkyButtonDataCallback#onInvalidDataReceived(BluetoothDevice, Data)}
	 * will be called with the data received.
	 */

	private	final BlinkyButtonDataCallback buttonCallback = new BlinkyButtonDataCallback() {

		// if the notification date is valid, execute this function
		@RequiresApi(api = Build.VERSION_CODES.O)
		@Override
		public void onButtonStateChanged(@NonNull final BluetoothDevice device,
										 final boolean pressed, Data data) {
			//			Log.d(TAG, data.toString());

			byte[] ba = data.getValue();
			int[] x = new int[ba.length];
			Log.d(TAG, Integer.toString(ba.length));
			for(int i = 0; i<ba.length;i++){
				if (ba[i]<0)
					x[i] = 256+ba[i];
				else
					x[i] = ba[i];
			}

			Log.d(TAG, Arrays.toString(x));
			//			log(LogContract.Log.Level.APPLICATION, "Button " + (pressed ? "pressed" : "released"));
			//			buttonState.setValue(pressed);
			Object obj =  parseResponse(x);
			if(obj != null){
				if (obj instanceof Integer){
					switch ((Integer)(obj)){
						case(26):
							// send shortlist to draw
							storeddata = shortList;
							buttonState.setValue(true);
							break;
						default:
							break;
					}

				} else if (obj instanceof List)
				{
					//					List<Short> newList = Stream.concat(shortList.stream(), ((List<Short>) obj).stream()).collect(Collectors.toList());
					//					shortList = newList;
					shortList.addAll((List)(obj));
				}
			}

		}

		// if the notification date is invalid, execute this function
		@Override
		public void onInvalidDataReceived(@NonNull final BluetoothDevice device,
										  @NonNull final Data data) {
				//			log(Log.WARN, "Invalid data received: " + data);
		}

	};

	/**
	 * The LED callback will be notified when the LED state was read or sent to the target device.
	 * <p>
	 * This callback implements both {@link no.nordicsemi.android.ble.callback.DataReceivedCallback}
	 * and {@link no.nordicsemi.android.ble.callback.DataSentCallback} and calls the same
	 * method on success.
	 * <p>
	 * If the data received were invalid, the
	 * {@link BlinkyLedDataCallback#onInvalidDataReceived(BluetoothDevice, Data)} will be
	 * called.
	 */
	private final BlinkyLedDataCallback ledCallback = new BlinkyLedDataCallback() {
		@Override
		public void onLedStateChanged(@NonNull final BluetoothDevice device,
									  final boolean on) {
			ledOn = on;
			log(LogContract.Log.Level.APPLICATION, "LED " + (on ? "ON" : "OFF"));
			ledState.setValue(on);
		}

		@Override
		public void onInvalidDataReceived(@NonNull final BluetoothDevice device,
										  @NonNull final Data data) {
			// Data can only invalid if we read them. We assume the app always sends correct data.
				//			log(Log.WARN, "Invalid data received: " + data);
		}
	};

	/**
	 * BluetoothGatt callbacks object.
	 */
	private class BlinkyBleManagerGattCallback extends BleManagerGattCallback {
		@Override
		protected void initialize() {
			setNotificationCallback(buttonCharacteristic).with(buttonCallback);
			readCharacteristic(ledCharacteristic).with(ledCallback).enqueue();
			readCharacteristic(buttonCharacteristic).with(buttonCallback).enqueue();
			enableNotifications(buttonCharacteristic).enqueue();
		}

		@Override
		public boolean isRequiredServiceSupported(@NonNull final BluetoothGatt gatt) {
			final BluetoothGattService service = gatt.getService(NORDIC_UART_SERVICE);
			if (service != null) {
				buttonCharacteristic = service.getCharacteristic(TX_CHARACTERISTIC);
				ledCharacteristic = service.getCharacteristic(RX_CHARACTERISTIC);
			}

			boolean writeRequest = false;
			if (ledCharacteristic != null) {
				final int ledProperties = ledCharacteristic.getProperties();
				final int buttonProperties = buttonCharacteristic.getProperties();
				writeRequest = (ledProperties & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0;
			}

			supported = buttonCharacteristic != null && ledCharacteristic != null && writeRequest;
			return supported;
		}

		@Override
		protected void onServicesInvalidated() {
			buttonCharacteristic = null;
			ledCharacteristic = null;
		}
	}

	/**
	 * Sends a request to the device to turn the LED on or off.
	 *
	 * @param on true to turn the LED on, false to turn it off.
	 */
	@RequiresApi(api = Build.VERSION_CODES.O)
	public void turnLed(final boolean on) {
		// Are we connected?
		if (ledCharacteristic == null)
			return;

		// No need to change?
		if (ledOn == on)
			return;

		log(Log.VERBOSE, "Turning LED " + (on ? "ON" : "OFF") + "...");
		log(Log.DEBUG, BlinkyLED.write(KEEP_CONNECTION).toString());
		writeCharacteristic(
				ledCharacteristic,
				BlinkyLED.write(KEEP_CONNECTION),
				//				BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
				BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
		).with(ledCallback).enqueue();
	}

	@RequiresApi(api = Build.VERSION_CODES.O)
	public void generateRequest(Command command){
		// Are we connected?
		if (ledCharacteristic == null)
			return;
		log(Log.VERBOSE, "generateKeepConnectionRequest");
		writeCharacteristic(
				ledCharacteristic,
				BlinkyLED.write(command),
				//				BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
				BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
		).with(ledCallback).enqueue();

	}

	@RequiresApi(api = Build.VERSION_CODES.O)
	private Object parseResponse(int[] x) {
		// Are we connected?
		if (ledCharacteristic == null)
			return null;
		int l = x.length;
		int cmd = x[0];
		switch (cmd){
			case(119):
				int state = x[1];
				switch (state){
					case (24):
						Log.d(TAG, "read: start drawing");
						return (Integer)(state);
					case (25):
						Log.i(TAG, "read: data");
						// data size = l
						int datasize = x[2];
						// TODO: output heartrate
						int heartrate = x[4];
						// discard first five byte of data
						byte[] ba = new byte[datasize-5];
						for(int i = 5; i<ba.length;i++){
							ba[i-5] = (byte)x[i];
						}
						ByteBuffer bb = ByteBuffer.wrap(ba);
						bb.order( ByteOrder.LITTLE_ENDIAN);
						List<Short> shortList = new ArrayList<>();
						while( bb.hasRemaining()) {
							shortList.add(bb.getShort());
						}
						Log.d(TAG, shortList.toString());
						return shortList;
					case (26):
						Log.d(TAG, "read: end drawing");
						return (Integer)(state);
					default:
						break;
				}
				break;
			case(106):
				break;
			default:
				return null;
		}
		return null;
	}



//		Object obj = read(x);
//		if (obj != null)
//		{
//			if(obj instanceof List)
//			{
//				List<Short> newList = Stream.concat(shortList.stream(), ((List<Short>) obj).stream())
//						.collect(Collectors.toList());
//				shortList = newList;
//			}
//		}

//	@RequiresApi(api = Build.VERSION_CODES.N)
//	public static Object read(int[] x) {
//	}





}
