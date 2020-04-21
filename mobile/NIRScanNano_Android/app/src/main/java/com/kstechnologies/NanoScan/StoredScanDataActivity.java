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
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.baoyz.swipemenulistview.SwipeMenu;
import com.baoyz.swipemenulistview.SwipeMenuCreator;
import com.baoyz.swipemenulistview.SwipeMenuItem;
import com.baoyz.swipemenulistview.SwipeMenuListView;
import com.baoyz.swipemenulistview.SwipeMenuListView.OnMenuItemClickListener;
import com.kstechnologies.nirscannanolibrary.KSTNanoSDK;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class StoredScanDataActivity extends Activity {
    /* access modifiers changed from: private */
    public static Context mContext;
    ProgressDialog barProgressDialog;
    private final IntentFilter disconnFilter = new IntentFilter(KSTNanoSDK.ACTION_GATT_DISCONNECTED);
    private final BroadcastReceiver disconnReceiver = new DisconnReceiver();
    /* access modifiers changed from: private */
    public int receivedScanSize;
    /* access modifiers changed from: private */
    public ProgressBar scanIndexProgress;
    /* access modifiers changed from: private */
    public StoredScanAdapter storedScanAdapter;
    private final IntentFilter storedScanFilter = new IntentFilter(KSTNanoSDK.STORED_SCAN_DATA);
    /* access modifiers changed from: private */
    public ArrayList<StoredScan> storedScanList = new ArrayList<>();
    private BroadcastReceiver storedScanReceiver = new StoredScanReceiver();
    /* access modifiers changed from: private */
    public int storedScanSize;
    private BroadcastReceiver storedScanSizeReceiver;
    /* access modifiers changed from: private */
    public TextView tv_no_scans;
    /* access modifiers changed from: private */
    public SwipeMenuCreator unknownCreator = createMenu();

    public class DisconnReceiver extends BroadcastReceiver {
        public DisconnReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            Toast.makeText(StoredScanDataActivity.mContext, R.string.nano_disconnected, Toast.LENGTH_SHORT).show();
            StoredScanDataActivity.this.finish();
        }
    }

    private class StoredScan {
        String scanDate;
        byte[] scanIndex;
        String scanName;

        public StoredScan(String scanName2, String scanDate2, byte[] scanIndex2) {
            this.scanName = scanName2;
            this.scanDate = scanDate2;
            this.scanIndex = scanIndex2;
        }

        public String getScanName() {
            return this.scanName;
        }

        public String getScanDate() {
            return this.scanDate;
        }

        public byte[] getScanIndex() {
            return this.scanIndex;
        }
    }

    public class StoredScanAdapter extends ArrayAdapter<StoredScan> {
        private final ArrayList<StoredScan> storedScans;

        public StoredScanAdapter(Context context, ArrayList<StoredScan> values) {
            super(context, -1, values);
            this.storedScans = values;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.row_stored_scan_item, parent, false);
                viewHolder = new ViewHolder();
                viewHolder.scanName = (TextView) convertView.findViewById(R.id.tv_scan_name);
                viewHolder.scanDate = (TextView) convertView.findViewById(R.id.tv_scan_date);
                viewHolder.scanIndex = (TextView) convertView.findViewById(R.id.tv_scan_index);
                viewHolder.scanIndex.setVisibility(View.GONE);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            StoredScan scan = (StoredScan) getItem(position);
            if (scan != null) {
                viewHolder.scanName.setText(scan.getScanName());
                viewHolder.scanDate.setText(scan.getScanDate());
                StringBuilder stringBuilder = new StringBuilder();
                for (byte b : scan.getScanIndex()) {
                    stringBuilder.append(b);
                }
                viewHolder.scanIndex.setText(stringBuilder.toString());
            }
            return convertView;
        }
    }

    private class StoredScanReceiver extends BroadcastReceiver {
        private StoredScanReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            String scanDate = intent.getStringExtra(KSTNanoSDK.EXTRA_SCAN_DATE);
            final SwipeMenuListView lv_stored_scans = (SwipeMenuListView) StoredScanDataActivity.this.findViewById(R.id.lv_stored_scans);
            StoredScanDataActivity.this.receivedScanSize = StoredScanDataActivity.this.receivedScanSize + 1;
            if (StoredScanDataActivity.this.receivedScanSize == StoredScanDataActivity.this.storedScanSize) {
                StoredScanDataActivity.this.barProgressDialog.dismiss();
                lv_stored_scans.setVisibility(View.VISIBLE);
            } else {
                StoredScanDataActivity.this.barProgressDialog.setProgress(StoredScanDataActivity.this.receivedScanSize);
            }
            StoredScanDataActivity.this.storedScanList.add(new StoredScan(intent.getStringExtra(KSTNanoSDK.EXTRA_SCAN_NAME), StoredScanDataActivity.this.createTimeString(scanDate), intent.getByteArrayExtra(KSTNanoSDK.EXTRA_SCAN_INDEX)));
            StoredScanDataActivity.this.storedScanAdapter = new StoredScanAdapter(StoredScanDataActivity.mContext, StoredScanDataActivity.this.storedScanList);
            lv_stored_scans.setAdapter((ListAdapter) StoredScanDataActivity.this.storedScanAdapter);
            lv_stored_scans.setMenuCreator(StoredScanDataActivity.this.unknownCreator);
            lv_stored_scans.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                public boolean onMenuItemClick(int position, SwipeMenu menu, int index) {
                    switch (index) {
                        case 0:
                            Intent deleteScanIntent = new Intent(KSTNanoSDK.DELETE_SCAN);
                            deleteScanIntent.putExtra(KSTNanoSDK.EXTRA_SCAN_INDEX, ((StoredScan) StoredScanDataActivity.this.storedScanAdapter.getItem(position)).getScanIndex());
                            LocalBroadcastManager.getInstance(StoredScanDataActivity.mContext).sendBroadcast(deleteScanIntent);
                            StoredScanDataActivity.this.storedScanAdapter.remove(StoredScanDataActivity.this.storedScanList.get(position));
                            lv_stored_scans.setAdapter((ListAdapter) StoredScanDataActivity.this.storedScanAdapter);
                            break;
                    }
                    return false;
                }
            });
        }
    }

    private class ViewHolder {
        /* access modifiers changed from: private */
        public TextView scanDate;
        /* access modifiers changed from: private */
        public TextView scanIndex;
        /* access modifiers changed from: private */
        public TextView scanName;

        private ViewHolder() {
        }
    }

    /* access modifiers changed from: protected */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stored_scan_data);
        this.scanIndexProgress = (ProgressBar) findViewById(R.id.scanIndexProgress);
        this.tv_no_scans = (TextView) findViewById(R.id.tv_no_scans);
        mContext = this;
        ActionBar ab = getActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setTitle(getString(R.string.stored_scan_data));
        }
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(KSTNanoSDK.GET_STORED_SCANS));
        this.storedScanSizeReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                StoredScanDataActivity.this.scanIndexProgress.setVisibility(View.INVISIBLE);
                StoredScanDataActivity.this.storedScanSize = intent.getIntExtra(KSTNanoSDK.EXTRA_INDEX_SIZE, 0);
                if (StoredScanDataActivity.this.storedScanSize > 0) {
                    StoredScanDataActivity.this.tv_no_scans.setVisibility(View.GONE);
                    StoredScanDataActivity.this.barProgressDialog = new ProgressDialog(StoredScanDataActivity.this);
                    StoredScanDataActivity.this.barProgressDialog.setTitle(StoredScanDataActivity.this.getString(R.string.reading_sd_card));
                    StoredScanDataActivity.this.barProgressDialog.setProgressStyle(1);
                    StoredScanDataActivity.this.barProgressDialog.setProgress(0);
                    StoredScanDataActivity.this.barProgressDialog.setMax(intent.getIntExtra(KSTNanoSDK.EXTRA_INDEX_SIZE, 0));
                    StoredScanDataActivity.this.barProgressDialog.setCancelable(true);
                    StoredScanDataActivity.this.barProgressDialog.setOnCancelListener(new OnCancelListener() {
                        public void onCancel(DialogInterface dialogInterface) {
                            StoredScanDataActivity.this.finish();
                        }
                    });
                    StoredScanDataActivity.this.barProgressDialog.show();
                    StoredScanDataActivity.this.receivedScanSize = 0;
                    return;
                }
                StoredScanDataActivity.this.receivedScanSize = 0;
                StoredScanDataActivity.this.tv_no_scans.setVisibility(View.VISIBLE);
            }
        };
        LocalBroadcastManager.getInstance(mContext).registerReceiver(this.storedScanReceiver, this.storedScanFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(this.storedScanSizeReceiver, new IntentFilter(KSTNanoSDK.SD_SCAN_SIZE));
        LocalBroadcastManager.getInstance(mContext).registerReceiver(this.disconnReceiver, this.disconnFilter);
    }

    public void onResume() {
        super.onResume();
    }

    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(this.storedScanReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(this.storedScanSizeReceiver);
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

    /* access modifiers changed from: private */
    public String createTimeString(String scanDate) {
        try {
            return new SimpleDateFormat("EEEE, M/d/yy " + getString(R.string.time_format_at) + " HH:mm:ss", Locale.US).format(new SimpleDateFormat("yyMMddFFHHmmss", Locale.US).parse(scanDate));
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    private SwipeMenuCreator createMenu() {
        return new SwipeMenuCreator() {
            public void create(SwipeMenu menu) {
                SwipeMenuItem settingsItem = new SwipeMenuItem(StoredScanDataActivity.this.getApplicationContext());
                settingsItem.setBackground((int) R.color.kst_red);
                settingsItem.setWidth(StoredScanDataActivity.this.dp2px(90));
                settingsItem.setTitleColor(ContextCompat.getColor(StoredScanDataActivity.mContext, R.color.white));
                settingsItem.setTitleSize(18);
                settingsItem.setTitle(StoredScanDataActivity.this.getResources().getString(R.string.delete));
                menu.addMenuItem(settingsItem);
            }
        };
    }

    /* access modifiers changed from: private */
    public int dp2px(int dp) {
        return (int) TypedValue.applyDimension(1, (float) dp, getResources().getDisplayMetrics());
    }
}
