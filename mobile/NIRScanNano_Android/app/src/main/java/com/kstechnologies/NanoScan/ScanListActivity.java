package com.kstechnologies.NanoScan;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;

import com.baoyz.swipemenulistview.SwipeMenu;
import com.baoyz.swipemenulistview.SwipeMenuCreator;
import com.baoyz.swipemenulistview.SwipeMenuItem;
import com.baoyz.swipemenulistview.SwipeMenuListView;
import com.kstechnologies.NanoScan.R.raw;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;

public class ScanListActivity extends Activity {
    /* access modifiers changed from: private */
    public static Context mContext;
    /* access modifiers changed from: private */
    public ArrayList<String> csvFiles = new ArrayList<>();
    /* access modifiers changed from: private */
    public SwipeMenuListView lv_csv_files;
    /* access modifiers changed from: private */
    public ArrayAdapter<String> mAdapter;
    private SwipeMenuCreator unknownCreator = createMenu();

    /* access modifiers changed from: protected */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        getWindow().requestFeature(8);
        ActionBar ab = getActionBar();
        if (ab != null) {
            ab.hide();
        }
        setContentView(R.layout.activity_scan_list);
        final RelativeLayout mSplashLayout = (RelativeLayout) findViewById(R.id.rl_splash);
        final RelativeLayout mMainLayout = (RelativeLayout) findViewById(R.id.rl_mainLayout);
        mSplashLayout.setVisibility(View.VISIBLE);
        mMainLayout.setVisibility(View.GONE);
        Animation animSplash = AnimationUtils.loadAnimation(this, R.anim.alpha_splash);
        animSplash.setAnimationListener(new AnimationListener() {
            public void onAnimationStart(Animation animation) {
            }

            public void onAnimationEnd(Animation animation) {
                mSplashLayout.setVisibility(View.GONE);
                mMainLayout.setVisibility(View.VISIBLE);
                ScanListActivity.this.getActionBar().show();
            }

            public void onAnimationRepeat(Animation animation) {
            }
        });
        mSplashLayout.setAnimation(animSplash);
        animSplash.start();
    }

    public void onDestroy() {
        super.onDestroy();
    }

//    @SuppressLint("ResourceType")
    public void onResume() {
        super.onResume();
//        this.csvFiles.clear();
//        this.lv_csv_files = (SwipeMenuListView) findViewById(R.id.lv_csv_files);
//        populateListView();
//        this.mAdapter = new ArrayAdapter<>(this, 17367043, this.csvFiles);
//        this.lv_csv_files.setAdapter((ListAdapter) this.mAdapter);
//        this.lv_csv_files.setMenuCreator(this.unknownCreator);
//        this.lv_csv_files.setOnMenuItemClickListener(new OnMenuItemClickListener() {
//            public boolean onMenuItemClick(int position, SwipeMenu menu, int index) {
//                switch (index) {
//                    case 0:
//                        ScanListActivity.this.removeFile((String) ScanListActivity.this.mAdapter.getItem(position));
//                        ScanListActivity.this.mAdapter.remove(ScanListActivity.this.csvFiles.get(position));
//                        ScanListActivity.this.lv_csv_files.setAdapter((ListAdapter) ScanListActivity.this.mAdapter);
//                        break;
//                }
//                return false;
//            }
//        });
//        this.lv_csv_files.setOnItemClickListener(new OnItemClickListener() {
//            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
//                ScanListActivity.this.lv_csv_files.smoothOpenMenu(position);
//            }
//        });
//        this.mAdapter.notifyDataSetChanged();
//        this.lv_csv_files.setOnItemClickListener(new OnItemClickListener() {
//            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
//                Intent graphIntent = new Intent(ScanListActivity.mContext, GraphActivity.class);
//                graphIntent.putExtra("file_name", (String) ScanListActivity.this.mAdapter.getItem(i));
//                ScanListActivity.this.startActivity(graphIntent);
//            }
//        });
//        ((EditText) findViewById(R.id.et_search)).addTextChangedListener(new TextWatcher() {
//            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
//            }
//
//            public void onTextChanged(CharSequence s, int start, int before, int count) {
//                ScanListActivity.this.mAdapter.getFilter().filter(s);
//            }
//
//            public void afterTextChanged(Editable s) {
//            }
//        });
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_scan_list, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (id == R.id.action_info) {
            startActivity(new Intent(this, InfoActivity.class));
            return true;
        } else {
            if (id == R.id.action_scan) {
                Intent graphIntent = new Intent(mContext, NewScanActivity.class);
                graphIntent.putExtra("file_name", getString(R.string.newScan));
                startActivity(graphIntent);
            }
            return super.onOptionsItemSelected(item);
        }
    }

    public void populateListView() {
        File[] listFiles;
        for (Field file : raw.class.getFields()) {
            this.csvFiles.add(file.getName());
        }
        for (File f : new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "/").listFiles()) {
            if (f.isFile()) {
                String fileName = f.getName();
                if (fileName.contains(".csv")) {
                    this.csvFiles.add(fileName);
                }
            }
        }
    }

    public void removeFile(String name) {
        File[] listFiles;
        for (File f : new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "/").listFiles()) {
            if (f.isFile() && f.getName().equals(name)) {
                f.delete();
            }
        }
    }

    private SwipeMenuCreator createMenu() {
        return new SwipeMenuCreator() {
            public void create(SwipeMenu menu) {
                SwipeMenuItem settingsItem = new SwipeMenuItem(ScanListActivity.this.getApplicationContext());
                settingsItem.setBackground((int) R.color.kst_red);
                settingsItem.setWidth(ScanListActivity.this.dp2px(90));
                settingsItem.setTitleColor(ContextCompat.getColor(ScanListActivity.mContext, R.color.white));
                settingsItem.setTitleSize(18);
                settingsItem.setTitle(ScanListActivity.this.getResources().getString(R.string.delete));
                menu.addMenuItem(settingsItem);
            }
        };
    }

    /* access modifiers changed from: private */
    public int dp2px(int dp) {
        return (int) TypedValue.applyDimension(1, (float) dp, getResources().getDisplayMetrics());
    }
}
