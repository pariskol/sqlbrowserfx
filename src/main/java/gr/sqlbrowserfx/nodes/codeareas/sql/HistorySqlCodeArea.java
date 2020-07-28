package gr.sqlbrowserfx.nodes.codeareas.sql;

import gr.sqlbrowserfx.nodes.SearchAndReplacePopOver;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;

public class HistorySqlCodeArea extends SqlCodeArea {

	public HistorySqlCodeArea() {
		super();
		searchAndReplacePopOver = new SearchAndReplacePopOver(this, false);
	}
	
	@Override
	public ContextMenu createContextMenu() {
		ContextMenu menu = new ContextMenu();

		MenuItem menuItemCopy = new MenuItem("Copy", JavaFXUtils.icon("/icons/copy.png"));
		menuItemCopy.setOnAction(event -> this.copy());
		
		MenuItem menuItemSearchAndReplace = new MenuItem("Search...", JavaFXUtils.icon("/icons/magnify.png"));
		menuItemSearchAndReplace.setOnAction(action -> this.showSearchAndReplacePopup());

		menu.getItems().addAll(menuItemCopy, menuItemSearchAndReplace);
		return menu;
	}
}
