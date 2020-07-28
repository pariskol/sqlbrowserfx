package org.dockfx;

import gr.sqlbrowserfx.nodes.ContextMenuOwner;
import gr.sqlbrowserfx.nodes.sqlpane.DraggingTabPaneSupport;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

public class DockTabPane extends TabPane implements ContextMenuOwner, Dockable {
	
	private DockNode parent;
	
	public DockTabPane() {
		super();
		this.setContextMenu(createContextMenu());
		new DraggingTabPaneSupport().addSupport(this);
	}

	public void addTab(DockNode dockNode) {
		Tab tab = new Tab(dockNode.getTitle(), dockNode);
		tab.setGraphic(dockNode.getGraphic());
		dockNode.setGraphic(null);
		tab.setOnClosed(closeEvent -> {
			Runnable closeAction = dockNode.getOnCloseAction();
			if (closeAction != null)
				closeAction.run();
			undockIfNeccessary();
		});
		this.getTabs().add(tab);
	}

	private void undockIfNeccessary() {
		if (this.getTabs().size() == 1) {
			Tab tab = this.getTabs().get(0);
			this.getTabs().remove(tab);
			DockNode dockNode = (DockNode) tab.getContent();
			dockNode.dock(dockNode.getDockPane(), DockPos.TOP, parent, DockWeights.asDoubleArrray(0.5f, 0.5f));
			dockNode.setGraphic(getGraphicFromTab(tab));
			dockNode.showTitleBar();
			dockNode.getDockPane().undock(parent);
		}
	}
	
	private Node getGraphicFromTab(Tab tab) {
		return ((Label)tab.getGraphic()).getGraphic();
	}
	
	@Override
	public ContextMenu createContextMenu() {

		MenuItem menuItemUndock = new MenuItem("Undock");
		menuItemUndock.setOnAction(event -> {
			Tab selectedTab = getSelectionModel().getSelectedItem();
			DockNode dockNode = (DockNode) selectedTab.getContent();
			this.getTabs().remove(selectedTab);
			dockNode.undock();
			dockNode.setGraphic(getGraphicFromTab(selectedTab));
			dockNode.showTitleBar();
			dockNode.setFloating(true);
			
			undockIfNeccessary();
		});
		
		return new ContextMenu(menuItemUndock);
	}

	@Override
	public DockNode asDockNode() {
		if (parent == null) {
			parent = new DockNode(this, "Grouped View");
			parent.setClosable(false);
		}
		return parent;
	}
}
