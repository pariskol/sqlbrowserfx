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
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.Executors;

import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.PopOver;
import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gr.sqlbrowserfx.LoggerConf;
import gr.sqlbrowserfx.conn.SqlConnector;
import gr.sqlbrowserfx.factories.DialogFactory;
import gr.sqlbrowserfx.listeners.TableColumnFilteringEvent;
import gr.sqlbrowserfx.listeners.TableSearchFilteringEvent;
import gr.sqlbrowserfx.nodes.ContextMenuOwner;
import gr.sqlbrowserfx.nodes.InputMapOwner;
import gr.sqlbrowserfx.nodes.SqlConsolePane;
import gr.sqlbrowserfx.nodes.ToolbarOwner;
import gr.sqlbrowserfx.nodes.tableviews.MapTableViewRow;
import gr.sqlbrowserfx.nodes.tableviews.SqlTableView;
import gr.sqlbrowserfx.nodes.tableviews.filter.SqlTableFilter;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import gr.sqlbrowserfx.utils.MemoryGuard;
import gr.sqlbrowserfx.utils.PropertiesLoader;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class SqlPane extends BorderPane implements ToolbarOwner, ContextMenuOwner, InputMapOwner {

	private static final int MAX_ENTRY_POP_OVER_HEIGHT = 800;
	protected FlowPane toolBar;
	protected Button addButton;
	protected Button editButton;
	protected Button deleteButton;
	protected Button settingsButton;
	protected Button columnsSettingsButton;
	protected Button searchButton;
	protected Button refreshButton;
	protected Button tableSelectButton;
	protected Button sqlConsoleButton;
	protected TextField searchField;
	protected CheckBox resizeModeCheckBox;
	protected CheckBox fullModeCheckBox;
	private final CheckBox limitModeCheckBox;
	protected ComboBox<String> tablesBox;
	protected ComboBox<String> viewsBox;
	private final TextField pathField;
	private Button exportCsvButton;
	private final LinkedHashMap<String, CheckBox> columnCheckBoxesMap;
	protected PopOver popOver;
	private Tab addRecordTab;
	private final Tab addTableTab;
	private final Label rowsCountLabel;
	protected TabPane tablesTabPane;

	protected SqlConnector sqlConnector;
	protected Boolean sqlQueryRunning;
	private Boolean isSearchApplied = false;
	private final String columnsFilter = "*";
	private Button importCsvButton;
	protected Logger logger = LoggerFactory.getLogger(LoggerConf.LOGGER_NAME);

	private final String EMPTY = "empty";
	private final String whereFilter = "";
	private final int linesLimit = 5000;
	private boolean isColumnFilteringEnabled = true;

	public SqlPane() {
		this(null);
	}

	public SqlPane(SqlConnector sqlConnector) {
		columnCheckBoxesMap = new LinkedHashMap<>();
		this.sqlConnector = sqlConnector;

		sqlQueryRunning = false;

		popOver = new CustomPopOver();
		pathField = new TextField();

		toolBar = this.createToolbar();
		toolBar.setOrientation(Orientation.VERTICAL);

		tablesTabPane = new TabPane();

		addTableTab = new Tab("");
		addTableTab.setGraphic(JavaFXUtils.createIcon("/icons/add.png"));
		addTableTab.setClosable(false);
		tablesTabPane.getTabs().add(addTableTab);

		tablesTabPane.setOnMouseClicked(mouseEvent -> this.tablesTabPaneClickAction());

		resizeModeCheckBox = new CheckBox("Auto resize");
		fullModeCheckBox = new CheckBox("Full mode");
		fullModeCheckBox.setOnMouseClicked(moueEvent -> {
			SqlTableTab sqlTableTab = getSelectedTableTab();
			if (isInFullMode() && (sqlTableTab != null && !sqlTableTab.getCustomText().equals(EMPTY))) {
				this.openInFullMode(sqlTableTab);
			} else {
				this.disableFullMode();
			}

		});

		limitModeCheckBox = new CheckBox("Lines limit " + linesLimit);
		rowsCountLabel = new Label("0 rows");

		this.setLeft(toolBar);
		this.setCenter(tablesTabPane);
		this.setBottom(rowsCountLabel);

		DraggingTabPaneSupport draggingSupport = new DraggingTabPaneSupport("/icons/table.png");
		draggingSupport.addSupport(this);

		this.setInputMap();

		this.addEventHandler(TableColumnFilteringEvent.EVENT_TYPE, event -> {
			SqlTableView sqlTableView = getSelectedSqlTableView();
            assert sqlTableView != null;
            int diff = sqlTableView.getSqlTableRows().size() - sqlTableView.getItems().size();
			setSearchApplied(diff > 0);
			this.updateRowsCountLabel();
		});

		this.addEventHandler(TableSearchFilteringEvent.EVENT_TYPE, event -> Platform.runLater(() -> SqlTableFilter.apply(getSelectedSqlTableView())));
	}

	@Override
	public FlowPane createToolbar() {
		addButton = new Button("", JavaFXUtils.createIcon("/icons/add.png"));
		addButton.setOnMouseClicked(event -> addButtonAction());
		addButton.setOnAction(event -> addButtonAction());
		addButton.setTooltip(new Tooltip("Insert record"));

		deleteButton = new Button("", JavaFXUtils.createIcon("/icons/minus.png"));
		deleteButton.setOnMouseClicked(event -> deleteButtonAction());
		deleteButton.setOnAction(event -> deleteButtonAction());
		deleteButton.setTooltip(new Tooltip("Delete selected records"));

		editButton = new Button("", JavaFXUtils.createIcon("/icons/edit.png"));
		editButton.setOnMouseClicked(this::editButtonAction);
		editButton.setOnAction(mouseEvent -> editButtonAction(this.simulateClickEvent(editButton)));
		editButton.setTooltip(new Tooltip("Edit selected record"));

		searchField = new TextField();
		searchField.setPromptText("Search...");
		searchField.setOnKeyPressed(keyEvent -> {
			if (keyEvent.getCode() == KeyCode.ENTER) {
				if (searchField.getText().isEmpty()) {
					Platform.runLater(() -> {
						getSelectedSqlTableView().setItems(getSelectedSqlTableView().getSqlTableRows());
						this.fireEvent(new TableSearchFilteringEvent());
						this.setSearchApplied(false);
						this.updateRowsCountLabel();
					});
				} else {
					this.searchFieldAction();
				}
			}
		});

		settingsButton = new Button("", JavaFXUtils.createIcon("/icons/settings.png"));
		settingsButton.setOnMouseClicked(event -> this.settingsButtonAction());
		settingsButton.setOnAction(event -> this.settingsButtonAction());
		settingsButton.setTooltip(new Tooltip("Adjust settings"));

		searchButton = new Button("", JavaFXUtils.createIcon("/icons/magnify.png"));
		searchButton.setOnAction(actionEvent -> this.searchButtonAction());
		searchButton.setOnMouseClicked(mouseEvent -> this.searchButtonAction());
		searchButton.setTooltip(new Tooltip("Search in table"));

		importCsvButton = new Button("", JavaFXUtils.createIcon("/icons/csv-import.png"));
		importCsvButton.setOnAction(actionEvent -> this.importCsvAction());
		importCsvButton.setOnMouseClicked(mouseEvent -> this.importCsvAction());
		importCsvButton.setTooltip(new Tooltip("Import from csv"));

		exportCsvButton = new Button("", JavaFXUtils.createIcon("/icons/csv.png"));
		exportCsvButton.setOnAction(actionEvent -> this.exportCsvAction());
		exportCsvButton.setOnMouseClicked(mouseEvent -> this.exportCsvAction());
		exportCsvButton.setTooltip(new Tooltip("Export to csv"));

		sqlConsoleButton = new Button("", JavaFXUtils.createIcon("/icons/console.png"));
		sqlConsoleButton.setOnMouseClicked(mouseEvent -> this.sqlConsoleButtonAction());
		sqlConsoleButton.setTooltip(new Tooltip("Open sql code area"));
		// FIXME maybe uncomment this
//		sqlConsoleButton.setOnAction(mouseEvent -> this.sqlConsoleButtonAction());

		if (sqlConnector != null) {
			refreshButton = new Button("", JavaFXUtils.createIcon("/icons/refresh.png"));
			refreshButton.setOnMouseClicked(event -> refreshButtonAction());
			refreshButton.setTooltip(new Tooltip("Refresh"));

			tableSelectButton = new Button("", JavaFXUtils.createIcon("/icons/database.png"));
			tableSelectButton.setOnMouseClicked(event -> this.tableSelectButtonAction());
			tableSelectButton.setOnAction(event -> this.tableSelectButtonAction());
			tableSelectButton.setTooltip(new Tooltip("Select table/view"));

			//FIXME something goes wrong if both action and mouse handlers set
//			refreshButton.setOnAction(event -> refreshButtonAction());
			columnsSettingsButton = new Button("", JavaFXUtils.createIcon("/icons/table-settings.png"));
			columnsSettingsButton.setOnMouseClicked(mouseEvent -> this.columnsSettingsButtonAction());
			columnsSettingsButton.setTooltip(new Tooltip("Select visible columns"));

			return new FlowPane(searchButton, tableSelectButton, columnsSettingsButton, settingsButton, refreshButton,
					addButton, editButton, deleteButton, importCsvButton, exportCsvButton, sqlConsoleButton);
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

		if (tablesBox == null || !Objects.equals(tablesList, tablesBox.getItems())) {
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

		if (viewsBox == null || !Objects.equals(tablesList, viewsBox.getItems())) {
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
		SqlTableView sqlTableView = new SqlTableView();
		sqlTableView.autoResizeProperty().bind(resizeModeCheckBox.selectedProperty());
		sqlTableView.enableColumnFiltering(isColumnFilteringEnabled);
		sqlTableView.setColumnWidth(0, 0, 500);
		sqlTableView.setSqlConnector(sqlConnector);
		sqlTableView.setOnMouseClicked(mouseEvent -> {
			sqlTableView.requestFocus();
			if (mouseEvent.getClickCount() == 2) {
				SqlTableView tableView = getSelectedSqlTableView();
				if (tableView.getSelectionModel().getSelectedItem() != null) {
					Boolean areCellEditable = PropertiesLoader.getProperty("sqlbrowserfx.default.editmode.cell", Boolean.class,
							false);
					if (areCellEditable) {
						tableView.getSelectedCell().startEdit();
					} else if (isInFullMode()) {
						this.editButtonActionFullMode();
					} else {
						this.editButtonAction(mouseEvent);
					}
					tableView.requestFocus();
				}
			}
		});
		sqlTableView.setContextMenu(this.createContextMenu());
		SqlTableTab tab = new SqlTableTab(EMPTY, sqlTableView);
		sqlTableView.setParent(tab);
		tab.customTextProperty().addListener((observable, oldValue, newValue) -> determineTabIcon(tab));

		return tab;
	}

	@Override
	public void setInputMap() {
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.F, KeyCombination.CONTROL_DOWN),
				action -> this.searchButtonAction()));
		Nodes.addInputMap(this,
				InputMap.consume(EventPattern.keyPressed(KeyCode.C, KeyCombination.CONTROL_DOWN), action -> {
					this.copyAction();
					getSelectedSqlTableView().requestFocus();
				}));
		Nodes.addInputMap(this,
				InputMap.consume(EventPattern.keyPressed(KeyCode.D, KeyCombination.CONTROL_DOWN), action -> {
					this.deleteButtonAction();
					getSelectedSqlTableView().requestFocus();
				}));
		Nodes.addInputMap(this,
				InputMap.consume(EventPattern.keyPressed(KeyCode.E, KeyCombination.CONTROL_DOWN), action -> {
					this.editButtonAction(simulateClickEvent(editButton));
					getSelectedSqlTableView().requestFocus();
				}));
		Nodes.addInputMap(this,
				InputMap.consume(EventPattern.keyPressed(KeyCode.Q, KeyCombination.CONTROL_DOWN), action -> {
					this.addButtonAction();
					getSelectedSqlTableView().requestFocus();
				}));
		Nodes.addInputMap(this,
				InputMap.consume(EventPattern.keyPressed(KeyCode.I, KeyCombination.CONTROL_DOWN), action -> {
					this.importCsvAction();
					getSelectedSqlTableView().requestFocus();
				}));
		Nodes.addInputMap(this,
				InputMap.consume(EventPattern.keyPressed(KeyCode.R, KeyCombination.CONTROL_DOWN), action -> {
					this.refreshButtonAction();
					getSelectedSqlTableView().requestFocus();
				}));
	}

	@SuppressWarnings("unused")
	@Deprecated
	private void setSqlTableViewKeys(SqlTableView sqlTableView) {
		sqlTableView.setOnKeyPressed(keyEvent -> {
			if (keyEvent.isControlDown()) {
                switch (keyEvent.getCode()) {
                    case F -> this.searchButtonAction();
                    case C -> {
                        this.copyAction();
                        sqlTableView.requestFocus();
                    }
                    case D -> {
                        this.deleteButtonAction();
                        sqlTableView.requestFocus();
                    }
                    case E -> {
                        this.editButtonAction(simulateClickEvent(editButton));
                        sqlTableView.requestFocus();
                    }
                    case Q -> {
                        this.addButtonAction();
                        sqlTableView.requestFocus();
                    }
                    case I -> {
                        this.importCsvAction();
                        sqlTableView.requestFocus();
                    }
                    case R -> {
                        this.refreshButtonAction();
                        sqlTableView.requestFocus();
                    }
                    default -> {
                    }
                }
			}
			sqlTableView.requestFocus();
		});
	}

	private void determineTabIcon(SqlTableTab tab) {
		if (getSelectedSqlTableView().isFilledByQuery()) {
			tab.setCustomGraphic(JavaFXUtils.createIcon("/icons/table-y.png"));
		} else if (tab.getGraphic() != null && viewsBox != null && viewsBox.getItems().contains(tab.getCustomText())) {
			tab.setCustomGraphic(JavaFXUtils.createIcon("/icons/view.png"));
		} else if (tab.getGraphic() != null && tablesBox != null
				&& tablesBox.getItems().contains(tab.getCustomText())) {
			tab.setCustomGraphic(JavaFXUtils.createIcon("/icons/table.png"));
		}
	}

	public final SqlTableTab addSqlTableTab() {
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
		if (!sqlQueryRunning) {
			final SqlTableTab tab = this.addSqlTableTab();
			tab.startLoading();
			this.createTablesBox();
			this.createViewsBox();
			tab.startLoading();
			sqlConnector.executeAsync(() -> this.getDataFromDB(table, tab));
		}
	}

	public void createSqlTableTabWithDataUnsafe(String table) {
		this.createTablesBox();
		this.createViewsBox();
		final SqlTableTab tab = this.addSqlTableTab();
		tab.startLoading();
		sqlConnector.executeAsync(() -> this.getDataFromDB(table, tab));
	}

	@Override
	public ContextMenu createContextMenu() {
		ContextMenu contextMenu = new ContextMenu();

		MenuItem menuItemEdit = new MenuItem("Edit", JavaFXUtils.createIcon("/icons/edit.png"));
		menuItemEdit.setOnAction(event -> {
			if (isInFullMode()) {
				this.editButtonActionFullMode();
			} else {
				this.editButtonAction(simulateClickEvent());
			}
		});

		MenuItem menuItemCellEdit = new MenuItem("Edit Cell", JavaFXUtils.createIcon("/icons/edit.png"));

		menuItemCellEdit.setOnAction(event -> {
			SqlTableView sqlTableView = getSelectedSqlTableView();
			if (sqlTableView.areCellsEditable() && sqlTableView.getSelectedCell() != null)
				sqlTableView.getSelectedCell().startEdit();
		});

		MenuItem menuItemCopyCell = new MenuItem("Copy Cell", JavaFXUtils.createIcon("/icons/copy.png"));

		menuItemCopyCell.setOnAction(event -> {
			if (getSelectedSqlTableView().getSelectedCell() != null) {
				StringSelection stringSelection = new StringSelection(
						getSelectedSqlTableView().getSelectedCell().getText());
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				clipboard.setContents(stringSelection, null);
			}
		});

		MenuItem menuItemDelete = new MenuItem("Delete", JavaFXUtils.createIcon("/icons/minus.png"));
		menuItemDelete.setOnAction(event -> this.deleteButtonAction());

		MenuItem menuItemCopy = new MenuItem("Copy Row", JavaFXUtils.createIcon("/icons/copy.png"));
		menuItemCopy.setOnAction(actionEvent -> this.copyAction());

		MenuItem menuItemSearch = new MenuItem("Search...", JavaFXUtils.createIcon("/icons/magnify.png"));
		menuItemSearch.setOnAction(actionEvent -> searchButtonAction());

		MenuItem menuItemCompare = new MenuItem("Compare", JavaFXUtils.createIcon("/icons/compare.png"));
		menuItemCompare.setOnAction(actionEvent -> compareAction(simulateClickEvent()));

		contextMenu.getItems().addAll(menuItemCopyCell, menuItemCopy, new SeparatorMenuItem(), menuItemEdit, menuItemCellEdit,
				menuItemDelete, new SeparatorMenuItem(), menuItemSearch, menuItemCompare);

		return contextMenu;
	}

	private SqlTableRowEditBox createEditBox(final MapTableViewRow sqlTableRow, boolean isResizable) {
		return createEditBox(sqlTableRow, isResizable, Orientation.HORIZONTAL);
	}

	private SqlTableRowEditBox createEditBox(final MapTableViewRow sqlTableRow, boolean isResizable,
			Orientation toolBarOrientation) {
		SqlTableRowEditBox editBox = new SqlTableRowEditBox(getSelectedSqlTableView(), sqlTableRow, isResizable);

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
		refreshButton.setOnAction(event -> editBox.refresh());
		refreshButton.setFocusTraversable(false);

		FlowPane sideBar = new FlowPane(Orientation.VERTICAL, copyButton, pasteButton, refreshButton);

        switch (toolBarOrientation) {
            case VERTICAL -> editBox.setBarBottom(sideBar);
            case HORIZONTAL -> editBox.setBarLeft(sideBar);
            default -> {
            }
        }

		if (isResizable) {
			for (Node node : editBox.getChildren()) {
				if (node instanceof HBox)
					((HBox) node).prefWidthProperty().bind(editBox.widthProperty());
			}
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

	protected void createRecordsAddTab(final TabPane recordsTabPane) {
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
		addBtn.setOnAction(event -> this.insertRecordToSqlTableViewRef(editBox));

		editBox.getToolbar().getChildren().addAll(clearBtn);
		editBox.setActionButton(addBtn);

		addRecordTab.setContent(editBox);
		recordsTabPane.getTabs().add(0, addRecordTab);
	}

	public boolean isInFullMode() {
		return fullModeCheckBox.isSelected();
	}

	public boolean isLimitSet() {
		return limitModeCheckBox.isSelected();
	}

	protected void getDataFromDB(String table, final SqlTableTab sqlTableTab) {
		SqlTableView sqlTableView = sqlTableTab.getSqlTableView();
		sqlQueryRunning = true;
		String query = "select " + columnsFilter + " from " + table + whereFilter;

		// TODO: a more abstract implementation is needed for different connectors
		if (this.isLimitSet()) {
			query += " limit " + linesLimit;
		}

		String message = "Executing : " + query;
		logger.debug(message);
		try {
			sqlConnector.executeQueryRawSafely(query, resultSet -> {
				sqlTableView.setItemsLater(resultSet);
				// in case the query contains a view reset name
				sqlTableView.setTableName(table);
			});

		} catch (SQLException e) {
			DialogFactory.createErrorNotification(e);
			if (e.getErrorCode() == MemoryGuard.SQL_MEMORY_ERROR_CODE) {
				// TODO what must be done here in order to free memory?
//				Platform.runLater(() -> {
//					sqlTableTab.getOnClosed().handle(new ActionEvent());
//					tablesTabPane.getTabs().remove(sqlTableTab);
//				});
				System.gc();
			}
		} finally {
			this.updateRowsCountLabel();
			this.openInFullMode(sqlTableTab);
		}
	}

	public void updateRowsCountLabel() {
		if (isSearchApplied())
			Platform.runLater(() -> this.rowsCountLabel.setText(getSelectedSqlTableView().getItems().size()
					+ " filtered rows of " + getSelectedSqlTableView().getSqlTableRows().size()));
		else if (getSelectedSqlTableView() != null)
			Platform.runLater(
					() -> this.rowsCountLabel.setText(getSelectedSqlTableView().getSqlTableRows().size() + " rows"));

	}

	public void openInFullMode(final SqlTableTab sqlTableTab) {
		Platform.runLater(() -> {
			if (isInFullMode()) {
				// TODO records tab pane may should be cleared everytime to avoid memory leaks?
				final TabPane recordsTabPane = sqlTableTab.getRecordsTabPane() != null ? sqlTableTab.getRecordsTabPane()
						: this.createRecordsTabPane();

				SplitPane fullModeSplitPane = new SplitPane(sqlTableTab.getSqlTableView(), recordsTabPane);
				fullModeSplitPane.setDividerPositions(0.7, 0.3);
				fullModeSplitPane.setOrientation(Orientation.HORIZONTAL);
				sqlTableTab.setContent(fullModeSplitPane);
				sqlTableTab.setRecordsTabPane(recordsTabPane);
			}

			sqlQueryRunning = false;
		});
	}

	public void disableFullMode() {
		tablesTabPane.getSelectionModel().getSelectedItem().setContent(getSelectedSqlTableView());
		tablesTabPane.getTabs().forEach(tab -> {
			if (tab instanceof SqlTableTab)
				((SqlTableTab) tab).setRecordsTabPane(null);
		});
	}

	public void fillColumnCheckBoxes(final SqlTableView sqlTableView) {
		columnCheckBoxesMap.clear();
		for (String column : sqlTableView.getSqlTable().getColumns()) {
			CheckBox columnCheckBox = new CheckBox(column);
			columnCheckBox.setSelected(false);
			columnCheckBoxesMap.put(column, columnCheckBox);
		}
		for (TableColumn<MapTableViewRow, ?> column : sqlTableView.getVisibleLeafColumns()) {
			columnCheckBoxesMap.get(column.getText()).setSelected(true);
		}
		sqlTableView.bindColumnsVisibility(columnCheckBoxesMap.values());
	}

	private void searchFieldAction() {
		final SqlTableView sqlTableView = this.getSelectedSqlTableView();
		sqlTableView.getSelectionModel().clearSelection();

		ObservableList<MapTableViewRow> searchRows = FXCollections.observableArrayList();

		String[] split = searchField.getText().split(":");
		String columnRegex = split.length > 1 ? split[0] : null;
		String regex = split.length > 1 ? split[1] : split[0];

		this.setSearchApplied(!regex.isEmpty());

		for (MapTableViewRow row : sqlTableView.getSqlTableRows()) {
			for (TableColumn<MapTableViewRow, ?> column : sqlTableView.getVisibleLeafColumns()) {

				if (columnRegex != null && column.getText().equals(columnRegex) && row.get(column.getText()) != null) {
					if (row.get(column.getText()).toString().matches("(?i:.*" + regex + ".*)")) {
						searchRows.add(new MapTableViewRow(row));
						break;
					}
				} else if (columnRegex == null && row.get(column.getText()) != null) {
					if (row.get(column.getText()).toString().matches("(?i:.*" + regex + ".*)")) {
						searchRows.add(new MapTableViewRow(row));
						break;
					}
				}
			}
		}

		Platform.runLater(() -> {
			sqlTableView.setItems(searchRows);
			this.updateRowsCountLabel();
			this.fireEvent(new TableSearchFilteringEvent());
		});
	}

	protected void tableComboBoxAction(ComboBox<String> comboBox) {
		if (!sqlQueryRunning) {
			SqlTableTab tab = getSelectedTableTab();
			tab.startLoading();
			sqlConnector.executeAsync(
					() -> this.getDataFromDB(comboBox.getSelectionModel().getSelectedItem(), tab));
		}
	}

	protected void tablesTabPaneClickAction() {
		if (tablesTabPane.getSelectionModel().getSelectedItem() == addTableTab)
			this.addSqlTableTab();

		this.updateRowsCountLabel();
	}

	private void editButtonActionFullMode() {
		final MapTableViewRow sqlTableRow = getSelectedSqlTableView().getSelectionModel().getSelectedItem();
		if (sqlTableRow == null)
			return;

		if (getSelectedRecordsTabPane() == null) {
			SqlTableTab selectedTab = getSelectedTableTab();
			this.openInFullMode(selectedTab);
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
		editButton.setOnAction(event -> this.updateRecordOfSqlTableView(editBox, sqlTableRow));
		editBox.setActionButton(editButton);


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

		if (getSelectedSqlTableView().getColumns().isEmpty())
			return;

		SqlTableRowEditBox editBox = this.createEditBox(null, false);

		Button addBtn = new Button("Add", JavaFXUtils.createIcon("/icons/check.png"));
		addBtn.setTooltip(new Tooltip("Add"));
		editBox.setActionButton(addBtn);

		ScrollPane sp = new ScrollPane(editBox);
		sp.setMaxHeight(MAX_ENTRY_POP_OVER_HEIGHT);
		sp.setFitToWidth(true);
		popOver = new CustomPopOver(sp);

		addBtn.setOnAction(submitEvent -> this.insertRecordToSqlTableViewRef(editBox));

		popOver.show(addButton);
		addBtn.requestFocus();
	}

	protected void editButtonAction(MouseEvent event) {
		final SqlTableView sqlTableView = getSelectedSqlTableView();
		if (!sqlTableView.isFocused())
			editButton.requestFocus();

		if (editButton.isFocused() && popOver.isShowing())
			return;

		MapTableViewRow sqlTableRow = sqlTableView.getSelectionModel().getSelectedItem();
		if (sqlTableRow == null)
			return;

		SqlTableRowEditBox editBox = this.createEditBox(sqlTableRow, false);
		sqlTableRow.addObserver(editBox);

		ScrollPane sp = new ScrollPane(editBox);
		sp.setMaxHeight(MAX_ENTRY_POP_OVER_HEIGHT);
		sp.setFitToWidth(true);
		popOver = new CustomPopOver(sp);

		if (sqlTableView.getPrimaryKey() != null) {
			Button editBtn = new Button("Edit", JavaFXUtils.createIcon("/icons/check.png"));
			editBtn.setTooltip(new Tooltip("Edit"));
			editBtn.setOnAction(submitEvent -> this.updateRecordOfSqlTableView(editBox, sqlTableRow));
			editBox.setActionButton(editBtn);
		}

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
		final SqlTableTab tab = getSelectedTableTab();
		final SqlTableView sqlTableView = tab.getSqlTableView();
		ObservableList<MapTableViewRow> sqlTableRows = sqlTableView.getSelectionModel().getSelectedItems();

		if (sqlTableRows.isEmpty())
			return;

		if (DialogFactory.createDeleteDialog(sqlTableView, sqlTableRows, "Do you want to delete records?") == 0)
			return;

		List<MapTableViewRow> rowsToRemove = new ArrayList<>();
		tab.startLoading();
		sqlConnector.executeAsync(() -> {
			for (MapTableViewRow sqlTableRow : sqlTableRows) {
				if (this.deleteRecord(sqlTableRow, sqlTableView) == 1)
					rowsToRemove.add(sqlTableRow);
			}
			Platform.runLater(() -> {
				sqlTableView.getSelectionModel().clearSelection();
				sqlTableView.getSqlTableRows().removeAll(rowsToRemove);
				DialogFactory.createNotification("Records update", rowsToRemove.size() + " records have been deleted");
				this.updateRowsCountLabel();
				tab.load();
			});

		});
	}

	private void compareAction(MouseEvent mouseEvent) {
		if (getSelectedSqlTableView().getSelectionModel().getSelectedItems().size() > 10) {
			DialogFactory.createErrorNotification(new Exception("Too much elements to compare!"));
			return;
		}

		HBox compareBox = new HBox();
		for (MapTableViewRow row : getSelectedSqlTableView().getSelectionModel().getSelectedItems()) {
			SqlTableRowEditBox editBox = createEditBox(row, true);

			row.addObserver(editBox);
			Button editButton = new Button("Edit", JavaFXUtils.createIcon("/icons/check.png"));
			editButton.setTooltip(new Tooltip("Edit"));
			editButton.setOnAction(event -> this.updateRecordOfSqlTableView(editBox, row));
			editBox.setActionButton(editButton);
			editBox.prefWidthProperty().bind(compareBox.widthProperty().divide(2));
			compareBox.getChildren().add(editBox);

			editBox.setOnClose(() -> row.removeObserver(editBox));
		}

		if (isInFullMode()) {
			Tab compareTab = new Tab("Compare");
			compareTab.setGraphic(JavaFXUtils.createIcon("/icons/compare.png"));
			compareTab.setContent(compareBox);
			compareTab.setOnCloseRequest(closeEvent -> {
				for (Node node : compareBox.getChildren()) {
					((SqlTableRowEditBox) node).close();
				}
			});
			getSelectedTableTab().getRecordsTabPane().getTabs().add(compareTab);
			getSelectedTableTab().getRecordsTabPane().getSelectionModel().select(compareTab);
		} else {
			compareBox.setMaxHeight(MAX_ENTRY_POP_OVER_HEIGHT);
			compareBox.setMaxWidth(1600);
			compareBox.setPrefWidth(compareBox.getChildren().size() * 400);
			popOver = new CustomPopOver(compareBox);
			popOver.setOnHidden(closeEvent -> {
				for (Node node : compareBox.getChildren()) {
					((SqlTableRowEditBox) node).close();
				}
			});
			popOver.show(editButton, mouseEvent.getScreenX(), mouseEvent.getScreenY());
		}
	}

	protected void tableSelectButtonAction() {
		if (tableSelectButton.isFocused() && popOver.isShowing())
			return;

		tableSelectButton.requestFocus();
		// invoke createTableBox(), createViewsBox every time button clicked to has the
		// latest updates from db
		ComboBox<String> tablesBox = this.createTablesBox();
		ComboBox<String> viewsBox = this.createViewsBox();
		viewsBox.prefWidthProperty().bind(tablesBox.widthProperty());

		popOver = new CustomPopOver(new VBox(new Label("Select"), tablesBox, viewsBox));
		popOver.show(tableSelectButton);
	}

	protected void refreshButtonAction() {
		// TODO rerun original query in case an sqlcodearea is involved
		refreshButton.requestFocus();
		if (!sqlQueryRunning) {
			if (tablesTabPane.getSelectionModel().getSelectedItem() != null
					&& !getSelectedTableTab().getCustomText().equals(EMPTY)) {
				String tableName = getSelectedTableTab().getCustomText();
				SqlTableTab tab = getSelectedTableTab();
				tab.startLoading();
				sqlConnector.executeAsync(() -> this.getDataFromDB(tableName, tab));
				this.setSearchApplied(false);
				this.updateRowsCountLabel();
			}
		}
	}

	protected void settingsButtonAction() {
		if (settingsButton.isFocused() && popOver.isShowing())
			return;

		settingsButton.requestFocus();
		popOver = new CustomPopOver(new VBox(resizeModeCheckBox, fullModeCheckBox, limitModeCheckBox));
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
		popOver = new CustomPopOver(searchBox);
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
			popOver = new CustomPopOver(vBox);
			popOver.show(columnsSettingsButton);
		}
	}

	protected void importCsvAction() {
		final SqlTableTab tab = getSelectedTableTab();
		final SqlTableView sqlTableView = tab.getSqlTableView();
		if (sqlTableView != null && !sqlTableView.titleProperty().get().isEmpty()) {
			FileChooser fileChooser = new FileChooser();
			File selectedFile = fileChooser.showOpenDialog(null);

			Executors.newSingleThreadExecutor().execute(() -> {
				try {
					String filePath = selectedFile.getAbsolutePath();
					Platform.runLater(tab::startLoading);

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
									Object tempValue = (values.length - 1 >= i) ? values[i].replaceAll("\"", "") : null;

									map.put(columns[i], tempValue);
								}
								try {
									sqlTableView.insertRecord(map);
									rows.add(new MapTableViewRow(map));
								} catch (Exception e) {
									logger.error(e.getMessage(), e);
								}

							}
						}

						Platform.runLater(() -> {
							sqlTableView.getSqlTableRows().addAll(rows);
							DialogFactory.createNotification("CSV import", "CSV has been imported");
							this.updateRowsCountLabel();
						});
					}
				} catch (IOException e) {
					DialogFactory.createErrorDialog(e);
				} finally {
					Platform.runLater(tab::load);
				}
			});
		}
	}

	protected void exportCsvAction() {
		if (exportCsvButton.isFocused() && popOver.isShowing())
			return;

		final SqlTableView sqlTableView = getSelectedSqlTableView();

		exportCsvButton.requestFocus();

		FileChooser fileChooser = new FileChooser();
		fileChooser.setInitialFileName(sqlTableView.titleProperty().get() + ".csv");
		File selectedFile = fileChooser.showSaveDialog(null);

		if (selectedFile == null)
			return;

		Thread exportThread = new Thread(() -> {
			try {
				DialogFactory.createNotification("CSV Export", "Export started");

				if (!Files.exists(Paths.get(selectedFile.getPath())))
					Files.createFile(Paths.get(selectedFile.getPath()));

				Files.write(Paths.get(selectedFile.getAbsolutePath()),
						(sqlTableView.getSqlTable().columnsToString() + "\n").getBytes(), StandardOpenOption.CREATE,
						StandardOpenOption.APPEND);

				List<String> data = sqlTableView.getItems().stream().map(MapTableViewRow::toString).toList();
				String dataStr = StringUtils.join(data, "\n");
				Files.write(Paths.get(selectedFile.getAbsolutePath()), dataStr.getBytes(),
						StandardOpenOption.APPEND);
				DialogFactory.createNotification("CSV Export", "Export to csv has been completed\n" + pathField.getText());
			} catch (IOException e) {
				DialogFactory.createErrorDialog(e);
			}
		});
		exportThread.setDaemon(true);
		exportThread.start();
	}

	protected void copyAction() {
		StringBuilder content = new StringBuilder();

		getSelectedSqlTableView().getSelectionModel().getSelectedItems()
				.forEach(row -> content.append(row.toString()).append("\n"));
		if (!content.isEmpty())
			content.delete(content.length() - 1, content.length());

		StringSelection stringSelection = new StringSelection(content.toString());
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(stringSelection, null);
	}

	protected void pasteAction(final SqlTableRowEditBox editBox) {
		try {
			SqlTableView sqlTableView = getSelectedSqlTableView();
			String data = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);

			String[] split = data.split(",");
			for (int i = 0; i < sqlTableView.getColumnsNames().size(); i++) {
				String column = sqlTableView.getColumnsNames().get(i);
				editBox.put(column, split[i].replaceAll("\"", ""));
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	// TODO replace with method deleteRecord of SqlTableView
	public int deleteRecord(final MapTableViewRow sqlTableRow, final SqlTableView sqlTableView) {
		StringBuilder query = new StringBuilder("delete from " + sqlTableView.getTableName() + " where ");
		List<Object> params = new ArrayList<>();
		Set<String> columns = sqlTableView.getSqlTable().getColumns();
		if (sqlTableView.getPrimaryKey() != null) {
			String[] keys = sqlTableView.getPrimaryKey().split(",");
			for (String key : keys) {
				query.append(key).append(" = ? and ");
				params.add(sqlTableRow.get(key));
			}
			query = new StringBuilder(query.substring(0, query.length() - "and ".length()));
		} else {
			for (String column : columns) {
				params.add(sqlTableRow.get(column));
				query.append(column).append("= ? and ");
			}
			query = new StringBuilder(query.substring(0, query.length() - 5));
		}

		String message = "Executing : " + query + " [ values : " + params + " ]";
		logger.debug(message);

		try {
			sqlConnector.executeUpdate(query.toString(), params);
		} catch (Exception e) {
			DialogFactory.createErrorNotification(e);
			return 0;
		}

		return 1;
	}

	public void insertRecordToSqlTableViewRef(final SqlTableRowEditBox editBox) {
		sqlConnector.executeAsync(() -> {
			try {
				getSelectedSqlTableView().insertRecord(editBox);
				this.updateRowsCountLabel();
				DialogFactory.createNotification("Record insertion", "Successfully inserted!");
			} catch (Throwable e) {
				DialogFactory.createErrorNotification(e);
			}
		});

	}

	public void updateRecordOfSqlTableView(final SqlTableRowEditBox editBox, final MapTableViewRow sqlTableRow) {
		sqlConnector.executeAsync(() -> {
			try {
				getSelectedSqlTableView().updateRecord(editBox, sqlTableRow);
				DialogFactory.createNotification("Record update", "Successfully updated!");
			} catch (Exception e) {
				DialogFactory.createErrorNotification(e);
			}
		});
	}

	protected MouseEvent simulateClickEvent() {
		SqlTableView sqlTableView = getSelectedSqlTableView();
		return new MouseEvent(MouseEvent.MOUSE_CLICKED, 0, 0, sqlTableView.getContextMenu().getX(),
				sqlTableView.getContextMenu().getY(), MouseButton.PRIMARY, 1, false, false, false, false, false, false,
				false, false, false, false, null);
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

	public final SqlTableView getSelectedSqlTableView() {
		Tab tab = tablesTabPane != null ? tablesTabPane.getSelectionModel().getSelectedItem() : null;
		return tab instanceof SqlTableTab ? ((SqlTableTab) tab).getSqlTableView() : null;
	}

	public final TabPane getSelectedRecordsTabPane() {
		return getSelectedTableTab().getRecordsTabPane();
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

	public ComboBox<String> getTablesBox() {
		return tablesBox;
	}

	public TabPane getTablesTabPane() {
		return tablesTabPane;
	}

	public final SqlTableTab getSelectedTableTab() {
		Tab tab = tablesTabPane.getSelectionModel().getSelectedItem();
		return tab instanceof SqlTableTab ? (SqlTableTab) tab : null;
	}

	public Boolean isSearchApplied() {
		return isSearchApplied;
	}

	public void setSearchApplied(Boolean isSearchApplied) {
		this.isSearchApplied = isSearchApplied;
	}

	public void enableColumnFiltering(boolean enable) {
		this.isColumnFilteringEnabled = enable;
	}
}
