package whu.zhang.collector;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

/**
 * Created by zhang on 2018/11/2.
 */

public class DataWriter {
    private OutputStreamWriter out;

    public DataWriter(File file, boolean append) throws IOException{
        out = new OutputStreamWriter(new FileOutputStream(file, append), StandardCharsets.UTF_8);
    }

    public DataWriter(String path, boolean append) throws IOException{
        out = new OutputStreamWriter(new FileOutputStream(path, append), StandardCharsets.UTF_8);
    }

    public DataWriter(File file, String charsetName, boolean append) throws IOException{
        out = new OutputStreamWriter(new FileOutputStream(file, append), charsetName);
    }

    public DataWriter(String path, String charsetName, boolean append) throws IOException{
        out = new OutputStreamWriter(new FileOutputStream(path, append), charsetName);
    }

    public void write(String str) throws IOException{
        out.write(str);
    }

    public void flush() throws  IOException{
        out.flush();
    }

    public void close() throws IOException{
        out.close();
    }
}
