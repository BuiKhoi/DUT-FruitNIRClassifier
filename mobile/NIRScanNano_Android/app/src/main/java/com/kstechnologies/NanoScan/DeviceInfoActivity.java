package com.kstechnologies.NanoScan;

import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.kstechnologies.nirscannanolibrary.KSTNanoSDK;

public class DeviceInfoActivity extends Activity {
    /* access modifiers changed from: private */
    public static Context mContext;
    private final IntentFilter disconnFilter = new IntentFilter(KSTNanoSDK.ACTION_GATT_DISCONNECTED);
    private final BroadcastReceiver disconnReceiver = new DisconnReceiver();
    private BroadcastReceiver mInfoReceiver;
    /* access modifiers changed from: private */
    public TextView tv_hw;
    /* access modifiers changed from: private */
    public TextView tv_manuf;
    /* access modifiers changed from: private */
    public TextView tv_model;
    /* access modifiers changed from: private */
    public TextView tv_serial;
    /* access modifiers changed from: private */
    public TextView tv_spec;
    /* access modifiers changed from: private */
    public TextView tv_tiva;

    public class DisconnReceiver extends BroadcastReceiver {
        public DisconnReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            Toast.makeText(DeviceInfoActivity.mContext, R.string.nano_disconnected, Toast.LENGTH_SHORT).show();
            DeviceInfoActivity.this.finish();
        }
    }

    /* access modifiers changed from: protected */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_info);
        mContext = this;
        ActionBar ab = getActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setTitle(getString(R.string.device_information));
        }
        this.tv_manuf = (TextView) findViewById(R.id.tv_manuf);
        this.tv_model = (TextView) findViewById(R.id.tv_model);
        this.tv_serial = (TextView) findViewById(R.id.tv_serial);
        this.tv_hw = (TextView) findViewById(R.id.tv_hw);
        this.tv_tiva = (TextView) findViewById(R.id.tv_tiva);
        this.tv_spec = (TextView) findViewById(R.id.tv_spectrum);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(KSTNanoSDK.GET_INFO));
        this.mInfoReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                DeviceInfoActivity.this.tv_manuf.setText(intent.getStringExtra(KSTNanoSDK.EXTRA_MANUF_NAME).replace("\n", ""));
                DeviceInfoActivity.this.tv_model.setText(intent.getStringExtra(KSTNanoSDK.EXTRA_MODEL_NUM).replace("\n", ""));
                DeviceInfoActivity.this.tv_serial.setText(intent.getStringExtra(KSTNanoSDK.EXTRA_SERIAL_NUM));
                DeviceInfoActivity.this.tv_hw.setText(intent.getStringExtra(KSTNanoSDK.EXTRA_HW_REV));
                DeviceInfoActivity.this.tv_tiva.setText(intent.getStringExtra(KSTNanoSDK.EXTRA_TIVA_REV));
                DeviceInfoActivity.this.tv_spec.setText(intent.getStringExtra(KSTNanoSDK.EXTRA_SPECTRUM_REV));
                ((ProgressBar) DeviceInfoActivity.this.findViewById(R.id.pb_info)).setVisibility(View.INVISIBLE);
            }
        };
        LocalBroadcastManager.getInstance(mContext).registerReceiver(this.mInfoReceiver, new IntentFilter(KSTNanoSDK.ACTION_INFO));
        LocalBroadcastManager.getInstance(mContext).registerReceiver(this.disconnReceiver, this.disconnFilter);
    }

    public void onResume() {
        super.onResume();
    }

    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(this.mInfoReceiver);
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
