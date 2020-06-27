package com.gaohui.nano;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.kstechnologies.nirscannanolibrary.SettingsManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SettingsActivity extends BaseActivity {

    private ToggleButton tb_temp;
    private ToggleButton tb_spatial;
    private ToggleButton tb_refCal;
    private Button btn_set;
    private Button btn_forget;
    private AlertDialog alertDialog;
    private TextView tv_pref_nano;
    private String preferredNano;
    private boolean tb_refCal_flag;

    private static Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mContext = this;

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        this.setSupportActionBar(toolbar);
        ActionBar actionBar = this.getSupportActionBar();
        actionBar.setTitle("Set up");
        actionBar.setDisplayHomeAsUpEnabled(true);

        tb_temp = (ToggleButton) findViewById(R.id.tb_temp);
        tb_spatial = (ToggleButton) findViewById(R.id.tb_spatial);
        tb_refCal = (ToggleButton) findViewById(R.id.tb_refCal);
        btn_set = (Button) findViewById(R.id.btn_set);
        btn_forget = (Button) findViewById(R.id.btn_forget);
        tv_pref_nano = (TextView) findViewById(R.id.tv_pref_nano);

        tb_temp.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                SettingsManager.storeBooleanPref(mContext, SettingsManager.SharedPreferencesKeys.tempUnits, b);
            }
        });

        tb_spatial.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                SettingsManager.storeBooleanPref(mContext, SettingsManager.SharedPreferencesKeys.spatialFreq, b);
            }
        });

        tb_refCal.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b && tb_refCal_flag)
                    Toast.makeText(mContext, "Once you choose this way, please make sure your Nano has been calibrated correctly, otherwise please choose local", Toast.LENGTH_LONG).show();
                SettingsManager.storeBooleanPref(mContext, "ReferenceCalibration", b);
            }
        });

        btn_set.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(mContext, ScanActivity.class));
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();

        preferredNano = SettingsManager.getStringPref(mContext, SettingsManager.SharedPreferencesKeys.preferredDevice, null);

        tb_temp.setChecked(SettingsManager.getBooleanPref(this, SettingsManager.SharedPreferencesKeys.tempUnits, SettingsManager.CELSIUS));
        tb_spatial.setChecked(SettingsManager.getBooleanPref(this, SettingsManager.SharedPreferencesKeys.spatialFreq, SettingsManager.WAVELENGTH));
        tb_refCal_flag = false;
        tb_refCal.setChecked(SettingsManager.getBooleanPref(this, "ReferenceCalibration", false));
        tb_refCal_flag = true;

        if(preferredNano == null){
            btn_forget.setEnabled(false);
        }else{
            btn_forget.setEnabled(true);
        }
        btn_forget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (preferredNano != null) {
                    confirmationDialog(preferredNano);
                }
            }
        });

        if (preferredNano != null) {
            btn_set.setVisibility(View.INVISIBLE);
            tv_pref_nano.setText(SettingsManager.getStringPref(mContext, SettingsManager.SharedPreferencesKeys.preferredDevice, null));
            tv_pref_nano.setVisibility(View.VISIBLE);
        } else {
            btn_set.setVisibility(View.VISIBLE);
            tv_pref_nano.setVisibility(View.INVISIBLE);
        }

    }

    /*
     * When the activity is destroyed, make a call to super class
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /*
     * Inflate the options menu
     * In this case, there is no menu and only an up indicator,
     * so the function should always return true.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    /*
     * Handle the selection of a menu item.
     * In this case, there is are two items, the up indicator, and the settings button
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == android.R.id.home) {
            this.finish();
        }

        else if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Function for displaying the dialog to confirm clearing the stored Nano
     * @param mac the mac address of the stored Nano
     */
    public void confirmationDialog(String mac) {

        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
        alertDialogBuilder.setTitle(mContext.getResources().getString(R.string.nano_confirmation_title));
        alertDialogBuilder.setMessage(mContext.getResources().getString(R.string.nano_forget_msg, mac));

        alertDialogBuilder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                alertDialog.dismiss();
                SettingsManager.storeStringPref(mContext, SettingsManager.SharedPreferencesKeys.preferredDevice, null);
                btn_set.setVisibility(View.VISIBLE);
                tv_pref_nano.setVisibility(View.INVISIBLE);
                btn_forget.setEnabled(false);
            }
        });

        alertDialogBuilder.setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                alertDialog.dismiss();
            }
        });
        alertDialogBuilder.setCancelable(false);

        alertDialog = alertDialogBuilder.create();
        alertDialog.show();

    }

}
