package gr.sqlbrowserfx;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import gr.sqlbrowserfx.conn.SqlConnector;
import gr.sqlbrowserfx.conn.SqliteConnector;
import gr.sqlbrowserfx.dock.nodes.DDBTreeView;
import gr.sqlbrowserfx.dock.nodes.DSqlConsolePane;
import gr.sqlbrowserfx.dock.nodes.DSqlPane;
import gr.sqlbrowserfx.nodes.sqlpane.SqlPane;

public class SqlBrowserFXAppManager {

	private static final SqlConnector SQL_CONNECTOR = new SqliteConnector("./sqlbrowser.db");
	private static final List<DSqlPane> DSQL_PANES = new ArrayList<>();
	private static final List<SqlPane> SQL_PANES = new ArrayList<>();
	private static final List<DDBTreeView> DB_TREE_VIEWS = new ArrayList<>();
	private static String DB_TYPE = "sqlite";
	
	public static SqlConnector getConfigSqlConnector() {
		return SQL_CONNECTOR;
	}
	
	public static void registerDSqlPane(DSqlPane sqlPane) {
		DSQL_PANES.add(sqlPane);
		DB_TREE_VIEWS.forEach(DDBTreeView::populateSqlPanesMenu);
	}
	
	public static void registerSqlPane(SqlPane sqlPane) {
		SQL_PANES.add(sqlPane);
		DB_TREE_VIEWS.forEach(DDBTreeView::populateSqlPanesMenu);
	}
	
	public static List<SqlPane> getActiveSqlPanes() {
		List<SqlPane> sqlPanes = DSQL_PANES.stream().map(sp -> (SqlPane) sp).collect(Collectors.toList());
		sqlPanes.addAll(SQL_PANES);
		return sqlPanes.stream().filter(Objects::nonNull).collect(Collectors.toList());
	}
	
	public static long getActiveSqlCodeAreasNum() {
		return DSQL_PANES.stream().filter(x -> x.getSqlCodeAreaRef() != null).count();
	}
	
	public static DSqlConsolePane getFirstActiveDSqlConsolePane() {
		DSqlPane activeSqlPane = DSQL_PANES.stream()
				.filter(sqlPane -> sqlPane.getSqlConsolePane() != null)
				.findFirst()
				.orElse(null);
		
		if (activeSqlPane != null) {
			return activeSqlPane.getSqlConsolePane();
		}
		
		return null;
	}
	
	public static void unregisterDSqlPane(DSqlPane sqlPane) {
		DSQL_PANES.remove(sqlPane);
		DB_TREE_VIEWS.forEach(DDBTreeView::populateSqlPanesMenu);
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
