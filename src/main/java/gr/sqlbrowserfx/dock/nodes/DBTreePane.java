package gr.sqlbrowserfx.dock.nodes;

import org.controlsfx.control.PopOver;
import org.dockfx.DockNode;
import org.dockfx.Dockable;
import org.fxmisc.flowless.VirtualizedScrollPane;

import gr.sqlbrowserfx.conn.SqlConnector;
import gr.sqlbrowserfx.listeners.SimpleEvent;
import gr.sqlbrowserfx.nodes.TableCreationPane;
import gr.sqlbrowserfx.nodes.ToolbarOwner;
import gr.sqlbrowserfx.nodes.codeareas.sql.SqlCodeArea;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;

public class DBTreePane extends BorderPane implements Dockable, ToolbarOwner {

	private FlowPane toolBar;
	private DDBTreeView dbTreeView;
	private DockNode thisDockNode = null;
	private SqlConnector sqlConnector;

	public DBTreePane(String dbPath, SqlConnector sqlConnector) {
		super();
		this.sqlConnector = sqlConnector;
		this.toolBar = this.createToolbar();
		// when dbTreeView is ready fires a simple event 
		this.dbTreeView = new DDBTreeView(dbPath, sqlConnector, this);
		this.dbTreeView.addEventHandler(SimpleEvent.EVENT_TYPE, simpleEvent -> {
			Platform.runLater(() -> this.setCenter(this.dbTreeView));
		});
		this.setLeft(toolBar);
		this.setLoading(true);
	}
	
	public void setLoading(boolean loading) {
		if (loading) {
			ProgressIndicator progressIndicator = new ProgressIndicator();
			progressIndicator.setMaxHeight(40);
			progressIndicator.setMaxWidth(40);
			this.setCenter(progressIndicator);
		}
		else {
			Platform.runLater(() -> this.setCenter(this.dbTreeView));
		}
	}
	
	@Override
	public FlowPane createToolbar() {
		Button searchButton = new Button("", JavaFXUtils.createIcon("/icons/magnify.png"));
		searchButton.setTooltip(new Tooltip("Search in tree"));
		searchButton.setOnAction(actionEvent -> this.dbTreeView.showSearchField());
		
		Button addButton = new Button("", JavaFXUtils.createIcon("/icons/add.png"));
		addButton.setOnAction(actionEvent -> {
			TableCreationPane tableCreationPane = new TableCreationPane(this.sqlConnector);
			tableCreationPane.addObserver(this.dbTreeView);
			JavaFXUtils.applyJMetro(tableCreationPane);
			new DockNode(asDockNode().getDockPane(), tableCreationPane, "Create New Table", JavaFXUtils.createIcon("/icons/add.png"), 1000.0, 600.0);
		});
		addButton.setTooltip(new Tooltip("Open table creator"));

		Button deleteButton = new Button("", JavaFXUtils.createIcon("/icons/minus.png"));
		deleteButton.setTooltip(new Tooltip("Drop"));
		deleteButton.setOnAction(action -> this.dbTreeView.dropAction());
		
		Button scemaDetailsButton = new Button("", JavaFXUtils.createIcon("/icons/details.png"));
		scemaDetailsButton.setTooltip(new Tooltip("Show schema"));
		scemaDetailsButton.setOnAction(actionEvent -> {
			SqlCodeArea codeArea = new SqlCodeArea(this.dbTreeView.copyScemaAction(), false, false);
			VirtualizedScrollPane<SqlCodeArea> scrollPane = new VirtualizedScrollPane<>(codeArea);
			scrollPane.setPrefWidth(500);

			PopOver popOver = new PopOver(scrollPane);
			popOver.setDetachable(false);
			popOver.show(scemaDetailsButton);
		});
		FlowPane toolbar =  new FlowPane(searchButton, addButton, deleteButton, scemaDetailsButton);
		toolbar.setPrefWidth(addButton.getWidth());
		return toolbar;
	}

	@Override
	public DockNode asDockNode() {
		if (thisDockNode == null) {
			thisDockNode = new DockNode(this, "Structure", JavaFXUtils.createIcon("/icons/structure.png"));
		}
		return thisDockNode;
	}
	
	public DDBTreeView getDBTreeView() {
		return dbTreeView;
	}

}
