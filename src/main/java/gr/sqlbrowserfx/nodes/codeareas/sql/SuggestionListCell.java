package gr.sqlbrowserfx.nodes.codeareas.sql;

import gr.sqlbrowserfx.utils.JavaFXUtils;
import javafx.scene.control.ListCell;

public class SuggestionListCell extends ListCell<String>{

	@Override
	protected void updateItem(String item, boolean empty) {
		super.updateItem(item, empty);
		this.setGraphic(null);
		if (item == null || empty) {
			setText(null);
		} else {
			setText(item.replaceAll("@", ""));
			if(item.contains("()"))
				this.setGraphic(JavaFXUtils.createIcon("/icons/function.png"));
			else if (item.contains("@"))
				this.setGraphic(JavaFXUtils.createIcon("/icons/table.png"));
		}
	}
}
