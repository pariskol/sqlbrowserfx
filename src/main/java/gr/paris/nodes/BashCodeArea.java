package gr.paris.nodes;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.reactfx.Subscription;

import gr.paris.utils.BashSyntaxUtils;
import gr.sqlfx.utils.JavaFXUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Bounds;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Popup;

public class BashCodeArea extends CodeArea{

    private static final int LIST_ITEM_HEIGHT = 30;
    private static final int LIST_MAX_HEIGHT = 120;
    private static final int WORD_LENGTH_LIMIT = 45;
    
	public BashCodeArea() {
		super();
		this.setContextMenu(this.createContextMenu());
		AtomicReference<Popup> auoCompletePopup = new AtomicReference<Popup>();
		this.setOnKeyTyped(event -> this.autoCompleteAction(event, auoCompletePopup));

//		this.setOnKeyPressed(keyEvent -> {
//			if (keyEvent.isControlDown() && keyEvent.getCode() == KeyCode.ENTER) {
////				executebutton.getOnAction().handle(new ActionEvent());
//				enterAction.run();
//			}
//			else if (keyEvent.isControlDown() && keyEvent.getCode() == KeyCode.Q) {
//				//TODO go to query x tab
//			}
//		});
		
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

    public static String getQuery(CodeArea codeArea, int position) {
        int limit = (position > WORD_LENGTH_LIMIT) ? WORD_LENGTH_LIMIT : position;
        String keywords = codeArea.getText().substring(position - limit, position);
        keywords = keywords.replaceAll("\\p{Punct}", " ").trim();
        keywords = keywords.replaceAll("\\n", " ").trim();
        int last = keywords.lastIndexOf(" ");
        return keywords.substring(last + 1);
    }

    public static ListView<String> getSuggestionsList(String query){
        List<String> suggestions = getQuerySuggestions(query);
        ListView<String> suggestionsList = new ListView<>();
        suggestionsList.getItems().clear();
        suggestionsList.getItems().addAll(FXCollections.observableList(new ArrayList<>(new HashSet<>(suggestions))));
        int suggestionsNum = suggestions.size();
        int listViewLength = ((suggestionsNum * LIST_ITEM_HEIGHT) > LIST_MAX_HEIGHT) ? LIST_MAX_HEIGHT : suggestionsNum * LIST_ITEM_HEIGHT;
        suggestionsList.setPrefHeight(listViewLength);
        return suggestionsList;
    }

    private static List<String> getQuerySuggestions(String query) {
        List<String> suggestions = BashSyntaxUtils.KEYWORDS_lIST.parallelStream()
        							.filter(keyword -> keyword.startsWith(query)).collect(Collectors.toList());
//        suggestions.sort(Comparator.comparing(String::length).thenComparing(String::compareToIgnoreCase));
        return suggestions;
    }
    
	private void autoCompleteAction(KeyEvent event, AtomicReference<Popup> auoCompletePopup) {
		String ch = event.getCharacter();
		// for some reason keycode does not work
		if (Character.isLetter(ch.charAt(0)) || (event.isControlDown() && ch.equals(" "))) {
			int position = this.getCaretPosition();
			String query = getQuery(this, position);
			if (auoCompletePopup.get() == null) {
				auoCompletePopup.set(new Popup());
			} else {
				auoCompletePopup.get().hide();
			}
			if (!query.trim().isEmpty()) {
				ListView<String> suggestionsList = getSuggestionsList(query);
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
		Matcher matcher = BashSyntaxUtils.PATTERN.matcher(text);
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
													: matcher.group("STRING") != null ? "string"// : null
															: matcher.group("COMMENT") != null ? "comment" :
																matcher.group("VAR") != null ? "var" :null;
			/* never happens */ assert styleClass != null;
			spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
			spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
			lastKwEnd = matcher.end();
		}
		spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
		return spansBuilder.create();
	}
	
}
