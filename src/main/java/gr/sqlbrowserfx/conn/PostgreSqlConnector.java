package gr.sqlbrowserfx.conn;

import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.LoggerFactory;

import gr.sqlbrowserfx.LoggerConf;

public class PostgreSqlConnector extends SqlConnector {
	
	public PostgreSqlConnector(String url, String database, String user, String password) {
		super("org.postgresql.Driver", url, user, password);

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

	@Override
	public Object castToDBType(SqlTable table, String label, String value) {
		Object actualValue;
		if (table.getColumnsMap().get(label).contains("int") && value != null && !value.isEmpty()) {
            actualValue = Integer.parseInt(value);
		} else if (table.getColumnsMap().get(label).equals("numeric")  && value != null && !value.isEmpty()) {
            actualValue = Double.parseDouble(value);
		} 
		else if (table.getColumnsMap().get(label).contains("bool") && value != null && !value.isEmpty()) {
            actualValue = Boolean.parseBoolean(value);
		}
		else if (table.getColumnsMap().get(label).contains("date") && value != null && !value.isEmpty()) {
            actualValue = Date.valueOf(value);
		}
		else if (table.getColumnsMap().get(label).contains("stamp") && value != null && !value.isEmpty()) {
            actualValue = Timestamp.valueOf(value);
		}
		else {
			actualValue = value;
		}
		return actualValue;
	}
	

	@Override
	public String getContentsQuery() {
		return 
		"""
		select tb.table_name, tb.table_type from INFORMATION_SCHEMA.tables as tb WHERE tb.table_schema = ANY (current_schemas(false)) 
		union 
		select indexname as table_name , 'INDEX'as table_type  from pg_indexes where schemaname = 'public'
		""";
	}

	@Override
	public String getDbSchema() {
		return "public";
	}
	
	@Override
	public void getTriggers(String table, ResultSetAction action) throws SQLException {
		String query =
			"""
			select 
			   event_object_schema as table_schema, 
		       event_object_table as table_name, 
		       trigger_schema, 
		       trigger_name as TRIGGER_NAME, 
		       string_agg(event_manipulation, ',') as event, 
		       action_timing as activation, 
		       action_condition as condition, 
		       action_statement as ACTION_STATEMENT 
			from information_schema.triggers 
			where event_object_table = ? 
			group by 1,2,3,4,6,7,8 
			order by table_schema, table_name
			""";
		this.executeQuery(query, Arrays.asList(table), action);

	}

	@Override
	public void getTableSchema(String name, ResultSetAction action) throws SQLException {
		this.getSchema(name, action);
	}
	
	@Override
	public void getViewSchema(String name, ResultSetAction action) throws SQLException {
		this.getSchema(name, action);
	}
	
	@Override
	public void getIndexSchema(String name, ResultSetAction action) throws SQLException {
		this.getSchema(name, action);
	}
	
	
	private void getSchema(String name, ResultSetAction action) throws SQLException {
		this.executeQuery(
				"""
				select table_name, view_definition as schema from INFORMATION_SCHEMA.views 
				where table_schema = ANY (current_schemas(false)) and table_name = ?
				""",
				Arrays.asList(name), action);
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

}
