package gr.paris;

import java.security.GeneralSecurityException;

import org.apache.commons.codec.digest.DigestUtils;

import gr.sqlfx.conn.SqlConnector;
import gr.sqlfx.factories.DialogFactory;
import gr.sqlfx.sqlTableView.SqlTableRow;
import gr.sqlfx.sqlTableView.SqlTableView;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TableCell;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;

public class PasswordCell extends TableCell<SqlTableRow, Object> {

	String realValue;
	SqlTableView parentTable;
	SqlConnector sqlConnector;
	
	public PasswordCell() {
		super();
		this.setAlignment(Pos.CENTER);
	}

	public PasswordCell(SqlTableView parentTable) {
		this();
		this.parentTable = parentTable;
		this.setOnMouseClicked(mouseEvent -> {
			parentTable.setSelectedCell(this);
			if (parentTable.areCellsEditableByClick() && mouseEvent.getClickCount() == 2) {
				this.startEdit();
			}
		});
	}
	
	public PasswordCell(SqlTableView parentTable, SqlConnector sqlConnector) {
		this(parentTable);
		this.sqlConnector = sqlConnector;
	}
	
	@Override
	public void startEdit() {
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
//			System.out.println(DigestUtils.sha1(newValue.toString()));
			TablePosition<?, ?> pos = parentTable.getSelectionModel().getSelectedCells().get(0);
			String column = parentTable.getColumns().get(pos.getColumn()).getText();
			this.realValue = (String)newValue;
			try {
				byte[] encryptedValue = Encrypter.encrypt(realValue);
				parentTable.getSelectionModel().getSelectedItem().set(column, encryptedValue);
				((PasswordTableView)parentTable).updateSelectedRow();
//				this.setText((String)newValue);
				parentTable.getSelectionModel().getSelectedItem().set(column, newValue.toString());
				this.updateItem(encryptedValue, false);
			} catch (GeneralSecurityException e) {
				this.cancelEdit();
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
		TextField textField = new TextField(realValue);
		
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

	@Override
	protected void updateItem(Object item, boolean empty) {
		super.updateItem(item, empty);
		setContentDisplay(ContentDisplay.TEXT_ONLY);
		if (item == null || empty) {
			setText(null);
			setGraphic(null);
		} else {
			try {
					this.realValue = Encrypter.decrypt(((byte[])item));
			} catch (Exception e) {
//				e.printStackTrace();
				System.out.println("Ignored");
			}
			String maskedValue = "";
			for (int i=0; i<item.toString().length(); i++) {
				maskedValue += "*";
			}
			setGraphic(null);
			setText(maskedValue);
			realValue = item.toString();

		}
	}

	public String getRealText() {
		return realValue;
	}
}