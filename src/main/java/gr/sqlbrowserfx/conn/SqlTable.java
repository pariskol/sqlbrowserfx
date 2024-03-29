package gr.sqlbrowserfx.conn;

import gr.sqlbrowserfx.LoggerConf;
import org.slf4j.LoggerFactory;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SqlTable {

	private String name;
	private String primaryKey;
	private List<String> foreignKeys;
	private List<String> relatedTables;
	private LinkedHashMap<String, String> columnsMap;

	public SqlTable(ResultSetMetaData rsmd) {

		foreignKeys = new ArrayList<>();
		try {
			name = rsmd.getTableName(1);
			columnsMap = new LinkedHashMap<>();
			for (int i = 1; i <= rsmd.getColumnCount(); i++) {
				columnsMap.put(rsmd.getColumnLabel(i), rsmd.getColumnTypeName(i));
			}

		} catch (SQLException e) {
			LoggerFactory.getLogger(LoggerConf.LOGGER_NAME).error(e.getMessage(), e);
		}
	}

	public SqlTable(String tableName, ResultSetMetaData rsmd) {
		this(rsmd);
		name = tableName;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Map<String, String> getColumnsMap() {
		return columnsMap;
	}

	public void setColumnsMap(LinkedHashMap<String, String> columns) {
		this.columnsMap = columns;
	}

	/*
	 * Returns tables's primary key , IMPORTANT in case of a composite
	 * key it returns a comma separated string with the keys 
	 * 
	 */
	public String getPrimaryKey() {
		return primaryKey;
	}

	public void setPrimaryKey(String primaryKey) {
		this.primaryKey = primaryKey;
	}

	public Set<String> getColumns() {
		return columnsMap.keySet();
	}

	public List<String> getForeignKeys() {
		return foreignKeys;
	}

	public void setForeignKeys(List<String> foreignKeys) {
		this.foreignKeys = foreignKeys;
	}

	public void addForeignKey(String foreignKey) {
		foreignKeys.add(foreignKey);
	}

	public boolean isForeignKey(String key) {
		for (String foreignKey : foreignKeys) {
			if (key.equals(foreignKey)) {
				return true;
			}
		}
		return false;
	}
	
	public boolean isPrimaryKey(String key) {
		return (primaryKey != null && primaryKey.equals(key));
	}

	public String columnsToString() {
		StringBuilder result = new StringBuilder();
		for (String column : columnsMap.keySet()) {
			result.append(column).append(",");
		}
		return result.substring(0, result.length() - 1);
	}

	public List<String> getRelatedTables() {
		return this.relatedTables;
	}
	
	public void setRelatedTables(List<String> relatedTables) {
		this.relatedTables = relatedTables;
	}

}
