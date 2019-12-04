package gr.sqlbrowserfx.conn;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;

public class MysqlConnector extends SqlConnector {

	private final String SCHEMA_VIEW_QUERY = "SHOW CREATE VIEW employees.";
	private final String SCHEMA_TABLE_QUERY = "SHOW CREATE TABLE employees.";

	private String database;

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
		dbcp2DataSource.setInitialSize(2);
		dbcp2DataSource.setMaxTotal(5);
		return dbcp2DataSource;
	}

	/**
	 * MySql has type checking return value as is.
	 */
	@Override
	public Object castToDBType(SqlTable sqlTable, String columnName, String text) {
		return text;
	}

	@Override
	public String getContentsQuery() {
		return "show full tables in " + database;
	}
	
	@Override
	public void getSchemas(String name, ResultSetAction action) throws SQLException {
		try {
			this.executeQuery(SCHEMA_TABLE_QUERY + name, action);
		} catch (SQLException e) {
			this.executeQuery(SCHEMA_VIEW_QUERY + name, action);
		}
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
	public String getIndexColumnName() {
		return null;
	}
	
	@Override
	public String getName() {
		return "TABLE_NAME";
	}
	
	@Override
	public String getType() {
		return "TABLE_TYPE";
	}
}
