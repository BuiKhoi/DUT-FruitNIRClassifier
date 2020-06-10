package com.gaohui.nano;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;//蓝牙低功耗相关
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
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.design.widget.TabLayout;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.gaohui.utils.DBUtil;
import com.gaohui.utils.NanoUtil;
import com.gaohui.utils.ThemeManageUtil;
import com.gaohui.utils.TimeUtil;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.kyleduo.switchbutton.SwitchButton;
import com.opencsv.CSVWriter;
import com.kstechnologies.nirscannanolibrary.KSTNanoSDK;
import com.kstechnologies.nirscannanolibrary.SettingsManager;


public class NewScanActivity extends BaseActivity {

    private static Context mContext;
    private static final String TAG = "gaohui";

    private ProgressDialog barProgressDialog;

    private ViewPager mViewPager;
    private String fileName;
    private ArrayList<String> mXValues;

    private ArrayList<Entry> mIntensityFloat;
    private ArrayList<Entry> mAbsorbanceFloat;
    private ArrayList<Entry> mReflectanceFloat;
    private ArrayList<Float> mWavelengthFloat;

    private final BroadcastReceiver notifyCompleteReceiver = new notifyCompleteReceiver();
    private final BroadcastReceiver requestCalCoeffReceiver = new requestCalCoeffReceiver();
    private final BroadcastReceiver requestCalMatrixReceiver = new requestCalMatrixReceiver();
    private final BroadcastReceiver refReadyReceiver = new refReadyReceiver();
    private final BroadcastReceiver scanStartedReceiver = new ScanStartedReceiver();
    private final BroadcastReceiver scanDataReadyReceiver = new scanDataReadyReceiver();
    private final BroadcastReceiver disconnReceiver = new DisconnReceiver();

    private final IntentFilter scanDataReadyFilter = new IntentFilter(KSTNanoSDK.SCAN_DATA);
    private final IntentFilter refReadyFilter = new IntentFilter(KSTNanoSDK.REF_CONF_DATA);
    private final IntentFilter notifyCompleteFilter = new IntentFilter(KSTNanoSDK.ACTION_NOTIFY_DONE);
    private final IntentFilter requestCalCoeffFilter = new IntentFilter(KSTNanoSDK.ACTION_REQ_CAL_COEFF);
    private final IntentFilter requestCalMatrixFilter = new IntentFilter(KSTNanoSDK.ACTION_REQ_CAL_MATRIX);
    private final IntentFilter disconnFilter = new IntentFilter(KSTNanoSDK.ACTION_GATT_DISCONNECTED);
    private final IntentFilter scanStartedFilter = new IntentFilter(NanoBLEService.ACTION_SCAN_STARTED);

    private final BroadcastReceiver scanConfReceiver = new ScanConfReceiver();
    private final IntentFilter scanConfFilter = new IntentFilter("ActiveConfigBroadcast");
//    private final IntentFilter scanConfFilter = new IntentFilter("NewScan:getActiveConfig");
//    private final IntentFilter scanConfFilter = new IntentFilter("ActiveConfigBroadcast");

    private ProgressBar calProgress;
    private KSTNanoSDK.ScanResults results;
    private Button btn_scan;

    private NanoBLEService mNanoBLEService;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private Handler mHandler;
    private static final String DEVICE_NAME = "NIRScanNano";
    private boolean connected = false;
    private AlertDialog alertDialog;
    private String preferredDevice;
    private KSTNanoSDK.ScanConfiguration activeConf;

    private Menu mMenu;
    public static final String API_BASE_URL = "http://10.10.42.32:8000/predict/";

