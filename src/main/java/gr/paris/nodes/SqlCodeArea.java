package gr.paris.nodes;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;

import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.reactfx.Subscription;

import gr.paris.utils.AutoComplete;
import gr.paris.utils.SyntaxUtils;
import gr.sqlfx.utils.JavaFXUtils;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Popup;

public class SqlCodeArea extends CodeArea {

//	private SqlConnector sqlConnector;
//	private AtomicBoolean sqlQueryRunning;
//
//	public SqlCodeArea(SqlConnector sqlConnector) {
//		this();
//		this.sqlConnector = sqlConnector;
//		sqlQueryRunning = new AtomicBoolean(false);
//	}
	
	private Runnable enterAction;

	public SqlCodeArea() {
		super();
		this.setContextMenu(this.createContextMenu());
		AtomicReference<Popup> auoCompletePopup = new AtomicReference<Popup>();
		this.setOnKeyTyped(event -> this.autoCompleteAction(event, auoCompletePopup));

		this.setOnKeyPressed(keyEvent -> {
			if (keyEvent.isControlDown() && keyEvent.getCode() == KeyCode.ENTER) {
//				executebutton.getOnAction().handle(new ActionEvent());
				enterAction.run();
			}
			else if (keyEvent.isControlDown() && keyEvent.getCode() == KeyCode.Q) {
				//TODO go to query x tab
			}
		});
		
		this.caretPositionProperty().addListener((observable, oldPosition, newPosition) -> {
			if (auoCompletePopup.get() != null)
				auoCompletePopup.get().hide();
		});

		// Unsubscribe when not needed
		@SuppressWarnings("unused")
		Subscription subscription = this.multiPlainChanges().successionEnds(Duration.ofMillis(500))
				.subscribe(ignore -> this.setStyleSpans(0, computeHighlighting(this.getText())));

	}

	private ContextMenu createContextMenu() {
		ContextMenu menu = new ContextMenu();

		MenuItem menuItemCopy = new MenuItem("Copy", JavaFXUtils.icon("/res/copy.png"));
		menuItemCopy.setOnAction(event -> this.copy());

		MenuItem menuItemCut = new MenuItem("Cut", JavaFXUtils.icon("/res/cut.png"));
		menuItemCut.setOnAction(event -> this.cut());

		MenuItem menuItemPaste = new MenuItem("Paste", JavaFXUtils.icon("/res/paste.png"));
		menuItemPaste.setOnAction(event -> this.paste());

		menu.getItems().addAll(menuItemCopy, menuItemCut, menuItemPaste);
		return menu;
	}
	
	private void autoCompleteAction(KeyEvent event, AtomicReference<Popup> auoCompletePopup) {
		String ch = event.getCharacter();
		// for some reason keycode does not work
		if (Character.isLetter(ch.charAt(0)) || (event.isControlDown() && ch.equals(" "))) {
			int position = this.getCaretPosition();
			String query = AutoComplete.getQuery(this, position);
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
					Bounds pointer = this.caretBoundsProperty().getValue().get();
					auoCompletePopup.get().show(this, pointer.getMaxX(), pointer.getMinY());
					suggestionsList.setOnKeyPressed(keyEvent -> {
						if (keyEvent.getCode() == KeyCode.ENTER) {
							AtomicReference<String> word = new AtomicReference<>();
							if (suggestionsList.getSelectionModel().getSelectedItem() != null) {
								word.set(suggestionsList.getSelectionModel().getSelectedItem().toString());
							} else {
								word.set(suggestionsList.getItems().get(0).toString());
							}
							Platform.runLater(() -> {
								this.replaceText(position - query.length(), position, word.get());
								this.moveTo(position + word.get().length() - query.length());
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

	private static StyleSpans<Collection<String>> computeHighlighting(String text) {
		Matcher matcher = SyntaxUtils.PATTERN.matcher(text);
		int lastKwEnd = 0;
		StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
		while (matcher.find()) {
			String styleClass = matcher.group("KEYWORD") != null ? "keyword"
					: matcher.group("FUNCTION") != null ? "function"
							: matcher.group("METHOD") != null ? "method" : matcher.group("PAREN") != null ? "paren"
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
	
	public void setEnterAction(Runnable action) {
		enterAction = action;
	}
}
