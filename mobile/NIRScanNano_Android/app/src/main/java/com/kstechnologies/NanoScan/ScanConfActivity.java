package com.kstechnologies.NanoScan;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.kstechnologies.nirscannanolibrary.KSTNanoSDK;
import com.kstechnologies.nirscannanolibrary.KSTNanoSDK.ScanConfiguration;
import com.kstechnologies.nirscannanolibrary.SettingsManager;
import com.kstechnologies.nirscannanolibrary.SettingsManager.SharedPreferencesKeys;
import java.util.ArrayList;
import java.util.Iterator;

public class ScanConfActivity extends Activity {
    /* access modifiers changed from: private */
    public static Context mContext;
    ProgressDialog barProgressDialog;
    /* access modifiers changed from: private */
    public ArrayList<ScanConfiguration> configs = new ArrayList<>();
    private final IntentFilter disconnFilter = new IntentFilter(KSTNanoSDK.ACTION_GATT_DISCONNECTED);
    private final BroadcastReceiver disconnReceiver = new DisconnReceiver();
    private BroadcastReceiver getActiveScanConfReceiver;
    /* access modifiers changed from: private */
    public ListView lv_configs;
    /* access modifiers changed from: private */
    public int receivedConfSize;
    /* access modifiers changed from: private */
    public ScanConfAdapter scanConfAdapter;
    private final IntentFilter scanConfFilter = new IntentFilter(KSTNanoSDK.SCAN_CONF_DATA);
    private final BroadcastReceiver scanConfReceiver = new ScanConfReceiver();
    private BroadcastReceiver scanConfSizeReceiver;
    /* access modifiers changed from: private */
    public int storedConfSize;

