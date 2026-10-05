package gr.sqlbrowserfx.utils;

import gr.sqlbrowserfx.LoggerConf;

import org.slf4j.LoggerFactory;

public class SqlBrowserFXThreadUtils {

    public interface ThrowableRunnable {

        void run() throws Exception;
    }

    /**
     * Creates a daemon thread that runs forever if not interrupted ,every given timeout milliseconds.
     * 
     * @param runnable
     * @param name
     * @param timeout
     * @return 
     */
    public static Thread createDaemonRepeaterThread(ThrowableRunnable runnable, String name, long timeout) {
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

    /**
     * Creates a daemon thread that instantly runs that given runnable.
     * 
     * @param runnable
     * @param name
     * @return 
     */
    public static Thread createDaemonThread(Runnable runnable, String name) {
        var thread = new Thread(() -> runnable.run(), name);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

}
