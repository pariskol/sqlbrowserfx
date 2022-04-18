package gr.sqlbrowserfx.nodes.codeareas.sql;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;
import org.slf4j.LoggerFactory;

import gr.sqlbrowserfx.LoggerConf;
import gr.sqlbrowserfx.SqlBrowserFXAppManager;
import gr.sqlbrowserfx.factories.DialogFactory;
import gr.sqlbrowserfx.nodes.ContextMenuOwner;
import gr.sqlbrowserfx.nodes.SearchAndReplacePopOver;
import gr.sqlbrowserfx.nodes.codeareas.AutoCompleteCodeArea;
import gr.sqlbrowserfx.nodes.codeareas.HighLighter;
import gr.sqlbrowserfx.nodes.codeareas.Keyword;
import gr.sqlbrowserfx.nodes.codeareas.KeywordType;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import javafx.event.Event;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.stage.Popup;

public class SqlCodeArea extends AutoCompleteCodeArea<SqlCodeAreaSyntaxProvider> implements ContextMenuOwner, HighLighter {

	private Map<String, Set<String>> tableAliases = new HashMap<>();
	private SqlCodeAreaSyntaxProvider syntaxProvider = new SqlCodeAreaSyntaxProvider();

	private Popup autoCompletePopup;
	protected SearchAndReplacePopOver searchAndReplacePopOver;
	private ListView<Keyword> suggestionsList;
	private Thread textAnalyzerDaemon;
	protected MenuItem menuItemRun;
	
	private Runnable runAction;


	public SqlCodeArea() {
		this(null);
	}
	
	public SqlCodeArea(String text) {
		this(text, true, true, false);
	}

	public SqlCodeArea(String text, boolean editable, boolean withMenu, boolean autoFormat) {
		super(text, editable, withMenu, autoFormat);
		this.startTextAnalyzerDaemon();
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
	
	@Override
	protected void autoCompleteAction(KeyEvent event) {
		
		String ch = event.getCharacter();
		if (event.isShiftDown() && event.isControlDown() && event.getCode() == KeyCode.SPACE) {
			autoCompletePopup = this.createAutoCompletePopup();
			
			int caretPosition = this.getCaretPosition();
			String query = this.calculateQuery(caretPosition);
			
			if (!query.isEmpty()) {
				if (event.getCode() == KeyCode.ENTER)
					return;

				List<Keyword> suggestions = this.getSavedQueries(query);
				
				suggestionsList = this.createSuggestionsListView(suggestions);
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
		else if(ch.equals("'")) {
			this.insertText(this.getCaretPosition(), "'");
			return;
		}
		else if ((Character.isLetter(ch.charAt(0)) && autoCompleteProperty().get() && !event.isControlDown())
				|| (event.isControlDown() && event.getCode() == KeyCode.SPACE)
				|| ch.equals(".") || ch.equals(",") || ch.equals("_")
				|| event.getCode() == KeyCode.ENTER
				|| event.getCode() == KeyCode.BACK_SPACE) {

			int caretPosition = this.getCaretPosition();
			String query = this.calculateQuery(caretPosition);
			
			autoCompletePopup = this.createAutoCompletePopup();

			if (!query.isEmpty()) {
				List<Keyword> suggestions = null;
				if (event.getCode() == KeyCode.ENTER) {
					return;
				}
				else if (query.contains(".")) {
					enableInsertMode(true);
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

	private List<Keyword> getSavedQueries(String query) {
		List<String> suggestions = new ArrayList<>();
		String sql = "select query from saved_queries where description like '%" + query + "%' ";
		try {
			SqlBrowserFXAppManager.getConfigSqlConnector().executeQuery(sql, rset -> suggestions.add(rset.getString(1)));
		} catch (SQLException e) {
			DialogFactory.createErrorNotification(e);
		}
		return suggestions.stream().map(kw -> new Keyword(kw, KeywordType.QUERY)).collect(Collectors.toList());
	}

	private List<Keyword> getQuerySuggestions(String query) {
		List<Keyword> suggestions = syntaxProvider.getKeywords().stream()
				.filter(keyword -> keyword != null && keyword.getKeyword().startsWith(query))
				.collect(Collectors.toList());
		return suggestions;
	}
    
    private List<Keyword> getColumnsSuggestions(String query) {
    	String[] split = query.split("\\.");
    	String tableAlias = split[0];
    	String columnPattern = split.length > 1 ? split[1] : null;

    	for (String knownTable : tableAliases.keySet()) {
    		Collection<String> shortcuts = tableAliases.get(knownTable);
    		for (String s : shortcuts) {
    			if (s.equals(tableAlias)) {
        	    	if (columnPattern != null) {
        	    		return syntaxProvider.getKeywords(KeywordType.COLUMN, knownTable)
        	    							.stream().filter(col -> col.getKeyword().toLowerCase().contains(columnPattern.toLowerCase()))
        	    							.collect(Collectors.toList());
        	    	}
        	    	else {
						return syntaxProvider.getKeywords(KeywordType.COLUMN, knownTable).stream().collect(Collectors.toList());
        	    	}
    			}
    		}
    	}
		return syntaxProvider.getKeywords(KeywordType.COLUMN, tableAlias) != null ? syntaxProvider.getKeywords(KeywordType.COLUMN, tableAlias).stream()
				.collect(Collectors.toList()) : new ArrayList<>();
    }

    private boolean syntaxProviderHasTable(String table) {
    	return syntaxProvider.getKeywords(KeywordType.COLUMN, table).size() > 0;
    }
    
	private Map<String, Set<String>> analyzeTextForTablesAliases(String text) {
		Map<String, Set<String>> newTableAliases = new HashMap<>();
		String[] words = text.split("\\W+");
		String saveTableShortcut = null;
		for (String word : words) {
			if (!word.isEmpty() && saveTableShortcut != null && !word.equals(saveTableShortcut.trim())
					&& !word.equalsIgnoreCase("as")) {
				newTableAliases.get(saveTableShortcut).add(word);
				saveTableShortcut = null;
			} else if (syntaxProviderHasTable(word)) {
				if (!newTableAliases.containsKey(word))
					newTableAliases.put(word, new HashSet<>());
				saveTableShortcut = word;
			}
		}
		return newTableAliases;
	}

	@Override
	protected SqlCodeAreaSyntaxProvider initSyntaxProvider() {
		return new SqlCodeAreaSyntaxProvider();
	}
	
	@Override
	protected void setInputMap() {
		super.setInputMap();
		InputMap<Event> run = InputMap.consume(
				EventPattern.keyPressed(KeyCode.ENTER, KeyCombination.CONTROL_DOWN),
				action -> { 
					if(runAction != null)
						runAction.run();
				}
        );
		InputMap<Event> autocomplete = InputMap.consume(
				EventPattern.keyPressed(KeyCode.SPACE, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN),
				action -> this.autoCompleteAction(new KeyEvent(KeyEvent.KEY_PRESSED, null, null, KeyCode.SPACE, true, true, false, false))
        );
        Nodes.addInputMap(this, run);
        Nodes.addInputMap(this, autocomplete);
	}
	
	@Override
	public ContextMenu createContextMenu() {
		ContextMenu menu = super.createContextMenu();
		menuItemRun = new MenuItem("Run", JavaFXUtils.createIcon("/icons/play.png"));
		menuItemRun.setOnAction(event -> runAction.run());
		menu.getItems().add(0, menuItemRun);
		return menu;
	}
	
	public Runnable getRunAction() {
		return runAction;
	}
	
	public void setRunAction(Runnable action) {
		runAction = action;
	}
	
}
