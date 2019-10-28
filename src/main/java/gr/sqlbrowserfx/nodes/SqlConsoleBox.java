package gr.sqlbrowserfx.nodes;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;

import gr.sqlbrowserfx.conn.SqlConnector;
import gr.sqlbrowserfx.listeners.SimpleChangeListener;
import gr.sqlbrowserfx.listeners.SimpleObservable;
import gr.sqlbrowserfx.nodes.sqlCodeArea.CSqlCodeArea;
import gr.sqlbrowserfx.nodes.sqlCodeArea.SqlCodeArea;
import gr.sqlbrowserfx.nodes.sqlPane.DraggingTabPaneSupport;
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
import javafx.scene.layout.VBox;

public class SqlConsoleBox extends VBox implements SimpleObservable<String>{

	private TextArea historyArea;
	protected TabPane queryTabPane;
	private ProgressIndicator progressIndicator;
	private Tab newConsoleTab;
	protected Button executebutton;
	private CSqlCodeArea codeAreaRef;
	protected CheckBox autoCompleteOnTypeCheckBox;
	
	private SqlConnector sqlConnector;
	protected AtomicBoolean sqlQueryRunning;
	protected List<SimpleChangeListener<String>> listeners;

	@SuppressWarnings("unchecked")
	public SqlConsoleBox(SqlConnector sqlConnector) {
		this.sqlConnector = sqlConnector;
		sqlQueryRunning = new AtomicBoolean(false);
		progressIndicator = new ProgressIndicator();
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
					this.createSqlConsoleBox();
					break;
				case D:
//					tabPane.getTabs().remove(tabPane.getSelectionModel().getSelectedItem());
					break;
				default:
					break;
				}
			}
		});

		executebutton = new Button("Execute", JavaFXUtils.icon("res/bolt.png"));
		executebutton.setOnAction(actionEvent -> executeButonAction());

		SplitPane splitPane = new SplitPane(historyArea, queryTabPane);
		splitPane.setOrientation(Orientation.VERTICAL);
		historyArea.prefHeightProperty().bind(splitPane.heightProperty().multiply(0.65));
		queryTabPane.prefHeightProperty().bind(splitPane.heightProperty().multiply(0.35));

		autoCompleteOnTypeCheckBox = new CheckBox("Autocomplete on type");
		autoCompleteOnTypeCheckBox.setSelected(true);
		autoCompleteOnTypeCheckBox.setOnAction(event -> {
			codeAreaRef.setAutoCompleteOnType(autoCompleteOnTypeCheckBox.isSelected());
		});
		queryTabPane.getSelectionModel().selectedItemProperty().addListener(
			    (ChangeListener<Tab>) (ov, oldTab, newTab) -> {
			    	if ((VirtualizedScrollPane<SqlCodeArea>)newTab.getContent() != null) {
			    		SqlCodeArea sqlCodeArea = ((VirtualizedScrollPane<SqlCodeArea>)newTab.getContent()).getContent();
				    	if (sqlCodeArea != null)
				    		sqlCodeArea.setAutoCompleteOnType(autoCompleteOnTypeCheckBox.isSelected());
			    	}
			    });
		
		this.getChildren().addAll(splitPane, autoCompleteOnTypeCheckBox, executebutton);
		splitPane.prefHeightProperty().bind(this.heightProperty());

		// initial create one tab
		this.addTab();
	}

	@SuppressWarnings("unchecked")
	private void addTab() {
		Tab selectedTab = queryTabPane.getSelectionModel().getSelectedItem();
		if (selectedTab == newConsoleTab) {
			this.createSqlConsoleBox();
		}
		else {
			codeAreaRef = ((VirtualizedScrollPane<CSqlCodeArea>) selectedTab.getContent()).getContent(); 
		}
	}

	private void createSqlConsoleBox() {
		CSqlCodeArea sqlCodeArea = new CSqlCodeArea();
		sqlCodeArea.setEnterAction(() -> this.executeButonAction());

		VirtualizedScrollPane<CodeArea> scrollPane = new VirtualizedScrollPane<>(sqlCodeArea);
		Tab newTab = new Tab("query " + queryTabPane.getTabs().size(), scrollPane);

		queryTabPane.getTabs().add(newTab);
		queryTabPane.getSelectionModel().select(newTab);
		codeAreaRef = sqlCodeArea;
		sqlCodeArea.requestFocus();
	}

	@SuppressWarnings("unchecked")
	private CodeArea getSelectedSqlCodeArea() {
		return ((VirtualizedScrollPane<CodeArea>) queryTabPane.getSelectionModel().getSelectedItem().getContent()).getContent();
	}
	
	public void executeButonAction() {
		CodeArea sqlConsoleArea = this.getSelectedSqlCodeArea();
		String query = !sqlConsoleArea.getSelectedText().isEmpty() ? sqlConsoleArea.getSelectedText() : sqlConsoleArea.getText();
		if (query.startsWith("select") || query.startsWith("SELECT")) {

			sqlConnector.executeAsync(() -> {
				if (sqlQueryRunning.get())
					return;

				sqlQueryRunning.set(true);
				Platform.runLater(() -> {
					this.getChildren().remove(executebutton);
					this.getChildren().add(progressIndicator);
				});
				try {
					sqlConnector.executeQueryRawSafely(query, rset -> {
						handleSelectResult(query, rset);
					});

				} catch (SQLException e) {
					hanldeException(e);
				} finally {
					Platform.runLater(() -> {
						this.getChildren().remove(progressIndicator);
						this.getChildren().add(executebutton);
					});
					sqlQueryRunning.set(false);
				}
			});
		} else {
			sqlConnector.executeAsync(() -> {
				if (sqlQueryRunning.get())
					return;

				sqlQueryRunning.set(true);
				Platform.runLater(() -> {
					this.getChildren().remove(executebutton);
					this.getChildren().add(progressIndicator);
				});
				try {
					int rowsAffected = sqlConnector.executeUpdate(query);
					handleUpdateResult(rowsAffected);

				} catch (SQLException e) {
					hanldeException(e);
				} finally {
					Platform.runLater(() -> {
						this.getChildren().remove(progressIndicator);
						this.getChildren().add(executebutton);
					});
					sqlQueryRunning.set(false);
				}
				
				if (query.contains("table") || query.contains("TABLE") ||
					query.contains("view") || query.contains("VIEW") ||
					query.contains("trigger") || query.contains("TRIGGER")) {
					this.changed(query);
				}
			});
		}
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

	public CodeArea getCodeAreaRef() {
		return codeAreaRef;
	}

}
