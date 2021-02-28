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
import gr.sqlbrowserfx.nodes.sqlpane.SqlTableRowEditBox;
import gr.sqlbrowserfx.nodes.sqlpane.SqlTableTab;
import gr.sqlbrowserfx.utils.MemoryGuard;
import gr.sqlbrowserfx.utils.mapper.DTOMapper;
import javafx.application.Platform;
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

//FIXME use deleteRow, log log4j in ui
public class SqlTableView extends TableView<MapTableViewRow> {

	protected ObservableList<MapTableViewRow> rows;
	protected SimpleStringProperty titleProperty;
	protected SqlTable sqlTable;
	protected List<String> columns;
	protected SqlConnector sqlConnector;
	private double minWidth, prefWidth, maxWidth;
	protected boolean autoResize;
	private int currentColumnPos = 0;
	private SqlTableViewEditableCell selectedCell;
	protected boolean filledByQuery = false;
	protected boolean areCellsEditableByClick;

	protected final static int NOT_SET = 0;

	private Logger logger = LoggerFactory.getLogger(LoggerConf.LOGGER_NAME);
	private SqlTableTab parent;

	public SqlTableView() {

		rows = FXCollections.observableArrayList();
		sqlConnector = null;
		autoResize = false;
		minWidth = 0;
		prefWidth = 0;
		maxWidth = 0;

		this.setInputMap();
//		this.setKeys();
		
		titleProperty = new SimpleStringProperty("empty");
	}

	@SuppressWarnings("unused")
	@Deprecated
	private void setKeys() {
		this.setOnKeyPressed(keyEvent -> {
			if (keyEvent.isControlDown()) {
				if (keyEvent.getCode() == KeyCode.LEFT) {
					if (currentColumnPos < getColumns().size() - 1)
						this.scrollToColumn(getColumns().get(currentColumnPos++));
				}
				if (keyEvent.getCode() == KeyCode.RIGHT) {
					if (currentColumnPos > 0)
						this.scrollToColumn(getColumns().get(currentColumnPos--));
				}
			}
		});
	}

	protected void setInputMap() {
		Nodes.addInputMap(this, 
				InputMap.consume(
				EventPattern.keyPressed(KeyCode.LEFT, KeyCombination.CONTROL_DOWN),
				action -> {
					if (currentColumnPos < getColumns().size() - 1)
						this.scrollToColumn(getColumns().get(currentColumnPos++));
				}
        ));
		Nodes.addInputMap(this, 
				InputMap.consume(
				EventPattern.keyPressed(KeyCode.LEFT, KeyCombination.CONTROL_DOWN),
				action -> {
					if (currentColumnPos > 0)
						this.scrollToColumn(getColumns().get(currentColumnPos--));
				}
        ));
	}

	public SqlTableView(ResultSet rs) throws SQLException {
		this();
		setItems(rs);
	}

	public SqlTableView(SqlConnector sqlConnector) {
		this();
		this.sqlConnector = sqlConnector;
	}
	
