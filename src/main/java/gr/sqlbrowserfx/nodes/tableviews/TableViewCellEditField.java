package gr.sqlbrowserfx.nodes.tableviews;

import javafx.scene.control.TableCell;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;

public class TableViewCellEditField extends TextField{

	public TableViewCellEditField(String text, TableCell<MapTableViewRow, Object> cell) {
		super(text);
		this.setOnKeyPressed(keyEvent -> {
			if (keyEvent.getCode() == KeyCode.ENTER) {
				cell.commitEdit(this.getText());
			} else if (keyEvent.getCode() == KeyCode.ESCAPE) {
				cell.cancelEdit();
			} else if (keyEvent.isShiftDown() || keyEvent.isControlDown()) {
				keyEvent.consume();
			}
		});
	}
}
