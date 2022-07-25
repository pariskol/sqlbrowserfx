package gr.sqlbrowserfx.conn;

import java.util.HashMap;
import java.util.Map;

public class DbCash {
	private static Map<String, String> SCHEMAS_MAP = new HashMap<>();

	
	public static synchronized String getSchemaFor(String table) {
		return SCHEMAS_MAP.get(table);
	}
	
	public static synchronized void addSchemaFor(String table, String schema) {
		SCHEMAS_MAP.put(table, schema);
	}
}
