package gr.sqlbrowserfx.nodes;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.controlsfx.control.PopOver;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.slf4j.LoggerFactory;

import gr.sqlbrowserfx.LoggerConf;
import gr.sqlbrowserfx.SqlBrowserFXAppManager;
import gr.sqlbrowserfx.conn.SqlConnector;
import gr.sqlbrowserfx.listeners.SimpleEvent;
import gr.sqlbrowserfx.listeners.SimpleObservable;
import gr.sqlbrowserfx.listeners.SimpleObserver;
import gr.sqlbrowserfx.nodes.codeareas.sql.CSqlCodeArea;
import gr.sqlbrowserfx.nodes.codeareas.sql.SqlCodeArea;
import gr.sqlbrowserfx.nodes.sqlpane.DraggingTabPaneSupport;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

public class SqlConsolePane extends BorderPane implements ToolbarOwner,SimpleObservable<String>{

	private TextArea historyArea;
	private TabPane queryTabPane;
	private ProgressIndicator progressIndicator;
	private Tab newConsoleTab;
	private Button executeButton;
	private CSqlCodeArea codeAreaRef;
	private CheckBox autoCompleteOnTypeCheckBox;
	private CheckBox openInNewTableViewCheckBox;
	private CheckBox wrapTextCheckBox;
	private FlowPane toolbar;
	private FlowPane bottomBar;
	
	private SqlConnector sqlConnector;
	protected AtomicBoolean sqlQueryRunning;
	protected List<SimpleObserver<String>> listeners;
	private Button stopExecutionButton;
	private Button settingsButton;
	private boolean popOverIsShowing = false;


	@SuppressWarnings("unchecked")
	public SqlConsolePane(SqlConnector sqlConnector) {
		this.sqlConnector = sqlConnector;
		sqlQueryRunning = new AtomicBoolean(false);
		progressIndicator = new ProgressIndicator();
		progressIndicator.setMaxSize(32, 32);
		historyArea = new TextArea();
		listeners = new ArrayList<>();

		queryTabPane = new TabPane();
		DraggingTabPaneSupport draggingSupport = new DraggingTabPaneSupport("/icons/thunder.png");
		draggingSupport.addSupport(queryTabPane);
		newConsoleTab = new Tab("");
		newConsoleTab.setGraphic(JavaFXUtils.createIcon("/icons/add.png"));
		queryTabPane.setOnMouseClicked(MouseEvent -> addTab());
		newConsoleTab.setClosable(false);
		queryTabPane.getTabs().add(newConsoleTab);
		queryTabPane.setOnKeyPressed(keyEvent -> {
			if (keyEvent.isControlDown()) {
				switch (keyEvent.getCode()) {
				case N:
					this.createSqlConsoleTab();
					break;
				case D:
//					tabPane.getTabs().remove(tabPane.getSelectionModel().getSelectedItem());
					break;
				default:
					break;
				}
			}
		});

		SplitPane splitPane = new SplitPane(queryTabPane, historyArea);
		splitPane.setOrientation(Orientation.VERTICAL);
		historyArea.prefHeightProperty().bind(splitPane.heightProperty().multiply(0.65));
		queryTabPane.prefHeightProperty().bind(splitPane.heightProperty().multiply(0.35));

		autoCompleteOnTypeCheckBox = new CheckBox("Autocomplete on type");
		autoCompleteOnTypeCheckBox.setSelected(true);
		autoCompleteOnTypeCheckBox.setOnAction(event -> {
			codeAreaRef.setAutoCompleteOnType(autoCompleteOnTypeCheckBox.isSelected());
		});
		
		openInNewTableViewCheckBox = new CheckBox("Open in new table");
		openInNewTableViewCheckBox.setSelected(false);
		
		queryTabPane.getSelectionModel().selectedItemProperty().addListener(
			    (ChangeListener<Tab>) (ov, oldTab, newTab) -> {
			    	if ((VirtualizedScrollPane<SqlCodeArea>)newTab.getContent() != null) {
			    		SqlCodeArea sqlCodeArea = ((VirtualizedScrollPane<SqlCodeArea>)oldTab.getContent() != null) ?((VirtualizedScrollPane<SqlCodeArea>)oldTab.getContent()).getContent()
			    				: null;
			    		if (sqlCodeArea != null) {
				    		sqlCodeArea.stopTextAnalyzerDaemon();
				    	}
			    		sqlCodeArea = ((VirtualizedScrollPane<SqlCodeArea>)newTab.getContent()).getContent();
				    	if (sqlCodeArea != null) {
				    		sqlCodeArea.setAutoCompleteOnType(autoCompleteOnTypeCheckBox.isSelected());
				    		sqlCodeArea.startTextAnalyzerDaemon();
				    	}
			    	}
			    });
		
		wrapTextCheckBox = new CheckBox("Wrap text");
		
		toolbar = this.createToolbar();
		
		this.setCenter(splitPane);
		this.setLeft(toolbar);

		// initial create one tab
		this.addTab();
	}

