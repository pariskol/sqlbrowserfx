package gr.sqlbrowserfx.nodes.sqlcodearea;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;

import org.apache.commons.lang3.StringUtils;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.reactfx.Subscription;

import gr.sqlbrowserfx.nodes.ContextMenuOwner;
import gr.sqlbrowserfx.nodes.SearchAndReplacePopOver;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Bounds;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Popup;

public class SqlCodeArea extends CodeArea implements ContextMenuOwner {

	private static final int LIST_ITEM_HEIGHT = 30;
	private static final int LIST_MAX_HEIGHT = 120;

	private Runnable enterAction;
	private boolean auoCompletePopupShowing = false;
	private AtomicReference<Popup> auoCompletePopup;
	private boolean autoCompleteOnType = true;
	protected SearchAndReplacePopOver searchAndReplacePopOver;

	public SqlCodeArea() {
		this(null);
	}
	
	public SqlCodeArea(String text) {
		auoCompletePopup = new AtomicReference<Popup>();
		searchAndReplacePopOver = new SearchAndReplacePopOver(this);

		this.setOnKeyTyped(keyEvent -> this.autoCompleteAction(keyEvent, auoCompletePopup));

		this.setContextMenu(this.createContextMenu());
		
		this.setOnKeyPressed(keyEvent -> {
			if (keyEvent.isControlDown() && keyEvent.getCode() == KeyCode.ENTER) {
				enterAction.run();
			} 
			else if (keyEvent.getCode() == KeyCode.BACK_SPACE) {
				if (auoCompletePopupShowing) {
					auoCompletePopup.get().hide();
					auoCompletePopup.set(null);
					auoCompletePopupShowing = false;
				}
				// uncomment this to activate autocomplete on backspace
//				this.autoCompleteAction(keyEvent, auoCompletePopup);
			
			}
			else if (keyEvent.isControlDown() && keyEvent.getCode() == KeyCode.Q) {
				// TODO go to query x tab
			}
			else if (keyEvent.isControlDown() && keyEvent.getCode() == KeyCode.F) {
				this.showSearchAndReplacePopup();
			}
			else if (keyEvent.isControlDown() && keyEvent.getCode() == KeyCode.D) {
				boolean hasInitialSelectedText = false;
				if (this.getSelectedText().isEmpty())
					this.selectLine();
				else
					hasInitialSelectedText = true;
				
				this.replaceSelection("");
				
				if (!hasInitialSelectedText && this.getCaretPosition() != 0) {
					this.deletePreviousChar();
					this.moveTo(this.getCaretPosition() + 1);
				}
			}
		});

		this.setOnMouseClicked(mouseEvent -> {
			if (auoCompletePopupShowing) {
				auoCompletePopup.get().hide();
				auoCompletePopupShowing = false;
			}
			searchAndReplacePopOver.hide();
		});

		this.enableHighlighting();
	}
	
	private int countLines(String str) {
		String[] lines = str.split("\r\n|\r|\n");
		return lines.length;
	}
	
	public SqlCodeArea(String text, boolean editable, boolean withMenu) {
		super();

		if (!withMenu)
			this.setContextMenu(null);
		
		this.enableHighlighting();
		if (text != null) {
			this.replaceText(text);
			this.setPrefHeight(countLines(text)*18);
		}
		
		this.setEditable(editable);
		

		this.setOnMouseClicked(mouseEvent -> {
			this.requestFocus();
			if (mouseEvent.getClickCount() == 2) {
				this.selectAll();
			}
		});
		this.focusedProperty().addListener((ov, oldV, newV) -> {
			if (!newV) { // focus lost
				this.deselect();
			}
		});
	}

	private void enableHighlighting() {
		@SuppressWarnings("unused")
		Subscription subscription = this.multiPlainChanges().successionEnds(Duration.ofMillis(100))
				.subscribe(ignore -> this.setStyleSpans(0, computeHighlighting(this.getText())));
	}
	
	protected void showSearchAndReplacePopup() {
		if (!this.getSelectedText().isEmpty()) {
			searchAndReplacePopOver.getFindField().setText(this.getSelectedText());
			searchAndReplacePopOver.getFindField().selectAll();
		}
		Bounds boundsInScene = this.localToScreen(this.getBoundsInLocal());
		searchAndReplacePopOver.show(this, boundsInScene.getMaxX() - searchAndReplacePopOver.getWidth(),
				boundsInScene.getMinY());
	}

	@Override
	public ContextMenu createContextMenu() {
		ContextMenu menu = new ContextMenu();

		MenuItem menuItemCopy = new MenuItem("Copy", JavaFXUtils.icon("/res/copy.png"));
		menuItemCopy.setOnAction(event -> this.copy());

		MenuItem menuItemCut = new MenuItem("Cut", JavaFXUtils.icon("/res/cut.png"));
		menuItemCut.setOnAction(event -> this.cut());

		MenuItem menuItemPaste = new MenuItem("Paste", JavaFXUtils.icon("/res/paste.png"));
		menuItemPaste.setOnAction(event -> this.paste());

		MenuItem menuItemSuggestions = new MenuItem("Suggestions", JavaFXUtils.icon("/res/suggestion.png"));
		menuItemSuggestions
				.setOnAction(event -> this.autoCompleteAction(this.simulateControlSpaceEvent(), auoCompletePopup));

		MenuItem menuItemSearchAndReplace = new MenuItem("Search...", JavaFXUtils.icon("/res/magnify.png"));
		menuItemSearchAndReplace.setOnAction(action -> this.showSearchAndReplacePopup());

		menu.getItems().addAll(menuItemCopy, menuItemCut, menuItemPaste, menuItemSuggestions, menuItemSearchAndReplace);
		return menu;
	}

