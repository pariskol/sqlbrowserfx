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
	public void getTableSchema(String name, ResultSetAction action) throws SQLException {
		this.executeQuery(
				"""
				SELECT 
				    'CREATE TABLE [' + s.name + '].[' + t.name + '] (' + CHAR(13) +
				    STRING_AGG(
				        '    [' + c.name + '] ' + 
				        UPPER(ty.name) +
				        CASE 
				            WHEN ty.name IN ('varchar','char','varbinary','binary','nvarchar','nchar')
				                THEN '(' + 
				                    CASE WHEN c.max_length = -1 THEN 'MAX'
				                         WHEN ty.name LIKE 'n%' THEN CAST(c.max_length / 2 AS VARCHAR(10))
				                         ELSE CAST(c.max_length AS VARCHAR(10))
				                    END + ')'
				            WHEN ty.name IN ('decimal','numeric')
				                THEN '(' + CAST(c.precision AS VARCHAR(10)) + ',' + CAST(c.scale AS VARCHAR(10)) + ')'
				            ELSE ''
				        END + ' ' +
				        CASE WHEN c.is_nullable = 0 THEN 'NOT NULL' ELSE 'NULL' END +
				        CASE WHEN ic.seed_value IS NOT NULL THEN ' IDENTITY(' + CAST(ic.seed_value AS VARCHAR(10)) + ',' + CAST(ic.increment_value AS VARCHAR(10)) + ')' ELSE '' END +
				        CASE WHEN dc.definition IS NOT NULL THEN ' DEFAULT ' + dc.definition ELSE '' END
				        , ',' + CHAR(13)
				        ) WITHIN GROUP (ORDER BY c.column_id)
				    + 
				    CASE 
				        WHEN pk.pkdef IS NOT NULL THEN ',' + CHAR(13) + pk.pkdef
				        ELSE ''
				    END
				    + CHAR(13) + ');' AS create_statement
				FROM sys.tables t
				JOIN sys.schemas s ON t.schema_id = s.schema_id
				JOIN sys.columns c ON t.object_id = c.object_id
				JOIN sys.types ty ON c.user_type_id = ty.user_type_id
				LEFT JOIN sys.default_constraints dc ON c.default_object_id = dc.object_id
				LEFT JOIN sys.identity_columns ic ON c.object_id = ic.object_id AND c.column_id = ic.column_id
				OUTER APPLY (
				    SELECT 
				        '    CONSTRAINT [' + MAX(i.name) + '] PRIMARY KEY (' +
				        STRING_AGG('[' + c2.name + ']', ', ') WITHIN GROUP (ORDER BY ic2.key_ordinal) + ')'
				        AS pkdef
				    FROM sys.indexes i
				    JOIN sys.index_columns ic2 ON i.object_id = ic2.object_id AND i.index_id = ic2.index_id
				    JOIN sys.columns c2 ON ic2.object_id = c2.object_id AND ic2.column_id = c2.column_id
				    WHERE i.is_primary_key = 1 AND i.object_id = t.object_id
				) pk
				WHERE t.name = ?
				GROUP BY s.name, t.name, pk.pkdef;
				""",
				Arrays.asList(name),
				action);
	}
	
	@Override
	public void getViewSchema(String name, ResultSetAction action) throws SQLException {
		this.executeQuery(
			"""
			SELECT 
			    m.definition AS create_statement
			FROM sys.views v
			JOIN sys.schemas s ON v.schema_id = s.schema_id
			JOIN sys.sql_modules m ON v.object_id = m.object_id
			WHERE v.name = ?
			""",
			Arrays.asList(name),
			action
		);
	}

	@Override
	public void getIndexSchema(String name, ResultSetAction action) throws SQLException {
		this.executeQuery(
				"""
				SELECT 
				    'CREATE ' + 
				    CASE WHEN i.is_unique = 1 THEN 'UNIQUE ' ELSE '' END + 
				    'INDEX [' + i.name + '] ON [' + s.name + '].[' + t.name + '] (' +
				    STRING_AGG('[' + c.name + ']' + 
				        CASE WHEN ic.is_descending_key = 1 THEN ' DESC' ELSE ' ASC' END, ', '
				        ) WITHIN GROUP (ORDER BY ic.key_ordinal) + ')' AS create_statement
				FROM sys.indexes i
				JOIN sys.tables t ON i.object_id = t.object_id
				JOIN sys.schemas s ON t.schema_id = s.schema_id
				JOIN sys.index_columns ic ON i.object_id = ic.object_id AND i.index_id = ic.index_id
				JOIN sys.columns c ON ic.object_id = c.object_id AND ic.column_id = c.column_id
				WHERE i.is_primary_key = 0 
				  AND i.is_unique_constraint = 0 
				  AND t.name = ?
				GROUP BY s.name, t.name, i.name, i.is_unique;
				""",
				Arrays.asList(name),
				action
			);
	}

	@Override
	public String findPrimaryKey(String tableName) throws SQLException {
		StringBuilder primaryKeyBuilder = new StringBuilder();

		this.executeQuery(
			"""
			SELECT c.name AS COLUMN_NAME 
			FROM sys.indexes i 
			INNER JOIN sys.index_columns ic ON i.object_id = ic.object_id AND i.index_id = ic.index_id 
			INNER JOIN sys.columns c ON ic.object_id = c.object_id AND ic.column_id = c.column_id 
			INNER JOIN sys.tables t ON i.object_id = t.object_id 
			WHERE i.is_primary_key = 1 AND t.name = ?	
			""", 
			Arrays.asList(tableName), 
			rset -> {
				primaryKeyBuilder.append(rset.getString("COLUMN_NAME"));
				primaryKeyBuilder.append(",");
			}
		);

		String primaryKey = primaryKeyBuilder.toString();
		if (!primaryKey.isEmpty()) {
			primaryKey = primaryKey.substring(0, primaryKey.length() - 1);
		}

		return primaryKey.isEmpty() ? null : primaryKey;
	}

	@Override
	public List<Map<String, String>> findForeignKeyReferences(String tableName) throws SQLException {
		List<Map<String, String>> foreignKeys = new ArrayList<>();
		this.executeQuery(
			"""
			SELECT COL_NAME(fc.parent_object_id, fc.parent_column_id) AS COLUMN_NAME, 
			OBJECT_NAME(f.referenced_object_id) AS REFERENCED_TABLE_NAME, 
			COL_NAME(fc.referenced_object_id, fc.referenced_column_id) AS REFERENCED_COLUMN_NAME 
			FROM sys.foreign_keys AS f 
			INNER JOIN sys.foreign_key_columns AS fc ON f.object_id = fc.constraint_object_id 
			INNER JOIN sys.tables t ON t.object_id = fc.parent_object_id 
			WHERE t.name = ?
			""", 
			Arrays.asList(tableName), 
			rset -> {
				Map<String, String> map = new HashMap<>();
				map.put(REFERENCED_KEY, rset.getString("REFERENCED_COLUMN_NAME"));
				map.put(REFERENCED_TABLE, rset.getString("REFERENCED_TABLE_NAME"));
				map.put(FOREIGN_KEY, rset.getString("COLUMN_NAME"));
				foreignKeys.add(map);
			}
		);

		return foreignKeys;
	}
	
	@Override
	public String getDbSchema() {
		return database;
	}

	@Override
	public void getTriggers(String table, ResultSetAction action) throws SQLException {
		this.executeQuery(
			"""
			SELECT name AS TRIGGER_NAME, OBJECT_DEFINITION(object_id) AS ACTION_STATEMENT 
			FROM sys.triggers WHERE parent_id = OBJECT_ID(?)
			""",
			Arrays.asList(table), action);
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
	
	@Override
	public Integer getLastGeneratedId() throws SQLException {
		AtomicInteger lastId = new AtomicInteger();
		this.executeQuery("SELECT CAST(SCOPE_IDENTITY() AS INT)", rset -> lastId.set(rset.getInt(1)));
		return lastId.get();
	}

}
