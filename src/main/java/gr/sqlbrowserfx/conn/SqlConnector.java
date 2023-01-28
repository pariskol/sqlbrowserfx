package gr.sqlbrowserfx.conn;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.slf4j.LoggerFactory;

import gr.sqlbrowserfx.LoggerConf;
import gr.sqlbrowserfx.utils.MemoryGuard;

public abstract class SqlConnector {

	public static final String FOREIGN_KEY = "FKCOLUMN_NAME";
	public static final String REFERENCED_TABLE = "PKTABLE_NAME";
	public static final String REFERENCED_KEY = "PKCOLUMN_NAME";
	private DataSource dataSource;
	private String driver;
	private String url;
	private String user;
	private String password;
	private ExecutorService executorService;
	private boolean isAutoCommitModeEnabled = true;

	public SqlConnector() {
		BasicThreadFactory factory = new BasicThreadFactory.Builder()
			     .namingPattern(getClass().getSimpleName() + "-pool-thread-%d")
			     .daemon(true)
			     .priority(Thread.NORM_PRIORITY)
			     .build();
		executorService = Executors.newCachedThreadPool(factory);
	}

	public SqlConnector(String driver, String url, String user, String password) {
		this();
		this.url = url;
		this.driver = driver;
		this.user = user;
		this.password = password;
		dataSource = this.initDatasource();
		this.startConnectionMonitorDaemon();
	}

	protected abstract DataSource initDatasource();

