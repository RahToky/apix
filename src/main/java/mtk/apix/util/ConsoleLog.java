package mtk.apix.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * System print
 * @author mahatoky rasolonirina
 */
public class ConsoleLog {

    private static ConsoleLog instance;
    private static final String pattern = "[${level}] - ${date} - ${message}";
    private final static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private boolean show;

    private ConsoleLog() {
        show = true;
    }

    public static ConsoleLog getInstance(){
        if(instance == null){
            instance = new ConsoleLog();
        }
        return instance;
    }

    public enum Level {
        TRACE, INFO, WARN, ERROR
    }

    public void setShow(boolean show) {
        this.show = show;
    }

    public static void trace(String message) {
        ConsoleLog.getInstance().log(Level.TRACE, message, instance.show);
    }

    public static void info(String message) {
        ConsoleLog.getInstance().log(Level.INFO, message, instance.show);
    }

    public static void warn(String message) {
        ConsoleLog.getInstance().log(Level.WARN, message, instance.show);
    }

    public static void error(Throwable throwable) {
        ConsoleLog.getInstance().log(Level.ERROR, throwable.getMessage(), instance.show);
        throwable.printStackTrace();
    }

    public static void forcedLog(Level level, String message){
        ConsoleLog.getInstance().log(level, message, true);
    }

    private void log(Level level, String message, boolean show) {
        if(!show){
            return;
        }
        try {
            if (Level.ERROR.equals(level)) {
                System.err.println(pattern.replace("${level}", level.name()).replace("${date}", LocalDateTime.now().format(formatter)).replace("${message}", message));
            } else {
                System.out.println(pattern.replace("${level}", level.name()).replace("${date}", LocalDateTime.now().format(formatter)).replace("${message}", message));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
