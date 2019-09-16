package gr.sqlbrowserfx.sqlTableView;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.slf4j.LoggerFactory;

import gr.sqlbrowserfx.conn.SqlConnector;
import gr.sqlbrowserfx.conn.SqlTable;
import gr.sqlbrowserfx.factories.DialogFactory;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.CheckBox;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;

public class SqlTableView extends TableView<SqlTableRow> {

	protected ObservableList<SqlTableRow> rows;
//	String tableName;
	protected SimpleStringProperty titleProperty;
	protected SqlTable sqlTable;
	protected List<String> columns;
	protected SqlConnector sqlConnector;
	double minWidth, prefWidth, maxWidth;
//	String primaryKey;
	protected boolean autoResize;
	int i = 0;
	private TableCell<SqlTableRow, Object> selectedCell;
	protected boolean filledByQuery = false;
	protected boolean areCellsEditableByClick;

	protected final static int NOT_SET = 0;
	
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
					if (i < getColumns().size() - 1)
						this.scrollToColumn(getColumns().get(i++));;
				}
				if (keyEvent.getCode() == KeyCode.RIGHT) {
					if (i > 0)
						this.scrollToColumn(getColumns().get(i--));;
				}
			}
		});
		
		titleProperty = new SimpleStringProperty("empty");
//		this.setEditable(true);
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
		String actualName = "";
		for (String table : tablesSet) {
			sqlTable = new SqlTable(table, rsmd);
			actualName += table + ", ";
		}
		actualName = actualName.substring(0, actualName.length() - ", ".length());
		sqlTable = new SqlTable(rsmd);
//		sqlTable.setName(actualName);
		columns = new ArrayList<>(sqlTable.getColumns());

		String primaryKey = sqlTable.getName() != null ? sqlConnector.findPrimaryKey(sqlTable.getName()) : null;
		sqlTable.setPrimaryKey(primaryKey);
		List<String> foreignKeys = sqlConnector.findForeignKeys(sqlTable.getName());
		sqlTable.setForeignKeys(foreignKeys);
		
		Platform.runLater(() -> titleProperty.set(sqlTable.getName()));
		
		while (rs.next()) {
			LinkedHashMap<String, Object> entry = new LinkedHashMap<>();
			for (String columnLabel : sqlTable.getColumns()) {
				entry.put(columnLabel, rs.getObject(columnLabel));
			}

			rows.add(new SqlTableRow(entry));
		}

		List<TableColumn<SqlTableRow, Object>> tableColumns = new ArrayList<>();
		for (String column : sqlTable.getColumns()) {
			TableColumn<SqlTableRow, Object> col = new TableColumn<>(column);
			col.setCellValueFactory(param -> {
				return param.getValue().getObjectProperty(column);
			});
			col.setCellFactory(callback -> {
				return new SqlTableViewEditCell(this, sqlConnector);
			});
//			col.setGraphic(JavaFXUtils.createImageView("res/mini-filter.png"));
//			col.getGraphic().setOnMouseClicked(mouseEvent -> {
//				if (mouseEvent.getButton() == MouseButton.SECONDARY) {
//				mouseEvent.consume();
//				DialogFactory.createInfoDialog("Filter", "Filtered");
//				}
//			});
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
//		super.setItems(FXCollections.emptyObservableList());

		HashSet<String> tablesSet = new HashSet<>();
		ResultSetMetaData rsmd = rs.getMetaData();
		for (int i=1;i<=rsmd.getColumnCount();i++) {
			tablesSet.add(rsmd.getTableName(i));
		}
		String actualName = "";
		for (String table : tablesSet) {
			sqlTable = new SqlTable(table, rsmd);
			actualName += table + ", ";
		}
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
//			logger
		}
		
		while (rs.next()) {
			LinkedHashMap<String, Object> entry = new LinkedHashMap<>();
			for (String columnLabel : sqlTable.getColumns()) {
//				if (tablesSet.size() > 1)
//					entry.put(columnLabel + " (" + sqlTable.getName() + ")", rs.getObject(columnLabel));
//				else
					entry.put(columnLabel, rs.getObject(columnLabel));
			}

			rows.add(new SqlTableRow(entry));
		}

		Platform.runLater(() -> {
			super.setItems(FXCollections.emptyObservableList());
			this.getColumns().clear();
			super.setItems(rows);
			titleProperty.set(sqlTable.getName());
			
			for (String column : sqlTable.getColumns()) {
				TableColumn<SqlTableRow, Object> col = new TableColumn<>(column);
				col.setCellValueFactory(param -> {
					return param.getValue().getObjectProperty(column);
				});
				col.setCellFactory(callback -> {
					return new SqlTableViewEditCell(this, sqlConnector);
				});
				this.getColumns().add(col);
			}

			this.autoResizedColumns(autoResize);
			this.setColumnWidth(10, NOT_SET, 300);
			this.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
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
			this.setColumnWidth(0, 0, 0);
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
		for (TableColumn<SqlTableRow, ?> tableColumn: this.getColumns()) {
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
			TableColumn<SqlTableRow, Object> col = new TableColumn<>(column);
			col.setCellValueFactory(param -> {
				return param.getValue().getObjectProperty(column);
			});
			col.setCellFactory(callback -> {
				return new SqlTableViewCell();
			});
			this.getColumns().add(col);
		}
	}
	
	public void updateSelectedRow() {
		SqlTableRow sqlTableRow = this.getSelectionModel().getSelectedItem();
		Set<String> columns = this.getSqlTable().getColumns();
		String query = "update " + this.getTableName() + " set ";
		List<Object> params = new ArrayList<>();

		for (String column : columns) {
			if (!column.equals(this.getPrimaryKey())) {
				String elm = null;
				if (sqlTableRow.get(column) != null)
					elm = sqlTableRow.get(column).toString();
//				if (elm != null && !elm.getText().equals("")) {
				// type checking
				Object actualValue = null;
				try {
					if (elm != null && !elm.isEmpty())
						actualValue = sqlConnector.castToDBType(this.getSqlTable(), column, elm);
					// Class<?> clazz = sqlTableRow.get(label.getText()).getValue().getClass();
					// actualValue = clazz.cast(actualValue);
				} catch (Exception e) {
					DialogFactory.createErrorDialog(e);
					return;
				}
				params.add(actualValue);
				query += column + "= ? ,";
//				}
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
			DialogFactory.createErrorDialog(e);
		}
	}
	
	public ObservableList<SqlTableRow> getSqlTableRows() {
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

	public TableCell<SqlTableRow, Object> getSelectedCell() {
		return selectedCell;
	}
	
	public void setSelectedCell(TableCell<SqlTableRow, Object> sqlTableViewEditCell) {
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
