package gr.paris.dock.nodes;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gr.sqlfx.conn.SqlConnector;
import gr.sqlfx.conn.SqlTable;
import gr.sqlfx.factories.DialogFactory;
import gr.sqlfx.listeners.SimpleChangeListener;
import gr.sqlfx.listeners.SimpleObservable;
import gr.sqlfx.utils.DTOMapper;
import gr.sqlfx.utils.JavaFXUtils;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

public class DBTreeView extends TreeView<String> implements SimpleChangeListener<String>, SimpleObservable<String>{
	
	private Logger logger = LoggerFactory.getLogger("SPARK");
	private SqlConnector sqlConnector;
	
	TreeItem<String> rootItem;	
	TreeItem<String> tablesRootItem;
	TreeItem<String> viewsRootItem;
	TreeItem<String> indicesRootItem;
	private List<String> allNames;
	private List<SimpleChangeListener<String>>  listeners;

	@SuppressWarnings("unchecked")
	public DBTreeView(String dbPath, SqlConnector sqlConnector) {
		this.sqlConnector = sqlConnector;
		this.allNames = new ArrayList<>();
		this.listeners = new ArrayList<>();
		
		rootItem = new TreeItem<>(dbPath, JavaFXUtils.icon("/res/database.png"));
		rootItem.setExpanded(true);

		tablesRootItem = new TreeItem<>("Tables", JavaFXUtils.icon("/res/table.png"));
		viewsRootItem = new TreeItem<>("Views", JavaFXUtils.icon("/res/view.png"));
		indicesRootItem = new TreeItem<>("Indices", JavaFXUtils.icon("/res/index.png"));
		rootItem.getChildren().addAll(tablesRootItem, viewsRootItem, indicesRootItem);

		try {
			this.fillTreeView();
//			this.setTreeItems();
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
		}

		if (indicesRootItem.getChildren().size() == 0) {
			rootItem.getChildren().remove(indicesRootItem);
		}
		
		this.createContextMenu();
		this.setRoot(rootItem);
		this.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
	}

