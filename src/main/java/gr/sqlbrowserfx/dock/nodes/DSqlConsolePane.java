package gr.sqlbrowserfx.dock.nodes;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.dockfx.DockNode;
import org.dockfx.DockPos;
import org.fxmisc.flowless.VirtualizedScrollPane;

import gr.sqlbrowserfx.conn.SqlConnector;
import gr.sqlbrowserfx.dock.DockWeights;
import gr.sqlbrowserfx.dock.Dockable;
import gr.sqlbrowserfx.factories.DialogFactory;
import gr.sqlbrowserfx.nodes.SqlConsolePane;
import gr.sqlbrowserfx.nodes.sqlCodeArea.HistorySqlCodeArea;
import gr.sqlbrowserfx.nodes.sqlCodeArea.SqlCodeArea;
import gr.sqlbrowserfx.nodes.sqlPane.SqlPane;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;

public class DSqlConsolePane extends SqlConsolePane implements Dockable{

	private DockNode thisDockNode;
	private SqlPane sqlPane;
	private Button historyButton;
	private boolean historyShowing = false;
	private HistorySqlCodeArea historyCodeArea;
 
	public DSqlConsolePane(SqlConnector sqlConnector, DSqlPane sqlPane) {
		super(sqlConnector);
		historyCodeArea = new HistorySqlCodeArea();
		historyCodeArea.setEditable(false);
		this.sqlPane = sqlPane;
		thisDockNode = new DockNode(this, sqlPane.asDockNode().getTitle() + " : SqlConsole", JavaFXUtils.icon("/res/console.png"));
		this.getChildren().clear();
		this.setCenter(getQueryTabPane());
		this.setBottom(getBottomBar());
		this.setLeft(getToolbar());
		thisDockNode.setOnClose(() -> this.listeners.clear()); 
	}
	
	protected void copyAction(ListView<SqlCodeArea> listView) {
		StringBuilder content = new StringBuilder();

		listView.getSelectionModel().getSelectedItems().forEach(row -> content.append(row.getText() + "\n"));

		StringSelection stringSelection = new StringSelection(content.toString());
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(stringSelection, null);
	}

	@Override
	public DockNode asDockNode() {
		return thisDockNode;
	}
	
	@Override
	public FlowPane createToolbar() {
		FlowPane toolbar = super.createToolbar();
		//FIXME 
		historyButton = new Button("", JavaFXUtils.icon("res/monitor.png"));
		historyButton.setTooltip(new Tooltip("Show history"));
		historyButton.setOnMouseClicked(mouseEvent -> {
			if (!historyShowing) {
				historyShowing = true;
				DockNode dockNode = new DockNode(new VirtualizedScrollPane<SqlCodeArea>(historyCodeArea), "Query history", JavaFXUtils.icon("/res/monitor.png"));
				dockNode.dock(this.asDockNode().getDockPane(), DockPos.RIGHT, this.asDockNode(),DockWeights.asDoubleArrray(0.7f, 0.3f));
				dockNode.setOnClose(() -> historyShowing = false);
			}
		});
		toolbar.getChildren().add(historyButton);
		return toolbar;
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
		});
	}
	
	@Override
	public String executeButonAction() {
		String query = super.executeButonAction();
		historyCodeArea.appendText("\n -- Executed at : " + new Timestamp(System.currentTimeMillis()).toString() + " --\n");
		historyCodeArea.appendText(query);
		historyCodeArea.appendText("\n");
		historyCodeArea.moveTo(historyCodeArea.getLength());
		historyCodeArea.requestFollowCaret();
		return query;
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
