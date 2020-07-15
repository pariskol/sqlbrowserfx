package gr.sqlbrowserfx.dock.nodes;

import org.dockfx.DockNode;

import gr.sqlbrowserfx.conn.SqlConnector;
import gr.sqlbrowserfx.dock.Dockable;
import gr.sqlbrowserfx.nodes.ContextMenuOwner;
import gr.sqlbrowserfx.nodes.DBTreeView;
import gr.sqlbrowserfx.nodes.TableCreationPane;
import gr.sqlbrowserfx.nodes.ToolbarOwner;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;

public class DBTreePane extends BorderPane implements Dockable, ToolbarOwner, ContextMenuOwner{

	private FlowPane toolBar;
	private DDBTreeView dbTreeView;
	private DockNode thisDockNode = null;
	private SqlConnector sqlConnector;

	public DBTreePane(String dbPath, SqlConnector sqlConnector) {
		super();
		this.sqlConnector = sqlConnector;
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
		addButton.setOnAction(actionEvent -> {
			TableCreationPane tableCreationPane = new TableCreationPane(this.sqlConnector);
			tableCreationPane.addObserver(this.dbTreeView);
			JavaFXUtils.applyJMetro(tableCreationPane);
		    Scene scene = new Scene(tableCreationPane, 1000, 600);
		    for (String styleSheet : this.getScene().getStylesheets())
		  	  scene.getStylesheets().add(styleSheet);
		    Stage stage = new Stage();
		    stage.setTitle("Create New Table");
		    stage.setScene(scene);
		    stage.show();
		});
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
