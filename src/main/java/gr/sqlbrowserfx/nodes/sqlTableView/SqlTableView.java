package gr.sqlbrowserfx.nodes.sqlTableView;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gr.sqlbrowserfx.conn.SqlConnector;
import gr.sqlbrowserfx.conn.SqlTable;
import gr.sqlbrowserfx.nodes.sqlPane.SqlTableRowEditBox;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import gr.sqlbrowserfx.utils.MemoryGuard;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.CheckBox;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;

//TODO use deleteRow, log log4j in ui
public class SqlTableView extends TableView<MapTableViewRow> {

	protected ObservableList<MapTableViewRow> rows;
	protected SimpleStringProperty titleProperty;
	protected SqlTable sqlTable;
	protected List<String> columns;
	protected SqlConnector sqlConnector;
	double minWidth, prefWidth, maxWidth;
	protected boolean autoResize;
	int currentColumnPos = 0;
	private TableCell<MapTableViewRow, Object> selectedCell;
	protected boolean filledByQuery = false;
	protected boolean areCellsEditableByClick;

	protected final static int NOT_SET = 0;

	private Logger logger = LoggerFactory.getLogger("SQLBROWSER");

	public SqlTableView() {

		rows = FXCollections.observableArrayList();
		sqlConnector = null;
		autoResize = false;
		minWidth = 0;
		prefWidth = 0;
		maxWidth = 0;

		this.setOnKeyPressed(keyEvent -> {
			if (keyEvent.isControlDown()) {
				if (keyEvent.getCode() == KeyCode.LEFT) {
					if (currentColumnPos < getColumns().size() - 1)
						this.scrollToColumn(getColumns().get(currentColumnPos++));;
				}
				if (keyEvent.getCode() == KeyCode.RIGHT) {
					if (currentColumnPos > 0)
						this.scrollToColumn(getColumns().get(currentColumnPos--));;
				}
			}
		});
		
		JavaFXUtils.addMouseScrolling(this);
		titleProperty = new SimpleStringProperty("empty");
	}

	public SqlTableView(ResultSet rs) throws SQLException {
		this();
		setItems(rs);
	}

	public SqlTableView(SqlConnector sqlConnector) throws SQLException {
		this();
		this.sqlConnector = sqlConnector;
	}
	
	public SqlTableView(SqlConnector sqlConnector, ResultSet rs) throws SQLException {
		this();
		this.sqlConnector = sqlConnector;
		setItems(rs);
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
				LinkedHashMap<String, Object> entry = new LinkedHashMap<>();
				for (String columnLabel : sqlTable.getColumns()) {
					entry.put(columnLabel, rs.getObject(columnLabel));
				}
	
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
		this.setColumnWidth(10, NOT_SET, 300);
		this.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
	}
	
	public synchronized void setItemsLater(ResultSet rs) throws SQLException {

		this.filledByQuery = true;
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
				LinkedHashMap<String, Object> entry = new LinkedHashMap<>();
				for (String columnLabel : sqlTable.getColumns()) {
					entry.put(columnLabel, rs.getObject(columnLabel));
				}
	
				rows.add(new MapTableViewRow(entry));
			}
		} catch (Throwable e) {
			rows.clear();
		}

		Platform.runLater(() -> {
			super.setItems(FXCollections.emptyObservableList());
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

			this.autoResizedColumns(autoResize);
			this.setColumnWidth(10, NOT_SET, 300);
			this.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
			super.setItems(rows);
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
	
	public int updateSelectedRow() {
		MapTableViewRow sqlTableRow = this.getSelectionModel().getSelectedItem();
		Set<String> columns = this.getSqlTable().getColumns();
		String query = "update " + this.getTableName() + " set ";
		List<Object> params = new ArrayList<>();

		for (String column : columns) {
			if (!column.equals(this.getPrimaryKey())) {
				String elm = null;
				if (sqlTableRow.get(column) != null)
					elm = sqlTableRow.get(column).toString();
				// type checking
				Object actualValue = null;
				try {
					if (elm != null && !elm.isEmpty())
						actualValue = sqlConnector.castToDBType(this.getSqlTable(), column, elm);
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
					return 0;
				}
				params.add(actualValue);
				query += column + "= ? ,";
			}
		}
		query = query.substring(0, query.length() - 1);
		query += " where " + this.getPrimaryKey() + "= ?";
		params.add(sqlTableRow.get(this.getPrimaryKey()));

		String message = "Executing : " + query + " [ values : " + params.toString() + " ]";
		LoggerFactory.getLogger("SQLBROWSER").debug(message);

		try {
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
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return 0;
		}
		
		return 1;
	}
	
	public int deleteRecord(MapTableViewRow sqlTableRow) {
		String query = "delete from " + this.getTableName() + " where ";
		List<Object> params = new ArrayList<>();
		Set<String> columns = this.getSqlTable().getColumns();
		if (this.getPrimaryKey() != null) {
			params.add(sqlTableRow.get(this.getPrimaryKey()));
			query += this.getPrimaryKey() + "= ?";
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
		Map<String, TextField> map = editBox.getMap();
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
					logger.error(message);
					return;
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
					return;
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

	public void updateRecord(SqlTableRowEditBox editBox, MapTableViewRow sqlTableRow) throws SQLException {
		Set<String> columns = this.getSqlTable().getColumns();
		String query = "update " + this.getTableName() + " set ";
		List<Object> params = new ArrayList<>();

		for (String column : columns) {
			if (!column.equals(this.getPrimaryKey())) {
				TextField elm = editBox.getMap().get(column);
				Object actualValue = null;
				if (elm != null && elm.getText() != null && !elm.getText().equals("")) {
					// type checking
					try {
						actualValue = sqlConnector.castToDBType(this.getSqlTable(), column,
								editBox.getMap().get(column).getText());
						// Class<?> clazz = sqlTableRow.get(label.getText()).getValue().getClass();
						// actualValue = clazz.cast(actualValue);
					} catch (Exception e) {
						logger.error(e.getMessage(), e);
						return;
					}
				}
				params.add(actualValue);
				query += column + "= ? ,";
			}
		}
		query = query.substring(0, query.length() - 1);
		query += " where " + this.getPrimaryKey() + "= ?";
		params.add(sqlTableRow.get(this.getPrimaryKey()));

		String message = "Executing : " + query + " [ values : " + params.toString() + " ]";
		logger.debug(message);

		if (sqlConnector.executeUpdate(query, params) > 0) {
			sqlConnector.executeQuery("select " + StringUtils.join(columns, ",") + " from " + this.getSqlTable().getName() + " where " + this.getPrimaryKey() + " = ?", Arrays.asList(sqlTableRow.get(this.getPrimaryKey())),
				rset -> {
					LinkedHashMap<String, Object> entry = new LinkedHashMap<>();
					for (String columnLabel : sqlTable.getColumns()) {
						entry.put(columnLabel, rset.getObject(columnLabel));
					}
					sqlTableRow.refreshMap(entry);
				});
		}

		// notify listeners
		sqlTableRow.changed();
	}
	
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

	public String getPrimaryKey() {
		return sqlTable.getPrimaryKey();
	}

	public SqlTable getSqlTable() {
		return sqlTable;
	}

	public void setSqlTable(SqlTable sqlTable) {
		this.sqlTable = sqlTable;
	}

	public TableCell<MapTableViewRow, Object> getSelectedCell() {
		return selectedCell;
	}
	
	public void setSelectedCell(TableCell<MapTableViewRow, Object> sqlTableViewEditCell) {
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
