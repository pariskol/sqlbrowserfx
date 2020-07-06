package gr.sqlbrowserfx.nodes;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.controlsfx.control.PopOver;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.slf4j.LoggerFactory;

import gr.sqlbrowserfx.conn.SqlConnector;
import gr.sqlbrowserfx.listeners.SimpleChangeListener;
import gr.sqlbrowserfx.listeners.SimpleObservable;
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
	private FlowPane toolbar;
	private FlowPane bottomBar;
	
	private SqlConnector sqlConnector;
	protected AtomicBoolean sqlQueryRunning;
	protected List<SimpleChangeListener<String>> listeners;
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
		DraggingTabPaneSupport draggingSupport = new DraggingTabPaneSupport("res/thunder.png");
		draggingSupport.addSupport(queryTabPane);
		newConsoleTab = new Tab("");
		newConsoleTab.setGraphic(JavaFXUtils.icon("/res/add.png"));
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
		openInNewTableViewCheckBox.setSelected(true);
		
		queryTabPane.getSelectionModel().selectedItemProperty().addListener(
			    (ChangeListener<Tab>) (ov, oldTab, newTab) -> {
			    	if ((VirtualizedScrollPane<SqlCodeArea>)newTab.getContent() != null) {
			    		SqlCodeArea sqlCodeArea = ((VirtualizedScrollPane<SqlCodeArea>)newTab.getContent()).getContent();
				    	if (sqlCodeArea != null)
				    		sqlCodeArea.setAutoCompleteOnType(autoCompleteOnTypeCheckBox.isSelected());
			    	}
			    });
		
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
		sqlCodeArea.setEnterAction(() -> this.executeButonAction());

		VirtualizedScrollPane<CodeArea> scrollPane = new VirtualizedScrollPane<>(sqlCodeArea);
		Tab newTab = new Tab("query " + queryTabPane.getTabs().size(), scrollPane);

		queryTabPane.getTabs().add(newTab);
		queryTabPane.getSelectionModel().select(newTab);
		codeAreaRef = sqlCodeArea;
		sqlCodeArea.requestFocus();
	}
	
	@Override
	public FlowPane createToolbar() {
		executeButton = new Button("", JavaFXUtils.icon("res/play.png"));
		executeButton.setTooltip(new Tooltip("Execute"));
		executeButton.setOnAction(actionEvent -> executeButonAction());
		
		stopExecutionButton = new Button("", JavaFXUtils.icon("res/stop.png"));
		executeButton.setTooltip(new Tooltip("Stop execution"));
		
		settingsButton = new Button("", JavaFXUtils.icon("/res/settings.png"));
		settingsButton.setOnMouseClicked(mouseEvent -> {
			if (!popOverIsShowing) {
				popOverIsShowing = true;
				PopOver popOver = new PopOver(new VBox(autoCompleteOnTypeCheckBox, openInNewTableViewCheckBox));
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
		if (fixedQuery.startsWith("select") || fixedQuery.startsWith("SELECT")) {

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
								LoggerFactory.getLogger("SQL-BROWSER").error(e.getMessage());
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
				
				if (fixedQuery.contains("table") || fixedQuery.contains("TABLE") ||
					fixedQuery.contains("view") || fixedQuery.contains("VIEW") ||
					fixedQuery.contains("trigger") || fixedQuery.contains("TRIGGER")) {
					this.changed(fixedQuery);
				}
			});
		}
		
		return fixedQuery;
	}

	private String fixQuery(String query) {
		int spacesNum = 0;
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
		query.replaceAll("--.*\n", "");
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
		listeners.forEach(listener -> listener.onChange(null));
	}

	@Override
	public void changed(String data) {
		listeners.forEach(listener -> listener.onChange(data));
		
	}

	@Override
	public void addListener(SimpleChangeListener<String> listener) {
		listeners.add(listener);
	}

	@Override
	public void removeListener(SimpleChangeListener<String> listener) {
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
	

}
