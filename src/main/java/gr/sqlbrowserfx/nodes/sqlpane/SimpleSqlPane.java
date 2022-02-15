package gr.sqlbrowserfx.nodes.sqlpane;

import gr.sqlbrowserfx.conn.SqlConnector;
import javafx.scene.layout.FlowPane;

public class SimpleSqlPane extends SqlPane {

	public SimpleSqlPane(SqlConnector sqlConnector) {
		super(sqlConnector);
		this.fullModeCheckBox.setSelected(true);
	}
	
	@Override
	public FlowPane createToolbar() {
		var toolbar = super.createToolbar();
		toolbar.getChildren().removeAll(sqlConsoleButton, columnsSettingsButton);
		return toolbar;
	}
}