	private void fillTreeView() throws SQLException {
		List<String> newNames = new ArrayList<>();
		sqlConnector.getContents(rset -> {
			try {
				HashMap<String, Object> dto = DTOMapper.map(rset);
				
				String name = (String) dto.get(sqlConnector.NAME);
				String type = (String) dto.get(sqlConnector.TYPE);
				
				newNames.add(name);
				if (!allNames.contains(name)) {
					allNames.add(name);
					TreeItem<String> treeItem = new TreeItem<String>(name);
					if (type.contains("table") || type.contains("TABLE")) {
						this.fillTableTreeItem(treeItem);
						tablesRootItem.getChildren().add(treeItem);
						tablesRootItem.getChildren().remove(null);
						treeItem.setGraphic(JavaFXUtils.icon("/res/table.png"));
					}
					else if (type.contains("view") || type.contains("VIEW")) {
						this.fillViewTreeItem(treeItem);
						viewsRootItem.getChildren().add(treeItem);
						treeItem.setGraphic(JavaFXUtils.icon("/res/view.png"));
					}
					else if (type.contains("index") || type.contains("INDEX")) {
						this.fillIndexTreeItem(treeItem);
						indicesRootItem.getChildren().add(treeItem);
						treeItem.setGraphic(JavaFXUtils.icon("/res/index.png"));
					}
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		});
		
		List<TreeItem<String>> found = new ArrayList<>();
		List<String> sfound = new ArrayList<>();
		
		tablesRootItem.getChildren().forEach(treeItem -> {
			if (!newNames.contains(treeItem.getValue())) {
				found.add(treeItem);
			}
		});
		tablesRootItem.getChildren().removeAll(found);
		allNames.removeAll(sfound);
		
		this.changed();
	}
	
	private void fillTVTreeItem(TreeItem<String> treeItem, String schemaColumn) throws SQLException {
		TreeItem<String> schemaTree = new TreeItem<>("schema", JavaFXUtils.icon("/res/script.png"));
		treeItem.getChildren().add(schemaTree);
		
		sqlConnector.getSchemas(treeItem.getValue(), rset -> {
			// TODO handle differnet queries of mysql
			String schema = rset.getString(schemaColumn);
			schemaTree.getChildren().add(new TreeItem<String>(schema));
		});
		
		TreeItem<String> columnsTree = new TreeItem<>("columns", JavaFXUtils.icon("/res/columns.png"));
		treeItem.getChildren().add(columnsTree);

		sqlConnector.executeQueryRaw("select * from " + treeItem.getValue() + " limit 1", rset -> {
			SqlTable sqlTable = new SqlTable(rset.getMetaData());
			sqlTable.setPrimaryKey(sqlConnector.findPrimaryKey(treeItem.getValue()));
			sqlTable.setForeignKeys(sqlConnector.findForeignKeys(treeItem.getValue()));
			sqlTable.getColumns();
			for(String column : sqlTable.getColumns()) {
				TreeItem<String> columnTreeItem = new TreeItem<String>(column);
				if (column.equals(sqlTable.getPrimaryKey()))
					columnTreeItem.setGraphic(JavaFXUtils.icon("/res/primary-key.png"));
				else if (sqlTable.isForeignKey(column))
					columnTreeItem.setGraphic(JavaFXUtils.icon("/res/foreign-key.png"));
				columnsTree.getChildren().add(columnTreeItem);
			}
		});
	}
	
	private void fillTableTreeItem(TreeItem<String> treeItem) throws SQLException {
		this.fillTVTreeItem(treeItem, sqlConnector.tableSchemaColumn());
	}
	
	private void fillViewTreeItem(TreeItem<String> treeItem) throws SQLException {
		this.fillTVTreeItem(treeItem, sqlConnector.viewSchemaColumn());
	}
	
	private void fillIndexTreeItem(TreeItem<String> treeItem) throws SQLException {
		TreeItem<String> schemaTree = new TreeItem<>("schema", JavaFXUtils.icon("/res/script.png"));
		treeItem.getChildren().add(schemaTree);
		
		sqlConnector.getSchemas(treeItem.getValue(), rset -> {
			String schema = rset.getString(sqlConnector.indexColumnName());
			schemaTree.getChildren().add(new TreeItem<String>(schema));
		});
	}
	
	private ContextMenu createContextMenu() {
		ContextMenu contextMenu = new ContextMenu();

		MenuItem menuItemCopy = new MenuItem("Copy", JavaFXUtils.icon("/res/copy.png"));
		menuItemCopy.setOnAction(event -> this.copyAction());

		MenuItem menuItemDrop = new MenuItem("Drop table", JavaFXUtils.icon("/res/minus.png"));
		menuItemDrop.setOnAction(event -> {
			String table = this.getSelectionModel().getSelectedItem().getValue();
			String message = "Do you want to delete " + table;
			int result = DialogFactory.createConfirmationDialog("Drop Table", message);
			if (result == 1) {
				try {
					sqlConnector.dropTable(table);
					this.fillTreeView();
				} catch (SQLException e) {
					DialogFactory.createErrorDialog(e);
				}
			}
		});

		contextMenu.getItems().addAll(menuItemCopy, menuItemDrop);
		this.setContextMenu(contextMenu);

		return contextMenu;
	}
	
	private void copyAction() {
		String text = "";
		for (TreeItem<String> treeItem : this.getSelectionModel().getSelectedItems()) {
			text += treeItem.getValue() + ", ";
		}
		text = text.substring(0, text.length() - ", ".length());
		
		StringSelection stringSelection = new StringSelection(text);
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(stringSelection, null);
	}
	
	public List<String> getContentNames() {
		return allNames;
	}

	@Override
	public void onChange(String newValue) {
		try {
			this.fillTreeView();
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	public void changed() {
		listeners.forEach(listener -> listener.onChange(null));
	}

	@Override
	public void addListener(SimpleChangeListener<String> listener) {
		listeners.add(listener);
	}

	@Override
	public void removeListener(SimpleChangeListener<String> listener) {
		listeners.remove(listener);
	}
}
