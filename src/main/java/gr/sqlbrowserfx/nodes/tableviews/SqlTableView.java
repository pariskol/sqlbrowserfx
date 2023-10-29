package gr.sqlbrowserfx.nodes.tableviews;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gr.sqlbrowserfx.LoggerConf;
import gr.sqlbrowserfx.conn.SqlConnector;
import gr.sqlbrowserfx.conn.SqlTable;
import gr.sqlbrowserfx.factories.DialogFactory;
import gr.sqlbrowserfx.nodes.InputMapOwner;
import gr.sqlbrowserfx.nodes.sqlpane.SqlTableRowEditBox;
import gr.sqlbrowserfx.nodes.sqlpane.SqlTableTab;
import gr.sqlbrowserfx.nodes.tableviews.filter.SqlTableFilter;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import gr.sqlbrowserfx.utils.MemoryGuard;
import gr.sqlbrowserfx.utils.mapper.DTOMapper;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.CheckBox;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;

public class SqlTableView extends TableView<MapTableViewRow> implements InputMapOwner {

	protected ObservableList<MapTableViewRow> rows;

	protected SimpleStringProperty titleProperty;
	protected SimpleBooleanProperty autoResizeProperty = new SimpleBooleanProperty(false);

	private SqlTableViewEditableCell selectedCell;

	protected SqlTable sqlTable;
	protected List<String> columns;
	protected SqlConnector sqlConnector;

	private double minWidth, prefWidth, maxWidth;
	private int currentColumnPos = 0;
	protected boolean filledByQuery = false;
	protected boolean areCellsEditableByClick;
	private boolean areColumnsFilterable = false;

	private final Map<String, Long> columnCounts;

	protected final static int NOT_SET = 0;

	private final Logger logger = LoggerFactory.getLogger(LoggerConf.LOGGER_NAME);
	private SqlTableTab parent;

	public SqlTableView() {

		rows = FXCollections.observableArrayList();
		sqlConnector = null;
		minWidth = 0;
		prefWidth = 0;
		maxWidth = 0;

		columnCounts = new HashMap<>();

		this.setInputMap();

		titleProperty = new SimpleStringProperty("empty");
		autoResizeProperty.addListener((ob, ov, nv) -> this.autoResizedColumns());
	}

	@Override
	public void setInputMap() {
		Nodes.addInputMap(this,
				InputMap.consume(EventPattern.keyPressed(KeyCode.LEFT, KeyCombination.CONTROL_DOWN), action -> {
					if (currentColumnPos < getColumns().size() - 1)
						this.scrollToColumn(getColumns().get(currentColumnPos++));
				}));
		Nodes.addInputMap(this,
				InputMap.consume(EventPattern.keyPressed(KeyCode.LEFT, KeyCombination.CONTROL_DOWN), action -> {
					if (currentColumnPos > 0)
						this.scrollToColumn(getColumns().get(currentColumnPos--));
				}));
	}

	public SqlTableView(ResultSet rs) throws Exception {
		this();
		setItems(rs);
	}

	public SqlTableView(SqlConnector sqlConnector) {
		this();
		this.sqlConnector = sqlConnector;
	}

	public SqlTableView(SqlConnector sqlConnector, ResultSet rs) throws Exception {
		this();
		this.sqlConnector = sqlConnector;
		setItems(rs);
	}

	public void setParent(SqlTableTab tab) {
		this.parent = tab;
	}

	public void clear() {
		this.getItems().clear();
		this.getColumns().clear();
		rows.clear();
	}

	protected void createColumnFilters() {
		// very poor performance of controlsfx tablefilter
		if (areColumnsFilterable)
			SqlTableFilter.apply(this);
	}

	public void resetColumnGraphic(TableColumn<?, ?> col) {
		String column = col.getText();
		if (sqlTable.isForeignKey(column))
			col.setGraphic(JavaFXUtils.createIcon("/icons/foreign-key.png"));
		else if (sqlTable.isPrimaryKey(column))
			col.setGraphic(JavaFXUtils.createIcon("/icons/primary-key.png"));
		else
			col.setGraphic(null);
	}

