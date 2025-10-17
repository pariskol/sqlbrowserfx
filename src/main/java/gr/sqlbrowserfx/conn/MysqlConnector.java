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

	private final String database;

	public MysqlConnector(String database, String user, String password) {
		super("com.mysql.cj.jdbc.Driver",
				"jdbc:mysql://localhost:3306/" + database + "?autoReconnect=true&useSSL=true&useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC",
				user, password);
		this.database = database;
	}
	
	public MysqlConnector(String url, String database, String user, String password) {
		super("com.mysql.cj.jdbc.Driver", url, user, password);
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
		return "show full tables in " + database;
	}
	
	@Override
	public void getTableSchema(String name, ResultSetAction action) throws SQLException {
		// FIXME: cannot make it work triple quoted
		String sql = "SELECT CONCAT('CREATE TABLE `', t.TABLE_SCHEMA, '`.`', t.TABLE_NAME, '` ( ', GROUP_CONCAT(CONCAT('  `', c.COLUMN_NAME, '` ', c.COLUMN_TYPE, IF(c.IS_NULLABLE='NO',' NOT NULL',''), IF(c.COLUMN_DEFAULT IS NOT NULL, CONCAT(' DEFAULT \\'', REPLACE(c.COLUMN_DEFAULT,'\\\\','\\\\\\\\'), '\\''),''), IF(c.EXTRA<>'', CONCAT(' ', c.EXTRA),''), IF(c.COLUMN_COMMENT<>'', CONCAT(' COMMENT \\'', REPLACE(c.COLUMN_COMMENT,'\\'','\\\\\\''), '\\''),'') ) ORDER BY c.ORDINAL_POSITION SEPARATOR ', '), IF(pk.primary_key IS NOT NULL, CONCAT(', PRIMARY KEY (', pk.primary_key, ')'), ''), IF(uk.unique_keys IS NOT NULL, CONCAT(', ', uk.unique_keys), ''), IF(fk.foreign_keys IS NOT NULL, CONCAT(', ', fk.foreign_keys), ''), ' ) ENGINE=', MAX(t.ENGINE), ' DEFAULT CHARSET=', MAX(SUBSTRING_INDEX(t.TABLE_COLLATION,'_',1)), ';') AS create_table_statement FROM information_schema.TABLES t JOIN information_schema.COLUMNS c ON t.TABLE_SCHEMA = c.TABLE_SCHEMA AND t.TABLE_NAME = c.TABLE_NAME LEFT JOIN (SELECT kcu.TABLE_SCHEMA, kcu.TABLE_NAME, GROUP_CONCAT(CONCAT('`', kcu.COLUMN_NAME, '`') ORDER BY kcu.ORDINAL_POSITION) AS primary_key FROM information_schema.TABLE_CONSTRAINTS tc JOIN information_schema.KEY_COLUMN_USAGE kcu ON tc.CONSTRAINT_NAME = kcu.CONSTRAINT_NAME AND tc.TABLE_SCHEMA = kcu.TABLE_SCHEMA AND tc.TABLE_NAME = kcu.TABLE_NAME WHERE tc.CONSTRAINT_TYPE='PRIMARY KEY' GROUP BY kcu.TABLE_SCHEMA, kcu.TABLE_NAME) pk ON t.TABLE_SCHEMA = pk.TABLE_SCHEMA AND t.TABLE_NAME = pk.TABLE_NAME LEFT JOIN (SELECT uk_sub.TABLE_SCHEMA, uk_sub.TABLE_NAME, GROUP_CONCAT(CONCAT('UNIQUE KEY `', uk_sub.CONSTRAINT_NAME, '` (', uk_sub.column_list, ')') SEPARATOR ', ') AS unique_keys FROM (SELECT kcu.TABLE_SCHEMA, kcu.TABLE_NAME, kcu.CONSTRAINT_NAME, GROUP_CONCAT(CONCAT('`', kcu.COLUMN_NAME, '`') ORDER BY kcu.ORDINAL_POSITION) AS column_list FROM information_schema.TABLE_CONSTRAINTS tc JOIN information_schema.KEY_COLUMN_USAGE kcu ON tc.CONSTRAINT_NAME = kcu.CONSTRAINT_NAME AND tc.TABLE_SCHEMA = kcu.TABLE_SCHEMA AND tc.TABLE_NAME = kcu.TABLE_NAME WHERE tc.CONSTRAINT_TYPE='UNIQUE' GROUP BY kcu.TABLE_SCHEMA, kcu.TABLE_NAME, kcu.CONSTRAINT_NAME) AS uk_sub GROUP BY uk_sub.TABLE_SCHEMA, uk_sub.TABLE_NAME) uk ON t.TABLE_SCHEMA = uk.TABLE_SCHEMA AND t.TABLE_NAME = uk.TABLE_NAME LEFT JOIN (SELECT fk_sub.TABLE_SCHEMA, fk_sub.TABLE_NAME, GROUP_CONCAT(CONCAT('CONSTRAINT `', fk_sub.CONSTRAINT_NAME, '` FOREIGN KEY (`', fk_sub.COLUMN_NAME, '`) REFERENCES `', fk_sub.REFERENCED_TABLE_NAME, '`(`', fk_sub.REFERENCED_COLUMN_NAME, '`)') SEPARATOR ', ') AS foreign_keys FROM information_schema.KEY_COLUMN_USAGE fk_sub JOIN information_schema.TABLE_CONSTRAINTS tc ON fk_sub.CONSTRAINT_NAME = tc.CONSTRAINT_NAME AND fk_sub.TABLE_SCHEMA = tc.TABLE_SCHEMA AND fk_sub.TABLE_NAME = tc.TABLE_NAME WHERE tc.CONSTRAINT_TYPE='FOREIGN KEY' GROUP BY fk_sub.TABLE_SCHEMA, fk_sub.TABLE_NAME) fk ON t.TABLE_SCHEMA = fk.TABLE_SCHEMA AND t.TABLE_NAME = fk.TABLE_NAME WHERE 1=1 ";
		this.executeQuery(
			sql + 
			" AND t.TABLE_SCHEMA = '" + database + "' " +
			" AND t.TABLE_NAME = '" + name + "' " +
			"""
			GROUP BY
				t.TABLE_SCHEMA,
				t.TABLE_NAME
			"""
			,
			action);	
		}
	
	@Override
	public void getViewSchema(String name, ResultSetAction action) throws SQLException {
		this.executeQuery(
			"""
			SELECT CONCAT(
			    'CREATE ',
			    CASE WHEN IS_UPDATABLE = 'NO' THEN 'ALGORITHM=UNDEFINED ' ELSE '' END,
			    'DEFINER=`', DEFINER, '` ',
			    'SQL SECURITY ', SECURITY_TYPE, ' ',
			    'VIEW `', TABLE_SCHEMA, '`.`', TABLE_NAME, '` AS ',
			    VIEW_DEFINITION,
			    CASE WHEN CHECK_OPTION != 'NONE' THEN CONCAT(' WITH ', CHECK_OPTION, ' CHECK OPTION') ELSE '' END,
			    ';'
			) AS create_view_statement
			FROM information_schema.VIEWS
			WHERE 1=1
			  AND TABLE_SCHEMA = ?
			  AND TABLE_NAME = ?
			""",
			Arrays.asList(database, name), 
			action);
	}
	
	@Override
	public void getIndexSchema(String name, ResultSetAction action) throws SQLException {
		this.executeQuery(
				"""
				SELECT DISTINCT
				  CONCAT(
				    'CREATE ',
				    CASE WHEN s.NON_UNIQUE = 0 AND s.INDEX_NAME <> 'PRIMARY' THEN 'UNIQUE ' ELSE '' END,
				    CASE WHEN s.INDEX_NAME = 'PRIMARY' THEN 'PRIMARY KEY ' ELSE CONCAT('INDEX `', s.INDEX_NAME, '` ') END,
				    '(',
				    GROUP_CONCAT(CONCAT('`', s.COLUMN_NAME, '`') ORDER BY s.SEQ_IN_INDEX SEPARATOR ', '),
				    ') USING ', s.INDEX_TYPE, ';'
				  ) AS create_index_statement
				FROM information_schema.STATISTICS s
				WHERE s.TABLE_SCHEMA = ?
				  AND s.TABLE_NAME = ?
				GROUP BY s.TABLE_NAME, s.INDEX_NAME, s.INDEX_TYPE, s.NON_UNIQUE
				""",
				Arrays.asList(database, name), 
				action);
		throw new RuntimeException("No implemented");

	}
	
	@Override
	public String findPrimaryKey(String tableName) throws SQLException {
		StringBuilder primaryKeyBuilder = new StringBuilder();

		String query = "SELECT TABLE_NAME,COLUMN_NAME,CONSTRAINT_NAME, REFERENCED_TABLE_NAME,REFERENCED_COLUMN_NAME "
				+ "	FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND CONSTRAINT_NAME = ?";
		this.executeQuery(query, Arrays.asList(this.database, tableName, "PRIMARY"), rset -> {
			primaryKeyBuilder.append(rset.getString("COLUMN_NAME"));
			primaryKeyBuilder.append(",");
		});
		
		String primaryKey = primaryKeyBuilder.toString();
		if (!primaryKey.isEmpty()) {
			primaryKey = primaryKey.substring(0, primaryKey.length() - 1);
		}

		return primaryKey.isEmpty() ? null : primaryKey;
	}
	
	@Override
	public List<Map<String, String>> findForeignKeyReferences(String tableName) throws SQLException {
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
		this.executeQuery("select last_insert_id()", rset -> lastId.set(rset.getInt(1)));
		return lastId.get();
	}
}
