package gr.sqlbrowserfx.sqlTableView;

import gr.sqlbrowserfx.conn.SqlConnector;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TableCell;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;

public class SqlTableViewEditCell extends TableCell<SqlTableRow, Object> {

	SqlTableView parentTableView;
	SqlConnector sqlConnector;
	
	public SqlTableViewEditCell() {
		super();
		this.setAlignment(Pos.CENTER);
	}

	public SqlTableViewEditCell(SqlTableView parentTableView) {
		this();
		this.parentTableView = parentTableView;
		this.setOnMouseClicked(mouseEvent -> {
			parentTableView.setSelectedCell(this);
			if (parentTableView.areCellsEditableByClick() && mouseEvent.getClickCount() == 2) {
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
		TablePosition<?, ?> pos = parentTableView.getSelectionModel().getSelectedCells().get(0);
		
		if (pos.getTableColumn().getText().equals(parentTableView.getPrimaryKey())) {
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
		Object oldValue = getText();
		if (newValue != null && !newValue.equals(getText())) {
			String column = this.getTableColumn().getText();
			parentTableView.getSelectionModel().getSelectedItem().set(column, newValue);
			if (parentTableView.updateSelectedRow() == 0) {
				parentTableView.getSelectionModel().getSelectedItem().set(column, oldValue);
			}
//			if (parentTableView.updateSelectedRow() == 1) {
//				this.updateItem(newValue, false);
//			}
//			else {
//				parentTableView.getSelectionModel().getSelectedItem().set(column, oldValue);
//				this.updateItem(oldValue, false);
//			}
		}
	}
	
	@Override
	public void cancelEdit() {
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
		return parentTableView;
	}
	
	
}