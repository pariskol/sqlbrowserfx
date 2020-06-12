package gr.sqlbrowserfx.nodes.tableviews;

import javafx.geometry.Pos;
import javafx.scene.control.TableCell;

public class SqlTableViewCell extends TableCell<MapTableViewRow, Object> {

	public SqlTableViewCell() {
		super();
		this.setAlignment(Pos.CENTER);
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
}