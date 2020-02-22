package gr.sqlbrowserfx.conn;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.LoggerFactory;

public class SqliteConnector extends SqlConnector {

	private final String SCHEMA_COLUMN = "sql";
	private final String SCHEMA_QUERY = "select sql from sqlite_master where name = ?";
	
	private LinkedBlockingQueue<UpdateQuery> updateQueriesQueue;
	
	public SqliteConnector(String database) {
		super("org.sqlite.JDBC", "jdbc:sqlite:" + database, null, null);
		this.updateQueriesQueue = new LinkedBlockingQueue<UpdateQuery>();
		this.startUpdateExecutor();
	}

	private void startUpdateExecutor() {
		Thread updatesExecutorThread = new Thread(() -> {
			try (Connection conn = this.getConnection();) {
				while(!Thread.currentThread().isInterrupted()) {
					UpdateQuery updateQuery;
					try {
						updateQuery = updateQueriesQueue.take();
						LoggerFactory.getLogger(getClass()).info("Executing update");
						super.executeUpdate(conn, updateQuery.getQuery(), updateQuery.getParams());
					} catch (Throwable e) {
						LoggerFactory.getLogger(getClass()).error(e.getMessage(), e);
					}
				}
			} catch (SQLException e1) {
				LoggerFactory.getLogger(getClass()).error(e1.getMessage(), e1);
			}
		}, "SQLite updates executor");
		updatesExecutorThread.setDaemon(true);
		updatesExecutorThread.start();
	}

//	@Override
//	protected DataSource initDatasource() {
//		SQLiteDataSource datasource = new SQLiteDataSource();
//		datasource.setUrl(this.getUrl());
//		return datasource;
//	}
	
	@Override
	protected DataSource initDatasource() {
		BasicDataSource dbcp2DataSource = new BasicDataSource();
		dbcp2DataSource.setDriverClassName(this.getDriver());
		dbcp2DataSource.setUrl(this.getUrl());
		dbcp2DataSource.setInitialSize(2);
		dbcp2DataSource.setMaxTotal(4);
		return dbcp2DataSource;
	}
	
	public int executeUpdateSerially(String query, List<Object> params) throws SQLException {
		try {
			this.updateQueriesQueue.put(new UpdateQuery(query, params));
		} catch (InterruptedException e) {
			LoggerFactory.getLogger(getClass()).error(e.getMessage(), e);
		}
		return 2;
	}
	
	@Override
	public Object castToDBType(SqlTable table, String label, String value) {
		Object actualValue = null;

		if (table.getColumnsMap().get(label).equals("INTEGER") && value != null && !value.isEmpty()) {
			Integer integerValue = Integer.parseInt(value);
			actualValue = integerValue.intValue();
		} else if (table.getColumnsMap().get(label).equals("REAL")  && value != null && !value.isEmpty()) {
			Double doubleValue = Double.parseDouble(value);
			actualValue = doubleValue.doubleValue();
		} else {
			actualValue = value;
		}
		return actualValue;
	}
	
	
	@Override
	public String getContentsQuery() {
		return "select name,type from sqlite_master";
	}

	@Override
	public void getSchemas(String name, ResultSetAction action) throws SQLException {
		this.executeQuery(SCHEMA_QUERY, Arrays.asList(name), action);
	}

	@Override
	public String getTableSchemaColumn() {
		return SCHEMA_COLUMN;
	}

	@Override
	public String getViewSchemaColumn() {
		return SCHEMA_COLUMN;
	}

	@Override
	public String getIndexColumnName() {
		return SCHEMA_COLUMN;
	}
	
}
