package gr.sqlbrowserfx.nodes.codeareas.sql;

import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;

import gr.sqlbrowserfx.nodes.SearchAndReplacePopOver;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import javafx.event.Event;
import javafx.geometry.Bounds;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;

public class HistorySqlCodeArea extends CSqlCodeArea {

	public HistorySqlCodeArea() {
		super();
		searchAndReplacePopOver = new SearchAndReplacePopOver(this, false);
		this.setEditable(false);
		this.enableShowLineNumbers(true);
		this.setOnKeyTyped(keyEvent -> {});
	}
	
	public HistorySqlCodeArea(String text) {
		this();
		this.replaceText(text);
	}
	

	public HBox getSearchAndReplacePopOver() {
		return (HBox) this.searchAndReplacePopOver.getContentNode();
	}
	
	@Override
	public void setInputMap() {
		InputMap<Event> searchAndReplace = InputMap.consume(
				EventPattern.keyPressed(KeyCode.F, KeyCombination.CONTROL_DOWN),
				action -> this.showSearchAndReplacePopup()
        );
		InputMap<Event> goToLine = InputMap.consume(
				EventPattern.keyPressed(KeyCode.L, KeyCombination.CONTROL_DOWN),
				action -> this.showGoToLinePopOver()
        );
		
        Nodes.addInputMap(this, searchAndReplace);
        Nodes.addInputMap(this, goToLine);
	}
	
	// FIXME: For some reason only if this method is overwritten shows the correct pop over
	@Override
	protected void showSearchAndReplacePopup() {
		if (!this.getSelectedText().isEmpty()) {
			searchAndReplacePopOver.getFindField().setText(this.getSelectedText());
			searchAndReplacePopOver.getFindField().selectAll();
		}
		Bounds boundsInScene = this.localToScreen(this.getBoundsInLocal());
		searchAndReplacePopOver.show(getParent(), boundsInScene.getMaxX() - searchAndReplacePopOver.getWidth(),
				boundsInScene.getMinY());
	}
	
	@Override
	public ContextMenu createContextMenu() {
		ContextMenu menu = new ContextMenu();

		MenuItem menuItemCopy = new MenuItem("Copy", JavaFXUtils.createIcon("/icons/copy.png"));
		menuItemCopy.setOnAction(event -> this.copy());
		
		MenuItem menuItemSearchAndReplace = new MenuItem("Search...", JavaFXUtils.createIcon("/icons/magnify.png"));
		menuItemSearchAndReplace.setOnAction(action -> this.showSearchAndReplacePopup());

		MenuItem menuItemSave = new MenuItem("Save Query", JavaFXUtils.createIcon("/icons/save.png"));
		menuItemSave.setOnAction(action -> this.saveQueryAction());
		
		menu.getItems().addAll(menuItemCopy, menuItemSearchAndReplace, menuItemSave);
		return menu;
	}
}
