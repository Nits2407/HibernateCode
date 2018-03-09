package com.example.nits.hibernateproject;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.nits.hibernateproject.ble.Command_Gernerator;
import com.example.nits.hibernateproject.ble.LightBLEService;
import com.example.nits.hibernateproject.ble.RFLampDevice;
import com.example.nits.hibernateproject.ble.Tools;
import com.example.nits.hibernateproject.ble.Util;

import java.util.ArrayList;
import java.util.Calendar;

public class DeviceScanActivity extends AppCompatActivity {

    private ArrayList<BluetoothDevice> mLeDevices;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;
    private ListView list;
    private int deviceNo;

    @Override
    public void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_scan);

        list = findViewById(R.id.list);
        mHandler = new Handler();

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }
        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("This app needs location access");
            builder.setMessage("Please grant location access so this app can detect peripherals.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss (DialogInterface dialog) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                }
            });
            builder.show();
        }

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
        // Initializes list view adapter.
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        list.setAdapter(mLeDeviceListAdapter);
        scanLeDevice(true);
    }

    @Override
    public boolean onCreateOptionsMenu (Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                mLeDeviceListAdapter.clear();
                scanLeDevice(true);
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                break;
        }
        return true;
    }

    @Override
    protected void onResume () {
        super.onResume();

    }

    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause () {
        super.onPause();
        scanLeDevice(false);
        mLeDeviceListAdapter.clear();
    }

    private void connectToDevice () {
        if (deviceNo < mLeDevices.size()) {
            Log.e("address", "" + mLeDevices.get(deviceNo).getAddress());
            final BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(mLeDevices.get(deviceNo).getAddress());
            Tools.mDevice = new RFLampDevice(this, device);
            Toast.makeText(this, "Connecting...", Toast.LENGTH_SHORT).show();

            Log.e("IsConnect:", Tools.mDevice.isConnected() + "");
            deviceNo++;
        } else
            scanLeDevice(true);

    }

    private void scanLeDevice (final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run () {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    invalidateOptionsMenu();
                    connectDeviceOneByOne();
                }
            }, SCAN_PERIOD);
            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }

    private void connectDeviceOneByOne () {
        if (mLeDevices.size() > 0) {
            Log.e("size", "" + mLeDevices.size());
            deviceNo = 0;
            connectToDevice();
        }

    }

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private LayoutInflater mInflator;

        public LeDeviceListAdapter () {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = DeviceScanActivity.this.getLayoutInflater();
        }

        public void addDevice (BluetoothDevice device) {
            if (!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        public BluetoothDevice getDevice (int position) {
            return mLeDevices.get(position);
        }

        public void clear () {
            mLeDevices.clear();
        }

        @Override
        public int getCount () {
            return mLeDevices.size();
        }

        @Override
        public Object getItem (int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId (int i) {
            return i;
        }

        @Override
        public View getView (int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = view.findViewById(R.id.device_address);
                viewHolder.deviceName = view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan (final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run () {
                            /*if (device.getName() != null && device.getName().equalsIgnoreCase("GOQii S")) {
                                mLeDeviceListAdapter.addDevice(device);
                                mLeDeviceListAdapter.notifyDataSetChanged();
                            }*/

                            if (device.getName() != null && device.getName().equalsIgnoreCase("GOQii S") && (device.getAddress().equalsIgnoreCase("F9:8A:9F:BC:14:91") ||
                                    device.getAddress().equalsIgnoreCase("EC:33:AD:C8:8B:89"))) {
                                mLeDeviceListAdapter.addDevice(device);
                                mLeDeviceListAdapter.notifyDataSetChanged();
                            }
                        }
                    });
                }
            };

    static class ViewHolder {
        TextView deviceName;

        TextView deviceAddress;
    }

    @Override
    protected void onStart () {
        IntentFilter filter = new IntentFilter();
        filter.addAction(LightBLEService.ACTION_GATT_CONNECTING);
        filter.addAction(LightBLEService.ACTION_GATT_CONNECTED);
        filter.addAction(LightBLEService.ACTION_DATA_AVAILABLE);
        filter.addAction(LightBLEService.ACTION_GATT_SERVICES_DISCOVERED);
        filter.addAction(LightBLEService.ACTION_GATT_DISCONNECTED);
        Log.e("onStart", "Activity_Mapping_MAC");
        registerReceiver(receiver, filter);
        super.onStart();
    }

    @Override
    protected void onStop () {
        unregisterReceiver(receiver);
        super.onStop();

    }

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive (Context context, Intent intent) {
            if (intent.getAction() == LightBLEService.ACTION_GATT_SERVICES_DISCOVERED) {
                Toast.makeText(context, "Connected", Toast.LENGTH_SHORT).show();
                whatTime();
            } else if (intent.getAction() == LightBLEService.ACTION_GATT_DISCONNECTED) {
                connectToDevice();
                Toast.makeText(DeviceScanActivity.this, "Connecting faild", Toast.LENGTH_SHORT).show();
            } else if (intent.getAction() == LightBLEService.ACTION_DATA_AVAILABLE) {

                byte[] values = intent.getByteArrayExtra(LightBLEService.EXTRA_DATA);
                byte Command_ID = values[0];

                if (Command_ID == 0x41) {
                    getTime(values);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run () {
                            if (Tools.mDevice != null && Tools.mDevice.isConnected()) {
                                Tools.mDevice.closeDevice();
                                Tools.mDevice = null;
                                Log.e("called", "called");
                                connectToDevice();
                            }
                        }
                    }, 2000);
                }

            }
        }
    };

    private void whatTime () {
        byte[] command = new byte[16];

        Calendar c = Calendar.getInstance();
        command[0] = Command_Gernerator.GETTING_DATETIME;
        Command_Gernerator.calculateCRC8_16(command);

        Tools.mDevice.sendUpdate(command);
    }

    private void getTime (byte[] value) {
        try {

            String dateTimeStr = "";
            int year = 2000 + Util.ConvertBCD2Decimal(value[1]);
            int month = Util.ConvertBCD2Decimal(value[2]);
            int day = Util.ConvertBCD2Decimal(value[3]);
            int min = Util.ConvertBCD2Decimal(value[4]);
            int sec = Util.ConvertBCD2Decimal(value[5]);

            dateTimeStr = "Device's DateTime is:" + year + "." + month + "." + day + " " + min + ":" + sec;

            Toast.makeText(this, dateTimeStr, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
