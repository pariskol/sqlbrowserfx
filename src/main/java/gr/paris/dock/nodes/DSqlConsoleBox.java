package gr.paris.dock.nodes;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.dockfx.DockNode;

import gr.paris.dock.Dockable;
import gr.paris.nodes.SqlConsoleBox;
import gr.sqlfx.conn.SqlConnector;
import gr.sqlfx.factories.DialogFactory;
import gr.sqlfx.utils.JavaFXUtils;
import javafx.application.Platform;

public class DSqlConsoleBox extends SqlConsoleBox implements Dockable{

	private DockNode thisDockNode;
	private DSqlPane sqlPane;

	public DSqlConsoleBox(SqlConnector sqlConnector, DSqlPane sqlPane) {
		super(sqlConnector);
		this.sqlPane = sqlPane;
		thisDockNode = new DockNode(this, "SqlConsole", JavaFXUtils.icon("/res/console.png"));
		this.getChildren().clear();
		this.getChildren().addAll(tabPane, executebutton);
		tabPane.prefHeightProperty().bind(this.heightProperty());
	}

	@Override
	public DockNode asDockNode() {
		return thisDockNode;
	}
	
	@Override
	protected void handleSelectResult(ResultSet rset) throws SQLException {
		
		sqlPane.getSqlTableView().setItemsLater(rset);
		Platform.runLater(() -> {
			sqlPane.fillColumnCheckBoxes();
			if (sqlPane.isFullMode()) {
				sqlPane.closeEditTabs();
				sqlPane.createAddTab();
			}
			sqlPane.asDockNode().setTitle(sqlPane.getSqlTableView().getTableName());
		});
	}
	
	@Override
	protected void handleUpdateResult(int rowsAffected) throws SQLException {
		DialogFactory.createInfoDialog("SQL query result", "Query OK (" + rowsAffected + ") rowsa affected!");
	}
	
	@Override
	public void hanldeException(Exception e) {
		DialogFactory.createErrorDialog(e);
	}

}
