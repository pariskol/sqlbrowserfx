package gr.sqlbrowserfx.dock.nodes;

import org.dockfx.DockNode;

import gr.sqlbrowserfx.conn.SqlConnector;
import gr.sqlbrowserfx.dock.Dockable;
import gr.sqlbrowserfx.nodes.ContextMenuOwner;
import gr.sqlbrowserfx.nodes.DBTreeView;
import gr.sqlbrowserfx.nodes.ToolbarOwner;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;

public class DBTreePane extends BorderPane implements Dockable, ToolbarOwner, ContextMenuOwner{

	private FlowPane toolBar;
	private DDBTreeView dbTreeView;
	private DockNode thisDockNode = null;

	public DBTreePane(String dbPath, SqlConnector sqlConnector) {
		super();
		this.toolBar = this.createToolbar();
		this.dbTreeView = new DDBTreeView(dbPath, sqlConnector);
		this.setLeft(toolBar);
		this.setCenter(dbTreeView);
	}
	@Override
	public ContextMenu createContextMenu() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FlowPane createToolbar() {
		Button addButton = new Button("", JavaFXUtils.icon("res/add.png"));
		Button deleteButton = new Button("", JavaFXUtils.icon("res/minus.png"));
		deleteButton.setOnAction(action -> this.dbTreeView.dropAction());
		FlowPane toolbar =  new FlowPane(addButton, deleteButton,
				new Button("", JavaFXUtils.icon("res/details.png")));
		toolbar.setPrefWidth(addButton.getWidth());
		return toolbar;
	}

	@Override
	public DockNode asDockNode() {
		if (thisDockNode == null) {
			thisDockNode = new DockNode(this, "Structure", JavaFXUtils.icon("/res/structure.png"));
		}
		return thisDockNode;
	}
	
	public DBTreeView getDBTreeView() {
		return dbTreeView;
	}

}