	public synchronized void setItems(ResultSet rs) throws Exception {
		this.filledByQuery = false;
		rows.clear();

		this.loadSqlTable(rs);

		Platform.runLater(() -> titleProperty.set(sqlTable.getName()));

		while (rs.next()) {
			LinkedHashMap<String, Object> entry = DTOMapper.map(rs);
			rows.add(new MapTableViewRow(entry));
		}

		List<TableColumn<MapTableViewRow, Object>> tableColumns = new ArrayList<>();
		for (String column : sqlTable.getColumns()) {
			TableColumn<MapTableViewRow, Object> col = new TableColumn<>(column);
			col.setCellValueFactory(param -> param.getValue().getObjectProperty(column));
			col.setCellFactory(callback -> new SqlTableViewEditableCell(this, sqlConnector));

			if (sqlTable.isForeignKey(column))
				col.setGraphic(JavaFXUtils.createIcon("/icons/foreign-key.png"));
			else if (sqlTable.isPrimaryKey(column))
				col.setGraphic(JavaFXUtils.createIcon("/icons/primary-key.png"));

			tableColumns.add(col);
		}

		this.getColumns().setAll(tableColumns);
		super.setItems(rows);

		this.createColumnFilters();

		this.autoResizedColumns();
		this.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
	}

	private void loadSqlTable(ResultSet rs) throws SQLException {
		HashSet<String> tablesSet = new HashSet<>();
		ResultSetMetaData rsmd = rs.getMetaData();
		for (int i = 1; i <= rsmd.getColumnCount(); i++) {
			if (!rsmd.getTableName(i).isEmpty())
				tablesSet.add(rsmd.getTableName(i));
		}
		
		StringBuilder actualName = new StringBuilder();
		for (String table : tablesSet) {
			actualName.append(table).append(", ");
		}
		if (!actualName.isEmpty()) {
			actualName = new StringBuilder(actualName.substring(0, actualName.length() - ", ".length()));
		}
		sqlTable = new SqlTable(actualName.toString(), rsmd);
		columns = new ArrayList<>(sqlTable.getColumns());

		// this exception handling is needed in case a raw sql query
		// was performed and joins many tables
		if (!this.isFilledByQuery()) {
			try {
				String primaryKey = sqlConnector.findPrimaryKey(sqlTable.getName());
				sqlTable.setPrimaryKey(primaryKey);
				List<String> foreignKeys = sqlConnector.findForeignKeys(sqlTable.getName());
				sqlTable.setForeignKeys(foreignKeys);
			} catch(SQLException e) {
				logger.info(e.getMessage());
			}
		}
	}
	
	// this function is a little ugly as Platform.runLater() is being used many
	// times
	// but it is needed in order not to block ui thread
	public synchronized void setItemsLater(ResultSet rs) throws SQLException {
		if (parent != null) {
			Platform.runLater(() -> parent.startLoading());
		}

		rows.clear();

		this.loadSqlTable(rs); 
		
		MemoryGuard.protect(rs);
		
		try {
			while (rs.next()) {
				LinkedHashMap<String, Object> entry = DTOMapper.map(rs);
				rows.add(new MapTableViewRow(entry));
			}

			sqlTable.getColumns().forEach(col -> {
				long count = rows.stream().map(row -> row.get(col)).distinct().count();
				columnCounts.put(col, count);
			});

		} catch (Throwable e) {
			// exception must be handled here to set an indicator that
			// something went wrong in case user hasn't sawed the notification
			Platform.runLater(() -> {
				this.titleProperty.set("error");
				parent.load();
				this.clear();
			});
			throw new SQLException("MemoryGuard action", "", MemoryGuard.SQL_MEMORY_ERROR_CODE);
		}

		Platform.runLater(() -> {
			this.getColumns().clear();
			String title = sqlTable.getName().isEmpty() ? "query" : sqlTable.getName();
			titleProperty.set(title);

			for (String column : sqlTable.getColumns()) {
				TableColumn<MapTableViewRow, Object> col = new TableColumn<>(column);
				col.setCellValueFactory(param -> param.getValue().getObjectProperty(column));
				col.setCellFactory(callback -> new SqlTableViewEditableCell(this, sqlConnector));

				if (sqlTable.isForeignKey(column)) {
					col.setGraphic(JavaFXUtils.createIcon("/icons/foreign-key.png"));
				} else if (sqlTable.isPrimaryKey(column)) {
					col.setGraphic(JavaFXUtils.createIcon("/icons/primary-key.png"));
				}

				this.getColumns().add(col);
			}

			this.autoResizedColumns();
			this.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
			super.setItems(rows);

			this.createColumnFilters();

			if (parent != null) {
				parent.load();
			}
		});
	}

