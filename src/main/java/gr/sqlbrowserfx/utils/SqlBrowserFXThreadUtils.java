package gr.sqlbrowserfx.utils;

import gr.sqlbrowserfx.LoggerConf;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.LoggerFactory;

public class SqlBrowserFXThreadUtils {

    public interface ThrowableRunnable {

        void run() throws Exception;
    }

    public static Thread createDaemonThread(ThrowableRunnable runnable, String name, long timeout) {
        var daemon = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    runnable.run();
                    Thread.sleep(timeout);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    LoggerFactory.getLogger(LoggerConf.LOGGER_NAME).error(name, "Exception ignored use try catch in runnable.", e);
                }
            }
        }, name);
        daemon.setDaemon(true);
        daemon.start();
        return daemon;
    }

    public static Thread createThread(Runnable runnable, String name) {
        var thread = new Thread(() -> runnable.run(), name);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    public static List<Thread> createDaemonThreads(ThrowableRunnable runnable, String prefixName, long timeout, int count) {
        var daemons = new ArrayList<Thread>();
        for (var i = 0; i < count; i++) {
            var daemon = createDaemonThread(runnable, prefixName + "-" + i, timeout);
            daemons.add(daemon);
        }
        return daemons;
    }
}