	@SuppressWarnings("unchecked")
	private void addTab() {
		Tab selectedTab = queryTabPane.getSelectionModel().getSelectedItem();
		if (selectedTab == newConsoleTab) {
			this.createSqlConsoleTab();
		}
		else {
			codeAreaRef = ((VirtualizedScrollPane<CSqlCodeArea>) selectedTab.getContent()).getContent(); 
		}
	}

	private void createSqlConsoleTab() {
		CSqlCodeArea sqlCodeArea = new CSqlCodeArea();
		sqlCodeArea.wrapTextProperty().bind(this.wrapTextCheckBox.selectedProperty());
		sqlCodeArea.setEnterAction(() -> this.executeButonAction());
		sqlCodeArea.addEventHandler(SimpleEvent.EVENT_TYPE, simpleEvent -> SqlConsolePane.this.changed());
		
		VirtualizedScrollPane<CodeArea> scrollPane = new VirtualizedScrollPane<>(sqlCodeArea);
		Tab newTab = new Tab("query " + queryTabPane.getTabs().size(), scrollPane);
		newTab.setOnClosed(event -> sqlCodeArea.stopTextAnalyzerDaemon());

		queryTabPane.getTabs().add(newTab);
		queryTabPane.getSelectionModel().select(newTab);
		codeAreaRef = sqlCodeArea;
		sqlCodeArea.requestFocus();
	}
	
	@Override
	public FlowPane createToolbar() {
		executeButton = new Button("", JavaFXUtils.createIcon("/icons/play.png"));
		executeButton.setTooltip(new Tooltip("Execute"));
		executeButton.setOnAction(actionEvent -> executeButonAction());
		
		stopExecutionButton = new Button("", JavaFXUtils.createIcon("/icons/stop.png"));
		executeButton.setTooltip(new Tooltip("Stop execution"));
		
		settingsButton = new Button("", JavaFXUtils.createIcon("/icons/settings.png"));
		settingsButton.setOnMouseClicked(mouseEvent -> {
			if (!popOverIsShowing) {
				popOverIsShowing = true;
				PopOver popOver = new PopOver(new VBox(autoCompleteOnTypeCheckBox, openInNewTableViewCheckBox, wrapTextCheckBox));
				popOver.setOnHidden(event -> popOverIsShowing = false);
				popOver.show(settingsButton);
			}
		});
		
		FlowPane toolbar = new FlowPane(executeButton, stopExecutionButton, settingsButton);
		toolbar.setOrientation(Orientation.VERTICAL);
		return toolbar;
	}

	@SuppressWarnings("unchecked")
	private CodeArea getSelectedSqlCodeArea() {
		return ((VirtualizedScrollPane<CodeArea>) queryTabPane.getSelectionModel().getSelectedItem().getContent()).getContent();
	}
	
