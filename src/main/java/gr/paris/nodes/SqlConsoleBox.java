package gr.paris.nodes;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;

import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.reactfx.Subscription;

import gr.sqlfx.conn.SqlConnector;
import gr.sqlfx.utils.JavaFXUtils;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;

public class SqlConsoleBox extends VBox {

	TextArea historyArea;
	protected TabPane tabPane;
	ProgressIndicator progressIndicator;
	Tab newConsoleTab;
	protected Button executebutton;
	private SqlConnector sqlConnector;
	protected AtomicBoolean sqlQueryRunning;

	public SqlConsoleBox(SqlConnector sqlConnector) {
		this.sqlConnector = sqlConnector;
		sqlQueryRunning = new AtomicBoolean(false);
		progressIndicator = new ProgressIndicator();
		historyArea = new TextArea();

		tabPane = new TabPane();
		newConsoleTab = new Tab("");
		newConsoleTab.setGraphic(JavaFXUtils.icon("/res/add.png"));
		tabPane.setOnMouseClicked(MouseEvent -> addTab());
		newConsoleTab.setClosable(false);
		tabPane.getTabs().add(newConsoleTab);

		executebutton = new Button("Execute", JavaFXUtils.icon("res/bolt.png"));
		executebutton.setOnAction(actionEvent -> executeButonAction());

		SplitPane splitPane = new SplitPane(historyArea, tabPane);
		splitPane.setOrientation(Orientation.VERTICAL);
		historyArea.prefHeightProperty().bind(splitPane.heightProperty().multiply(0.65));
		tabPane.prefHeightProperty().bind(splitPane.heightProperty().multiply(0.35));

		this.getChildren().addAll(splitPane, executebutton);
		splitPane.prefHeightProperty().bind(this.heightProperty());

		// initial create one tab
		addTab();
	}

	private void addTab() {
		if (tabPane.getSelectionModel().getSelectedItem() == newConsoleTab) {
			CodeArea sqlConsoleArea = new CodeArea();
			AtomicReference<Popup> auoCompletePopup = new AtomicReference<Popup>();
			sqlConsoleArea.setOnKeyTyped(event -> this.autoCompleteAction(event, sqlConsoleArea, auoCompletePopup));

			sqlConsoleArea.caretPositionProperty().addListener((observable, oldPosition, newPosition) -> {
				if (auoCompletePopup.get() != null)
					auoCompletePopup.get().hide();
			});
			sqlConsoleArea.setOnKeyPressed(keyEvent -> {
				if (keyEvent.isControlDown() && keyEvent.getCode() == KeyCode.ENTER) {
//					executebutton.getOnAction().handle(new ActionEvent());
					this.executeButonAction();
				}
			});

			// Unsubscribe when not needed
			Subscription subscription = sqlConsoleArea.multiPlainChanges()
													  .successionEnds(Duration.ofMillis(500))
													  .subscribe(ignore -> sqlConsoleArea.setStyleSpans(0, computeHighlighting(sqlConsoleArea.getText())));

			Tab newTab = new Tab("query " + tabPane.getTabs().size(), sqlConsoleArea);
			tabPane.getTabs().add(newTab);
			tabPane.getSelectionModel().select(newTab);
		}
	}

	public void autoCompleteAction(KeyEvent event, CodeArea sqlConsoleArea,  AtomicReference<Popup> auoCompletePopup) {
		String ch = event.getCharacter();
		// for some reason keycode does not work
		if (Character.isLetter(ch.charAt(0)) || (event.isControlDown() && ch.equals(" "))) {
			int position = sqlConsoleArea.getCaretPosition();
			String query = AutoComplete.getQuery(sqlConsoleArea, position);
			if (auoCompletePopup.get() == null) {
				auoCompletePopup.set(new Popup());
			} else {
				auoCompletePopup.get().hide();
			}
			if (!query.trim().isEmpty()) {
				ListView<String> suggestionsList = AutoComplete.getSuggestionsList(query);
				if (suggestionsList.getItems().size() != 0) {
					auoCompletePopup.get().getContent().clear();
					auoCompletePopup.get().getContent().add(suggestionsList);
					Bounds pointer = sqlConsoleArea.caretBoundsProperty().getValue().get();
					auoCompletePopup.get().show(sqlConsoleArea, pointer.getMaxX(), pointer.getMinY());
					suggestionsList.setOnKeyPressed(keyEvent -> {
						if (keyEvent.getCode() == KeyCode.ENTER) {
							AtomicReference<String> word = new AtomicReference<>();
							if (suggestionsList.getSelectionModel().getSelectedItem() != null) {
								word.set(suggestionsList.getSelectionModel().getSelectedItem().toString());
							} else {
								word.set(suggestionsList.getItems().get(0).toString());
							}
							Platform.runLater(() -> {
								sqlConsoleArea.replaceText(position - query.length(), position, word.get());
								sqlConsoleArea.moveTo(position + word.get().length() - query.length());
							});
							auoCompletePopup.get().hide();
						}
						if (keyEvent.getCode() == KeyCode.ESCAPE || keyEvent.getCode() == KeyCode.SPACE) {
							auoCompletePopup.get().hide();
							auoCompletePopup.set(null);
						}
					});
				}
			} else {
				auoCompletePopup.get().hide();
			}
		}
	}

	public void executeButonAction() {
		CodeArea sqlConsoleArea = (CodeArea) tabPane.getSelectionModel().getSelectedItem().getContent();
		String query = sqlConsoleArea.getText();
		if (query.startsWith("select")) {
			sqlConnector.getExecutorService().execute(() -> {
				if (sqlQueryRunning.get())
					return;

				sqlQueryRunning.set(true);
				Platform.runLater(() -> {
					this.getChildren().remove(executebutton);
					this.getChildren().add(progressIndicator);
				});
				try {
					sqlConnector.executeQueryRaw(query, rset -> {
						handleSelectResult(rset);
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
			sqlConnector.getExecutorService().execute(() -> {
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
			});
		}
	}

	protected void handleUpdateResult(int rowsAffected) throws SQLException {
		historyArea.appendText("Query OK (" + rowsAffected + " rows affected)\n");
	}

	protected void handleSelectResult(ResultSet rset) throws SQLException {
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

	public void hanldeException(Exception e) {
		historyArea.appendText(e.getMessage() + "\n");
	}

	private static StyleSpans<Collection<String>> computeHighlighting(String text) {
		Matcher matcher = SyntaxUtils.PATTERN.matcher(text);
		int lastKwEnd = 0;
		StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
		while (matcher.find()) {
			String styleClass = matcher.group("KEYWORD") != null ? "keyword"
					: matcher.group("FUNCTION") != null ? "function"
					: matcher.group("METHOD") != null ? "method"
					: matcher.group("PAREN") != null ? "paren"
//							: matcher.group("BRACE") != null ? "brace"
//									: matcher.group("BRACKET") != null ? "bracket"
											: matcher.group("SEMICOLON") != null ? "semicolon"
													: matcher.group("STRING2") != null ? "string2"
													: matcher.group("STRING") != null ? "string" : null;
//															: matcher.group("COMMENT") != null ? "comment" : null;
			/* never happens */ assert styleClass != null;
			spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
			spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
			lastKwEnd = matcher.end();
		}
		spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
		return spansBuilder.create();
	}

}
