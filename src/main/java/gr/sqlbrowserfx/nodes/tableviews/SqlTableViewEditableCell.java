package gr.sqlbrowserfx.nodes.tableviews;

import gr.sqlbrowserfx.conn.SqlConnector;
import gr.sqlbrowserfx.factories.DialogFactory;
import gr.sqlbrowserfx.utils.PropertiesLoader;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TableCell;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TextField;

public class SqlTableViewEditableCell extends TableCell<MapTableViewRow, Object> {

	SqlTableView parentTableView;
	SqlConnector sqlConnector;
	
	public SqlTableViewEditableCell() {
		super();
		this.setAlignment(Pos.CENTER_LEFT);
	}

	public SqlTableViewEditableCell(SqlTableView parentTableView) {
		this();
		this.parentTableView = parentTableView;
		this.setOnMouseClicked(mouseEvent -> {
			parentTableView.setSelectedCell(this);
			if (((Boolean)PropertiesLoader.getProperty("sqlbrowserfx.default.editmode.cell", Boolean.class, false)) && parentTableView.areCellsEditableByClick() && mouseEvent.getClickCount() == 2) {
				this.startEdit();
			}
		});
	}
	
	public SqlTableViewEditableCell(SqlTableView parentTable, SqlConnector sqlConnector) {
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
			try {
				parentTableView.updateSelectedRow();
			} catch (Exception e) {
				parentTableView.getSelectionModel().getSelectedItem().set(column, oldValue);
				DialogFactory.createErrorDialog(e);
			}
		}
	}
	
	@Override
	public void cancelEdit() {
		setText(getText());
		setContentDisplay(ContentDisplay.TEXT_ONLY);
	}

	protected TextField createTextField() {
		TableViewCellEditField textField = new TableViewCellEditField(getText(), this);
		
		if (sqlConnector != null)
			textField.setEditable(true);
		else
			textField.setEditable(false);
		
//		textField.setAlignment(Pos.CENTER);
		textField.setStyle("-fx-border-width: 0.0 0.0 0.0 0.0; -fx-padding: 0;");
		return textField;
	}

	public SqlTableView getParentTable() {
		return parentTableView;
	}
	
	
}