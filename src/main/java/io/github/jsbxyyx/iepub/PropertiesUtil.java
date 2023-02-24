package io.github.jsbxyyx.iepub;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
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

    private static final File globalFile = new File(base + "/" + "global.properties");
    private static final File logFile = new File(base + "/" + "all.log");

    static {
        File file = new File(base);
        if (!file.exists()) {
            file.mkdirs();
        }
        if (!globalFile.exists()) {
            try {
                globalFile.createNewFile();
            } catch (IOException e) {
                PropertiesUtil.log(globalFile.getName() + " create fail." + ExceptionUtils.getStackTrace(e));
            }
        }
        try (FileInputStream in = new FileInputStream(globalFile)) {
            prop.load(in);
        } catch (IOException e) {
            PropertiesUtil.log("load " + globalFile.getName() + " fail." + ExceptionUtils.getStackTrace(e));
        }
    }


    public static void setValue(String key, String value) {
        prop.setProperty(key, value);
        try (FileOutputStream out = new FileOutputStream(globalFile)) {
            prop.store(out, "date : " + getDateTimeString());
        } catch (IOException e) {
            PropertiesUtil.log("store " + globalFile.getName() + " error." + ExceptionUtils.getStackTrace(e));
        }
    }

    public static String getValue(String key) {
        return prop.getProperty(key);
    }

    public static void log(String text) {
        log(getCaller(3), text, null);
    }

    public static void log(String text, Throwable throwable) {
        log(getCaller(3), text, throwable);
    }

    private static void log(StackTraceElement caller, String text, Throwable throwable) {
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(logFile, true)));
            String d = getDateTimeString();
            String c = caller.getClassName();
            String m = caller.getMethodName();
            String l = caller.getLineNumber() + "";
            StringBuilder msg = new StringBuilder(text);
            if (throwable != null) {
                msg.append(System.lineSeparator());
                msg.append(ExceptionUtils.getStackTrace(throwable));
            }
            String log = StringUtils.replaceEach("${d} ${c}.${m}:${l} - ${msg} ${n}",
                    new String[]{"${d}", "${c}", "${m}", "${l}", "${msg}", "${n}"},
                    new String[]{d, c, m, l, msg.toString(), System.lineSeparator()});
            bw.write(log);
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

    private static StackTraceElement getCaller(int level) {
        return Thread.currentThread().getStackTrace()[level];
    }

    private static String getDateTimeString() {
        return DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss.SSS");
    }

}