package gr.sqlbrowserfx.dock.nodes;

import org.dockfx.DockNode;
import org.dockfx.Dockable;

import gr.sqlbrowserfx.conn.SqlConnector;
import gr.sqlbrowserfx.nodes.SqlConsolePane;
import gr.sqlbrowserfx.utils.JavaFXUtils;

public class DSqlConsoleView extends SqlConsolePane implements Dockable{

	private DockNode thisDockNode = null;

	public DSqlConsoleView(SqlConnector sqlConnector) {
		super(sqlConnector);
		thisDockNode = new DockNode(this, "SqlConsole", JavaFXUtils.createIcon("/icons/console.png"));
	}

	@Override
	public DockNode asDockNode() {
		return thisDockNode;
	}

}
