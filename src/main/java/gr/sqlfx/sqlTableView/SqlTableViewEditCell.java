package gr.sqlfx.sqlTableView;

import gr.sqlfx.conn.SqlConnector;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TableCell;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;

public class SqlTableViewEditCell extends TableCell<SqlTableRow, Object> {

	SqlTableView parentTable;
	SqlConnector sqlConnector;
	
	public SqlTableViewEditCell() {
		super();
		this.setAlignment(Pos.CENTER);
	}

	public SqlTableViewEditCell(SqlTableView parentTable) {
		this();
		this.parentTable = parentTable;
		this.setOnMouseClicked(mouseEvent -> {
			parentTable.setSelectedCell(this);
			if (parentTable.areCellsEditableByClick() && mouseEvent.getClickCount() == 2) {
				this.startEdit();
			}
		});
	}
	
	public SqlTableViewEditCell(SqlTableView parentTable, SqlConnector sqlConnector) {
		this(parentTable);
		this.sqlConnector = sqlConnector;
	}
	
	@Override
	protected void updateItem(Object item, boolean empty) {
		super.updateItem(item, empty);

		if (item == null || empty) {
			setText(null);
		} else {
			setText(item.toString());
		}
	}
	
	@Override
	public void startEdit() {
		TablePosition<?, ?> pos = parentTable.getSelectionModel().getSelectedCells().get(0);
		
		if (pos.getTableColumn().getText().equals(parentTable.getPrimaryKey())) {
			System.out.println(pos.getTableColumn().getText());
			return;
		}
		
		TextField textField = createTextField();
		setGraphic(textField);
		setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
		Platform.runLater(() -> {
			textField.requestFocus();
			textField.selectAll();
			textField.focusedProperty().addListener((ChangeListener<Boolean>) (observable, oldValue, newValue) -> {
				cancelEdit();
			});
		});
		
	}

	@Override
	public void commitEdit(Object newValue) {
		if (newValue != null && !newValue.equals(getText())) {
			TablePosition<?, ?> pos = parentTable.getSelectionModel().getSelectedCells().get(0);
			String column = parentTable.getColumns().get(pos.getColumn()).getText();
			parentTable.getSelectionModel().getSelectedItem().set(column, newValue);
			parentTable.updateSelectedRow();
		}
		
		super.commitEdit(newValue);
		if (newValue == null) {
			setText(null);
		} else {
			setText(newValue.toString());
		}
		setContentDisplay(ContentDisplay.TEXT_ONLY);
	}
	
	@Override
	public void cancelEdit() {
		super.cancelEdit();
		setText(getText());
		setContentDisplay(ContentDisplay.TEXT_ONLY);
	}

	protected TextField createTextField() {
		TextField textField = new TextField(getText());
		
		if (sqlConnector != null)
			textField.setEditable(true);
		else
			textField.setEditable(false);
		
		textField.setAlignment(Pos.CENTER);
		textField.setOnKeyPressed(keyEvent -> {
			if (keyEvent.getCode() == KeyCode.ENTER) {
				commitEdit(textField.getText());
			} else if (keyEvent.getCode() == KeyCode.ESCAPE) {
				cancelEdit();
			} else if (keyEvent.isShiftDown() || keyEvent.isControlDown()) {
				if (keyEvent.getCode() == KeyCode.A) {
					textField.selectAll();
				}
				else if (keyEvent.getCode() == KeyCode.C && keyEvent.isControlDown()) {
					textField.copy();
				}
				else if (keyEvent.getCode() == KeyCode.V && keyEvent.isControlDown()) {
					textField.paste();
				}
				else if (keyEvent.getCode() == KeyCode.X && keyEvent.isControlDown()) {
					textField.cut();
				}
				else if (keyEvent.getCode() == KeyCode.Z && keyEvent.isControlDown()) {
					textField.undo();
				}
				else if (keyEvent.getCode() == KeyCode.C  && keyEvent.isShiftDown() && keyEvent.isControlDown()) {
					textField.redo();
				}
				keyEvent.consume();
			}
		});
//		textField.focusedProperty().addListener((ChangeListener<Boolean>) (observable, oldValue, newValue) -> {
//			if (!newValue && textField != null) {
//				commitEdit(textField.getText());
//			}
//		});
		
		return textField;
	}

	public SqlTableView getParentTable() {
		return parentTable;
	}
	
	
}