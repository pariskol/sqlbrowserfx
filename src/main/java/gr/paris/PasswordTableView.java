package gr.paris;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.security.GeneralSecurityException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gr.sqlfx.conn.SqlConnector;
import gr.sqlfx.conn.SqlTable;
import gr.sqlfx.factories.DialogFactory;
import gr.sqlfx.sqlTableView.EditBox;
import gr.sqlfx.sqlTableView.SqlTableRow;
import gr.sqlfx.sqlTableView.SqlTableView;
import gr.sqlfx.sqlTableView.SqlTableViewEditCell;
import gr.sqlfx.utils.JavaFXUtils;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;

public class PasswordTableView extends SqlTableView {

	protected Logger logger = LoggerFactory.getLogger("SQLBROWSER");

	public PasswordTableView(SqlConnector sqlConnector) throws SQLException {
		super(sqlConnector);
		this.setCellsEditableByClick(true);
		this.setContextMenu(this.createContextMenu());
		this.getSelectionModel().setCellSelectionEnabled(true);
	}
	
	public PasswordTableView(SqlConnector sqlConnector, ResultSet rs) throws SQLException {
		super(sqlConnector, rs);
	}

	@SuppressWarnings("unchecked")
	public void configureColumns() {
		for (TableColumn<SqlTableRow, ?> tc : this.getColumns()) {
			if (tc.getText().equals("PASSWORD")) {
				tc.setGraphic(JavaFXUtils.icon("/res/primary-key.png"));
				((TableColumn<SqlTableRow, Object> )tc).setCellFactory(cellFactory -> {
					return new PasswordCell(this, getSqlConnector());
				});
			}
			else if (tc.getText().equals("USERNAME")) {
				tc.setGraphic(JavaFXUtils.icon("/res/user.png"));
			}
			else if (tc.getText().equals("APP")) {
				tc.setGraphic(JavaFXUtils.icon("/res/app.png"));
			}
			else if (tc.getText().equals("COMMENT")) {
				tc.setGraphic(JavaFXUtils.icon("/res/comments.png"));
			}
		}
		
		this.getSelectionModel().setCellSelectionEnabled(true);
	}
	
	private ContextMenu createContextMenu() {
		ContextMenu contextMenu = new ContextMenu();

		MenuItem menuItemCellEdit = new MenuItem("Edit cell", JavaFXUtils.icon("/res/edit.png"));

		menuItemCellEdit.setOnAction(event -> {
			this.getSelectedCell().startEdit();
		});

		MenuItem menuItemCopyCell = new MenuItem("Copy cell", JavaFXUtils.icon("/res/copy.png"));

		menuItemCopyCell.setOnAction(event -> {
			String toCopy = "";
			if (this.getSelectedCell() instanceof PasswordCell) {
				toCopy = ((PasswordCell)this.getSelectedCell()).getRealText();
			}
			else toCopy = this.getSelectedCell().getText();
			
			StringSelection stringSelection = new StringSelection(toCopy);
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			clipboard.setContents(stringSelection, null);
		});

		MenuItem menuItemDelete = new MenuItem("Delete", JavaFXUtils.icon("/res/minus.png"));
		menuItemDelete.setOnAction(event -> {
			{
				ObservableList<SqlTableRow> sqlTableRows = this.getSelectionModel().getSelectedItems();

				if (sqlTableRows.size() == 0)
					return;

				if (DialogFactory.createDeleteDialog(this, sqlTableRows, "Do you want to delete records?",
						null) == 0)
					return;

				List<SqlTableRow> toRemove = new ArrayList<>();
				this.getSqlConnector().executeAsync(() -> {
					for (SqlTableRow sqlTableRow : sqlTableRows) {
						if (this.deleteRecord(sqlTableRow) == 1)
							toRemove.add(sqlTableRow);
					}
					this.getSelectionModel().clearSelection();
					this.getSqlTableRows().removeAll(toRemove);
				});
			}
		});


		contextMenu.getItems().addAll(menuItemCellEdit, menuItemCopyCell,
				menuItemDelete);

		return contextMenu;
	}
	
	public void insertRecord(EditBox editBox) {
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
					actualValue = this.getSqlConnector().castToDBType(this.getSqlTable(), column,
							editBox.getMap().get(column).getText());
				} catch (NumberFormatException e) {
					String message = "Value \"" + editBox.getMap().get(column).getText() + "\" is not valid for column "
							+ column + ", expecting " + this.getSqlTable().getColumnsMap().get(column);
					DialogFactory.createErrorDialog(new Exception(message));
					return;
				} catch (Exception e) {
					DialogFactory.createErrorDialog(e);
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
		try {
			this.getSqlConnector().executeUpdate(query, params);
			this.getSqlTableRows().add(new SqlTableRow(entry));
//			this.sortSqlTableView(this);
		} catch (Exception e) {
			DialogFactory.createErrorDialog(e);
		}

	}
	
	public int deleteRecord(SqlTableRow sqlTableRow) {
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
			this.getSqlConnector().executeUpdate(query, params);
		} catch (Exception e) {
			DialogFactory.createErrorDialog(e);
			return 0;
		}

		return 1;
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
				if (columnLabel.equals("PASSWORD")) {
					try {
						entry.put(columnLabel, Encrypter.decrypt(rs.getBytes(columnLabel)));
					} catch (GeneralSecurityException e) {
						e.printStackTrace();
					}
				}
				else
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
		this.configureColumns();
	}
	
	@Override
	public void updateSelectedRow() {
		SqlTableRow sqlTableRow = this.getSelectionModel().getSelectedItem();
		Set<String> columns = this.getSqlTable().getColumns();
		String query = "update " + this.getTableName() + " set ";
		List<Object> params = new ArrayList<>();

		for (String column : columns) {
			if (!column.equals(this.getPrimaryKey())) {
				Object elm = null;
				if (sqlTableRow.get(column) != null) {
					elm = sqlTableRow.get(column);
					if (elm instanceof byte[]) {
						 params.add(elm);
					}
					else {
						elm = sqlTableRow.get(column).toString();
						System.out.println(elm);
						Object actualValue = null;
						try {
							if (elm != null && !((String)elm).isEmpty())
								actualValue = sqlConnector.castToDBType(this.getSqlTable(), column, (String)elm);
						} catch (Exception e) {
							DialogFactory.createErrorDialog(e);
							return;
						}
						params.add(actualValue);
					}
						
				}
				else
					params.add(elm);
				
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
			DialogFactory.createErrorDialog(e);
		}
	}
	
}
