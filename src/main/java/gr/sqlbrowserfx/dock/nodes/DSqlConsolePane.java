package gr.sqlbrowserfx.dock.nodes;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.dockfx.DockNode;
import org.dockfx.DockPos;
import org.dockfx.DockWeights;
import org.dockfx.Dockable;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.slf4j.LoggerFactory;

import gr.sqlbrowserfx.LoggerConf;
import gr.sqlbrowserfx.SqlBrowserFXAppManager;
import gr.sqlbrowserfx.conn.SqlConnector;
import gr.sqlbrowserfx.factories.DialogFactory;
import gr.sqlbrowserfx.nodes.SqlConsolePane;
import gr.sqlbrowserfx.nodes.codeareas.sql.HistorySqlCodeArea;
import gr.sqlbrowserfx.nodes.codeareas.sql.SqlCodeArea;
import gr.sqlbrowserfx.nodes.sqlpane.SqlPane;
import gr.sqlbrowserfx.nodes.sqlpane.SqlTableTab;
import gr.sqlbrowserfx.nodes.tableviews.SqlTableView;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import gr.sqlbrowserfx.utils.MemoryGuard;
import gr.sqlbrowserfx.utils.mapper.DTOMapper;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class DSqlConsolePane extends SqlConsolePane implements Dockable{

	private DockNode thisDockNode;
	private DSqlPane sqlPane;
	private Button historyButton;
	private boolean historyShowing = false;
	private HistorySqlCodeArea historyCodeArea;
	private DatePicker datePicker;
 
	public DSqlConsolePane(SqlConnector sqlConnector) {
		this(sqlConnector, null);
	}
	
	public DSqlConsolePane(SqlConnector sqlConnector, DSqlPane sqlPane) {
		super(sqlConnector);
		historyCodeArea = new HistorySqlCodeArea();
		historyCodeArea.setEditable(false);
		datePicker = new DatePicker(LocalDate.now());

		datePicker.setOnAction(actionEvent -> {
            LocalDate date = datePicker.getValue();
            String dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            this.getQueriesHistory(dateStr);

		});
		
		this.getQueriesHistory(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
		
		this.sqlPane = sqlPane;
		this.getChildren().clear();
		this.setCenter(getQueryTabPane());
		this.setBottom(getBottomBar());
		this.setLeft(getToolbar());
	}

	//Async function , runs into another thread
	private void getQueriesHistory(String dateStr) {
		SqlBrowserFXAppManager.getConfigSqlConnector().executeQueryRawAsync("select query,datetime(timestamp,'localtime') timestamp from queries_history "
				+ "where date(datetime(timestamp,'localtime')) = '" + dateStr + "' order by id",
			rset -> {
				StringBuilder history = new StringBuilder();
				while (rset.next()) {
					try {
						Map<String, Object> map = DTOMapper.map(rset);
						history.append("\n--  Executed at : " + map.get("timestamp") + " --\n");
						history.append(map.get("query"));
						history.append("\n");
					} catch (Exception e) {
						LoggerFactory.getLogger(LoggerConf.LOGGER_NAME).error("Could not get query");
					}
				}
				Platform.runLater(() -> historyCodeArea.replaceText(history.toString()));
			}
		);
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
		if (thisDockNode == null) {
			if (sqlPane != null) {
				thisDockNode = new DockNode(sqlPane.asDockNode().getDockPane(), this, sqlPane.asDockNode().getTitle() + " : SqlConsole", JavaFXUtils.createIcon("/icons/console.png"), 600.0, 400.0);
				thisDockNode.setOnClose(() -> this.listeners.clear());
			}
		}
		return thisDockNode;
	}
	
	@Override
	public FlowPane createToolbar() {
		FlowPane toolbar = super.createToolbar();
		historyButton = new Button("", JavaFXUtils.createIcon("/icons/monitor.png"));
		historyButton.setTooltip(new Tooltip("Show history"));
		historyButton.setOnMouseClicked(mouseEvent -> {
			if (!historyShowing) {
				historyShowing = true;
				VirtualizedScrollPane<SqlCodeArea> vsp = new VirtualizedScrollPane<>(historyCodeArea);
				VBox vb = new VBox(datePicker, vsp);
				DockNode dockNode = new DockNode(vb, "Query history", JavaFXUtils.createIcon("/icons/monitor.png"));
				VBox.setVgrow(vsp, Priority.ALWAYS);
				datePicker.prefWidthProperty().unbind();
				datePicker.prefWidthProperty().bind(vb.widthProperty());
				dockNode.dock(this.asDockNode().getDockPane(), DockPos.RIGHT, this.asDockNode(),DockWeights.asDoubleArrray(0.7f, 0.3f));
				dockNode.setOnClose(() -> historyShowing = false);
			}
		});
		toolbar.getChildren().add(historyButton);
		return toolbar;
	}
	
	@Override
	protected void handleSelectResult(String query, ResultSet rset) throws SQLException {
		SqlTableView sqlTableView = sqlPane.getSelectedSqlTableView();
		SqlTableTab tab = sqlPane.getSelectedTableTab();
		if (this.openInNewTableView() || tab == null) {
			tab = sqlPane.addSqlTableTabLater();
			sqlTableView = tab.getSqlTableView();
		}


		sqlTableView.setFilledByQuery(true);
		try {
			sqlTableView.setItemsLater(rset);
		} catch (SQLException e) {
			if (e.getErrorCode() == 9 || e.getErrorCode() == MemoryGuard.SQL_MEMORY_ERROR_CODE) {
				if (openInNewTableView())
					Platform.runLater(() -> sqlPane.getTablesTabPane().getTabs().remove(sqlPane.getSelectedTableTab()));
			}
		}
		
		final SqlTableTab fTab = tab;
		Platform.runLater(() -> {
			if (sqlPane.isInFullMode()) {
				sqlPane.enableFullMode(fTab);
			}
			sqlPane.updateRowsCountLabel();
		});
	}
	
	@Override
	public String executeButonAction() {
		String query = super.executeButonAction();
		if (datePicker.getValue().equals(LocalDate.now())) {
			historyCodeArea.appendText("\n -- Executed at : " + new Timestamp(System.currentTimeMillis()).toString() + " --\n");
			historyCodeArea.appendText(query);
			historyCodeArea.appendText("\n");
			historyCodeArea.moveTo(historyCodeArea.getLength());
			historyCodeArea.requestFollowCaret();
		}
		return query;
	}
	
	@Override
	protected void handleUpdateResult(int rowsAffected) throws SQLException {
		DialogFactory.createNotification("SQL query result", "Query OK (" + rowsAffected + ") rows affected!");
	}
	
	@Override
	public void hanldeException(SQLException e) {
		if (e.getErrorCode() == 9 || e.getErrorCode() == MemoryGuard.SQL_MEMORY_ERROR_CODE) {
			String message = "Not enough memory , try again to run query.\n"+
					"If you are trying to run a select query try to use limit";
			e = new SQLException(message, e);
		}
		DialogFactory.createErrorNotification(e);
		System.gc();
	}

	public SqlPane getSqlPane() {
		return sqlPane;
	}

	public void setSqlPane(DSqlPane sqlPane) {
		this.sqlPane = sqlPane;
	}
	
	

}
