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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import com.kstechnologies.nirscannanolibrary.KSTNanoSDK;

public class ConfigureActivity extends Activity {
    /* access modifiers changed from: private */
    public static Context mContext;
    private final IntentFilter disconnFilter = new IntentFilter(KSTNanoSDK.ACTION_GATT_DISCONNECTED);
    private final BroadcastReceiver disconnReceiver = new DisconnReceiver();

    public class DisconnReceiver extends BroadcastReceiver {
        public DisconnReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            ConfigureActivity.this.finish();
        }
    }

    /* access modifiers changed from: protected */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configure);
        mContext = this;
        ActionBar ab = getActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setTitle(getString(R.string.configure));
        }
        ((ListView) findViewById(R.id.lv_configure)).setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                switch (i) {
                    case 0:
                        ConfigureActivity.this.startActivity(new Intent(ConfigureActivity.mContext, DeviceInfoActivity.class));
                        return;
                    case 1:
                        ConfigureActivity.this.startActivity(new Intent(ConfigureActivity.mContext, DeviceStatusActivity.class));
                        return;
                    case 2:
                        ConfigureActivity.this.startActivity(new Intent(ConfigureActivity.mContext, ScanConfActivity.class));
                        return;
                    case 3:
                        ConfigureActivity.this.startActivity(new Intent(ConfigureActivity.mContext, StoredScanDataActivity.class));
                        return;
                    default:
                        return;
                }
            }
        });
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
