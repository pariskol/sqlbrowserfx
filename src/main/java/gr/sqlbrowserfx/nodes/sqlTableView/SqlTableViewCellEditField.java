package gr.sqlbrowserfx.nodes.sqlTableView;

import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;

public class SqlTableViewCellEditField extends TextField{

	public SqlTableViewCellEditField(String text, SqlTableViewEditableCell cell) {
		super(text);
		this.setOnKeyPressed(keyEvent -> {
			if (keyEvent.getCode() == KeyCode.ENTER) {
				cell.commitEdit(this.getText());
			} else if (keyEvent.getCode() == KeyCode.ESCAPE) {
				cell.cancelEdit();
			} else if (keyEvent.isShiftDown() || keyEvent.isControlDown()) {
				if (keyEvent.getCode() == KeyCode.A) {
					this.selectAll();
				}
				else if (keyEvent.getCode() == KeyCode.C && keyEvent.isControlDown()) {
					this.copy();
				}
				else if (keyEvent.getCode() == KeyCode.V && keyEvent.isControlDown()) {
					this.paste();
				}
				else if (keyEvent.getCode() == KeyCode.X && keyEvent.isControlDown()) {
					this.cut();
				}
				else if (keyEvent.getCode() == KeyCode.Z && keyEvent.isControlDown()) {
					this.undo();
				}
				else if (keyEvent.getCode() == KeyCode.Z  && keyEvent.isShiftDown() && keyEvent.isControlDown()) {
					this.redo();
				}
				else if (keyEvent.getCode() == KeyCode.LEFT && keyEvent.isShiftDown() && keyEvent.isControlDown()) {
					this.selectPreviousWord();
				}
				else if(keyEvent.getCode() == KeyCode.RIGHT && keyEvent.isShiftDown() && keyEvent.isControlDown()) {
					this.selectNextWord();
				}
				else if (keyEvent.getCode() == KeyCode.LEFT && keyEvent.isControlDown()) {
					this.previousWord();
				}
				else if(keyEvent.getCode() == KeyCode.RIGHT && keyEvent.isControlDown()) {
					this.nextWord();
				}
				else if (keyEvent.getCode() == KeyCode.LEFT && keyEvent.isShiftDown()) {
					this.selectBackward();
				}
				else if(keyEvent.getCode() == KeyCode.RIGHT && keyEvent.isShiftDown()) {
					this.selectForward();
				}
				
				keyEvent.consume();
			}
		});
//		this.focusedProperty().addListener((ChangeListener<Boolean>) (observable, oldValue, newValue) -> {
//		if (!newValue) {
//			cell.commitEdit(this.getText());
//		}
//	});
	}
}
