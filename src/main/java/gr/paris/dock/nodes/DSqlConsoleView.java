package gr.paris.dock.nodes;

import org.dockfx.DockNode;

import gr.paris.dock.Dockable;
import gr.paris.nodes.SqlConsoleBox;
import gr.sqlfx.conn.SqlConnector;
import gr.sqlfx.utils.JavaFXUtils;

public class DSqlConsoleView extends SqlConsoleBox implements Dockable{

	private DockNode thisDockNode = null;

	public DSqlConsoleView(SqlConnector sqlConnector) {
		super(sqlConnector);
		thisDockNode = new DockNode(this, "SqlConsole", JavaFXUtils.icon("/res/console.png"));
	}

	@Override
	public DockNode asDockNode() {
		return thisDockNode;
	}

}