	public void startConnectionMonitorDaemon() {
		int max = 10;
		Thread daemon = new Thread(() -> {
			int i = 0;
			while(!Thread.currentThread().isInterrupted()) {
				try {
					SqlConnector.this.checkConnection();
					i = 0;
					Thread.sleep(600000);
				} catch (SQLException e) {
					LoggerFactory.getLogger(LoggerConf.LOGGER_NAME).error(e.getMessage());
					if ( i == max)
						break;
					i++;
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e1) {
					}
					SqlConnector.this.initDatasource();
				} catch (Exception e) {
					LoggerFactory.getLogger(LoggerConf.LOGGER_NAME).error(e.getMessage());
				}
			}
		}, getClass().getSimpleName() + "-connection-monitor");
		daemon.setDaemon(true);
		daemon.start();
	}
	protected PreparedStatement prepareStatementWithParams(Connection conn, String query, List<Object> params)
			throws SQLException {
		PreparedStatement statement = conn.prepareStatement(query);
		int i = 1;
		for (Object param : params) {
			if (param == null || param.toString().equals(""))
				statement.setNull(i++, Types.VARCHAR);
			else if (param instanceof byte[]) {
				statement.setBytes(i++, (byte[]) param);
			} else
				statement.setObject(i++, param);
		}

		return statement;
	}

	public void checkConnection() throws SQLException {
		try (Connection conn = dataSource.getConnection();) {
			LoggerFactory.getLogger(LoggerConf.LOGGER_NAME).debug("Successful try to get connection , pool is ok.");
		}
	}

	/**
	 * Executes query. Action provided is applied to the whole result set.
	 * 
	 * @param query
	 * @param action
	 * @throws SQLException
	 */
	public void executeQueryRaw(String query, ResultSetAction action) throws SQLException {

		try (Connection conn = dataSource.getConnection();
				Statement statement = conn.createStatement();
				ResultSet rset = statement.executeQuery(query);) {
			action.onResultSet(rset);
		}
	}

	/**
	 * Executes query. Action provided is applied to the whole result set.
	 * 
	 * @param query
	 * @param params
	 * @param action
	 * @throws SQLException
	 */
	public void executeQueryRaw(String query, List<Object> params, ResultSetAction action) throws SQLException {

		try (Connection conn = dataSource.getConnection();
				PreparedStatement statement = prepareStatementWithParams(conn, query, params);
				ResultSet rset = statement.executeQuery(query);) {
			action.onResultSet(rset);
			System.gc();
		}
	}

	/**
	 * Executes query. Action provided is applied to the whole result set. This
	 * function also avoid jvm memory crashes due to too big result set, by
	 * canceling the query if memory consumption gets too high.
	 * 
	 * @param query
	 * @param action
	 * @throws SQLException
	 */
	public void executeQueryRawSafely(String query, ResultSetAction action) throws SQLException {

		ResultSet rset = null;
		try (Connection conn = dataSource.getConnection();
			 Statement statement = conn.createStatement();) {
			MemoryGuard.protect(statement);
			rset = statement.executeQuery(query);
			action.onResultSet(rset);
			System.gc();
		} finally {
			this.closeQuitely(rset);
		}
	}

	/**
	 * Executes query. Action provided is applied to each row of result set.
	 * 
	 * @param query
	 * @param action
	 * @throws SQLException
	 */
	public void executeQuery(String query, ResultSetAction action) throws SQLException {

		try (Connection conn = dataSource.getConnection();
				Statement statement = conn.createStatement();
				ResultSet rset = statement.executeQuery(query);) {
			while (rset.next()) {
				action.onResultSet(rset);
			}
		}
	}

	/**
	 * Executes query. Action provided is applied to each row of result set.
	 * 
	 * @param query
	 * @param params
	 * @param action
	 * @throws SQLException
	 */
	public void executeQuery(String query, List<Object> params, ResultSetAction action) throws SQLException {
		try (Connection conn = dataSource.getConnection();
				PreparedStatement statement = prepareStatementWithParams(conn, query, params);
				ResultSet rset = statement.executeQuery();) {
			while (rset.next()) {
				action.onResultSet(rset);
			}
		}
	}
	
	/**
	 * Use this function if you want to register a button action to cancel query,
	 * by canceling statement
	 * 
	 * @param query
	 * @param action
	 * @param statementAction
	 * @throws SQLException
	 */
	public void executeCancelableQuery(String query, ResultSetAction action, StatementAction statementAction) throws SQLException {
		ResultSet rset = null;
		try (Connection conn = dataSource.getConnection();
				Statement statement = conn.createStatement();) {
			statementAction.onStatement(statement);
			MemoryGuard.protect(statement);
			rset = statement.executeQuery(query);
			action.onResultSet(rset);
		} finally {
			this.closeQuitely(rset);
		}
	}

	/**
	 * Executes query asynchronously through
	 * {@link gr.sqlbrowserfx.conn.SqlConnector} executor service. Action provided
	 * is applied to each row of result set.
	 * 
	 * @param query
	 * @param action
	 * @throws SQLException
	 */
	public void executeQueryAsync(String query, List<Object> params, ResultSetAction action) throws SQLException {
		executorService.execute(() -> {
			try {
				this.executeQuery(query, params,action);
			} catch (SQLException e) {
				LoggerFactory.getLogger(LoggerConf.LOGGER_NAME).error(e.getMessage(), e);
			}
		});
	}
	
	/**
	 * Executes query asynchronously through
	 * {@link gr.sqlbrowserfx.conn.SqlConnector} executor service. Action provided
	 * is applied to each row of result set.
	 * 
	 * @param query
	 * @param action
	 * @throws SQLException
	 */
	public void executeQueryAsync(String query, ResultSetAction action) throws SQLException {
		executorService.execute(() -> {
			try {
				this.executeQuery(query, action);
			} catch (SQLException e) {
				LoggerFactory.getLogger(LoggerConf.LOGGER_NAME).error(e.getMessage(), e);
			}
		});
	}
	
	public void executeQueryRawAsync(String query, ResultSetAction action) {
		executorService.execute(() -> {
			try {
				this.executeQueryRaw(query, action);
			} catch (SQLException e) {
				LoggerFactory.getLogger(LoggerConf.LOGGER_NAME).error(e.getMessage(), e);
			}
		});
	}

	public int executeUpdate(String query) throws SQLException {
		int result = 0;
		try (Connection conn = dataSource.getConnection(); Statement statement = conn.createStatement();) {
			result = statement.executeUpdate(query);
		}

		return result;
	}
	
	public void executeUpdateAsync(String query) throws SQLException {
		this.executeAsync(() -> {
			try {
				this.executeUpdate(query);
			} catch (SQLException e) {
				LoggerFactory.getLogger(LoggerConf.LOGGER_NAME).error(e.getMessage(), e);
			}
		});
	}

	public void executeUpdateAsync(String query, List<Object> params) throws SQLException {
		this.executeAsync(() -> {
			try {
				this.executeUpdate(query, params);
			} catch (SQLException e) {
				LoggerFactory.getLogger(LoggerConf.LOGGER_NAME).error(e.getMessage(), e);
			}
		});
	}
	public int executeUpdate(String query, List<Object> params) throws SQLException {
		int result = 0;
		try (Connection conn = dataSource.getConnection();
				PreparedStatement statement = prepareStatementWithParams(conn, query, params);) {
			result = statement.executeUpdate();
		}

		return result;
	}

	public int executeUpdate(Connection conn, String query, List<Object> params) throws SQLException {
		int result = 0;
		try (PreparedStatement statement = prepareStatementWithParams(conn, query, params);) {
			result = statement.executeUpdate();
		}

		return result;
	}

	/**
	 * Pass runnable to be executed by {@link gr.sqlfx.conn.sqlConnector} executor
	 * service.
	 * 
	 * @param runnable
	 */
	public void executeAsync(Runnable runnable) {
		executorService.execute(runnable);
	}

	public void dropTable(String table) throws SQLException {
		this.executeUpdate("drop table " + table);
	}

	public void dropView(String view) throws SQLException {
		this.executeUpdate("drop view " + view);
	}

	public void dropIndex(String index) throws SQLException {
		this.executeUpdate("drop index " + index);
	}

	public String findPrimaryKey(String tableName) throws SQLException {
		String primaryKey = "";

		try (Connection conn = dataSource.getConnection();) {
			DatabaseMetaData meta = conn.getMetaData();
			ResultSet rset = meta.getPrimaryKeys(null, getDbSchema(), tableName);
			while (rset.next())
				primaryKey += rset.getString("COLUMN_NAME") + ",";
		}
		if (!primaryKey.isEmpty())
			primaryKey = primaryKey.substring(0, primaryKey.length() - 1);

		return primaryKey.isEmpty() ? null : primaryKey;
	}

	public void rollbackQuitely(Connection conn) {
		try {
			conn.rollback();
			LoggerFactory.getLogger(LoggerConf.LOGGER_NAME).debug("Successful rollback");
		} catch (SQLException e) {
			LoggerFactory.getLogger(LoggerConf.LOGGER_NAME).error(e.getMessage(), e);
		}
	}
	
	protected void closeQuitely(AutoCloseable closeable) {
		try {
			if (closeable != null)
				closeable.close();
		} catch (Throwable e) {
		}
	}

	public List<String> findForeignKeys(String tableName) throws SQLException {
		return findFoireignKeyReferences(tableName).stream().map(x -> x.get(FOREIGN_KEY)).collect(Collectors.toList());
	}

	public List<Map<String, String>> findFoireignKeyReferences(String tableName) throws SQLException {
		List<Map<String, String>> foreignKeys = new ArrayList<>();
		try (Connection conn = dataSource.getConnection();) {
			DatabaseMetaData meta = conn.getMetaData();
			try (ResultSet rset = meta.getImportedKeys(null, getDbSchema(), tableName);) {
				while (rset.next()) {
					Map<String, String> map = new HashMap<>();
					map.put(REFERENCED_KEY, rset.getString(REFERENCED_KEY));
					map.put(REFERENCED_TABLE, rset.getString(REFERENCED_TABLE));
					map.put(FOREIGN_KEY, rset.getString(FOREIGN_KEY));
					foreignKeys.add(map);
				}
			}
		}

		return foreignKeys;
	}

	public abstract Object castToDBType(SqlTable sqlTable, String columnName, String text);

	protected DataSource getDataSource() {
		return this.dataSource;
	}

	public List<String> getTVTypes(String[] types) throws SQLException {
		List<String> tables = new ArrayList<>();

		try (Connection conn = dataSource.getConnection();) {
			DatabaseMetaData dbmd = conn.getMetaData();
			try (ResultSet rs = dbmd.getTables(null, null, "%", types);) {
				while (rs.next()) {
					tables.add(rs.getString("TABLE_NAME"));
				}
			}
		}

		return tables;
	}

	public List<String> getTables() throws SQLException {
		return getTVTypes(new String[] { "TABLE" });
	}

	public List<String> getViews() throws SQLException {
		return getTVTypes(new String[] { "VIEW" });
	}

	public ExecutorService getExecutorService() {
		return executorService;
	}

	public void setExecutorService(ExecutorService executorService) {
		this.executorService = executorService;
	}

	public String getDbSchema() {
		return null;
	}
	
	abstract public String getContentsQuery();
	
	abstract public void getTriggers(String table, ResultSetAction action) throws SQLException;
	
	abstract public void getSchema(String name, ResultSetAction action) throws SQLException;

	public void getContents(ResultSetAction action) throws SQLException {
		this.executeQuery(getContentsQuery(), action);
	}

	public abstract String getTableSchemaColumn();

	public abstract String getViewSchemaColumn();

	public abstract String getIndexSchemaColumn();


	public String getDriver() {
		return driver;
	}

	public void setDriver(String driver) {
		this.driver = driver;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	
	public boolean isAutoCommitModeEnabled() {
		return isAutoCommitModeEnabled;
	}

	/**
	 * 
	 * This method must be called before trying to get any connection
	 * from datasource otherwise it will not work.
	 * 
	 * @param isAutoCommitModeEnabled
	 */
	public void setAutoCommitModeEnabled(boolean isAutoCommitModeEnabled) {
		this.isAutoCommitModeEnabled = isAutoCommitModeEnabled;
	}

	public abstract void commitAll();

	public abstract void rollbackAll();

	public Integer getLastGeneratedId() throws SQLException {
		return -1;
	}
}
