package gr.sqlbrowserfx.nodes.codeareas.sql;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;
import org.reactfx.Subscription;
import org.slf4j.LoggerFactory;

import gr.sqlbrowserfx.LoggerConf;
import gr.sqlbrowserfx.SqlBrowserFXAppManager;
import gr.sqlbrowserfx.factories.DialogFactory;
import gr.sqlbrowserfx.nodes.ContextMenuOwner;
import gr.sqlbrowserfx.nodes.SearchAndReplacePopOver;
import gr.sqlbrowserfx.nodes.codeareas.HighLighter;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import gr.sqlbrowserfx.utils.SqlFormatter;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.Event;
import javafx.geometry.Bounds;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.stage.FileChooser;
import javafx.stage.Popup;

public class SqlCodeArea extends CodeArea implements ContextMenuOwner, HighLighter {

	private Runnable runAction;
	private boolean autoCompletePopupShowing = false;
	private boolean autoCompleteOnType = true;
	private boolean insertMode = false;
	private Map<String, Set<String>> tableAliases = new HashMap<>();

	private Popup autoCompletePopup;
	protected SearchAndReplacePopOver searchAndReplacePopOver;
	private ListView<String> suggestionsList;
	private Thread textAnalyzerDaemon;
	protected MenuItem menuItemRun;
	

	public SqlCodeArea() {
		this(null);
	}
	
