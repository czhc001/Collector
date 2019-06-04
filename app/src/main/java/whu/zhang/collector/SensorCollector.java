package whu.zhang.collector;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

public class SensorCollector {
    public int sensor_type = -1;
    public int sample_rate_level = -1;
    Sensor sensor;
    SensorManager sensorManager;
    boolean allow = false;
    boolean isRunning = false;

    final Queue<MySensorEvent> cached = new LinkedList<>();
    final ArrayList<MySensorEvent> write_cache = new ArrayList<>(1000);

    String filePath = "";

    private void append(MySensorEvent mySensorEvent){
        synchronized (cached){
            cached.offer(mySensorEvent);
        }
    }

    private MySensorEvent get(){
        synchronized (cached){
            return cached.poll();
        }
    }


    public SensorCollector(Context context, int sensor_type, int sample_rate_level){
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(sensor_type);
        this.sample_rate_level = sample_rate_level;
    }

    public boolean available(){
        return (sensor != null);
    }

    public void setSampleRateLevel(int sample_rate_level){
        this.sample_rate_level = sample_rate_level;
    }

    public void setFilePath(String path){
        filePath = path;
    }

    public void start(){
        allow = true;
        isRunning = true;
        writer = new Thread(new Runnable() {
            @Override
            public void run() {
                long last_timestamp = System.currentTimeMillis();
                MySensorEvent event = null;
                while (allow){
                    if((event = get()) != null){
                        write_cache.add(event);
                        long current_t = System.currentTimeMillis();
                        if(current_t - last_timestamp > 2000){
                            write();
                        }
                    }
                }
                write();
                isRunning = false;
            }
        });
        writer.start();
        filePath = filePath + "_" + System.currentTimeMillis() + ".csv";
        sensorManager.registerListener(sensorEventListener, sensor, sample_rate_level);
    }

    public void stop(){
        sensorManager.unregisterListener(sensorEventListener, sensor);
        allow = false;
    }

    private void write(){
        try {
            File file = new File(filePath);
            DataWriter dataWriter = new DataWriter(file, true);
            int len = write_cache.size();
            for(int i = 0; i < len; ++i){
                MySensorEvent event = write_cache.get(i);
                String line = event.timestamp + "," + event.values[0] + ","  + event.values[1] + "," + event.values[2] + "\r\n";
                dataWriter.write(line);
            }
            dataWriter.close();
            write_cache.clear();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            MySensorEvent mySensorEvent = new MySensorEvent(sensorEvent);
            append(mySensorEvent);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    };

    Thread writer;
}
