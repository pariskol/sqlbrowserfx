package gr.sqlbrowserfx.conn;

import java.sql.SQLException;
import java.util.Arrays;

import org.apache.commons.dbcp2.BasicDataSource;

public class SqliteConnector extends SqlConnector {

	private final String SCHEMA_COLUMN = "sql";
	private final String SCHEMA_QUERY = "select sql from sqlite_master where name = ?";
	
	public SqliteConnector(String database) {
		url = "jdbc:sqlite:" + database;
		driver = "org.sqlite.JDBC";
		
		NAME = "name";
		TYPE = "type";
		
		initDatasource();
	}

	@Override
	protected void initDatasource() {
		BasicDataSource dbcp2DataSource = new BasicDataSource();
		dbcp2DataSource.setDriverClassName(driver);
		dbcp2DataSource.setUrl(url);
		dbcp2DataSource.setInitialSize(2);
		dbcp2DataSource.setMaxTotal(5);
		
		dataSource = dbcp2DataSource;
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
