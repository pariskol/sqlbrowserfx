package gr.sqlbrowserfx.utils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.LoggerFactory;

import gr.sqlbrowserfx.LoggerConf;
import gr.sqlbrowserfx.factories.DialogFactory;

public class MemoryGuard {

	private static final int MEMORY_DIVIDER = 10;
	public static Integer SQL_MEMORY_ERROR_CODE = 1234;
	/**
	 * A new thread monitors the execution of a query, in
	 * order to cancel it if memory consumption gets very big, to avoid jvm memory
	 * crashes.
	 * 
	 * @param rset
	 */
	public static void protect(ResultSet rset) {
		new Thread(() -> {
			try {
				long heapMaxSize = Runtime.getRuntime().maxMemory();
				while (rset != null && !rset.isClosed()) {
					long currentUsage = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
					if (currentUsage > heapMaxSize - heapMaxSize / MEMORY_DIVIDER) {
						rset.close();
						LoggerFactory.getLogger(LoggerConf.LOGGER_NAME).debug("Query was canceled due to fast growing memory consumption of sql result set");
						System.gc();
						DialogFactory.createErrorNotification(new OutOfMemoryError("Fast growing memory consumption of sql result set"));
						return;
					}
					Thread.sleep(100);
				}
			} catch (Throwable e) {
				LoggerFactory.getLogger(LoggerConf.LOGGER_NAME).error(e.getMessage(), e);
			} finally {
				try {
					rset.close();
				} catch (SQLException e) {
					LoggerFactory.getLogger(LoggerConf.LOGGER_NAME).error(e.getMessage(), e);
				}
			}
		}, "memory-guard").start();
	}
	
	/**
	 * A new thread monitors the execution of a query, in
	 * order to cancel it if memory consumption gets very big, to avoid jvm memory
	 * crashes.
	 * 
	 * @param statement
	 */
	public static void protect(Statement statement) {
		new Thread(() -> {
			try {
				long heapMaxSize = Runtime.getRuntime().maxMemory();
				while ((statement != null && !statement.isClosed()) ) {
					long currentUsage = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
					if (currentUsage > heapMaxSize - heapMaxSize / MEMORY_DIVIDER) {
						LoggerFactory.getLogger(LoggerConf.LOGGER_NAME).debug("Query was canceled due to fast growing memory consumption of sql statement");
						statement.cancel();
						System.gc();
						DialogFactory.createErrorNotification(new OutOfMemoryError("Fast growing memory consumption of sql statement"));
						return;
					}
					Thread.sleep(100);
				}
			} catch (Throwable e) {
				LoggerFactory.getLogger(LoggerConf.LOGGER_NAME).error(e.getMessage(), e);
			} finally {
				try {
					statement.close();
				} catch (SQLException e) {
					LoggerFactory.getLogger(LoggerConf.LOGGER_NAME).error(e.getMessage(), e);
				}
			}
		}, "memory-guard").start();
	}
}
