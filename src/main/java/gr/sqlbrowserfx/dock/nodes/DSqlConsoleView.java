package gr.sqlbrowserfx.dock.nodes;

import org.dockfx.DockNode;

import gr.sqlbrowserfx.conn.SqlConnector;
import gr.sqlbrowserfx.dock.Dockable;
import gr.sqlbrowserfx.nodes.SqlConsoleBox;
import gr.sqlbrowserfx.utils.JavaFXUtils;

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
