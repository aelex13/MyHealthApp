package com.jcheed06.myhealthapp.measurement;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

import com.jcheed06.myhealthapp.R;
import com.jcheed06.myhealthapp.Registry;
import com.jcheed06.myhealthapp.R.id;
import com.jcheed06.myhealthapp.R.layout;
import com.jcheed06.myhealthapp.measurement.domain.ECGMeasurement;
import com.jcheed06.myhealthapp.measurement.domain.PressureMeasurement;
import com.jcheed06.myhealthapp.measurement.domain.PulseMeasurement;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

public class BluetoothActivity extends Activity {

	private PulseMeasurement pulseMeasurement;
	private PressureMeasurement pressureMeasurement;
	private ECGMeasurement ecgMeasurement;
	
	
	private final static int REQUEST_ENABLE_BT = 1;
	BluetoothAdapter btAdapter;
	BluetoothDevice device;
	ArrayList<BluetoothDevice> devices;
	BroadcastReceiver receiver;
	Handler handler = new Handler();
	
	LinearLayout connectedLayout;
	ListView devicesList;
	Button discoverBtDevices;
	Button buttonStopDevices;
	
	ArrayAdapter aa;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bluetooth);

		devices = new ArrayList<BluetoothDevice>();
		connectedLayout = (LinearLayout) findViewById(R.id.layoutConnected);

		aa = new ArrayAdapter(this, android.R.layout.simple_list_item_1);

		devicesList = (ListView) findViewById(R.id.list_devices);
		devicesList.setClickable(true);
		devicesList.setAdapter(aa);

		devicesList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View view,
					int position, long arg3) {
				btAdapter.cancelDiscovery();
				device = devices.get(position);
				new ConnectThread(device).start();
			}

		});

		btAdapter = BluetoothAdapter.getDefaultAdapter();
		if (btAdapter == null) {
			AlertDialog.Builder builder = new AlertDialog.Builder(
					BluetoothActivity.this);
			builder.setMessage("Your device doesn't support bluetooth.")
					.setPositiveButton("OK",
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.cancel();
									finish();
								}
							});
			builder.setCancelable(false);
			builder.create();
			builder.show();
		}

		if (!btAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(btAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}

		receiver = new BroadcastReceiver() {
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				if (BluetoothDevice.ACTION_FOUND.equals(action)) {
					device = intent
							.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
					aa.add(device.getName() + "\n" + device.getAddress());
					devices.add(device);
				}
			}
		};

		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		registerReceiver(receiver, filter);

		discoverConnections();
		
		buttonStopDevices = (Button) findViewById(R.id.button_stop_listening);
		buttonStopDevices.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = getIntent();
			    finish();
			    startActivity(intent);
			}
		});

	}

	@Override
	protected void onStop() {
		super.onStop();
		
		try {
		unregisterReceiver(receiver);
		} catch (IllegalArgumentException e) {
			Log.e("myhealth","Unregistering the receiver!");
		}
	}

	private void discoverConnections() {
		discoverBtDevices = (Button) findViewById(R.id.button_discover_bt);
		discoverBtDevices.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Log.e("health", "Responding to button!");
				if (btAdapter.isDiscovering()) {
					btAdapter.cancelDiscovery();
					Toast.makeText(BluetoothActivity.this,
							"Stopped discovering.", Toast.LENGTH_LONG).show();
				} else {
					btAdapter.startDiscovery();
					Toast.makeText(BluetoothActivity.this,
							"Started discovering.", Toast.LENGTH_LONG).show();
				}
			}
		});
	}

	private class SendMeasurement extends AsyncTask<Void, Void, Boolean> {
		
		@Override
		protected Boolean doInBackground(Void... params) {
			HttpClient httpclient = new DefaultHttpClient();
			HttpPost httppost = new HttpPost(Registry.BASE_API_URL
					+ Registry.SEND_MEASUREMENT_COMMAND);
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			return null;
		}

	}

	private class ConnectThread extends Thread {
		private final BluetoothSocket socket;
		private final BluetoothDevice device;
		private final UUID deviceId = UUID
				.fromString("889a38c0-251b-11e3-8224-0800200c9a66");

		public ConnectThread(BluetoothDevice device) {
			Log.e("health", "I got in the ConnectThread");
			BluetoothSocket tmp = null;
			this.device = device;
			try {
				tmp = device.createRfcommSocketToServiceRecord(deviceId);
				Log.e("Health", "Just did createRfcommSocketToService");
			} catch (IOException e) {
			}
			socket = tmp;
		}

		public void run() {
			btAdapter.cancelDiscovery();

			try {
				socket.connect();
				connectedView();
				new ConnectedThread(socket).start();
			} catch (IOException e) {
				try {
					socket.close();
				} catch (IOException e1) {
					return;
				}
				// manageConnectedSocket(socket);
			}

		}

		public void cancel() {
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	private class ConnectedThread extends Thread {
		private final BluetoothSocket socket;
		private final InputStream is;
		private final OutputStream os;

		public ConnectedThread(BluetoothSocket socket) {
			this.socket = socket;
			InputStream tmpis = null;
			OutputStream tmpos = null;
			try {
				tmpis = socket.getInputStream();
				tmpos = socket.getOutputStream();
			} catch (IOException e) {
				Log.e("health", "Couldn't create input or outputstream.");
				e.printStackTrace();
			}
			is = tmpis;
			os = tmpos;
		}

		public void run() {
			Log.e("health", "ik zit in run van ConnectedThread.");
			while (true) {
				int available = 0;
				try {
					available = is.available();
					if (available > 0) {
						byte[] buffer;
						buffer = new byte[available];
						is.read(buffer);
						String message = new String(buffer);
						Log.e("health", "Message is: " + message);
						String[] partsOfMessage = message.split(";");
						if (partsOfMessage[0].equals("pulse")) {
							makeToastAtMeasurementReceived("Pulse measurement received: " + message);
							startPulseMeasurement(partsOfMessage[1]);
						} else if (partsOfMessage[0].equals("ecg")) {
							makeToastAtMeasurementReceived("ECG measurement received");
							startECGMeasurement(partsOfMessage);
						} else if (partsOfMessage[0].equals("bloodpressure")) {
							makeToastAtMeasurementReceived("Bloodpressure measurement received");
							startBloodPressureMeasurement(partsOfMessage);
						}
						Log.e("received", "Message received! " + message);
					}
				} catch (IOException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}
			}
		}

		public void write(byte[] bytes) {
			try {
				os.write(bytes);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		public void cancel() {
			try {
				socket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private void startPulseMeasurement(final String message) {
		BluetoothActivity.this.runOnUiThread(new Runnable() {

	        public void run() {
	        	
	    		pulseMeasurement = new PulseMeasurement();
	    		Integer pulse = Integer.parseInt(message);
	    		pulseMeasurement.setBPM(pulse);
	    		new SendMeasurementTask(pulseMeasurement);
	        }
	    });
		
	}

	private void startECGMeasurement(final String[] message) {
		BluetoothActivity.this.runOnUiThread(new Runnable() {

	        public void run() {
	        	ecgMeasurement = new ECGMeasurement();
	        	ecgMeasurement.setPrinterval(Integer.parseInt(message[1]));
	        	ecgMeasurement.setPrsegment(Integer.parseInt(message[2]));
	    		ecgMeasurement.setQrscomplex(Integer.parseInt(message[3]));
	    		ecgMeasurement.setStsegment(Integer.parseInt(message[4]));
	    		ecgMeasurement.setQtinterval(Integer.parseInt(message[5]));
	    		ecgMeasurement.setQtrough(Integer.parseInt(message[6]));
	    		ecgMeasurement.setRpeak(Integer.parseInt(message[7]));
	    		ecgMeasurement.setStrough(Integer.parseInt(message[8]));
	    		ecgMeasurement.setTpeak(Integer.parseInt(message[9]));
	    		ecgMeasurement.setPpeak(Integer.parseInt(message[10]));
	    		new SendMeasurementTask(ecgMeasurement);
	        }
	    });
	}

	private void startBloodPressureMeasurement(final String[] message) {
		BluetoothActivity.this.runOnUiThread(new Runnable() {

	        public void run() {
	    		pressureMeasurement = new PressureMeasurement();
	    		pressureMeasurement.setHypoTension(Integer.parseInt(message[1]));
	    		pressureMeasurement.setHyperTension(Integer.parseInt(message[2]));
	    		new SendMeasurementTask(pressureMeasurement);
	        }
	    });
	}
	
	private void makeToastAtMeasurementReceived(final String message) {
		BluetoothActivity.this.runOnUiThread(new Runnable() {

	        public void run() {
	        	Toast.makeText(BluetoothActivity.this, message, Toast.LENGTH_SHORT).show();
	        }
	    });
	}

	private void connectedView() {
		BluetoothActivity.this.runOnUiThread(new Runnable() {

	        public void run() {
	        	devicesList.setVisibility(View.INVISIBLE);
	        	discoverBtDevices.setVisibility(View.INVISIBLE);
	        	connectedLayout.setVisibility(View.VISIBLE);
	        }
	    });
	}
}
