package gr.sqlbrowserfx.dock.nodes;

import java.sql.SQLException;

import org.controlsfx.control.PopOver;
import org.dockfx.DockNode;
import org.dockfx.Dockable;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;

import gr.sqlbrowserfx.conn.MysqlConnector;
import gr.sqlbrowserfx.conn.SqlConnector;
import gr.sqlbrowserfx.conn.SqliteConnector;
import gr.sqlbrowserfx.factories.DialogFactory;
import gr.sqlbrowserfx.listeners.SimpleEvent;
import gr.sqlbrowserfx.nodes.InputMapOwner;
import gr.sqlbrowserfx.nodes.TableCreationPane;
import gr.sqlbrowserfx.nodes.ToolbarOwner;
import gr.sqlbrowserfx.nodes.codeareas.sql.SqlCodeArea;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;

public class DDBTreePane extends BorderPane implements Dockable, ToolbarOwner, InputMapOwner {

	private final FlowPane toolBar;
	private final DDBTreeView dbTreeView;
	private DockNode thisDockNode = null;
	private final SqlConnector sqlConnector;
	private Button searchButton;

	public DDBTreePane(String dbPath, SqlConnector sqlConnector) {
		super();
		this.sqlConnector = sqlConnector;
		this.toolBar = this.createToolbar();
		// when dbTreeView is ready fires a simple event 
		this.dbTreeView = new DDBTreeView(dbPath, sqlConnector, this);
		this.dbTreeView.addEventHandler(SimpleEvent.EVENT_TYPE, simpleEvent -> Platform.runLater(() -> this.setCenter(this.dbTreeView)));
		this.setInputMap();

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
		searchButton = new Button("", JavaFXUtils.createIcon("/icons/magnify.png"));
		searchButton.setTooltip(new Tooltip("Search in tree"));
		searchButton.setOnAction(actionEvent -> this.dbTreeView.showSearchPopup(searchButton));
		
		Button addButton = new Button("", JavaFXUtils.createIcon("/icons/add.png"));
		addButton.setOnAction(actionEvent -> {
			TableCreationPane tableCreationPane = new TableCreationPane(this.sqlConnector);
			tableCreationPane.addObserver(this.dbTreeView);
			JavaFXUtils.applyJMetro(tableCreationPane);
			new DockNode(asDockNode().getDockPane(), tableCreationPane, "Create New Table", JavaFXUtils.createIcon("/icons/add.png"), 1050.0, 600.0);
		});
		addButton.setTooltip(new Tooltip("Open table creator"));

		Button deleteButton = new Button("", JavaFXUtils.createIcon("/icons/minus.png"));
		deleteButton.setTooltip(new Tooltip("Drop"));
		deleteButton.setOnAction(action -> this.dbTreeView.dropAction());
		
		Button scemaDetailsButton = new Button("", JavaFXUtils.createIcon("/icons/details.png"));
		scemaDetailsButton.setTooltip(new Tooltip("Show schema"));
		scemaDetailsButton.setOnAction(actionEvent -> {
			SqlCodeArea codeArea = new SqlCodeArea(this.dbTreeView.copyScemaAction(), false, false, isUsingMysql());
			VirtualizedScrollPane<SqlCodeArea> scrollPane = new VirtualizedScrollPane<>(codeArea);
			scrollPane.setPrefSize(600, 400);

			PopOver popOver = new PopOver(scrollPane);
			popOver.setArrowSize(0);
			popOver.setDetachable(false);
			popOver.show(scemaDetailsButton);
		});
		
		Button refreshButton = new Button("", JavaFXUtils.createIcon("/icons/refresh.png"));
		refreshButton.setOnAction(event -> {
			try {
				dbTreeView.refreshItems();
				if (!(sqlConnector instanceof SqliteConnector))
					dbTreeView.refreshFunctionAndProcedures();
			} catch (SQLException e) {
				DialogFactory.createErrorDialog(e);
			}
		});
		refreshButton.setTooltip(new Tooltip("Refresh"));
		FlowPane toolbar =  new FlowPane(searchButton, addButton, deleteButton, scemaDetailsButton, refreshButton);
		toolbar.setPrefWidth(addButton.getWidth());
		return toolbar;
	}

	@Override
	public void setInputMap() {
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.F, KeyCombination.CONTROL_DOWN),
				action -> dbTreeView.showSearchPopup(searchButton)));
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

	private boolean isUsingMysql() {
		return sqlConnector instanceof MysqlConnector;
	}
}
