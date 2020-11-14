package gr.sqlbrowserfx;

import java.util.ArrayList;
import java.util.List;

import gr.sqlbrowserfx.conn.SqlConnector;
import gr.sqlbrowserfx.conn.SqliteConnector;
import gr.sqlbrowserfx.dock.nodes.DDBTreeView;
import gr.sqlbrowserfx.dock.nodes.DSqlPane;
import gr.sqlbrowserfx.nodes.sqlpane.SqlPane;

public class SqlBrowserFXAppManager {

	private static SqlConnector SQL_CONNECTOR = new SqliteConnector("./sqlbrowser.db");
	private static List<DSqlPane> SQL_PANES = new ArrayList<>();
	private static List<DDBTreeView> DB_TREE_VIEWS = new ArrayList<>();
	private static String DB_TYPE = "sqlite";
	
	public static SqlConnector getConfigSqlConnector() {
		return SQL_CONNECTOR;
	}
	
	public static void addSqlPane(DSqlPane sqlPane) {
		SQL_PANES.add(sqlPane);
		DB_TREE_VIEWS.forEach(tv -> tv.populateSqlPanesMenu());
	}
	
	public static List<DSqlPane> getActiveSqlPanes() {
		return SQL_PANES;
	}
	
	public static void removeSqlPane(SqlPane sqlPane) {
		SQL_PANES.remove(sqlPane);
		DB_TREE_VIEWS.forEach(tv -> tv.populateSqlPanesMenu());
	}

	public static String getDBtype() {
		return DB_TYPE;
	}
	
	public static void setDBtype(String type) {
		DB_TYPE = type;
	}
	
	public static void registerDDBTreeView(DDBTreeView treeView) {
		DB_TREE_VIEWS.add(treeView);
	}
	
	public static void unregisterDDBTreeView(DDBTreeView treeView) {
		DB_TREE_VIEWS.remove(treeView);
	}
}
