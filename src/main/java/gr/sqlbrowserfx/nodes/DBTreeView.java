package gr.sqlbrowserfx.nodes;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.textfield.CustomTextField;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gr.sqlbrowserfx.LoggerConf;
import gr.sqlbrowserfx.conn.DbCash;
import gr.sqlbrowserfx.conn.MysqlConnector;
import gr.sqlbrowserfx.conn.SqlConnector;
import gr.sqlbrowserfx.conn.SqlTable;
import gr.sqlbrowserfx.conn.SqliteConnector;
import gr.sqlbrowserfx.dock.nodes.DDBTreePane;
import gr.sqlbrowserfx.factories.DialogFactory;
import gr.sqlbrowserfx.listeners.SimpleEvent;
import gr.sqlbrowserfx.listeners.SimpleObservable;
import gr.sqlbrowserfx.listeners.SimpleObserver;
import gr.sqlbrowserfx.nodes.codeareas.sql.SqlCodeArea;
import gr.sqlbrowserfx.nodes.codeareas.sql.SqlCodeAreaSyntaxProvider;
import gr.sqlbrowserfx.nodes.sqlpane.CustomPopOver;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import gr.sqlbrowserfx.utils.mapper.DTOMapper;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;

public class DBTreeView extends TreeView<String>
		implements ContextMenuOwner, InputMapOwner, SimpleObserver<String>, SimpleObservable<String> {

	private final Logger logger = LoggerFactory.getLogger(LoggerConf.LOGGER_NAME);
	private final SqlConnector sqlConnector;

	private final TreeItem<String> rootItem;
	private TreeItem<String> selectedRootItem;
	
	protected TreeItem<String> tablesRootItem;
	protected TreeItem<String> viewsRootItem;
	private TreeItem<String> indexesRootItem;
	private TreeItem<String> proceduresRootItem;
	private TreeItem<String> functionsRootItem;

	private final List<String> allItems;
	private final List<SimpleObserver<String>> listeners;

	private Integer lastSelectedItemPos = 0;

	private final CustomTextField searchField;
	private DDBTreePane parent = null;
	private final List<TreeItem<String>> searchResultsList = new ArrayList<>();
	private final Button nextSearchResultButton;
	
	private final SimpleBooleanProperty hasSelectedSchemaProperty = new SimpleBooleanProperty(false);
	private final SimpleBooleanProperty canSelectedOpenProperty = new SimpleBooleanProperty(false);
	private HBox searchBox;
	private String currentSearchPattern;

	

	@SuppressWarnings("unchecked")
	public DBTreeView(String dbPath, SqlConnector sqlConnector) {
		super();
		this.sqlConnector = sqlConnector;
		this.allItems = new ArrayList<>();
		this.listeners = new ArrayList<>();

		this.setupSelectionChangeListener();

		rootItem = new TreeItem<>(dbPath, JavaFXUtils.createIcon("/icons/database.png"));
		rootItem.setExpanded(true);

		tablesRootItem = new TreeItem<>("Tables", JavaFXUtils.createIcon("/icons/table.png"));
		tablesRootItem.setExpanded(true);
		viewsRootItem = new TreeItem<>("Views", JavaFXUtils.createIcon("/icons/view.png"));
		viewsRootItem.setExpanded(true);
		indexesRootItem = new TreeItem<>("Indexes", JavaFXUtils.createIcon("/icons/index.png"));
		indexesRootItem.setExpanded(true);

		rootItem.getChildren().addAll(tablesRootItem, viewsRootItem, indexesRootItem);

		try {
			this.fillTreeView();
			// FIXME implement in a more abstract way
			if (!(sqlConnector instanceof SqliteConnector)) {
				proceduresRootItem = new TreeItem<>("Procedures", JavaFXUtils.createIcon("/icons/procedure.png"));
				functionsRootItem = new TreeItem<>("Functions", JavaFXUtils.createIcon("/icons/function.png"));
				rootItem.getChildren().addAll(proceduresRootItem, functionsRootItem);
				this.getFunctionsAndProcedures();
			}
		} catch (SQLException e) {
			DialogFactory.createErrorDialog(e);
		}

		this.setContextMenu(this.createContextMenu());
//		this.setRoot(rootItem);
		this.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

		searchField = new CustomTextField();
		searchField.setPromptText("Search...");
		searchField.setRight(JavaFXUtils.createIcon("/icons/magnify.png"));
		searchField.setOnKeyPressed(keyEvent -> {
			if (keyEvent.getCode() == KeyCode.ENTER) {
				this.searchFieldAction(true);
			}
		});
		nextSearchResultButton = new Button("", JavaFXUtils.createIcon("/icons/next.png"));
		nextSearchResultButton.setOnAction(event -> {
			if (!searchField.getText().equals(currentSearchPattern)) {
				this.searchFieldAction(false);
			}

			lastSelectedItemPos = lastSelectedItemPos == searchResultsList.size() - 1 ? 0 : ++lastSelectedItemPos;
			this.getSelectionModel().clearSelection();
			this.getSelectionModel().select(searchResultsList.get(lastSelectedItemPos));
			int row = this.getRow(searchResultsList.get(lastSelectedItemPos));
			this.scrollTo(row);
		});

		searchBox = new HBox(searchField, nextSearchResultButton);
		this.setInputMap();
	}

	private void setupSelectionChangeListener() {
		this.getSelectionModel().selectedItemProperty().addListener((ob, ov, nv) -> {
			if (nv == null) return;
			
			var selected = nv.getValue();
			canSelectedOpenProperty.set(false);
			hasSelectedSchemaProperty.set(false);
			
			var tables = tablesRootItem.getChildren().stream().map(TreeItem::getValue).toList();
			if (tables.contains(selected)) {
				hasSelectedSchemaProperty.set(true);
				canSelectedOpenProperty.set(true);
				return;
			}
			var views = viewsRootItem.getChildren().stream().map(TreeItem::getValue).toList();
			if (views.contains(selected)) {
				hasSelectedSchemaProperty.set(true);
				canSelectedOpenProperty.set(true);
				return;
			}
			var indexes = indexesRootItem.getChildren().stream().map(TreeItem::getValue).toList();
			if (indexes.contains(selected)) {
				hasSelectedSchemaProperty.set(true);
				return;
			}
			
			if (isUsingMysql()) {
				var procedures = proceduresRootItem.getChildren().stream().map(TreeItem::getValue)
						.toList();
				if (procedures.contains(selected)) {
					hasSelectedSchemaProperty.set(true);
					return;
				}
				var functions = functionsRootItem.getChildren().stream().map(TreeItem::getValue)
						.toList();
				if (functions.contains(selected)) {
					hasSelectedSchemaProperty.set(true);
                }
			}
			
		});
	}
	
	@Override
	public void setInputMap() {
		// enable following line if this view used as standalone
//		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.F, KeyCombination.CONTROL_DOWN),
//				action -> this.showSearchPopup()));
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.C, KeyCombination.CONTROL_DOWN),
				action -> this.copyAction()));
	}

	@SuppressWarnings("unused")
	@Deprecated
	private void setKeys() {
		this.setOnKeyPressed(keyEvent -> {
			if (keyEvent.isControlDown()) {
                switch (keyEvent.getCode()) {
                    case C -> this.copyAction();
                    case F -> this.showSearchPopup();
                    default -> {
                    }
                }
			}
		});
	}

	public DBTreeView(String dbPath, SqlConnector sqlConnector, DDBTreePane parent) {
		this(dbPath, sqlConnector);
		this.parent = parent;
	}

	/**
	 * This is function is needed because postgresql and mysql have same
	 * column names for some result sets but with different cases.
	 * It is being used in getFunctionsAndProcedures() method.
	 * 
	 * @param map
	 * @param col
	 * @return
	 */
	private String getULN(Map<String, Object> map, String col) {
		try {
			return map.get(col.toUpperCase()).toString();
		} catch(Exception e) {
			try {
				return map.get(col.toLowerCase()).toString();
			} catch(Exception e2) {
				return null;
			}
		}
	}
	
	// FIXME: This should be moved to sql connector level.
	// Currently this works for mysql and postgresql.
	private void getFunctionsAndProcedures() {
		if (sqlConnector instanceof SqliteConnector)
			return;

		try {
			sqlConnector.executeQueryAsync("select * from INFORMATION_SCHEMA.ROUTINES where ROUTINE_SCHEMA = ? ",
					Arrays.asList(sqlConnector.getDbSchema()), rset -> {
						var map = DTOMapper.mapUnsafely(rset);
						var ti = new TreeItem<String>();
						ti.setValue(String.valueOf(getULN(map, "ROUTINE_NAME")));
						var routineType = getULN(map, "ROUTINE_TYPE");
						
						var url = "/icons/" + ("PROCEDURE".equals(routineType) ? "procedure" : "function") + ".png";
						ti.setGraphic(JavaFXUtils.createIcon(url));
						this.fillFPTreeItem(ti, map);

						if ("PROCEDURE".equals(routineType)) {
							proceduresRootItem.getChildren().add(ti);
						}
						else {
							functionsRootItem.getChildren().add(ti);
						}
						
					});

		} catch (SQLException e1) {
			DialogFactory.createErrorDialog(e1);
		}
	}

	private List<String> setupTreeItems() throws SQLException {
		var newItems = new ArrayList<String>();
		sqlConnector.getContents(rset -> {
			try {
				// In every sql connector implementation columns must be ordered as 'name','type' in contents query
				var name = rset.getString(1);
				var type = rset.getString(2);
				
				if (name == null || type == null) {
					logger.error("Could not map table name or type, type: " + type + " ,name: " + name);
					return;
				}
	
				newItems.add(name);
				if (!allItems.contains(name)) {
					allItems.add(name);
					var treeItem = new TreeItem<>(name);
					if (type.toLowerCase().contains("table")) {
						this.fillTableTreeItem(treeItem);
						tablesRootItem.getChildren().add(treeItem);
						treeItem.setGraphic(JavaFXUtils.createIcon("/icons/table.png"));
						// TODO find another way with no calls to static class SqlCodeAreaSyntax
						SqlCodeAreaSyntaxProvider.bind(name, this.getColumnsFor(name));
					} else if (type.toLowerCase().contains("view")) {
						this.fillViewTreeItem(treeItem);
						viewsRootItem.getChildren().add(treeItem);
						treeItem.setGraphic(JavaFXUtils.createIcon("/icons/view.png"));
						// TODO find another way with no calls to static class SqlCodeAreaSyntax
						SqlCodeAreaSyntaxProvider.bind(name, this.getColumnsFor(name));
					} else if (type.toLowerCase().contains("index")) {
						this.fillIndexTreeItem(treeItem);
						indexesRootItem.getChildren().add(treeItem);
						treeItem.setGraphic(JavaFXUtils.createIcon("/icons/index.png"));
					}
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		});
		
		return newItems;
	}

	private void updateTriggers() throws SQLException {
		for (var treeItem : tablesRootItem.getChildren()) {
			// triggers tree item is the 2nd child
			treeItem.getChildren().get(2).getChildren().clear();
			sqlConnector.getTriggers(treeItem.getValue(), rset -> {
				var triggerTreeItem = new TreeItem<>(rset.getString(1),
                        JavaFXUtils.createIcon("/icons/trigger.png"));
				var schema = rset.getString(2);
				triggerTreeItem.getChildren()
						.add(new TreeItem<>(schema, JavaFXUtils.createIcon("/icons/script.png")));
				var triggerItems = treeItem.getChildren().get(2).getChildren();
				triggerItems.add(triggerTreeItem);
			});
		}
	}

	private void clearAll() {
		tablesRootItem.getChildren().clear();
		viewsRootItem.getChildren().clear();
		indexesRootItem.getChildren().clear();
		allItems.clear();
	}

	private void removeMissingTreeItems(List<String> newItems) {
		var found = new ArrayList<TreeItem<String>>();
		var sfound = new ArrayList<String>();
		
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

		indexesRootItem.getChildren().forEach(treeItem -> {
			if (!newItems.contains(treeItem.getValue())) {
				found.add(treeItem);
				sfound.add(treeItem.getValue());
			}
		});
		indexesRootItem.getChildren().removeAll(found);
		allItems.removeAll(sfound);
	}
	
	private void fillTreeView() throws SQLException {
		var thread = new Thread(() -> {
			try {
				var timeCounter = System.currentTimeMillis();
	
				var newItems = this.setupTreeItems();
				this.removeMissingTreeItems(newItems);
	
				Platform.runLater(() -> {
					this.setRoot(rootItem);
					this.fireEvent(new SimpleEvent());
				});
	
				timeCounter = (System.currentTimeMillis() - timeCounter) / 1000;
				LoggerFactory.getLogger(LoggerConf.LOGGER_NAME)
						.info("Database analysis took " + timeCounter + " seconds");
				this.changed();
			} catch (Throwable e) {
				DialogFactory.createErrorDialog(e);
			}
		});
		
		thread.setDaemon(true);
		thread.start();
	}

	/**
	 * Fills a treeItem that is either referring to a table or view.
	 * 
	 * @param treeItem
	 * @param schemaColumn
	 * @throws SQLException
	 */
	private void fillTVTreeItem(TreeItem<String> treeItem, String schemaColumn) throws SQLException {
		var schemaTree = new TreeItem<>("schema", JavaFXUtils.createIcon("/icons/script.png"));
		treeItem.getChildren().add(schemaTree);
	
		if (schemaColumn.equals("table")) {
			sqlConnector.getTableSchema(treeItem.getValue(), rset -> {
				var schema = rset.getString(1);
				// FIXME: find a more abstract way
				DbCash.addSchemaFor(treeItem.getValue(), schema);
				var schemaItem = new TreeItem<>(schema);
				schemaTree.getChildren().add(schemaItem);
			});
		}
		else {
			sqlConnector.getViewSchema(treeItem.getValue(), rset -> {
				var schema = rset.getString(1);
				// FIXME: find a more abstract way
				DbCash.addSchemaFor(treeItem.getValue(), schema);
				var schemaItem = new TreeItem<>(schema);
				schemaTree.getChildren().add(schemaItem);
			});
		}

	
		var columnsTree = new TreeItem<>("columns", JavaFXUtils.createIcon("/icons/columns.png"));
		treeItem.getChildren().add(columnsTree);
	
		// executing a query that will return zero results just to resolve columns
		sqlConnector.executeQueryRaw("select * from " + treeItem.getValue() + " where 1 = 2", rset -> {
			var sqlTable = new SqlTable(rset.getMetaData());
			sqlTable.setPrimaryKey(sqlConnector.findPrimaryKey(treeItem.getValue()));
			var fkeys = sqlConnector.findForeignKeyReferences(treeItem.getValue());
			sqlTable.setForeignKeys(
					fkeys.stream().map(x -> x.get(SqlConnector.FOREIGN_KEY)).collect(Collectors.toList()));
			sqlTable.getColumns();
			for (var column : sqlTable.getColumns()) {
				var columnTreeItem = new TreeItem<>(column);
				columnTreeItem.setGraphic(JavaFXUtils.createIcon("/icons/blue.png"));
				if (sqlTable.getPrimaryKey() != null && sqlTable.getPrimaryKey().contains(column))
					columnTreeItem.setGraphic(JavaFXUtils.createIcon("/icons/primary-key.png"));
				else if (sqlTable.isForeignKey(column)) {
					columnTreeItem.setGraphic(JavaFXUtils.createIcon("/icons/foreign-key.png"));
					var l = fkeys.stream()
							.filter(x -> x.get(SqlConnector.FOREIGN_KEY).equals(column)).toList();
					var map = !l.isEmpty() ? l.get(0) : null;
					if (map != null) {
						var refColumn = map.get(SqlConnector.REFERENCED_TABLE) + ": "
								+ map.get(SqlConnector.REFERENCED_KEY);
						var referenceItem = new TreeItem<>(refColumn);
						referenceItem.setGraphic(JavaFXUtils.createIcon("/icons/blue.png"));
						columnTreeItem.getChildren().add(referenceItem);
					}
	
				}
				columnsTree.getChildren().add(columnTreeItem);
			}
			DbCash.addTable(sqlTable);
		});
	}

	private void fillTableTreeItem(TreeItem<String> treeItem) throws SQLException {
		this.fillTVTreeItem(treeItem, "table");
		var triggersTreeItem = new TreeItem<>("triggers",
                JavaFXUtils.createIcon("/icons/trigger.png"));
		sqlConnector.getTriggers(treeItem.getValue(), rset -> {
			var triggerTreeItem = new TreeItem<>(rset.getString(1),
                    JavaFXUtils.createIcon("/icons/trigger.png"));
			var schema = rset.getString(2);
			triggerTreeItem.getChildren()
					.add(new TreeItem<>(schema, JavaFXUtils.createIcon("/icons/script.png")));
			triggersTreeItem.getChildren().add(triggerTreeItem);
		});

		treeItem.getChildren().add(triggersTreeItem);
	}

	private void fillViewTreeItem(TreeItem<String> treeItem) throws SQLException {
		this.fillTVTreeItem(treeItem, "view");
	}

	private void fillIndexTreeItem(TreeItem<String> treeItem) throws SQLException {
		var schemaTree = new TreeItem<>("schema", JavaFXUtils.createIcon("/icons/script.png"));
		treeItem.getChildren().add(schemaTree);

		sqlConnector.getIndexSchema(treeItem.getValue(), rset -> {
			var schema = rset.getString(1);
			schemaTree.getChildren().add(new TreeItem<>(schema));
		});
	}

	/**
	 * Fills a treeItem that is either referring to a procedure or function.
	 * 
	 * @param treeItem
	 * @param map
	 * @throws SQLException
	 */
	private void fillFPTreeItem(TreeItem<String> treeItem, Map<String, Object> map) throws SQLException {
		var bodyTreeItem = new TreeItem<>("body",
                JavaFXUtils.createIcon("/icons/script.png"));
		bodyTreeItem.getChildren().add(new TreeItem<>(getULN(map, "ROUTINE_DEFINITION")));
		treeItem.getChildren().add(bodyTreeItem);
	
		var paramsTreeItem = new TreeItem<>("parameters",
				JavaFXUtils.createIcon("/icons/var.png"));
		treeItem.getChildren().add(paramsTreeItem);
	
		sqlConnector.executeQuery(
				"select * from INFORMATION_SCHEMA.PARAMETERS where SPECIFIC_NAME = ? ",
				Arrays.asList(getULN(map, "SPECIFIC_NAME")), rset2 -> {
					var map2 = DTOMapper.mapUnsafely(rset2);
	
					if (getULN(map2, "PARAMETER_MODE") != null) {
						var param = "";
						if (getULN(map2, "PARAMETER_NAME") != null)
							param += getULN(map2, "PARAMETER_NAME") + " ";
						if (getULN(map2, "PARAMETER_MODE") != null)
							param += getULN(map2, "PARAMETER_MODE") + " ";
						if (getULN(map2, "DATA_TYPE") != null)
							param += getULN(map2, "DATA_TYPE");
	
						paramsTreeItem.getChildren().add(new TreeItem<>(param));
					} else {
						treeItem.setValue(treeItem.getValue() + " returns " + getULN(map2, "DATA_TYPE"));
					}
				});
	}

	public void showSearchPopup() {
		var popOver = new PopOver();
		popOver.setArrowSize(0);
		popOver.show(this);
    }
	
	public HBox getSearchBox() {
		return searchBox;
	}

	public CustomTextField getSearchField() {
		return this.searchField;
	}
	
	public void showSearchPopup(Node owner) {
		var popOver = new CustomPopOver(new HBox(searchField, nextSearchResultButton));
		popOver.show(owner);
    }

	@Override
	public ContextMenu createContextMenu() {
		var contextMenu = new ContextMenu();

		var copy = new MenuItem("Copy Text", JavaFXUtils.createIcon("/icons/copy.png"));
		copy.setOnAction(event -> this.copyAction());

		var copySchema = new MenuItem("Copy Schema", JavaFXUtils.createIcon("/icons/script.png"));
		copySchema.setOnAction(event -> this.copyScemaAction());
		copySchema.disableProperty().bind(this.hasSelectedSchemaProperty.not());

		var drop = new MenuItem("Drop", JavaFXUtils.createIcon("/icons/minus.png"));
		drop.setOnAction(event -> dropAction());
		drop.disableProperty().bind(this.hasSelectedSchemaProperty.not());

		var collapseAll = new MenuItem("Collapse All", JavaFXUtils.createIcon("/icons/collapse.png"));
		collapseAll.setOnAction(event -> {
			if (this.getSelectionModel().getSelectedItem() != null)
				this.collapseAll(this.getSelectionModel().getSelectedItem());
		});

		var showSchema = new MenuItem("Show Schema", JavaFXUtils.createIcon("/icons/details.png"));
		showSchema.setOnAction(action -> {
			var codeArea = new SqlCodeArea(this.copyScemaAction(), false, false, isUsingMysql());
			var scrollPane = new VirtualizedScrollPane<>(codeArea);
			scrollPane.setPrefSize(600, 400);

			var popOver = new PopOver(scrollPane);
			popOver.setArrowSize(0);
			popOver.setDetachable(false);
			popOver.show(this.getSelectionModel().getSelectedItem().getGraphic());
		});
		showSchema.disableProperty().bind(this.hasSelectedSchemaProperty.not());

		var setAsRoot = new MenuItem("Select As Root", JavaFXUtils.createIcon("/icons/database.png"));
		setAsRoot.setOnAction(action -> {
			if (this.getSelectionModel().getSelectedItem() == null)
				return;

			this.selectedRootItem = this.getSelectionModel().getSelectedItem();
			this.setRoot(this.selectedRootItem);
			this.selectedRootItem.setExpanded(true);
		});
		setAsRoot.disableProperty().bind(this.getSelectionModel().selectedItemProperty().isNull());

		var restoreRoot = new MenuItem("Restore Db Root", JavaFXUtils.createIcon("/icons/refresh.png"));
		restoreRoot.setOnAction(event -> this.setRoot(this.rootItem));
		restoreRoot.disableProperty().bind(this.getSelectionModel().selectedItemProperty().isEqualTo(this.rootItem));

		contextMenu.getItems().addAll(copy, copySchema, new SeparatorMenuItem(), collapseAll, showSchema, new SeparatorMenuItem(), setAsRoot, restoreRoot, new SeparatorMenuItem(), drop);

		return contextMenu;
	}

	private void collapseAll(TreeItem<String> treeItem) {
		treeItem.getChildren().forEach(this::collapseAll);
		treeItem.setExpanded(false);
	}

	public void refreshItems() throws SQLException {
		if (this.parent != null)
			parent.setLoading(true);
		this.clearAll();
		this.fillTreeView();
	}

	public void dropAction() {
		if (tablesRootItem.getChildren().contains(this.getSelectionModel().getSelectedItem())) {
			var table = this.getSelectionModel().getSelectedItem().getValue();
			var message = "Do you want to delete " + table;
			var result = DialogFactory.createConfirmationDialog("Drop Table", message);
			if (result) {
				try {
					// TODO maybe execute async?
					sqlConnector.dropTable(table);
					this.fillTreeView();
				} catch (SQLException e) {
					DialogFactory.createErrorDialog(e);
				}
			}
		} else if (viewsRootItem.getChildren().contains(this.getSelectionModel().getSelectedItem())) {
			var view = this.getSelectionModel().getSelectedItem().getValue();
			var message = "Do you want to delete " + view;
			var result = DialogFactory.createConfirmationDialog("Drop View", message);
			if (result) {
				try {
					// TODO maybe execute async?
					sqlConnector.dropView(view);
					this.fillTreeView();
				} catch (SQLException e) {
					DialogFactory.createErrorDialog(e);
				}
			}
		} else if (indexesRootItem.getChildren().contains(this.getSelectionModel().getSelectedItem())) {
			var index = this.getSelectionModel().getSelectedItem().getValue();
			var message = "Do you want to delete " + index;
			var result = DialogFactory.createConfirmationDialog("Drop Index", message);
			if (result) {
				try {
					// TODO maybe execute async?
					sqlConnector.dropIndex(index);
					this.fillTreeView();
				} catch (SQLException e) {
					DialogFactory.createErrorDialog(e);
				}
			}
		} else if (proceduresRootItem.getChildren().contains(this.getSelectionModel().getSelectedItem())) {
			var procedure = this.getSelectionModel().getSelectedItem().getValue();
			var message = "Do you want to delete " + procedure;
			var result = DialogFactory.createConfirmationDialog("Drop Procedure", message);
			if (result) {
				try {
					sqlConnector.executeUpdate("drop procedure " + procedure);
					this.refreshFunctionAndProcedures();
				} catch (SQLException e) {
					DialogFactory.createErrorDialog(e);
				}
			}
		} else if (functionsRootItem.getChildren().contains(this.getSelectionModel().getSelectedItem())) {
			var function = this.getSelectionModel().getSelectedItem().getValue();
			function = function.replaceAll(" returns.*", "");
			var message = "Do you want to delete " + function;
			var result = DialogFactory.createConfirmationDialog("Drop Function", message);
			if (result) {
				try {
					sqlConnector.executeUpdate("drop function " + function);
					this.refreshFunctionAndProcedures();
				} catch (SQLException e) {
					DialogFactory.createErrorDialog(e);
				}
			}
		}
	}

	public String copyScemaAction() {
		var text = "";
		try {
			var startItem = this.getSelectionModel().getSelectedItems().get(0);

			if (tablesRootItem.getChildren().contains(startItem) || viewsRootItem.getChildren().contains(startItem)
					|| indexesRootItem.getChildren().contains(startItem)
					|| proceduresRootItem.getChildren().contains(startItem)
					|| functionsRootItem.getChildren().contains(startItem)) {
				text = startItem.getChildren().get(0).getChildren().get(0).getValue();
			}

			var stringSelection = new StringSelection(text);
			var clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			clipboard.setContents(stringSelection, null);
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return text;
	}

	private void copyAction() {
		var text = new StringBuilder();
		for (var treeItem : this.getSelectionModel().getSelectedItems()) {
			text.append(treeItem.getValue()).append(", ");
		}
		text = new StringBuilder(text.substring(0, text.length() - ", ".length()));

		var stringSelection = new StringSelection(text.toString());
		var clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(stringSelection, null);
	}

	private void searchFieldAction(boolean doSelectItems) {
		if (searchField.getText().isEmpty()) {
			return;
		}
		
		currentSearchPattern = searchField.getText();
		this.lastSelectedItemPos = -1;
		this.searchResultsList.clear();
		this.getSelectionModel().clearSelection();

		searchRootItem(tablesRootItem);
		searchRootItem(viewsRootItem);
		searchRootItem(indexesRootItem);
		searchRootItem(proceduresRootItem);
		searchRootItem(functionsRootItem);
		
		if (doSelectItems) {
			searchResultsList.forEach(treeItem -> this.getSelectionModel().select(treeItem));
		}
	}

	private void searchRootItem(TreeItem<String> rootItem) {
		if (rootItem == null)
			return;

		for (var t : rootItem.getChildren()) {
			if (t.getValue().matches("(?i:.*" + searchField.getText() + ".*)")) {
				searchResultsList.add(t);
			}
		}
	}

	public List<String> getContentNames() {
		return allItems;
	}

	@SuppressWarnings("unchecked")
	public List<String> getColumnsFor(String table) {
		var columns = new ArrayList<String>();
		for (var ti : FXCollections.concat(tablesRootItem.getChildren(), viewsRootItem.getChildren())) {
			if (ti.getValue().equals(table)) {
				ti.getChildren().forEach(c -> {
					if (c.getValue().contentEquals("columns")) {
						c.getChildren().forEach(cc -> columns.add(cc.getValue()));
                    }
				});
			}
		}
		return columns;
	}

	public void refreshFunctionAndProcedures() {
		proceduresRootItem.getChildren().clear();
		functionsRootItem.getChildren().clear();
		this.getFunctionsAndProcedures();
	}

	@Override
	public void onObservableChange(String newValue) {
		try {
			this.fillTreeView();
			if (newValue != null && (newValue.toLowerCase().contains("trigger")))
				this.updateTriggers();
			if (newValue != null
					&& (newValue.toLowerCase().contains("function") || newValue.toLowerCase().contains("procedure")))
				this.refreshFunctionAndProcedures();
		} catch (SQLException e) {
			DialogFactory.createErrorDialog(e);
		}
	}

	@Override
	public void changed() {
		listeners.forEach(listener -> listener.onObservableChange(null));
	}

	@Override
	public void changed(String data) {
	}

	@Override
	public void addObserver(SimpleObserver<String> listener) {
		listeners.add(listener);
	}

	@Override
	public void removeObserver(SimpleObserver<String> listener) {
		listeners.remove(listener);
	}

	private boolean isUsingMysql() {
		return sqlConnector instanceof MysqlConnector;
	}

	public SimpleBooleanProperty hasSelectedSchemaProperty() {
		return hasSelectedSchemaProperty;
	}

	public SimpleBooleanProperty canSelectedOpenProperty() {
		return canSelectedOpenProperty;
	}
}
