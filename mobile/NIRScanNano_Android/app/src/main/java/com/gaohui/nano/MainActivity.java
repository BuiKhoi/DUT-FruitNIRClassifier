package com.gaohui.nano;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.support.v7.app.ActionBar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.baoyz.swipemenulistview.SwipeMenu;
import com.baoyz.swipemenulistview.SwipeMenuCreator;
import com.baoyz.swipemenulistview.SwipeMenuItem;
import com.baoyz.swipemenulistview.SwipeMenuListView;
import com.gaohui.utils.DBUtil;
import com.gaohui.utils.ThemeManageUtil;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;


public class MainActivity extends BaseActivity {

    private static Context mContext;
    private DrawerLayout drawerLayout;
    private BluetoothAdapter bluetoothAdapter;
    private boolean bluetoothState = false;
    private Button btnBlt, btnStart, btnSetup;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_main);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        btnBlt = (Button) findViewById(R.id.btn_blt);
        btnStart = (Button) findViewById(R.id.btn_start);
        btnSetup = (Button) findViewById(R.id.btn_setup);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        this.setSupportActionBar(toolbar);
        ActionBar actionBar = this.getSupportActionBar();
        actionBar.setTitle("NIRScan Nano");

        drawerLayout = (DrawerLayout) findViewById(R.id.main_drawer_layout);

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        mContext.registerReceiver(bluetoothStateChangeReceiver, filter);

        DBUtil.copyDBToDatabases(mContext);

        btnSetup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent settingsIntent = new Intent(mContext, SettingsActivity.class);
                startActivity(settingsIntent);
            }
        });

        btnBlt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bluetoothState){
                    closeBluetooth();
                    btnBlt.setText("Turn on Bluetooth");
                }
                else {
                    openBluetooth();
                    btnBlt.setText("Turn off Bluetooth");
                }
            }
        });

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent graphIntent = new Intent(mContext, NewScanActivity.class);
                startActivity(graphIntent);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        bluetoothState = bluetoothAdapter.isEnabled();
        if (bluetoothState){
            btnBlt.setText("Turn off Bluetooth");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_scan_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == 16908332) {
            drawerLayout.openDrawer(Gravity.LEFT);
            return true;
        } else if (id == R.id.action_scan) {
            Intent graphIntent = new Intent(mContext, NewScanActivity.class);
            graphIntent.putExtra("file_name", getString(R.string.newScan));
            startActivity(graphIntent);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        try {
            mContext.unregisterReceiver(bluetoothStateChangeReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    private void openBluetooth() {

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Local Bluetooth is not available", Toast.LENGTH_SHORT).show();
        }

        if (!bluetoothAdapter.isEnabled()) {
            boolean state = bluetoothAdapter.enable();
            if(state){
                Toast.makeText(mContext, "Turning on Bluetooth. . .", Toast.LENGTH_SHORT).show();
                bluetoothState = true;
            }
            else
                Toast.makeText(mContext, "Failed to turn on Bluetooth", Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(mContext, "Bluetooth is on", Toast.LENGTH_SHORT).show();
        }
    }

    private void closeBluetooth() {

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Local Bluetooth is not available", Toast.LENGTH_SHORT).show();
        }

        if (bluetoothAdapter.isEnabled()) {
            boolean state = bluetoothAdapter.disable();
            if(state){
                Toast.makeText(mContext, "Turn off Bluetooth successfully", Toast.LENGTH_SHORT).show();
                bluetoothState = false;
            }

            else
                Toast.makeText(mContext, "Failed to turn off Bluetooth", Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(mContext, "Bluetooth is off", Toast.LENGTH_SHORT).show();
        }
    }

    private BroadcastReceiver bluetoothStateChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
            if (blueState == BluetoothAdapter.STATE_ON || blueState == BluetoothAdapter.STATE_OFF){
                bluetoothState = bluetoothAdapter.isEnabled();
            }
        }
    };

}