package gr.sqlbrowserfx.nodes;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.controlsfx.control.PopOver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gr.sqlbrowserfx.conn.SqlConnector;
import gr.sqlbrowserfx.conn.SqlTable;
import gr.sqlbrowserfx.conn.SqliteConnector;
import gr.sqlbrowserfx.factories.DialogFactory;
import gr.sqlbrowserfx.listeners.SimpleChangeListener;
import gr.sqlbrowserfx.listeners.SimpleObservable;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import gr.sqlbrowserfx.utils.mapper.DTOMapper;
import javafx.collections.ObservableList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;

public class DBTreeView extends TreeView<String> implements SimpleChangeListener<String>, SimpleObservable<String> {

	private Logger logger = LoggerFactory.getLogger("SQLBROWSER");
	private SqlConnector sqlConnector;

	TreeItem<String> rootItem;
	TreeItem<String> tablesRootItem;
	TreeItem<String> viewsRootItem;
	TreeItem<String> indicesRootItem;
	private List<String> allItems;
	private List<SimpleChangeListener<String>> listeners;
	
	TextField searchField;

	@SuppressWarnings("unchecked")
	public DBTreeView(String dbPath, SqlConnector sqlConnector) {
		super();
		this.sqlConnector = sqlConnector;
		this.allItems = new ArrayList<>();
		this.listeners = new ArrayList<>();

		rootItem = new TreeItem<>(dbPath, JavaFXUtils.icon("/res/database.png"));
		rootItem.setExpanded(true);

		tablesRootItem = new TreeItem<>("Tables", JavaFXUtils.icon("/res/table.png"));
		tablesRootItem.setExpanded(true);
		viewsRootItem = new TreeItem<>("Views", JavaFXUtils.icon("/res/view.png"));
		viewsRootItem.setExpanded(true);
		indicesRootItem = new TreeItem<>("Indices", JavaFXUtils.icon("/res/index.png"));
		indicesRootItem.setExpanded(true);
		rootItem.getChildren().addAll(tablesRootItem, viewsRootItem, indicesRootItem);

		try {
			this.fillTreeView();
		} catch (SQLException e) {
			DialogFactory.createErrorDialog(e);
		}

		this.createContextMenu();
		this.setRoot(rootItem);
		this.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		
		searchField = new TextField();
		searchField.setPromptText("Search...");
		searchField.setOnKeyPressed(keyEvent -> {
			if (keyEvent.getCode() == KeyCode.ENTER) {
				this.searchFieldAction();
			}
		});
		
		this.setOnKeyPressed(keyEvent -> {
			if (keyEvent.isControlDown()) {
				switch (keyEvent.getCode()) {
				case C:
					this.copyAction();
					break;
				case F:
					this.showSearch();
					break;
				default:
					break;
				}
			}
		});
	}

	private void showSearch() {
		PopOver popOver = new PopOver(searchField);
		popOver.setArrowSize(0);
		popOver.show(rootItem.getGraphic());
	}

	private void clearAll() {
		tablesRootItem.getChildren().clear();
		viewsRootItem.getChildren().clear();
		indicesRootItem.getChildren().clear();
		allItems.clear();
	}
	
