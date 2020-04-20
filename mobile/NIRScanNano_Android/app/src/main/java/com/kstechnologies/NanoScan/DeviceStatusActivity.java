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
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.kstechnologies.nirscannanolibrary.KSTNanoSDK;
import com.kstechnologies.nirscannanolibrary.SettingsManager;
import com.kstechnologies.nirscannanolibrary.SettingsManager.SharedPreferencesKeys;

public class DeviceStatusActivity extends Activity {
    /* access modifiers changed from: private */
    public static Context mContext;
    private final IntentFilter disconnFilter = new IntentFilter(KSTNanoSDK.ACTION_GATT_DISCONNECTED);
    private final BroadcastReceiver disconnReceiver = new DisconnReceiver();
    /* access modifiers changed from: private */
    public EditText et_humidThresh;
    /* access modifiers changed from: private */
    public EditText et_tempThresh;
    private BroadcastReceiver mStatusReceiver;
    /* access modifiers changed from: private */
    public TextView tv_batt;
    /* access modifiers changed from: private */
    public TextView tv_humid;
    /* access modifiers changed from: private */
    public TextView tv_temp;

    public class DisconnReceiver extends BroadcastReceiver {
        public DisconnReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            Toast.makeText(DeviceStatusActivity.mContext, R.string.nano_disconnected, 0).show();
            DeviceStatusActivity.this.finish();
        }
    }

    /* access modifiers changed from: protected */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_status);
        mContext = this;
        ActionBar ab = getActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setTitle(getString(R.string.device_status));
        }
        this.tv_batt = (TextView) findViewById(R.id.tv_batt);
        this.tv_temp = (TextView) findViewById(R.id.tv_temp);
        this.tv_humid = (TextView) findViewById(R.id.tv_humid);
        this.et_tempThresh = (EditText) findViewById(R.id.et_tempThresh);
        this.et_humidThresh = (EditText) findViewById(R.id.et_humidThresh);
        ((Button) findViewById(R.id.btn_update_thresholds)).setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                Intent thresholdUpdateIntent = new Intent(KSTNanoSDK.UPDATE_THRESHOLD);
                String tempString = DeviceStatusActivity.this.et_tempThresh.getText().toString();
                String humidString = DeviceStatusActivity.this.et_humidThresh.getText().toString();
                byte[] tempThreshBytes = {0, 0};
                byte[] humidThreshBytes = {0, 0};
                if (!tempString.equals("")) {
                    int tempThreshFloat = (int) (Float.parseFloat(tempString) * 100.0f);
                    tempThreshBytes[0] = (byte) (tempThreshFloat & 255);
                    tempThreshBytes[1] = (byte) ((tempThreshFloat >> 8) & 255);
                }
                if (!humidString.equals("")) {
                    int humidThreshFloat = (int) (Float.parseFloat(humidString) * 100.0f);
                    humidThreshBytes[0] = (byte) (humidThreshFloat & 255);
                    humidThreshBytes[1] = (byte) ((humidThreshFloat >> 8) & 255);
                }
                thresholdUpdateIntent.putExtra(KSTNanoSDK.EXTRA_TEMP_THRESH, tempThreshBytes);
                thresholdUpdateIntent.putExtra(KSTNanoSDK.EXTRA_HUMID_THRESH, humidThreshBytes);
                LocalBroadcastManager.getInstance(DeviceStatusActivity.mContext).sendBroadcast(thresholdUpdateIntent);
            }
        });
        if (!SettingsManager.getBooleanPref(this, SharedPreferencesKeys.tempUnits, false)) {
            this.et_tempThresh.setHint(R.string.deg_c);
        } else {
            this.et_tempThresh.setHint(R.string.deg_f);
        }
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(KSTNanoSDK.GET_STATUS));
        this.mStatusReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                int batt = intent.getIntExtra(KSTNanoSDK.EXTRA_BATT, 0);
                float temp = intent.getFloatExtra(KSTNanoSDK.EXTRA_TEMP, 0.0f);
                DeviceStatusActivity.this.tv_batt.setText(DeviceStatusActivity.this.getString(R.string.batt_level_value, new Object[]{Integer.valueOf(batt)}));
                if (!SettingsManager.getBooleanPref(DeviceStatusActivity.mContext, SharedPreferencesKeys.tempUnits, false)) {
                    DeviceStatusActivity.this.tv_temp.setText(DeviceStatusActivity.this.getString(R.string.temp_value_c, new Object[]{Float.toString(temp)}));
                } else {
                    float temp2 = ((float) (((double) temp) * 1.8d)) + 32.0f;
                    DeviceStatusActivity.this.tv_temp.setText(DeviceStatusActivity.this.getString(R.string.temp_value_f, new Object[]{Float.toString(temp2)}));
                }
                DeviceStatusActivity.this.tv_humid.setText(DeviceStatusActivity.this.getString(R.string.humid_value, new Object[]{Float.valueOf(intent.getFloatExtra(KSTNanoSDK.EXTRA_HUMID, 0.0f))}));
                ((ProgressBar) DeviceStatusActivity.this.findViewById(R.id.pb_status)).setVisibility(4);
            }
        };
        LocalBroadcastManager.getInstance(mContext).registerReceiver(this.mStatusReceiver, new IntentFilter(KSTNanoSDK.ACTION_STATUS));
        LocalBroadcastManager.getInstance(mContext).registerReceiver(this.disconnReceiver, this.disconnFilter);
    }

    public void onResume() {
        super.onResume();
    }

    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(this.mStatusReceiver);
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
