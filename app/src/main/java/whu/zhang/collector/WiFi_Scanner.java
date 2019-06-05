package whu.zhang.collector;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;

//import android.util.Log;

/**
 * Created by zhang on 2018/3/27.
 */

public class WiFi_Scanner {

    class MyRSS implements Comparable {
        public int rss;
        String mac;
        String name;
        public long timestamp;

        public MyRSS(String mac, int rss, long timestamp) {
            this.rss = rss;
            this.mac = mac;
            this.timestamp = timestamp;
            this.name = "";
        }

        public MyRSS(String name, String mac, int rss, long timestamp) {
            this.name = name;
            this.rss = rss;
            this.mac = mac;
            this.timestamp = timestamp;
            if(this.name == null)
                this.name = "";
        }

        @Override
        public int compareTo(Object object) {
            MyRSS myRSS = (MyRSS) object;
            if (this.rss > myRSS.rss)
                return -1;
            else if (this.rss < myRSS.rss)
                return 1;
            else return 0;
        }

        @Override
        public boolean equals(Object ob){
            if(ob instanceof MyRSS){
                if(ob == null)
                    return false;
                return ((MyRSS) ob).mac.equals(this.mac);
            }
            return false;
        }
    }

    static DecimalFormat df = new DecimalFormat("0.000000");
    final java.text.SimpleDateFormat sDateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

    private String wifi_path = "";
    private String ibeacon_path = "";


    private boolean wifi_allow = false, bt_allow = false;
    public boolean wifi_scanning = false, bt_scanning = false;
    private long start_time = 0;

    public WifiManager wifiManager;
    BluetoothLeScanner bTScanner;


    private final int initial_list_num = 200;

    public void setPath(String path){
        wifi_path = path + "/Wi-Fi.txt";
        ibeacon_path = path + "/iBeacon.csv";
    }

    public WiFi_Scanner(Context context) {
        try {
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            BluetoothManager bm = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            BluetoothAdapter bTAdatper;
            if (wifiManager != null) {
                if (!wifiManager.isWifiEnabled()) {
                    wifiManager.setWifiEnabled(true);
                }
            } else throw new Exception("Wifi Exception");

            if(bm != null) {
                if (!(bTAdatper = bm.getAdapter()).isEnabled()) {
                    bTAdatper.enable();
                }
            }else{
                throw new Exception("Bluetooth Exception");
            }

            while (!wifiManager.isWifiEnabled() || !bTAdatper.isEnabled());

            this.wifiManager = wifiManager;
            this.bTScanner = bTAdatper.getBluetoothLeScanner();

        } catch (Exception e) {
            e.printStackTrace();
            Log.e("WiFi_Scanner", "Create Failed");
        }
    }


    public boolean startScan() {
        if (!wifi_allow && !bt_allow) {
            start_time = System.currentTimeMillis();
            wifi_allow = true;
            bt_allow = true;
            start();
            return true;
        }
        return false;
    }





    synchronized private boolean start() {
        synchronized ("0") {
            if (wifiManager != null) {

                final Thread wifi_thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        wifi_scanning = true;
                        while (wifi_allow) {
                            try {
                                long current_t = System.currentTimeMillis();
                                wifiManager.startScan();
                                Thread.sleep(1000);
                                ArrayList<ScanResult> scanResults = (ArrayList<ScanResult>) wifiManager.getScanResults();
                                Log.e("Wi-Fi List", "" + scanResults.size());
                                int len = scanResults.size();
                                if(len > 0) {
                                    try {
                                        File file = new File(wifi_path);
                                        DataWriter dataWriter = new DataWriter(file, true);
                                        dataWriter.write(String.valueOf(current_t));
                                        for (int i = 0; i < len; ++i) {
                                            ScanResult scanResult = scanResults.get(i);
                                            String line = ";" + scanResult.SSID + "," + scanResult.BSSID + "," + scanResult.level;
                                            dataWriter.write(line);
                                        }
                                        dataWriter.write("\r\n");
                                        dataWriter.close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }

                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        wifi_scanning = false;
                    }
                });

                final Thread ibeacon_thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        bt_scanning = true;
                        bTScanner.startScan(mLeScanCallback);
                        try {
                            while (bt_allow) {
                                Thread.sleep(500);
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        bTScanner.stopScan(mLeScanCallback);
                        bt_scanning = false;
                    }
                });


                wifi_thread.start();
                ibeacon_thread.start();


                return true;
            } else return false;
        }
    }

    synchronized public void stop() {
        wifi_allow = false;
        bt_allow = false;
    }


    ArrayList<MyRSS> cached_bt = new ArrayList<>(1000);
    long lastBtT = 0;

    private ScanCallback mLeScanCallback =
            new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, android.bluetooth.le.ScanResult result) {
                    long current_t = System.currentTimeMillis();
                    if(!bt_allow) {
                        File file = new File(ibeacon_path);
                        try {
                            DataWriter dataWriter = new DataWriter(file, true);
                            int len = cached_bt.size();
                            for(int i = 0; i < len; ++i){
                                MyRSS rss = cached_bt.get(i);
                                String line = rss.timestamp + "," + rss.mac + "," + rss.name + "," + rss.rss + "\r\n";
                                dataWriter.write(line);
                            }
                            dataWriter.close();
                            cached_bt.clear();
                        }catch (IOException e){
                            e.printStackTrace();
                        }
                        return;
                    }

                    final iBeaconClass.iBeacon ibeacon = iBeaconClass.fromScanData(result.getDevice(), result.getRssi(), result.getScanRecord().getBytes());
                    if(ibeacon == null)
                        return;
                    MyRSS item = new MyRSS(ibeacon.name, ibeacon.bluetoothAddress, ibeacon.rssi, current_t);
                    cached_bt.add(item);
                    if(current_t - lastBtT > 1000 && cached_bt.size() > 0) {
                        File file = new File(ibeacon_path);
                        try {
                            DataWriter dataWriter = new DataWriter(file, true);
                            int len = cached_bt.size();
                            for(int i = 0; i < len; ++i){
                                MyRSS rss = cached_bt.get(i);
                                String line = rss.timestamp + "," + rss.mac + "," + new String(rss.name.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8) + "," + rss.rss + "\r\n";
                                Log.e("iBeacon", line);
                                dataWriter.write(line);
                            }
                            dataWriter.close();
                            cached_bt.clear();
                        }catch (IOException e){
                            e.printStackTrace();
                        }
                        lastBtT = current_t;
                    }
                }
            };
}