    TextView tvOutput;
    ImageView imgFruit;
    int imgId;
    String fruit;
    String inss = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_scan);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        this.setSupportActionBar(toolbar);
        ActionBar ab = this.getSupportActionBar();
        tvOutput = (TextView) findViewById(R.id.tv_output);
        imgFruit = (ImageView) findViewById(R.id.img_fruit);

        mContext = this;
        calProgress = (ProgressBar) findViewById(R.id.calProgress);//进度条
        calProgress.setVisibility(View.VISIBLE);
        connected = false;


        //从intent 中获取文件名并设置
        Intent intent = getIntent();
        fileName = intent.getStringExtra("file_name");

        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);//设置返回箭头
            ab.setTitle(getString(R.string.new_scan));

            mViewPager = (ViewPager) findViewById(R.id.viewpager);
            mViewPager.setOffscreenPageLimit(2);
        }

        btn_scan = (Button) findViewById(R.id.btn_scan);
        btn_scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SettingsManager.storeStringPref(mContext, SettingsManager.SharedPreferencesKeys.prefix, "");
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(KSTNanoSDK.START_SCAN));
                calProgress.setVisibility(View.VISIBLE);
                btn_scan.setText(getString(R.string.scanning));
            }
        });

        btn_scan.setClickable(false);//此时按钮不可用

        //绑定到service 。这将开启这个service，并且会调用start command 方法
        Intent gattServiceIntent = new Intent(this, NanoBLEService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        //注册所有需要的广播接收器
        LocalBroadcastManager.getInstance(mContext).registerReceiver(scanDataReadyReceiver, scanDataReadyFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(refReadyReceiver, refReadyFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(notifyCompleteReceiver, notifyCompleteFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(requestCalCoeffReceiver, requestCalCoeffFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(requestCalMatrixReceiver, requestCalMatrixFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(disconnReceiver, disconnFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(scanConfReceiver, scanConfFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(scanStartedReceiver, scanStartedFilter);
    }

    @Override
    public void onResume() {
        Log.i(TAG, "onResume()");
        super.onResume();

        //初始化 view pager
        CustomPagerAdapter pagerAdapter = new CustomPagerAdapter(this);
        mViewPager.setAdapter(pagerAdapter);
        mViewPager.invalidate();

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs); //获取TabLayout
        tabLayout.setupWithViewPager(mViewPager); //将TabLayout 和ViewPager 关联

        mXValues = new ArrayList<>();
        mIntensityFloat = new ArrayList<>();
        mAbsorbanceFloat = new ArrayList<>();
        mReflectanceFloat = new ArrayList<>();
        mWavelengthFloat = new ArrayList<>();
    }

    /**
     * 当activity 被销毁时，取消所有的broadcast receivers 注册，移除回掉并且保存所有用户偏好
     */
    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy()");
        super.onDestroy();
        unbindService(mServiceConnection);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(scanDataReadyReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(refReadyReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(notifyCompleteReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(requestCalCoeffReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(requestCalMatrixReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(disconnReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(scanConfReceiver);

        mHandler.removeCallbacksAndMessages(null);
    }

    /**
     * 添加右上角的配置按钮
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "onCreateOptionsMenu()");
        getMenuInflater().inflate(R.menu.menu_new_scan, menu);
        mMenu = menu;
        mMenu.findItem(R.id.action_config).setEnabled(false);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "onOptionsItemSelected()");

        int id = item.getItemId();

        if (id == R.id.action_config) {
            Intent configureIntent = new Intent(mContext, ConfigureActivity.class);
            startActivity(configureIntent);
        }else if (id == android.R.id.home) {
            if (connected == true){
                confirmDialog();
                Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                vibrator.vibrate(500);//震动1s
            }else{
                finish();
            }
        }

        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK ) {
            if (connected == true){
                confirmDialog();
                Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                vibrator.vibrate(500);//震动1s
            }else{
                finish();
            }
        }

        return false;

    }

    /**
     * 点击删除的时候弹出这个确认对话框
     */
    private void confirmDialog() {
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(mContext, R.style.DialogTheme);
        builder.setMessage("This operation will disconnect the connection! Continue?");
        builder.setTitle("Warning");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                Toast.makeText(mContext, R.string.nano_disconnected, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setCancelable(false).show();
    }

    /**
     * Pager enum to control tab tile and layout resource
     */
    public enum CustomPagerEnum {

        REFLECTANCE(R.string.reflectance, R.layout.page_graph_reflectance),
        ABSORBANCE(R.string.absorbance, R.layout.page_graph_absorbance),
        INTENSITY(R.string.intensity, R.layout.page_graph_intensity);

        private final int mTitleResId;
        private final int mLayoutResId;

        CustomPagerEnum(int titleResId, int layoutResId) {
            mTitleResId = titleResId;
            mLayoutResId = layoutResId;
        }

        public int getLayoutResId() {
            return mLayoutResId;
        }

    }

    /**
     * 定义一个adapter 用来处理页面内容
     */
    public class CustomPagerAdapter extends PagerAdapter {

        private final Context mContext;

        public CustomPagerAdapter(Context context) {
            mContext = context;
        }

        /*
         * 这个方法，return一个对象，这个对象表明了PagerAdapter 适配器选择哪个对象放在当前的ViewPager中
         */
        @Override
        public Object instantiateItem(ViewGroup collection, int position) {
            CustomPagerEnum customPagerEnum = CustomPagerEnum.values()[position];
            LayoutInflater inflater = LayoutInflater.from(mContext);
            ViewGroup layout = (ViewGroup) inflater.inflate(customPagerEnum.getLayoutResId(), collection, false);
            collection.addView(layout);
            LineChart mChart = null;

            if (customPagerEnum.getLayoutResId() == R.layout.page_graph_intensity) {
                mChart = (LineChart) layout.findViewById(R.id.lineChartInt);
                setData(mChart, mXValues, mIntensityFloat);
            } else if (customPagerEnum.getLayoutResId() == R.layout.page_graph_absorbance) {
                mChart = (LineChart) layout.findViewById(R.id.lineChartAbs);
                setData(mChart, mXValues, mAbsorbanceFloat);
            } else if (customPagerEnum.getLayoutResId() == R.layout.page_graph_reflectance) {
                mChart = (LineChart) layout.findViewById(R.id.lineChartRef);
                setData(mChart, mXValues, mReflectanceFloat);
            }

            mChart.setDrawGridBackground(false);
            mChart.setDescription("");
            mChart.setTouchEnabled(true);
            mChart.setDragEnabled(true);
            mChart.setScaleEnabled(true);
            mChart.setPinchZoom(true);

            // X 轴限制线
            LimitLine llXAxis = new LimitLine(10f, "Index 10");
            llXAxis.setLineWidth(4f);
            llXAxis.enableDashedLine(10f, 10f, 0f);
            llXAxis.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_BOTTOM);
            llXAxis.setTextSize(10f);

            XAxis xAxis = mChart.getXAxis();
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

            YAxis leftAxis = mChart.getAxisLeft();
            leftAxis.removeAllLimitLines(); //重置所有限制线，以避免重叠线
            leftAxis.setStartAtZero(false); //纵坐标从0 开始绘制
            leftAxis.enableGridDashedLine(10f, 10f, 0f);
            leftAxis.setDrawLimitLinesBehindData(true);

            mChart.setAutoScaleMinMaxEnabled(true);
            mChart.getAxisRight().setEnabled(false);
            mChart.animateX(2500, Easing.EasingOption.EaseInOutQuart);

            Legend l = mChart.getLegend();
            l.setForm(Legend.LegendForm.LINE);

            if (customPagerEnum.getLayoutResId() == R.layout.page_graph_intensity) {
                leftAxis.setStartAtZero(true);//强度图从0 开始绘制
            }

            return layout;
        }

        /*
         * 这个方法，是从ViewGroup中移出当前View
         */
        @Override
        public void destroyItem(ViewGroup collection, int position, Object view) {
            collection.removeView((View) view);//删除页卡
        }

        /*
         * 这个方法，是获取当前窗体界面数，这里是3个
         */
        @Override
        public int getCount() {
            return CustomPagerEnum.values().length;
        }

        /*
         * 用于判断是否由对象生成界面
         */
        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.reflectance);
                case 1:
                    return getString(R.string.absorbance);
                case 2:
                    return getString(R.string.intensity);
            }
            return null;
        }

    }

    /**
     * 为一个指定的图表设置X 轴，Y 轴数据
     * @param mChart 为那个图标更新数据
     * @param xValues 横坐标是String类型
     * @param yValues 纵坐标是数字类型
     */
    private void setData(LineChart mChart, ArrayList<String> xValues, ArrayList<Entry> yValues) {

        int themeColor = ThemeManageUtil.getCurrentThemeColor();

        LineDataSet lineDataSet = new LineDataSet(yValues, fileName);

        //设置线型为这样的 Set the line type to this "- - - - - -"
        lineDataSet.enableDashedLine(10f, 5f, 0f);
        lineDataSet.enableDashedHighlightLine(10f, 5f, 0f);
        lineDataSet.setColor(themeColor); //设置线的颜色
        lineDataSet.setCircleColor(themeColor); //设置圆圈的颜色
        lineDataSet.setLineWidth(1f); //设置线宽
        lineDataSet.setCircleSize(3f); //设置圆圈大小
        lineDataSet.setDrawCircleHole(true);
        lineDataSet.setValueTextSize(9f);
        lineDataSet.setFillAlpha(65); //设置填充的透明度
        lineDataSet.setFillColor(themeColor); //设置填充颜色
        lineDataSet.setDrawFilled(true); //设置是否填充

        ArrayList<LineDataSet> dataSets = new ArrayList<>();
        dataSets.add(lineDataSet);

        LineData data = new LineData(xValues, dataSets);//设置横坐标数据值，和纵坐标及数据样式

        mChart.setData(data);

        mChart.setMaxVisibleValueCount(20);
    }

    /**
     * 自定义图表类型 枚举
     */
    public enum ChartType {
        REFLECTANCE,
        ABSORBANCE,
        INTENSITY
    }

    /**
     * 当数据接收完毕后会触发这个广播接收器，然后依次执行以下几步：
     * 1. 旋转进度条消失
     * 2. 获取扫描数据，扫描类型，扫描时间
     * 3. 获取参考校准对象{@link KSTNanoSDK.ReferenceCalibration}，可以从本地获取，也可以从Nano 中读取
     * 4. 根据扫描数据和参考校准对象，获得扫描结果对象{@link KSTNanoSDK.ScanResults}，是利用JNI 调用C 语言函数
     * 5. 根据扫描结果对象计算出吸收率，反射率，强度
     * 6. 画图
     * 7. 保存数据（如果设置了）
     * 8. 继续扫描（如果设置了）
     *
     */
    public class scanDataReadyReceiver extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "gaohuixx:scanDataReadyReceiver.onReceive()");
            calProgress.setVisibility(View.GONE);
            btn_scan.setText(getString(R.string.scan));
            byte[] scanData = intent.getByteArrayExtra(KSTNanoSDK.EXTRA_DATA);

//            String scanType = intent.getStringExtra(KSTNanoSDK.EXTRA_SCAN_TYPE);//好像不应该从Intent中获取扫描类型
//            String scanType = activeConf.getScanType();//从activeConf 中获取扫描类型，只有三种：Hadamard，Column，Slew
            String scanType = SettingsManager.getStringPref(mContext, SettingsManager.SharedPreferencesKeys.scanConfiguration, "Column 1");//从数据库中获取当前配置名称

            String scanDate = intent.getStringExtra(KSTNanoSDK.EXTRA_SCAN_DATE);//17031800200720
            scanDate = TimeUtil.convertTime(scanDate);


            boolean b = SettingsManager.getBooleanPref(mContext, "ReferenceCalibration", false);//获取设置
            KSTNanoSDK.ReferenceCalibration ref = null;

            if (b){
                ref = KSTNanoSDK.ReferenceCalibration.currentCalibration.get(0);
            }else{
                try {
                    ref = NanoUtil.getRefCal(getResources().getAssets().open("refcals"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            results = KSTNanoSDK.KSTNanoSDK_dlpSpecScanInterpReference(scanData, ref.getRefCalCoefficients(), ref.getRefCalMatrix());

            mXValues.clear();
            mIntensityFloat.clear();
            mAbsorbanceFloat.clear();
            mReflectanceFloat.clear();
            mWavelengthFloat.clear();

            int index;
            int[] ins = new int[results.getLength()];
            float sum = 0;
            float u = 0;
            float std = 0;

            for (index = 0; index < results.getLength(); index++) {
                mXValues.add(String.format("%.02f", KSTNanoSDK.ScanResults.getSpatialFreq(mContext, results.getWavelength()[index])));
                mIntensityFloat.add(new Entry((float) results.getUncalibratedIntensity()[index], index));
                mAbsorbanceFloat.add(new Entry((-1) * (float) Math.log10((double) results.getUncalibratedIntensity()[index] / (double) results.getIntensity()[index]), index));
                mReflectanceFloat.add(new Entry((float) results.getUncalibratedIntensity()[index] / results.getIntensity()[index], index));
                mWavelengthFloat.add((float) results.getWavelength()[index]);
                ins[index] = results.getUncalibratedIntensity()[index];
                sum += ins[index];
            }
            u = sum/results.getLength();
            float d = 0;
            for (index = 0; index < results.getLength(); index++){
                d += (ins[index] - u)*(ins[index] - u);
            }
            d = (float) Math.sqrt(d/results.getLength());

            inss = "";
            for (int i=0; i < results.getLength(); i++){
                float value = (float)Math.round(((ins[i] - u)/d)*1000)/1000;

                if(Math.abs(value) >= 1){
                    inss = (inss == "")? inss + value : inss + "x" + value;
                }
                else{
                    String x = "" + value;
                    x = x.replaceFirst("0", "");
                    inss = (inss == "")? inss + x : inss + "x" + x;
                }
            }


            final RequestQueue requestQueue = Volley.newRequestQueue(mContext);
            StringRequest objectRequest = new StringRequest(
                    Request.Method.POST,
                    API_BASE_URL,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            response=response.substring(1,response.length()-1);
                            fruit=response.substring(0,1).toUpperCase()+response.substring(1).replace("_"," ");
                            tvOutput.setText(fruit);
                            imgId= getResources().getIdentifier(response, "drawable", getPackageName());
                            imgFruit.setImageResource(imgId);
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Toast.makeText(getApplicationContext(),"Error occurred",Toast.LENGTH_SHORT).show();
                        }
                    }
            ){
                @Override
                public String getBodyContentType(){
                    return "application/json;";
                }

                @Override
                public byte[] getBody() {
                    String param = "{\"intensity\": \"" + inss+"\"}";
                    return param.getBytes();
                }
            };
            requestQueue.add(objectRequest);

            float minWavelength = mWavelengthFloat.get(0);
            float maxWavelength = mWavelengthFloat.get(0);

            for (Float f : mWavelengthFloat) {
                if (f < minWavelength) minWavelength = f;
                if (f > maxWavelength) maxWavelength = f;
            }


            mViewPager.setAdapter(mViewPager.getAdapter());//开始绘制光谱图
            mViewPager.invalidate();


            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyMMddHHmmss", java.util.Locale.getDefault());
            String ts = simpleDateFormat.format(new Date());
            ActionBar ab = getSupportActionBar();
            if (ab != null) {
                ab.setTitle("Nano" + ts);
            }


            int id = DBUtil.queryScanConfbyName("Column 1");

            String experimentName = "Nano";
            String sampleName = "sample";

            String wavelength = NanoUtil.convertFloatResultToText(mWavelengthFloat);
            String reflectance = NanoUtil.convertEntryResultToText(mReflectanceFloat);
            String absorbance = NanoUtil.convertEntryResultToText(mAbsorbanceFloat);
            String intensity = NanoUtil.convertEntryResultToText(mIntensityFloat);
            Log.i(TAG, "id= " + id);

            if(id > 0){
                DBUtil.insertExperimentResult(experimentName, sampleName, wavelength, reflectance, absorbance, intensity, id);
            }else {
                String configName = activeConf.getConfigName();
                int numOfScan = activeConf.getNumRepeats();
                int numOfSection = activeConf.getSlewNumSections();
                if (numOfSection == 0)
                    numOfSection = 1;

                int scanConfigId = DBUtil.insertScanConfig(configName, numOfScan, numOfSection);

                for (int i = 0; i < numOfSection; i++){
                    int sectionNo = i + 1;
                    String method = null;
                    if (activeConf.getSectionScanType()[i] == 0)
                        method = "Column";
                    else
                        method = "Hadamard";
                    int start = activeConf.getSectionWavelengthStartNm()[i];
                    int end = activeConf.getSectionWavelengthEndNm()[i];
                    int width = activeConf.getSectionWidthPx()[i];
                    int digitalResolution = activeConf.getSectionNumPatterns()[i];
                    int exposureTime = activeConf.getSectionExposureTime()[i];

                    DBUtil.insertSectionConfig(scanConfigId, sectionNo, method, start, end, width, digitalResolution, exposureTime);

                }

                DBUtil.insertExperimentResult(experimentName, sampleName, wavelength, reflectance, absorbance, intensity, scanConfigId);

            }

            SettingsManager.storeStringPref(mContext, SettingsManager.SharedPreferencesKeys.prefix, "");
        }
    }

    public class refReadyReceiver extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {
            byte[] refCoeff = intent.getByteArrayExtra(KSTNanoSDK.EXTRA_REF_COEF_DATA);
            byte[] refMatrix = intent.getByteArrayExtra(KSTNanoSDK.EXTRA_REF_MATRIX_DATA);
            ArrayList<KSTNanoSDK.ReferenceCalibration> refCal = new ArrayList<>();
            refCal.add(new KSTNanoSDK.ReferenceCalibration(refCoeff, refMatrix));
            KSTNanoSDK.ReferenceCalibration.writeRefCalFile(mContext, refCal);
            calProgress.setVisibility(View.GONE);

            barProgressDialog.dismiss();
            btn_scan.setClickable(true);
            mMenu.findItem(R.id.action_config).setEnabled(true);

            Intent configureIntent = new Intent(mContext, ScanConfActivity.class);
            configureIntent.putExtra("title", "Please select a scan configuration");
            startActivity(configureIntent);

        }
    }

    public class ScanStartedReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            calProgress.setVisibility(View.VISIBLE);
            btn_scan.setText(getString(R.string.scanning));
        }
    }

    public class notifyCompleteReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(KSTNanoSDK.SET_TIME));
        }
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mNanoBLEService = ((NanoBLEService.LocalBinder) service).getService();
            if (!mNanoBLEService.initialize()) {
                finish();
            }

            final BluetoothManager bluetoothManager =
                    (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothAdapter = bluetoothManager.getAdapter();
            mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
            if(mBluetoothLeScanner == null){
                finish();
                Toast.makeText(NewScanActivity.this, "Please turn on Bluetooth and try again", Toast.LENGTH_SHORT).show();
            }
            mHandler = new Handler();
            if (SettingsManager.getStringPref(mContext, SettingsManager.SharedPreferencesKeys.preferredDevice, null) != null) {
                preferredDevice = SettingsManager.getStringPref(mContext, SettingsManager.SharedPreferencesKeys.preferredDevice, null);
                scanPreferredLeDevice(true);
            } else {
                scanLeDevice(true);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mNanoBLEService = null;
        }
    };

    private final ScanCallback mLeScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice device = result.getDevice();
            String name = device.getName();
            if (name != null && connected == false) {
                if (device.getName().equals(DEVICE_NAME)) {
                    Log.i(TAG, "mLeScanCallback：The corresponding Nano is searched, ready to connect");
                    mNanoBLEService.connect(device.getAddress());
                    connected = true;
                    //scanLeDevice(false);
                }
            }
        }
    };

    private final ScanCallback mPreferredLeScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice device = result.getDevice();
            String name = device.getName();
            if (name != null && connected == false) {
                if (device.getName().equals(DEVICE_NAME)) {
                    Log.i(TAG, "mPreferredLeScanCallback：The corresponding Nano is searched, ready to connect");
                    if (device.getAddress().equals(preferredDevice)) {
                        Log.i(TAG, "The Nano found is a prefered Nano");
                        mNanoBLEService.connect(device.getAddress());
                        Log.i(TAG, "Connected to prefered Nano");
                        connected = true;
//                        scanPreferredLeDevice(false);
                    }
                }
            }
        }
    };

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(mBluetoothLeScanner != null) {
                        mBluetoothLeScanner.stopScan(mLeScanCallback);
                        if (!connected) {
                            Log.i(TAG, "Scanned, but not connected");
                            notConnectedDialog();
                        }
                    }
                }
            }, NanoBLEService.SCAN_PERIOD);
            if(mBluetoothLeScanner != null) {
                mBluetoothLeScanner.startScan(mLeScanCallback);
            }else{
                finish();
                Toast.makeText(NewScanActivity.this, "Please turn on Bluetooth and try again", Toast.LENGTH_SHORT).show();
            }
        } else {
            mBluetoothLeScanner.stopScan(mLeScanCallback);
        }
    }

    private void scanPreferredLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBluetoothLeScanner.stopScan(mPreferredLeScanCallback);
                    if (!connected) {

                        scanLeDevice(true);
                    }
                }
            }, NanoBLEService.SCAN_PERIOD);
            if(mBluetoothLeScanner != null) {
                mBluetoothLeScanner.startScan(mPreferredLeScanCallback);
            }else{
                finish();
                Toast.makeText(NewScanActivity.this, "Please turn on Bluetooth and try again", Toast.LENGTH_SHORT).show();
            }
        } else {
            mBluetoothLeScanner.stopScan(mPreferredLeScanCallback);
        }
    }

    private void notConnectedDialog() {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext, R.style.DialogTheme);
        alertDialogBuilder.setTitle(mContext.getResources().getString(R.string.not_connected_title));
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setMessage(mContext.getResources().getString(R.string.not_connected_message));

        alertDialogBuilder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                alertDialog.dismiss();
                finish();
            }
        });
        alertDialogBuilder.setCancelable(false);

        alertDialog = alertDialogBuilder.create();
        alertDialog.show();

    }

    public class requestCalCoeffReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            intent.getIntExtra(KSTNanoSDK.EXTRA_REF_CAL_COEFF_SIZE, 0);
            Boolean size = intent.getBooleanExtra(KSTNanoSDK.EXTRA_REF_CAL_COEFF_SIZE_PACKET, false);
            if (size) {
                calProgress.setVisibility(View.INVISIBLE);
                barProgressDialog = new ProgressDialog(NewScanActivity.this, R.style.DialogTheme);

                barProgressDialog.setTitle(getString(R.string.dl_ref_cal));
                barProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                barProgressDialog.setProgress(0);
                barProgressDialog.setMax(intent.getIntExtra(KSTNanoSDK.EXTRA_REF_CAL_COEFF_SIZE, 0));
                barProgressDialog.setCancelable(false);
                barProgressDialog.show();
            } else {
                barProgressDialog.setProgress(barProgressDialog.getProgress() + intent.getIntExtra(KSTNanoSDK.EXTRA_REF_CAL_COEFF_SIZE, 0));
            }
        }
    }

    public class requestCalMatrixReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            intent.getIntExtra(KSTNanoSDK.EXTRA_REF_CAL_MATRIX_SIZE, 0);
            Boolean size = intent.getBooleanExtra(KSTNanoSDK.EXTRA_REF_CAL_MATRIX_SIZE_PACKET, false);
            if (size) {
                barProgressDialog.dismiss();
                barProgressDialog = new ProgressDialog(NewScanActivity.this, R.style.DialogTheme);

                barProgressDialog.setTitle(getString(R.string.dl_cal_matrix));
                barProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                barProgressDialog.setProgress(0);
                barProgressDialog.setMax(intent.getIntExtra(KSTNanoSDK.EXTRA_REF_CAL_MATRIX_SIZE, 0));
                barProgressDialog.setCancelable(false);
                barProgressDialog.show();
            } else {
                barProgressDialog.setProgress(barProgressDialog.getProgress() + intent.getIntExtra(KSTNanoSDK.EXTRA_REF_CAL_MATRIX_SIZE, 0));
            }
            if (barProgressDialog.getProgress() == barProgressDialog.getMax()) {
//                LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(KSTNanoSDK.REQUEST_ACTIVE_CONF));
            }
        }
    }

    private class ScanConfReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            KSTNanoSDK.ScanConfiguration scanConf = KSTNanoSDK.KSTNanoSDK_dlpSpecScanReadConfiguration(intent.getByteArrayExtra(KSTNanoSDK.EXTRA_DATA));
            activeConf = scanConf;
            SettingsManager.storeStringPref(mContext, SettingsManager.SharedPreferencesKeys.scanConfiguration, scanConf.getConfigName());
        }
    }

    public class DisconnReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(mContext, R.string.nano_disconnected, Toast.LENGTH_SHORT).show();
            finish();
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            vibrator.vibrate(1000);
        }
    }

}
