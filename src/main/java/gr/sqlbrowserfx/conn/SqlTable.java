package gr.sqlbrowserfx.conn;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SqlTable {

	String name;
	String primaryKey;
	List<String> foreignKeys;
	LinkedHashMap<String, String> columnsMap;

	public SqlTable(ResultSetMetaData rsmd) {

		foreignKeys = new ArrayList<>();
		try {
			name = rsmd.getTableName(1);
			columnsMap = new LinkedHashMap<>();
			for (int i = 1; i <= rsmd.getColumnCount(); i++) {
				columnsMap.put(rsmd.getColumnLabel(i), rsmd.getColumnTypeName(i));
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public SqlTable(String tableName, ResultSetMetaData rsmd) {

		foreignKeys = new ArrayList<>();
		try {
			name = tableName;
			columnsMap = new LinkedHashMap<>();
			for (int i = 1; i <= rsmd.getColumnCount(); i++) {
				if (rsmd.getTableName(i).equals(name))
					columnsMap.put(rsmd.getColumnLabel(i), rsmd.getColumnTypeName(i));
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
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
		String result = new String();
		for (String column : columnsMap.keySet()) {
			result += column + ",";
		}
		return result.substring(0, result.length() - 1);
	}

}