	private KeyEvent simulateControlSpaceEvent() {
		return new KeyEvent(KeyEvent.KEY_TYPED, " ", " ", null, false, true, false, false);
	}

	private ListView<String> createListView(List<String> suggestions) {
		ListView<String> suggestionsList = new ListView<>();
		if (suggestions != null) {
			suggestionsList.getItems().addAll(FXCollections.observableList(new ArrayList<>(new HashSet<>(suggestions))));
			int suggestionsNum = suggestions.size();
			int listViewLength = ((suggestionsNum * LIST_ITEM_HEIGHT) > LIST_MAX_HEIGHT) ? LIST_MAX_HEIGHT
					: suggestionsNum * LIST_ITEM_HEIGHT;
			suggestionsList.setPrefHeight(listViewLength);
		}
		return suggestionsList;
	}

	private void autoCompleteAction(KeyEvent event, AtomicReference<Popup> auoCompletePopup) {
		AtomicBoolean insertMode = new AtomicBoolean(false);
		String ch = event.getCharacter();
		// for some reason keycode does not work
		if ((Character.isLetter(ch.charAt(0)) && autoCompleteOnType && !event.isControlDown())
				|| (event.isControlDown() && ch.equals(" ")) || ch.equals(".")
				|| event.getCode() == KeyCode.BACK_SPACE) {
			int caretPosition = this.getCaretPosition();
			String query = CodeAreaAutoComplete.getQuery(this, caretPosition);
			if (auoCompletePopup.get() == null) {
				Popup popup = new Popup();
				popup.setAutoHide(true);
				popup.setOnAutoHide(event2 -> auoCompletePopupShowing = false);
				auoCompletePopup.set(popup);
			}

			if (!query.trim().isEmpty()) {
				List<String> suggestions = null;
				if (ch.equals(".")) {
					insertMode.set(true);
					query = "";
					ch = "";
					caretPosition--;
					do {
						ch = this.getText(caretPosition - 1, caretPosition--);
						query += ch;
					} while (!ch.equals(" ") && !(caretPosition == 0));

					query = StringUtils.reverse(query).trim();
					suggestions = CodeAreaAutoComplete.getColumnsSuggestions(query);
				} else
					suggestions = CodeAreaAutoComplete.getQuerySuggestions(query);

				ListView<String> suggestionsList = this.createListView(suggestions);
				if (suggestionsList.getItems().size() != 0) {
					auoCompletePopup.get().getContent().setAll(suggestionsList);
					Bounds pointer = this.caretBoundsProperty().getValue().get();
					if (!auoCompletePopupShowing) {
						auoCompletePopup.get().show(this, pointer.getMaxX(), pointer.getMinY() + 20);
						auoCompletePopupShowing = true;
					}
					final String fQuery = query;
					final int fCaretPosition = caretPosition;
					suggestionsList.setOnKeyPressed(keyEvent -> {
						if (keyEvent.getCode() == KeyCode.ENTER) {
							AtomicReference<String> word = new AtomicReference<>();
							if (suggestionsList.getSelectionModel().getSelectedItem() != null) {
								word.set(suggestionsList.getSelectionModel().getSelectedItem().toString());
							} else {
								word.set(suggestionsList.getItems().get(0).toString());
							}

							Platform.runLater(() -> {
								if (insertMode.get()) {
									this.insertText(this.getCaretPosition(), word.get());
								}
								else {
									this.replaceText(fCaretPosition - fQuery.length(), fCaretPosition, word.get());
									this.moveTo(fCaretPosition + word.get().length() - fQuery.length());
								}
							});
							auoCompletePopup.get().hide();
							auoCompletePopupShowing = false;
						}
						if (keyEvent.getCode() == KeyCode.ESCAPE || keyEvent.getCode() == KeyCode.SPACE) {
							auoCompletePopup.get().hide();
							auoCompletePopupShowing = false;
						}
					});
				} else {
					auoCompletePopup.get().hide();
					auoCompletePopupShowing = false;
				}
			} else {
				auoCompletePopup.get().hide();
				auoCompletePopupShowing = false;
			}
		} else if (!event.isControlDown()) {
			if (auoCompletePopup.get() != null) {
				auoCompletePopup.get().hide();
				auoCompletePopupShowing = false;
			}
		}
	}

	private static StyleSpans<Collection<String>> computeHighlighting(String text) {
		Matcher matcher = CodeAreaSyntax.PATTERN.matcher(text);
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
													: matcher.group("STRING") != null ? "string"
															: matcher.group("COMMENT") != null ? "comment" : null;
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

	public boolean isAutoCompleteOnTypeEnabled() {
		return autoCompleteOnType;
	}

	public void setAutoCompleteOnType(boolean autoCompleteOnType) {
		this.autoCompleteOnType = autoCompleteOnType;
	}

}
