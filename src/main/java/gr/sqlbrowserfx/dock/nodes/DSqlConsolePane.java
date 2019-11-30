package gr.sqlbrowserfx.dock.nodes;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.dockfx.DockNode;

import gr.sqlbrowserfx.conn.SqlConnector;
import gr.sqlbrowserfx.dock.Dockable;
import gr.sqlbrowserfx.factories.DialogFactory;
import gr.sqlbrowserfx.nodes.SqlConsolePane;
import gr.sqlbrowserfx.nodes.sqlPane.SqlPane;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import javafx.application.Platform;

public class DSqlConsolePane extends SqlConsolePane implements Dockable{

	private DockNode thisDockNode;
	private SqlPane sqlPane;

	public DSqlConsolePane(SqlConnector sqlConnector, DSqlPane sqlPane) {
		super(sqlConnector);
		this.sqlPane = sqlPane;
		thisDockNode = new DockNode(this, sqlPane.asDockNode().getTitle() + " : SqlConsole", JavaFXUtils.icon("/res/console.png"));
		this.getChildren().clear();
		this.setCenter(getQueryTabPane());
		this.setBottom(getBottomBar());
		this.setLeft(getToolbar());
		thisDockNode.setOnClose(() -> this.listeners.clear()); 
	}

	@Override
	public DockNode asDockNode() {
		return thisDockNode;
	}
	
	@Override
	protected void handleSelectResult(String query, ResultSet rset) throws SQLException {
//		sqlPane.setInProgress();
		sqlPane.getSelectedSqlTableView().setItemsLater(rset);
		sqlPane.getSelectedSqlTableView().setFilledByQuery(true);
		
		Platform.runLater(() -> {
			sqlPane.fillColumnCheckBoxes();
			if (sqlPane.isFullMode()) {
				sqlPane.enableFullMode();
			}
			sqlPane.updateRowsCountLabel();
//			sqlPane.asDockNode().setTitle(sqlPane.getSqlTableView().getTableName());
		});
	}
	
	@Override
	protected void handleUpdateResult(int rowsAffected) throws SQLException {
		DialogFactory.createInfoDialog("SQL query result", "Query OK (" + rowsAffected + ") rows affected!");
	}
	
	@Override
	public void hanldeException(SQLException e) {
		if (e.getErrorCode() == 9) {
			String message = "Not enough memory , try again to run query.\n"+
					"If you are trying to run a select query try to use limit";
			e = new SQLException(message, e);
		}
		DialogFactory.createErrorDialog(e);
		System.gc();
	}

	public SqlPane getSqlPane() {
		return sqlPane;
	}

	public void setSqlPane(DSqlPane sqlPane) {
		this.sqlPane = sqlPane;
	}
	
	

}