	public SqlTableView(SqlConnector sqlConnector, ResultSet rs) throws SQLException {
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

	public synchronized void setItems(ResultSet rs) throws SQLException {

		this.filledByQuery = false;
		rows.clear();

		ResultSetMetaData rsmd = rs.getMetaData();
		HashSet<String> tablesSet = new HashSet<>();
		for (int i=1;i<=rsmd.getColumnCount();i++) {
			tablesSet.add(rsmd.getTableName(i));
		}
		sqlTable = new SqlTable(rsmd);
		columns = new ArrayList<>(sqlTable.getColumns());

		String primaryKey = sqlTable.getName() != null ? sqlConnector.findPrimaryKey(sqlTable.getName()) : null;
		sqlTable.setPrimaryKey(primaryKey);
		List<String> foreignKeys = sqlConnector.findForeignKeys(sqlTable.getName());
		sqlTable.setForeignKeys(foreignKeys);
		
		Platform.runLater(() -> titleProperty.set(sqlTable.getName()));
		
		try {
			while (rs.next()) {
				LinkedHashMap<String, Object> entry = DTOMapper.map(rs);
				rows.add(new MapTableViewRow(entry));
			}
		} catch (Exception e) {
			rows.clear();
		}

		List<TableColumn<MapTableViewRow, Object>> tableColumns = new ArrayList<>();
		for (String column : sqlTable.getColumns()) {
			TableColumn<MapTableViewRow, Object> col = new TableColumn<>(column);
			col.setCellValueFactory(param -> {
				return param.getValue().getObjectProperty(column);
			});
			col.setCellFactory(callback -> {
				return new SqlTableViewEditableCell(this, sqlConnector);
			});
			tableColumns.add(col);
		}
		
		this.getColumns().setAll(tableColumns);
		super.setItems(rows);

		this.autoResizedColumns(autoResize);
		this.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
	}
	
	public synchronized void setItemsLater(ResultSet rs) throws SQLException {

		if (parent != null)
			Platform.runLater(() -> parent.startLoading());
		
		rows.clear();

		HashSet<String> tablesSet = new HashSet<>();
		ResultSetMetaData rsmd = rs.getMetaData();
		for (int i=1;i<=rsmd.getColumnCount();i++) {
			if (!rsmd.getTableName(i).isEmpty())
				tablesSet.add(rsmd.getTableName(i));
		}
		
		String actualName = "";
		for (String table : tablesSet) {
			sqlTable = new SqlTable(table, rsmd);
			actualName += table + ", ";
		}
		if (actualName.length() > ", ".length())
			actualName = actualName.substring(0, actualName.length() - ", ".length());
		sqlTable = new SqlTable(rsmd);
		sqlTable.setName(actualName);
		columns = new ArrayList<>(sqlTable.getColumns());

		try {
			String primaryKey = sqlConnector.findPrimaryKey(sqlTable.getName());
			sqlTable.setPrimaryKey(primaryKey);
			List<String> foreignKeys = sqlConnector.findForeignKeys(sqlTable.getName());
			sqlTable.setForeignKeys(foreignKeys);
		} catch (SQLException e) {
			logger.info(e.getMessage());
		}
		
		MemoryGuard.startMemoryGuard(rs);
		try {
			while (rs.next()) {
				LinkedHashMap<String, Object> entry = DTOMapper.map(rs);
				rows.add(new MapTableViewRow(entry));
			}
		} catch (Throwable e) {
			this.clear();
			Platform.runLater(() -> {
				this.titleProperty.set("error");
				parent.load();
			});
			throw new SQLException("MemoryGuard action", "", MemoryGuard.SQL_MEMORY_ERROR_CODE);
		}

		Platform.runLater(() -> {
			this.getColumns().clear();
			titleProperty.set(sqlTable.getName());
			
			for (String column : sqlTable.getColumns()) {
				TableColumn<MapTableViewRow, Object> col = new TableColumn<>(column);
				col.setCellValueFactory(param -> {
					return param.getValue().getObjectProperty(column);
				});
				col.setCellFactory(callback -> {
					return new SqlTableViewEditableCell(this, sqlConnector);
				});
				this.getColumns().add(col);
			}

//			this.setRowFactory(tv -> {
//			    TableRow<MapTableViewRow> row = new TableRow<>();
//			    BooleanBinding updatedByGui = row.itemProperty().get().isUpdatedByGui().not().not();
//			    row.styleProperty().bind(Bindings.when(updatedByGui)
//			        .then("-fx-background-color: green ;")
//			        .otherwise(""));
//			    return row ;
//			});
			
			this.autoResizedColumns(autoResize);
			this.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
			super.setItems(rows);
			
			if (parent != null)
				parent.load();
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

	public void autoResizedColumns(boolean autoResize) {
		this.autoResize = autoResize;
		if (autoResize) {
			this.setColumnWidth(NOT_SET, NOT_SET, NOT_SET);
			for (TableColumn<?, ?> column : this.getVisibleLeafColumns()) {
				column.prefWidthProperty().bind(this.widthProperty().divide(this.getVisibleLeafColumns().size()));
			}
		} else {
			for (TableColumn<?, ?> column : this.getVisibleLeafColumns()) {
				column.prefWidthProperty().unbind();
			}
			this.setColumnWidth(minWidth, prefWidth, maxWidth);
		}
	}
	
	public void bindColumsVisibility(Collection<CheckBox> columCheckBoxes) {
		for (TableColumn<MapTableViewRow, ?> tableColumn: this.getColumns()) {
			for (CheckBox checkBox: columCheckBoxes) {
				if (tableColumn.getText().equals(checkBox.getText())) {
					tableColumn.visibleProperty().unbind();
					tableColumn.visibleProperty().bindBidirectional(checkBox.selectedProperty());
					checkBox.setOnAction(actionEvent -> this.autoResizedColumns(autoResize));
				}
			}
		}
	}

	public void createColumns(List<String> colums) {
		for (String column : colums) {
			TableColumn<MapTableViewRow, Object> col = new TableColumn<>(column);
			col.setCellValueFactory(param -> {
				return param.getValue().getObjectProperty(column);
			});
			col.setCellFactory(callback -> {
				return new SqlTableViewCell();
			});
			this.getColumns().add(col);
		}
	}
	
	public void updateSelectedRow() throws Exception {
		MapTableViewRow sqlTableRow = this.getSelectionModel().getSelectedItem();
		Set<String> columns = this.getSqlTable().getColumns();
		String query = "update " + this.getTableName() + " set ";
		List<Object> params = new ArrayList<>();

		for (String column : columns) {
//			if (!column.equals(this.getPrimaryKey())) {
			if (this.getPrimaryKey() != null && !this.getPrimaryKey().contains(column)) {
				String elm = null;
				if (sqlTableRow.get(column) != null)
					elm = sqlTableRow.get(column).toString();
				// type checking
				Object actualValue = null;
				if (elm != null && !elm.isEmpty())
					actualValue = sqlConnector.castToDBType(this.getSqlTable(), column, elm);
				params.add(actualValue);
				query += column + "= ? ,";
			}
		}
		query = query.substring(0, query.length() - 1);
		query += " where ";
		String[] keys = this.getPrimaryKey().split(",");
		for (String key : keys) {
			query += key + " = ? and ";
			params.add(sqlTableRow.get(key));
		}
		query = query.substring(0, query.length() - "and ".length());

		String message = "Executing : " + query + " [ values : " + params.toString() + " ]";
		LoggerFactory.getLogger(LoggerConf.LOGGER_NAME).debug(message);
		sqlConnector.executeUpdate(query, params);

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
	
	public int deleteRecord(MapTableViewRow sqlTableRow) {
		String query = "delete from " + this.getTableName() + " where ";
		List<Object> params = new ArrayList<>();
		Set<String> columns = this.getSqlTable().getColumns();
		if (this.getPrimaryKey() != null) {
			String[] keys = this.getPrimaryKey().split(",");
			for (String key : keys) {
				query += key + " = ? and ";
				params.add(sqlTableRow.get(key));
			}
			query = query.substring(0, query.length() - "and ".length());
		} else {
			for (String column : columns) {
				params.add(sqlTableRow.get(column));
				query += column + "= ? and ";
			}
			query = query.substring(0, query.length() - 5);
		}

		String message = "Executing : " + query + " [ values : " + params.toString() + " ]";
		logger.debug(message);

		try {
			sqlConnector.executeUpdate(query, params);
			this.getSqlTableRows().remove(sqlTableRow);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return 0;
		}

		return 1;
	}

	public void insertRecord(SqlTableRowEditBox editBox) throws SQLException {
		Set<String> columns = this.getSqlTable().getColumns();
		List<Object> params = new ArrayList<>();
		String notEmptyColumns = "";
		String values = "";
		Map<String, TextArea> map = editBox.getMap();
		Map<String, Object> entry = new HashMap<>();

		for (String column : columns) {
			Object elm = map.get(column).getText();
			if (elm != null && !elm.toString().equals("")) {
				notEmptyColumns += column + ", ";
				Object actualValue = null;
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
				values += "?, ";
			}
		}
		notEmptyColumns = notEmptyColumns.substring(0, notEmptyColumns.length() - ", ".length());
		values = values.substring(0, values.length() - ", ".length());

		String sqlQuery = "insert into " + this.getTableName() + "(" + notEmptyColumns + ")" + " values ("
				+ values + ")";

		String message = "Executing : " + sqlQuery + " [ values : " + params.toString() + " ]";
		logger.debug(message);
		final String query = sqlQuery;
		sqlConnector.executeUpdate(query, params);
		this.getSqlTableRows().add(new MapTableViewRow(entry));
//		this.sort();
	}

	public void insertRecord(Map<String, Object> map) throws SQLException {
		Set<String> columns = this.getSqlTable().getColumns();
		List<Object> params = new ArrayList<>();
		String notEmptyColumns = "";
		String values = "";

		for (String column : columns) {
			Object elm = map.get(column);
			if (elm != null && !elm.toString().equals("")) {
				notEmptyColumns += column + ", ";
				params.add(map.get(column));
				values += "?, ";
			}
		}
		notEmptyColumns = notEmptyColumns.substring(0, notEmptyColumns.length() - ", ".length());
		values = values.substring(0, values.length() - ", ".length());

		String sqlQuery = "insert into " + this.getTableName() + "(" + notEmptyColumns + ")" + " values ("
				+ values + ")";

		String message = "Executing : " + sqlQuery + " [ values : " + params.toString() + " ]";
		logger.debug(message);
		final String query = sqlQuery;
		sqlConnector.executeUpdate(query, params);
	}

	public void updateRecord(final SqlTableRowEditBox editBox, final MapTableViewRow sqlTableRow) throws SQLException {
		Set<String> columns = this.getSqlTable().getColumns();
		String query = "update " + this.getTableName() + " set ";
		List<Object> params = new ArrayList<>();

		for (String column : columns) {
//			if (!column.equals(this.getPrimaryKey())) {
			if (this.getPrimaryKey() != null && !this.getPrimaryKey().contains(column)) {
				TextArea elm = editBox.getMap().get(column);
				Object actualValue = null;
				if (elm != null && elm.getText() != null && !elm.getText().equals("")) {
					// type checking
					try {
						actualValue = sqlConnector.castToDBType(this.getSqlTable(), column,
								editBox.getMap().get(column).getText());
						// Class<?> clazz = sqlTableRow.get(label.getText()).getValue().getClass();
						// actualValue = clazz.cast(actualValue);
					} catch (Exception e) {
						String message = "Value \"" + editBox.getMap().get(column).getText() + "\" is not valid for column "
								+ column + ", expecting " + this.getSqlTable().getColumnsMap().get(column);
						throw new SQLException(message, e);
					}
				}
				params.add(actualValue);
				query += column + "= ? ,";
			}
		}
		query = query.substring(0, query.length() - 1);
		query += " where ";
		String[] keys = this.getPrimaryKey().split(",");
		for (String key : keys) {
			query += key + " = ? and ";
			params.add(sqlTableRow.get(key));
		}
		query = query.substring(0, query.length() - "and ".length());

		String message = "Executing : " + query + " [ values : " + params.toString() + " ]";
		logger.debug(message);

		if (sqlConnector.executeUpdate(query, params) > 0) {
			if (sqlConnector.isAutoCommitModeEnabled())
				updateRowFromDb(sqlTableRow, columns);
			else
				sqlTableRow.refreshMapFromEditBox(editBox);
		}

		// notify listeners
		sqlTableRow.changed();
	}

	private void updateRowFromDb(final MapTableViewRow sqlTableRow, Set<String> columns)
			throws SQLException {
		List<Object> params = new ArrayList<>();
		String[] keys;
		String selectQuery = "select " + StringUtils.join(columns, ",") + " from " + this.getSqlTable().getName()
				+ " where ";
		keys = this.getPrimaryKey().split(",");
		for (String key : keys) {
			selectQuery += key + " = ? and ";
			params.add(sqlTableRow.get(key));
		}
		selectQuery = selectQuery.substring(0, selectQuery.length() - "and ".length());
		
		sqlConnector.executeQuery(selectQuery, params,
			rset -> {
				LinkedHashMap<String, Object> entry = new LinkedHashMap<>();
				for (String columnLabel : sqlTable.getColumns()) {
					entry.put(columnLabel, rset.getObject(columnLabel));
				}
				sqlTableRow.refreshMap(entry);
			});
	}
	
	@SuppressWarnings("unused")
	private void customSort() {
		this.getSqlTableRows().sort((o1, o2) -> {
			if (o1.get(this.getPrimaryKey()) != null && o2.get(this.getPrimaryKey()) != null) {
				if (o1.get(this.getPrimaryKey()).toString()
						.compareTo(o2.get(this.getPrimaryKey()).toString()) > 0) {

					return 1;
				}
			}
			return 0;
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
	 * Returns tables's primary key , IMPORTANT in case of a composite
	 * key it returns a comma separated string with the keys 
	 * 
	 */
	public String getPrimaryKey() {
		return sqlTable.getPrimaryKey();
	}

	public SqlTable getSqlTable() {
		return sqlTable;
	}

	public void setSqlTable(SqlTable sqlTable) {
		this.sqlTable = sqlTable;
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
	
	public boolean isFilledByQuery() {
		return filledByQuery;
	}

	public boolean areCellsEditableByClick() {
		return areCellsEditableByClick;
	}
	
	public void setCellsEditableByClick(boolean areEditable) {
		this.areCellsEditableByClick = areEditable;
	}
}
