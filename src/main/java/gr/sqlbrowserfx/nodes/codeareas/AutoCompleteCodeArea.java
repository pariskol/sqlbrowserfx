package gr.sqlbrowserfx.nodes.codeareas;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.PopOver;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;
import org.reactfx.Subscription;

import gr.sqlbrowserfx.factories.DialogFactory;
import gr.sqlbrowserfx.nodes.ContextMenuOwner;
import gr.sqlbrowserfx.nodes.SearchAndReplacePopOver;
import gr.sqlbrowserfx.nodes.codeareas.sql.SimpleLineNumberFactory;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.event.Event;
import javafx.geometry.Bounds;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.stage.FileChooser;
import javafx.stage.Popup;

@SuppressWarnings("rawtypes")
public abstract class AutoCompleteCodeArea<T extends CodeAreaSyntaxProvider> extends CodeArea
		implements ContextMenuOwner, HighLighter {

	// this is a random offset that achieves to locate correctly 
	// the autocomplete pop up when zooming
	private static final int Y_OFFSET = (int) (Math.round(JavaFXUtils.getZoomFactorApplied() * 35 + 5));
	private boolean autoCompletePopupShowing = false;
	private boolean insertMode = false;
	private T syntaxProvider;

	private ListView<Keyword> suggestionsList;
	private Popup autoCompletePopup;
	protected SearchAndReplacePopOver searchAndReplacePopOver;
	private SimpleBooleanProperty showLinesProperty = new SimpleBooleanProperty(true);
	private SimpleBooleanProperty autoCompleteProperty = new SimpleBooleanProperty(true);
	private SimpleBooleanProperty isTextSelectedProperty = new SimpleBooleanProperty(false);;

	protected PopOver goToLinePopOver = null;

	public AutoCompleteCodeArea() {
		this(null, true, true, false);
	}

	public AutoCompleteCodeArea(String text) {
		this(text, true, true, false);
	}

	public AutoCompleteCodeArea(String text, boolean editable, boolean withMenu, boolean autoFormat) {
		super();

		this.setEditable(editable);

		this.selectedTextProperty().addListener((ob, ov, nv) -> this.isTextSelectedProperty.set(!nv.isEmpty()));

		autoCompletePopup = new Popup();
		searchAndReplacePopOver = new SearchAndReplacePopOver(this);

		this.setOnKeyTyped(keyEvent -> this.autoCompleteAction(keyEvent));
		this.setContextMenu(this.createContextMenu());
		this.setKeys();

		this.setOnMouseClicked(mouseEvent -> {
			this.onMouseClicked();
		});

		this.showLinesProperty.addListener((ob, ov, nv) -> enableShowLineNumbers(nv));
		this.enableShowLineNumbers(this.showLinesProperty.get());
		this.enableHighlighting();
		this.enableIndentationMaintenance();

		this.syntaxProvider = this.initSyntaxProvider();

		if (!withMenu)
			this.setContextMenu(null);

		if (text != null) {
			this.replaceText(text);
			this.setPrefHeight(countLines(text) * 18);
		}

		if (autoFormat) {
			this.formatText();
		}
	}

	abstract protected T initSyntaxProvider();

	protected void onMouseClicked() {
		if (autoCompletePopupShowing)
			hideAutocompletePopup();

		searchAndReplacePopOver.hide();

		if (goToLinePopOver != null)
			goToLinePopOver.hide();
	}

	protected void setInputMap() {
		if (!isEditable())
			return;

		InputMap<Event> addTabs = InputMap.consume(EventPattern.keyPressed(KeyCode.TAB, KeyCombination.CONTROL_DOWN),
				action -> {
					if (!this.getSelectedText().isEmpty()) {
						String[] lines = this.getSelectedText().split("\r\n|\r|\n");
						List<String> newLines = new ArrayList<>();
						for (String line : lines) {
							line = "\t" + line;
							newLines.add(line);
						}
						String replacement = StringUtils.join(newLines, "\n");
						if (!replacement.equals(this.getSelectedText())) {
							this.replaceSelection(replacement);
							this.selectRange(this.getCaretPosition() - replacement.length(), this.getCaretPosition());
						}
					}
				});
		InputMap<Event> removeTabs = InputMap.consume(
				EventPattern.keyPressed(KeyCode.TAB, KeyCombination.SHIFT_DOWN, KeyCombination.CONTROL_DOWN),
				action -> {
					if (!this.getSelectedText().isEmpty()) {
						String[] lines = this.getSelectedText().split("\r\n|\r|\n");
						List<String> newLines = new ArrayList<>();
						for (String line : lines) {
							line = line.replaceFirst("\t", "");
							newLines.add(line);
						}
						String replacement = StringUtils.join(newLines, "\n");
						if (!replacement.equals(this.getSelectedText())) {
							this.replaceSelection(replacement);
							this.selectRange(this.getCaretPosition() - replacement.length(), this.getCaretPosition());
						}
					}
				});
		InputMap<Event> autocomplete = InputMap.consume(
				EventPattern.keyPressed(KeyCode.SPACE, KeyCombination.CONTROL_DOWN), action -> this.autoCompleteAction(
						new KeyEvent(KeyEvent.KEY_PRESSED, null, null, KeyCode.SPACE, false, true, false, false)));

		InputMap<Event> searchAndReplace = InputMap.consume(
				EventPattern.keyPressed(KeyCode.F, KeyCombination.CONTROL_DOWN),
				action -> this.showSearchAndReplacePopup());
		InputMap<Event> delete = InputMap.consume(EventPattern.keyPressed(KeyCode.D, KeyCombination.CONTROL_DOWN),
				action -> {
					boolean hasInitialSelectedText = false;
					if (this.getSelectedText().isEmpty())
						this.selectLine();
					else
						hasInitialSelectedText = true;

					this.replaceSelection("");

					if (!hasInitialSelectedText && this.getCaretPosition() != 0) {
						this.deletePreviousChar();
						this.moveTo(this.getCaretPosition());
					}
				});
		InputMap<Event> toUpper = InputMap.consume(EventPattern.keyPressed(KeyCode.U, KeyCombination.CONTROL_DOWN),
				action -> this.convertSelectedTextToUpperCase());
		InputMap<Event> toLower = InputMap.consume(EventPattern.keyPressed(KeyCode.I, KeyCombination.CONTROL_DOWN),
				action -> this.convertSelectedTextToLowerCase());
// FIXME Desired behaviour can't be achieved with input map autocomplete popover does not hide.
//		 Use traditional javafx way for this specific case
//		InputMap<Event> backspace = InputMap.consume(
//				EventPattern.keyPressed(KeyCode.BACK_SPACE),
//				action -> {
//					this.hideAutocompletePopup();
//					// uncomment this to activate autocomplete on backspace
////					this.autoCompleteAction(keyEvent, auoCompletePopup);
//				}
//        );

		InputMap<Event> format = InputMap.consume(
				EventPattern.keyPressed(KeyCode.F, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN), action -> {
					if (this.getSelectedText().isEmpty())
						this.replaceText(syntaxProvider.format(this.getText()));
					else
						this.replaceSelection(syntaxProvider.format(this.getSelectedText()));
				});
		InputMap<Event> goToLine = InputMap.consume(EventPattern.keyPressed(KeyCode.L, KeyCombination.CONTROL_DOWN),
				action -> this.showGoToLinePopOver());

		InputMap<Event> stringify = InputMap.consume(
				EventPattern.keyPressed(KeyCode.QUOTE, KeyCombination.CONTROL_DOWN),
				action -> this.replaceSelection("'" + getSelectedText() + "'"));

		InputMap<Event> parentesisfy = InputMap.consume(
				EventPattern.keyPressed(KeyCode.DIGIT9, KeyCombination.CONTROL_DOWN),
				action -> this.replaceSelection("(" + getSelectedText() + ")"));

		Nodes.addFallbackInputMap(this, addTabs);
		Nodes.addFallbackInputMap(this, removeTabs);
		Nodes.addInputMap(this, autocomplete);
		Nodes.addInputMap(this, searchAndReplace);
		Nodes.addInputMap(this, delete);
		Nodes.addInputMap(this, toUpper);
		Nodes.addInputMap(this, toLower);
		Nodes.addInputMap(this, format);
		Nodes.addInputMap(this, goToLine);
		Nodes.addInputMap(this, stringify);
		Nodes.addInputMap(this, parentesisfy);
//        Nodes.addFallbackInputMap(this, backspace);
	}

	private void enableIndentationMaintenance() {
		Pattern whiteSpace = Pattern.compile("^\\s+");
		this.addEventFilter(KeyEvent.KEY_PRESSED, KE -> {
			if (KE.getCode() == KeyCode.ENTER) {
				Matcher m = whiteSpace.matcher(this.getParagraph(this.getCurrentParagraph()).getSegments().get(0));
				if (m.find())
					Platform.runLater(() -> this.insertText(this.getCaretPosition(), m.group()));
			}
		});
	}

	private void setKeys() {
		// FIXME Desired behaviour can't be achieved with input map autocomplete popover
		// does not hide.
//		 Use traditional javafx way for this specific case
		this.setOnKeyPressed(keyEvent -> {
			if (keyEvent.isControlDown() && (keyEvent.getCode() == KeyCode.MINUS || keyEvent.getCode() == KeyCode.EQUALS)) {
				// do not consume event to enable global zoom in/out (if applied)
				return;
			}
			if (keyEvent.getCode() == KeyCode.BACK_SPACE) {
				this.hideAutocompletePopup();
				// uncomment this to activate autocomplete on backspace
//					this.autoCompleteAction(keyEvent, auoCompletePopup);
			}
			// These keycodes must be excluded to delegate event to queries tab pane
			if (keyEvent.getCode() != KeyCode.ESCAPE && keyEvent.getCode() != KeyCode.N
					&& keyEvent.getCode() != KeyCode.O) {
				keyEvent.consume();
			}
		});
		this.setInputMap();
	}

	private int countLines(String str) {
		String[] lines = str.split("\r\n|\r|\n");
		return lines.length;
	}

	@Override
	public void enableHighlighting() {

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
		searchAndReplacePopOver.show(this, boundsInScene.getMaxX() - 400, boundsInScene.getMinY());
	}

	@Override
	public ContextMenu createContextMenu() {
		ContextMenu menu = new ContextMenu();

		MenuItem menuItemCopy = new MenuItem("Copy", JavaFXUtils.createIcon("/icons/copy.png"));
		menuItemCopy.setOnAction(event -> this.copy());
		menuItemCopy.disableProperty().bind(this.isTextSelectedProperty().not());

		MenuItem menuItemCut = new MenuItem("Cut", JavaFXUtils.createIcon("/icons/cut.png"));
		menuItemCut.setOnAction(event -> this.cut());
		menuItemCut.disableProperty().bind(this.isTextSelectedProperty().not());

		MenuItem menuItemPaste = new MenuItem("Paste", JavaFXUtils.createIcon("/icons/paste.png"));
		menuItemPaste.setOnAction(event -> this.paste());

		MenuItem menuItemSuggestions = new MenuItem("Suggestions", JavaFXUtils.createIcon("/icons/suggestion.png"));
		menuItemSuggestions.setOnAction(event -> this.autoCompleteAction(this.simulateControlSpaceEvent()));

		MenuItem menuItemSearchAndReplace = new MenuItem("Search...", JavaFXUtils.createIcon("/icons/magnify.png"));
		menuItemSearchAndReplace.setOnAction(action -> this.showSearchAndReplacePopup());

		MenuItem menuItemUperCase = new MenuItem("To Upper Case", JavaFXUtils.createIcon("/icons/uppercase.png"));
		menuItemUperCase.setOnAction(action -> this.convertSelectedTextToUpperCase());
		menuItemUperCase.disableProperty().bind(this.isTextSelectedProperty().not());

		MenuItem menuItemLowerCase = new MenuItem("To Lower Case", JavaFXUtils.createIcon("/icons/lowercase.png"));
		menuItemLowerCase.setOnAction(action -> this.convertSelectedTextToLowerCase());
		menuItemLowerCase.disableProperty().bind(this.isTextSelectedProperty().not());

		MenuItem menuItemFormat = new MenuItem("Format", JavaFXUtils.createIcon("/icons/format.png"));
		menuItemFormat.setOnAction(action -> {
			if (this.getSelectedText().isEmpty())
				this.replaceText(syntaxProvider.format(this.getText()));
			else
				this.replaceSelection(syntaxProvider.format(this.getSelectedText()));
		});

		MenuItem menuItemFormat2 = new MenuItem("Format Default", JavaFXUtils.createIcon("/icons/format.png"));
		menuItemFormat2.setOnAction(action -> {
			if (this.getSelectedText().isEmpty())
				this.replaceText(syntaxProvider.format(this.getText(), FormatterMode.DEFAULT));
			else
				this.replaceSelection(syntaxProvider.format(this.getText(), FormatterMode.DEFAULT));
		});

		MenuItem menuItemFormat3 = new MenuItem("Format Alternate", JavaFXUtils.createIcon("/icons/format.png"));
		menuItemFormat3.setOnAction(action -> {
			if (this.getSelectedText().isEmpty())
				this.replaceText(syntaxProvider.format(this.getText(), FormatterMode.ALTERNATE));
			else
				this.replaceSelection(syntaxProvider.format(this.getText(), FormatterMode.ALTERNATE));
		});

		MenuItem menuItemGoToLine = new MenuItem("Go to line...", JavaFXUtils.createIcon("/icons/next.png"));
		menuItemGoToLine.setOnAction(action -> this.showGoToLinePopOver());

		MenuItem menuItemSaveAs = new MenuItem("Save As...", JavaFXUtils.createIcon("/icons/save.png"));
		menuItemSaveAs.setOnAction(action -> this.saveAsFileAction());

		menu.getItems().addAll(menuItemCopy, menuItemCut, menuItemPaste, menuItemUperCase, menuItemLowerCase,
				menuItemFormat, menuItemFormat3, menuItemGoToLine, menuItemSuggestions, menuItemSearchAndReplace,
				menuItemSaveAs);
		return menu;
	}

	protected void showGoToLinePopOver() {
		if (goToLinePopOver != null)
			return;

		goToLinePopOver = createGoToLinePopOver();
		Bounds boundsInScene = this.localToScreen(this.getBoundsInLocal());
		goToLinePopOver.show(this, boundsInScene.getMaxX() - goToLinePopOver.getWidth() - 170, boundsInScene.getMinY());
	}

	@SuppressWarnings("unused")
	private void hideGoToLinePopOver() {
		if (goToLinePopOver != null) {
			goToLinePopOver.hide();
			goToLinePopOver = null;
		}
	}

	protected PopOver createGoToLinePopOver() {
		TextField textField = new TextField();
		textField.setPromptText("Go to line");
		textField.setOnKeyPressed(keyEvent -> {
			if (keyEvent.getCode() == KeyCode.ENTER) {
				if (textField.getText().isEmpty())
					return;

				int targetParagraph = Integer.parseInt(textField.getText()) - 1;
				if (targetParagraph >= 0 && targetParagraph < this.getParagraphs().size()) {
					this.moveTo(targetParagraph, 0);
					this.requestFollowCaret();
					this.hideAutocompletePopup();
				}
			} else if (keyEvent.getCode() == KeyCode.ESCAPE) {
				goToLinePopOver.hide();
			}

			keyEvent.consume();
		});

		goToLinePopOver = new PopOver(textField);
		goToLinePopOver.setOnHidden(event -> goToLinePopOver = null);
		goToLinePopOver.setArrowSize(0);

		return goToLinePopOver;
	}

	private void convertSelectedTextToUpperCase() {
		if (!this.getSelectedText().isEmpty()) {
			String toUpperCase = this.getSelectedText().toUpperCase();
			if (!toUpperCase.equals(this.getSelectedText()))
				this.replaceSelection(toUpperCase);
		}
	}

	private void convertSelectedTextToLowerCase() {
		if (!this.getSelectedText().isEmpty()) {
			String toLowerCase = this.getSelectedText().toLowerCase();
			if (!toLowerCase.equals(this.getSelectedText()))
				this.replaceSelection(toLowerCase);
		}
	}

	private KeyEvent simulateControlSpaceEvent() {
		return new KeyEvent(KeyEvent.KEY_PRESSED, null, null, KeyCode.SPACE, false, true, false, false);
	}

	protected ListView<Keyword> createSuggestionsListView(List<Keyword> suggestions) {
		ListView<Keyword> suggestionsList = new ListView<>();
		if (suggestions != null) {
			suggestionsList.getItems().addAll(FXCollections.observableList(suggestions));
			suggestionsList.setPrefHeight(200);
		}
		suggestionsList.setCellFactory(callback -> {
			return new SuggestionListCell(suggestionsList);
		});
		return suggestionsList;
	}

	private void saveAsFileAction() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setInitialFileName("new.sql");
		File selectedFile = fileChooser.showSaveDialog(null);

		if (selectedFile == null)
			return;

		try {
			if (!Files.exists(Paths.get(selectedFile.getPath())))
				Files.createFile(Paths.get(selectedFile.getPath()));

			Files.write(Paths.get(selectedFile.getPath()), this.getText().getBytes(),
					StandardOpenOption.TRUNCATE_EXISTING);
		} catch (IOException e) {
			DialogFactory.createErrorDialog(e);
		}
		DialogFactory.createNotification("File saved", "File saved at " + new Date().toString());
	}

	protected void autoCompleteAction(KeyEvent event) {

		String ch = event.getCharacter();
		if (event.isShiftDown() && event.isControlDown() && event.getCode() == KeyCode.SPACE) {
			autoCompletePopup = this.createAutoCompletePopup();

			int caretPosition = this.getCaretPosition();
			String query = this.calculateQuery(caretPosition);

			if (!query.isEmpty()) {
				if (event.getCode() == KeyCode.ENTER)
					return;

				List<Keyword> suggestions = this.getQuerySuggestions(query);

				suggestionsList = this.createSuggestionsListView(suggestions);
				if (suggestionsList.getItems().size() != 0) {
					autoCompletePopup.getContent().setAll(suggestionsList);
					this.showAutoCompletePopup();
					this.setOnSuggestionListKeyPressed(suggestionsList, query, caretPosition);
				} else {
					this.hideAutocompletePopup();
				}

			} else {
				this.hideAutocompletePopup();
			}
		} else if ((Character.isLetter(ch.charAt(0)) && autoCompleteProperty().get() && !event.isControlDown())
				|| (event.isControlDown() && event.getCode() == KeyCode.SPACE) || ch.equals(".") || ch.equals(",")
				|| ch.equals("_") || event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.BACK_SPACE) {

			int caretPosition = this.getCaretPosition();
			String query = this.calculateQuery(caretPosition);

			autoCompletePopup = this.createAutoCompletePopup();

			if (!query.isEmpty()) {
				List<Keyword> suggestions = null;
				if (event.getCode() == KeyCode.ENTER) {
					return;
				} else {
					suggestions = this.getQuerySuggestions(query);
				}
				suggestionsList = this.createSuggestionsListView(suggestions);
				if (suggestionsList.getItems().size() != 0) {
					autoCompletePopup.getContent().setAll(suggestionsList);
					this.showAutoCompletePopup();
					this.setOnSuggestionListKeyPressed(suggestionsList, query, caretPosition);
				} else {
					this.hideAutocompletePopup();
				}

			} else {
				this.hideAutocompletePopup();
			}
		} else if (!event.isControlDown()) {
			this.hideAutocompletePopup();
		}

		event.consume();
	}

	protected void hideAutocompletePopup() {
		if (autoCompletePopup != null && autoCompletePopupShowing) {
			autoCompletePopup.hide();
			autoCompletePopupShowing = false;
		}
	}

	protected void setOnSuggestionListKeyPressed(ListView<Keyword> suggestionsList, final String query,
			final int caretPosition) {

		suggestionsList.setOnKeyPressed(keyEvent -> {
			if (keyEvent.getCode() == KeyCode.ENTER) {
				listViewOnEnterAction(suggestionsList, query, caretPosition, keyEvent);
			} else if (keyEvent.getCode() == KeyCode.ESCAPE || keyEvent.getCode() == KeyCode.SPACE) {
				hideAutocompletePopup();
			}
		});
		suggestionsList.setOnMouseClicked(
				mouseEvent -> listViewOnEnterAction(suggestionsList, query, caretPosition, new KeyEvent(suggestionsList,
						suggestionsList, KeyEvent.KEY_PRESSED, null, null, KeyCode.ENTER, false, false, false, false)));
	}

	protected void listViewOnEnterAction(ListView<Keyword> suggestionsList, final String query, final int caretPosition,
			KeyEvent keyEvent) {
		final String word = (suggestionsList.getSelectionModel().getSelectedItem() != null)
				? suggestionsList.getSelectionModel().getSelectedItem().getKeyword()
				: suggestionsList.getItems().get(0).getKeyword();

		Platform.runLater(() -> {
			if (insertMode) {
				int trl = 0;
				if (query.contains(".")) {
					String[] split = query.split("\\.");
					if (split.length > 1) {
						trl = split[1].length();
					}
				}
				this.replaceText(this.getCaretPosition() - trl, this.getCaretPosition(), word);
			} else {
				this.replaceText(caretPosition - query.length(), caretPosition, word);
				this.moveTo(caretPosition + word.length() - query.length());
			}
			enableInsertMode(false);
		});

		AutoCompleteCodeArea.this.hideAutocompletePopup();
		AutoCompleteCodeArea.this.autoCompleteAction(keyEvent);
	}

	protected void showAutoCompletePopup() {
		Bounds pointer = this.caretBoundsProperty().getValue().get();
		if (!autoCompletePopupShowing) {
			autoCompletePopup.show(this, pointer.getMaxX(), pointer.getMinY() + Y_OFFSET);
			autoCompletePopupShowing = true;
		}
	}

	protected Popup createAutoCompletePopup() {
		if (autoCompletePopup != null)
			return autoCompletePopup;

		Popup popup = new Popup();
		popup.setAutoHide(true);
		popup.setOnAutoHide(event -> autoCompletePopupShowing = false);
		return popup;
	}

	@Override
	public StyleSpans<Collection<String>> computeHighlighting(String text) {
		Matcher matcher = syntaxProvider.getPatternMatcher(text);
		int lastKwEnd = 0;
		StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
		while (matcher.find()) {
			String styleClass = matcher
					.group("KEYWORD") != null
							? "keyword"
							: matcher.group("FUNCTION") != null ? "function"
									: matcher.group("METHOD") != null ? "method"
											: matcher.group("PAREN") != null ? "paren"
													: matcher.group("SEMICOLON") != null ? "semicolon"
															: matcher.group("STRING2") != null ? "string2"
																	: matcher.group("STRING") != null ? "string"
																			: matcher.group("COMMENT") != null
																					? "comment"
																					: null;
			/* never happens */ assert styleClass != null;
			spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
			spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
			lastKwEnd = matcher.end();
		}
		spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
		return spansBuilder.create();
	}

	private static final int WORD_LENGTH_LIMIT = 45;

	protected String calculateQuery(int position) {
		if (this.getText().charAt(position - 1) == '\n')
			return "";

		int limit = (position > WORD_LENGTH_LIMIT) ? WORD_LENGTH_LIMIT : position;
		String query = this.getText().substring(position - limit, position);
		int last = query.lastIndexOf(" ");
		String[] split = query.substring(last + 1).trim().split("\n");
		query = split[split.length - 1].trim().replaceAll(".*\\(", "");
		return query;
	}

	@SuppressWarnings({ "unchecked" })
	private List<Keyword> getQuerySuggestions(String query) {
		List<Keyword> suggestions = (List<Keyword>) syntaxProvider.getKeywords().stream()
				.filter(keyword -> keyword != null && ((Keyword) keyword).getKeyword().startsWith(query))
				.collect(Collectors.toList());
		return suggestions;
	}

	@Override
	public void enableShowLineNumbers(boolean enable) {
		if (enable)
			this.setParagraphGraphicFactory(new SimpleLineNumberFactory(this));
		else
			this.setParagraphGraphicFactory(null);
	}

	public SimpleBooleanProperty showLinesProperty() {
		return showLinesProperty;
	}

	public SimpleBooleanProperty isTextSelectedProperty() {
		return isTextSelectedProperty;
	}

	public SimpleBooleanProperty autoCompleteProperty() {
		return autoCompleteProperty;
	}

	public void formatText() {
		this.replaceText(syntaxProvider.format(this.getText()));
	}

	protected void enableInsertMode(Boolean enable) {
		this.insertMode = enable;
	}
}
