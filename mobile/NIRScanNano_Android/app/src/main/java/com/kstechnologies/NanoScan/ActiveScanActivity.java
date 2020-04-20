package com.kstechnologies.NanoScan;

import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.internal.view.SupportMenu;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.kstechnologies.nirscannanolibrary.KSTNanoSDK;
import com.kstechnologies.nirscannanolibrary.KSTNanoSDK.ScanConfiguration;
import com.kstechnologies.nirscannanolibrary.KSTNanoSDK.SlewScanSection;
import java.util.ArrayList;

public class ActiveScanActivity extends Activity {
    /* access modifiers changed from: private */
    public static Context mContext;
    private ArrayList<ScanConfiguration> configs = new ArrayList<>();
    private final IntentFilter disconnFilter = new IntentFilter(KSTNanoSDK.ACTION_GATT_DISCONNECTED);
    private final BroadcastReceiver disconnReceiver = new DisconnReceiver();
    private ListView lv_configs;
    private ScanConfAdapter scanConfAdapter;
    private ArrayList<SlewScanSection> sections = new ArrayList<>();
    private SlewScanConfAdapter slewScanConfAdapter;

    public class DisconnReceiver extends BroadcastReceiver {
        public DisconnReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            Toast.makeText(ActiveScanActivity.mContext, R.string.nano_disconnected, Toast.LENGTH_SHORT).show();
            ActiveScanActivity.this.finish();
        }
    }

    public class ScanConfAdapter extends ArrayAdapter<ScanConfiguration> {
        private final ArrayList<ScanConfiguration> configs;

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
                viewHolder.scanType.setVisibility(View.GONE);
                LinearLayout ll_range_end = (LinearLayout) convertView.findViewById(R.id.ll_range_end);
                LinearLayout ll_patterns = (LinearLayout) convertView.findViewById(R.id.ll_patterns);
                LinearLayout ll_width = (LinearLayout) convertView.findViewById(R.id.ll_width);
                ((LinearLayout) convertView.findViewById(R.id.ll_range_start)).setVisibility(View.VISIBLE);
                ll_range_end.setVisibility(View.VISIBLE);
                ll_patterns.setVisibility(View.VISIBLE);
                ll_width.setVisibility(View.VISIBLE);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            ScanConfiguration config = (ScanConfiguration) getItem(position);
            if (config != null) {
                viewHolder.scanType.setText(config.getConfigName());
                viewHolder.rangeStart.setText(ActiveScanActivity.this.getString(R.string.range_start_value, new Object[]{Integer.valueOf(config.getWavelengthStartNm())}));
                viewHolder.rangeEnd.setText(ActiveScanActivity.this.getString(R.string.range_end_value, new Object[]{Integer.valueOf(config.getWavelengthEndNm())}));
                viewHolder.width.setText(ActiveScanActivity.this.getString(R.string.width_value, new Object[]{Integer.valueOf(config.getWidthPx())}));
                viewHolder.patterns.setText(ActiveScanActivity.this.getString(R.string.patterns_value, new Object[]{Integer.valueOf(config.getNumPatterns())}));
                viewHolder.repeats.setText(ActiveScanActivity.this.getString(R.string.repeats_value, new Object[]{Integer.valueOf(config.getNumRepeats())}));
                viewHolder.serial.setText(config.getScanConfigSerialNumber());
            }
            return convertView;
        }
    }

    public class SlewScanConfAdapter extends ArrayAdapter<SlewScanSection> {
        private final ArrayList<SlewScanSection> sections;

        public SlewScanConfAdapter(Context context, ArrayList<SlewScanSection> values) {
            super(context, -1, values);
            this.sections = values;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.row_slew_scan_configuration_item, parent, false);
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
            SlewScanSection config = (SlewScanSection) getItem(position);
            if (config != null) {
                viewHolder.rangeStart.setText(ActiveScanActivity.this.getString(R.string.range_start_value, new Object[]{Integer.valueOf(config.getWavelengthStartNm())}));
                viewHolder.rangeEnd.setText(ActiveScanActivity.this.getString(R.string.range_end_value, new Object[]{Integer.valueOf(config.getWavelengthEndNm())}));
                viewHolder.width.setText(ActiveScanActivity.this.getString(R.string.width_value, new Object[]{Byte.valueOf(config.getWidthPx())}));
                viewHolder.patterns.setText(ActiveScanActivity.this.getString(R.string.patterns_value, new Object[]{Integer.valueOf(config.getNumPatterns())}));
                viewHolder.repeats.setText(ActiveScanActivity.this.getString(R.string.repeats_value, new Object[]{Integer.valueOf(config.getNumRepeats())}));
            }
            return convertView;
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
        setContentView(R.layout.activity_active_scan);
        mContext = this;
        ScanConfiguration activeConf = null;
        if (getIntent().getSerializableExtra("conf") != null) {
            activeConf = (ScanConfiguration) getIntent().getSerializableExtra("conf");
        }
        ActionBar ab = getActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
            if (activeConf != null) {
                ab.setTitle(activeConf.getConfigName());
            }
        }
        this.lv_configs = (ListView) findViewById(R.id.lv_configs);
        if (activeConf == null || !activeConf.getScanType().equals("Slew")) {
            this.configs.add(activeConf);
            this.scanConfAdapter = new ScanConfAdapter(mContext, this.configs);
            this.lv_configs.setAdapter(this.scanConfAdapter);
        } else {
            int numSections = activeConf.getSlewNumSections();
            for (int i = 0; i < numSections; i++) {
                this.sections.add(new SlewScanSection(activeConf.getSectionScanType()[i], activeConf.getSectionWidthPx()[i], activeConf.getSectionWavelengthStartNm()[i] & SupportMenu.USER_MASK, activeConf.getSectionWavelengthEndNm()[i] & SupportMenu.USER_MASK, activeConf.getSectionNumPatterns()[i], activeConf.getSectionNumRepeats()[i], activeConf.getSectionExposureTime()[i]));
            }
            Log.i("__ACTIVE_CONF", "Setting slew conf adapter");
            this.slewScanConfAdapter = new SlewScanConfAdapter(mContext, this.sections);
            this.lv_configs.setAdapter(this.slewScanConfAdapter);
        }
        LocalBroadcastManager.getInstance(mContext).registerReceiver(this.disconnReceiver, this.disconnFilter);
    }

    public void onResume() {
        super.onResume();
    }

    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(this.disconnReceiver);
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
