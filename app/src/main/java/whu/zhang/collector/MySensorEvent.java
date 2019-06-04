package whu.zhang.collector;

import android.hardware.SensorEvent;
import android.support.annotation.NonNull;
import android.util.Log;

/**
 * Created by zhang on 2019/1/20.
 */

public class MySensorEvent {
    double[] values;
    long timestamp;
    int type;
    public MySensorEvent(SensorEvent sensorEvent){
        values = new double[3];
        int i = 0;
        for(; i < sensorEvent.values.length && i < 3; ++i){
            values[i] = sensorEvent.values[i];
        }
        timestamp = sensorEvent.timestamp;
        type = sensorEvent.sensor.getType();
    }

    public MySensorEvent(MySensorEvent event){
        values = new double[]{event.values[0], event.values[1], event.values[2]};
        this.type = event.type;
        this.timestamp = event.timestamp;
    }

    public MySensorEvent(int type, double x, double y, double z, long timestamp){
        values = new double[]{x, y, z};
        this.type = type;
        this.timestamp = timestamp;
    }
    @Override
    @NonNull
    public String toString(){
        StringBuilder result = new StringBuilder();
        result.append(type).append(",").append(timestamp);
        for(int i = 0; i < 9; ++i){
            result.append(",");
            if(i < values.length){
                result.append(values[i]);
            }
            else{
                result.append(0);
            }
        }
        return result.toString();
    }
}