	public List<String> getColumnsNames() {
		return columns;
	}

	// 0 for no set
	public void setColumnWidth(double min, double pref, double max) {
		this.minWidth = min;
		this.prefWidth = pref;
		this.maxWidth = max;

		for (TableColumn<?, ?> column : this.getColumns()) {
			if (min > NOT_SET)
				column.setMinWidth(minWidth);
			if (pref > NOT_SET)
				column.setPrefWidth(prefWidth);
			if (max > NOT_SET)
				column.setMaxWidth(maxWidth);
		}
	}

	private void autoResizedColumns() {
		if (autoResizeProperty.get()) {
			this.setColumnWidth(NOT_SET, NOT_SET, NOT_SET);
			for (TableColumn<?, ?> column : this.getVisibleLeafColumns()) {
				column.prefWidthProperty().bind(this.widthProperty().divide(this.getVisibleLeafColumns().size()));
			}
			return;
		}
		
		for (TableColumn<?, ?> column : this.getVisibleLeafColumns()) {
			column.prefWidthProperty().unbind();
		}
		this.setColumnWidth(minWidth, prefWidth, maxWidth);
	}

	public void bindColumnsVisibility(Collection<CheckBox> columCheckBoxes) {
		for (TableColumn<MapTableViewRow, ?> tableColumn : this.getColumns()) {
			for (CheckBox checkBox : columCheckBoxes) {
				if (tableColumn.getText().equals(checkBox.getText())) {
					tableColumn.visibleProperty().unbind();
					tableColumn.visibleProperty().bindBidirectional(checkBox.selectedProperty());
				}
			}
		}
	}

	public void createColumns(List<String> columns) {
		for (String column : columns) {
			TableColumn<MapTableViewRow, Object> col = new TableColumn<>(column);
			col.setCellValueFactory(param -> param.getValue().getObjectProperty(column));
			col.setCellFactory(callback -> new SqlTableViewCell());
			this.getColumns().add(col);
		}
	}

	public void updateSelectedRow() throws Exception {
		MapTableViewRow sqlTableRow = this.getSelectionModel().getSelectedItem();
		Set<String> columns = this.getSqlTable().getColumns();
		StringBuilder query = new StringBuilder("update " + this.getTableName() + " set ");
		List<Object> params = new ArrayList<>();

		for (String column : columns) {
			if (this.getPrimaryKey() != null && !this.getPrimaryKey().contains(column)) {
				String elm = null;
				if (sqlTableRow.get(column) != null)
					elm = sqlTableRow.get(column).toString();
				// type checking
				Object actualValue = null;
				if (elm != null && !elm.isEmpty())
					actualValue = sqlConnector.castToDBType(this.getSqlTable(), column, elm);
				params.add(actualValue);
				query.append(column).append("= ? ,");
			}
		}
		query = new StringBuilder(query.substring(0, query.length() - 1));
		query.append(" where ");
		String[] keys = this.getPrimaryKey().split(",");
		for (String key : keys) {
			query.append(key).append(" = ? and ");
			params.add(sqlTableRow.get(key));
		}
		query = new StringBuilder(query.substring(0, query.length() - "and ".length()));

		String message = "Executing : " + query + " [ values : " + params + " ]";
		LoggerFactory.getLogger(LoggerConf.LOGGER_NAME).debug(message);
		sqlConnector.executeUpdate(query.toString(), params);

		for (String column : columns) {
			String elm = null;
			if (sqlTableRow.get(column) != null)
				elm = sqlTableRow.get(column).toString();

			Object actualValue = null;
			if (elm != null)
				actualValue = sqlConnector.castToDBType(this.getSqlTable(), column, elm);
			sqlTableRow.set(column, actualValue);
		}
		// notify listeners
		sqlTableRow.changed();
	}

