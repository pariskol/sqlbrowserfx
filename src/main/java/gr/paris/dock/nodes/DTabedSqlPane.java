package gr.paris.dock.nodes;

import org.dockfx.DockNode;

import gr.paris.dock.Dockable;
import gr.sqlfx.conn.SqlConnector;
import gr.sqlfx.utils.JavaFXUtils;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

public class DTabedSqlPane extends TabPane implements Dockable {

	SqlConnector sqlConnector;
	Tab addTab;
	DockNode thisDockNode;
	
	public DTabedSqlPane(SqlConnector sqlConnector) {
		this.sqlConnector = sqlConnector;
		
		addTab = new Tab("Add");
		addTab.setGraphic(JavaFXUtils.icon("/res/add.png"));
		addTab.setClosable(false);
		this.getTabs().add(addTab);
		this.createTab();
		this.setOnMouseClicked(MouseEvent -> this.createTab());
		
		thisDockNode = new DockNode(this, "", JavaFXUtils.icon("/res/database.png"));
	}

	private void createTab() {
		if (this.getSelectionModel().getSelectedItem() == addTab) {
			DSqlPane dSqlPane = new DSqlPane(sqlConnector);
			Tab tab = new Tab("Empty", dSqlPane);
			tab.textProperty().bind(dSqlPane.asDockNode().titleProperty());
			this.getTabs().add(tab);
			this.getSelectionModel().select(tab);
		}
	}

	@Override
	public DockNode asDockNode() {
		return thisDockNode;
	}
}
