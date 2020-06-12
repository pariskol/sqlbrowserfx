package gr.sqlbrowserfx.nodes.tableviews;

import gr.sqlbrowserfx.conn.SqlConnector;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

public class EditableCell extends TableCell<MapTableViewRow, Object> {

	TableView<?> parentTableView;
	
	public EditableCell() {
		super();
		this.setAlignment(Pos.CENTER);
	}

	public EditableCell(MapTableView parentTableView) {
		this();
		this.parentTableView = parentTableView;
		this.setOnMouseClicked(mouseEvent -> {
//			parentTableView.setSelectedCell(this);
			if (parentTableView.areCellsEditableByClick() && mouseEvent.getClickCount() == 2) {
				this.startEdit();
			}
		});
	}
	
	public EditableCell(MapTableView parentTable, SqlConnector sqlConnector) {
		this(parentTable);
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
//		TablePosition<?, ?> pos = parentTableView.getSelectionModel().getSelectedCells().get(0);
//		
//		if (pos.getTableColumn().getText().equals(parentTableView.getPrimaryKey())) {
//			System.out.println(pos.getTableColumn().getText());
//			return;
//		}
		
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
//		Object oldValue = getText();
//		if (newValue != null && !newValue.equals(getText())) {
//			String column = this.getTableColumn().getText();
//			parentTableView.getSelectionModel().getSelectedItem().set(column, newValue);
//			if (parentTableView.updateSelectedRow() == 0) {
//				parentTableView.getSelectionModel().getSelectedItem().set(column, oldValue);
//			}
//		}
	}
	
	@Override
	public void cancelEdit() {
		setText(getText());
		setContentDisplay(ContentDisplay.TEXT_ONLY);
	}

	protected TextField createTextField() {
		TableViewCellEditField textField = new TableViewCellEditField(getText(), this);
		
		textField.setEditable(true);
		
		textField.setAlignment(Pos.CENTER);
		return textField;
	}

	public TableView<?> getParentTable() {
		return parentTableView;
	}
	
	
}