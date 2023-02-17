package io.github.jsbxyyx.iepub;

import com.google.common.base.Throwables;
import org.apache.commons.lang3.time.DateFormatUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.Properties;

public class PropertiesUtil {

    private static final Properties prop = new Properties();

    private static final String name = "iepub";

    private static final String base = System.getProperty("user.home") + "/." + name;

    private static final String filename = "global.properties";
    private static final File globalFile = new File(base + "/" + filename);
    private static final File logFile = new File(base + "/" + name + ".log");

    static {
        File file = new File(base);
        if (!file.exists()) {
            file.mkdirs();
        }

        if (!globalFile.exists()) {
            try {
                globalFile.createNewFile();
            } catch (IOException e) {
                PropertiesUtil.log(filename + " create fail." + Throwables.getStackTraceAsString(e));
            }
        }
        try (FileInputStream in = new FileInputStream(globalFile)) {
            prop.load(in);
        } catch (IOException e) {
            PropertiesUtil.log("load " + filename + " fail." + Throwables.getStackTraceAsString(e));
        }
    }


    public static void setValue(String key, String value) {
        prop.setProperty(key, value);
        try (FileOutputStream out = new FileOutputStream(globalFile)) {
            prop.store(out, "");
        } catch (IOException e) {
            PropertiesUtil.log("store " + filename + " error." + Throwables.getStackTraceAsString(e));
        }
    }

    public static String getValue(String key) {
        return prop.getProperty(key);
    }

    public static void log(String text) {
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(logFile, true)));
            bw.write(DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss.SSS"));
            bw.write(" ");
            bw.write(text);
            bw.write(System.lineSeparator());
            bw.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException ignore) {
                }
            }
        }
    }

}