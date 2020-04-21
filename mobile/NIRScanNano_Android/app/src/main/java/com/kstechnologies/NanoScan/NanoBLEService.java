package com.kstechnologies.NanoScan;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Build.VERSION;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.kstechnologies.nirscannanolibrary.KSTNanoSDK;
import com.kstechnologies.nirscannanolibrary.KSTNanoSDK.NanoGATT;
import com.kstechnologies.nirscannanolibrary.KSTNanoSDK.NanoGattCharacteristic;
import com.kstechnologies.nirscannanolibrary.SettingsManager;
import com.kstechnologies.nirscannanolibrary.SettingsManager.SharedPreferencesKeys;

import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.UUID;

public class NanoBLEService extends Service {
    public static final String ACTION_SCAN_STARTED = "com.kstechnologies.NanoScan.bluetooth.service.ACTION_SCAN_STARTED";
    public static final long SCAN_PERIOD = 6000;
    private static final String TAG = "__BT_SERVICE";
    private static final boolean debug = true;
    private static BroadcastReceiver mDataReceiver;
    private static BroadcastReceiver mDeleteScanReceiver;
    private static BroadcastReceiver mGetActiveScanConfReceiver;
    private static BroadcastReceiver mInfoRequestReceiver;
    private static BroadcastReceiver mRequestActiveConfReceiver;
    private static BroadcastReceiver mScanConfRequestReceiver;
    private static BroadcastReceiver mSetActiveScanConfReceiver;
    private static BroadcastReceiver mSetTimeReceiver;
    private static BroadcastReceiver mStartScanReceiver;
    private static BroadcastReceiver mStatusRequestReceiver;
    private static BroadcastReceiver mStoredScanRequestReceiver;
    private static BroadcastReceiver mUpdateThresholdReceiver;
    UUID CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    /* access modifiers changed from: private */
    public boolean activeConfRequested = false;
    /* access modifiers changed from: private */
    public int battLevel;
    /* access modifiers changed from: private */
    public String devStatus;
    /* access modifiers changed from: private */
    public String errStatus;
    /* access modifiers changed from: private */
    public String hardwareRev;
    /* access modifiers changed from: private */
    public byte[] humidThresh;
    /* access modifiers changed from: private */
    public float humidity;
    private final IBinder mBinder = new LocalBinder();
    private String mBluetoothDeviceAddress;
    private BluetoothManager mBluetoothManager;
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == 2) {
                Log.i(NanoBLEService.TAG, "Connected to GATT server.");
                Log.i(NanoBLEService.TAG, "Attempting to start service discovery:" + KSTNanoSDK.mBluetoothGatt.discoverServices());
            } else if (newState == 0) {
                String intentAction = KSTNanoSDK.ACTION_GATT_DISCONNECTED;
                NanoBLEService.this.refresh();
                Log.i(NanoBLEService.TAG, "Disconnected from GATT server.");
                NanoBLEService.this.broadcastUpdate(intentAction);
            }
        }

        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            boolean enumerated = KSTNanoSDK.enumerateServices(gatt);
            if (status != 0 || !enumerated) {
                Log.e(NanoBLEService.TAG, "onServicesDiscovered received: " + status);
                return;
            }
            Log.d(NanoBLEService.TAG, "Services discovered:SUCCESS");
            NanoBLEService.this.broadcastUpdate(KSTNanoSDK.ACTION_GATT_SERVICES_DISCOVERED);
            BluetoothGattDescriptor descriptor = NanoGattCharacteristic.mBleGattCharGCISRetRefCalCoefficients.getDescriptor(NanoBLEService.this.CCCD_UUID);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            KSTNanoSDK.mBluetoothGatt.writeDescriptor(descriptor);
        }

        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            Log.d("__onDescriptorWrite", "descriptor: " + descriptor.getUuid() + ". characteristic: " + descriptor.getCharacteristic().getUuid() + ". status: " + status);
            if (descriptor.getCharacteristic().getUuid().compareTo(NanoGATT.GCIS_RET_REF_CAL_COEFF) == 0) {
                Log.d(NanoBLEService.TAG, "Wrote Notify request for GCIS_RET_REF_CAL_COEFF");
                BluetoothGattDescriptor mDescriptor = NanoGattCharacteristic.mBleGattCharGCISRetRefCalMatrix.getDescriptor(NanoBLEService.this.CCCD_UUID);
                mDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                KSTNanoSDK.mBluetoothGatt.writeDescriptor(mDescriptor);
            } else if (descriptor.getCharacteristic().getUuid().compareTo(NanoGATT.GCIS_RET_REF_CAL_MATRIX) == 0) {
                Log.d(NanoBLEService.TAG, "Wrote Notify request for GCIS_RET_REF_CAL_MATRIX");
                BluetoothGattDescriptor mDescriptor2 = NanoGattCharacteristic.mBleGattCharGSDISStartScanNotify.getDescriptor(NanoBLEService.this.CCCD_UUID);
                mDescriptor2.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                KSTNanoSDK.mBluetoothGatt.writeDescriptor(mDescriptor2);
            } else if (descriptor.getCharacteristic().getUuid().compareTo(NanoGATT.GSDIS_START_SCAN) == 0) {
                Log.d(NanoBLEService.TAG, "Wrote Notify request for GSDIS_START_SCAN");
                BluetoothGattDescriptor mDescriptor3 = NanoGattCharacteristic.mBleGattCharGSDISRetScanName.getDescriptor(NanoBLEService.this.CCCD_UUID);
                mDescriptor3.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                KSTNanoSDK.mBluetoothGatt.writeDescriptor(mDescriptor3);
            } else if (descriptor.getCharacteristic().getUuid().compareTo(NanoGATT.GSDIS_RET_SCAN_NAME) == 0) {
                Log.d(NanoBLEService.TAG, "Wrote Notify request for GSDIS_RET_SCAN_NAME");
                BluetoothGattDescriptor mDescriptor4 = NanoGattCharacteristic.mBleGattCharGSDISRetScanType.getDescriptor(NanoBLEService.this.CCCD_UUID);
                mDescriptor4.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                KSTNanoSDK.mBluetoothGatt.writeDescriptor(mDescriptor4);
            } else if (descriptor.getCharacteristic().getUuid().compareTo(NanoGATT.GSDIS_RET_SCAN_TYPE) == 0) {
                Log.d(NanoBLEService.TAG, "Wrote Notify request for GSDIS_RET_SCAN_TYPE");
                BluetoothGattDescriptor mDescriptor5 = NanoGattCharacteristic.mBleGattCharGSDISRetScanDate.getDescriptor(NanoBLEService.this.CCCD_UUID);
                mDescriptor5.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                KSTNanoSDK.mBluetoothGatt.writeDescriptor(mDescriptor5);
            } else if (descriptor.getCharacteristic().getUuid().compareTo(NanoGATT.GSDIS_RET_SCAN_DATE) == 0) {
                Log.d(NanoBLEService.TAG, "Wrote Notify request for GSDIS_RET_SCAN_DATE");
                BluetoothGattDescriptor mDescriptor6 = NanoGattCharacteristic.mBleGattCharGSDISRetPacketFormatVersion.getDescriptor(NanoBLEService.this.CCCD_UUID);
                mDescriptor6.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                KSTNanoSDK.mBluetoothGatt.writeDescriptor(mDescriptor6);
            } else if (descriptor.getCharacteristic().getUuid().compareTo(NanoGATT.GSDIS_RET_PKT_FMT_VER) == 0) {
                Log.d(NanoBLEService.TAG, "Wrote Notify request for GSDIS_RET_PKT_FMT_VER");
                BluetoothGattDescriptor mDescriptor7 = NanoGattCharacteristic.mBleGattCharGSDISRetSerialScanDataStruct.getDescriptor(NanoBLEService.this.CCCD_UUID);
                mDescriptor7.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                KSTNanoSDK.mBluetoothGatt.writeDescriptor(mDescriptor7);
            } else if (descriptor.getCharacteristic().getUuid().compareTo(NanoGATT.GSDIS_RET_SER_SCAN_DATA_STRUCT) == 0) {
                Log.d(NanoBLEService.TAG, "Wrote Notify request for GSDIS_RET_SER_SCAN_DATA_STRUCT");
                BluetoothGattDescriptor mDescriptor8 = NanoGattCharacteristic.mBleGattCharGSCISRetStoredConfList.getDescriptor(NanoBLEService.this.CCCD_UUID);
                mDescriptor8.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                KSTNanoSDK.mBluetoothGatt.writeDescriptor(mDescriptor8);
            } else if (descriptor.getCharacteristic().getUuid().compareTo(NanoGATT.GSCIS_RET_STORED_CONF_LIST) == 0) {
                Log.d(NanoBLEService.TAG, "Wrote Notify request for GSCIS_RET_STORED_CONF_LIST");
                BluetoothGattDescriptor mDescriptor9 = NanoGattCharacteristic.mBleGattCharGSDISSDStoredScanIndicesListData.getDescriptor(NanoBLEService.this.CCCD_UUID);
                mDescriptor9.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                KSTNanoSDK.mBluetoothGatt.writeDescriptor(mDescriptor9);
            } else if (descriptor.getCharacteristic().getUuid().compareTo(NanoGATT.GSDIS_SD_STORED_SCAN_IND_LIST_DATA) == 0) {
                Log.d(NanoBLEService.TAG, "Wrote Notify request for GSDIS_SD_STORED_SCAN_IND_LIST_DATA");
                BluetoothGattDescriptor mDescriptor10 = NanoGattCharacteristic.mBleGattCharGSDISClearScanNotify.getDescriptor(NanoBLEService.this.CCCD_UUID);
                mDescriptor10.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                KSTNanoSDK.mBluetoothGatt.writeDescriptor(mDescriptor10);
            } else if (descriptor.getCharacteristic().getUuid().compareTo(NanoGATT.GSDIS_CLEAR_SCAN) == 0) {
                Log.d(NanoBLEService.TAG, "Wrote Notify request for GSDIS_CLEAR_SCAN");
                BluetoothGattDescriptor mDescriptor11 = NanoGattCharacteristic.mBleGattCharGSCISRetScanConfData.getDescriptor(NanoBLEService.this.CCCD_UUID);
                mDescriptor11.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                KSTNanoSDK.mBluetoothGatt.writeDescriptor(mDescriptor11);
            } else if (descriptor.getCharacteristic().getUuid().compareTo(NanoGATT.GSCIS_RET_SCAN_CONF_DATA) == 0) {
                Log.d(NanoBLEService.TAG, "Wrote Notify request for GSCIS_RET_SCAN_CONF_DATA");
                LocalBroadcastManager.getInstance(NanoBLEService.this.getApplicationContext()).sendBroadcast(new Intent(KSTNanoSDK.ACTION_NOTIFY_DONE));
            }
        }

        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status != 0) {
                return;
            }
            if (characteristic.getUuid().equals(NanoGATT.DIS_MANUF_NAME)) {
                NanoBLEService.this.manufName = new String(characteristic.getValue());
                KSTNanoSDK.getModelNumber();
            } else if (characteristic.getUuid().equals(NanoGATT.DIS_MODEL_NUMBER)) {
                NanoBLEService.this.modelNum = new String(characteristic.getValue());
                KSTNanoSDK.getSerialNumber();
            } else if (characteristic.getUuid().equals(NanoGATT.DIS_SERIAL_NUMBER)) {
                NanoBLEService.this.serialNum = new String(characteristic.getValue());
                KSTNanoSDK.getHardwareRev();
            } else if (characteristic.getUuid().equals(NanoGATT.DIS_HW_REV)) {
                NanoBLEService.this.hardwareRev = new String(characteristic.getValue());
                KSTNanoSDK.getFirmwareRev();
            } else if (characteristic.getUuid().equals(NanoGATT.DIS_TIVA_FW_REV)) {
                NanoBLEService.this.tivaRev = new String(characteristic.getValue());
                KSTNanoSDK.getSpectrumCRev();
            } else if (characteristic.getUuid().equals(NanoGATT.DIS_SPECC_REV)) {
                NanoBLEService.this.spectrumRev = new String(characteristic.getValue());
                Intent intent = new Intent(KSTNanoSDK.ACTION_INFO);
                intent.putExtra(KSTNanoSDK.EXTRA_MANUF_NAME, NanoBLEService.this.manufName);
                intent.putExtra(KSTNanoSDK.EXTRA_MODEL_NUM, NanoBLEService.this.modelNum);
                intent.putExtra(KSTNanoSDK.EXTRA_SERIAL_NUM, NanoBLEService.this.serialNum);
                intent.putExtra(KSTNanoSDK.EXTRA_HW_REV, NanoBLEService.this.hardwareRev);
                intent.putExtra(KSTNanoSDK.EXTRA_TIVA_REV, NanoBLEService.this.tivaRev);
                intent.putExtra(KSTNanoSDK.EXTRA_SPECTRUM_REV, NanoBLEService.this.spectrumRev);
                LocalBroadcastManager.getInstance(NanoBLEService.this.getApplicationContext()).sendBroadcast(intent);
            } else if (characteristic.getUuid().equals(NanoGATT.BAS_BATT_LVL)) {
                byte[] data = characteristic.getValue();
                StringBuilder stringBuilder = new StringBuilder(data.length);
                for (byte byteChar : data) {
                    stringBuilder.append(String.format("%02X", new Object[]{Byte.valueOf(byteChar)}));
                }
                Log.d(NanoBLEService.TAG, "batt level:" + stringBuilder.toString());
                NanoBLEService.this.battLevel = data[0];
                KSTNanoSDK.getTemp();
            } else if (characteristic.getUuid().equals(NanoGATT.GGIS_TEMP_MEASUREMENT)) {
                byte[] data2 = characteristic.getValue();
                StringBuilder stringBuilder2 = new StringBuilder(data2.length);
                for (byte byteChar2 : data2) {
                    stringBuilder2.append(String.format("%02X", new Object[]{Byte.valueOf(byteChar2)}));
                }
                Log.d(NanoBLEService.TAG, "temp level string:" + stringBuilder2.toString());
                NanoBLEService.this.temp = ((float) ((data2[1] << 8) | (data2[0] & 255))) / 100.0f;
                Log.d(NanoBLEService.TAG, "temp level int:" + NanoBLEService.this.temp);
                KSTNanoSDK.getHumidity();
            } else if (characteristic.getUuid().equals(NanoGATT.GGIS_HUMID_MEASUREMENT)) {
                byte[] data3 = characteristic.getValue();
                StringBuilder stringBuilder3 = new StringBuilder(data3.length);
                for (byte byteChar3 : data3) {
                    stringBuilder3.append(String.format("%02X", new Object[]{Byte.valueOf(byteChar3)}));
                }
                Log.d(NanoBLEService.TAG, "humid level:" + stringBuilder3.toString());
                NanoBLEService.this.humidity = ((float) ((data3[1] << 8) | (data3[0] & 255))) / 100.0f;
                KSTNanoSDK.getDeviceStatus();
            } else if (characteristic.getUuid().equals(NanoGATT.GGIS_DEV_STATUS)) {
                byte[] data4 = characteristic.getValue();
                StringBuilder stringBuilder4 = new StringBuilder(data4.length);
                for (byte byteChar4 : data4) {
                    stringBuilder4.append(String.format("%02X", new Object[]{Byte.valueOf(byteChar4)}));
                }
                Log.d(NanoBLEService.TAG, "dev status:" + stringBuilder4.toString());
                NanoBLEService.this.devStatus = stringBuilder4.toString();
                KSTNanoSDK.getErrorStatus();
            } else if (characteristic.getUuid().equals(NanoGATT.GGIS_ERR_STATUS)) {
                byte[] data5 = characteristic.getValue();
                StringBuilder stringBuilder5 = new StringBuilder(data5.length);
                for (byte byteChar5 : data5) {
                    stringBuilder5.append(String.format("%02X", new Object[]{Byte.valueOf(byteChar5)}));
                }
                Log.d(NanoBLEService.TAG, "error status:" + stringBuilder5.toString());
                NanoBLEService.this.errStatus = stringBuilder5.toString();
                Intent intent2 = new Intent(KSTNanoSDK.ACTION_STATUS);
                intent2.putExtra(KSTNanoSDK.EXTRA_BATT, NanoBLEService.this.battLevel);
                intent2.putExtra(KSTNanoSDK.EXTRA_TEMP, NanoBLEService.this.temp);
                intent2.putExtra(KSTNanoSDK.EXTRA_HUMID, NanoBLEService.this.humidity);
                intent2.putExtra(KSTNanoSDK.EXTRA_DEV_STATUS, NanoBLEService.this.devStatus);
                intent2.putExtra(KSTNanoSDK.EXTRA_ERR_STATUS, NanoBLEService.this.errStatus);
                LocalBroadcastManager.getInstance(NanoBLEService.this.getApplicationContext()).sendBroadcast(intent2);
            } else if (characteristic.getUuid().equals(NanoGATT.GSCIS_NUM_STORED_CONF)) {
                byte[] data6 = characteristic.getValue();
                NanoBLEService.this.scanConfIndex = 0;
                NanoBLEService.this.scanConfIndexSize = (data6[1] << 8) | (data6[0] & 255);
                Intent scanConfSizeIntent = new Intent(KSTNanoSDK.SCAN_CONF_SIZE);
                scanConfSizeIntent.putExtra(KSTNanoSDK.EXTRA_CONF_SIZE, NanoBLEService.this.scanConfIndexSize);
                LocalBroadcastManager.getInstance(NanoBLEService.this.getApplicationContext()).sendBroadcast(scanConfSizeIntent);
                Log.d(NanoBLEService.TAG, "Num stored scan configs:" + NanoBLEService.this.scanConfIndexSize);
                KSTNanoSDK.requestStoredConfigurationList();
            } else if (characteristic.getUuid().equals(NanoGATT.GSDIS_NUM_SD_STORED_SCANS)) {
                byte[] data7 = characteristic.getValue();
                NanoBLEService.this.storedSDScanSize = (data7[1] << 8) | (data7[0] & 255);
                Log.d(NanoBLEService.TAG, "Num stored SD scans:" + NanoBLEService.this.storedSDScanSize);
                Intent sdScanSizeIntent = new Intent(KSTNanoSDK.SD_SCAN_SIZE);
                sdScanSizeIntent.putExtra(KSTNanoSDK.EXTRA_INDEX_SIZE, NanoBLEService.this.storedSDScanSize);
                LocalBroadcastManager.getInstance(NanoBLEService.this.getApplicationContext()).sendBroadcast(sdScanSizeIntent);
                KSTNanoSDK.requestScanIndicesList();
            } else if (characteristic.getUuid().equals(NanoGATT.GSCIS_ACTIVE_SCAN_CONF)) {
                byte[] data8 = characteristic.getValue();
                if (!NanoBLEService.this.activeConfRequested) {
                    StringBuilder stringBuilder6 = new StringBuilder(data8.length);
                    for (byte byteChar6 : data8) {
                        stringBuilder6.append(String.format("%02X", new Object[]{Byte.valueOf(byteChar6)}));
                    }
                    Log.d(NanoBLEService.TAG, "Active scan conf index:" + stringBuilder6.toString());
                    Intent sendActiveConfIntent = new Intent(KSTNanoSDK.SEND_ACTIVE_CONF);
                    sendActiveConfIntent.putExtra(KSTNanoSDK.EXTRA_ACTIVE_CONF, data8);
                    LocalBroadcastManager.getInstance(NanoBLEService.this.getApplicationContext()).sendBroadcast(sendActiveConfIntent);
                    return;
                }
                byte[] confIndex = {data8[0], data8[1]};
                Log.d(NanoBLEService.TAG, "Writing request for scan conf at index:" + confIndex[0] + "-" + confIndex[1]);
                KSTNanoSDK.requestScanConfiguration(confIndex);
            }
        }

        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.d(NanoBLEService.TAG, "onCharacteristic changed for characteristic:" + characteristic.getUuid().toString());
            if (characteristic == NanoGattCharacteristic.mBleGattCharGSDISStartScanNotify) {
                LocalBroadcastManager.getInstance(NanoBLEService.this.getApplicationContext()).sendBroadcast(new Intent(NanoBLEService.ACTION_SCAN_STARTED));
                NanoBLEService.this.scanData.reset();
                NanoBLEService.this.refConf.reset();
                NanoBLEService.this.refMatrix.reset();
                NanoBLEService.this.size = 0;
                NanoBLEService.this.refSize = 0;
                NanoBLEService.this.refMatrixSize = 0;
                byte[] data = characteristic.getValue();
                if (data[0] == -1) {
                    Log.d(NanoBLEService.TAG, "Scan data is ready to be read");
                    NanoBLEService.this.scanIndex[0] = data[1];
                    NanoBLEService.this.scanIndex[1] = data[2];
                    NanoBLEService.this.scanIndex[2] = data[3];
                    NanoBLEService.this.scanIndex[3] = data[4];
                    Log.d(NanoBLEService.TAG, "the scan index is:" + NanoBLEService.this.scanIndex[0] + StringUtils.SPACE + NanoBLEService.this.scanIndex[1] + StringUtils.SPACE + NanoBLEService.this.scanIndex[2] + StringUtils.SPACE + NanoBLEService.this.scanIndex[3]);
                    KSTNanoSDK.requestScanName(NanoBLEService.this.scanIndex);
                }
            } else if (characteristic == NanoGattCharacteristic.mBleGattCharGSDISRetScanName) {
                byte[] data2 = characteristic.getValue();
                Log.d(NanoBLEService.TAG, "Received scan name:" + new String(data2));
                NanoBLEService.this.scanName = new String(data2);
                if (NanoBLEService.this.readingStoredScans) {
                    NanoBLEService.this.storedScanName = data2;
                    KSTNanoSDK.requestScanDate((byte[]) NanoBLEService.this.storedScanList.get(0));
                    return;
                }
                KSTNanoSDK.requestScanType(NanoBLEService.this.scanIndex);
            } else if (characteristic == NanoGattCharacteristic.mBleGattCharGSDISRetScanType) {
                byte[] data3 = characteristic.getValue();
                StringBuilder stringBuilder = new StringBuilder(data3.length);
                for (byte byteChar : data3) {
                    stringBuilder.append(String.format("%02X", new Object[]{Byte.valueOf(byteChar)}));
                }
                Log.d(NanoBLEService.TAG, "Received scan type:" + stringBuilder.toString());
                NanoBLEService.this.scanType = stringBuilder.toString();
                KSTNanoSDK.requestScanDate(NanoBLEService.this.scanIndex);
            } else if (characteristic == NanoGattCharacteristic.mBleGattCharGSDISRetScanDate) {
                byte[] data4 = characteristic.getValue();
                StringBuilder stringBuilder2 = new StringBuilder(data4.length);
                for (byte byteChar2 : data4) {
                    stringBuilder2.append(String.format("%02d", new Object[]{Byte.valueOf(byteChar2)}));
                }
                Log.d(NanoBLEService.TAG, "Received scan date:" + stringBuilder2.toString());
                NanoBLEService.this.scanDate = stringBuilder2.toString();
                if (NanoBLEService.this.readingStoredScans) {
                    NanoBLEService.this.broadcastUpdate(KSTNanoSDK.STORED_SCAN_DATA, NanoBLEService.this.scanDate, (byte[]) NanoBLEService.this.storedScanList.get(0));
                    NanoBLEService.this.storedScanList.remove(0);
                    if (NanoBLEService.this.storedScanList.size() > 0) {
                        KSTNanoSDK.requestScanName((byte[]) NanoBLEService.this.storedScanList.get(0));
                    } else {
                        NanoBLEService.this.readingStoredScans = false;
                    }
                } else {
                    KSTNanoSDK.requestPacketFormatVersion(NanoBLEService.this.scanIndex);
                }
            } else if (characteristic == NanoGattCharacteristic.mBleGattCharGSDISRetPacketFormatVersion) {
                byte[] data5 = characteristic.getValue();
                StringBuilder stringBuilder3 = new StringBuilder(data5.length);
                for (byte byteChar3 : data5) {
                    stringBuilder3.append(String.format("%02X ", new Object[]{Byte.valueOf(byteChar3)}));
                }
                Log.d(NanoBLEService.TAG, "Received Packet Format Version:" + stringBuilder3.toString());
                NanoBLEService.this.scanPktFmtVer = stringBuilder3.toString();
                KSTNanoSDK.requestSerializedScanDataStruct(NanoBLEService.this.scanIndex);
            } else if (characteristic == NanoGattCharacteristic.mBleGattCharGSDISRetSerialScanDataStruct) {
                byte[] data6 = characteristic.getValue();
                StringBuilder stringBuilder4 = new StringBuilder(data6.length);
                for (byte byteChar4 : data6) {
                    stringBuilder4.append(String.format("%02X ", new Object[]{Byte.valueOf(byteChar4)}));
                }
                Log.d(NanoBLEService.TAG, "Received Serialized Scan Data Struct:" + stringBuilder4.toString());
                if (data6[0] == 0) {
                    NanoBLEService.this.size = (data6[2] << 8) | (data6[1] & 255);
                } else {
                    for (int i = 1; i < data6.length; i++) {
                        NanoBLEService.this.scanData.write(data6[i]);
                    }
                }
                if (NanoBLEService.this.scanData.size() == NanoBLEService.this.size) {
                    NanoBLEService.this.size = 0;
                    Log.d(NanoBLEService.TAG, "Done collecting scan data, sending broadcast");
                    NanoBLEService.this.broadcastUpdate(KSTNanoSDK.SCAN_DATA, NanoBLEService.this.scanData.toByteArray());
                }
                Log.d("__SIZE", "new ScanData size:" + NanoBLEService.this.scanData.size());
            } else if (characteristic == NanoGattCharacteristic.mBleGattCharGCISRetRefCalCoefficients) {
                byte[] data7 = characteristic.getValue();
                StringBuilder stringBuilder5 = new StringBuilder(data7.length);
                for (byte byteChar5 : data7) {
                    stringBuilder5.append(String.format("%02X ", new Object[]{Byte.valueOf(byteChar5)}));
                }
                Log.d(NanoBLEService.TAG, "Received Reference calibration coefficients:" + stringBuilder5.toString());
                if (data7[0] == 0) {
                    NanoBLEService.this.refConf.reset();
                    NanoBLEService.this.refSizeIndex = 0;
                    NanoBLEService.this.refSize = (data7[2] << 8) | (data7[1] & 255);
                    Intent requestCalCoef = new Intent(KSTNanoSDK.ACTION_REQ_CAL_COEFF);
                    requestCalCoef.putExtra(KSTNanoSDK.EXTRA_REF_CAL_COEFF_SIZE, NanoBLEService.this.refSize);
                    requestCalCoef.putExtra(KSTNanoSDK.EXTRA_REF_CAL_COEFF_SIZE_PACKET, true);
                    LocalBroadcastManager.getInstance(NanoBLEService.this.getApplicationContext()).sendBroadcast(requestCalCoef);
                } else {
                    for (int i2 = 1; i2 < data7.length; i2++) {
                        NanoBLEService.this.refConf.write(data7[i2]);
                    }
                    Intent requestCalCoef2 = new Intent(KSTNanoSDK.ACTION_REQ_CAL_COEFF);
                    requestCalCoef2.putExtra(KSTNanoSDK.EXTRA_REF_CAL_COEFF_SIZE, data7.length - 1);
                    requestCalCoef2.putExtra(KSTNanoSDK.EXTRA_REF_CAL_COEFF_SIZE_PACKET, false);
                    LocalBroadcastManager.getInstance(NanoBLEService.this.getApplicationContext()).sendBroadcast(requestCalCoef2);
                }
                if (NanoBLEService.this.refConf.size() == NanoBLEService.this.refSize) {
                    NanoBLEService.this.refSize = 0;
                    Log.d(NanoBLEService.TAG, "Done collecting reference, sending broadcast");
                    KSTNanoSDK.requestRefCalMatrix();
                }
            } else if (characteristic == NanoGattCharacteristic.mBleGattCharGCISRetRefCalMatrix) {
                byte[] data8 = characteristic.getValue();
                StringBuilder stringBuilder6 = new StringBuilder(data8.length);
                for (byte byteChar6 : data8) {
                    stringBuilder6.append(String.format("%02X ", new Object[]{Byte.valueOf(byteChar6)}));
                }
                Log.d(NanoBLEService.TAG, "Received Reference calibration matrix:" + stringBuilder6.toString());
                if (data8[0] == 0) {
                    NanoBLEService.this.refMatrix.reset();
                    NanoBLEService.this.refMatrixSize = (data8[2] << 8) | (data8[1] & 255);
                    Intent requestCalMatrix = new Intent(KSTNanoSDK.ACTION_REQ_CAL_MATRIX);
                    requestCalMatrix.putExtra(KSTNanoSDK.EXTRA_REF_CAL_MATRIX_SIZE, NanoBLEService.this.refMatrixSize);
                    requestCalMatrix.putExtra(KSTNanoSDK.EXTRA_REF_CAL_MATRIX_SIZE_PACKET, true);
                    LocalBroadcastManager.getInstance(NanoBLEService.this.getApplicationContext()).sendBroadcast(requestCalMatrix);
                } else {
                    for (int i3 = 1; i3 < data8.length; i3++) {
                        NanoBLEService.this.refMatrix.write(data8[i3]);
                    }
                    Intent requestCalCoef3 = new Intent(KSTNanoSDK.ACTION_REQ_CAL_MATRIX);
                    requestCalCoef3.putExtra(KSTNanoSDK.EXTRA_REF_CAL_MATRIX_SIZE, data8.length - 1);
                    requestCalCoef3.putExtra(KSTNanoSDK.EXTRA_REF_CAL_MATRIX_SIZE_PACKET, false);
                    LocalBroadcastManager.getInstance(NanoBLEService.this.getApplicationContext()).sendBroadcast(requestCalCoef3);
                }
                if (NanoBLEService.this.refMatrix.size() == NanoBLEService.this.refMatrixSize) {
                    NanoBLEService.this.refSize = 0;
                    Log.d(NanoBLEService.TAG, "Done collecting reference Matrix, sending broadcast");
                    NanoBLEService.this.broadcastUpdate(KSTNanoSDK.REF_CONF_DATA, NanoBLEService.this.refConf.toByteArray(), NanoBLEService.this.refMatrix.toByteArray());
                }
            } else if (characteristic == NanoGattCharacteristic.mBleGattCharGSCISRetStoredConfList) {
                byte[] data9 = characteristic.getValue();
                StringBuilder stringBuilder7 = new StringBuilder(data9.length);
                for (byte byteChar7 : data9) {
                    stringBuilder7.append(String.format("%02X ", new Object[]{Byte.valueOf(byteChar7)}));
                }
                Log.d(NanoBLEService.TAG, "Received Scan Conf index:" + stringBuilder7.toString());
                NanoBLEService.this.scanConfIndex++;
                NanoBLEService.this.scanConfList.add(data9);
                if (NanoBLEService.this.scanConfIndexSize == 1 && NanoBLEService.this.scanConfList.size() > 1) {
                    NanoBLEService.this.scanConfIndex = 1;
                    byte[] confIndex = {0, 0};
                    confIndex[0] = ((byte[]) NanoBLEService.this.scanConfList.get(NanoBLEService.this.scanConfIndex))[1];
                    confIndex[1] = ((byte[]) NanoBLEService.this.scanConfList.get(NanoBLEService.this.scanConfIndex))[2];
                    Log.d(NanoBLEService.TAG, "Writing request for scan conf at index:" + confIndex[0] + "-" + confIndex[1]);
                    KSTNanoSDK.requestScanConfiguration(confIndex);
                }
                if (NanoBLEService.this.scanConfIndex == NanoBLEService.this.scanConfIndexSize && NanoBLEService.this.scanConfIndexSize != 1) {
                    NanoBLEService.this.scanConfIndex = 1;
                    byte[] confIndex2 = {0, 0};
                    confIndex2[0] = ((byte[]) NanoBLEService.this.scanConfList.get(NanoBLEService.this.scanConfIndex))[1];
                    confIndex2[1] = ((byte[]) NanoBLEService.this.scanConfList.get(NanoBLEService.this.scanConfIndex))[2];
                    Log.d(NanoBLEService.TAG, "Writing request for scan conf at index:" + confIndex2[0] + "-" + confIndex2[1]);
                    KSTNanoSDK.requestScanConfiguration(confIndex2);
                }
            } else if (characteristic == NanoGattCharacteristic.mBleGattCharGSDISSDStoredScanIndicesListData) {
                byte[] data10 = characteristic.getValue();
                StringBuilder stringBuilder8 = new StringBuilder(data10.length);
                for (byte byteChar8 : data10) {
                    stringBuilder8.append(String.format("%02X ", new Object[]{Byte.valueOf(byteChar8)}));
                }
                Log.d(NanoBLEService.TAG, "Received SD scan indices list:" + stringBuilder8.toString());
                for (int index = 0; index < data10.length / 4; index++) {
                    NanoBLEService.this.storedScanList.add(new byte[]{data10[index * 4], data10[(index * 4) + 1], data10[(index * 4) + 2], data10[(index * 4) + 3]});
                    Log.d(NanoBLEService.TAG, "new storedScanList size:" + NanoBLEService.this.storedScanList.size());
                }
                if (NanoBLEService.this.storedScanList.size() == NanoBLEService.this.storedSDScanSize) {
                    byte[] indexData = (byte[]) NanoBLEService.this.storedScanList.get(0);
                    NanoBLEService.this.readingStoredScans = true;
                    KSTNanoSDK.requestScanName(indexData);
                }
            } else if (characteristic == NanoGattCharacteristic.mBleGattCharGSCISRetScanConfData) {
                byte[] data11 = characteristic.getValue();
                StringBuilder stringBuilder9 = new StringBuilder(data11.length);
                for (byte byteChar9 : data11) {
                    stringBuilder9.append(String.format("%02X ", new Object[]{Byte.valueOf(byteChar9)}));
                }
                Log.d(NanoBLEService.TAG, "Received Scan Conf Data:" + stringBuilder9.toString());
                if (data11[0] == 0) {
                    NanoBLEService.this.scanConf.reset();
                    NanoBLEService.this.scanConfSize = (data11[2] << 8) | (data11[1] & 255);
                } else {
                    for (int i4 = 1; i4 < data11.length; i4++) {
                        NanoBLEService.this.scanConf.write(data11[i4]);
                    }
                }
                if (NanoBLEService.this.scanConf.size() != NanoBLEService.this.scanConfSize) {
                    return;
                }
                if (!NanoBLEService.this.activeConfRequested) {
                    NanoBLEService.this.scanConfSize = 0;
                    Log.d(NanoBLEService.TAG, "Done collecting scanConfiguration, sending broadcast");
                    NanoBLEService.this.broadcastScanConfig(KSTNanoSDK.SCAN_CONF_DATA, NanoBLEService.this.scanConf.toByteArray());
                    if (NanoBLEService.this.scanConfIndex < NanoBLEService.this.scanConfIndexSize) {
                        NanoBLEService.this.scanConfIndex++;
                        byte[] confIndex3 = {0, 0};
                        Log.d(NanoBLEService.TAG, "Retrieving scan at index:" + NanoBLEService.this.scanConfIndex + " Size is:" + NanoBLEService.this.scanConfList.size());
                        confIndex3[0] = ((byte[]) NanoBLEService.this.scanConfList.get(1))[NanoBLEService.this.scanConfIndex + 1];
                        confIndex3[1] = ((byte[]) NanoBLEService.this.scanConfList.get(1))[NanoBLEService.this.scanConfIndex + 2];
                        Log.d(NanoBLEService.TAG, "Writing request for scan conf at index:" + confIndex3[0] + "-" + confIndex3[1]);
                        KSTNanoSDK.requestScanConfiguration(confIndex3);
                        return;
                    }
                    NanoBLEService.this.scanConfIndex = 0;
                    return;
                }
                NanoBLEService.this.scanConfSize = 0;
                Log.d(NanoBLEService.TAG, "Done collecting active scanConfiguration");
                NanoBLEService.this.broadcastScanConfig(KSTNanoSDK.SCAN_CONF_DATA, NanoBLEService.this.scanConf.toByteArray());
                NanoBLEService.this.scanConfIndex = 0;
                NanoBLEService.this.activeConfRequested = false;
            } else if (characteristic == NanoGattCharacteristic.mBleGattCharGSDISClearScanNotify) {
                byte[] data12 = characteristic.getValue();
                StringBuilder stringBuilder10 = new StringBuilder(data12.length);
                for (byte byteChar10 : data12) {
                    stringBuilder10.append(String.format("%02X ", new Object[]{Byte.valueOf(byteChar10)}));
                }
                Log.d(NanoBLEService.TAG, "Received status from clear scan:" + stringBuilder10.toString());
            } else {
                Log.d(NanoBLEService.TAG, "Received notify/indicate from unknown characteristic:" + characteristic.getUuid().toString());
            }
        }

        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if (characteristic.getUuid().equals(NanoGATT.GSDIS_START_SCAN)) {
                Log.d(NanoBLEService.TAG, "Wrote start scan! status=" + status);
            } else if (characteristic.getUuid().equals(NanoGATT.GSDIS_REQ_SER_SCAN_DATA_STRUCT)) {
                Log.d(NanoBLEService.TAG, "Wrote Request for Scan Data Struct! status=" + status);
            } else if (characteristic.getUuid().equals(NanoGATT.GSDIS_REQ_SCAN_TYPE)) {
                Log.d(NanoBLEService.TAG, "Wrote Request for Scan Type! status=" + status);
            } else if (characteristic.getUuid().equals(NanoGATT.GSDIS_REQ_SCAN_NAME)) {
                Log.d(NanoBLEService.TAG, "Wrote Request for Scan Name! status=" + status);
            } else if (characteristic.getUuid().equals(NanoGATT.GSDIS_REQ_SCAN_DATE)) {
                Log.d(NanoBLEService.TAG, "Wrote Request for Scan Date! status=" + status);
            } else if (characteristic.getUuid().equals(NanoGATT.GSDIS_REQ_PKT_FMT_VER)) {
                Log.d(NanoBLEService.TAG, "Wrote Request for Packet Format Version! status=" + status);
            } else if (characteristic.getUuid().equals(NanoGATT.GCIS_REQ_REF_CAL_COEFF)) {
                Log.d(NanoBLEService.TAG, "Wrote Request for Reference Calibration Coefficients! status=" + status);
            } else if (characteristic.getUuid().equals(NanoGATT.GCIS_REQ_REF_CAL_MATRIX)) {
                Log.d(NanoBLEService.TAG, "Wrote Request for Reference Calibration Matrix! status=" + status);
            } else if (characteristic.getUuid().equals(NanoGATT.GSCIS_REQ_STORED_CONF_LIST)) {
                Log.d(NanoBLEService.TAG, "Wrote Request for Scan configuration list! status=" + status);
            } else if (characteristic.getUuid().equals(NanoGATT.GSDIS_SD_STORED_SCAN_IND_LIST)) {
                Log.d(NanoBLEService.TAG, "Wrote Request for SD Stored scan indices list! status=" + status);
            } else if (characteristic.getUuid().equals(NanoGATT.GSCIS_REQ_SCAN_CONF_DATA)) {
                Log.d(NanoBLEService.TAG, "Wrote Request for Scan Conf data! Status=" + status);
            } else if (characteristic.getUuid().equals(NanoGATT.GDTS_TIME)) {
                Log.d(NanoBLEService.TAG, "Wrote Time! Status=" + status);
                String dataString = SettingsManager.getStringPref(NanoBLEService.this.getApplicationContext(), SharedPreferencesKeys.prefix, "Data");
                if (dataString.equals("")) {
                    dataString = "Data";
                }
                byte[] data = new StringBuilder(dataString).reverse().toString().getBytes();
                Log.d(NanoBLEService.TAG, "Writing scan stub to:" + dataString);
                KSTNanoSDK.setStub(data);
            } else if (characteristic.getUuid().equals(NanoGATT.GSDIS_SET_SCAN_NAME_STUB)) {
                Log.d(NanoBLEService.TAG, "Wrote Scan Name Stub! Status=" + status);
                if (!NanoBLEService.this.scanStarted) {
                    Log.d(NanoBLEService.TAG, "Requesting Calibration Data");
                    KSTNanoSDK.requestRefCalCoefficients();
                    return;
                }
                NanoBLEService.this.scanStarted = false;
                Log.d(NanoBLEService.TAG, "Starting Scan");
                byte[] data2 = {0};
                if (SettingsManager.getBooleanPref(NanoBLEService.this.getApplicationContext(), SharedPreferencesKeys.saveSD, false)) {
                    data2[0] = 1;
                    Log.d(NanoBLEService.TAG, "Save to SD selected, writing 1");
                } else {
                    data2[0] = 0;
                    Log.d(NanoBLEService.TAG, "Save to SD not selected, writing 0");
                }
                NanoBLEService.this.scanData.reset();
                NanoBLEService.this.refConf.reset();
                NanoBLEService.this.refMatrix.reset();
                NanoBLEService.this.size = 0;
                NanoBLEService.this.refSize = 0;
                NanoBLEService.this.refMatrixSize = 0;
                KSTNanoSDK.startScan(data2);
            } else if (characteristic.getUuid().equals(NanoGATT.GSDIS_CLEAR_SCAN)) {
                Log.d(NanoBLEService.TAG, "wrote clear scan! status=" + status);
            } else if (characteristic.getUuid().equals(NanoGATT.GSCIS_ACTIVE_SCAN_CONF)) {
                Log.d(NanoBLEService.TAG, "Wrote set active scan conf! status=" + status);
                KSTNanoSDK.getActiveConf();
            } else if (characteristic.getUuid().equals(NanoGATT.GGIS_TEMP_THRESH)) {
                Log.d(NanoBLEService.TAG, "Wrote Temperature threshold! status=" + status);
                KSTNanoSDK.setHumidityThreshold(NanoBLEService.this.humidThresh);
            } else if (characteristic.getUuid().equals(NanoGATT.GGIS_HUMID_THRESH)) {
                Log.d(NanoBLEService.TAG, "Wrote Humidity threshold! status=" + status);
            } else {
                Log.d(NanoBLEService.TAG, "Unknown characteristic");
            }
        }
    };
    /* access modifiers changed from: private */
    public String manufName;
    /* access modifiers changed from: private */
    public String modelNum;
    /* access modifiers changed from: private */
    public boolean readingStoredScans = false;
    ByteArrayOutputStream refConf = new ByteArrayOutputStream();
    ByteArrayOutputStream refMatrix = new ByteArrayOutputStream();
    int refMatrixSize;
    int refSize;
    int refSizeIndex;
    ByteArrayOutputStream scanConf = new ByteArrayOutputStream();
    int scanConfIndex;
    int scanConfIndexSize;
    /* access modifiers changed from: private */
    public ArrayList<byte[]> scanConfList = new ArrayList<>();
    int scanConfSize;
    ByteArrayOutputStream scanData = new ByteArrayOutputStream();
    /* access modifiers changed from: private */
    public String scanDate;
    /* access modifiers changed from: private */
    public byte[] scanIndex = {0, 0, 0, 0};
    /* access modifiers changed from: private */
    public String scanName;
    /* access modifiers changed from: private */
    public String scanPktFmtVer;
    /* access modifiers changed from: private */
    public boolean scanStarted = false;
    /* access modifiers changed from: private */
    public String scanType;
    /* access modifiers changed from: private */
    public String serialNum;
    int size;
    /* access modifiers changed from: private */
    public String spectrumRev;
    int storedSDScanSize;
    /* access modifiers changed from: private */
    public ArrayList<byte[]> storedScanList = new ArrayList<>();
    /* access modifiers changed from: private */
    public byte[] storedScanName;
    /* access modifiers changed from: private */
    public float temp;
    /* access modifiers changed from: private */
    public byte[] tempThresh;
    /* access modifiers changed from: private */
    public String tivaRev;

    public class LocalBinder extends Binder {
        public LocalBinder() {
        }

        public NanoBLEService getService() {
            return NanoBLEService.this;
        }
    }

    public IBinder onBind(Intent intent) {
        return this.mBinder;
    }

    /* access modifiers changed from: private */
    public void broadcastUpdate(String action) {
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(action));
    }

    private void broadcastUpdate(String action, BluetoothGattCharacteristic characteristic) {
        Intent intent = new Intent(action);
        byte[] data = characteristic.getValue();
        if (data != null && data.length > 0) {
            StringBuilder stringBuilder = new StringBuilder(data.length);
            for (byte byteChar : data) {
                stringBuilder.append(String.format("%02X ", new Object[]{Byte.valueOf(byteChar)}));
            }
            Log.d(TAG, "Notify characteristic:" + characteristic.getUuid().toString() + " -- Notify data:" + stringBuilder.toString());
            intent.putExtra(KSTNanoSDK.EXTRA_DATA, stringBuilder.toString());
        }
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    /* access modifiers changed from: private */
    public void broadcastUpdate(String action, byte[] scanData2) {
        Intent intent = new Intent(action);
        intent.putExtra(KSTNanoSDK.EXTRA_DATA, scanData2);
        intent.putExtra(KSTNanoSDK.EXTRA_SCAN_NAME, this.scanName);
        intent.putExtra(KSTNanoSDK.EXTRA_SCAN_TYPE, this.scanType);
        intent.putExtra(KSTNanoSDK.EXTRA_SCAN_DATE, this.scanDate);
        intent.putExtra(KSTNanoSDK.EXTRA_SCAN_FMT_VER, this.scanPktFmtVer);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    /* access modifiers changed from: private */
    public void broadcastScanConfig(String action, byte[] scanData2) {
        Intent intent = new Intent(action);
        intent.putExtra(KSTNanoSDK.EXTRA_DATA, scanData2);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    /* access modifiers changed from: private */
    public void broadcastUpdate(String action, byte[] refCoeff, byte[] refMatrix2) {
        Intent intent = new Intent(action);
        intent.putExtra(KSTNanoSDK.EXTRA_DATA, this.scanData.toByteArray());
        intent.putExtra(KSTNanoSDK.EXTRA_REF_COEF_DATA, refCoeff);
        intent.putExtra(KSTNanoSDK.EXTRA_REF_MATRIX_DATA, refMatrix2);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    /* access modifiers changed from: private */
    public void broadcastUpdate(String action, String scanDate2, byte[] index) {
        Intent intent = new Intent(action);
        intent.putExtra(KSTNanoSDK.EXTRA_SCAN_NAME, nameToUTF8(this.storedScanName));
        intent.putExtra(KSTNanoSDK.EXTRA_SCAN_DATE, scanDate2);
        intent.putExtra(KSTNanoSDK.EXTRA_SCAN_INDEX, index);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind called");
        close();
        return super.onUnbind(intent);
    }

    public boolean initialize() {
        Log.d(TAG, "Initializing BLE");
        if (this.mBluetoothManager == null) {
            this.mBluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
            if (this.mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }
        KSTNanoSDK.mBluetoothAdapter = this.mBluetoothManager.getAdapter();
        if (KSTNanoSDK.mBluetoothAdapter != null) {
            return true;
        }
        Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
        return false;
    }

    public boolean connect(String address) {
        if (KSTNanoSDK.mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        } else if (this.mBluetoothDeviceAddress == null || !address.equals(this.mBluetoothDeviceAddress) || KSTNanoSDK.mBluetoothGatt == null) {
            BluetoothDevice device = KSTNanoSDK.mBluetoothAdapter.getRemoteDevice(address);
            if (device == null) {
                Log.w(TAG, "Device not found.  Unable to connect.");
                return false;
            }
            if (VERSION.SDK_INT >= 23) {
                Log.d(TAG, "Using LE Transport");
                KSTNanoSDK.mBluetoothGatt = device.connectGatt(this, false, this.mGattCallback, 2);
            } else {
                KSTNanoSDK.mBluetoothGatt = device.connectGatt(this, false, this.mGattCallback);
            }
            Log.d(TAG, "Trying to create a new connection.");
            this.mBluetoothDeviceAddress = address;
            return true;
        } else {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            return KSTNanoSDK.mBluetoothGatt.connect();
        }
    }

    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate called");
        mDataReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent i) {
                if (i != null) {
                    Log.d(NanoBLEService.TAG, "Starting Scan");
                    byte[] data = {0};
                    if (SettingsManager.getBooleanPref(context, SharedPreferencesKeys.saveSD, false)) {
                        data[0] = 1;
                        Log.e(NanoBLEService.TAG, "Save to SD selected, writing 1");
                    } else {
                        data[0] = 0;
                        Log.e(NanoBLEService.TAG, "Save to SD not selected, writing 0");
                    }
                    NanoBLEService.this.scanData.reset();
                    NanoBLEService.this.refConf.reset();
                    NanoBLEService.this.refMatrix.reset();
                    NanoBLEService.this.size = 0;
                    NanoBLEService.this.refSize = 0;
                    NanoBLEService.this.refMatrixSize = 0;
                    KSTNanoSDK.startScan(data);
                }
            }
        };
        mInfoRequestReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (intent != null) {
                    Log.d(NanoBLEService.TAG, "Requesting Device Info");
                    KSTNanoSDK.getManufacturerName();
                }
            }
        };
        mStatusRequestReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (intent != null) {
                    Log.d(NanoBLEService.TAG, "Requesting Device Status");
                    KSTNanoSDK.getBatteryLevel();
                }
            }
        };
        mScanConfRequestReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (intent != null) {
                    Log.d(NanoBLEService.TAG, "Requesting Device Status");
                    KSTNanoSDK.getNumberStoredConfigurations();
                }
            }
        };
        mStoredScanRequestReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (intent != null) {
                    Log.d(NanoBLEService.TAG, "Requesting Stored Scans");
                    KSTNanoSDK.getNumberStoredScans();
                }
            }
        };
        mSetTimeReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (intent != null) {
                    Log.d(NanoBLEService.TAG, "writing time to nano");
                    KSTNanoSDK.setTime();
                }
            }
        };
        mStartScanReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                NanoBLEService.this.scanStarted = true;
                String dataString = SettingsManager.getStringPref(NanoBLEService.this.getApplicationContext(), SharedPreferencesKeys.prefix, "Nano");
                if (dataString.equals("")) {
                    dataString = "Nano";
                }
                byte[] data = new StringBuilder(dataString).reverse().toString().getBytes();
                Log.d(NanoBLEService.TAG, "Writing scan stub to:" + dataString);
                KSTNanoSDK.setStub(data);
            }
        };
        mDeleteScanReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                byte[] index = intent.getByteArrayExtra(KSTNanoSDK.EXTRA_SCAN_INDEX);
                Log.d(NanoBLEService.TAG, "deleting index:" + index[0] + "-" + index[1] + "-" + index[2] + "-" + index[3]);
                KSTNanoSDK.deleteScan(index);
            }
        };
        mGetActiveScanConfReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                Log.d(NanoBLEService.TAG, "Reading active scan conf");
                KSTNanoSDK.getActiveConf();
            }
        };
        mSetActiveScanConfReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                Log.d(NanoBLEService.TAG, "Setting active scan conf");
                KSTNanoSDK.setActiveConf(intent.getByteArrayExtra(KSTNanoSDK.EXTRA_SCAN_INDEX));
            }
        };
        mUpdateThresholdReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                Log.d(NanoBLEService.TAG, "Updating Thresholds");
                NanoBLEService.this.tempThresh = intent.getByteArrayExtra(KSTNanoSDK.EXTRA_TEMP_THRESH);
                NanoBLEService.this.humidThresh = intent.getByteArrayExtra(KSTNanoSDK.EXTRA_HUMID_THRESH);
                KSTNanoSDK.setTemperatureThreshold(NanoBLEService.this.tempThresh);
            }
        };
        mRequestActiveConfReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                NanoBLEService.this.activeConfRequested = true;
                KSTNanoSDK.getActiveConf();
            }
        };
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mDataReceiver, new IntentFilter(KSTNanoSDK.SEND_DATA));
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mInfoRequestReceiver, new IntentFilter(KSTNanoSDK.GET_INFO));
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mStatusRequestReceiver, new IntentFilter(KSTNanoSDK.GET_STATUS));
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mScanConfRequestReceiver, new IntentFilter(KSTNanoSDK.GET_SCAN_CONF));
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mStoredScanRequestReceiver, new IntentFilter(KSTNanoSDK.GET_STORED_SCANS));
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mSetTimeReceiver, new IntentFilter(KSTNanoSDK.SET_TIME));
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mStartScanReceiver, new IntentFilter(KSTNanoSDK.START_SCAN));
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mDeleteScanReceiver, new IntentFilter(KSTNanoSDK.DELETE_SCAN));
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mGetActiveScanConfReceiver, new IntentFilter(KSTNanoSDK.GET_ACTIVE_CONF));
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mSetActiveScanConfReceiver, new IntentFilter(KSTNanoSDK.SET_ACTIVE_CONF));
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mUpdateThresholdReceiver, new IntentFilter(KSTNanoSDK.UPDATE_THRESHOLD));
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mRequestActiveConfReceiver, new IntentFilter(KSTNanoSDK.REQUEST_ACTIVE_CONF));
    }

    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy called");
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mDataReceiver);
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mInfoRequestReceiver);
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mStatusRequestReceiver);
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mScanConfRequestReceiver);
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mStoredScanRequestReceiver);
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mSetTimeReceiver);
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mStartScanReceiver);
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mDeleteScanReceiver);
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mGetActiveScanConfReceiver);
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mSetActiveScanConfReceiver);
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mUpdateThresholdReceiver);
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mRequestActiveConfReceiver);
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand called");
        return super.onStartCommand(intent, flags, startId);
    }

    public void onTaskRemoved(Intent rootIntent) {
        Log.d(TAG, "onTaskRemoved called");
    }

    public void disconnect() {
        if (KSTNanoSDK.mBluetoothAdapter == null) {
            Log.w(TAG, "BluetoothAdapter is null");
        }
        if (KSTNanoSDK.mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothGatt is null");
        }
        if (KSTNanoSDK.mBluetoothAdapter == null || KSTNanoSDK.mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
        } else {
            KSTNanoSDK.mBluetoothGatt.disconnect();
        }
    }

    public void close() {
        if (KSTNanoSDK.mBluetoothGatt != null) {
            KSTNanoSDK.mBluetoothGatt.close();
            KSTNanoSDK.mBluetoothGatt = null;
        }
    }

    private String nameToUTF8(byte[] data) {
        byte[] byteChars = new byte[data.length];
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        for (byte b : byteChars) {
            byteChars[b] = 0;
        }
        int i = 0;
        while (i < data.length) {
            byteChars[i] = data[i];
            if (data[i] != 0) {
                os.write(data[i]);
                i++;
            }
        }
        try {
            return new String(os.toByteArray(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean refreshDeviceCache(BluetoothGatt gatt) {
        BluetoothGatt localBluetoothGatt = gatt;
        try {
            Method localMethod = localBluetoothGatt.getClass().getMethod("refresh", new Class[0]);
            if (localMethod != null) {
                return ((Boolean) localMethod.invoke(localBluetoothGatt, new Object[0])).booleanValue();
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "An exception occurred while refreshing device");
            return false;
        }
    }

    /* access modifiers changed from: private */
    public void refresh() {
        refreshDeviceCache(KSTNanoSDK.mBluetoothGatt);
    }
}
