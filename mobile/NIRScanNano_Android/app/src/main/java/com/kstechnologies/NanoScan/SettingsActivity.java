package com.kstechnologies.NanoScan;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.ToggleButton;
import com.kstechnologies.nirscannanolibrary.SettingsManager;
import com.kstechnologies.nirscannanolibrary.SettingsManager.SharedPreferencesKeys;

public class SettingsActivity extends Activity {
    /* access modifiers changed from: private */
    public static Context mContext;
    /* access modifiers changed from: private */
    public AlertDialog alertDialog;
    /* access modifiers changed from: private */
    public Button btn_forget;
    /* access modifiers changed from: private */
    public Button btn_set;
    /* access modifiers changed from: private */
    public String preferredNano;
    private ToggleButton tb_spatial;
    private ToggleButton tb_temp;
    /* access modifiers changed from: private */
    public TextView tv_pref_nano;
    private TextView tv_version;

    /* access modifiers changed from: protected */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        mContext = this;
        ActionBar ab = getActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }
        this.tv_version = (TextView) findViewById(R.id.tv_version);
        this.tb_temp = (ToggleButton) findViewById(R.id.tb_temp);
        this.tb_spatial = (ToggleButton) findViewById(R.id.tb_spatial);
        this.btn_set = (Button) findViewById(R.id.btn_set);
        this.btn_forget = (Button) findViewById(R.id.btn_forget);
        this.tv_pref_nano = (TextView) findViewById(R.id.tv_pref_nano);
    }

    public void onResume() {
        super.onResume();
        this.preferredNano = SettingsManager.getStringPref(mContext, SharedPreferencesKeys.preferredDevice, null);
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = pInfo.versionName;
            int versionCode = pInfo.versionCode;
            this.tv_version.setText(getString(R.string.version, new Object[]{version, Integer.valueOf(versionCode)}));
        } catch (NameNotFoundException e) {
            this.tv_version.setText("");
        }
        this.tb_temp.setChecked(SettingsManager.getBooleanPref(this, SharedPreferencesKeys.tempUnits, false));
        this.tb_spatial.setChecked(SettingsManager.getBooleanPref(this, SharedPreferencesKeys.spatialFreq, true));
        this.tb_temp.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                SettingsManager.storeBooleanPref(SettingsActivity.mContext, SharedPreferencesKeys.tempUnits, b);
            }
        });
        this.tb_spatial.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                SettingsManager.storeBooleanPref(SettingsActivity.mContext, SharedPreferencesKeys.spatialFreq, b);
            }
        });
        this.btn_set.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                SettingsActivity.this.startActivity(new Intent(SettingsActivity.mContext, ScanActivity.class));
            }
        });
        if (this.preferredNano == null) {
            this.btn_forget.setEnabled(false);
        } else {
            this.btn_forget.setEnabled(true);
        }
        this.btn_forget.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                if (SettingsActivity.this.preferredNano != null) {
                    SettingsActivity.this.confirmationDialog(SettingsActivity.this.preferredNano);
                }
            }
        });
        if (this.preferredNano != null) {
            this.btn_set.setVisibility(View.INVISIBLE);
            this.tv_pref_nano.setText(SettingsManager.getStringPref(mContext, SharedPreferencesKeys.preferredDevice, null));
            this.tv_pref_nano.setVisibility(View.VISIBLE);
            return;
        }
        this.btn_set.setVisibility(View.VISIBLE);
        this.tv_pref_nano.setVisibility(View.INVISIBLE);
    }

    public void onDestroy() {
        super.onDestroy();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == 16908332) {
            finish();
        } else if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void confirmationDialog(String mac) {
        Builder alertDialogBuilder = new Builder(mContext);
        alertDialogBuilder.setTitle(mContext.getResources().getString(R.string.nano_confirmation_title));
        alertDialogBuilder.setMessage(mContext.getResources().getString(R.string.nano_forget_msg, new Object[]{mac}));
        alertDialogBuilder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                SettingsActivity.this.alertDialog.dismiss();
                SettingsManager.storeStringPref(SettingsActivity.mContext, SharedPreferencesKeys.preferredDevice, null);
                SettingsActivity.this.btn_set.setVisibility(View.VISIBLE);
                SettingsActivity.this.tv_pref_nano.setVisibility(View.INVISIBLE);
                SettingsActivity.this.btn_forget.setEnabled(false);
            }
        });
        alertDialogBuilder.setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                SettingsActivity.this.alertDialog.dismiss();
            }
        });
        this.alertDialog = alertDialogBuilder.create();
        this.alertDialog.show();
    }
}
