package gr.sqlbrowserfx.nodes.codeareas.sql;

import gr.sqlbrowserfx.utils.JavaFXUtils;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;

public class PHistorySqlCodeArea extends HistorySqlCodeArea {
	
	@Override
	protected void setInputMap() {
		// no map
	}
	
	@Override
	public ContextMenu createContextMenu() {
		ContextMenu menu = new ContextMenu();

		MenuItem menuItemCopy = new MenuItem("Copy", JavaFXUtils.createIcon("/icons/copy.png"));
		menuItemCopy.setOnAction(event -> this.copy());
		
		MenuItem menuItemSave = new MenuItem("Save Query", JavaFXUtils.createIcon("/icons/check.png"));
		menuItemSave.setOnAction(action -> this.saveQueryAction());
		
		menu.getItems().addAll(menuItemCopy, menuItemSave);
		return menu;
	}
}
