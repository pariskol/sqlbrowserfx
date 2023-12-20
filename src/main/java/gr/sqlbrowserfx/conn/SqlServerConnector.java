package gr.sqlbrowserfx.conn;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.LoggerFactory;

import gr.sqlbrowserfx.LoggerConf;

public class SqlServerConnector extends SqlConnector {

	private final String database;

	public SqlServerConnector(String database, String user, String password) {
		super("com.microsoft.sqlserver.jdbc.SQLServerDriver",
				"jdbc:sqlserver://localhost:1433;encrypt=false;databaseName=" + database ,
				user, password);
		this.database = database;
	}
	
	public SqlServerConnector(String url, String database, String user, String password) {
		super("com.microsoft.sqlserver.jdbc.SQLServerDriver", url, user, password);
		this.database = database;
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
		if (!isAutoCommitModeEnabled && this.getDataSource() instanceof BasicDataSource basicDataSource) {
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
			this.rollbackQuietly(conn);
		}
		for (Connection conn2 : connections)
			this.closeQuietly(conn2);
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
			this.closeQuietly(conn2);
	}
	
	@Override
	public String getContentsQuery() {
		return """
				SELECT table_name, table_type
				FROM information_schema.tables
				ORDER BY table_name ASC;
				""";
	}
	
	@Override
	public void getSchema(String name, ResultSetAction action) throws SQLException {
		// TODO: implement this method
	}

	@Override
	public String findPrimaryKey(String tableName) throws SQLException {
		// TODO: implement this method
		return null;
	}
	
	@Override
	public List<Map<String, String>> findForeignKeyReferences(String tableName) throws SQLException {
		// TODO: implement this method
		List<Map<String, String>> foreignKeys = new ArrayList<>();
		return foreignKeys;
	}
	
	@Override
	public String getTableSchemaColumn() {
		// TODO: implement this method
		return "";
	}

	@Override
	public String getViewSchemaColumn() {
		return "CreateSchema";
	}

	@Override
	public String getIndexSchemaColumn() {
		// TODO: implement this method
		return "";
	}
	

	@Override
	public String getDbSchema() {
		return database;
	}

	@Override
	public void getTriggers(String table, ResultSetAction action) throws SQLException {
		// TODO: implement this method
	}
	
	@Override
	public List<String> getTables() throws SQLException {
		List<String> tables = new ArrayList<>();
		this.executeQuery(
    """
				SELECT table_name, table_type
				FROM information_schema.tables
				WHERE table_type = 'BASE TABLE'
			""",
			rset -> {
				try {
					tables.add(rset.getString(1));
				} catch (Exception e) {
					LoggerFactory.getLogger(LoggerConf.LOGGER_NAME).error(e.getMessage());
				}
			}
		);
		return tables;
	}
	
	@Override
	public List<String> getViews() throws SQLException {
		List<String> tables = new ArrayList<>();
		this.executeQuery(
    	"""
				SELECT table_name, table_type
				FROM information_schema.tables
				WHERE table_type = 'VIEW'
			""",
			rset -> {
				try {
					tables.add(rset.getString(1));
				} catch (Exception e) {
					LoggerFactory.getLogger(LoggerConf.LOGGER_NAME).error(e.getMessage());
				}
			}
		);
		return tables;
	}
}
