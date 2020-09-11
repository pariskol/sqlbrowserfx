package gr.sqlbrowserfx.nodes.codeareas.sql;

import gr.sqlbrowserfx.nodes.SearchAndReplacePopOver;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;

public class HistorySqlCodeArea extends CSqlCodeArea {

	public HistorySqlCodeArea() {
		super();
		searchAndReplacePopOver = new SearchAndReplacePopOver(this, false);
	}
	
	@Override
	public ContextMenu createContextMenu() {
		ContextMenu menu = new ContextMenu();

		MenuItem menuItemCopy = new MenuItem("Copy", JavaFXUtils.createIcon("/icons/copy.png"));
		menuItemCopy.setOnAction(event -> this.copy());
		
		MenuItem menuItemSearchAndReplace = new MenuItem("Search...", JavaFXUtils.createIcon("/icons/magnify.png"));
		menuItemSearchAndReplace.setOnAction(action -> this.showSearchAndReplacePopup());

		MenuItem menuItemSave = new MenuItem("Save Query", JavaFXUtils.createIcon("/icons/check.png"));
		menuItemSave.setOnAction(action -> this.saveQueryAction());
		
		menu.getItems().addAll(menuItemCopy, menuItemSearchAndReplace, menuItemSave);
		return menu;
	}
}
