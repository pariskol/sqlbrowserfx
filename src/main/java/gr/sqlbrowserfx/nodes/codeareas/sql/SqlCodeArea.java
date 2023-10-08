package gr.sqlbrowserfx.nodes.codeareas.sql;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;
import org.slf4j.LoggerFactory;

import gr.sqlbrowserfx.LoggerConf;
import gr.sqlbrowserfx.SqlBrowserFXAppManager;
import gr.sqlbrowserfx.conn.DbCash;
import gr.sqlbrowserfx.factories.DialogFactory;
import gr.sqlbrowserfx.nodes.ContextMenuOwner;
import gr.sqlbrowserfx.nodes.codeareas.AutoCompleteCodeArea;
import gr.sqlbrowserfx.nodes.codeareas.HighLighter;
import gr.sqlbrowserfx.nodes.codeareas.Keyword;
import gr.sqlbrowserfx.nodes.codeareas.KeywordType;
import gr.sqlbrowserfx.nodes.codeareas.TextAnalyzer;
import gr.sqlbrowserfx.nodes.sqlpane.CustomPopOver;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import gr.sqlbrowserfx.utils.mapper.DTOMapper;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.geometry.Bounds;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;

public class SqlCodeArea extends AutoCompleteCodeArea<SqlCodeAreaSyntaxProvider> implements ContextMenuOwner, HighLighter, TextAnalyzer {

	private Map<String, Set<String>> tableAliases = new HashMap<>();
	private Set<String> variablesAliases = new HashSet<>();
	private final SqlCodeAreaSyntaxProvider syntaxProvider = new SqlCodeAreaSyntaxProvider();

	private Popup autoCompletePopup;
	private ListView<Keyword> suggestionsList;
	private Thread textAnalyzerDaemon;
	protected MenuItem menuItemRun;
	
