package com.kstechnologies.NanoScan;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.kstechnologies.nirscannanolibrary.KSTNanoSDK.NanoDevice;
import com.kstechnologies.nirscannanolibrary.SettingsManager;
import com.kstechnologies.nirscannanolibrary.SettingsManager.SharedPreferencesKeys;
import java.util.ArrayList;
import java.util.Iterator;

public class ScanActivity extends Activity {
    private static final String DEVICE_NAME = "NIRScanNano";
    /* access modifiers changed from: private */
    public static Context mContext;
    /* access modifiers changed from: private */
    public AlertDialog alertDialog;
    private BluetoothAdapter mBluetoothAdapter;
    /* access modifiers changed from: private */
    public BluetoothLeScanner mBluetoothLeScanner;
    private Handler mHandler;
    /* access modifiers changed from: private */
    public ScanCallback mLeScanCallback = new ScanCallback() {
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice device = result.getDevice();
            String name = device.getName();
            if (name != null && name.equals(ScanActivity.DEVICE_NAME) && result.getScanRecord() != null) {
                Boolean isDeviceInList = Boolean.valueOf(false);
                NanoDevice nanoDevice = new NanoDevice(device, result.getRssi(), result.getScanRecord().getBytes());
                Iterator it = ScanActivity.this.nanoDeviceList.iterator();
                while (it.hasNext()) {
                    NanoDevice d = (NanoDevice) it.next();
                    if (d.getNanoMac().equals(device.getAddress())) {
                        isDeviceInList = Boolean.valueOf(true);
                        d.setRssi(result.getRssi());
                        ScanActivity.this.nanoScanAdapter.notifyDataSetChanged();
                    }
                }
                if (!isDeviceInList.booleanValue()) {
                    ScanActivity.this.nanoDeviceList.add(nanoDevice);
                    ScanActivity.this.nanoScanAdapter.notifyDataSetChanged();
                }
            }
        }
    };
    /* access modifiers changed from: private */
    public ArrayList<NanoDevice> nanoDeviceList = new ArrayList<>();
    /* access modifiers changed from: private */
    public NanoScanAdapter nanoScanAdapter;

    private class NanoScanAdapter extends ArrayAdapter<NanoDevice> {
        private final ArrayList<NanoDevice> nanoDevices;

        public NanoScanAdapter(Context context, ArrayList<NanoDevice> values) {
            super(context, -1, values);
            this.nanoDevices = values;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.row_nano_scan_item, parent, false);
                viewHolder = new ViewHolder();
                viewHolder.nanoName = (TextView) convertView.findViewById(R.id.tv_nano_name);
                viewHolder.nanoMac = (TextView) convertView.findViewById(R.id.tv_nano_mac);
                viewHolder.nanoRssi = (TextView) convertView.findViewById(R.id.tv_rssi);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            NanoDevice device = (NanoDevice) getItem(position);
            if (device != null) {
                viewHolder.nanoName.setText(device.getNanoName());
                viewHolder.nanoMac.setText(device.getNanoMac());
                viewHolder.nanoRssi.setText(device.getRssiString());
            }
            return convertView;
        }
    }

    private class ViewHolder {
        /* access modifiers changed from: private */
        public TextView nanoMac;
        /* access modifiers changed from: private */
        public TextView nanoName;
        /* access modifiers changed from: private */
        public TextView nanoRssi;

        private ViewHolder() {
        }
    }

    /* access modifiers changed from: protected */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        mContext = this;
        ActionBar ab = getActionBar();
        if (ab != null) {
            ab.setTitle(getString(R.string.select_nano));
            ab.setDisplayHomeAsUpEnabled(true);
        }
        ListView lv_nanoDevices = (ListView) findViewById(R.id.lv_nanoDevices);
        this.mBluetoothAdapter = ((BluetoothManager) getSystemService(BLUETOOTH_SERVICE)).getAdapter();
        this.mBluetoothLeScanner = this.mBluetoothAdapter.getBluetoothLeScanner();
        this.nanoScanAdapter = new NanoScanAdapter(this, this.nanoDeviceList);
        lv_nanoDevices.setAdapter(this.nanoScanAdapter);
        lv_nanoDevices.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                ScanActivity.this.confirmationDialog(((NanoDevice) ScanActivity.this.nanoDeviceList.get(i)).getNanoMac());
            }
        });
        this.mHandler = new Handler();
        scanLeDevice(true);
    }

    public void confirmationDialog(String mac) {
        Builder alertDialogBuilder = new Builder(mContext);
        final String deviceMac = mac;
        alertDialogBuilder.setTitle(mContext.getResources().getString(R.string.nano_confirmation_title));
        alertDialogBuilder.setMessage(mContext.getResources().getString(R.string.nano_confirmation_msg, new Object[]{mac}));
        alertDialogBuilder.setPositiveButton(getResources().getString(R.string.ok), new OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                ScanActivity.this.alertDialog.dismiss();
                SettingsManager.storeStringPref(ScanActivity.mContext, SharedPreferencesKeys.preferredDevice, deviceMac);
                ScanActivity.this.finish();
            }
        });
        alertDialogBuilder.setNegativeButton(getResources().getString(R.string.cancel), new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                ScanActivity.this.alertDialog.dismiss();
            }
        });
        this.alertDialog = alertDialogBuilder.create();
        this.alertDialog.show();
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 16908332) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private void scanLeDevice(boolean enable) {
        if (this.mBluetoothLeScanner == null) {
            Toast.makeText(this, "Could not open LE scanner", Toast.LENGTH_SHORT).show();
        } else if (enable) {
            this.mHandler.postDelayed(new Runnable() {
                public void run() {
                    ScanActivity.this.mBluetoothLeScanner.stopScan(ScanActivity.this.mLeScanCallback);
                }
            }, NanoBLEService.SCAN_PERIOD);
            this.mBluetoothLeScanner.startScan(this.mLeScanCallback);
        } else {
            this.mBluetoothLeScanner.stopScan(this.mLeScanCallback);
        }
    }
}
