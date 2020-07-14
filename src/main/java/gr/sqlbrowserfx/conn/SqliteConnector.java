package gr.sqlbrowserfx.conn;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import javax.sql.DataSource;

import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteDataSource;

public class SqliteConnector extends SqlConnector {

	private final String SCHEMA_COLUMN = "sql";
	private final String SCHEMA_QUERY = "select sql from sqlite_master where name = ?";
	
	private LinkedBlockingQueue<UpdateQuery> updateQueriesQueue;
	private Connection updateConnection;
	
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

	@Override
	protected DataSource initDatasource() {
		SQLiteDataSource datasource = new SQLiteDataSource();
		datasource.setUrl(this.getUrl());
		try {
			this.updateConnection = datasource.getConnection();
		} catch (SQLException e) {
			LoggerFactory.getLogger(getClass()).error("Could not initialize connection", e);
		}
		return datasource;
	}
	
	@Override
	public void setAutoCommitModeEnabled(boolean isAutoCommitModeEnabled) {
		super.setAutoCommitModeEnabled(isAutoCommitModeEnabled);
		try {
			this.updateConnection.setAutoCommit(false);
		} catch (SQLException e) {
			LoggerFactory.getLogger(getClass()).error("Could not initialize connection", e);
		}
	}
	
	
	protected Connection getConnection() {
		return this.updateConnection;
	}
	
	@Override
	public int executeUpdate(String query) throws SQLException {
		int result = 0;
		if (isAutoCommitModeEnabled()) {
			result = super.executeUpdate(query);
		}
		else {
			Connection conn = getConnection();
			try (Statement statement = conn.createStatement();) {
				result = statement.executeUpdate(query);
			}
		}

		return result;
	}

	@Override
	public int executeUpdate(String query, List<Object> params) throws SQLException {
		int result = 0;
		if (isAutoCommitModeEnabled()) {
			result = super.executeUpdate(query);
		}
		else {
			Connection conn = getConnection();
			try (PreparedStatement statement = prepareStatementWithParams(conn, query, params);) {
				result = statement.executeUpdate();
			}
		}
		return result;
	}

	@Override
	public int executeUpdate(Connection conn, String query, List<Object> params) throws SQLException {
		int result = 0;
		if (isAutoCommitModeEnabled()) {
			result = super.executeUpdate(query);
		}
		else {
			try (PreparedStatement statement = prepareStatementWithParams(conn, query, params);) {
				result = statement.executeUpdate();
			}
		}

		return result;
	}
	
	@Override
	public void rollbackAll() {
		try {
			this.updateConnection.rollback();
		} catch (SQLException e) {
			LoggerFactory.getLogger(getClass()).error("Failed to commit changes , about to rollback", e);
		}
	}
	
	@Override
	public void commitAll() {
		try {
			this.updateConnection.commit();
		} catch (SQLException e) {
			LoggerFactory.getLogger(getClass()).error("Failed to commit changes , about to rollback", e);
			this.rollbackQuitely(this.updateConnection);
		}
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
