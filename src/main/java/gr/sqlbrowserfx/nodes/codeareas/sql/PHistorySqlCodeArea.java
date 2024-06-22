package gr.sqlbrowserfx.nodes.codeareas.sql;

import gr.sqlbrowserfx.utils.JavaFXUtils;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;

public class PHistorySqlCodeArea extends HistorySqlCodeArea {
	
	@Override
	public void setInputMap() {
		// no map
	}
	
	@Override
	public ContextMenu createContextMenu() {
		var menu = new ContextMenu();

		var menuItemCopy = new MenuItem("Copy", JavaFXUtils.createIcon("/icons/copy.png"));
		menuItemCopy.setOnAction(event -> this.copy());
		
		var menuItemSave = new MenuItem("Save Query", JavaFXUtils.createIcon("/icons/save.png"));
		menuItemSave.setOnAction(action -> this.saveQueryAction());
		
		menu.getItems().addAll(menuItemCopy, menuItemSave);
		return menu;
	}
}
