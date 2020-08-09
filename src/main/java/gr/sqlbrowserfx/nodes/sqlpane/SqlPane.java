package gr.sqlbrowserfx.nodes.sqlpane;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.controlsfx.control.PopOver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gr.sqlbrowserfx.conn.SqlConnector;
import gr.sqlbrowserfx.factories.DialogFactory;
import gr.sqlbrowserfx.nodes.ContextMenuOwner;
import gr.sqlbrowserfx.nodes.SqlConsolePane;
import gr.sqlbrowserfx.nodes.ToolbarOwner;
import gr.sqlbrowserfx.nodes.tableviews.MapTableViewRow;
import gr.sqlbrowserfx.nodes.tableviews.SqlTableView;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class SqlPane extends BorderPane implements ToolbarOwner, ContextMenuOwner{

	protected FlowPane toolBar;
	protected Button addButton;
	protected Button editButton;
	protected Button deleteButton;
	protected Button settingsButton;
	private Button columnsSettingsButton;
	protected Button searchButton;
	protected Button refreshButton;
	protected Button tableSelectButton;
	protected Button sqlConsoleButton;
	protected TextField searchField;
	protected CheckBox resizeModeCheckBox;
	protected CheckBox fullModeCheckBox;
	private CheckBox limitModeCheckBox;
	protected ComboBox<String> tablesBox;
	protected ComboBox<String> viewsBox;
	private TextField pathField;
	protected ListView<String> logListView;
	private Button exportCsvButton;
	private LinkedHashMap<String, CheckBox> columnCheckBoxesMap;
	protected PopOver popOver;
	private Tab addRecordTab;
	private Tab addTableTab;
	private Label rowsCountLabel;
	protected TabPane tablesTabPane;
	
	protected SqlConnector sqlConnector;
	protected Boolean sqlQueryRunning;
	private String columnsFilter = "*";
	private Button importCsvButton;
	protected Logger logger = LoggerFactory.getLogger("SQLBROWSER");
	protected boolean uiLogging = false;

	private String EMPTY = "empty";
	private String whereFilter = "";
	private int linesLimit = 50000;

	public SqlPane() {
		this(null);
	}

	public SqlPane(SqlConnector sqlConnector) {
		columnCheckBoxesMap = new LinkedHashMap<>();
		this.sqlConnector = sqlConnector;

		sqlQueryRunning = false;

		popOver = new PopOver();
		pathField = new TextField();

		toolBar = this.createToolbar();
		toolBar.setOrientation(Orientation.VERTICAL);
		// toolBar.orientationProperty().set(Orientation.VERTICAL);

		tablesTabPane = new TabPane();

		addTableTab = new Tab("");
		addTableTab.setGraphic(JavaFXUtils.createIcon("/icons/add.png"));
		addTableTab.setClosable(false);
		tablesTabPane.getTabs().add(addTableTab);

		tablesTabPane.setOnMouseClicked(mouseEvent -> this.tablesTabPaneClickAction());

		this.addSqlTableTab();

		resizeModeCheckBox = new CheckBox("Auto resize");
		resizeModeCheckBox.setOnMouseClicked(event -> {
			if (resizeModeCheckBox.isSelected())
				getSelectedSqlTableView().autoResizedColumns(true);
			else
				getSelectedSqlTableView().autoResizedColumns(false);
		});
		fullModeCheckBox = new CheckBox("Full mode");
		fullModeCheckBox.setOnMouseClicked(moueEvent -> {
			SqlTableTab sqlTableTab = getSelectedTableTab();
			if (isFullMode() && !sqlTableTab.getCustomText().equals(EMPTY)) {
				this.enableFullMode(sqlTableTab);
			} else {
				this.disableFullMode();
			}

		});

		limitModeCheckBox = new CheckBox("Lines limit " + linesLimit );
		rowsCountLabel = new Label("0 rows");
		
		this.setLeft(toolBar);
		this.setCenter(tablesTabPane);
		this.setBottom(rowsCountLabel);

		DraggingTabPaneSupport dragingSupport = new DraggingTabPaneSupport("/icons/table.png");
        dragingSupport.addSupport(this);
	}

	@Override
	public FlowPane createToolbar() {
		addButton = new Button("", JavaFXUtils.createIcon("/icons/add.png"));
		addButton.setOnMouseClicked(event -> addButtonAction());
		addButton.setOnAction(event -> addButtonAction());

		deleteButton = new Button("", JavaFXUtils.createIcon("/icons/minus.png"));
		deleteButton.setOnMouseClicked(event -> deleteButtonAction());
		deleteButton.setOnAction(event -> deleteButtonAction());

		editButton = new Button("", JavaFXUtils.createIcon("/icons/edit.png"));
		editButton.setOnMouseClicked(mouseEvent -> editButtonAction(mouseEvent));
		editButton.setOnAction(mouseEvent -> editButtonAction(this.simulateClickEvent(editButton)));

		searchField = new TextField();
		searchField.setPromptText("Search...");
		searchField.setOnKeyPressed(keyEvent -> {
			if (keyEvent.getCode() == KeyCode.ENTER) {
				if (searchField.getText().isEmpty()) {
					Platform.runLater(() -> getSelectedSqlTableView().setItems(getSelectedSqlTableView().getSqlTableRows()));
				} else {
					SqlPane.this.searchFieldAction();
				}
			}
		});

		settingsButton = new Button("", JavaFXUtils.createIcon("/icons/settings.png"));
		settingsButton.setOnMouseClicked(event -> this.settingsButtonAction());
		settingsButton.setOnAction(event -> this.settingsButtonAction());

		searchButton = new Button("", JavaFXUtils.createIcon("/icons/magnify.png"));
		searchButton.setOnAction(actionEvent -> this.searchButtonAction());
		searchButton.setOnMouseClicked(mouseEvent -> this.searchButtonAction());

		importCsvButton = new Button("", JavaFXUtils.createIcon("/icons/csv-import.png"));
		importCsvButton.setOnAction(actionEvent -> this.importCsvAction());
		importCsvButton.setOnMouseClicked(mouseEvent -> this.importCsvAction());

		exportCsvButton = new Button("", JavaFXUtils.createIcon("/icons/csv.png"));
		exportCsvButton.setOnAction(actionEvent -> this.exportCsvAction());
		exportCsvButton.setOnMouseClicked(mouseEvent -> this.exportCsvAction());

		sqlConsoleButton = new Button("", JavaFXUtils.createIcon("/icons/console.png"));
		sqlConsoleButton.setOnMouseClicked(mouseEvent -> this.sqlConsoleButtonAction());
		//FIXME maybe uncomment this
//		sqlConsoleButton.setOnAction(mouseEvent -> this.sqlConsoleButtonAction());
		
		if (sqlConnector != null) {
			refreshButton = new Button("", JavaFXUtils.createIcon("/icons/refresh.png"));
			tableSelectButton = new Button("", JavaFXUtils.createIcon("/icons/database.png"));

			tableSelectButton.setOnMouseClicked(event -> this.tableSelectButtonAction());
			tableSelectButton.setOnAction(event -> this.tableSelectButtonAction());

			refreshButton.setOnMouseClicked(event -> refreshButtonAction());
//FIXME something goes wrong if both action and mouse handlers set
//			refreshButton.setOnAction(event -> refreshButtonAction());
			columnsSettingsButton = new Button("", JavaFXUtils.createIcon("/icons/table-settings.png"));
			columnsSettingsButton.setOnMouseClicked(mouseEvent -> this.columnsSettingsButtonAction());

			return new FlowPane(searchButton, settingsButton, columnsSettingsButton,tableSelectButton, refreshButton, addButton, editButton, deleteButton,
					importCsvButton, exportCsvButton, sqlConsoleButton);
		} else {
			return new FlowPane(searchButton, settingsButton);
		}

	}

	protected ComboBox<String> createTablesBox() {
		List<String> tablesList = null;
		try {
			tablesList = sqlConnector.getTables();
		} catch (SQLException e) {
			DialogFactory.createErrorDialog(e);
		}
		ObservableList<String> options = FXCollections.observableArrayList(tablesList);

		if (tablesBox == null || !tablesList.equals(tablesBox.getItems())) {
			String lastSelected = null;
			if (tablesBox != null)
				lastSelected = tablesBox.getSelectionModel().getSelectedItem();
			tablesBox = new ComboBox<>(options);
			tablesBox.setPromptText("table...");
			if (lastSelected != null)
				tablesBox.getSelectionModel().select(lastSelected);
			tablesBox.setOnAction(event -> this.tableComboBoxAction(tablesBox));
		}
		return tablesBox;
	}
	
	protected ComboBox<String> createViewsBox() {
		List<String> tablesList = null;
		try {
			tablesList = sqlConnector.getViews();
		} catch (SQLException e) {
			DialogFactory.createErrorDialog(e);
		}
		ObservableList<String> options = FXCollections.observableArrayList(tablesList);

		if (viewsBox == null || !tablesList.equals(viewsBox.getItems())) {
			String lastSelected = null;
			if (viewsBox != null)
				lastSelected = viewsBox.getSelectionModel().getSelectedItem();
			viewsBox = new ComboBox<>(options);
			viewsBox.setPromptText("view...");
			if (lastSelected != null)
				viewsBox.getSelectionModel().select(lastSelected);
			viewsBox.setOnAction(event -> this.tableComboBoxAction(viewsBox));
		}
		return viewsBox;
	}

	private SqlTableTab createSqlTableTab() {
		SqlTableView sqlTableViewRef = new SqlTableView();
		sqlTableViewRef.setSqlConnector(sqlConnector);
		sqlTableViewRef.setOnMouseClicked(mouseEvent -> {
			sqlTableViewRef.requestFocus();
			if (mouseEvent.getClickCount() == 2) {
				if (getSelectedSqlTableView().getSelectionModel().getSelectedItem() != null) {
					getSelectedSqlTableView().getSelectedCell().startEdit();

//						if (isFullMode()) {
//							this.fullModeAction();
//						} else {
//							this.editButtonAction(mouseEvent);
//						}
					getSelectedSqlTableView().requestFocus();
				}
			}
		});
		sqlTableViewRef.setContextMenu(this.createContextMenu());
		sqlTableViewRef.setOnKeyPressed(keyEvent -> {
			if (keyEvent.isControlDown()) {
				switch (keyEvent.getCode()) {
				case F:
					this.searchButtonAction();
					break;
				case C:
					this.copyAction();
					sqlTableViewRef.requestFocus();
					break;
				case D:
					this.deleteButtonAction();
					sqlTableViewRef.requestFocus();
					break;
				case E:
					this.editButtonAction(simulateClickEvent(editButton));
					sqlTableViewRef.requestFocus();
					break;
				case Q:
					this.addButtonAction();
					sqlTableViewRef.requestFocus();
					break;
				case I:
					this.importCsvAction();
					sqlTableViewRef.requestFocus();
					break;
				case R:
					this.refreshButtonAction();
					sqlTableViewRef.requestFocus();
					break;
				default:
					break;
				}
			}
			sqlTableViewRef.requestFocus();
		});
		SqlTableTab tab = new SqlTableTab(EMPTY, sqlTableViewRef);
		sqlTableViewRef.setParent(tab);
		tab.customTextProperty().addListener((observable, oldValue, newValue) -> {
			if (getSelectedSqlTableView().isFilledByQuery()) {
				tab.setCustomGraphic(JavaFXUtils.createIcon("/icons/table-y.png"));
            }
			else if (tab.getGraphic() != null && viewsBox != null && viewsBox.getItems().contains(tab.getCustomText())) {
				tab.setCustomGraphic(JavaFXUtils.createIcon("/icons/view.png"));
            }
			else if (tab.getGraphic() != null && tablesBox != null && tablesBox.getItems().contains(tab.getCustomText())) {
				tab.setCustomGraphic(JavaFXUtils.createIcon("/icons/table.png"));
            }
		});
		
		return tab;
	}
	
	public SqlTableTab addSqlTableTab() {
		SqlTableTab tab = this.createSqlTableTab();
		tablesTabPane.getTabs().add(tab);
		tablesTabPane.getSelectionModel().select(tab);
		return tab;
	}
	
	public SqlTableTab addSqlTableTabLater() {
		SqlTableTab tab = this.createSqlTableTab();
		
		Platform.runLater(() -> {
			tablesTabPane.getTabs().add(tab);
			tablesTabPane.getSelectionModel().select(tab);
		});
		return tab;
	}

	public void createSqlTableTabWithData(String table) {
		if (sqlQueryRunning) {
			return;
		} else {
			tablesTabPane.getSelectionModel().select(addTableTab);
			SqlTableTab tab = this.addSqlTableTab();
			this.createTablesBox();
			this.createViewsBox();
			sqlConnector.executeAsync(() -> this.getDataFromDB(table, tab.getSqlTableView()));
		}
	}

	@Override
	public ContextMenu createContextMenu() {
		ContextMenu contextMenu = new ContextMenu();

		MenuItem menuItemEdit = new MenuItem("Edit", JavaFXUtils.createIcon("/icons/edit.png"));
		menuItemEdit.setOnAction(event -> {
			if (isFullMode()) {
				this.fullModeAction();
			} else {
				this.editButtonAction(simulateClickEvent());
			}
		});

		MenuItem menuItemCellEdit = new MenuItem("Edit cell", JavaFXUtils.createIcon("/icons/edit.png"));

		menuItemCellEdit.setOnAction(event -> {
			if (getSelectedSqlTableView().getSelectedCell() != null)
				getSelectedSqlTableView().getSelectedCell().startEdit();
		});

		MenuItem menuItemCopyCell = new MenuItem("Copy cell", JavaFXUtils.createIcon("/icons/copy.png"));

		menuItemCopyCell.setOnAction(event -> {
			if (getSelectedSqlTableView().getSelectedCell() != null) {
				StringSelection stringSelection = new StringSelection(getSelectedSqlTableView().getSelectedCell().getText());
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				clipboard.setContents(stringSelection, null);
			}
		});

		MenuItem menuItemDelete = new MenuItem("Delete", JavaFXUtils.createIcon("/icons/minus.png"));
		menuItemDelete.setOnAction(event -> this.deleteButtonAction());

		MenuItem menuItemCopy = new MenuItem("Copy row", JavaFXUtils.createIcon("/icons/copy.png"));
		menuItemCopy.setOnAction(actionEvent -> this.copyAction());

		MenuItem menuItemCompare = new MenuItem("Compare", JavaFXUtils.createIcon("/icons/compare.png"));
		menuItemCompare.setOnAction(actionEvent -> compareAction(simulateClickEvent()));
		
		contextMenu.getItems().addAll(menuItemEdit, menuItemCellEdit, menuItemCopyCell, menuItemCopy, menuItemCompare,
				menuItemDelete);

		return contextMenu;
	}

	@SuppressWarnings("unused")
	private SqlTableRowEditBox createEditBox(MapTableViewRow sqlTableRow) {
		return isFullMode() ? createEditBox(sqlTableRow, true) : createEditBox(sqlTableRow, false);
	}

	@SuppressWarnings("unused")
	private SqlTableRowEditBox createEditBox(MapTableViewRow sqlTableRow, Orientation toolBarOrientation) {
		return isFullMode() ? createEditBox(sqlTableRow, true, toolBarOrientation)
				: createEditBox(sqlTableRow, false, toolBarOrientation);
	}

	private SqlTableRowEditBox createEditBox(MapTableViewRow sqlTableRow, boolean resizeable) {
		return createEditBox(sqlTableRow, resizeable, Orientation.HORIZONTAL);
	}

	private SqlTableRowEditBox createEditBox(MapTableViewRow sqlTableRow, boolean resizeable, Orientation toolBarOrientation) {
		SqlTableRowEditBox editBox = new SqlTableRowEditBox(getSelectedSqlTableView(), sqlTableRow, resizeable);

		Button copyButton = new Button("", JavaFXUtils.createIcon("/icons/copy.png"));
		copyButton.setTooltip(new Tooltip("Copy"));
		copyButton.setOnAction(actionEvent -> editBox.copy());
		copyButton.setFocusTraversable(false);

		Button pasteButton = new Button("", JavaFXUtils.createIcon("/icons/paste.png"));
		pasteButton.setTooltip(new Tooltip("Paste"));
		pasteButton.setOnAction(actionEvent -> this.pasteAction(editBox));
		pasteButton.setFocusTraversable(false);

		Button refreshButton = new Button("", JavaFXUtils.createIcon("/icons/refresh.png"));
		refreshButton.setTooltip(new Tooltip("Refresh"));
		refreshButton.setOnAction(event -> {
			editBox.refresh();
		});
		refreshButton.setFocusTraversable(false);

		FlowPane sideBar = new FlowPane(Orientation.VERTICAL, copyButton, pasteButton, refreshButton);

		switch (toolBarOrientation) {
		case VERTICAL:
			editBox.setBarBottom(sideBar);
			break;
		case HORIZONTAL:
			editBox.setBarLeft(sideBar);
			break;

		default:
			break;
		}

		return editBox;
	}

	protected TabPane createRecordsTabPane() {
		TabPane recordsTabPane = new TabPane();
		DraggingTabPaneSupport draggingSupport = new DraggingTabPaneSupport();
		draggingSupport.addSupport(recordsTabPane);
		this.createRecordsAddTab(recordsTabPane);
		return recordsTabPane;
	}

	protected void createRecordsAddTab(TabPane recordsTabPane) {
		if (recordsTabPane == null)
			return;
	
		addRecordTab = new Tab("Add");
		addRecordTab.setGraphic(JavaFXUtils.createIcon("/icons/add.png"));
		addRecordTab.setClosable(false);
	
		SqlTableRowEditBox editBox = createEditBox(null, true);
	
		Button addBtn = new Button("Add", JavaFXUtils.createIcon("/icons/check.png"));
		addBtn.setTooltip(new Tooltip("Add"));
		Button clearBtn = new Button("", JavaFXUtils.createIcon("/icons/clear.png"));
		clearBtn.setTooltip(new Tooltip("Clear"));
		clearBtn.setOnAction(event -> editBox.clear());
		addBtn.setOnMouseClicked(event2 -> this.insertRecordToSqlTableViewRef(editBox));
		addBtn.setOnKeyPressed(keyEvent -> {
			if (keyEvent.getCode() == KeyCode.ENTER) {
				this.insertRecordToSqlTableViewRef(editBox);
			}
		});
	
		editBox.getToolbar().getChildren().addAll(clearBtn);
		editBox.getMainBox().getChildren().add(addBtn);
	
		for (Node node : editBox.getChildren()) {
			if (node instanceof HBox)
				((HBox) node).prefWidthProperty().bind(editBox.widthProperty());
		}
	
		addRecordTab.setContent(editBox);
		recordsTabPane.getTabs().add(0, addRecordTab);
	}

	public boolean isFullMode() {
		return fullModeCheckBox.isSelected();
	}

	public boolean isLimitSet() {
		return limitModeCheckBox.isSelected();
	}

	protected void getDataFromDB(String table, final SqlTableView sqlTableView) {
		SqlTableTab sqlTableTab = (SqlTableTab)tablesTabPane.getSelectionModel().getSelectedItem();
		sqlQueryRunning = true;
		String query = "select " + columnsFilter + " from " + table + whereFilter;

		if (this.isLimitSet()) {
			query += " limit " + linesLimit;
		}

		String message = "Executing : " + query;
		logger.debug(message);
		if (uiLogging)
			Platform.runLater(() -> logListView.getItems().add(message));
		try {
			sqlConnector.executeQueryRawSafely(query, resultSet -> {
				sqlTableView.setItemsLater(resultSet);
				sqlTableView.setTableName(table);
			});

		} catch (SQLException e) {
			DialogFactory.createErrorDialog(e);
		} finally {
			this.updateRowsCountLabel();
			this.enableFullMode(sqlTableTab);
		}
	}

	public void updateRowsCountLabel() {
		Platform.runLater(() -> this.rowsCountLabel.setText(getSelectedSqlTableView().getSqlTableRows().size() + " rows"));
		
	}

	public void enableFullMode(final SqlTableTab sqlTableTab) {
		Platform.runLater(() -> {
			if (isFullMode()) {
				final TabPane recordsTabPane = sqlTableTab.getRecordsTabPane() != null ?
						sqlTableTab.getRecordsTabPane() :
												this.createRecordsTabPane();
													
				SplitPane fullModeSplitPane = new SplitPane(sqlTableTab.getSqlTableView(), recordsTabPane);
				fullModeSplitPane.setDividerPositions(0.7, 0.3);
				fullModeSplitPane.setOrientation(Orientation.HORIZONTAL);
				sqlTableTab.setContent(fullModeSplitPane);
				sqlTableTab.setRecordsTabPane(recordsTabPane);
			} 
//			else
//				guiState.getTableTab().setContent(guiState.getSqlTableView());

			sqlQueryRunning = false;
		});
	}

	public void disableFullMode() {
		tablesTabPane.getSelectionModel().getSelectedItem().setContent(getSelectedSqlTableView());
	}

	public void fillColumnCheckBoxes(final SqlTableView sqlTableView) {
//		Platform.runLater(() -> {
			columnCheckBoxesMap.clear();
			for (String column : sqlTableView.getSqlTable().getColumns()) {
				CheckBox columnCheckBox = new CheckBox(column);
				columnCheckBox.setSelected(false);
				columnCheckBoxesMap.put(column, columnCheckBox);
			}
			for (TableColumn<MapTableViewRow, ?> column : sqlTableView.getVisibleLeafColumns()) {
				columnCheckBoxesMap.get(column.getText()).setSelected(true);
			}
			sqlTableView.bindColumsVisibility(columnCheckBoxesMap.values());
//		});
	}

	private void searchFieldAction() {
		getSelectedSqlTableView().getSelectionModel().clearSelection();
		// use executor service of sqlConnector
		sqlConnector.executeAsync(() -> {
			Platform.runLater(() -> getSelectedSqlTableView().setItems(getSelectedSqlTableView().getSqlTableRows()));
			ObservableList<MapTableViewRow> searchRows = FXCollections.observableArrayList();

			String[] split = searchField.getText().split(":");
			String columnRegex = split.length > 1 ? split[0] : null;
			String regex = split.length > 1 ? split[1] : split[0];
			
			for (MapTableViewRow row : getSelectedSqlTableView().getSqlTableRows()) {
				for (TableColumn<MapTableViewRow, ?> column : getSelectedSqlTableView().getVisibleLeafColumns()) {

					if (columnRegex != null && column.getText().equals(columnRegex) && row.get(column.getText()) != null) {
						if (row.get(column.getText()).toString().matches("(?i:.*" + regex + ".*)")) {
							searchRows.add(new MapTableViewRow(row));
							break;
						}
					}
					else if (columnRegex == null && row.get(column.getText()) != null) {
						if (row.get(column.getText()).toString().matches("(?i:.*" + regex + ".*)")) {
							searchRows.add(new MapTableViewRow(row));
							break;
						}
					}
				}
				Platform.runLater(() -> getSelectedSqlTableView().setItems(searchRows));
			}
		});
	}

	// Buttons' actions -------------------------------------------------------
	protected void tableComboBoxAction(ComboBox<String> comboBox) {
		if (sqlQueryRunning) {
			return;
		} else {
			sqlConnector.executeAsync(() -> this.getDataFromDB(comboBox.getSelectionModel().getSelectedItem(), getSelectedSqlTableView()));
		}
	}

	protected void tablesTabPaneClickAction() {
		if (tablesTabPane.getSelectionModel().getSelectedItem() == addTableTab)
			this.addSqlTableTab();
		
		this.updateRowsCountLabel();
	}

	private void fullModeAction() {
		MapTableViewRow sqlTableRow = getSelectedSqlTableView().getSelectionModel().getSelectedItem();
		if (sqlTableRow == null)
			return;

		if (getSelectedRecordsTabPane() == null) {
			SqlTableTab selectedTab = getSelectedTableTab();
			this.enableFullMode(selectedTab);
			return;
		}

		ObservableList<Tab> tabs = getSelectedRecordsTabPane().getTabs();
		for (Tab tab : tabs) {
			String tabTitle = sqlTableRow.get(getSelectedSqlTableView().getPrimaryKey()) != null
					? sqlTableRow.get(getSelectedSqlTableView().getPrimaryKey()).toString()
					: null;
			if (tabTitle != null) {
				if (((Label) tab.getGraphic()).getText().equals(tabTitle)) {
					getSelectedRecordsTabPane().getSelectionModel().select(tab);
					// record already opened
					return;
				}
			}
		}

		SqlTableRowEditBox editBox = this.createEditBox(sqlTableRow, true);

		sqlTableRow.addObserver(editBox);

		Button editButton = new Button("Edit", JavaFXUtils.createIcon("/icons/check.png"));
		editButton.setTooltip(new Tooltip("Edit"));
		editButton.setOnAction(event -> this.updateRecordOfSqlTableViewRef(editBox, sqlTableRow));
		editButton.setOnKeyPressed(keyEvent -> {
			if (keyEvent.getCode() == KeyCode.ENTER) {
				editButton.getOnAction().handle(new ActionEvent());
			}
		});

		editBox.getMainBox().getChildren().addAll(editButton);

		for (Node node : editBox.getChildren()) {
			if (node instanceof HBox)
				((HBox) node).prefWidthProperty().bind(editBox.widthProperty());
		}

		String tabTitle = sqlTableRow.get(getSelectedSqlTableView().getPrimaryKey()) != null
				? sqlTableRow.get(getSelectedSqlTableView().getPrimaryKey()).toString()
				: "record";

		Tab editTab = new Tab(tabTitle);
		editTab.setGraphic(JavaFXUtils.createIcon("/icons/edit.png"));
		editTab.setContent(editBox);
		editTab.setOnCloseRequest(closeEvent -> sqlTableRow.removeObserver(editBox));

		getSelectedRecordsTabPane().getTabs().add(editTab);
		getSelectedRecordsTabPane().getSelectionModel().select(editTab);
		getSelectedRecordsTabPane().setOnKeyPressed(keyEvent -> {
			if (editTab.isSelected()) {
				if (keyEvent.isControlDown() && keyEvent.getCode() == KeyCode.HOME) {
					editBox.getScrollPane().setVvalue(0);
				} else if (keyEvent.isControlDown() && keyEvent.getCode() == KeyCode.END) {
					editBox.getScrollPane().setVvalue(editBox.getScrollPane().getHeight());
				}
			}
		});
	}

	protected void sqlConsoleButtonAction() {
		Scene scene = new Scene(new SqlConsolePane(sqlConnector), 400, 300);
		scene.getStylesheets().addAll(this.getScene().getStylesheets());
		Stage newStage = new Stage();
		newStage.setScene(scene);
		newStage.show();
	}
	
	protected void addButtonAction() {
		if (addButton.isFocused() && popOver.isShowing())
			return;

		addButton.requestFocus();

		if (getSelectedSqlTableView().getColumns().size() == 0)
			return;

		SqlTableRowEditBox editBox = this.createEditBox(null, false);

		Button addBtn = new Button("Add", JavaFXUtils.createIcon("/icons/check.png"));
		addBtn.setTooltip(new Tooltip("Add"));
		editBox.getMainBox().getChildren().add(addBtn);

		popOver = new PopOver(editBox);
		popOver.setHeight(editBox.getMainBox().getHeight());

		addBtn.setOnAction(submitEvent -> this.insertRecordToSqlTableViewRef(editBox));
		addBtn.setOnKeyPressed(keyEvent -> {
			if (keyEvent.getCode() == KeyCode.ENTER) {
				addBtn.getOnAction().handle(new ActionEvent());
			}
		});

		popOver.setDetachable(false);
		popOver.show(addButton);
		addBtn.requestFocus();
	}

	protected void editButtonAction(MouseEvent event) {
		if (!getSelectedSqlTableView().isFocused())
			editButton.requestFocus();

		if (editButton.isFocused() && popOver.isShowing())
			return;

		MapTableViewRow sqlTableRow = getSelectedSqlTableView().getSelectionModel().getSelectedItem();
		if (sqlTableRow == null)
			return;

		SqlTableRowEditBox editBox = this.createEditBox(sqlTableRow, false);
		sqlTableRow.addObserver(editBox);

		popOver = new PopOver(editBox);

		if (getSelectedSqlTableView().getPrimaryKey() != null) {
			Button editBtn = new Button("Edit", JavaFXUtils.createIcon("/icons/check.png"));
			editBtn.setTooltip(new Tooltip("Edit"));
			editBtn.setOnAction(submitEvent -> this.updateRecordOfSqlTableViewRef(editBox, sqlTableRow));
			editBtn.setOnKeyPressed(keyEvent -> {
				if (keyEvent.getCode() == KeyCode.ENTER) {
					editBtn.getOnAction().handle(new ActionEvent());
				}
			});

			editBox.getMainBox().getChildren().add(editBtn);
		}

		popOver.setDetachable(false);
		// remove listener on close
		popOver.setOnHidden(windowEvent -> sqlTableRow.removeObserver(editBox));
		popOver.show(editButton, event.getScreenX(), event.getScreenY());
		editBox.setOnKeyPressed(keyEvent -> {
			if (keyEvent.getCode() == KeyCode.ESCAPE) {
				popOver.hide();
			}
		});
	}

	protected void deleteButtonAction() {
		ObservableList<MapTableViewRow> sqlTableRows = getSelectedSqlTableView().getSelectionModel().getSelectedItems();

		if (sqlTableRows.size() == 0)
			return;

		if (DialogFactory.createDeleteDialog(getSelectedSqlTableView(), sqlTableRows, "Do you want to delete records?") == 0)
			return;

		List<MapTableViewRow> toRemove = new ArrayList<>();
		sqlConnector.executeAsync(() -> {
			for (MapTableViewRow sqlTableRow : sqlTableRows) {
				if (this.deleteRecord(sqlTableRow) == 1)
					toRemove.add(sqlTableRow);
			}
			getSelectedSqlTableView().getSelectionModel().clearSelection();
			getSelectedSqlTableView().getSqlTableRows().removeAll(toRemove);
			this.updateRowsCountLabel();
		});
	}

	private void compareAction(MouseEvent mouseEvent) {
		if (getSelectedSqlTableView().getSelectionModel().getSelectedItems().size() > 8) {
			DialogFactory.createErrorDialog(new Exception("Too much elements to compare!"));
			return;
		}

		VBox compareBox = new VBox();
		HBox compareRowBox = null;
		int cells = 2;
		for (MapTableViewRow row : getSelectedSqlTableView().getSelectionModel().getSelectedItems()) {
			if (cells == 2) {
				compareRowBox = new HBox();
				// compareRowBox.prefWidthProperty().bind(compareBox.widthProperty());
				compareBox.getChildren().add(compareRowBox);
			}
			SqlTableRowEditBox editBox = createEditBox(row, true);

			row.addObserver(editBox);
			Button editButton = new Button("Edit", JavaFXUtils.createIcon("/icons/check.png"));
			editButton.setTooltip(new Tooltip("Edit"));
			editButton.setOnAction(event -> this.updateRecordOfSqlTableViewRef(editBox, row));
			editBox.getMainBox().getChildren().add(editButton);
			editBox.prefWidthProperty().bind(compareBox.widthProperty().divide(2));
			compareRowBox.getChildren().add(editBox);

			cells--;
			if (cells == 0)
				cells = 2;

			editBox.setOnClose(() -> row.removeObserver(editBox));
		}

		if (isFullMode()) {
			Tab compareTab = new Tab("Compare");
			compareTab.setGraphic(JavaFXUtils.createIcon("/icons/compare.png"));
			compareTab.setContent(compareBox);
			compareTab.setOnCloseRequest(closeEvent -> {
				for (Node node : compareBox.getChildren()) {
					HBox hbox = (HBox) node;
					for (Node editBox : hbox.getChildren())
						((SqlTableRowEditBox) editBox).close();
				}
			});
			getSelectedTableTab().getRecordsTabPane().getTabs().add(compareTab);
			getSelectedTableTab().getRecordsTabPane().getSelectionModel().select(compareTab);
		} else {
			compareBox.setPrefWidth(800);
			popOver = new PopOver(compareBox);
			popOver.setDetachable(false);
			popOver.setOnHidden(closeEvent -> {
				for (Node node : compareBox.getChildren()) {
					HBox hbox = (HBox) node;
					for (Node editBox : hbox.getChildren())
						((SqlTableRowEditBox) editBox).close();
				}
			});
			popOver.show(editButton, mouseEvent.getScreenX(), mouseEvent.getScreenY());
		}
	}

	protected void tableSelectButtonAction() {
		if (tableSelectButton.isFocused() && popOver.isShowing())
			return;

		tableSelectButton.requestFocus();
		// invoke createTableBox(), createViewsBox every time button clicked to has the latset updates from db
		ComboBox<String> tablesBox = this.createTablesBox();
		ComboBox<String> viewsBox = this.createViewsBox();
		viewsBox.prefWidthProperty().bind(tablesBox.widthProperty());
		
		popOver = new PopOver(new VBox(new Label("Select"), tablesBox, viewsBox));
		popOver.setDetachable(false);
		popOver.show(tableSelectButton);
	}

	protected void refreshButtonAction() {
		//TODO rerun original query in case an sqlcodearea is involved
		refreshButton.requestFocus();
//		if (tablesBox != null && tablesBox.getSelectionModel().getSelectedItem() != null && !sqlQueryRunning.get()) {
//			tablesBox.getOnAction().handle(new ActionEvent());
//		}
		if (sqlQueryRunning) {
			return;
		} else {
			if (tablesTabPane.getSelectionModel().getSelectedItem() != null
					&& !((SqlTableTab)tablesTabPane.getSelectionModel().getSelectedItem()).getCustomText().equals(EMPTY)) {
				String tableName = ((SqlTableTab)tablesTabPane.getSelectionModel().getSelectedItem()).getCustomText();
				sqlConnector.executeAsync(() -> this.getDataFromDB(tableName, getSelectedSqlTableView()));
			}
		}
	}

	protected void settingsButtonAction() {
		if (settingsButton.isFocused() && popOver.isShowing())
			return;

		settingsButton.requestFocus();
		popOver = new PopOver(new VBox(resizeModeCheckBox, fullModeCheckBox, limitModeCheckBox));
		popOver.setDetachable(false);
		popOver.show(settingsButton);
	}

	protected void searchButtonAction() {
		if (searchButton.isFocused() && popOver.isShowing())
			return;

		searchButton.requestFocus();
		VBox searchBox = new VBox(new Label("Type and press enter"), searchField);
		searchBox.setOnKeyPressed(keyEvent -> {
			if (keyEvent.getCode() == KeyCode.ESCAPE) {
				popOver.hide();
			}
		});
		popOver = new PopOver(searchBox);
		popOver.setDetachable(false);
//		popOver.setOnHidden(closeEvent -> sqlTableView.setItems(sqlTableView.getSqlTableRows()));
		popOver.show(searchButton);
	}

	protected void columnsSettingsButtonAction() {
		if (columnsSettingsButton.isFocused() && popOver.isShowing())
			return;

		columnsSettingsButton.requestFocus();

		if (getSelectedSqlTableView().getSqlTable() != null) {
			this.fillColumnCheckBoxes(getSelectedSqlTableView());
			VBox vBox = new VBox();
			for (CheckBox checkBox : columnCheckBoxesMap.values()) {
				vBox.getChildren().add(checkBox);
			}
			popOver = new PopOver();
			popOver.setDetachable(false);
			popOver.setContentNode(vBox);
			popOver.show(columnsSettingsButton);
		}
	}

	protected void importCsvAction() {
		if (getSelectedSqlTableView() != null && !getSelectedSqlTableView().titleProperty().get().isEmpty()) {
			FileChooser fileChooser = new FileChooser();
			File selectedFile = fileChooser.showOpenDialog(null);
			if (selectedFile != null) {
				String filePath = selectedFile.getAbsolutePath();

				String[] columns = null;
				ObservableList<MapTableViewRow> rows = FXCollections.observableArrayList();
				try (Scanner scanner = new Scanner(new File(filePath))) {
					while (scanner.hasNext()) {
						String line = scanner.nextLine();
						if (columns == null) {
							columns = line.split(",");
						} else {
							String[] values = line.split(",");
							HashMap<String, Object> map = new HashMap<>();
							for (int i = 0; i < columns.length; i++) {
								Object tempValue = (values.length-1 >= i) ? values[i] : null;
									
								map.put(columns[i], tempValue);
							}
							try {
								getSelectedSqlTableView().insertRecord(map);
								rows.add(new MapTableViewRow(map));
							} catch (Exception e) {
								logger.error(e.getMessage(), e);
							}

						}
					}
				} catch (IOException e) {
					DialogFactory.createErrorDialog(e);
				} finally {
					Platform.runLater(() -> {
						getSelectedSqlTableView().getSqlTableRows().addAll(rows);
					});
				}

			}
		}
	}

	protected void exportCsvAction() {
		if (exportCsvButton.isFocused() && popOver.isShowing())
			return;

		exportCsvButton.requestFocus();
		Button startButton = new Button("Export", JavaFXUtils.createIcon("/icons/csv.png"));
		Button dirButton = new Button("Search", JavaFXUtils.createIcon("/icons/magnify.png"));

		dirButton.setOnAction(actionEvent -> {
			DirectoryChooser directoryChooser = new DirectoryChooser();
			File selectedFile = directoryChooser.showDialog(null);
			if (selectedFile != null) {
				String dirPath = selectedFile.getAbsolutePath();
				pathField.setText(dirPath + "/" + getSelectedSqlTableView().titleProperty().get() + ".csv");
			}
			exportCsvButton.getOnAction().handle(new ActionEvent());

		});

		startButton.setOnAction(actionEvent -> {
			if (popOver != null)
				popOver.hide();

			String filePath = pathField.getText();

			if (filePath != null && !filePath.isEmpty()) {
				if (filePath != null) {
					Executor executor = Executors.newSingleThreadExecutor();
					executor.execute(() -> {
						try {
							Files.write(Paths.get(filePath),
									(getSelectedSqlTableView().getSqlTable().columnsToString() + "\n").getBytes(),
									StandardOpenOption.CREATE, StandardOpenOption.APPEND);

							getSelectedSqlTableView().getItems().forEach(row -> {
								try {
									Files.write(Paths.get(filePath), (row.toString() + "\n").getBytes(),
											StandardOpenOption.APPEND);
								} catch (IOException e) {
									logger.error(e.getMessage(), e);
									;
								}
							});
							DialogFactory.createInfoDialog("CSV Export",
									"Export to csv has been completed\n" + pathField.getText());
						} catch (IOException e) {
							logger.error(e.getMessage(), e);
							DialogFactory.createErrorDialog(e);
						}
					});
				}
			}
		});

		popOver = new PopOver(new VBox(pathField, new HBox(dirButton, startButton)));
		popOver.setDetachable(false);
		popOver.show(exportCsvButton);
	}

	protected void copyAction() {
		StringBuilder content = new StringBuilder();

		getSelectedSqlTableView().getSelectionModel().getSelectedItems().forEach(row -> content.append(row.toString() + "\n"));

		StringSelection stringSelection = new StringSelection(content.toString());
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(stringSelection, null);
	}

	protected void pasteAction(SqlTableRowEditBox editBox) {
		try {
			String data = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);

			String[] split = data.split(",");
			for (int i = 0; i < getSelectedSqlTableView().getColumnsNames().size(); i++) {
				String column = getSelectedSqlTableView().getColumnsNames().get(i);
				editBox.put(column, split[i]);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	//TODO replace with method deleteRecord of SqlTableView
	public int deleteRecord(MapTableViewRow sqlTableRow) {
		String query = "delete from " + getSelectedSqlTableView().getTableName() + " where ";
		List<Object> params = new ArrayList<>();
		Set<String> columns = getSelectedSqlTableView().getSqlTable().getColumns();
		if (getSelectedSqlTableView().getPrimaryKey() != null) {
			params.add(sqlTableRow.get(getSelectedSqlTableView().getPrimaryKey()));
			query += getSelectedSqlTableView().getPrimaryKey() + "= ?";
		} else {
			for (String column : columns) {
				params.add(sqlTableRow.get(column));
				query += column + "= ? and ";
			}
			query = query.substring(0, query.length() - 5);
		}

		String message = "Executing : " + query + " [ values : " + params.toString() + " ]";
		logger.debug(message);
		if (uiLogging)
			Platform.runLater(() -> logListView.getItems().add(message));

		try {
			sqlConnector.executeUpdate(query, params);
		} catch (Exception e) {
			DialogFactory.createErrorDialog(e);
			return 0;
		}

		return 1;
	}

	public void insertRecordToSqlTableViewRef(SqlTableRowEditBox editBox) {
		sqlConnector.executeAsync(() -> {
			try {
				getSelectedSqlTableView().insertRecord(editBox);
				this.updateRowsCountLabel();
				DialogFactory.createInfoDialog("Record insert", "Succesfully inserted! \n\n" + editBox.toString());
			} catch (Throwable e) {
				DialogFactory.createErrorDialog(e);
			}
		});

	}

	public void updateRecordOfSqlTableViewRef(SqlTableRowEditBox editBox, MapTableViewRow sqlTableRow) {
		sqlConnector.executeAsync(() -> {
			try {
				getSelectedSqlTableView().updateRecord(editBox, sqlTableRow);
				DialogFactory.createInfoDialog("Record insert", "Succesfully updated! \n\n" + editBox.toString());
			} catch (SQLException e) {
				DialogFactory.createErrorDialog(e);
			}
		});
	}

	protected MouseEvent simulateClickEvent() {
		return new MouseEvent(MouseEvent.MOUSE_CLICKED, 0, 0, getSelectedSqlTableView().getContextMenu().getX(), getSelectedSqlTableView().getContextMenu().getY(),
				MouseButton.PRIMARY, 1, false, false, false, false, false, false, false, false, false, false, null);
		// new MouseEvent(source, target, eventType, x, y, screenX, screenY, button,
		// clickCount, shiftDown, controlDown, altDown, metaDown, primaryButtonDown,
		// middleButtonDown, secondaryButtonDown, synthesized, popupTrigger,
		// stillSincePress, pickResult)
	}

	protected MouseEvent simulateClickEvent(Node node) {
		Bounds boundsInScene = node.localToScreen(node.getBoundsInLocal());
		double x = (boundsInScene.getMaxX() + boundsInScene.getMinX()) / 2;
		double y = (boundsInScene.getMaxY() + boundsInScene.getMinY()) / 2;

		return new MouseEvent(MouseEvent.MOUSE_CLICKED, 0, 0, x, y, MouseButton.PRIMARY, 1, true, true, true, true,
				true, true, true, true, true, true, null);
	}

	@SuppressWarnings("unused")
	private void sortSqlTableView(SqlTableView sqlTableView) {
		sqlTableView.getSqlTableRows().sort((o1, o2) -> {
			if (o1.get(sqlTableView.getPrimaryKey()) != null && o2.get(sqlTableView.getPrimaryKey()) != null) {
				if (o1.get(sqlTableView.getPrimaryKey()).toString()
						.compareTo(o2.get(sqlTableView.getPrimaryKey()).toString()) > 0) {

					return 1;
				}
			}
			return 0;
		});
	}

	public final SqlTableView getSelectedSqlTableView() {
		Tab tab = tablesTabPane != null ? tablesTabPane.getSelectionModel().getSelectedItem() : null;
		return tab != null && tab instanceof SqlTableTab ? ((SqlTableTab)tab).getSqlTableView() : null;
	}
	
	public final TabPane getSelectedRecordsTabPane() {
		return getSelectedTableTab().getRecordsTabPane();
	}

	public FlowPane getToolBar() {
		return toolBar;
	}

	public void setToolBar(FlowPane toolBar) {
		this.toolBar = toolBar;
	}

	public void setToolBarTop() {
		this.setLeft(null);
		toolBar.orientationProperty().set(Orientation.HORIZONTAL);
		this.setTop(toolBar);

	}

	public void setToolBarLeft() {
		this.setTop(null);
		toolBar.orientationProperty().set(Orientation.VERTICAL);
		this.setLeft(toolBar);
	}

	public void setSqlConnector(SqlConnector connector) {
		this.sqlConnector = connector;
		getSelectedSqlTableView().setSqlConnector(connector);
	}

	public SqlConnector getSqlConnector() {
		return sqlConnector;
	}

	public PopOver getPopOver() {
		return popOver;
	}

	public void setPopOver(PopOver popOver) {
		this.popOver = popOver;
	}

	public ComboBox<String> getTablesBox() {
		return tablesBox;
	}

	public TabPane getTablesTabPane() {
		return tablesTabPane;
	}

	public final SqlTableTab getSelectedTableTab() {
		return (SqlTableTab) tablesTabPane.getSelectionModel().getSelectedItem();
	}

}