	public SqlCodeArea(String text) {
		autoCompletePopup = new Popup();
		searchAndReplacePopOver = new SearchAndReplacePopOver(this);

		this.setOnKeyTyped(keyEvent -> this.autoCompleteAction(keyEvent));
		this.setContextMenu(this.createContextMenu());
		this.setKeys();

		this.setOnMouseClicked(mouseEvent -> {
			this.onMouseClicked();
		});

		this.setParagraphGraphicFactory(LineNumberFactory.get(this));
		this.enableHighlighting();
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

	protected void onMouseClicked() {
		if (autoCompletePopupShowing) {
			hideAutocompletePopup();
		}
		searchAndReplacePopOver.hide();
	}

	private void initTextAnalyzerDaemon() {
		this.textAnalyzerDaemon = new Thread(() -> {
			while (!textAnalyzerDaemon.isInterrupted()) {
				try {
					Thread.sleep(1000);
					Map<String, Set<String>> newTableAliases = this.analyzeTextForTablesAliases(this.getText());
					if (!this.areMapsEqual(this.tableAliases, newTableAliases))
						this.tableAliases = newTableAliases;
				} catch (InterruptedException e) {
					LoggerFactory.getLogger(LoggerConf.LOGGER_NAME).debug(e.getMessage());
					break;
				}
			}
		}, getClass().getSimpleName() + "-text-analyzer-daemon");
		this.textAnalyzerDaemon.setDaemon(true);
	}

	public void startTextAnalyzerDaemon() {
		this.stopTextAnalyzerDaemon();
		if (this.textAnalyzerDaemon == null) {
			this.initTextAnalyzerDaemon();
			this.textAnalyzerDaemon.start();
		}
	}
	
	public void stopTextAnalyzerDaemon() {
		if (this.textAnalyzerDaemon != null && !this.textAnalyzerDaemon.isInterrupted()) {
			this.textAnalyzerDaemon.interrupt();
			this.textAnalyzerDaemon = null;
		}
	}
	
	private boolean areMapsEqual(Map<String, Set<String>> map1, Map<String, Set<String>> map2) {
		if (map1.keySet().size() != map2.keySet().size())
			return false;
		
		for (String key : map1.keySet()) {
			if (!map2.containsKey(key))
				return false;
			if (!map1.get(key).equals(map2.get(key)))
				return false;
		}
		return true;
	}


	@Override
	public void appendText(String text) {
		super.appendText(text);
		this.analyzeTextForTablesAliases(text);
	}
	
	@Override
	public void paste() {
		super.paste();
		this.analyzeTextForTablesAliases(this.getText());
	}
	
	protected void setInputMap() {
		InputMap<Event> addTabs = InputMap.consume(
				EventPattern.keyPressed(KeyCode.TAB, KeyCombination.CONTROL_DOWN),
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
				}
        );
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
				}
        );
		InputMap<Event> run = InputMap.consume(
				EventPattern.keyPressed(KeyCode.ENTER, KeyCombination.CONTROL_DOWN),
				action -> { 
					if(runAction != null)
						runAction.run();
				}
        );
		InputMap<Event> autocomplete = InputMap.consume(
				EventPattern.keyPressed(KeyCode.SPACE, KeyCombination.CONTROL_DOWN),
				action -> this.autoCompleteAction(new KeyEvent(KeyEvent.KEY_PRESSED, null, null, KeyCode.SPACE, false, true, false, false))
        );
		InputMap<Event> autocomplete2 = InputMap.consume(
				EventPattern.keyPressed(KeyCode.SPACE, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN),
				action -> this.autoCompleteAction(new KeyEvent(KeyEvent.KEY_PRESSED, null, null, KeyCode.SPACE, true, true, false, false))
        );
		InputMap<Event> searchAndReplace = InputMap.consume(
				EventPattern.keyPressed(KeyCode.F, KeyCombination.CONTROL_DOWN),
				action -> this.showSearchAndReplacePopup()
        );
		InputMap<Event> delete = InputMap.consume(
				EventPattern.keyPressed(KeyCode.D, KeyCombination.CONTROL_DOWN),
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
				}
        );
		InputMap<Event> toUpper = InputMap.consume(
				EventPattern.keyPressed(KeyCode.U, KeyCombination.CONTROL_DOWN),
				action -> this.convertSelectedTextToUpperCase()
        );
		InputMap<Event> toLower = InputMap.consume(
				EventPattern.keyPressed(KeyCode.L, KeyCombination.CONTROL_DOWN),
				action -> this.convertSelectedTextToLowerCase()
        );
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
		InputMap<Event> enter = InputMap.consume(
				EventPattern.keyPressed(KeyCode.ENTER),
				action -> this.autoCompleteAction(new KeyEvent(this, this, 
						KeyEvent.KEY_PRESSED, null, null, KeyCode.ENTER, 
						false, false, false, false))
        );
		
		InputMap<Event> format = InputMap.consume(
				EventPattern.keyPressed(KeyCode.F, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN),
				action -> {
					if (this.getSelectedText().isEmpty())
						this.replaceText(SqlFormatter.format(this.getText()));
					else
						this.replaceSelection(SqlFormatter.format(this.getSelectedText()));
				}
		);
		
        Nodes.addFallbackInputMap(this, addTabs);
        Nodes.addFallbackInputMap(this, removeTabs);
        Nodes.addInputMap(this, run);
        Nodes.addInputMap(this, autocomplete);
        Nodes.addInputMap(this, autocomplete2);
        Nodes.addInputMap(this, searchAndReplace);
        Nodes.addInputMap(this, delete);
        Nodes.addInputMap(this, toUpper);
        Nodes.addInputMap(this, toLower);
        Nodes.addInputMap(this, format);
