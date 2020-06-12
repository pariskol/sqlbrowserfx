package gr.sqlbrowserfx;

import java.util.ArrayList;
import java.util.List;

import gr.sqlbrowserfx.conn.SqlConnector;
import gr.sqlbrowserfx.conn.SqliteConnector;
import gr.sqlbrowserfx.dock.nodes.DSqlPane;
import gr.sqlbrowserfx.nodes.sqlpane.SqlPane;

public class SqlBrowserFXAppManager {

	private static SqlConnector sqlConnector = new SqliteConnector("./sqlbrowser.db");
	private static List<DSqlPane> sqlPanesList = new ArrayList<>();
	
	public static SqlConnector getConfigSqlConnector() {
		return sqlConnector;
	}
	
	public static void addSqlPane(DSqlPane sqlPane) {
		sqlPanesList.add(sqlPane);
	}
	
	public static List<DSqlPane> getActiveSqlPanes() {
		return sqlPanesList;
	}
	
	public static void removeSqlPane(SqlPane sqlPane) {
		sqlPanesList.remove(sqlPane);
	}
}
