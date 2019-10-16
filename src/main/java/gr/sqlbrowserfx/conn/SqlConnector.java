package gr.sqlbrowserfx.conn;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.sql.DataSource;

import org.slf4j.LoggerFactory;

public abstract class SqlConnector {

	protected DataSource dataSource;
	protected String driver;
	protected String url;
	protected ExecutorService executorService;

	public String NAME;
	public String TYPE;

	public SqlConnector() {
//		int processors = Runtime.getRuntime().availableProcessors() / 2;
//		if (processors < 2)
//			processors = 1;
//		else
//			processors = 2;
//
//		LoggerFactory.getLogger(getClass()).debug("Executor service threads = " + processors);
//		executorService = Executors.newFixedThreadPool(processors);
		executorService = Executors.newCachedThreadPool();
	}

	abstract protected void initDatasource();

	private PreparedStatement prepareStatementWithParams(Connection conn, String query, List<Object> params)
			throws SQLException {
		PreparedStatement statement = conn.prepareStatement(query);
		int i = 1;
		for (Object param : params) {
			if (param == null || param.toString().equals(""))
				statement.setNull(i++, Types.VARCHAR);
			else if (param instanceof Byte) {
				statement.setBytes(i++, (byte[])param);
			}
			else
				statement.setObject(i++, param);
		}
	
		return statement;
	}

	/**
	 * A new worker of the executor service monitors the execution of a query,
	 * in order to cancel it if memory consumption gets very big, to avoid
	 * jvm memory crashes.
	 * 
	 * @param statement
	 * @param rset
	 */
	private void avoidMemoryCrash(Statement statement, ResultSet rset) {
		executorService.execute(() -> {
			try {
				long heapMaxSize = Runtime.getRuntime().maxMemory();
				while (!statement.isClosed()) {
					long currentUsage = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
					if (currentUsage > heapMaxSize - heapMaxSize / 5) {
						statement.cancel();
						System.gc();
					}
					Thread.sleep(100);
				}
			} catch (SQLException | InterruptedException e) {
				LoggerFactory.getLogger(getClass()).error(e.getMessage(), e);
			} finally {
				try {
					statement.close();
					rset.close();
				} catch (SQLException e) {
					LoggerFactory.getLogger(getClass()).error(e.getMessage(), e);
				}
			}
		});
	}

	public void checkConnection() throws SQLException {
		try (Connection conn = dataSource.getConnection();) {
			LoggerFactory.getLogger(getClass()).info("Successful try to get connection , pool is ok.");
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
			action.use(rset);
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
			action.use(rset);
			System.gc();
		}
	}

	/**
	 * Executes query. Action provided is applied to the whole result set.
	 * This function also avoid jvm memory crashes due to too big result set,
	 * by canceling the query if memory consumption gets too high.
	 * 
	 * @param query
	 * @param action
	 * @throws SQLException
	 */
	public void executeQueryRawSafely(String query, ResultSetAction action) throws SQLException {
	
		try (Connection conn = dataSource.getConnection();
				Statement statement = conn.createStatement();
				ResultSet rset = statement.executeQuery(query);) {
	
			this.avoidMemoryCrash(statement, rset);
			action.use(rset);
			System.gc();
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
				action.use(rset);
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
				action.use(rset);
			}
		}
	}

	/**
	 * Executes query asynchronously through {@link gr.sqlbrowserfx.conn.SqlConnector}
	 * executor service. Action provided is applied to each row of result set.
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
				LoggerFactory.getLogger("SQLBROWSER").error(e.getMessage(), e);
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

	public int executeUpdate(String query, List<Object> params) throws SQLException {
		int result = 0;
		try (Connection conn = dataSource.getConnection();
				PreparedStatement statement = prepareStatementWithParams(conn, query, params);) {
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
		String primaryKey = null;

		try (Connection conn = dataSource.getConnection();) {
			DatabaseMetaData meta = conn.getMetaData();
			try (ResultSet rset = meta.getPrimaryKeys(null, null, tableName);) {
				while (rset.next())
					primaryKey = rset.getString("COLUMN_NAME");
			}
		}

		return primaryKey;
	}

	public List<String> findForeignKeys(String tableName) throws SQLException {
		List<String> foreignKeys = new ArrayList<>();
		try (Connection conn = dataSource.getConnection();) {
			DatabaseMetaData meta = conn.getMetaData();
			try (ResultSet rset = meta.getImportedKeys(null, null, tableName);) {
				while (rset.next()) {
					String foreignKey = rset.getString("FKCOLUMN_NAME");
					foreignKeys.add(foreignKey);
				}
			}
		}

		return foreignKeys;
	}

	public String findFoireignKeyReference(String tableName, String key) throws SQLException {
		try (Connection conn = dataSource.getConnection();) {
			DatabaseMetaData meta = conn.getMetaData();
			try (ResultSet rset = meta.getImportedKeys(null, null, tableName);) {
				while (rset.next()) {
					String referenceKey = rset.getString("PKCOLUMN_NAME");
					String referenceTable = rset.getString("PKTABLE_NAME");
					String foreignKey = rset.getString("FKCOLUMN_NAME");

					if (foreignKey.equals(key))
						return referenceKey + " : " + referenceTable;

				}
			}
		}

		return null;
	}

	public abstract Object castToDBType(SqlTable sqlTable, String columnName, String text);

	public Connection getConnection() throws SQLException {
		return dataSource.getConnection();
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

	public String getContentsQuery() {
		return null;
	}

	abstract public void getSchemas(String name, ResultSetAction action) throws SQLException;

	public void getContents(ResultSetAction action) throws SQLException {
		this.executeQuery(getContentsQuery(), action);
	}

	public abstract String getTableSchemaColumn();

	public abstract String getViewSchemaColumn();

	public abstract String getIndexColumnName();
}