//        Nodes.addFallbackInputMap(this, backspace);
        Nodes.addFallbackInputMap(this, enter);
	}
	
	private void setKeys() {
		// FIXME Desired behaviour can't be achieved with input map autocomplete popover does not hide.
//		 Use traditional javafx way for this specific case
		this.setOnKeyPressed(keyEvent -> {
				if (keyEvent.getCode() == KeyCode.BACK_SPACE) {
					this.hideAutocompletePopup();
					// uncomment this to activate autocomplete on backspace
//					this.autoCompleteAction(keyEvent, auoCompletePopup);
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
		searchAndReplacePopOver.show(this, boundsInScene.getMaxX() - searchAndReplacePopOver.getWidth(),
				boundsInScene.getMinY());
	}

	@Override
	public ContextMenu createContextMenu() {
		ContextMenu menu = new ContextMenu();

		menuItemRun = new MenuItem("Run", JavaFXUtils.createIcon("/icons/play.png"));
		menuItemRun.setOnAction(event -> runAction.run());
		MenuItem menuItemCopy = new MenuItem("Copy", JavaFXUtils.createIcon("/icons/copy.png"));
		menuItemCopy.setOnAction(event -> this.copy());

		MenuItem menuItemCut = new MenuItem("Cut", JavaFXUtils.createIcon("/icons/cut.png"));
		menuItemCut.setOnAction(event -> this.cut());

		MenuItem menuItemPaste = new MenuItem("Paste", JavaFXUtils.createIcon("/icons/paste.png"));
		menuItemPaste.setOnAction(event -> this.paste());

		MenuItem menuItemSuggestions = new MenuItem("Suggestions", JavaFXUtils.createIcon("/icons/suggestion.png"));
		menuItemSuggestions
				.setOnAction(event -> this.autoCompleteAction(this.simulateControlSpaceEvent()));

		MenuItem menuItemSearchAndReplace = new MenuItem("Search...", JavaFXUtils.createIcon("/icons/magnify.png"));
		menuItemSearchAndReplace.setOnAction(action -> this.showSearchAndReplacePopup());
		
		MenuItem menuItemUperCase = new MenuItem("To Upper Case", JavaFXUtils.createIcon("/icons/uppercase.png"));
		menuItemUperCase.setOnAction(action -> this.convertSelectedTextToUpperCase());
		MenuItem menuItemLowerCase = new MenuItem("To Lower Case", JavaFXUtils.createIcon("/icons/lowercase.png"));
		menuItemLowerCase.setOnAction(action -> this.convertSelectedTextToLowerCase());
		
		MenuItem menuItemSaveAs = new MenuItem("Save As...", JavaFXUtils.createIcon("/icons/save.png"));
		menuItemSaveAs.setOnAction(action -> this.saveAsFileAction());
		
		menu.getItems().addAll(menuItemRun, menuItemCopy, menuItemCut, menuItemPaste, menuItemUperCase, menuItemLowerCase, menuItemSuggestions, menuItemSearchAndReplace, menuItemSaveAs);
		return menu;
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

	private ListView<String> createSuggestionsListView(List<String> suggestions) {
		ListView<String> suggestionsList = new ListView<>();
		if (suggestions != null) {
			suggestionsList.getItems().addAll(FXCollections.observableList(suggestions));
			suggestionsList.setPrefHeight(100);
		}
		suggestionsList.setCellFactory(callback -> {
			return new SuggestionListCell();
		});
		return suggestionsList;
	}

	private void saveAsFileAction() {
		FileChooser fileChooser = new FileChooser();
		File selectedFile = fileChooser.showOpenDialog(null);
		
		if (selectedFile == null) return;
		
		try {
		    Files.createFile(Paths.get(selectedFile.getPath()));
			Files.write(Paths.get(selectedFile.getPath()), this.getText().getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
		} catch (IOException e) {
			e.printStackTrace();
		}
		DialogFactory.createNotification("File saved", "File saved at " + new Date().toString());
	} 
	
	private void autoCompleteAction(KeyEvent event) {
		
		String ch = event.getCharacter();
		if (event.isShiftDown() && event.isControlDown() && event.getCode() == KeyCode.SPACE) {
			autoCompletePopup = this.createPopup();
			
			int caretPosition = this.getCaretPosition();
			String query = this.getQuery(caretPosition);
			
			if (!query.isEmpty()) {
				if (event.getCode() == KeyCode.ENTER)
					return;

				List<String> suggestions = this.getSavedQueries(query);
				
				suggestionsList = this.createSuggestionsListView(suggestions);
				suggestionsList.setPrefSize(400, 200);
				if (suggestionsList.getItems().size() != 0) {
					autoCompletePopup.getContent().setAll(suggestionsList);
					this.showAutoCompletePopup();
					this.setOnSuggestionListKeyPressed(suggestionsList, query, caretPosition);
				} else {
					this.hideAutocompletePopup();
				}
				
			}
			else {
				this.hideAutocompletePopup();
			}
		}
		else if ((Character.isLetter(ch.charAt(0)) && autoCompleteOnType && !event.isControlDown())
				|| (event.isControlDown() && event.getCode() == KeyCode.SPACE)
				|| ch.equals(".") || ch.equals(",") || ch.equals("_")
				|| event.getCode() == KeyCode.ENTER
				|| event.getCode() == KeyCode.BACK_SPACE) {

			int caretPosition = this.getCaretPosition();
			String query = this.getQuery(caretPosition);
			
			autoCompletePopup = this.createPopup();

			if (!query.isEmpty()) {
				List<String> suggestions = null;
				if (event.getCode() == KeyCode.ENTER) {
					return;
				}
				else if (query.contains(".")) {
					insertMode  = true;
					suggestions = this.getColumnsSuggestions(query);
				}
				else {
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

	private List<String> getSavedQueries(String query) {
		List<String> suggestions = new ArrayList<>();
		String sql = "select query from saved_queries where description like '%" + query + "%' ";
		try {
			SqlBrowserFXAppManager.getConfigSqlConnector().executeQuery(sql, rset -> suggestions.add(rset.getString(1)));
		} catch (SQLException e) {
			DialogFactory.createErrorNotification(e);
		}
		return suggestions;
	}

	private void hideAutocompletePopup() {
		if (autoCompletePopup != null && autoCompletePopupShowing) {
			autoCompletePopup.hide();
			autoCompletePopupShowing = false;
		}
	}

	private void setOnSuggestionListKeyPressed(ListView<String> suggestionsList,
			final String query, final int caretPosition) {
		
		suggestionsList.setOnKeyPressed(keyEvent -> {
			if (keyEvent.getCode() == KeyCode.ENTER) {
				listViewOnEnterActrion(suggestionsList, query, caretPosition, keyEvent);
			}
			else if (keyEvent.getCode() == KeyCode.ESCAPE || keyEvent.getCode() == KeyCode.SPACE) {
				hideAutocompletePopup();
			}
		});
		suggestionsList.setOnMouseClicked(mouseEvent -> listViewOnEnterActrion(suggestionsList, query, caretPosition,
							new KeyEvent(suggestionsList, suggestionsList, 
									KeyEvent.KEY_PRESSED, null, null, KeyCode.ENTER, 
									false, false, false, false)));
	}

	private void listViewOnEnterActrion(ListView<String> suggestionsList, final String query, final int caretPosition,
			KeyEvent keyEvent) {
		final String word = (suggestionsList.getSelectionModel().getSelectedItem() != null) ?
								suggestionsList.getSelectionModel().getSelectedItem().replaceAll("@", "")	 :
									suggestionsList.getItems().get(0).replaceAll("@", "");

		Platform.runLater(() -> {
			if (insertMode) {
				int trl = 0;
				if (query.contains(".")) {
					String[] split = query.split("\\.");
					if (split.length > 1) {
						trl = split[1].length();
					}
				}
				this.insertText(this.getCaretPosition(), word.substring(0 + trl));
			} else {
				this.replaceText(caretPosition - query.length(), caretPosition, word);
				this.moveTo(caretPosition + word.length() - query.length());
			}
			insertMode = false;
		});
		
		SqlCodeArea.this.hideAutocompletePopup();
		SqlCodeArea.this.autoCompleteAction(keyEvent);
	}

	private void showAutoCompletePopup() {
		Bounds pointer = this.caretBoundsProperty().getValue().get();
		if (!autoCompletePopupShowing) {
			autoCompletePopup.show(this, pointer.getMaxX(), pointer.getMinY() + 20);
			autoCompletePopupShowing = true;
		}
	}

	private Popup createPopup() {
		if (autoCompletePopup != null)
			return autoCompletePopup;
		
		Popup popup = new Popup();
		popup.setAutoHide(true);
		popup.setOnAutoHide(event -> autoCompletePopupShowing = false);
		return popup;
	}

	@Override
	public StyleSpans<Collection<String>> computeHighlighting(String text) {
		Matcher matcher = SqlCodeAreaSyntax.PATTERN.matcher(text);
		int lastKwEnd = 0;
		StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
		while (matcher.find()) {
			String styleClass = matcher.group("KEYWORD") != null ? "keyword"
					: matcher.group("FUNCTION") != null ? "function"
							: matcher.group("METHOD") != null ? "method" : matcher.group("PAREN") != null ? "paren"
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

	public Runnable getRunAction() {
		return runAction;
	}
	
	public void setRunAction(Runnable action) {
		runAction = action;
	}

	public boolean isAutoCompleteOnTypeEnabled() {
		return autoCompleteOnType;
	}

	public void setAutoCompleteOnType(boolean autoCompleteOnType) {
		this.autoCompleteOnType = autoCompleteOnType;
	}
	
	private static final int WORD_LENGTH_LIMIT = 45;

    public String getQuery(int position) {
        int limit = (position > WORD_LENGTH_LIMIT) ? WORD_LENGTH_LIMIT : position;
        String keywords = this.getText().substring(position - limit, position);
        keywords = keywords.replaceAll("\\n", " ").trim();
        int last = keywords.lastIndexOf(" ");
        return keywords.substring(last + 1).trim();
    }

	public List<String> getQuerySuggestions(String query) {
        List<String> suggestions = SqlCodeAreaSyntax.KEYWORDS_lIST.stream()
        							.filter(keyword -> keyword != null && keyword.startsWith(query)).collect(Collectors.toList());
        return suggestions;
    }
    
    public List<String> getColumnsSuggestions(String query) {
    	String[] split = query.split("\\.");
    	
    	String tableAlias = split[0];
    	String columnPattern = split.length > 1 ? split[1] : null;
    	
    	for (String knownTable : tableAliases.keySet()) {
    		Collection<String> shortcuts = tableAliases.get(knownTable);
    		for (String s : shortcuts) {
    			if (s.equals(tableAlias)) {
        	    	if (columnPattern != null) {
        	    		return SqlCodeAreaSyntax.COLUMNS_MAP.get(knownTable)
        	    							.stream().filter(col -> col.toLowerCase().contains(columnPattern.toLowerCase()))
        	    							.collect(Collectors.toList());
        	    	}
        	    	else {
        	    		return SqlCodeAreaSyntax.COLUMNS_MAP.get(knownTable);
        	    	}
    			}
    		}
    	}
    	return SqlCodeAreaSyntax.COLUMNS_MAP.get(tableAlias);
    }

	public Map<String, Set<String>> analyzeTextForTablesAliases(String text) {
		Map<String, Set<String>> newTableAliases = new HashMap<>();
		String[] words = text.split("\\W+");
		String saveTableShortcut = null;
		for (String word : words) {
			if (!word.isEmpty() && saveTableShortcut != null && !word.equals(saveTableShortcut.trim())
					&& !word.equalsIgnoreCase("as")) {
				newTableAliases.get(saveTableShortcut).add(word);
				saveTableShortcut = null;
			} else if (SqlCodeAreaSyntax.COLUMNS_MAP.containsKey(word)) {
				if (!newTableAliases.containsKey(word))
					newTableAliases.put(word, new HashSet<>());
				saveTableShortcut = word;
			}
		}
		return newTableAliases;
	}

}