    public class DisconnReceiver extends BroadcastReceiver {
        public DisconnReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            Toast.makeText(ScanConfActivity.mContext, R.string.nano_disconnected, Toast.LENGTH_SHORT).show();
            ScanConfActivity.this.finish();
        }
    }

    public class ScanConfAdapter extends ArrayAdapter<ScanConfiguration> {
        /* access modifiers changed from: private */
        public final ArrayList<ScanConfiguration> configs;

        public ScanConfAdapter(Context context, ArrayList<ScanConfiguration> values) {
            super(context, -1, values);
            this.configs = values;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.row_scan_configuration_item, parent, false);
                viewHolder = new ViewHolder();
                viewHolder.scanType = (TextView) convertView.findViewById(R.id.tv_scan_type);
                viewHolder.rangeStart = (TextView) convertView.findViewById(R.id.tv_range_start_value);
                viewHolder.rangeEnd = (TextView) convertView.findViewById(R.id.tv_range_end_value);
                viewHolder.width = (TextView) convertView.findViewById(R.id.tv_width_value);
                viewHolder.patterns = (TextView) convertView.findViewById(R.id.tv_patterns_value);
                viewHolder.repeats = (TextView) convertView.findViewById(R.id.tv_repeats_value);
                viewHolder.serial = (TextView) convertView.findViewById(R.id.tv_serial_value);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            ScanConfiguration config = (ScanConfiguration) getItem(position);
            if (config != null) {
                viewHolder.scanType.setText(config.getConfigName());
                viewHolder.rangeStart.setText(ScanConfActivity.this.getString(R.string.range_start_value, new Object[]{Integer.valueOf(config.getWavelengthStartNm())}));
                viewHolder.rangeEnd.setText(ScanConfActivity.this.getString(R.string.range_end_value, new Object[]{Integer.valueOf(config.getWavelengthEndNm())}));
                viewHolder.width.setText(ScanConfActivity.this.getString(R.string.width_value, new Object[]{Integer.valueOf(config.getWidthPx())}));
                viewHolder.patterns.setText(ScanConfActivity.this.getString(R.string.patterns_value, new Object[]{Integer.valueOf(config.getNumPatterns())}));
                viewHolder.repeats.setText(ScanConfActivity.this.getString(R.string.repeats_value, new Object[]{Integer.valueOf(config.getNumRepeats())}));
                viewHolder.serial.setText(config.getScanConfigSerialNumber());
                if (config.isActive()) {
                    viewHolder.scanType.setTextColor(ContextCompat.getColor(ScanConfActivity.mContext, R.color.active_conf));
                    SettingsManager.storeStringPref(ScanConfActivity.mContext, SharedPreferencesKeys.scanConfiguration, config.getConfigName());
                } else {
                    viewHolder.scanType.setTextColor(ContextCompat.getColor(ScanConfActivity.mContext, R.color.black));
                }
            }
            return convertView;
        }
    }

    private class ScanConfReceiver extends BroadcastReceiver {
        private ScanConfReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            ScanConfiguration scanConf = KSTNanoSDK.KSTNanoSDK_dlpSpecScanReadConfiguration(intent.getByteArrayExtra(KSTNanoSDK.EXTRA_DATA));
            ScanConfActivity.this.lv_configs = (ListView) ScanConfActivity.this.findViewById(R.id.lv_configs);
            ScanConfActivity.this.lv_configs.setOnItemClickListener(new OnItemClickListener() {
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    byte[] index = {0, 0};
                    index[0] = (byte) ((ScanConfiguration) ScanConfActivity.this.scanConfAdapter.configs.get(i)).getScanConfigIndex();
                    Intent setActiveConfIntent = new Intent(KSTNanoSDK.SET_ACTIVE_CONF);
                    setActiveConfIntent.putExtra(KSTNanoSDK.EXTRA_SCAN_INDEX, index);
                    LocalBroadcastManager.getInstance(ScanConfActivity.mContext).sendBroadcast(setActiveConfIntent);
                }
            });
            ScanConfActivity.this.receivedConfSize = ScanConfActivity.this.receivedConfSize + 1;
            if (ScanConfActivity.this.receivedConfSize == ScanConfActivity.this.storedConfSize) {
                LocalBroadcastManager.getInstance(ScanConfActivity.mContext).sendBroadcast(new Intent(KSTNanoSDK.GET_ACTIVE_CONF));
            } else {
                ScanConfActivity.this.barProgressDialog.setProgress(ScanConfActivity.this.receivedConfSize);
            }
            ScanConfActivity.this.configs.add(scanConf);
            ScanConfActivity.this.scanConfAdapter = new ScanConfAdapter(ScanConfActivity.mContext, ScanConfActivity.this.configs);
            ScanConfActivity.this.lv_configs.setAdapter(ScanConfActivity.this.scanConfAdapter);
        }
    }

    private class ViewHolder {
        /* access modifiers changed from: private */
        public TextView patterns;
        /* access modifiers changed from: private */
        public TextView rangeEnd;
        /* access modifiers changed from: private */
        public TextView rangeStart;
        /* access modifiers changed from: private */
        public TextView repeats;
        /* access modifiers changed from: private */
        public TextView scanType;
        /* access modifiers changed from: private */
        public TextView serial;
        /* access modifiers changed from: private */
        public TextView width;

        private ViewHolder() {
        }
    }

    /* access modifiers changed from: protected */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_conf);
        mContext = this;
        ActionBar ab = getActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setTitle(getString(R.string.stored_configurations));
        }
        this.scanConfSizeReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                ScanConfActivity.this.storedConfSize = intent.getIntExtra(KSTNanoSDK.EXTRA_CONF_SIZE, 0);
                if (ScanConfActivity.this.storedConfSize > 0) {
                    ScanConfActivity.this.barProgressDialog = new ProgressDialog(ScanConfActivity.this);
                    ScanConfActivity.this.barProgressDialog.setTitle(ScanConfActivity.this.getString(R.string.reading_configurations));
                    ScanConfActivity.this.barProgressDialog.setProgressStyle(1);
                    ScanConfActivity.this.barProgressDialog.setProgress(0);
                    ScanConfActivity.this.barProgressDialog.setMax(intent.getIntExtra(KSTNanoSDK.EXTRA_CONF_SIZE, 0));
                    ScanConfActivity.this.barProgressDialog.setCancelable(true);
                    ScanConfActivity.this.barProgressDialog.setOnCancelListener(new OnCancelListener() {
                        public void onCancel(DialogInterface dialogInterface) {
                            ScanConfActivity.this.finish();
                        }
                    });
                    ScanConfActivity.this.barProgressDialog.show();
                    ScanConfActivity.this.receivedConfSize = 0;
                }
            }
        };
        this.getActiveScanConfReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                byte index = intent.getByteArrayExtra(KSTNanoSDK.EXTRA_ACTIVE_CONF)[0];
                ScanConfActivity.this.barProgressDialog.dismiss();
                ScanConfActivity.this.lv_configs.setVisibility(View.VISIBLE);
                Iterator it = ScanConfActivity.this.scanConfAdapter.configs.iterator();
                while (it.hasNext()) {
                    ScanConfiguration c = (ScanConfiguration) it.next();
                    if (c.getScanConfigIndex() == index) {
                        c.setActive(true);
                        ScanConfActivity.this.lv_configs.setAdapter(ScanConfActivity.this.scanConfAdapter);
                    } else {
                        c.setActive(false);
                    }
                }
            }
        };
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(KSTNanoSDK.GET_SCAN_CONF));
        LocalBroadcastManager.getInstance(mContext).registerReceiver(this.scanConfReceiver, this.scanConfFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(this.disconnReceiver, this.disconnFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(this.scanConfSizeReceiver, new IntentFilter(KSTNanoSDK.SCAN_CONF_SIZE));
        LocalBroadcastManager.getInstance(mContext).registerReceiver(this.getActiveScanConfReceiver, new IntentFilter(KSTNanoSDK.SEND_ACTIVE_CONF));
    }

    public void onResume() {
        super.onResume();
    }

    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(this.scanConfReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(this.disconnReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(this.scanConfSizeReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(this.getActiveScanConfReceiver);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 16908332) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}