	private Runnable runAction;
	private CustomPopOver historyPopOver;
	private CustomPopOver schemaPopOver;
	
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
					Map<String, Set<String>> newTableAliases = this.analyzeTextForTables(this.getText());
					if (!this.areMapsEqual(this.tableAliases, newTableAliases))
						this.tableAliases = newTableAliases;
					this.variablesAliases = this.analyzeTextForVariables(this.getText());
					Thread.sleep(1000);
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
		this.analyzeTextForTables(text);
	}
	
	@Override
	public void paste() {
		super.paste();
		this.analyzeTextForTables(this.getText());
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
				if (!suggestionsList.getItems().isEmpty()) {
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
		// FIXME: this may be removed as a new key combination Ctrl + ' stringifies selected text
//		else if(ch.equals("'")) {
//			this.insertText(this.getCaretPosition(), "'");
//			this.moveTo(this.getCaretPosition() - 1);
//			
//			return;
//		}
		else if(ch.equals("(")) {
			this.insertText(this.getCaretPosition(), ")");
			this.moveTo(this.getCaretPosition() - 1);
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
				List<Keyword> suggestions;
				if (event.getCode() == KeyCode.ENTER) {
					return;
				}
				else if (query.contains(".")) {
					enableInsertMode(true);
					suggestions = this.getColumnsSuggestions(query);
				}
				else {
					enableInsertMode(false);
					suggestions = this.getQuerySuggestions(query);
				}
				
				suggestionsList = this.createSuggestionsListView(suggestions);
				if (!suggestionsList.getItems().isEmpty()) {
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

	@Override
	protected void onMouseClicked() {
		super.onMouseClicked();
		if(isHistoryPopOverShowing())
			historyPopOver.hide();
		if (schemaPopOver != null)
			schemaPopOver.hide();
	}
	
	private boolean isHistoryPopOverShowing() {
		return historyPopOver != null && historyPopOver.isShowing();
	}
	
	private boolean isSchemaPopOverShowing() {
		return schemaPopOver != null && schemaPopOver.isShowing();
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

	@Override
	protected List<Keyword> getQuerySuggestions(String query) {
		return
				Stream.concat(
					Stream.concat(
						variablesAliases.stream().map(v -> new Keyword(v, KeywordType.VARIABLE)), 
						tableAliases.values().stream()
									.flatMap(Collection::stream)
									.map(t -> new Keyword(t, KeywordType.ALIAS))
					),
					syntaxProvider.getKeywords().stream()
					)
				.filter(keyword -> keyword != null && keyword.getKeyword().startsWith(query))
				.collect(Collectors.toList());
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
						return new ArrayList<>(syntaxProvider.getKeywords(KeywordType.COLUMN, knownTable));
        	    	}
    			}
    		}
    	}
		return syntaxProvider.getKeywords(KeywordType.COLUMN, tableAlias) != null ? new ArrayList<>(syntaxProvider.getKeywords(KeywordType.COLUMN, tableAlias)) : new ArrayList<>();
    }

    private boolean syntaxProviderHasTable(String table) {
    	return !syntaxProvider.getKeywords(KeywordType.COLUMN, table).isEmpty();
    }
    
	private Map<String, Set<String>> analyzeTextForTables(String text) {
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
	
	private Set<String> analyzeTextForVariables(String text) {
		Set<String> newVariables = new HashSet<>();
		String[] words = text.split("\\W+");
		for (int i = 0; i < words.length; i++) {
			String word = words[i];
			if (!word.isEmpty() && 
				(word.equalsIgnoreCase("declare") ||
				 word.equalsIgnoreCase("in") ||
				 word.equalsIgnoreCase("out")
				) && 
				i < words.length - 1) {
				if (!words[i+1].startsWith("(")) {
					newVariables.add(words[i+1]);
				}
			} 
		}
		return newVariables;
	}

	@Override
	protected SqlCodeAreaSyntaxProvider initSyntaxProvider() {
		return new SqlCodeAreaSyntaxProvider();
	}
	
	@Override
	public void setInputMap() {
		if (!isEditable())
			return;
		
		super.setInputMap();
		InputMap<Event> run = InputMap.consume(
				EventPattern.keyPressed(KeyCode.ENTER, KeyCombination.CONTROL_DOWN),
				action -> { 
					if(runAction != null) {
						runAction.run();
						action.consume();
					}
				}
        );
		InputMap<Event> autocomplete = InputMap.consume(
				EventPattern.keyPressed(KeyCode.SPACE, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN),
				action -> this.autoCompleteAction(new KeyEvent(KeyEvent.KEY_PRESSED, null, null, KeyCode.SPACE, true, true, false, false))
        );
		InputMap<Event> history = InputMap.consume(
				EventPattern.keyPressed(KeyCode.H, KeyCombination.CONTROL_DOWN),
				action -> {
					if (isHistoryPopOverShowing()) {
						historyPopOver.requestFocus();
						return;
					}
					
					showHistoryPopOver();
				}
        );
		

		
        Nodes.addInputMap(this, run);
        Nodes.addInputMap(this, autocomplete);
        Nodes.addInputMap(this, history);
	}

	private void showHistoryPopOver() {
		PHistorySqlCodeArea codeArea = new PHistorySqlCodeArea();
		DatePicker datePicker = new DatePicker(LocalDate.now());

		datePicker.setOnAction(actionEvent -> {
		    LocalDate date = datePicker.getValue();
		    String dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		    this.getQueriesHistory(codeArea,dateStr);

		});
		VirtualizedScrollPane<CodeArea> pane = new VirtualizedScrollPane<>(codeArea);
		pane.setPrefSize(600, 400);
		HBox hbox = new HBox(codeArea.getSearchAndReplacePopOver(), codeArea.createGoToLinePopOver().getContentNode(), datePicker);
		hbox.setSpacing(20);
		
		VBox vbox = new VBox(new Label("Query History", JavaFXUtils.createIcon("/icons/monitor.png")), hbox, pane);
		vbox.setPrefSize(600, 500);

		historyPopOver = new CustomPopOver(vbox);
		
		historyPopOver.setOnHidden(event -> SqlCodeArea.this.historyPopOver = null);
		Bounds boundsInScene = this.localToScreen(this.getBoundsInLocal());
		historyPopOver.show(getParent(), boundsInScene.getMaxX() - 620,
				boundsInScene.getMinY());
		this.getQueriesHistory(codeArea, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
	}

	private void getQueriesHistory(CodeArea codeArea, String dateStr) {
		SqlBrowserFXAppManager.getConfigSqlConnector().executeQueryRawAsync("select query, duration, datetime(timestamp,'localtime') timestamp from queries_history "
				+ "where date(datetime(timestamp,'localtime')) = '" + dateStr + "' order by id",
			rset -> {
				StringBuilder history = new StringBuilder();
				while (rset.next()) {
					try {
						Map<String, Object> map = DTOMapper.map(rset);
						history.append("\n--  Executed at : ").append(map.get("timestamp")).append(" Duration: ").append(map.get("duration")).append("ms --\n");
						history.append(map.get("query"));
						history.append("\n");
					} catch (Exception e) {
						LoggerFactory.getLogger(LoggerConf.LOGGER_NAME).error("Could not get query");
					}
				}
				Platform.runLater(() -> codeArea.replaceText(history.toString()));
			}
		);
	}
	
	@Override
	public ContextMenu createContextMenu() {
		ContextMenu menu = super.createContextMenu();
		menuItemRun = new MenuItem("Run", JavaFXUtils.createIcon("/icons/play.png"));
		menuItemRun.setOnAction(event -> runAction.run());
		menu.getItems().add(0, new SeparatorMenuItem());
		menu.getItems().add(0, menuItemRun);
		MenuItem menuItemHistory = new MenuItem("History", JavaFXUtils.createIcon("/icons/monitor.png"));
		menuItemHistory.setOnAction(event -> SqlCodeArea.this.showHistoryPopOver());
		
		MenuItem menuItemShowSchema = new MenuItem("Show Schema", JavaFXUtils.createIcon("/icons/script.png"));
		menuItemShowSchema.setOnAction(action -> SqlCodeArea.this.showSchemaPopOver());
		menuItemShowSchema.disableProperty().bind(this.isTextSelectedProperty().not());
		
		menu.getItems().addAll(new SeparatorMenuItem(), menuItemHistory, menuItemShowSchema);

		return menu;
	}

	private void showSchemaPopOver() {
		String table = this.getSelectedText();
		String schema = DbCash.getSchemaFor(table);
		
		if (schema == null)
			return;
		
		SqlCodeArea codeArea = new SqlCodeArea(schema, false, false, true);
		VirtualizedScrollPane<SqlCodeArea> scrollPane = new VirtualizedScrollPane<>(codeArea);
		scrollPane.setPrefSize(600, 400);

		schemaPopOver = new CustomPopOver(scrollPane);
		schemaPopOver.setOnHidden(event -> schemaPopOver = null);
		schemaPopOver.show(this, this.getContextMenu().getX(), this.getContextMenu().getY());
	}
	
	public void setRunAction(Runnable action) {
		runAction = action;
	}
}
