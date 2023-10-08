package gr.sqlbrowserfx.dock.nodes;

import org.dockfx.DockNode;
import org.dockfx.Dockable;

import gr.sqlbrowserfx.SqlBrowserFXAppManager;
import gr.sqlbrowserfx.conn.SqlConnector;
import gr.sqlbrowserfx.nodes.DBTreeView;
import gr.sqlbrowserfx.nodes.sqlpane.SqlPane;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

public class DDBTreeView extends DBTreeView implements Dockable {

	private DockNode thisDockNode = null;
	private Menu openInSqlPaneMenu;
	
	public DDBTreeView(String dbPath, SqlConnector sqlConnector) {
		super(dbPath, sqlConnector);
		this.setOnContextMenuRequested(menuRequestedEvent -> {
			openInSqlPaneMenu.getItems().clear();
			int i = 1;
			for (SqlPane sqlPane : SqlBrowserFXAppManager.getActiveSqlPanes()) {
				MenuItem item = new MenuItem("SqlPane " + i++);
				item.setOnAction(actionEvent -> sqlPane.createSqlTableTabWithData(this.getSelectionModel().getSelectedItem().getValue()));
				openInSqlPaneMenu.getItems().add(item);
			}
			this.getContextMenu().show(this, menuRequestedEvent.getScreenX(),menuRequestedEvent.getScreenY());
		});
	}

	public DDBTreeView(String dbPath, SqlConnector sqlConnector, DDBTreePane parent) {
		super(dbPath, sqlConnector, parent);
	}
	
	@Override
	public DockNode asDockNode() {
		if (thisDockNode == null) {
			thisDockNode = new DockNode(this, "Structure", JavaFXUtils.createIcon("/icons/structure.png"));
		}
		return thisDockNode;
	}
	
	@Override
	public ContextMenu createContextMenu() {
		ContextMenu menu = super.createContextMenu();
		openInSqlPaneMenu = new Menu("Open in..." , JavaFXUtils.createIcon("/icons/openTab.png"));
		this.populateSqlPanesMenu();
		openInSqlPaneMenu.disableProperty().bind(this.canSelectedOpenProperty().not());
		menu.getItems().add(0, new SeparatorMenuItem());
		menu.getItems().add(0, openInSqlPaneMenu);
		return menu;
		
	}

	public void populateSqlPanesMenu() {
		openInSqlPaneMenu.getItems().clear();
		for (SqlPane sqlPane : SqlBrowserFXAppManager.getActiveSqlPanes()) {
			MenuItem item = new MenuItem((sqlPane instanceof DSqlPane) ? ((DSqlPane) sqlPane).asDockNode().getTitle() : "SqlPane");
			item.setOnAction(action2 -> {
				if (this.tablesRootItem.getChildren().contains(this.getSelectionModel().getSelectedItem()) ||
					this.viewsRootItem.getChildren().contains(this.getSelectionModel().getSelectedItem())
					)
					sqlPane.createSqlTableTabWithData(this.getSelectionModel().getSelectedItem().getValue());
			});
			openInSqlPaneMenu.getItems().add(item);
		}
	}

}
