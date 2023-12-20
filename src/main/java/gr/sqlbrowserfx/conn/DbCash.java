package gr.sqlbrowserfx.conn;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DbCash {
	private static final Map<String, String> SCHEMAS_MAP = new HashMap<>();
	private static final Map<String, SqlTable> TABLES_MAP = new HashMap<>();


	
	public static synchronized String getSchemaFor(String table) {
		return SCHEMAS_MAP.get(table);
	}
	
	public static synchronized void addSchemaFor(String table, String schema) {
		SCHEMAS_MAP.put(table, schema);
	}
	
	public static synchronized void addTable(SqlTable table) {
		TABLES_MAP.put(table.getName(), table);
	}
	
	public static synchronized SqlTable getTable(String table) {
		return TABLES_MAP.get(table);
	}

	public static List<String> getAllTableNames() {
		return TABLES_MAP.values().stream().map(SqlTable::getName).toList();
	}
}
