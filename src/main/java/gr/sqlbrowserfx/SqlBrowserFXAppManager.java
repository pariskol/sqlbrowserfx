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
import gr.sqlbrowserfx.nodes.FilesTabPane;
import gr.sqlbrowserfx.nodes.ollama.OllamaChatPane;
import gr.sqlbrowserfx.nodes.ollama.OllamaHandler;
import gr.sqlbrowserfx.nodes.sqlpane.SqlPane;
import javafx.beans.property.SimpleBooleanProperty;

public class SqlBrowserFXAppManager {

	public final static String INTERNAL_DB_PATH = "./sqlbrowser.db";

	private static final SqlConnector SQL_CONNECTOR = new SqliteConnector(INTERNAL_DB_PATH);
	private static final List<DSqlPane> DSQL_PANES = new ArrayList<>();
	private static final List<SqlPane> SQL_PANES = new ArrayList<>();
	private static final List<FilesTabPane> FILES_TAB_PANES = new ArrayList<>();
	private static final List<DDBTreeView> DB_TREE_VIEWS = new ArrayList<>();
	private static OllamaChatPane OLLAMA;
	private static String DB_TYPE = "sqlite";
	private static SimpleBooleanProperty aiAvailableProperty = new SimpleBooleanProperty(false); 
	
	public static SqlConnector getConfigSqlConnector() {
		return SQL_CONNECTOR;
	}
	
	public static void registerDSqlPane(DSqlPane sqlPane) {
		DSQL_PANES.add(sqlPane);
		DB_TREE_VIEWS.forEach(DDBTreeView::populateSqlPanesMenu);
	}
	
	public static void registerFilesTabPane(FilesTabPane tabPane) {
		FILES_TAB_PANES.add(tabPane);
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
		var activeSqlPane = DSQL_PANES.stream()
				.filter(sqlPane -> sqlPane.getSqlConsolePane() != null)
				.findFirst()
				.orElse(null);
		
		if (activeSqlPane != null) {
			return activeSqlPane.getSqlConsolePane();
		}
		
		return null;
	}
	
	public static FilesTabPane getFirstActiveFilesTabPane() {
		return FILES_TAB_PANES.stream()
				.findFirst()
				.orElse(null);
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

	public static void registerOllamaPane(OllamaChatPane chatPane) {
		OLLAMA = chatPane;
		SqlBrowserFXAppManager.aiAvailableProperty.set(true);
	}
	
	public static void unregisterOllamaPane() {
		OLLAMA = null;
		SqlBrowserFXAppManager.aiAvailableProperty.set(false);
	}

	public static OllamaChatPane getOllamaPane() {
		return OLLAMA;
	}

	public static SimpleBooleanProperty aiAvailableProperty() {
		return aiAvailableProperty;
	}
	
}