	public void deleteRecord(MapTableViewRow sqlTableRow) {
		StringBuilder query = new StringBuilder("delete from " + this.getTableName() + " where ");
		List<Object> params = new ArrayList<>();
		Set<String> columns = this.getSqlTable().getColumns();
		if (this.getPrimaryKey() != null) {
			String[] keys = this.getPrimaryKey().split(",");
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
			// TODO: may enable the following row with param
//			this.getSqlTableRows().remove(sqlTableRow);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	public void insertRecord(SqlTableRowEditBox editBox) throws SQLException {
		Set<String> columns = this.getSqlTable().getColumns();
		List<Object> params = new ArrayList<>();
		StringBuilder notEmptyColumns = new StringBuilder();
		StringBuilder values = new StringBuilder();
		Map<String, TextArea> map = editBox.getMap();
		Map<String, Object> entry = new HashMap<>();

		for (String column : columns) {
			Object elm = map.get(column).getText();
			if (elm != null && !elm.toString().isEmpty()) {
				notEmptyColumns.append(column).append(", ");
				Object actualValue;
				try {
					actualValue = sqlConnector.castToDBType(this.getSqlTable(), column,
							editBox.getMap().get(column).getText());
				} catch (NumberFormatException e) {
					String message = "Value \"" + editBox.getMap().get(column).getText() + "\" is not valid for column "
							+ column + ", expecting " + this.getSqlTable().getColumnsMap().get(column);
					throw new SQLException(message);
				} catch (Exception e) {
					throw new SQLException(e);
				}
				params.add(actualValue);
				entry.put(column, actualValue);
				values.append("?, ");
			}
		}
		notEmptyColumns = new StringBuilder(notEmptyColumns.substring(0, notEmptyColumns.length() - ", ".length()));
		values = new StringBuilder(values.substring(0, values.length() - ", ".length()));

		String sqlQuery = "insert into " + this.getTableName() + "(" + notEmptyColumns + ")" + " values (" + values
				+ ")";

		String message = "Executing : " + sqlQuery + " [ values : " + params + " ]";
		logger.debug(message);
        sqlConnector.executeUpdate(sqlQuery, params);
		try {
			Integer lastId = sqlConnector.getLastGeneratedId();
			if (lastId == -1)
				throw new Exception("Could not retrieve last inserted id");

			sqlConnector.executeQuery("select " + StringUtils.join(columns, ", ") + " from " + this.getTableName()
					+ " where " + this.getSqlTable().getPrimaryKey() + " = ? ", List.of(lastId), rset -> {
						try {
							this.getSqlTableRows().add(new MapTableViewRow(DTOMapper.map(rset)));
						} catch (Exception e) {
							DialogFactory.createErrorDialog(e);
						}
					});
		} catch (Exception e) {
			// fallback option if query to retrieve last id failed
			this.getSqlTableRows().add(new MapTableViewRow(entry));
		}
	}

	public void insertRecord(Map<String, Object> map) throws SQLException {
		Set<String> columns = this.getSqlTable().getColumns();
		List<Object> params = new ArrayList<>();
		StringBuilder notEmptyColumns = new StringBuilder();
		StringBuilder values = new StringBuilder();

		for (String column : columns) {
			Object elm = map.get(column);
			if (elm != null && !elm.toString().isEmpty()) {
				notEmptyColumns.append(column).append(", ");
				params.add(map.get(column));
				values.append("?, ");
			}
		}
		notEmptyColumns = new StringBuilder(notEmptyColumns.substring(0, notEmptyColumns.length() - ", ".length()));
		values = new StringBuilder(values.substring(0, values.length() - ", ".length()));

		String sqlQuery = "insert into " + this.getTableName() + "(" + notEmptyColumns + ")" + " values (" + values
				+ ")";

		String message = "Executing : " + sqlQuery + " [ values : " + params + " ]";
		logger.debug(message);
		sqlConnector.executeUpdate(sqlQuery, params);
	}

	public void updateRecord(final SqlTableRowEditBox editBox, final MapTableViewRow sqlTableRow) throws SQLException {
		Set<String> columns = this.getSqlTable().getColumns();
		StringBuilder query = new StringBuilder("update " + this.getTableName() + " set ");
		List<Object> params = new ArrayList<>();

		for (String column : columns) {
			if (this.getPrimaryKey() != null && !this.getPrimaryKey().contains(column)) {
				TextArea elm = editBox.getMap().get(column);
				Object actualValue = null;
				if (elm != null && elm.getText() != null && !elm.getText().isEmpty()) {
					// type checking
					try {
						actualValue = sqlConnector.castToDBType(this.getSqlTable(), column,
								editBox.getMap().get(column).getText());
					} catch (Exception e) {
						String message = "Value \"" + editBox.getMap().get(column).getText()
								+ "\" is not valid for column " + column + ", expecting "
								+ this.getSqlTable().getColumnsMap().get(column);
						throw new SQLException(message, e);
					}
				}
				params.add(actualValue);
				query.append(column).append("= ? ,");
			}
		}
		query = new StringBuilder(query.substring(0, query.length() - 1));
		query.append(" where ");
		String[] keys = this.getPrimaryKey().split(",");
		for (String key : keys) {
			query.append(key).append(" = ? and ");
			params.add(sqlTableRow.get(key));
		}
		query = new StringBuilder(query.substring(0, query.length() - "and ".length()));

		String message = "Executing : " + query + " [ values : " + params + " ]";
		logger.debug(message);

		if (sqlConnector.executeUpdate(query.toString(), params) > 0) {
			if (sqlConnector.isAutoCommitModeEnabled())
				updateRowFromDb(sqlTableRow, columns);
			else
				sqlTableRow.refreshMapFromEditBox(editBox);
		}

		// notify listeners
		sqlTableRow.changed();
	}

	private void updateRowFromDb(final MapTableViewRow sqlTableRow, Set<String> columns) throws SQLException {
		List<Object> params = new ArrayList<>();
		String[] keys;
		StringBuilder selectQuery = new StringBuilder("select " + StringUtils.join(columns, ",") + " from " + this.getSqlTable().getName()
                + " where ");
		keys = this.getPrimaryKey().split(",");
		for (String key : keys) {
			selectQuery.append(key).append(" = ? and ");
			params.add(sqlTableRow.get(key));
		}
		selectQuery = new StringBuilder(selectQuery.substring(0, selectQuery.length() - "and ".length()));

		sqlConnector.executeQuery(selectQuery.toString(), params, rset -> {
			LinkedHashMap<String, Object> entry = new LinkedHashMap<>();
			for (String columnLabel : sqlTable.getColumns()) {
				entry.put(columnLabel, rset.getObject(columnLabel));
			}
			sqlTableRow.refreshMap(entry);
		});
	}

	public ObservableList<MapTableViewRow> getSqlTableRows() {
		return rows;
	}

	public String getTableName() {
		return sqlTable.getName();
	}

	public void setSqlConnector(SqlConnector sqlConnector) {
		this.sqlConnector = sqlConnector;
	}

	public SqlConnector getSqlConnector() {
		return sqlConnector;
	}

	/*
	 * Returns table's primary key , IMPORTANT in case of a composite key it
	 * returns a comma separated string with the keys
	 * 
	 */
	public String getPrimaryKey() {
		return sqlTable.getPrimaryKey();
	}

	public SqlTable getSqlTable() {
		return sqlTable;
	}

	public SqlTableViewEditableCell getSelectedCell() {
		return selectedCell;
	}

	public void setSelectedCell(SqlTableViewEditableCell sqlTableViewEditCell) {
		this.selectedCell = sqlTableViewEditCell;
	}

	public SimpleStringProperty titleProperty() {
		return titleProperty;
	}

	public void setTableName(String name) {
		sqlTable.setName(name);
		Platform.runLater(() -> titleProperty.set(sqlTable.getName()));
	}
	
	public void setFilledByQuery(boolean b) {
		this.filledByQuery = b;
	}

	public boolean areCellsEditable() {
		return getPrimaryKey() != null;
	}
	
	public boolean isFilledByQuery() {
		return filledByQuery;
	}

	public boolean areCellsEditableByClick() {
		return areCellsEditableByClick;
	}

	public void setCellsEditableByClick(boolean areEditable) {
		this.areCellsEditableByClick = areEditable;
	}

	public void enableColumnFiltering(boolean areColumnsFilterable) {
		this.areColumnsFilterable = areColumnsFilterable;
	}

	public Long getUniqueEntriesForColumn(String col) {
		return columnCounts.get(col);
	}

	public SimpleBooleanProperty autoResizeProperty() {
		return autoResizeProperty;
	}

}
