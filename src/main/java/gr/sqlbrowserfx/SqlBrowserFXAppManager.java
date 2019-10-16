package gr.sqlbrowserfx;

import java.util.ArrayList;
import java.util.List;

import gr.sqlbrowserfx.conn.SqlConnector;
import gr.sqlbrowserfx.conn.SqliteConnector;
import gr.sqlbrowserfx.nodes.sqlPane.SqlPane;

public class SqlBrowserFXAppManager {

	private static SqlConnector sqlConnector = new SqliteConnector("./sqlbrowser.db");
	private static List<SqlPane> sqlPanesList = new ArrayList<>();
	
	public static SqlConnector getConfigSqlConnector() {
		return sqlConnector;
	}
	
	public static void addSqlPane(SqlPane sqlPane) {
		sqlPanesList.add(sqlPane);
	}
	
	public static List<SqlPane> getActiveSqlPanes() {
		return sqlPanesList;
	}

	public static void removeSqlPane(SqlPane sqlPane) {
		sqlPanesList.remove(sqlPane);
	}
}
