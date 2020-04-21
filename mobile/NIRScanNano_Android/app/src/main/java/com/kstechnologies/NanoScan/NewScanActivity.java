package com.kstechnologies.NanoScan;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.internal.view.SupportMenu;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.SimpleOnPageChangeListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import com.github.mikephil.charting.animation.Easing.EasingOption;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend.LegendForm;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.LimitLine.LimitLabelPosition;
import com.github.mikephil.charting.components.XAxis.XAxisPosition;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.kstechnologies.NanoScan.NanoBLEService.LocalBinder;
import com.kstechnologies.nirscannanolibrary.KSTNanoSDK;
import com.kstechnologies.nirscannanolibrary.KSTNanoSDK.ReferenceCalibration;
import com.kstechnologies.nirscannanolibrary.KSTNanoSDK.ScanConfiguration;
import com.kstechnologies.nirscannanolibrary.KSTNanoSDK.ScanResults;
import com.kstechnologies.nirscannanolibrary.SettingsManager;
import com.kstechnologies.nirscannanolibrary.SettingsManager.SharedPreferencesKeys;
import com.opencsv.CSVWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class NewScanActivity extends Activity {
    private static final String DEVICE_NAME = "NIRScanNano";
    /* access modifiers changed from: private */
    public static Context mContext;
    /* access modifiers changed from: private */
    public ScanConfiguration activeConf;
    /* access modifiers changed from: private */
    public AlertDialog alertDialog;
    /* access modifiers changed from: private */
    public ProgressDialog barProgressDialog;
    /* access modifiers changed from: private */
    public ToggleButton btn_continuous;
    /* access modifiers changed from: private */
    public ToggleButton btn_os;
    /* access modifiers changed from: private */
    public Button btn_scan;
    private ToggleButton btn_sd;
    /* access modifiers changed from: private */
    public ProgressBar calProgress;
    /* access modifiers changed from: private */
    public boolean connected;
    private final IntentFilter disconnFilter = new IntentFilter(KSTNanoSDK.ACTION_GATT_DISCONNECTED);
    private final BroadcastReceiver disconnReceiver = new DisconnReceiver();
    private String fileName;
    /* access modifiers changed from: private */
    public EditText filePrefix;
    private LinearLayout ll_conf;
    /* access modifiers changed from: private */
    public ArrayList<Entry> mAbsorbanceFloat;
    /* access modifiers changed from: private */
    public BluetoothAdapter mBluetoothAdapter;
    /* access modifiers changed from: private */
    public BluetoothLeScanner mBluetoothLeScanner;
    /* access modifiers changed from: private */
    public Handler mHandler;
    /* access modifiers changed from: private */
    public ArrayList<Entry> mIntensityFloat;
    /* access modifiers changed from: private */
    public final ScanCallback mLeScanCallback = new ScanCallback() {
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice device = result.getDevice();
            if (device.getName() != null && device.getName().equals(NewScanActivity.DEVICE_NAME)) {
                NewScanActivity.this.mNanoBLEService.connect(device.getAddress());
                NewScanActivity.this.connected = true;
                NewScanActivity.this.scanLeDevice(false);
            }
        }
    };
    /* access modifiers changed from: private */
    public Menu mMenu;
    /* access modifiers changed from: private */
    public NanoBLEService mNanoBLEService;
    /* access modifiers changed from: private */
    public final ScanCallback mPreferredLeScanCallback = new ScanCallback() {
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice device = result.getDevice();
            if (device.getName() != null && device.getName().equals(NewScanActivity.DEVICE_NAME) && device.getAddress().equals(NewScanActivity.this.preferredDevice)) {
                NewScanActivity.this.mNanoBLEService.connect(device.getAddress());
                NewScanActivity.this.connected = true;
                NewScanActivity.this.scanPreferredLeDevice(false);
            }
        }
    };
    /* access modifiers changed from: private */
    public ArrayList<Entry> mReflectanceFloat;
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            NewScanActivity.this.mNanoBLEService = ((LocalBinder) service).getService();
            if (!NewScanActivity.this.mNanoBLEService.initialize()) {
                NewScanActivity.this.finish();
            }
            NewScanActivity.this.mBluetoothAdapter = ((BluetoothManager) NewScanActivity.this.getSystemService(BLUETOOTH_SERVICE)).getAdapter();
            NewScanActivity.this.mBluetoothLeScanner = NewScanActivity.this.mBluetoothAdapter.getBluetoothLeScanner();
            if (NewScanActivity.this.mBluetoothLeScanner == null) {
                NewScanActivity.this.finish();
                Toast.makeText(NewScanActivity.this, "Please ensure Bluetooth is enabled and try again", Toast.LENGTH_SHORT).show();
            }
            NewScanActivity.this.mHandler = new Handler();
            if (SettingsManager.getStringPref(NewScanActivity.mContext, SharedPreferencesKeys.preferredDevice, null) != null) {
                NewScanActivity.this.preferredDevice = SettingsManager.getStringPref(NewScanActivity.mContext, SharedPreferencesKeys.preferredDevice, null);
                NewScanActivity.this.scanPreferredLeDevice(true);
                return;
            }
            NewScanActivity.this.scanLeDevice(true);
        }

        public void onServiceDisconnected(ComponentName componentName) {
            NewScanActivity.this.mNanoBLEService = null;
        }
    };
    /* access modifiers changed from: private */
    public ViewPager mViewPager;
    /* access modifiers changed from: private */
    public ArrayList<Float> mWavelengthFloat;
    /* access modifiers changed from: private */
    public ArrayList<String> mXValues;
    private final IntentFilter notifyCompleteFilter = new IntentFilter(KSTNanoSDK.ACTION_NOTIFY_DONE);
    private final BroadcastReceiver notifyCompleteReceiver = new notifyCompleteReceiver();
    /* access modifiers changed from: private */
    public String preferredDevice;
    private final IntentFilter refReadyFilter = new IntentFilter(KSTNanoSDK.REF_CONF_DATA);
    private final BroadcastReceiver refReadyReceiver = new refReadyReceiver();
    private final IntentFilter requestCalCoeffFilter = new IntentFilter(KSTNanoSDK.ACTION_REQ_CAL_COEFF);
    private final BroadcastReceiver requestCalCoeffReceiver = new requestCalCoeffReceiver();
    private final IntentFilter requestCalMatrixFilter = new IntentFilter(KSTNanoSDK.ACTION_REQ_CAL_MATRIX);
    private final BroadcastReceiver requestCalMatrixReceiver = new requestCalMatrixReceiver();
    /* access modifiers changed from: private */
    public ScanResults results;
    private final IntentFilter scanConfFilter = new IntentFilter(KSTNanoSDK.SCAN_CONF_DATA);
    private final BroadcastReceiver scanConfReceiver = new ScanConfReceiver();
    private final IntentFilter scanDataReadyFilter = new IntentFilter(KSTNanoSDK.SCAN_DATA);
    private final BroadcastReceiver scanDataReadyReceiver = new scanDataReadyReceiver();
    private final IntentFilter scanStartedFilter = new IntentFilter(NanoBLEService.ACTION_SCAN_STARTED);
    private final BroadcastReceiver scanStartedReceiver = new ScanStartedReceiver();
    /* access modifiers changed from: private */
    public TextView tv_scan_conf;

    public enum ChartType {
        REFLECTANCE,
        ABSORBANCE,
        INTENSITY
    }

    public class CustomPagerAdapter extends PagerAdapter {
        private final Context mContext;

        public CustomPagerAdapter(Context context) {
            this.mContext = context;
        }

        public Object instantiateItem(ViewGroup collection, int position) {
            CustomPagerEnum customPagerEnum = CustomPagerEnum.values()[position];
            ViewGroup layout = (ViewGroup) LayoutInflater.from(this.mContext).inflate(customPagerEnum.getLayoutResId(), collection, false);
            collection.addView(layout);
            if (customPagerEnum.getLayoutResId() == R.layout.page_graph_intensity) {
                LineChart mChart = (LineChart) layout.findViewById(R.id.lineChartInt);
                mChart.setDrawGridBackground(false);
                mChart.setDescription("");
                mChart.setTouchEnabled(true);
                mChart.setDragEnabled(true);
                mChart.setScaleEnabled(true);
                mChart.setPinchZoom(true);
                LimitLine llXAxis = new LimitLine(10.0f, "Index 10");
                llXAxis.setLineWidth(4.0f);
                llXAxis.enableDashedLine(10.0f, 10.0f, 0.0f);
                llXAxis.setLabelPosition(LimitLabelPosition.RIGHT_BOTTOM);
                llXAxis.setTextSize(10.0f);
                mChart.getXAxis().setPosition(XAxisPosition.BOTTOM);
                YAxis leftAxis = mChart.getAxisLeft();
                leftAxis.removeAllLimitLines();
                mChart.setAutoScaleMinMaxEnabled(true);
                leftAxis.setStartAtZero(true);
                leftAxis.enableGridDashedLine(10.0f, 10.0f, 0.0f);
                leftAxis.setDrawLimitLinesBehindData(true);
                mChart.getAxisRight().setEnabled(false);
                NewScanActivity.this.setData(mChart, NewScanActivity.this.mXValues, NewScanActivity.this.mIntensityFloat, ChartType.INTENSITY);
                mChart.animateX(2500, EasingOption.EaseInOutQuart);
                mChart.getLegend().setForm(LegendForm.LINE);
                mChart.getLegend().setEnabled(false);
            } else if (customPagerEnum.getLayoutResId() == R.layout.page_graph_absorbance) {
                LineChart mChart2 = (LineChart) layout.findViewById(R.id.lineChartAbs);
                mChart2.setDrawGridBackground(false);
                mChart2.setDescription("");
                mChart2.setTouchEnabled(true);
                mChart2.setDragEnabled(true);
                mChart2.setScaleEnabled(true);
                mChart2.setPinchZoom(true);
                LimitLine llXAxis2 = new LimitLine(10.0f, "Index 10");
                llXAxis2.setLineWidth(4.0f);
                llXAxis2.enableDashedLine(10.0f, 10.0f, 0.0f);
                llXAxis2.setLabelPosition(LimitLabelPosition.RIGHT_BOTTOM);
                llXAxis2.setTextSize(10.0f);
                mChart2.getXAxis().setPosition(XAxisPosition.BOTTOM);
                YAxis leftAxis2 = mChart2.getAxisLeft();
                leftAxis2.removeAllLimitLines();
                mChart2.setAutoScaleMinMaxEnabled(true);
                leftAxis2.setStartAtZero(false);
                leftAxis2.enableGridDashedLine(10.0f, 10.0f, 0.0f);
                leftAxis2.setDrawLimitLinesBehindData(true);
                mChart2.getAxisRight().setEnabled(false);
                NewScanActivity.this.setData(mChart2, NewScanActivity.this.mXValues, NewScanActivity.this.mAbsorbanceFloat, ChartType.ABSORBANCE);
                mChart2.animateX(2500, EasingOption.EaseInOutQuart);
                mChart2.getLegend().setForm(LegendForm.LINE);
                mChart2.getLegend().setEnabled(false);
            } else if (customPagerEnum.getLayoutResId() == R.layout.page_graph_reflectance) {
                LineChart mChart3 = (LineChart) layout.findViewById(R.id.lineChartRef);
                mChart3.setDrawGridBackground(false);
                mChart3.setDescription("");
                mChart3.setTouchEnabled(true);
                mChart3.setDragEnabled(true);
                mChart3.setScaleEnabled(true);
                mChart3.setPinchZoom(true);
                LimitLine llXAxis3 = new LimitLine(10.0f, "Index 10");
                llXAxis3.setLineWidth(4.0f);
                llXAxis3.enableDashedLine(10.0f, 10.0f, 0.0f);
                llXAxis3.setLabelPosition(LimitLabelPosition.RIGHT_BOTTOM);
                llXAxis3.setTextSize(10.0f);
                mChart3.getXAxis().setPosition(XAxisPosition.BOTTOM);
                YAxis leftAxis3 = mChart3.getAxisLeft();
                leftAxis3.removeAllLimitLines();
                mChart3.setAutoScaleMinMaxEnabled(true);
                leftAxis3.setStartAtZero(false);
                leftAxis3.enableGridDashedLine(10.0f, 10.0f, 0.0f);
                leftAxis3.setDrawLimitLinesBehindData(true);
                mChart3.getAxisRight().setEnabled(false);
                NewScanActivity.this.setData(mChart3, NewScanActivity.this.mXValues, NewScanActivity.this.mReflectanceFloat, ChartType.REFLECTANCE);
                mChart3.animateX(2500, EasingOption.EaseInOutQuart);
                mChart3.getLegend().setForm(LegendForm.LINE);
                mChart3.getLegend().setEnabled(false);
            }
            return layout;
        }

        public void destroyItem(ViewGroup collection, int position, Object view) {
            collection.removeView((View) view);
        }

        public int getCount() {
            return CustomPagerEnum.values().length;
        }

        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return NewScanActivity.this.getString(R.string.reflectance);
                case 1:
                    return NewScanActivity.this.getString(R.string.absorbance);
                case 2:
                    return NewScanActivity.this.getString(R.string.intensity);
                default:
                    return null;
            }
        }
    }

    public enum CustomPagerEnum {
        REFLECTANCE(R.string.reflectance, R.layout.page_graph_reflectance),
        ABSORBANCE(R.string.absorbance, R.layout.page_graph_absorbance),
        INTENSITY(R.string.intensity, R.layout.page_graph_intensity);

        private final int mLayoutResId;
        private final int mTitleResId;

        private CustomPagerEnum(int titleResId, int layoutResId) {
            this.mTitleResId = titleResId;
            this.mLayoutResId = layoutResId;
        }

        public int getLayoutResId() {
            return this.mLayoutResId;
        }
    }

    public class DisconnReceiver extends BroadcastReceiver {
        public DisconnReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            Toast.makeText(NewScanActivity.mContext, R.string.nano_disconnected, Toast.LENGTH_SHORT).show();
            NewScanActivity.this.finish();
        }
    }

    private class ScanConfReceiver extends BroadcastReceiver {
        private ScanConfReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            byte[] smallArray = intent.getByteArrayExtra(KSTNanoSDK.EXTRA_DATA);
            byte[] addArray = new byte[(smallArray.length * 3)];
            byte[] largeArray = new byte[(smallArray.length + addArray.length)];
            System.arraycopy(smallArray, 0, largeArray, 0, smallArray.length);
            System.arraycopy(addArray, 0, largeArray, smallArray.length, addArray.length);
            Log.w("_JNI", "largeArray Size: " + largeArray.length);
            ScanConfiguration scanConf = KSTNanoSDK.KSTNanoSDK_dlpSpecScanReadConfiguration(intent.getByteArrayExtra(KSTNanoSDK.EXTRA_DATA));
            NewScanActivity.this.activeConf = scanConf;
            NewScanActivity.this.barProgressDialog.dismiss();
            NewScanActivity.this.btn_scan.setClickable(true);
            NewScanActivity.this.btn_scan.setBackgroundColor(ContextCompat.getColor(NewScanActivity.mContext, R.color.kst_red));
            NewScanActivity.this.mMenu.findItem(R.id.action_settings).setEnabled(true);
            SettingsManager.storeStringPref(NewScanActivity.mContext, SharedPreferencesKeys.scanConfiguration, scanConf.getConfigName());
            NewScanActivity.this.tv_scan_conf.setText(scanConf.getConfigName());
        }
    }

    public class ScanStartedReceiver extends BroadcastReceiver {
        public ScanStartedReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            NewScanActivity.this.calProgress.setVisibility(View.VISIBLE);
            NewScanActivity.this.btn_scan.setText(NewScanActivity.this.getString(R.string.scanning));
        }
    }

    public class notifyCompleteReceiver extends BroadcastReceiver {
        public notifyCompleteReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            LocalBroadcastManager.getInstance(NewScanActivity.mContext).sendBroadcast(new Intent(KSTNanoSDK.SET_TIME));
        }
    }

    public class refReadyReceiver extends BroadcastReceiver {
        public refReadyReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            byte[] refCoeff = intent.getByteArrayExtra(KSTNanoSDK.EXTRA_REF_COEF_DATA);
            byte[] refMatrix = intent.getByteArrayExtra(KSTNanoSDK.EXTRA_REF_MATRIX_DATA);
            ArrayList<ReferenceCalibration> refCal = new ArrayList<>();
            refCal.add(new ReferenceCalibration(refCoeff, refMatrix));
            ReferenceCalibration.writeRefCalFile(NewScanActivity.mContext, refCal);
            NewScanActivity.this.calProgress.setVisibility(View.GONE);
        }
    }

    public class requestCalCoeffReceiver extends BroadcastReceiver {
        public requestCalCoeffReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            intent.getIntExtra(KSTNanoSDK.EXTRA_REF_CAL_COEFF_SIZE, 0);
            if (Boolean.valueOf(intent.getBooleanExtra(KSTNanoSDK.EXTRA_REF_CAL_COEFF_SIZE_PACKET, false)).booleanValue()) {
                NewScanActivity.this.calProgress.setVisibility(View.INVISIBLE);
                NewScanActivity.this.barProgressDialog = new ProgressDialog(NewScanActivity.this);
                NewScanActivity.this.barProgressDialog.setTitle(NewScanActivity.this.getString(R.string.dl_ref_cal));
                NewScanActivity.this.barProgressDialog.setProgressStyle(1);
                NewScanActivity.this.barProgressDialog.setProgress(0);
                NewScanActivity.this.barProgressDialog.setMax(intent.getIntExtra(KSTNanoSDK.EXTRA_REF_CAL_COEFF_SIZE, 0));
                NewScanActivity.this.barProgressDialog.setCancelable(false);
                NewScanActivity.this.barProgressDialog.show();
                return;
            }
            NewScanActivity.this.barProgressDialog.setProgress(NewScanActivity.this.barProgressDialog.getProgress() + intent.getIntExtra(KSTNanoSDK.EXTRA_REF_CAL_COEFF_SIZE, 0));
        }
    }

    public class requestCalMatrixReceiver extends BroadcastReceiver {
        public requestCalMatrixReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            intent.getIntExtra(KSTNanoSDK.EXTRA_REF_CAL_MATRIX_SIZE, 0);
            if (Boolean.valueOf(intent.getBooleanExtra(KSTNanoSDK.EXTRA_REF_CAL_MATRIX_SIZE_PACKET, false)).booleanValue()) {
                NewScanActivity.this.barProgressDialog.dismiss();
                NewScanActivity.this.barProgressDialog = new ProgressDialog(NewScanActivity.this);
                NewScanActivity.this.barProgressDialog.setTitle(NewScanActivity.this.getString(R.string.dl_cal_matrix));
                NewScanActivity.this.barProgressDialog.setProgressStyle(1);
                NewScanActivity.this.barProgressDialog.setProgress(0);
                NewScanActivity.this.barProgressDialog.setMax(intent.getIntExtra(KSTNanoSDK.EXTRA_REF_CAL_MATRIX_SIZE, 0));
                NewScanActivity.this.barProgressDialog.setCancelable(false);
                NewScanActivity.this.barProgressDialog.show();
            } else {
                NewScanActivity.this.barProgressDialog.setProgress(NewScanActivity.this.barProgressDialog.getProgress() + intent.getIntExtra(KSTNanoSDK.EXTRA_REF_CAL_MATRIX_SIZE, 0));
            }
            if (NewScanActivity.this.barProgressDialog.getProgress() == NewScanActivity.this.barProgressDialog.getMax()) {
                LocalBroadcastManager.getInstance(NewScanActivity.mContext).sendBroadcast(new Intent(KSTNanoSDK.REQUEST_ACTIVE_CONF));
            }
        }
    }

    public class scanDataReadyReceiver extends BroadcastReceiver {
        public scanDataReadyReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            String scanType;
            NewScanActivity.this.calProgress.setVisibility(View.GONE);
            NewScanActivity.this.btn_scan.setText(NewScanActivity.this.getString(R.string.scan));
            byte[] scanData = intent.getByteArrayExtra(KSTNanoSDK.EXTRA_DATA);
            String scanType2 = intent.getStringExtra(KSTNanoSDK.EXTRA_SCAN_TYPE);
            String scanDate = intent.getStringExtra(KSTNanoSDK.EXTRA_SCAN_DATE);
            ReferenceCalibration ref = (ReferenceCalibration) ReferenceCalibration.currentCalibration.get(0);
            NewScanActivity.this.results = KSTNanoSDK.KSTNanoSDK_dlpSpecScanInterpReference(scanData, ref.getRefCalCoefficients(), ref.getRefCalMatrix());
            NewScanActivity.this.mXValues.clear();
            NewScanActivity.this.mIntensityFloat.clear();
            NewScanActivity.this.mAbsorbanceFloat.clear();
            NewScanActivity.this.mReflectanceFloat.clear();
            NewScanActivity.this.mWavelengthFloat.clear();
            for (int index = 0; index < NewScanActivity.this.results.getLength(); index++) {
                NewScanActivity.this.mXValues.add(String.format("%.02f", new Object[]{Double.valueOf(ScanResults.getSpatialFreq(NewScanActivity.mContext, NewScanActivity.this.results.getWavelength()[index]))}));
                NewScanActivity.this.mIntensityFloat.add(new Entry((float) NewScanActivity.this.results.getUncalibratedIntensity()[index], index));
                NewScanActivity.this.mAbsorbanceFloat.add(new Entry(-1.0f * ((float) Math.log10(((double) NewScanActivity.this.results.getUncalibratedIntensity()[index]) / ((double) NewScanActivity.this.results.getIntensity()[index]))), index));
                NewScanActivity.this.mReflectanceFloat.add(new Entry(((float) NewScanActivity.this.results.getUncalibratedIntensity()[index]) / ((float) NewScanActivity.this.results.getIntensity()[index]), index));
                NewScanActivity.this.mWavelengthFloat.add(Float.valueOf((float) NewScanActivity.this.results.getWavelength()[index]));
            }
            float minWavelength = ((Float) NewScanActivity.this.mWavelengthFloat.get(0)).floatValue();
            float maxWavelength = ((Float) NewScanActivity.this.mWavelengthFloat.get(0)).floatValue();
            Iterator it = NewScanActivity.this.mWavelengthFloat.iterator();
            while (it.hasNext()) {
                Float f = (Float) it.next();
                if (f.floatValue() < minWavelength) {
                    minWavelength = f.floatValue();
                }
                if (f.floatValue() > maxWavelength) {
                    maxWavelength = f.floatValue();
                }
            }
            float minAbsorbance = ((Entry) NewScanActivity.this.mAbsorbanceFloat.get(0)).getVal();
            float maxAbsorbance = ((Entry) NewScanActivity.this.mAbsorbanceFloat.get(0)).getVal();
            Iterator it2 = NewScanActivity.this.mAbsorbanceFloat.iterator();
            while (it2.hasNext()) {
                Entry e = (Entry) it2.next();
                if (e.getVal() < minAbsorbance) {
                    minAbsorbance = e.getVal();
                }
                if (e.getVal() > maxAbsorbance) {
                    maxAbsorbance = e.getVal();
                }
            }
            float minReflectance = ((Entry) NewScanActivity.this.mReflectanceFloat.get(0)).getVal();
            float maxReflectance = ((Entry) NewScanActivity.this.mReflectanceFloat.get(0)).getVal();
            Iterator it3 = NewScanActivity.this.mReflectanceFloat.iterator();
            while (it3.hasNext()) {
                Entry e2 = (Entry) it3.next();
                if (e2.getVal() < minReflectance) {
                    minReflectance = e2.getVal();
                }
                if (e2.getVal() > maxReflectance) {
                    maxReflectance = e2.getVal();
                }
            }
            float minIntensity = ((Entry) NewScanActivity.this.mIntensityFloat.get(0)).getVal();
            float maxIntensity = ((Entry) NewScanActivity.this.mIntensityFloat.get(0)).getVal();
            Iterator it4 = NewScanActivity.this.mIntensityFloat.iterator();
            while (it4.hasNext()) {
                Entry e3 = (Entry) it4.next();
                if (e3.getVal() < minIntensity) {
                    minIntensity = e3.getVal();
                }
                if (e3.getVal() > maxIntensity) {
                    maxIntensity = e3.getVal();
                }
            }
            NewScanActivity.this.mViewPager.setAdapter(NewScanActivity.this.mViewPager.getAdapter());
            NewScanActivity.this.mViewPager.invalidate();
            if (scanType2.equals("00")) {
                scanType = "Column 1";
            } else {
                scanType = "Hadamard";
            }
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("ddMMyyhhmmss", Locale.getDefault());
            String ts = simpleDateFormat.format(new Date());
            ActionBar ab = NewScanActivity.this.getActionBar();
            if (ab != null) {
                if (NewScanActivity.this.filePrefix.getText().toString().equals("")) {
                    ab.setTitle("Nano" + ts);
                } else {
                    ab.setTitle(NewScanActivity.this.filePrefix.getText().toString() + ts);
                }
                ab.setSelectedNavigationItem(0);
            }
            boolean saveOS = NewScanActivity.this.btn_os.isChecked();
            boolean continuous = NewScanActivity.this.btn_continuous.isChecked();
            NewScanActivity.this.writeCSV(ts, NewScanActivity.this.results, saveOS);
            NewScanActivity.this.writeCSVDict(ts, scanType, scanDate, String.valueOf(minWavelength), String.valueOf(maxWavelength), String.valueOf(NewScanActivity.this.results.getLength()), String.valueOf(NewScanActivity.this.results.getLength()), "1", "2.00", saveOS);
            SettingsManager.storeStringPref(NewScanActivity.mContext, SharedPreferencesKeys.prefix, NewScanActivity.this.filePrefix.getText().toString());
            if (continuous) {
                NewScanActivity.this.calProgress.setVisibility(View.VISIBLE);
                NewScanActivity.this.btn_scan.setText(NewScanActivity.this.getString(R.string.scanning));
                LocalBroadcastManager.getInstance(NewScanActivity.mContext).sendBroadcast(new Intent(KSTNanoSDK.SEND_DATA));
            }
        }
    }

    /* access modifiers changed from: protected */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_scan);
        mContext = this;
        this.calProgress = (ProgressBar) findViewById(R.id.calProgress);
        this.calProgress.setVisibility(View.VISIBLE);
        this.connected = false;
        this.ll_conf = (LinearLayout) findViewById(R.id.ll_conf);
        this.ll_conf.setClickable(false);
        this.ll_conf.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                if (NewScanActivity.this.activeConf != null) {
                    Intent activeConfIntent = new Intent(NewScanActivity.mContext, ActiveScanActivity.class);
                    activeConfIntent.putExtra("conf", NewScanActivity.this.activeConf);
                    NewScanActivity.this.startActivity(activeConfIntent);
                }
            }
        });
        this.fileName = getIntent().getStringExtra("file_name");
        ActionBar ab = getActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setTitle(getString(R.string.new_scan));
            ab.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
            this.mViewPager = (ViewPager) findViewById(R.id.viewpager);
            this.mViewPager.setOffscreenPageLimit(2);
            TabListener tl = new TabListener() {
                public void onTabSelected(Tab tab, FragmentTransaction fragmentTransaction) {
                    NewScanActivity.this.mViewPager.setCurrentItem(tab.getPosition());
                }

                public void onTabUnselected(Tab tab, FragmentTransaction fragmentTransaction) {
                }

                public void onTabReselected(Tab tab, FragmentTransaction fragmentTransaction) {
                }
            };
            for (int i = 0; i < 3; i++) {
                ab.addTab(ab.newTab().setText(getResources().getStringArray(R.array.graph_tab_index)[i]).setTabListener(tl));
            }
        }
        this.filePrefix = (EditText) findViewById(R.id.et_prefix);
        this.btn_os = (ToggleButton) findViewById(R.id.btn_saveOS);
        this.btn_sd = (ToggleButton) findViewById(R.id.btn_saveSD);
        this.btn_continuous = (ToggleButton) findViewById(R.id.btn_continuous);
        this.btn_scan = (Button) findViewById(R.id.btn_scan);
        this.tv_scan_conf = (TextView) findViewById(R.id.tv_scan_conf);
        this.btn_os.setChecked(SettingsManager.getBooleanPref(mContext, SharedPreferencesKeys.saveOS, false));
        this.btn_sd.setChecked(SettingsManager.getBooleanPref(mContext, SharedPreferencesKeys.saveSD, false));
        this.btn_continuous.setChecked(SettingsManager.getBooleanPref(mContext, SharedPreferencesKeys.continuousScan, false));
        this.btn_sd.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                SettingsManager.storeBooleanPref(NewScanActivity.mContext, SharedPreferencesKeys.saveSD, b);
            }
        });
        this.btn_scan.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                SettingsManager.storeStringPref(NewScanActivity.mContext, SharedPreferencesKeys.prefix, NewScanActivity.this.filePrefix.getText().toString());
                LocalBroadcastManager.getInstance(NewScanActivity.mContext).sendBroadcast(new Intent(KSTNanoSDK.START_SCAN));
                NewScanActivity.this.calProgress.setVisibility(View.VISIBLE);
                NewScanActivity.this.btn_scan.setText(NewScanActivity.this.getString(R.string.scanning));
            }
        });
        this.btn_scan.setClickable(false);
        this.btn_scan.setBackgroundColor(ContextCompat.getColor(mContext, R.color.btn_unavailable));
        bindService(new Intent(this, NanoBLEService.class), this.mServiceConnection, BIND_AUTO_CREATE);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(this.scanDataReadyReceiver, this.scanDataReadyFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(this.refReadyReceiver, this.refReadyFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(this.notifyCompleteReceiver, this.notifyCompleteFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(this.requestCalCoeffReceiver, this.requestCalCoeffFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(this.requestCalMatrixReceiver, this.requestCalMatrixFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(this.disconnReceiver, this.disconnFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(this.scanConfReceiver, this.scanConfFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(this.scanStartedReceiver, this.scanStartedFilter);
    }

    public void onResume() {
        super.onResume();
        this.mViewPager.setAdapter(new CustomPagerAdapter(this));
        this.mViewPager.invalidate();
        this.tv_scan_conf.setText(SettingsManager.getStringPref(mContext, SharedPreferencesKeys.scanConfiguration, "Column 1"));
        this.mViewPager.setOnPageChangeListener(new SimpleOnPageChangeListener() {
            public void onPageSelected(int position) {
                if (NewScanActivity.this.getActionBar() != null) {
                    NewScanActivity.this.getActionBar().setSelectedNavigationItem(position);
                }
            }
        });
        this.mXValues = new ArrayList<>();
        this.mIntensityFloat = new ArrayList<>();
        this.mAbsorbanceFloat = new ArrayList<>();
        this.mReflectanceFloat = new ArrayList<>();
        this.mWavelengthFloat = new ArrayList<>();
    }

    public void onDestroy() {
        super.onDestroy();
        unbindService(this.mServiceConnection);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(this.scanDataReadyReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(this.refReadyReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(this.notifyCompleteReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(this.requestCalCoeffReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(this.requestCalMatrixReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(this.disconnReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(this.scanConfReceiver);
        this.mHandler.removeCallbacksAndMessages(null);
        SettingsManager.storeBooleanPref(mContext, SharedPreferencesKeys.saveOS, this.btn_os.isChecked());
        SettingsManager.storeBooleanPref(mContext, SharedPreferencesKeys.saveSD, this.btn_sd.isChecked());
        SettingsManager.storeBooleanPref(mContext, SharedPreferencesKeys.continuousScan, this.btn_continuous.isChecked());
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_new_scan, menu);
        this.mMenu = menu;
        this.mMenu.findItem(R.id.action_settings).setEnabled(false);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startActivity(new Intent(mContext, ConfigureActivity.class));
        }
        if (id == 16908332) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    /* access modifiers changed from: private */
    public void setData(LineChart mChart, ArrayList<String> xValues, ArrayList<Entry> yValues, ChartType type) {
        if (type == ChartType.REFLECTANCE) {
            LineDataSet set1 = new LineDataSet(yValues, this.fileName);
            set1.enableDashedLine(10.0f, 5.0f, 0.0f);
            set1.enableDashedHighlightLine(10.0f, 5.0f, 0.0f);
            set1.setColor(ViewCompat.MEASURED_STATE_MASK);
            set1.setCircleColor(SupportMenu.CATEGORY_MASK);
            set1.setLineWidth(1.0f);
            set1.setCircleSize(3.0f);
            set1.setDrawCircleHole(true);
            set1.setValueTextSize(9.0f);
            set1.setFillAlpha(65);
            set1.setFillColor(SupportMenu.CATEGORY_MASK);
            set1.setDrawFilled(true);
            ArrayList<LineDataSet> dataSets = new ArrayList<>();
            dataSets.add(set1);
            mChart.setData(new LineData((List<String>) xValues, (List<LineDataSet>) dataSets));
            mChart.setMaxVisibleValueCount(20);
        } else if (type == ChartType.ABSORBANCE) {
            LineDataSet set12 = new LineDataSet(yValues, this.fileName);
            set12.enableDashedLine(10.0f, 5.0f, 0.0f);
            set12.enableDashedHighlightLine(10.0f, 5.0f, 0.0f);
            set12.setColor(ViewCompat.MEASURED_STATE_MASK);
            set12.setCircleColor(-16711936);
            set12.setLineWidth(1.0f);
            set12.setCircleSize(3.0f);
            set12.setDrawCircleHole(true);
            set12.setValueTextSize(9.0f);
            set12.setFillAlpha(65);
            set12.setFillColor(-16711936);
            set12.setDrawFilled(true);
            ArrayList<LineDataSet> dataSets2 = new ArrayList<>();
            dataSets2.add(set12);
            mChart.setData(new LineData((List<String>) xValues, (List<LineDataSet>) dataSets2));
            mChart.setMaxVisibleValueCount(20);
        } else if (type == ChartType.INTENSITY) {
            LineDataSet set13 = new LineDataSet(yValues, this.fileName);
            set13.enableDashedLine(10.0f, 5.0f, 0.0f);
            set13.enableDashedHighlightLine(10.0f, 5.0f, 0.0f);
            set13.setColor(ViewCompat.MEASURED_STATE_MASK);
            set13.setCircleColor(-16776961);
            set13.setLineWidth(1.0f);
            set13.setCircleSize(3.0f);
            set13.setDrawCircleHole(true);
            set13.setValueTextSize(9.0f);
            set13.setFillAlpha(65);
            set13.setFillColor(-16776961);
            set13.setDrawFilled(true);
            ArrayList<LineDataSet> dataSets3 = new ArrayList<>();
            dataSets3.add(set13);
            mChart.setData(new LineData((List<String>) xValues, (List<LineDataSet>) dataSets3));
            mChart.setMaxVisibleValueCount(20);
        } else {
            LineDataSet set14 = new LineDataSet(yValues, this.fileName);
            set14.enableDashedLine(10.0f, 5.0f, 0.0f);
            set14.enableDashedHighlightLine(10.0f, 5.0f, 0.0f);
            set14.setColor(ViewCompat.MEASURED_STATE_MASK);
            set14.setCircleColor(ViewCompat.MEASURED_STATE_MASK);
            set14.setLineWidth(1.0f);
            set14.setCircleSize(3.0f);
            set14.setDrawCircleHole(true);
            set14.setValueTextSize(9.0f);
            set14.setFillAlpha(65);
            set14.setFillColor(ViewCompat.MEASURED_STATE_MASK);
            set14.setDrawFilled(true);
            ArrayList<LineDataSet> dataSets4 = new ArrayList<>();
            dataSets4.add(set14);
            mChart.setData(new LineData((List<String>) xValues, (List<LineDataSet>) dataSets4));
            mChart.setMaxVisibleValueCount(10);
        }
    }

    /* access modifiers changed from: private */
    public void writeCSV(String currentTime, ScanResults scanResults, boolean saveOS) {
        String prefix = this.filePrefix.getText().toString();
        if (prefix.equals("")) {
            prefix = "Nano";
        }
        if (saveOS) {
            try {
                CSVWriter writer = new CSVWriter(new FileWriter(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + prefix + currentTime + ".csv"), ',', (char) 0);
                List<String[]> data = new ArrayList<>();
                data.add(new String[]{"Wavelength,Intensity,Absorbance,Reflectance"});
                for (int csvIndex = 0; csvIndex < scanResults.getLength(); csvIndex++) {
                    data.add(new String[]{String.valueOf(scanResults.getWavelength()[csvIndex]), String.valueOf(scanResults.getUncalibratedIntensity()[csvIndex]), String.valueOf(-1.0f * ((float) Math.log10(((double) scanResults.getUncalibratedIntensity()[csvIndex]) / ((double) scanResults.getIntensity()[csvIndex])))), String.valueOf(((float) this.results.getUncalibratedIntensity()[csvIndex]) / ((float) this.results.getIntensity()[csvIndex]))});
                }
                writer.writeAll(data);
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /* access modifiers changed from: private */
    public void writeCSVDict(String currentTime, String scanType, String timeStamp, String spectStart, String spectEnd, String numPoints, String resolution, String numAverages, String measTime, boolean saveOS) {
        String prefix = this.filePrefix.getText().toString();
        if (prefix.equals("")) {
            prefix = "Nano";
        }
        if (saveOS) {
            try {
                CSVWriter writer = new CSVWriter(new FileWriter(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + prefix + currentTime + ".dict"));
                List<String[]> data = new ArrayList<>();
                data.add(new String[]{"Method", scanType});
                data.add(new String[]{"Timestamp", timeStamp});
                data.add(new String[]{"Spectral Range Start (nm)", spectStart});
                data.add(new String[]{"Spectral Range End (nm)", spectEnd});
                data.add(new String[]{"Number of Wavelength Points", numPoints});
                data.add(new String[]{"Digital Resolution", resolution});
                data.add(new String[]{"Number of Scans to Average", numAverages});
                data.add(new String[]{"Total Measurement Time (s)", measTime});
                writer.writeAll(data);
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /* access modifiers changed from: private */
    public void scanLeDevice(boolean enable) {
        if (enable) {
            this.mHandler.postDelayed(new Runnable() {
                public void run() {
                    if (NewScanActivity.this.mBluetoothLeScanner != null) {
                        NewScanActivity.this.mBluetoothLeScanner.stopScan(NewScanActivity.this.mLeScanCallback);
                        if (!NewScanActivity.this.connected) {
                            NewScanActivity.this.notConnectedDialog();
                        }
                    }
                }
            }, NanoBLEService.SCAN_PERIOD);
            if (this.mBluetoothLeScanner != null) {
                this.mBluetoothLeScanner.startScan(this.mLeScanCallback);
                return;
            }
            finish();
            Toast.makeText(this, "Please ensure Bluetooth is enabled and try again", Toast.LENGTH_SHORT).show();
            return;
        }
        this.mBluetoothLeScanner.stopScan(this.mLeScanCallback);
    }

    /* access modifiers changed from: private */
    public void scanPreferredLeDevice(boolean enable) {
        if (enable) {
            this.mHandler.postDelayed(new Runnable() {
                public void run() {
                    NewScanActivity.this.mBluetoothLeScanner.stopScan(NewScanActivity.this.mPreferredLeScanCallback);
                    if (!NewScanActivity.this.connected) {
                        NewScanActivity.this.scanLeDevice(true);
                    }
                }
            }, NanoBLEService.SCAN_PERIOD);
            this.mBluetoothLeScanner.startScan(this.mPreferredLeScanCallback);
            return;
        }
        this.mBluetoothLeScanner.stopScan(this.mPreferredLeScanCallback);
    }

    /* access modifiers changed from: private */
    public void notConnectedDialog() {
        Builder alertDialogBuilder = new Builder(mContext);
        alertDialogBuilder.setTitle(mContext.getResources().getString(R.string.not_connected_title));
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setMessage(mContext.getResources().getString(R.string.not_connected_message));
        alertDialogBuilder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                NewScanActivity.this.alertDialog.dismiss();
                NewScanActivity.this.finish();
            }
        });
        this.alertDialog = alertDialogBuilder.create();
        this.alertDialog.show();
    }
}
