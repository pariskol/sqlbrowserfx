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
	private final SqlPane sqlPane;
	protected Button historyButton;
	private boolean historyShowing = false;
	private final HistorySqlCodeArea historyCodeArea;
	private final DatePicker datePicker;
	private final VBox historyBox;
 
	public DSqlConsolePane(SqlConnector sqlConnector) {
		this(sqlConnector, null);
	}
	
	public DSqlConsolePane(SqlConnector sqlConnector, SqlPane sqlPane) {
		super(sqlConnector);
		historyCodeArea = new HistorySqlCodeArea();
		historyCodeArea.setEditable(false);
		datePicker = new DatePicker(LocalDate.now());

		datePicker.setOnAction(actionEvent -> {
            LocalDate date = datePicker.getValue();
            String dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            this.getQueriesHistory(dateStr);

		});
		VirtualizedScrollPane<SqlCodeArea> vsp = new VirtualizedScrollPane<>(historyCodeArea);
		historyBox = new VBox(datePicker, vsp);
		VBox.setVgrow(vsp, Priority.ALWAYS);
		
		this.getQueriesHistory(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
		
		this.sqlPane = sqlPane;
		this.destroySplitPane();
		this.getChildren().clear();
		this.setCenter(getQueryTabPane());
		this.setLeft(getToolbar());
	}

	//Async function , runs into another thread
	private void getQueriesHistory(String dateStr) {
		SqlBrowserFXAppManager.getConfigSqlConnector().executeQueryRawAsync("select query, duration, datetime(timestamp,'localtime') timestamp from queries_history "
				+ "where date(datetime(timestamp,'localtime')) = '" + dateStr + "' order by id",
			rset -> {
				StringBuilder history = new StringBuilder();
				while (rset.next()) {
					try {
						Map<String, Object> map = DTOMapper.map(rset);
						history.append("\n--  Executed at : ").append(map.get("timestamp")).append(" Duration: ").append(map.get("duration")).append("ms --\n");
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

		listView.getSelectionModel().getSelectedItems().forEach(row -> content.append(row.getText()).append("\n"));

		StringSelection stringSelection = new StringSelection(content.toString());
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(stringSelection, null);
	}

	@Override
	public DockNode asDockNode() {
		if (thisDockNode == null) {
			if (sqlPane != null && sqlPane instanceof DSqlPane dsqlPane) {
				thisDockNode = new DockNode(dsqlPane.asDockNode().getDockPane(), this, dsqlPane.asDockNode().getTitle() + " : Console", JavaFXUtils.createIcon("/icons/console.png"), 600.0, 400.0);
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
				DockNode dockNode = new DockNode(this.asDockNode().getDockPane(), historyBox, "Query History", JavaFXUtils.createIcon("/icons/monitor.png"));
				datePicker.prefWidthProperty().unbind();
				datePicker.prefWidthProperty().bind(historyBox.widthProperty());
				dockNode.setOnClose(() -> historyShowing = false);
			}
		});
		toolbar.getChildren().add(historyButton);
		return toolbar;
	}
	
	@Override
	protected void handleSelectResult(String query, ResultSet rset) {
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
				sqlPane.openInFullMode(fTab);
			}
			sqlPane.updateRowsCountLabel();
		});
	}
	
	@Override
	public String executeButtonAction() {
		String query = super.executeButtonAction();
		if (query != null && datePicker.getValue().equals(LocalDate.now())) {
			historyCodeArea.appendText("\n -- Executed at : " + new Timestamp(System.currentTimeMillis()) + " --\n");
			historyCodeArea.appendText(query);
			historyCodeArea.appendText("\n");
			historyCodeArea.moveTo(historyCodeArea.getLength());
			historyCodeArea.requestFollowCaret();
		}
		return query;
	}
	
	@Override
	protected void handleUpdateResult(int rowsAffected) {
		DialogFactory.createNotification("SQL query result", "Query OK (" + rowsAffected + ") rows affected!");
	}
	
	@Override
	public void hanldeException(SQLException e) {
		if (e.getErrorCode() == 9 || e.getErrorCode() == MemoryGuard.SQL_MEMORY_ERROR_CODE) {
			String message = "Not enough memory. Try to limit the result set";
			e = new SQLException(message, e);
		}
		DialogFactory.createErrorNotification(e);
		System.gc();
	}
}
