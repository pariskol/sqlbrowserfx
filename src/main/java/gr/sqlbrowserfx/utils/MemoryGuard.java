package gr.sqlbrowserfx.utils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.LoggerFactory;

import gr.sqlbrowserfx.factories.DialogFactory;

public class MemoryGuard {

	private static int MEMORY_DIVIDER = 5;
	/**
	 * A new thread monitors the execution of a query, in
	 * order to cancel it if memory consumption gets very big, to avoid jvm memory
	 * crashes.
	 * 
	 * @param rset
	 */
	public static void startMemoryGuard(ResultSet rset) {
		new Thread(() -> {
			try {
				long heapMaxSize = Runtime.getRuntime().maxMemory();
				while (rset != null && !rset.isClosed()) {
					long currentUsage = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
					if (currentUsage > heapMaxSize - heapMaxSize / MEMORY_DIVIDER) {
						rset.close();
						LoggerFactory.getLogger("sqlbrowserfx").debug("Query was canceled due to fast growing memory consumption of ResultSet");
						System.gc();
						DialogFactory.createErrorDialog(new OutOfMemoryError("Fast growing memory consumption of ResultSet"));
						return;
					}
					Thread.sleep(100);
				}
			} catch (SQLException | InterruptedException e) {
				LoggerFactory.getLogger("sqlbrowserfx").error(e.getMessage(), e);
			} finally {
				try {
					rset.close();
				} catch (SQLException e) {
					LoggerFactory.getLogger("sqlbrowserfx").error(e.getMessage(), e);
				}
			}
		}, "MemoryGuard Thread").start();
	}
	
	/**
	 * A new thread monitors the execution of a query, in
	 * order to cancel it if memory consumption gets very big, to avoid jvm memory
	 * crashes.
	 * 
	 * @param statement
	 */
	public static void startMemoryGuard(Statement statement) {
		new Thread(() -> {
			try {
				long heapMaxSize = Runtime.getRuntime().maxMemory();
				while ((statement != null && !statement.isClosed()) ) {
					long currentUsage = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
					if (currentUsage > heapMaxSize - heapMaxSize / MEMORY_DIVIDER) {
						LoggerFactory.getLogger("sqlbrowserfx").debug("Query was canceled due to fast growing memory consumption of Statement");
						statement.cancel();
						System.gc();
						DialogFactory.createErrorDialog(new OutOfMemoryError("Fast growing memory consumption of Statement"));
						return;
					}
					Thread.sleep(100);
				}
			} catch (SQLException | InterruptedException e) {
				LoggerFactory.getLogger("sqlbrowserfx").error(e.getMessage(), e);
			} finally {
				try {
					statement.close();
				} catch (SQLException e) {
					LoggerFactory.getLogger("sqlbrowserfx").error(e.getMessage(), e);
				}
			}
		}, "MemoryGuard Thread").start();
	}
}
