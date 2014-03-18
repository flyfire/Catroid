/**
 *  Catroid: An on-device visual programming system for Android devices
 *  Copyright (C) 2010-2013 The Catrobat Team
 *  (<http://developer.catrobat.org/credits>)
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *  
 *  An additional term exception under section 7 of the GNU Affero
 *  General Public License, version 3, is available at
 *  http://developer.catrobat.org/license_additional_term
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU Affero General Public License for more details.
 *  
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.catrobat.catroid.stage;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.parrot.freeflight.receivers.DroneAvailabilityDelegate;
import com.parrot.freeflight.receivers.DroneAvailabilityReceiver;
import com.parrot.freeflight.receivers.DroneBatteryChangedReceiver;
import com.parrot.freeflight.receivers.DroneBatteryChangedReceiverDelegate;
import com.parrot.freeflight.receivers.DroneConnectionChangeReceiverDelegate;
import com.parrot.freeflight.receivers.DroneConnectionChangedReceiver;
import com.parrot.freeflight.receivers.DroneEmergencyChangeReceiver;
import com.parrot.freeflight.receivers.DroneReadyReceiver;
import com.parrot.freeflight.receivers.DroneReadyReceiverDelegate;
import com.parrot.freeflight.service.DroneControlService;
import com.parrot.freeflight.service.intents.DroneStateManager;
import com.parrot.freeflight.tasks.CheckDroneNetworkAvailabilityTask;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.R;
import org.catrobat.catroid.bluetooth.BluetoothManager;
import org.catrobat.catroid.bluetooth.DeviceListActivity;
import org.catrobat.catroid.common.Constants;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.bricks.Brick;
import org.catrobat.catroid.legonxt.LegoNXT;
import org.catrobat.catroid.legonxt.LegoNXTBtCommunicator;
import org.catrobat.catroid.ui.dialogs.CustomAlertDialogBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

@SuppressWarnings("deprecation")
public class PreStageActivity extends Activity implements DroneReadyReceiverDelegate,
		DroneConnectionChangeReceiverDelegate, DroneAvailabilityDelegate
		//implements DroneReadyReceiverDelegate, DroneFlyingStateReceiverDelegate,
		, DroneBatteryChangedReceiverDelegate {
	private static final String TAG = PreStageActivity.class.getSimpleName();

	private static final int REQUEST_ENABLE_BLUETOOTH = 2000;
	private static final int REQUEST_CONNECT_DEVICE = 1000;
	public static final int REQUEST_RESOURCES_INIT = 101;
	public static final int REQUEST_TEXT_TO_SPEECH = 10;

	public static final String STRING_EXTRA_INIT_DRONE = "STRING_EXTRA_INIT_DRONE";

	private int requiredResourceCounter;
	private static LegoNXT legoNXT;
	private ProgressDialog connectingProgressDialog;
	private static TextToSpeech textToSpeech;
	private static OnUtteranceCompletedListenerContainer onUtteranceCompletedListenerContainer;

	private DroneControlService droneControlService = null;
	private BroadcastReceiver droneReadyReceiver = null;
	private BroadcastReceiver droneStateReceiver = null;

	private CheckDroneNetworkAvailabilityTask checkDroneConnectionTask;

	//	private DroneFlyingStateReceiver droneFlyingStateReceiver;

	private boolean autoConnect = false;

	private Intent intent = null;

	private DroneConnectionChangedReceiver droneConnectionChangeReceiver;
	private DroneEmergencyChangeReceiver droneEmergencyReceiver;
	private DroneBatteryChangedReceiver droneBatteryReceiver;

	private int droneBatteryStatus = 0;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate");
		intent = new Intent();

		int requiredResources = getRequiredRessources();
		requiredResourceCounter = Integer.bitCount(requiredResources);

		setContentView(R.layout.activity_prestage);

		if ((requiredResources & Brick.TEXT_TO_SPEECH) > 0) {
			Intent checkIntent = new Intent();
			checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
			startActivityForResult(checkIntent, REQUEST_TEXT_TO_SPEECH);
		}
		if ((requiredResources & Brick.BLUETOOTH_LEGO_NXT) > 0) {
			BluetoothManager bluetoothManager = new BluetoothManager(this);

			int bluetoothState = bluetoothManager.activateBluetooth();
			if (bluetoothState == BluetoothManager.BLUETOOTH_NOT_SUPPORTED) {

				Toast.makeText(PreStageActivity.this, R.string.notification_blueth_err, Toast.LENGTH_LONG).show();
				resourceFailed();
			} else if (bluetoothState == BluetoothManager.BLUETOOTH_ALREADY_ON) {
				if (legoNXT == null) {
					startBluetoothCommunication(true);
				} else {
					resourceInitialized();
				}

			}
		}

		if ((requiredResources & Brick.ARDRONE_SUPPORT) > 0) {
			Log.d(TAG, "Adding drone support!");

			droneReadyReceiver = new DroneReadyReceiver(this);
			droneStateReceiver = new DroneAvailabilityReceiver(this);
			droneBatteryReceiver = new DroneBatteryChangedReceiver(this);
			droneConnectionChangeReceiver = new DroneConnectionChangedReceiver(this);

			Intent startService = new Intent(this, DroneControlService.class);

			Object obj = startService(startService);

			boolean isSuccessful = bindService(new Intent(this, DroneControlService.class),
					this.droneServiceConnection, Context.BIND_AUTO_CREATE);
			// TODO: Abfrage sinnlos, auch wenn die Drone ausgeschalten ist, wird vermeindlich eine Verbindung hergestellt
			if (obj == null || !isSuccessful) {
				Toast.makeText(this, "Connection to the drone failed!", Toast.LENGTH_LONG).show();
				resourceFailed();
			}
		}

		if (requiredResourceCounter == Brick.NO_RESOURCES) {
			startStage();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if (requiredResourceCounter == 0) {
			finish();
		}

		LocalBroadcastManager manager = LocalBroadcastManager.getInstance(getApplicationContext());
		manager.registerReceiver(droneBatteryReceiver, new IntentFilter(
				DroneControlService.DRONE_BATTERY_CHANGED_ACTION));
		manager.registerReceiver(droneReadyReceiver, new IntentFilter(DroneControlService.DRONE_STATE_READY_ACTION));
		manager.registerReceiver(droneConnectionChangeReceiver, new IntentFilter(
				DroneControlService.DRONE_CONNECTION_CHANGED_ACTION));
		manager.registerReceiver(droneStateReceiver, new IntentFilter(DroneStateManager.ACTION_DRONE_STATE_CHANGED));
		checkDroneConnectivity();

	}

	@SuppressLint("NewApi")
	private void checkDroneConnectivity() {
		if (checkDroneConnectionTask != null && checkDroneConnectionTask.getStatus() != Status.FINISHED) {
			checkDroneConnectionTask.cancel(true);
		}

		checkDroneConnectionTask = new CheckDroneNetworkAvailabilityTask() {

			@Override
			protected void onPostExecute(Boolean result) {
				onDroneAvailabilityChanged(result);
			}

		};

		if (Build.VERSION.SDK_INT >= 11) {
			checkDroneConnectionTask.executeOnExecutor(CheckDroneNetworkAvailabilityTask.THREAD_POOL_EXECUTOR, this);
		} else {
			checkDroneConnectionTask.execute(this);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();

		if (droneControlService != null) {
			droneControlService.pause();
		}

		LocalBroadcastManager manager = LocalBroadcastManager.getInstance(getApplicationContext());
		manager.unregisterReceiver(droneReadyReceiver);
		manager.unregisterReceiver(droneConnectionChangeReceiver);
		manager.unregisterReceiver(droneStateReceiver);
		manager.unregisterReceiver(droneBatteryReceiver);

		if (taskRunning(checkDroneConnectionTask)) {
			checkDroneConnectionTask.cancelAnyFtpOperation();
		}
	}

	private boolean taskRunning(AsyncTask<?, ?, ?> checkMediaTask2) {
		if (checkMediaTask2 == null) {
			return false;
		}

		if (checkMediaTask2.getStatus() == Status.FINISHED) {
			return false;
		}

		return true;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (droneControlService != null) {
			unbindService(this.droneServiceConnection);
		}
		Log.d(TAG, "PrestageActivity destroyed");
	}

	//all resources that should be reinitialized with every stage start
	public static void shutdownResources() {
		if (textToSpeech != null) {
			textToSpeech.stop();
			textToSpeech.shutdown();
		}
		if (legoNXT != null) {
			legoNXT.pauseCommunicator();
		}
	}

	//all resources that should not have to be reinitialized every stage start
	public static void shutdownPersistentResources() {
		if (legoNXT != null) {
			legoNXT.destroyCommunicator();
			legoNXT = null;
		}
		deleteSpeechFiles();
	}

	private static void deleteSpeechFiles() {
		File pathToSpeechFiles = new File(Constants.TEXT_TO_SPEECH_TMP_PATH);
		if (pathToSpeechFiles.isDirectory()) {
			for (File file : pathToSpeechFiles.listFiles()) {
				file.delete();
			}
		}
	}

	private void resourceFailed() {
		setResult(RESULT_CANCELED, intent);
		finish();
	}

	private synchronized void resourceInitialized() {
		//Log.i("res", "Resource initialized: " + requiredResourceCounter);

		requiredResourceCounter--;
		if (requiredResourceCounter == 0) {
			Log.d(TAG, "Start Stage");

			startStage();
		}
	}

	public void startStage() {
		setResult(RESULT_OK, intent);
		finish();
	}

	private void startBluetoothCommunication(boolean autoConnect) {
		connectingProgressDialog = ProgressDialog.show(this, "",
				getResources().getString(R.string.connecting_please_wait), true);
		Intent serverIntent = new Intent(this, DeviceListActivity.class);
		serverIntent.putExtra(DeviceListActivity.AUTO_CONNECT, autoConnect);
		this.startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
	}

	private int getRequiredRessources() {
		ArrayList<Sprite> spriteList = (ArrayList<Sprite>) ProjectManager.getInstance().getCurrentProject()
				.getSpriteList();

		int ressources = Brick.NO_RESOURCES;
		for (Sprite sprite : spriteList) {
			ressources |= sprite.getRequiredResources();
		}
		return ressources;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.i("bt", "requestcode " + requestCode + " result code" + resultCode);

		switch (requestCode) {
			case REQUEST_ENABLE_BLUETOOTH:
				switch (resultCode) {
					case Activity.RESULT_OK:
						startBluetoothCommunication(true);
						break;
					case Activity.RESULT_CANCELED:
						Toast.makeText(PreStageActivity.this, R.string.notification_blueth_err, Toast.LENGTH_LONG)
								.show();
						resourceFailed();
						break;
				}
				break;

			case REQUEST_CONNECT_DEVICE:
				switch (resultCode) {
					case Activity.RESULT_OK:
						legoNXT = new LegoNXT(this, recieveHandler);
						String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
						autoConnect = data.getExtras().getBoolean(DeviceListActivity.AUTO_CONNECT);
						legoNXT.startBTCommunicator(address);
						break;

					case Activity.RESULT_CANCELED:
						connectingProgressDialog.dismiss();
						Toast.makeText(PreStageActivity.this, R.string.bt_connection_failed, Toast.LENGTH_LONG).show();
						resourceFailed();
						break;
				}
				break;

			case REQUEST_TEXT_TO_SPEECH:
				if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
					textToSpeech = new TextToSpeech(getApplicationContext(), new OnInitListener() {
						@Override
						public void onInit(int status) {
							onUtteranceCompletedListenerContainer = new OnUtteranceCompletedListenerContainer();
							textToSpeech.setOnUtteranceCompletedListener(onUtteranceCompletedListenerContainer);
							resourceInitialized();
							if (status == TextToSpeech.ERROR) {
								Toast.makeText(PreStageActivity.this,
										"Error occurred while initializing Text-To-Speech engine", Toast.LENGTH_LONG)
										.show();
								resourceFailed();
							}
						}
					});
					if (textToSpeech.isLanguageAvailable(Locale.getDefault()) == TextToSpeech.LANG_MISSING_DATA) {
						Intent installIntent = new Intent();
						installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
						startActivity(installIntent);
						resourceFailed();
					}
				} else {
					AlertDialog.Builder builder = new CustomAlertDialogBuilder(this);
					builder.setMessage(R.string.text_to_speech_engine_not_installed).setCancelable(false)
							.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int id) {
									Intent installIntent = new Intent();
									installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
									startActivity(installIntent);
									resourceFailed();
								}
							}).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int id) {
									dialog.cancel();
									resourceFailed();
								}
							});
					AlertDialog alert = builder.create();
					alert.show();
				}
				break;
			default:
				resourceFailed();
				break;
		}
	}

	public static void textToSpeech(String text, File speechFile, OnUtteranceCompletedListener listener,
			HashMap<String, String> speakParameter) {
		if (text == null) {
			text = "";
		}

		if (onUtteranceCompletedListenerContainer.addOnUtteranceCompletedListener(speechFile, listener,
				speakParameter.get(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID))) {
			int status = textToSpeech.synthesizeToFile(text, speakParameter, speechFile.getAbsolutePath());
			if (status == TextToSpeech.ERROR) {
				Log.e(TAG, "File synthesizing failed");
			}
		}
	}

	//messages from Lego NXT device can be handled here
	// TODO should be fixed - could lead to problems
	@SuppressLint("HandlerLeak")
	final Handler recieveHandler = new Handler() {
		@Override
		public void handleMessage(Message myMessage) {

			Log.i("bt", "message" + myMessage.getData().getInt("message"));
			switch (myMessage.getData().getInt("message")) {
				case LegoNXTBtCommunicator.STATE_CONNECTED:
					//autoConnect = false;
					connectingProgressDialog.dismiss();
					resourceInitialized();
					break;
				case LegoNXTBtCommunicator.STATE_CONNECTERROR:
					Toast.makeText(PreStageActivity.this, R.string.bt_connection_failed, Toast.LENGTH_SHORT).show();
					connectingProgressDialog.dismiss();
					legoNXT.destroyCommunicator();
					legoNXT = null;
					if (autoConnect) {
						startBluetoothCommunication(false);
					} else {
						resourceFailed();
					}
					break;
			}
		}
	};

	private void onDroneServiceConnected(IBinder service) {
		Log.d(TAG, "onDroneServiceConnected");
		droneControlService = ((DroneControlService.LocalBinder) service).getService();

		droneControlService.resume();
		droneControlService.requestDroneStatus();
	}

	private ServiceConnection droneServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			onDroneServiceConnected(service);
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			droneControlService = null; //nothing else to do here

		}
	};

	public static Intent addDroneSupportToIntent(Intent oldIntent, Intent newIntent) {
		if (newIntent == null || newIntent == null) {
			return null;
		}
		Boolean isDroneRequired = oldIntent.getBooleanExtra(STRING_EXTRA_INIT_DRONE, false);
		Log.d(TAG, "Extra STRING_EXTRA_INIT_DRONE=" + isDroneRequired.toString());
		newIntent.putExtra(STRING_EXTRA_INIT_DRONE, isDroneRequired);
		return newIntent;
	}

	@Override
	public void onDroneReady() {
		Log.d(TAG, "onDroneReady -> check battery -> go to stage");
		//droneControlService.
		if (droneBatteryStatus < 20) {
			showErrorDialogWhenDroneIsNotAvailable(this, R.string.error_drone_low_battery);
			return;
		}
		intent.putExtra(STRING_EXTRA_INIT_DRONE, true);
		intent.putExtra("USE_SOFTWARE_RENDERING", false); //TODO: Drone: Hand over to Stage Activity
		intent.putExtra("FORCE_COMBINED_CONTROL_MODE", false); //TODO: Drone: Hand over to Stage Activity
		//
		resourceInitialized();
	}

	@Override
	public void onDroneConnected() {
		// We still waiting for onDroneReady event

		Log.d(TAG, "onDroneConnected, requesting Config update and wait for drone ready.");
		droneControlService.requestConfigUpdate();
	}

	@Override
	public void onDroneDisconnected() {
		//nothing to do
	}

	@Override
	public void onDroneAvailabilityChanged(boolean isDroneOnNetwork) {
		// Here we know that the drone is on the network
		Log.d(TAG, "Drone availability  = " + isDroneOnNetwork);
		if (isDroneOnNetwork) {
			//TODO: connect, set config, init video, ...
		} else {
			showErrorDialogWhenDroneIsNotAvailable(this, R.string.error_no_drone_connected);
		}

	}

	public static void showErrorDialogWhenDroneIsNotAvailable(final PreStageActivity context, int errorMessage) {
		Builder builder = new CustomAlertDialogBuilder(context);

		builder.setTitle(R.string.error);
		builder.setCancelable(false);
		builder.setMessage(errorMessage);
		builder.setNeutralButton(R.string.close, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				//TODO: shut down nicely all resources and go back to the activity we came from
				context.resourceFailed();
			}
		});
		builder.show();
	}

	@Override
	public void onDroneBatteryChanged(int value) {
		Log.d(TAG, "Drone Battery Status =" + Integer.toString(value));
		droneBatteryStatus = value;

	}
}
