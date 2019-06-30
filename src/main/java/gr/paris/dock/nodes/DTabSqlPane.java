package gr.paris.dock.nodes;

import org.dockfx.DockNode;
import org.dockfx.DockPos;

import gr.paris.dock.Dockable;
import gr.sqlfx.conn.SqlConnector;
import gr.sqlfx.utils.JavaFXUtils;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;

public class DTabSqlPane extends BorderPane implements Dockable {

	private SqlConnector sqlConnector;
	private Tab addTab;
	private DockNode thisDockNode;
	private FlowPane toolbar;
	private TabPane tabPane;
	private DSqlConsoleBox sqlConsoleBox;
	private Button sqlConsoleButton;
	private Button logButton;
	
	public DTabSqlPane(SqlConnector sqlConnector) {
		this.sqlConnector = sqlConnector;
		
		toolbar = this.createToolbar();
		tabPane = new TabPane();
		addTab = new Tab("Add");
		addTab.setGraphic(JavaFXUtils.icon("/res/add.png"));
		addTab.setClosable(false);
		tabPane.getTabs().add(addTab);
		this.createTab();
		tabPane.setOnMouseClicked(MouseEvent -> this.handleMouseEvent());
		
		this.setLeft(toolbar);
		this.setCenter(tabPane);
		thisDockNode = new DockNode(this, "", JavaFXUtils.icon("/res/m-database.png"));
	}

	private void handleMouseEvent() {
		this.createTab();
		Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
		sqlConsoleBox.setSqlPane((DSqlPane) selectedTab.getContent());
	}

	private FlowPane createToolbar() {
		FlowPane toolbar = new FlowPane();
		toolbar.setOrientation(Orientation.VERTICAL);
		sqlConsoleButton = new Button("", JavaFXUtils.icon("/res/console.png"));
		sqlConsoleButton.setOnMouseClicked(mouseEvent -> this.sqlConsoleButtonAction());

		logButton = new Button("", JavaFXUtils.icon("/res/monitor.png"));

		toolbar.getChildren().addAll(sqlConsoleButton, logButton);
		return toolbar;
		
	}

	private void createTab() {
		if (tabPane.getSelectionModel().getSelectedItem() == addTab) {
			DSqlPane dSqlPane = new DSqlPane(sqlConnector);
			Tab tab = new Tab("Empty", dSqlPane);
			tab.textProperty().bind(dSqlPane.asDockNode().titleProperty());
			tabPane.getTabs().add(tab);
			tabPane.getSelectionModel().select(tab);
		}
	}

	@Override
	public DockNode asDockNode() {
		return thisDockNode;
	}
	
	public void sqlConsoleButtonAction() {
		if (sqlConsoleBox == null) {
			sqlConsoleBox = new DSqlConsoleBox(this.sqlConnector, null);
				sqlConsoleBox.asDockNode().setOnClose(() -> sqlConsoleBox = null);
				sqlConsoleBox.asDockNode().setPrefSize(this.getWidth(), this.getHeight()/3);
				sqlConsoleBox.asDockNode().setMaxHeight(1080);
				sqlConsoleBox.asDockNode().dock(this.asDockNode().getDockPane(), DockPos.BOTTOM, thisDockNode);
//			}
		}
	}
}
