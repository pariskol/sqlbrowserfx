package gr.sqlbrowserfx.dock.nodes;

import gr.sqlbrowserfx.conn.SqlConnector;
import gr.sqlbrowserfx.nodes.sqlpane.SqlPane;
import javafx.scene.layout.FlowPane;

public class DSqlConsolePaneNH extends DSqlConsolePane {

	public DSqlConsolePaneNH(SqlConnector sqlConnector, SqlPane sqlPane) {
		super(sqlConnector, sqlPane);
	}
	
	@Override
	public FlowPane createToolbar() {
		var toolbar = super.createToolbar();
		toolbar.getChildren().remove(historyButton);
		return toolbar;
	}

}
