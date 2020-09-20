package gr.sqlbrowserfx;

import java.util.ArrayList;
import java.util.List;

import gr.sqlbrowserfx.conn.SqlConnector;
import gr.sqlbrowserfx.conn.SqliteConnector;
import gr.sqlbrowserfx.dock.nodes.DSqlPane;
import gr.sqlbrowserfx.nodes.sqlpane.SqlPane;

public class SqlBrowserFXAppManager {

	private static SqlConnector SQL_CONNECTOR = new SqliteConnector("./sqlbrowser.db");
	private static List<DSqlPane> SQL_PANE_LIST = new ArrayList<>();
	private static String DB_TYPE = "sqlite";
	
	public static SqlConnector getConfigSqlConnector() {
		return SQL_CONNECTOR;
	}
	
	public static void addSqlPane(DSqlPane sqlPane) {
		SQL_PANE_LIST.add(sqlPane);
	}
	
	public static List<DSqlPane> getActiveSqlPanes() {
		return SQL_PANE_LIST;
	}
	
	public static void removeSqlPane(SqlPane sqlPane) {
		SQL_PANE_LIST.remove(sqlPane);
	}

	public static String getDBtype() {
		return DB_TYPE;
	}
	
	public static void setDBtype(String type) {
		DB_TYPE = type;
	}
	
}
