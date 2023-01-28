package gr.sqlbrowserfx.conn;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.LoggerFactory;

import gr.sqlbrowserfx.LoggerConf;

public class MysqlConnector extends SqlConnector {

	private String SCHEMA_VIEW_QUERY;
	private String SCHEMA_TABLE_QUERY;
	private String SCHEMA_INDEX_QUERY;

	private String database;

	public MysqlConnector(String database, String user, String password) {
		super("com.mysql.cj.jdbc.Driver",
				"jdbc:mysql://localhost:3306/" + database + "?autoReconnect=true&useSSL=true&useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC",
				user, password);
		this.database = database;
		SCHEMA_VIEW_QUERY = "SHOW CREATE VIEW " + database + ".";
		SCHEMA_TABLE_QUERY = "SHOW CREATE TABLE " + database + ".";
		SCHEMA_INDEX_QUERY = "SELECT DISTINCT TABLE_NAME, COLUMN_NAME FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = '" + database + "' AND TABLE_NAME = ?";
	}
	
	public MysqlConnector(String url, String database, String user, String password) {
		super("com.mysql.cj.jdbc.Driver", url, user, password);
		this.database = database;
		SCHEMA_VIEW_QUERY = "SHOW CREATE VIEW " + database + ".";
		SCHEMA_TABLE_QUERY = "SHOW CREATE TABLE " + database + ".";
		SCHEMA_INDEX_QUERY = "SHOW CREATE TABLE " + database + ".";
	}

	
	@Override
	protected DataSource initDatasource() {
		BasicDataSource dbcp2DataSource = new BasicDataSource();
		dbcp2DataSource.setDriverClassName(this.getDriver());
		dbcp2DataSource.setUrl(this.getUrl());
		dbcp2DataSource.setUsername(this.getUser());
		dbcp2DataSource.setPassword(this.getPassword());
		dbcp2DataSource.setInitialSize(4);
		dbcp2DataSource.setMaxTotal(4);
		
		return dbcp2DataSource;
	}

	/**
	 * MySql has type checking return value as is.
	 */
	@Override
	public Object castToDBType(SqlTable sqlTable, String columnName, String text) {
		if ("true".equals(text)) {
			return 1;
		}
		else if ("false".equals(text)) {
			return 0;
		}
		return text;
	}

	@Override
	public void setAutoCommitModeEnabled(boolean isAutoCommitModeEnabled) {
		super.setAutoCommitModeEnabled(isAutoCommitModeEnabled);
		if (!isAutoCommitModeEnabled && this.getDataSource() instanceof BasicDataSource) {
			BasicDataSource basicDataSource = (BasicDataSource) this.getDataSource();
			basicDataSource.setAutoCommitOnReturn(false);
			basicDataSource.setDefaultAutoCommit(false);
			basicDataSource.setRollbackOnReturn(false);
			LoggerFactory.getLogger(LoggerConf.LOGGER_NAME).debug("Detected Apache BasicDataSource");
			LoggerFactory.getLogger(LoggerConf.LOGGER_NAME).debug("Disable autoCommit for all connections");
		}
	}
	
	@Override
	public void commitAll() {
		BasicDataSource dataSource = (BasicDataSource) this.getDataSource();
		List<Connection> connections = new ArrayList<>();
		Connection conn = null;
		try {
			int activeConnections = dataSource.getNumIdle();
			for (int i = 0; i < activeConnections; i++) {
					conn = dataSource.getConnection();
					conn.commit();
					connections.add(conn);
			}
			LoggerFactory.getLogger(LoggerConf.LOGGER_NAME).debug(activeConnections + " connections commited");
		} catch (SQLException e) {
			LoggerFactory.getLogger(LoggerConf.LOGGER_NAME).error("Failed to commit changes , about to rollback", e);
			this.rollbackQuitely(conn);
		}
		for (Connection conn2 : connections)
			this.closeQuitely(conn2);
	}

	@Override
	public void rollbackAll() {
		BasicDataSource dataSource = (BasicDataSource) this.getDataSource();
		List<Connection> connections = new ArrayList<>();
		Connection conn = null;
		try {
			int activeConnections = dataSource.getNumIdle();
			for (int i = 0; i < activeConnections; i++) {
					conn = dataSource.getConnection();
					conn.rollback();
					connections.add(conn);
			}
		} catch (SQLException e) {
			LoggerFactory.getLogger(LoggerConf.LOGGER_NAME).error("Failed to rollback changes", e);
		}
		for (Connection conn2 : connections)
			this.closeQuitely(conn2);
	}
	
