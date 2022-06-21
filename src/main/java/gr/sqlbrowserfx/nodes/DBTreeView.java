package gr.sqlbrowserfx.nodes;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.controlsfx.control.PopOver;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gr.sqlbrowserfx.LoggerConf;
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
import gr.sqlbrowserfx.utils.JavaFXUtils;
import gr.sqlbrowserfx.utils.mapper.DTOMapper;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;

public class DBTreeView extends TreeView<String>
		implements ContextMenuOwner, SimpleObserver<String>, SimpleObservable<String> {

	private static final String ACTION_STATEMENT = "ACTION_STATEMENT";
	private static final String TRIGGER_NAME = "TRIGGER_NAME";
	private Logger logger = LoggerFactory.getLogger(LoggerConf.LOGGER_NAME);
	private SqlConnector sqlConnector;

	private TreeItem<String> rootItem;
	protected TreeItem<String> tablesRootItem;
	protected TreeItem<String> viewsRootItem;
	private TreeItem<String> indexesRootItem;
	private TreeItem<String> proceduresRootItem;
	private TreeItem<String> functionsRootItem;

	private List<String> allItems;
	private List<SimpleObserver<String>> listeners;

	private TextField searchField;
	private DDBTreePane parent = null;
	private Integer lastSelectedItemPos = 0;
	private List<TreeItem<String>> searchResultsList = new ArrayList<>();
	private Button nextSearchResultButton;

	@SuppressWarnings("unchecked")
	public DBTreeView(String dbPath, SqlConnector sqlConnector) {
		super();
		this.sqlConnector = sqlConnector;
		this.allItems = new ArrayList<>();
		this.listeners = new ArrayList<>();

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

		searchField = new TextField();
		searchField.setPromptText("Search...");
		searchField.setOnKeyPressed(keyEvent -> {
			if (keyEvent.getCode() == KeyCode.ENTER) {
				this.searchFieldAction();
			}
		});
		nextSearchResultButton = new Button("", JavaFXUtils.createIcon("/icons/next.png"));
		nextSearchResultButton.setOnAction(event -> {
			if (searchResultsList.isEmpty())
				return;
			lastSelectedItemPos = lastSelectedItemPos == searchResultsList.size() - 1 ? 0 : ++lastSelectedItemPos;
			this.getSelectionModel().clearSelection();
			this.getSelectionModel().select(searchResultsList.get(lastSelectedItemPos));
			int row = this.getRow(searchResultsList.get(lastSelectedItemPos));
			this.scrollTo(row);
		});

		this.setInputMap();
	}

	protected void setInputMap() {
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
				case C:
					this.copyAction();
					break;
				case F:
					this.showSearchPopup();
					break;
				default:
					break;
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
						Map<String, Object> map = DTOMapper.mapUnsafely(rset);
						TreeItem<String> ti = new TreeItem<>();
						ti.setValue(String.valueOf(getULN(map, "ROUTINE_NAME")));
						String routineType = getULN(map, "ROUTINE_TYPE");
						
						if (routineType.equals("PROCEDURE"))
							ti.setGraphic(JavaFXUtils.createIcon("/icons/procedure.png"));
						else
							ti.setGraphic(JavaFXUtils.createIcon("/icons/function.png"));

						TreeItem<String> bodyTreeItem = new TreeItem<String>("body",
								JavaFXUtils.createIcon("/icons/script.png"));
						bodyTreeItem.getChildren().add(new TreeItem<String>(getULN(map, "ROUTINE_DEFINITION")));
						ti.getChildren().add(bodyTreeItem);

						TreeItem<String> paramsTreeItem = new TreeItem<>("parameters",
								JavaFXUtils.createIcon("/icons/var.png"));
						ti.getChildren().add(paramsTreeItem);

						sqlConnector.executeQuery(
								"select * from INFORMATION_SCHEMA.PARAMETERS where SPECIFIC_NAME = ? ",
								Arrays.asList(getULN(map, "SPECIFIC_NAME")), rset2 -> {
									Map<String, Object> map2 = DTOMapper.mapUnsafely(rset2);

									if (getULN(map2, "PARAMETER_MODE") != null) {
										String param = "";
										if (getULN(map2, "PARAMETER_NAME") != null)
											param += getULN(map2, "PARAMETER_NAME") + " ";
										if (getULN(map2, "PARAMETER_MODE") != null)
											param += getULN(map2, "PARAMETER_MODE") + " ";
										if (getULN(map2, "DATA_TYPE") != null)
											param += getULN(map2, "DATA_TYPE");

										paramsTreeItem.getChildren().add(new TreeItem<String>(param));
									} else {
										ti.setValue(ti.getValue() + " returns " + getULN(map2, "DATA_TYPE"));
									}
								});

						if (routineType.equals("PROCEDURE"))
							proceduresRootItem.getChildren().add(ti);
						else
							functionsRootItem.getChildren().add(ti);
						
					});

		} catch (SQLException e1) {
			DialogFactory.createErrorDialog(e1);
		}
	}

	public void showSearchPopup() {
		PopOver popOver = new PopOver(new HBox(searchField, nextSearchResultButton));
		popOver.setArrowSize(0);
		popOver.show(this);;
	}
	
	public void showSearchPopup(Node owner) {
		PopOver popOver = new PopOver(new HBox(searchField, nextSearchResultButton));
		popOver.setArrowSize(0);
		popOver.show(owner);;
	}

	private void clearAll() {
		tablesRootItem.getChildren().clear();
		viewsRootItem.getChildren().clear();
		indexesRootItem.getChildren().clear();
		allItems.clear();
	}

	private void fillTreeView() throws SQLException {
		new Thread(() -> {
			try {
				long timeCounter = System.currentTimeMillis();

				List<String> newItems = this.getContents();
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

				indexesRootItem.getChildren().forEach(treeItem -> {
					if (!newItems.contains(treeItem.getValue())) {
						found.add(treeItem);
						sfound.add(treeItem.getValue());
					}
				});
				indexesRootItem.getChildren().removeAll(found);
				allItems.removeAll(sfound);

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
		}).start();
	}

	private List<String> getContents() throws SQLException {
		List<String> newItems = new ArrayList<>();
		sqlConnector.getContents(rset -> {
			try {
				HashMap<String, Object> dto = DTOMapper.mapUsingRealColumnNames(rset);

				String name = (String) dto.get(sqlConnector.getTableNameColumn());
				String type = (String) dto.get(sqlConnector.getTableTypeColumn());

				newItems.add(name);
				if (!allItems.contains(name)) {
					allItems.add(name);
					TreeItem<String> treeItem = new TreeItem<String>(name);
					if (type.toLowerCase().contains("table")) {
						this.fillTableTreeItem(treeItem);
						tablesRootItem.getChildren().add(treeItem);
						treeItem.setGraphic(JavaFXUtils.createIcon("/icons/table.png"));
						// TODO find another way with no calls to static class SqlCodeAreaSyntax
						SqlCodeAreaSyntaxProvider.bind(name, this.getColumnsForTable(name));
					} else if (type.toLowerCase().contains("view")) {
						this.fillViewTreeItem(treeItem);
						viewsRootItem.getChildren().add(treeItem);
						treeItem.setGraphic(JavaFXUtils.createIcon("/icons/view.png"));
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

	private void fillTVTreeItem(TreeItem<String> treeItem, String schemaColumn) throws SQLException {
		TreeItem<String> schemaTree = new TreeItem<>("schema", JavaFXUtils.createIcon("/icons/script.png"));
		treeItem.getChildren().add(schemaTree);

		sqlConnector.getSchema(treeItem.getValue(), rset -> {
			String schema = rset.getString(schemaColumn);
			TreeItem<String> schemaItem = new TreeItem<>(schema); // , new SqlCodeArea(schema, false, false,
																	// isUsingMysql()));
			schemaTree.getChildren().add(schemaItem);
		});

		TreeItem<String> columnsTree = new TreeItem<>("columns", JavaFXUtils.createIcon("/icons/columns.png"));
		treeItem.getChildren().add(columnsTree);

		sqlConnector.executeQueryRaw("select * from " + treeItem.getValue() + " where 1 = 2", rset -> {
			SqlTable sqlTable = new SqlTable(rset.getMetaData());
			sqlTable.setPrimaryKey(sqlConnector.findPrimaryKey(treeItem.getValue()));
			List<Map<String, String>> fkeys = sqlConnector.findFoireignKeyReferences(treeItem.getValue());
			sqlTable.setForeignKeys(
					fkeys.stream().map(x -> x.get(SqlConnector.FOREIGN_KEY)).collect(Collectors.toList()));
			sqlTable.getColumns();
			for (String column : sqlTable.getColumns()) {
				TreeItem<String> columnTreeItem = new TreeItem<>(column);
				columnTreeItem.setGraphic(JavaFXUtils.createIcon("/icons/blue.png"));
				if (sqlTable.getPrimaryKey() != null && sqlTable.getPrimaryKey().contains(column))
					columnTreeItem.setGraphic(JavaFXUtils.createIcon("/icons/primary-key.png"));
				else if (sqlTable.isForeignKey(column)) {
					columnTreeItem.setGraphic(JavaFXUtils.createIcon("/icons/foreign-key.png"));
					List<Map<String, String>> l = fkeys.stream()
							.filter(x -> x.get(SqlConnector.FOREIGN_KEY).equals(column)).collect(Collectors.toList());
					Map<String, String> map = l.size() > 0 ? l.get(0) : null;
					if (map != null) {
						String refColumn = map.get(SqlConnector.REFERENCED_TABLE) + ": "
								+ map.get(SqlConnector.REFERENCED_KEY);
						TreeItem<String> referenceItem = new TreeItem<>(refColumn);
						referenceItem.setGraphic(JavaFXUtils.createIcon("/icons/blue.png"));
						columnTreeItem.getChildren().add(referenceItem);
					}

				}
				columnsTree.getChildren().add(columnTreeItem);
			}
		});
	}

	private void updateTriggers() throws SQLException {
		for (TreeItem<String> treeItem : tablesRootItem.getChildren()) {
			treeItem.getChildren().get(2).getChildren().clear();
			sqlConnector.getTriggers(treeItem.getValue(), rset -> {
				TreeItem<String> triggerTreeItem = new TreeItem<String>(rset.getString(TRIGGER_NAME),
						JavaFXUtils.createIcon("/icons/trigger.png"));
				String schema = rset.getString(ACTION_STATEMENT);
				triggerTreeItem.getChildren()
						.add(new TreeItem<String>(schema, JavaFXUtils.createIcon("/icons/script.png")));
				ObservableList<TreeItem<String>> triggerItems = treeItem.getChildren().get(2).getChildren();
				triggerItems.add(triggerTreeItem);
			});
		}
	}

	private void fillTableTreeItem(TreeItem<String> treeItem) throws SQLException {
		this.fillTVTreeItem(treeItem, sqlConnector.getTableSchemaColumn());
		TreeItem<String> triggersTreeItem = new TreeItem<String>("triggers",
				JavaFXUtils.createIcon("/icons/trigger.png"));
		sqlConnector.getTriggers(treeItem.getValue(), rset -> {
			TreeItem<String> triggerTreeItem = new TreeItem<String>(rset.getString(TRIGGER_NAME),
					JavaFXUtils.createIcon("/icons/trigger.png"));
			String schema = rset.getString(ACTION_STATEMENT);
			triggerTreeItem.getChildren()
					.add(new TreeItem<String>(schema, JavaFXUtils.createIcon("/icons/script.png")));
			triggersTreeItem.getChildren().add(triggerTreeItem);
		});

//		TreeItem<String> indexesTreeItem = new TreeItem<String>("indexes", JavaFXUtils.createIcon("/icons/index.png"));
		treeItem.getChildren().add(triggersTreeItem);

	}

	private void fillViewTreeItem(TreeItem<String> treeItem) throws SQLException {
		this.fillTVTreeItem(treeItem, sqlConnector.getViewSchemaColumn());
	}

	private void fillIndexTreeItem(TreeItem<String> treeItem) throws SQLException {
		TreeItem<String> schemaTree = new TreeItem<>("schema", JavaFXUtils.createIcon("/icons/script.png"));
		treeItem.getChildren().add(schemaTree);

		sqlConnector.getSchema(treeItem.getValue(), rset -> {
			String schema = rset.getString(sqlConnector.getIndexSchemaColumn());
			schemaTree.getChildren().add(new TreeItem<String>(schema));
		});
	}

	@Override
	public ContextMenu createContextMenu() {
		ContextMenu contextMenu = new ContextMenu();

		MenuItem menuItemCopy = new MenuItem("Copy text", JavaFXUtils.createIcon("/icons/copy.png"));
		menuItemCopy.setOnAction(event -> this.copyAction());

		MenuItem menuItemCopyScema = new MenuItem("Copy schema", JavaFXUtils.createIcon("/icons/script.png"));
		menuItemCopyScema.setOnAction(event -> this.copyScemaAction());

		MenuItem menuItemDrop = new MenuItem("Drop", JavaFXUtils.createIcon("/icons/minus.png"));
		menuItemDrop.setOnAction(event -> dropAction());

		MenuItem menuItemCollapseAll = new MenuItem("Collapse All", JavaFXUtils.createIcon("/icons/collapse.png"));
		menuItemCollapseAll.setOnAction(event -> {
			if (this.getSelectionModel().getSelectedItem() != null)
				this.collapseAll(this.getSelectionModel().getSelectedItem());
		});

		MenuItem menuItemOpenSchema = new MenuItem("Show schema", JavaFXUtils.createIcon("/icons/details.png"));
		menuItemOpenSchema.setOnAction(action -> {
			SqlCodeArea codeArea = new SqlCodeArea(this.copyScemaAction(), false, false, isUsingMysql());
			VirtualizedScrollPane<SqlCodeArea> scrollPane = new VirtualizedScrollPane<>(codeArea);
			scrollPane.setPrefSize(600, 400);

			PopOver popOver = new PopOver(scrollPane);
			popOver.setArrowSize(0);
			popOver.setDetachable(false);
			popOver.show(this.getSelectionModel().getSelectedItem().getGraphic());
		});

		contextMenu.getItems().addAll(menuItemCopy, menuItemCopyScema, menuItemOpenSchema, menuItemDrop,
				menuItemCollapseAll);

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
			String table = this.getSelectionModel().getSelectedItem().getValue();
			String message = "Do you want to delete " + table;
			int result = DialogFactory.createConfirmationDialog("Drop Table", message);
			if (result == 1) {
				try {
					// TODO maybe execute async?
					sqlConnector.dropTable(table);
					this.fillTreeView();
				} catch (SQLException e) {
					DialogFactory.createErrorDialog(e);
				}
			}
		} else if (viewsRootItem.getChildren().contains(this.getSelectionModel().getSelectedItem())) {
			String view = this.getSelectionModel().getSelectedItem().getValue();
			String message = "Do you want to delete " + view;
			int result = DialogFactory.createConfirmationDialog("Drop View", message);
			if (result == 1) {
				try {
					// TODO maybe execute async?
					sqlConnector.dropView(view);
					this.fillTreeView();
				} catch (SQLException e) {
					DialogFactory.createErrorDialog(e);
				}
			}
		} else if (indexesRootItem.getChildren().contains(this.getSelectionModel().getSelectedItem())) {
			String index = this.getSelectionModel().getSelectedItem().getValue();
			String message = "Do you want to delete " + index;
			int result = DialogFactory.createConfirmationDialog("Drop Index", message);
			if (result == 1) {
				try {
					// TODO maybe execute async?
					sqlConnector.dropIndex(index);
					this.fillTreeView();
				} catch (SQLException e) {
					DialogFactory.createErrorDialog(e);
				}
			}
		} else if (proceduresRootItem.getChildren().contains(this.getSelectionModel().getSelectedItem())) {
			String procedure = this.getSelectionModel().getSelectedItem().getValue();
			String message = "Do you want to delete " + procedure;
			int result = DialogFactory.createConfirmationDialog("Drop Procedure", message);
			if (result == 1) {
				try {
					sqlConnector.executeUpdate("drop procedure " + procedure);
					this.refreshFunctionAndProcedures();
				} catch (SQLException e) {
					DialogFactory.createErrorDialog(e);
				}
			}
		} else if (functionsRootItem.getChildren().contains(this.getSelectionModel().getSelectedItem())) {
			String function = this.getSelectionModel().getSelectedItem().getValue();
			function = function.replaceAll(" returns.*", "");
			String message = "Do you want to delete " + function;
			int result = DialogFactory.createConfirmationDialog("Drop Function", message);
			if (result == 1) {
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
		String text = "";
		try {
			TreeItem<String> startItem = this.getSelectionModel().getSelectedItems().get(0);

			if (tablesRootItem.getChildren().contains(startItem) || viewsRootItem.getChildren().contains(startItem)
					|| indexesRootItem.getChildren().contains(startItem)
					|| proceduresRootItem.getChildren().contains(startItem)
					|| functionsRootItem.getChildren().contains(startItem)) {
				text = startItem.getChildren().get(0).getChildren().get(0).getValue();
			}

			StringSelection stringSelection = new StringSelection(text);
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			clipboard.setContents(stringSelection, null);
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return text;
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
		this.lastSelectedItemPos = -1;
		this.searchResultsList.clear();
		this.getSelectionModel().clearSelection();

		searchRootItem(tablesRootItem);
		searchRootItem(viewsRootItem);
		searchRootItem(indexesRootItem);
		searchRootItem(proceduresRootItem);
		searchRootItem(functionsRootItem);
	}

	private void searchRootItem(TreeItem<String> rootItem) {
		if (rootItem == null)
			return;

		for (TreeItem<String> t : rootItem.getChildren()) {
			if (t.getValue().matches("(?i:.*" + searchField.getText() + ".*)")) {
				this.getSelectionModel().select(t);
				searchResultsList.add(t);
			}
		}
	}

	public List<String> getContentNames() {
		return allItems;
	}

	public List<String> getColumnsForTable(String table) {
		List<String> colums = new ArrayList<>();
		for (TreeItem<String> ti : tablesRootItem.getChildren()) {
			if (ti.getValue().equals(table)) {
				ti.getChildren().forEach(c -> {
					if (c.getValue().contentEquals("columns")) {
						c.getChildren().forEach(cc -> {
							colums.add(cc.getValue());
						});
						return;
					}
				});
			}
		}
		return colums;
	}

	public void refreshFunctionAndProcedures() {
		proceduresRootItem.getChildren().clear();
		functionsRootItem.getChildren().clear();
		this.getFunctionsAndProcedures();
	}

	@Override
	public void onObservaleChange(String newValue) {
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
		listeners.forEach(listener -> listener.onObservaleChange(null));
	}

	@Override
	public void changed(String data) {
		// TODO Auto-generated method stub

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
}
