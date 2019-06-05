package whu.zhang.collector;

import android.Manifest;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TextView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private Spinner spinner_acc, spinner_gyr, spinner_mag, spinner_uacc, spinner_ugyr, spinner_umag, spinner_light, spinner_temp, spinner_prox, spinner_pres;
    private List<String> sample_rate_list;
    private ArrayAdapter<String> adapter_acc, adapter_gyr, adapter_mag, adapter_uacc, adapter_ugyr, adapter_umag, adapter_light, adapter_temp, adapter_prox, adapter_pres;
    private TextView tv_acc, tv_gyr, tv_mag, tv_uacc, tv_ugyr, tv_umag, tv_light, tv_temp, tv_prox, tv_pres;
    private SensorCollector accC, gyrC, magC, uaccC, ugyrC, umagC, lightC, tempC, proxC, presC;
    private ArrayList<SensorCollector> collectors = new ArrayList<>(10);
    private ArrayList<TextView> tv0s = new ArrayList<>(10);
    private ArrayList<ArrayAdapter> adapters = new ArrayList<>(10);
    private ArrayList<Spinner> spinners = new ArrayList<>(10);
    private Button bt;
    private String[] sensor_name = new String[]{"加速度", "角速度", "磁场", "加速度(未校正)", "角速度(未校正)", "磁场(未校正)", "光照", "接近", "压力", "温度", "Wi-Fi", "iBeacon"};
    private TextView tvw, tvb;
    private WiFi_Scanner wifi_scanner;
    private String root_dir = "SensorData_";
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");

    Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg){
            switch (msg.what){
                case 0:{
                    bt.setText("开始");
                    bt.setEnabled(true);
                }break;
                case 1:{
                    TextView tv = (TextView) msg.obj;
                    tv.setText("可用");
                }break;
                case 2:{
                    tvw.setText("Wi-Fi");
                }break;
                case 3:{
                    tvb.setText("iBeacon");
                }
            }
        }
    };

    private boolean collecting = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sample_rate_list = new ArrayList<>();
        sample_rate_list.add("最快");
        sample_rate_list.add("较快");
        sample_rate_list.add("普通");
        sample_rate_list.add("慢");

        spinner_acc = findViewById(R.id.spinner_acc);
        spinner_gyr = findViewById(R.id.spinner_gyr);
        spinner_mag = findViewById(R.id.spinner_mag);
        spinner_uacc = findViewById(R.id.spinner_uacc);
        spinner_ugyr = findViewById(R.id.spinner_ugyr);
        spinner_umag = findViewById(R.id.spinner_umag);
        spinner_light = findViewById(R.id.spinner_light);
        spinner_temp = findViewById(R.id.spinner_temp);
        spinner_prox = findViewById(R.id.spinner_prox);
        spinner_pres = findViewById(R.id.spinner_pres);

        tv_acc = findViewById(R.id.tv_acc_);
        tv_gyr = findViewById(R.id.tv_gyr_);
        tv_mag = findViewById(R.id.tv_mag_);
        tv_uacc = findViewById(R.id.tv_uacc_);
        tv_ugyr = findViewById(R.id.tv_ugyr_);
        tv_umag = findViewById(R.id.tv_umag_);
        tv_light = findViewById(R.id.tv_light_);
        tv_temp = findViewById(R.id.tv_temp_);
        tv_prox = findViewById(R.id.tv_prox_);
        tv_pres = findViewById(R.id.tv_pres_);

        tvw = findViewById(R.id.tvw);
        tvb = findViewById(R.id.tvb);

        bt = findViewById(R.id.bt_start);
        bt.setOnClickListener(this);

        adapter_acc = new ArrayAdapter<String>(this, R.layout.spinner_item0, sample_rate_list);
        adapter_gyr = new ArrayAdapter<String>(this, R.layout.spinner_item0, sample_rate_list);
        adapter_mag = new ArrayAdapter<String>(this, R.layout.spinner_item0, sample_rate_list);
        adapter_uacc = new ArrayAdapter<String>(this, R.layout.spinner_item0, sample_rate_list);
        adapter_ugyr = new ArrayAdapter<String>(this, R.layout.spinner_item0, sample_rate_list);
        adapter_umag = new ArrayAdapter<String>(this, R.layout.spinner_item0, sample_rate_list);
        adapter_light = new ArrayAdapter<String>(this, R.layout.spinner_item0, sample_rate_list);
        adapter_temp = new ArrayAdapter<String>(this, R.layout.spinner_item0, sample_rate_list);
        adapter_prox = new ArrayAdapter<String>(this, R.layout.spinner_item0, sample_rate_list);
        adapter_pres = new ArrayAdapter<String>(this, R.layout.spinner_item0, sample_rate_list);
        spinner_acc.setAdapter(adapter_acc);
        spinner_gyr.setAdapter(adapter_gyr);
        spinner_mag.setAdapter(adapter_mag);
        spinner_uacc.setAdapter(adapter_uacc);
        spinner_umag.setAdapter(adapter_umag);
        spinner_ugyr.setAdapter(adapter_ugyr);
        spinner_light.setAdapter(adapter_light);
        spinner_temp.setAdapter(adapter_temp);
        spinner_prox.setAdapter(adapter_prox);
        spinner_pres.setAdapter(adapter_pres);

        accC = new SensorCollector(this, Sensor.TYPE_ACCELEROMETER, SensorManager.SENSOR_DELAY_FASTEST);
        gyrC = new SensorCollector(this, Sensor.TYPE_GYROSCOPE, SensorManager.SENSOR_DELAY_FASTEST);
        magC = new SensorCollector(this, Sensor.TYPE_MAGNETIC_FIELD, SensorManager.SENSOR_DELAY_FASTEST);
        uaccC = new SensorCollector(this, Sensor.TYPE_ACCELEROMETER_UNCALIBRATED, SensorManager.SENSOR_DELAY_FASTEST);
        ugyrC = new SensorCollector(this, Sensor.TYPE_GYROSCOPE_UNCALIBRATED, SensorManager.SENSOR_DELAY_FASTEST);
        umagC = new SensorCollector(this, Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED, SensorManager.SENSOR_DELAY_FASTEST);
        lightC = new SensorCollector(this, Sensor.TYPE_LIGHT, SensorManager.SENSOR_DELAY_FASTEST);
        proxC = new SensorCollector(this, Sensor.TYPE_PROXIMITY, SensorManager.SENSOR_DELAY_FASTEST);
        presC = new SensorCollector(this, Sensor.TYPE_PRESSURE, SensorManager.SENSOR_DELAY_FASTEST);
        tempC = new SensorCollector(this, Sensor.TYPE_AMBIENT_TEMPERATURE, SensorManager.SENSOR_DELAY_FASTEST);

        collectors.add(accC);   tv0s.add(tv_acc);   adapters.add(adapter_acc);  spinners.add(spinner_acc);
        collectors.add(gyrC);   tv0s.add(tv_gyr);   adapters.add(adapter_gyr);  spinners.add(spinner_gyr);
        collectors.add(magC);   tv0s.add(tv_mag);   adapters.add(adapter_mag);  spinners.add(spinner_mag);
        collectors.add(uaccC);  tv0s.add(tv_uacc);  adapters.add(adapter_uacc); spinners.add(spinner_uacc);
        collectors.add(ugyrC);  tv0s.add(tv_ugyr);  adapters.add(adapter_ugyr); spinners.add(spinner_ugyr);
        collectors.add(umagC);  tv0s.add(tv_umag);  adapters.add(adapter_umag); spinners.add(spinner_umag);
        collectors.add(lightC); tv0s.add(tv_light); adapters.add(adapter_light);spinners.add(spinner_light);
        collectors.add(proxC);  tv0s.add(tv_prox);  adapters.add(adapter_prox); spinners.add(spinner_prox);
        collectors.add(presC);  tv0s.add(tv_pres);  adapters.add(adapter_pres); spinners.add(spinner_pres);
        collectors.add(tempC);  tv0s.add(tv_temp);  adapters.add(adapter_temp); spinners.add(spinner_temp);

        for(int i = 0; i < collectors.size(); ++i) {
            SensorCollector collector = collectors.get(i);
            TextView tv = tv0s.get(i);
            Spinner spinner = spinners.get(i);
            if (collector.available()) {
                tv.setText("可用");
                tv.setTextColor(Color.GREEN);
            } else {
                tv.setText("无");
                tv.setTextColor(Color.RED);
                spinner.setEnabled(false);
            }
        }
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);

        wifi_scanner = new WiFi_Scanner(this);

        File file = new File(Environment.getExternalStorageDirectory(), root_dir + Build.MODEL);
        if(!file.exists()){
            while(!file.mkdir()){
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            }
        }

    }



    @Override
    public void onClick(View view){
        switch (view.getId()){
            case R.id.bt_start:{
                if(!collecting){
                    long timestamp = System.currentTimeMillis();
                    String timestamp_str = sdf.format(timestamp);
                    File root = new File(Environment.getExternalStorageDirectory(), root_dir + Build.MODEL);
                    File dir = new File(root, timestamp_str);
                    if(!dir.exists())
                        dir.mkdir();
                    for(int i = 0; i < collectors.size(); ++i) {
                        SensorCollector collector = collectors.get(i);
                        TextView tv = tv0s.get(i);
                        Spinner spinner = spinners.get(i);
                        String rate = (String) spinner.getSelectedItem();
                        if(collector.available()) {
                            File file = new File(dir, sensor_name[i]);
                            collector.filePath = file.getAbsolutePath();
                            int sample_rate = SensorManager.SENSOR_DELAY_FASTEST;
                            if(rate.equals("最快")){
                                sample_rate = SensorManager.SENSOR_DELAY_FASTEST;
                            }else if(rate.equals("较快")){
                                sample_rate = SensorManager.SENSOR_DELAY_GAME;
                            }else if(rate.equals("普通")){
                                sample_rate = SensorManager.SENSOR_DELAY_UI;
                            }else {
                                sample_rate = SensorManager.SENSOR_DELAY_NORMAL;
                            }
                            collector.setSampleRateLevel(sample_rate);
                            collector.start();
                            tv.setText("采集中");
                        }
                    }
                    wifi_scanner.setPath(dir.getAbsolutePath());
                    wifi_scanner.startScan();
                    tvb.setText("iBeacon采集中");
                    tvw.setText("Wi-Fi采集中");


                    collecting = true;
                    bt.setText("停止");
                }else {
                    for(int i = 0; i < collectors.size(); ++i) {
                        SensorCollector collector = collectors.get(i);
                        TextView tv = tv0s.get(i);
                        if(collector.available()) {
                            collector.stop();
                            tv.setText("停止中");
                        }
                    }
                    wifi_scanner.stop();
                    bt.setText("停止中");
                    bt.setEnabled(false);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            while (true){
                                boolean running = false;
                                for(int i = 0; i < collectors.size(); ++i) {
                                    SensorCollector collector = collectors.get(i);
                                    TextView tv = tv0s.get(i);
                                    if(collector.available()){
                                        if(!collector.isRunning){
                                            Message msg = handler.obtainMessage();
                                            msg.what = 1;
                                            msg.obj = tv;
                                            handler.sendMessage(msg);
                                        }
                                        else {
                                            running = true;
                                        }
                                    }
                                }
                                if(!wifi_scanner.bt_scanning){
                                    handler.sendEmptyMessage(3);
                                }
                                else {
                                    running = true;
                                }

                                if(!wifi_scanner.wifi_scanning){
                                    handler.sendEmptyMessage(2);
                                }
                                else {
                                    running = true;
                                }
                                if(!running) {
                                    handler.sendEmptyMessage(0);
                                    collecting = false;
                                    break;
                                }
                                try {
                                    Thread.sleep(100);
                                }catch (InterruptedException e){
                                    e.printStackTrace();
                                }
                            }
                        }
                    }).start();
                }
            }break;
        }
    }
}
