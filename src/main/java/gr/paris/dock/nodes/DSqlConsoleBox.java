package gr.paris.dock.nodes;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.dockfx.DockNode;
import org.slf4j.LoggerFactory;

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
		this.getChildren().addAll(queryTabPane, executebutton);
		queryTabPane.prefHeightProperty().bind(this.heightProperty());
	}

	@Override
	public DockNode asDockNode() {
		return thisDockNode;
	}
	
	@Override
	protected void handleSelectResult(String query, ResultSet rset) throws SQLException {
//		sqlPane.setInProgress();
		sqlPane.getSqlTableView().setItemsLater(rset);
		sqlPane.getSqlTableView().setFilledByQuery(true);
		
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
		LoggerFactory.getLogger("SQLBROWSER").error(e.getMessage(), e);
		DialogFactory.createErrorDialog(e);
		System.gc();
	}

	public DSqlPane getSqlPane() {
		return sqlPane;
	}

	public void setSqlPane(DSqlPane sqlPane) {
		this.sqlPane = sqlPane;
	}
	
	

}
