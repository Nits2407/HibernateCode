package com.example.nits.hibernateproject;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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

import com.example.nits.hibernateproject.ble.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private ArrayList<BluetoothDevice> mLeDevices;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;
    private ListView list;
    private int deviceNo;
    public final static int REQUEST_ENABLE_BT = 0x1;
    public final static String SERVICE_UUID = "0000fff0-0000-1000-8000-00805f9b34fb";
    public final static UUID TX_UUID = UUID.fromString("0000fff6-0000-1000-8000-00805f9b34fb");
    public final static UUID RX_UUID = UUID.fromString("0000fff7-0000-1000-8000-00805f9b34fb");
    protected UUID Notification_Descriptor_uuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    BluetoothGatt BLE_gatt = null;
    BluetoothGattService BLE_service = null;
    BluetoothGattCharacteristic BLE_characteristic_RX = null;
    BluetoothGattCharacteristic BLE_characteristic_TX = null;
    BluetoothGattDescriptor bluetoothGattDescriptor = null;
    private ProgressDialog dialog = null;
    private final static int MSG_FUN12 = 0x12;


    @SuppressLint("HandlerLeak")
    Handler handler = new Handler() {

        public void handleMessage(Message msg) {

            byte[] data = (byte[]) msg.obj;

            switch (msg.what) {

                case MSG_FUN12:
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(MainActivity.this, "Factory Reset Succesful", Toast.LENGTH_SHORT).show();
                            BLE_gatt.disconnect();
                            BLE_gatt.close();
                            BLE_gatt = null;
                            connectToDevice();
                        }
                    });
                    break;
            }

        }

    };
    boolean isSended = true;
    private boolean isConnect;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_scan);

        list = findViewById(R.id.list);
        mHandler = new Handler();

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

    private void connectToDevice () {
        if (deviceNo < mLeDevices.size()) {
            dialog = new ProgressDialog(this);
            dialog.setTitle("Connecting  with " + mLeDevices.get(deviceNo).getAddress());
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
            this.BLE_gatt =  mLeDevices.get(deviceNo).connectGatt(this, false, new BaseBletoothGattCallBack());
          /*  Log.e("address", "" + mLeDevices.get(deviceNo).getAddress());
            final BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(mLeDevices.get(deviceNo).getAddress());
            Tools.mDevice = new RFLampDevice(this, device);
            Toast.makeText(this, "Connecting...", Toast.LENGTH_SHORT).show();

            Log.e("IsConnect:", Tools.mDevice.isConnected() + "");*/
            deviceNo++;
        } else {
            mLeDeviceListAdapter.clear();
            mLeDeviceListAdapter.notifyDataSetChanged();
            scanLeDevice(true);
        }

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
            mInflator = MainActivity.this.getLayoutInflater();
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
            DeviceScanActivity.ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new DeviceScanActivity.ViewHolder();
                viewHolder.deviceAddress = view.findViewById(R.id.device_address);
                viewHolder.deviceName = view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (DeviceScanActivity.ViewHolder) view.getTag();
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

                            if (device.getName() != null && device.getName().equalsIgnoreCase("GOQii S")) {
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
       /* IntentFilter filter = new IntentFilter();
        filter.addAction(LightBLEService.ACTION_GATT_CONNECTING);
        filter.addAction(LightBLEService.ACTION_GATT_CONNECTED);
        filter.addAction(LightBLEService.ACTION_DATA_AVAILABLE);
        filter.addAction(LightBLEService.ACTION_GATT_SERVICES_DISCOVERED);
        filter.addAction(LightBLEService.ACTION_GATT_DISCONNECTED);
        Log.e("onStart", "Activity_Mapping_MAC");
        registerReceiver(receiver, filter);*/
        super.onStart();
    }

    @Override
    protected void onStop () {
        //unregisterReceiver(receiver);
        super.onStop();

    }

    public class BaseBletoothGattCallBack extends BluetoothGattCallback {
        @Override

        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            // TODO Auto-generated method stub

            byte[] data = characteristic.getValue();
            Log.e("Responsed Data", Util.Show20Hexes(data));

            Message msg = new Message();

            switch (data[0]) {

                case (byte)  0x12:
                    msg.what = MSG_FUN12;
                    break;

            }

            // if (msg.what != MSG_FUN88) {
            msg.obj = data;
            handler.sendMessage(msg);
            // }
            super.onCharacteristicChanged(gatt, characteristic);
            isSended = true;
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            // TODO Auto-generated method stub

            Log.e("onCharacteristicRead()", Util.Show20Hexes(characteristic.getValue()));
            super.onCharacteristicRead(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            // TODO Auto-generated method stub
            Log.e("onCharacteristicWrite()", Util.Show20Hexes(characteristic.getValue()));
            super.onCharacteristicWrite(gatt, characteristic, status);
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

            Message msg = new Message();

            if (newState == BluetoothProfile.STATE_CONNECTING)// Connected state
            {

            } else if (newState == BluetoothProfile.STATE_CONNECTED) {

                Log.e("in onConnectionStateChange()", "Connected");
                BLE_gatt.discoverServices();

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {

                DialogHide();

                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        connectToDevice();
                        Toast.makeText(MainActivity.this, "Disconnect,try again!", Toast.LENGTH_SHORT).show();
                    }
                });

                Log.e("in onConnectionStateChange()", "Disconnected");
                isConnect = false;

            } else if (newState == BluetoothProfile.STATE_DISCONNECTING) {

                Log.e("in onConnectionStateChange()", "Disconnecting");

            } else {

                Log.e("in onConnectionStateChange()", status + "");
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            // TODO Auto-generated method stub
            Log.e("onDescriptorRead()", "");
            super.onDescriptorRead(gatt, descriptor, status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            // TODO Auto-generated method stub
            Log.e("onDescriptorWrite()", "");
            isSended = false;
            super.onDescriptorWrite(gatt, descriptor, status);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            // TODO Auto-generated method stub
            Log.e("onReadRemoteRssi()", "");
            super.onReadRemoteRssi(gatt, rssi, status);
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            // TODO Auto-generated method stub
            Log.e("onReliableWriteCompleted()", "");

            super.onReliableWriteCompleted(gatt, status);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.e("onServicesDiscovered()", "Service Discovered!");
            SearchService(gatt.getServices());
        }
    }

    private void SearchService(List<BluetoothGattService> services) {

        for (BluetoothGattService service : services) {

            if (service.getUuid().toString().equals((SERVICE_UUID.toString()))) {

                if (service.getCharacteristic(RX_UUID) != null && service.getCharacteristic(TX_UUID) != null) {

                    BLE_service = service;
                    BLE_characteristic_RX = service.getCharacteristic(RX_UUID);
                    BLE_characteristic_TX = service.getCharacteristic(TX_UUID);

                    bluetoothGattDescriptor = BLE_characteristic_RX.getDescriptor(Notification_Descriptor_uuid);
                    bluetoothGattDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

                    BLE_gatt.writeDescriptor(bluetoothGattDescriptor);

                    BLE_gatt.setCharacteristicNotification(BLE_characteristic_RX, true);
                    Log.e("on SearchService()", "Notification Enabled");

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.hide();
                            Toast.makeText(MainActivity.this, "Connect successfully", Toast.LENGTH_SHORT).show();
                            isConnect = true;
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run () {
                                    fun12();
                                }
                            },2000);
                        }
                    });

                }
            }
        }
    }
    private void DialogHide() {

        runOnUiThread(new Runnable(

        ) {
            @Override
            public void run() {

                dialog.hide();
            }
        });

    }

    private void fun12() {

        byte[] command = new byte[16];
        command[0] = 0x12;
        command[15] = 0x12;
        WriteDeviceValue(command);

    }

    private void WriteDeviceValue(byte[] data) {

        Log.e("onWriteDeviceValue", Util.Show20Hexes(data));
        this.BLE_characteristic_TX.setValue(data);
        isSended = false;

        if (isConnect)
            this.BLE_gatt.writeCharacteristic(this.BLE_characteristic_TX);

    }

}