	@Override
	public String getContentsQuery() {
		return "show full tables in " + database;
	}
	
	@Override
	public void getSchema(String name, ResultSetAction action) throws SQLException {
		try {
			this.executeQuery(SCHEMA_TABLE_QUERY + name, action);
		} catch (SQLException e) {
			try {
				this.executeQuery(SCHEMA_VIEW_QUERY + name, action);
			} catch (SQLException e2) {
				this.executeQuery(SCHEMA_INDEX_QUERY, Arrays.asList(name), action);
			}
		}
	}

	@Override
	public String findPrimaryKey(String tableName) throws SQLException {
		StringBuilder primaryKeyBuilder = new StringBuilder("");

		String query = "SELECT TABLE_NAME,COLUMN_NAME,CONSTRAINT_NAME, REFERENCED_TABLE_NAME,REFERENCED_COLUMN_NAME "
				+ "	FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND CONSTRAINT_NAME = ?";
		this.executeQuery(query, Arrays.asList(this.database, tableName, "PRIMARY"), rset -> {
			primaryKeyBuilder.append(rset.getString("COLUMN_NAME"));
			primaryKeyBuilder.append(",");
		});
		
		String primaryKey = primaryKeyBuilder.toString();
		if (!primaryKey.isEmpty())
			primaryKey = primaryKey.substring(0, primaryKey.length() - 1);

		return primaryKey.isEmpty() ? null : primaryKey;
	}
	
	@Override
	public List<Map<String, String>> findFoireignKeyReferences(String tableName) throws SQLException {
		List<Map<String, String>> foreignKeys = new ArrayList<>();
		String query = "SELECT TABLE_NAME,COLUMN_NAME,CONSTRAINT_NAME, REFERENCED_TABLE_NAME,REFERENCED_COLUMN_NAME "
				+ "	FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND CONSTRAINT_NAME != ?";
		this.executeQuery(query, Arrays.asList(this.database, tableName, "PRIMARY"), rset -> {
			Map<String, String> map = new HashMap<>();
			map.put(REFERENCED_KEY, rset.getString("REFERENCED_COLUMN_NAME"));
			map.put(REFERENCED_TABLE, rset.getString("REFERENCED_TABLE_NAME"));
			map.put(FOREIGN_KEY, rset.getString("COLUMN_NAME"));
			foreignKeys.add(map);
		});
			

		return foreignKeys;
	}
	
	@Override
	public String getTableSchemaColumn() {
		return "Create Table";
	}

	@Override
	public String getViewSchemaColumn() {
		return "Create View";
	}

	@Override
	public String getIndexSchemaColumn() {
		return "COLUMN_NAME";
	}
	

	@Override
	public String getDbSchema() {
		return database;
	}

	@Override
	public void getTriggers(String table, ResultSetAction action) throws SQLException {
		this.executeQuery("select TRIGGER_NAME, ACTION_STATEMENT from INFORMATION_SCHEMA.TRIGGERS  where EVENT_OBJECT_TABLE= ? and TRIGGER_SCHEMA = ?",
				Arrays.asList(table, database), action);
	}
	
	@Override
	public List<String> getTables() throws SQLException {
		List<String> tables = new ArrayList<>();
		this.executeQuery("show full tables where TABLE_TYPE = 'BASE TABLE'", rset -> {
			try {
				tables.add(rset.getString(1));
			} catch (Exception e) {
				LoggerFactory.getLogger(LoggerConf.LOGGER_NAME).error(e.getMessage());
			}
		});
		return tables;
	}
	
	@Override
	public List<String> getViews() throws SQLException {
		List<String> tables = new ArrayList<>();
		this.executeQuery("show full tables where TABLE_TYPE = 'VIEW'", rset -> {
			try {
				tables.add(rset.getString(1));
			} catch (Exception e) {
				LoggerFactory.getLogger(LoggerConf.LOGGER_NAME).error(e.getMessage());
			}
		});
		return tables;
	}
	
	@Override
	public Integer getLastGeneratedId() throws SQLException {
		AtomicInteger lastId = new AtomicInteger();
		this.executeQuery("select last_insert_id()", rset -> {
			lastId.set(rset.getInt(1));
		});
		return lastId.get();
	}
}
