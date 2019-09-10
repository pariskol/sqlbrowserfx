package gr.sqlfx.conn;

import java.sql.SQLException;

import org.apache.commons.dbcp2.BasicDataSource;

public class MysqlConnector extends SqlConnector {

	private final String SCHEMA_VIEW_QUERY = "SHOW CREATE VIEW employees.";
	private final String SCHEMA_TABLE_QUERY = "SHOW CREATE TABLE employees.";

	private String user;
	private String password;

	public MysqlConnector(String database, String user, String password) {
		url = "jdbc:mysql://localhost:3306/" + database + "?autoReconnect=true&useSSL=true";// &useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";
		driver = "com.mysql.jdbc.Driver";
		this.user = user;
		this.password = password;

		NAME = "TABLE_NAME";
		TYPE = "TABLE_TYPE";
		
		initDatasource();
	}

	@Override
	protected void initDatasource() {
		BasicDataSource dbcp2DataSource = new BasicDataSource();
		dbcp2DataSource.setDriverClassName(driver);
		dbcp2DataSource.setUrl(url);
		dbcp2DataSource.setUsername(user);
		dbcp2DataSource.setPassword(password);
		dbcp2DataSource.setInitialSize(2);
		dbcp2DataSource.setMaxTotal(5);

		dataSource = dbcp2DataSource;
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
		return "show full tables in " + "employees";
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
}