	private void fillTreeView() throws SQLException {
		List<String> newItems = new ArrayList<>();
		sqlConnector.getContents(rset -> {
			try {
				HashMap<String, Object> dto = DTOMapper.map(rset);

				String name = (String) dto.get(sqlConnector.getName());
				String type = (String) dto.get(sqlConnector.getType());

				newItems.add(name);
				if (!allItems.contains(name)) {
					allItems.add(name);
					TreeItem<String> treeItem = new TreeItem<String>(name);
					if (type.contains("table") || type.contains("TABLE")) {
						this.fillTableTreeItem(treeItem);
						tablesRootItem.getChildren().add(treeItem);
						treeItem.setGraphic(JavaFXUtils.icon("/res/table.png"));
					} else if (type.contains("view") || type.contains("VIEW")) {
						this.fillViewTreeItem(treeItem);
						viewsRootItem.getChildren().add(treeItem);
						treeItem.setGraphic(JavaFXUtils.icon("/res/view.png"));
					} else if (type.contains("index") || type.contains("INDEX")) {
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
			if (!newItems.contains(treeItem.getValue())) {
				found.add(treeItem);
				sfound.add(treeItem.getValue());
			}
		});
		tablesRootItem.getChildren().removeAll(found);
		allItems.removeAll(sfound);
		
		viewsRootItem.getChildren().forEach(treeItem -> {
			if (!newItems.contains(treeItem.getValue())) {
				found.add(treeItem);
				sfound.add(treeItem.getValue());
			}
		});
		viewsRootItem.getChildren().removeAll(found);
		allItems.removeAll(sfound);
		
		indicesRootItem.getChildren().forEach(treeItem -> {
			if (!newItems.contains(treeItem.getValue())) {
				found.add(treeItem);
				sfound.add(treeItem.getValue());
			}
		});
		indicesRootItem.getChildren().removeAll(found);
		allItems.removeAll(sfound);

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

		//TODO maybe executeAsync?
		sqlConnector.executeQueryRaw("select * from " + treeItem.getValue() + " limit 1", rset -> {
			SqlTable sqlTable = new SqlTable(rset.getMetaData());
			sqlTable.setPrimaryKey(sqlConnector.findPrimaryKey(treeItem.getValue()));
			sqlTable.setForeignKeys(sqlConnector.findForeignKeys(treeItem.getValue()));
			sqlTable.getColumns();
			for (String column : sqlTable.getColumns()) {
				TreeItem<String> columnTreeItem = new TreeItem<String>(column);
				if (column.equals(sqlTable.getPrimaryKey()))
					columnTreeItem.setGraphic(JavaFXUtils.icon("/res/primary-key.png"));
				else if (sqlTable.isForeignKey(column)) {
					columnTreeItem.setGraphic(JavaFXUtils.icon("/res/foreign-key.png"));
					TreeItem<String> referenceItem = new TreeItem<>(
							sqlConnector.findFoireignKeyReference(treeItem.getValue(), column));
					columnTreeItem.getChildren().add(referenceItem);

				}
				columnsTree.getChildren().add(columnTreeItem);
			}
		});
	}

	//TODO implement in a more abstract way
	private void updateTriggers() throws SQLException {
		if (sqlConnector instanceof SqliteConnector) {
			for (TreeItem<String> treeItem : tablesRootItem.getChildren()) {
				sqlConnector.executeQueryRaw("select * from sqlite_master where type like 'trigger' and tbl_name like '" +treeItem.getValue()+"'", rset -> {
					treeItem.getChildren().get(2).getChildren().clear();
					while (rset.next()) {
						TreeItem<String> triggerTreeItem = new TreeItem<String>(rset.getString("NAME"), JavaFXUtils.icon("/res/trigger.png"));
						String schema = rset.getString("SQL");
						triggerTreeItem.getChildren().add(new TreeItem<String>(schema, JavaFXUtils.icon("/res/script.png")));
						ObservableList<TreeItem<String>> triggerItems = treeItem.getChildren().get(2).getChildren();
						triggerItems.add(triggerTreeItem);
					}
					
				});
			}
		}
	}
	
	private void fillTableTreeItem(TreeItem<String> treeItem) throws SQLException {
		this.fillTVTreeItem(treeItem, sqlConnector.getTableSchemaColumn());
			TreeItem<String> triggersTreeItem = new TreeItem<String>("triggers", JavaFXUtils.icon("/res/trigger.png"));
			if (sqlConnector instanceof SqliteConnector) {
				sqlConnector.executeQuery("select * from sqlite_master where type like 'trigger' and tbl_name like '" +treeItem.getValue()+"'", rset -> {
					TreeItem<String> triggerTreeItem = new TreeItem<String>(rset.getString("NAME"), JavaFXUtils.icon("/res/trigger.png"));
					String schema = rset.getString("SQL");
					triggerTreeItem.getChildren().add(new TreeItem<String>(schema, JavaFXUtils.icon("/res/script.png")));
					triggersTreeItem.getChildren().add(triggerTreeItem);
				});
				
				treeItem.getChildren().add(triggersTreeItem);
			}
	}

	private void fillViewTreeItem(TreeItem<String> treeItem) throws SQLException {
		this.fillTVTreeItem(treeItem, sqlConnector.getViewSchemaColumn());
	}

	private void fillIndexTreeItem(TreeItem<String> treeItem) throws SQLException {
		TreeItem<String> schemaTree = new TreeItem<>("schema", JavaFXUtils.icon("/res/script.png"));
		treeItem.getChildren().add(schemaTree);

		sqlConnector.getSchemas(treeItem.getValue(), rset -> {
			String schema = rset.getString(sqlConnector.getIndexColumnName());
			schemaTree.getChildren().add(new TreeItem<String>(schema));
		});
	}

	protected ContextMenu createContextMenu() {
		ContextMenu contextMenu = new ContextMenu();

		MenuItem menuItemCopy = new MenuItem("Copy text", JavaFXUtils.icon("/res/copy.png"));
		menuItemCopy.setOnAction(event -> this.copyAction());

		MenuItem menuItemDrop = new MenuItem("Drop", JavaFXUtils.icon("/res/minus.png"));
		menuItemDrop.setOnAction(event -> {
			if (tablesRootItem.getChildren().contains(this.getSelectionModel().getSelectedItem())) {
				String table = this.getSelectionModel().getSelectedItem().getValue();
				String message = "Do you want to delete " + table;
				int result = DialogFactory.createConfirmationDialog("Drop Table", message);
				if (result == 1) {
					try {
						//TODO maybe execute async?
						sqlConnector.dropTable(table);
						this.fillTreeView();
					} catch (SQLException e) {
						DialogFactory.createErrorDialog(e);
					}
				}
			}
			else if (viewsRootItem.getChildren().contains(this.getSelectionModel().getSelectedItem())) {
				String view = this.getSelectionModel().getSelectedItem().getValue();
				String message = "Do you want to delete " + view;
				int result = DialogFactory.createConfirmationDialog("Drop View", message);
				if (result == 1) {
					try {
						//TODO maybe execute async?
						sqlConnector.dropView(view);
						this.fillTreeView();
					} catch (SQLException e) {
						DialogFactory.createErrorDialog(e);
					}
				}
			}
			else if (indicesRootItem.getChildren().contains(this.getSelectionModel().getSelectedItem())) {
				String index = this.getSelectionModel().getSelectedItem().getValue();
				String message = "Do you want to delete " + index;
				int result = DialogFactory.createConfirmationDialog("Drop Index", message);
				if (result == 1) {
					try {
						//TODO maybe execute async?
						sqlConnector.dropIndex(index);
						this.fillTreeView();
					} catch (SQLException e) {
						DialogFactory.createErrorDialog(e);
					}
				}
			}
		});

		MenuItem menuItemSearch = new MenuItem("Search...", JavaFXUtils.icon("/res/magnify.png"));
		menuItemSearch.setOnAction(event -> this.showSearch());
		
		MenuItem menuItemRefresh = new MenuItem("Refresh View", JavaFXUtils.icon("/res/refresh.png"));
		menuItemRefresh.setOnAction(event -> {
			try {
				this.clearAll();
				this.fillTreeView();
			} catch (SQLException e) {
				DialogFactory.createErrorDialog(e);
			} 
		});
		contextMenu.getItems().addAll(menuItemCopy, menuItemDrop, menuItemSearch, menuItemRefresh);
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

	private void searchFieldAction() {
		sqlConnector.executeAsync(() -> {
			this.getSelectionModel().clearSelection();
			for (TreeItem<String> t : tablesRootItem.getChildren()) {
				if(t.getValue().matches("(?i:.*" + searchField.getText() + ".*)")) {
					this.getSelectionModel().select(t);
				}
			}
		});
	}

	public List<String> getContentNames() {
		return allItems;
	}

	@Override
	public void onChange(String newValue) {
		try {
			this.fillTreeView();
			if (newValue.contains("trigger") || newValue.contains("trigger".toUpperCase()))
				this.updateTriggers();
		} catch (SQLException e) {
			DialogFactory.createErrorDialog(e);
		}
	}

	@Override
	public void changed() {
		listeners.forEach(listener -> listener.onChange(null));
	}

	@Override
	public void changed(String data) {
		// TODO Auto-generated method stub
		
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