	public String executeButonAction() {
		CodeArea sqlConsoleArea = this.getSelectedSqlCodeArea();
		String query = !sqlConsoleArea.getSelectedText().isEmpty() ? sqlConsoleArea.getSelectedText() : sqlConsoleArea.getText();
		final String fixedQuery = this.fixQuery(query);
		if (fixedQuery.startsWith("select") || fixedQuery.startsWith("SELECT")
				|| fixedQuery.startsWith("show") || fixedQuery.startsWith("SHOW")) {
			sqlConnector.executeAsync(() -> {
				if (sqlQueryRunning.get())
					return;

				sqlQueryRunning.set(true);
				Platform.runLater(() -> {
					executeButton.setDisable(true);
				});
				try {
					sqlConnector.executeCancelableQuery(fixedQuery, rset -> {
						handleSelectResult(fixedQuery, rset);
					}, stmt -> {
						stopExecutionButton.setOnAction(action -> {
							try {
								stmt.cancel();
							} catch (SQLException e) {
								LoggerFactory.getLogger(LoggerConf.LOGGER_NAME).error(e.getMessage());
							}
						});
					});

				} catch (SQLException e) {
					hanldeException(e);
				} finally {
					Platform.runLater(() -> {
						executeButton.setDisable(false);
					});
					sqlQueryRunning.set(false);
				}
			});
		} else if (!fixedQuery.isEmpty()){
			sqlConnector.executeAsync(() -> {
				if (sqlQueryRunning.get())
					return;

				sqlQueryRunning.set(true);
				Platform.runLater(() -> {
					executeButton.setDisable(true);
				});
				try {
					int rowsAffected = sqlConnector.executeUpdate(fixedQuery);
					handleUpdateResult(rowsAffected);

				} catch (SQLException e) {
					hanldeException(e);
				} finally {
					Platform.runLater(() -> {
						executeButton.setDisable(false);
					});
					sqlQueryRunning.set(false);
				}
				
				String queryToLowerCase = fixedQuery.toLowerCase();
				if ((queryToLowerCase.contains("drop") || queryToLowerCase.contains("create"))
						&&
					(queryToLowerCase.contains("table")   ||
					queryToLowerCase.contains("view")     ||
					queryToLowerCase.contains("trigger")  ||
					queryToLowerCase.contains("procedure")||
					queryToLowerCase.contains("function"))
					) {
					this.changed(fixedQuery);
				}
			});
		}
		
		if (!fixedQuery.isEmpty())
			this.saveHistory(fixedQuery);
		
		return fixedQuery;
	}

	private void saveHistory(final String fixedQuery) {
		try {
			SqlBrowserFXAppManager.getConfigSqlConnector().executeUpdate("insert into queries_history (query) values (?)",
					Arrays.asList(fixedQuery));
		} catch (SQLException e) {
			LoggerFactory.getLogger(LoggerConf.LOGGER_NAME).error(e.getMessage());
		}
	}

	private String fixQuery(String query) {
		int spacesNum = 0;
		query = query.replaceAll("\t", "    ");
		for (int i=0; i<query.length(); i++) {
			if (query.charAt(i) == ' ' || query.charAt(i) == '\n') {
				spacesNum++;
			}
			else {
				break;
			}
		}
		query = query.substring(spacesNum, query.length());
		//FIXME find right pattern to ignore comments 
		query = query.replaceAll("--.*\n", "");
		return query;
	}

	protected void handleUpdateResult(int rowsAffected) throws SQLException {
		historyArea.appendText("Query OK (" + rowsAffected + " rows affected)\n");
	}

	protected void handleSelectResult(String query, ResultSet rset) throws SQLException {
		String lines = "";
		while (rset.next()) {
			String line = "";
			ResultSetMetaData rsmd = rset.getMetaData();
			for (int i = 1; i <= rsmd.getColumnCount(); i++) {
				line += rsmd.getColumnName(i) + " : ";
				if (rset.getObject(rsmd.getColumnName(i)) != null)
					line += rset.getObject(rsmd.getColumnName(i)).toString() + ", ";
			}
			line = line.substring(0, line.length() - ", ".length());
			lines += line + "\n";
		}
		historyArea.setText(lines);
	}

	public void hanldeException(SQLException e) {
		historyArea.appendText(e.getMessage() + "\n");
	}

	@Override
	public void changed() {
		listeners.forEach(listener -> listener.onObservaleChange(null));
	}

	@Override
	public void changed(String data) {
		listeners.forEach(listener -> listener.onObservaleChange(data));
		
	}

	@Override
	public void addObserver(SimpleObserver<String> listener) {
		listeners.add(listener);
	}

	@Override
	public void removeObserver(SimpleObserver<String> listener) {
		listeners.remove(listener);
	}

	public boolean openInNewTableView() {
		return openInNewTableViewCheckBox.isSelected();
	}
	
	public CodeArea getCodeAreaRef() {
		return codeAreaRef;
	}

	public TabPane getQueryTabPane() {
		return queryTabPane;
	}

	public void setQueryTabPane(TabPane queryTabPane) {
		this.queryTabPane = queryTabPane;
	}

	public Button getExecutebutton() {
		return executeButton;
	}

	public void setExecutebutton(Button executebutton) {
		this.executeButton = executebutton;
	}

	public FlowPane getToolbar() {
		return toolbar;
	}

	public void setToolbar(FlowPane toolbar) {
		this.toolbar = toolbar;
	}

	public FlowPane getBottomBar() {
		return bottomBar;
	}

	public void setBottomBar(FlowPane bottomBar) {
		this.bottomBar = bottomBar;
	}

	public List<SimpleObserver<String>> getListeners() {
		return listeners;
	}
	

}
