package gr.sqlbrowserfx.dock.nodes;

import java.util.ArrayList;
import java.util.List;

import org.dockfx.DockNode;
import org.dockfx.DockPos;
import org.dockfx.DockWeights;
import org.dockfx.Dockable;
import org.fxmisc.richtext.CodeArea;

import gr.sqlbrowserfx.SqlBrowserFXAppManager;
import gr.sqlbrowserfx.conn.SqlConnector;
import gr.sqlbrowserfx.listeners.SimpleObservable;
import gr.sqlbrowserfx.listeners.SimpleObserver;
import gr.sqlbrowserfx.nodes.sqlpane.SqlPane;
import gr.sqlbrowserfx.nodes.sqlpane.SqlTableTab;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.TabPane;

public class DSqlPane extends SqlPane implements Dockable, SimpleObserver<String>, SimpleObservable<String> {

	private List<SimpleObserver<String>> listeners;

	private DockNode thisDockNode = null;
	private DSqlConsolePane sqlConsolePane;
	private DockNode dRecordsTabPane = null;

	public DSqlPane() {
		this(null);
	}

	public DSqlPane(SqlConnector sqlConnector) {
		super(sqlConnector);

		listeners = new ArrayList<>();
	}


	@Override
	protected void sqlConsoleButtonAction() {
		if (sqlConsolePane == null) {
			sqlConsolePane = new DSqlConsolePane(this.sqlConnector, this);
			sqlConsolePane.asDockNode().setOnClose(() -> sqlConsolePane = null);
			sqlConsolePane.asDockNode().dock(this.asDockNode().getDockPane(), DockPos.TOP, this.asDockNode(),
					DockWeights.asDoubleArrray(0.4f, 0.6f));
		}
	}

	public final CodeArea getSqlCodeAreaRef() {
		return sqlConsolePane != null ? sqlConsolePane.getCodeAreaRef() : null;
	}


	@Override
	protected void getDataFromDB(String table, final SqlTableTab sqlTableTab) {
		if (table != null && !table.equals("empty")) {
			super.getDataFromDB(table, sqlTableTab);
		}
	}

	@Override
	public void openInFullMode(final SqlTableTab tab) {
//		super.enableFullMode(tab);
		Platform.runLater(() -> {
//			tab.setContent(guiState.getSqlTableView());
			if (isInFullMode()) {
//				final TabPane recordsTabPane = tab.getRecordsTabPane() != null ?
//						tab.getRecordsTabPane() :
//						this.createRecordsTabPane();
				TabPane recordsTabPane = this.createRecordsTabPane();
				if (dRecordsTabPane == null) {
					dRecordsTabPane = new DockNode(recordsTabPane, this.asDockNode().getTitle() + " : Full mode",
							JavaFXUtils.createIcon("/icons/details.png"));
					dRecordsTabPane.dock(this.asDockNode().getDockPane(), DockPos.RIGHT, this.asDockNode(),
							DockWeights.asDoubleArrray(0.7f, 0.3f));
					dRecordsTabPane.setOnClose(() -> {
						dRecordsTabPane = null;
						this.setFullMode(false);
						this.disableFullMode();
						System.gc();
					});
				} else {
					dRecordsTabPane.setContents(recordsTabPane);
				}
				tab.setRecordsTabPane(recordsTabPane);
			}

			sqlQueryRunning = false;
		});
	}

	@Override
	public void disableFullMode() {
		if (dRecordsTabPane != null) {
			dRecordsTabPane.close();
			
			tablesTabPane.getTabs().forEach(tab -> {
				if (tab instanceof SqlTableTab)
					((SqlTableTab)tab).setRecordsTabPane(null);
			});
		}
//		super.disableFullMode();
	}

	@Override
	protected void tablesTabPaneClickAction() {
		super.tablesTabPaneClickAction();
		if (dRecordsTabPane != null && getSelectedRecordsTabPane() != null)
			dRecordsTabPane.setContents(getSelectedRecordsTabPane());
	}

	@Override
	public DockNode asDockNode() {
		if (thisDockNode == null) {
			thisDockNode = new DockNode(this, "Data Explorer", JavaFXUtils.createIcon("/icons/table.png"));

			thisDockNode.setOnClose(() -> {
				SqlBrowserFXAppManager.unregisterDSqlPane(this);
//				it does not work as expected
//				if (sqlConsoleBox != null)
//					sqlConsoleBox.asDockNode().close();
//				if (dRecordsTabPane != null)
//					dRecordsTabPane.close();
//				if (dLogListView != null)
//					dLogListView.close();
			});
		}
		return thisDockNode;
	}
	
	@Override
	public void onObservableChange(String tableName) {
		if (tablesBox.getSelectionModel().getSelectedItem().equals(tableName))
			tablesBox.getOnAction().handle(new ActionEvent());
	}

	@Override
	public void changed() {
		listeners.forEach(listener -> listener.onObservableChange(getTablesBox().getValue()));
	}

	@Override
	public void changed(String data) {
	}

	@Override
	public void addObserver(SimpleObserver<String> listener) {
		listeners.add(listener);
	}

	@Override
	public void removeObserver(SimpleObserver<String> listener) {
		listeners.remove(listener);
	}

	public void setFullMode(boolean mode) {
		fullModeCheckBox.setSelected(mode);
	}

	public void showConsole() {
		this.sqlConsoleButtonAction();
	}

	public DSqlConsolePane getSqlConsolePane() {
		return sqlConsolePane;
	}

}
