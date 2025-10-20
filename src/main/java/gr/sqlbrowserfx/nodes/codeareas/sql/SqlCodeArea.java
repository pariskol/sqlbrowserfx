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

import org.apache.commons.lang3.StringUtils;
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
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class SqlCodeArea extends AutoCompleteCodeArea<SqlCodeAreaSyntaxProvider> implements ContextMenuOwner, HighLighter, TextAnalyzer {

	private Map<String, Set<String>> tableAliases = new HashMap<>();
	private Set<String> variablesAliases = new HashSet<>();
	private final SqlCodeAreaSyntaxProvider syntaxProvider = new SqlCodeAreaSyntaxProvider();

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
					var newTableAliases = this.analyzeTextForTables(this.getText());
					if (!this.areMapsEqual(this.tableAliases, newTableAliases)) {
						this.tableAliases = newTableAliases;
					}
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
			if (!map2.containsKey(key)) {
				return false;
			}
			if (!map1.get(key).equals(map2.get(key))) {
				return false;
			}
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
	
    protected void autoCompleteSavedQuery(KeyEvent event) {
		var caretPosition = this.getCaretPosition();
		var query = this.calculateQuery(caretPosition);
		
		if (query.isEmpty()) {
			this.hideAutocompletePopup();
			return;
		}
		
		var suggestions = this.getSavedQueries(query);
		
		if (suggestions == null || suggestions.isEmpty()) {
			this.hideAutocompletePopup();
			return;
		}
		
		this.showSuggestionsList(suggestions, query, caretPosition);
		event.consume();
    }
    
    
	@Override
	protected void onMouseClicked() {
		super.onMouseClicked();
		if(isHistoryPopOverShowing()) {
			historyPopOver.hide();
		}
		if (schemaPopOver != null) {
			schemaPopOver.hide();
		}
	}
	
	private boolean isHistoryPopOverShowing() {
		return historyPopOver != null && historyPopOver.isShowing();
	}
	
	private boolean isSchemaPopOverShowing() {
		return schemaPopOver != null && schemaPopOver.isShowing();
	}
	
	private List<Keyword> getSavedQueries(String query) {
		if (query.isEmpty()) {
			return null;
		}
		
		var suggestions = new ArrayList<String>();
		var sql = "select query from saved_queries where description like '%" + query + "%' ";
		try {
			SqlBrowserFXAppManager.getConfigSqlConnector().executeQuery(sql, rset -> suggestions.add(rset.getString(1)));
		} catch (SQLException e) {
			DialogFactory.createErrorNotification(e);
		}
		return suggestions.stream().map(kw -> new Keyword(kw, KeywordType.QUERY)).collect(Collectors.toList());
	}

	@Override
	protected List<Keyword> getQuerySuggestions(String query) {
		if (query.isEmpty()) {
			return null;
		}
		var isColumnSuggestion = query.contains(".");
		enableInsertMode(isColumnSuggestion);
		
		if (isColumnSuggestion) {
			return this.getColumnsSuggestions(query);
		}
		
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
				.toList();
	}
    
	private List<Keyword> getColumnsSuggestions(String query) {
	    if (query.isEmpty()) {
	        return null;
	    }
	    
	    var split = query.split("\\.");
	    var tableAlias = split[0];
	    var columnPattern = split.length > 1 ? split[1] : null;

	    for (var entry : tableAliases.entrySet()) {
	        if (entry.getValue().contains(tableAlias)) {
	            var knownTable = entry.getKey();
	            var keywords = syntaxProvider.getKeywords(KeywordType.COLUMN, knownTable);
	            if (columnPattern != null) {
	                return keywords.stream()
	                               .filter(col -> StringUtils.containsIgnoreCase(col.getKeyword(), columnPattern))
	                               .toList();
	            }

	            return new ArrayList<>(keywords);
	        }
	    }

	    // If no alias match was found, try using the tableAlias directly as the table name
	    var keywords = syntaxProvider.getKeywords(KeywordType.COLUMN, tableAlias);
	    return keywords != null ? new ArrayList<>(keywords) : new ArrayList<>();
	}
	
    private boolean syntaxProviderHasTable(String table) {
    	return !syntaxProvider.getKeywords(KeywordType.COLUMN, table).isEmpty();
    }
    
	private Map<String, Set<String>> analyzeTextForTables(String text) {
		var newTableAliases = new HashMap<String, Set<String>>();
		var words = text.split("\\W+");
		String saveTableShortcut = null;
		for (var word : words) {
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
		var newVariables = new HashSet<String>();
		var words = text.split("\\W+");
		for (var i = 0; i < words.length; i++) {
			var word = words[i];
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
		var run = InputMap.consume(
			EventPattern.keyPressed(KeyCode.ENTER, KeyCombination.CONTROL_DOWN),
			action -> { 
				if(runAction != null) {
					runAction.run();
					action.consume();
				}
			}
        );
		var autocomplete = InputMap.consume(
				EventPattern.keyPressed(KeyCode.SPACE, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN),
				action -> this.autoCompleteSavedQuery(action)
        );
		var history = InputMap.consume(
			EventPattern.keyPressed(KeyCode.H, KeyCombination.CONTROL_DOWN),
			action -> {
				if (isHistoryPopOverShowing()) {
					historyPopOver.requestFocus();
					return;
				}
				
				showHistoryPopOver();
			}
        );
		var comment = InputMap.consume(
			EventPattern.keyPressed(KeyCode.SLASH, KeyCombination.CONTROL_DOWN),
			action -> {
				var commentPrefix = "-- ";
                if (!this.getSelectedText().isEmpty()) {
                    String[] lines = this.getSelectedText().split("\r\n|\r|\n");
                    List<String> newLines = new ArrayList<>();
                    for (String line : lines) {
                    	if (line.startsWith(commentPrefix)) {
                    		line = line.substring(commentPrefix.length());
                    	}
                    	else {
                    		line = "-- " + line;
                    	}
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
		

		
        Nodes.addInputMap(this, run);
        Nodes.addInputMap(this, autocomplete);
        Nodes.addInputMap(this, history);
        Nodes.addInputMap(this, comment);
	}

	private void showHistoryPopOver() {
		var codeArea = new PHistorySqlCodeArea();
		var datePicker = new DatePicker(LocalDate.now());

		datePicker.setOnAction(actionEvent -> {
		    var date = datePicker.getValue();
		    var dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		    this.getQueriesHistory(codeArea, dateStr);

		});
		var pane = new VirtualizedScrollPane<CodeArea>(codeArea);
		pane.setPrefSize(600, 400);
		var hbox = new HBox(codeArea.getSearchAndReplacePopOver(), codeArea.createGoToLinePopOver().getContentNode(), datePicker);
		hbox.setSpacing(20);
		
		var vbox = new VBox(new Label("Query History", JavaFXUtils.createIcon("/icons/monitor.png")), hbox, pane);
		vbox.setPrefSize(600, 500);

		historyPopOver = new CustomPopOver(vbox);
		
		historyPopOver.setOnHidden(event -> SqlCodeArea.this.historyPopOver = null);
		var boundsInScene = this.localToScreen(this.getBoundsInLocal());
		historyPopOver.show(getParent(), boundsInScene.getMaxX() - 620,
				boundsInScene.getMinY());
		this.getQueriesHistory(codeArea, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
	}

	private void getQueriesHistory(CodeArea codeArea, String dateStr) {
		SqlBrowserFXAppManager.getConfigSqlConnector().executeQueryRawAsync("select query, duration, datetime(timestamp,'localtime') timestamp from queries_history "
				+ "where date(datetime(timestamp,'localtime')) = '" + dateStr + "' order by id",
			rset -> {
				var history = new StringBuilder();
				while (rset.next()) {
					try {
						var map = DTOMapper.map(rset);
						history.append("\n--  Executed at : ").append(map.get("timestamp")).append(" Duration: ").append(map.get("duration")).append("ms --\n");
						history.append(map.get("query"));
						history.append("\n");
					} catch (Exception e) {
						LoggerFactory.getLogger(LoggerConf.LOGGER_NAME).error("Could not get query");
					}
				}
				Platform.runLater(() -> {
					codeArea.replaceText(history.toString());
					var pattern = "--  Executed at :";
					codeArea.moveTo(codeArea.getText().lastIndexOf(pattern) + pattern.length());
					codeArea.requestFollowCaret();
				});
			}
		);
	}
	
	@Override
	public ContextMenu createContextMenu() {
		var menu = super.createContextMenu();
		menuItemRun = new MenuItem("Run", JavaFXUtils.createIcon("/icons/play.png"));
		menuItemRun.setOnAction(event -> runAction.run());
		menu.getItems().add(0, new SeparatorMenuItem());
		menu.getItems().add(0, menuItemRun);
		var menuItemHistory = new MenuItem("History", JavaFXUtils.createIcon("/icons/monitor.png"));
		menuItemHistory.setOnAction(event -> SqlCodeArea.this.showHistoryPopOver());
		
		var menuItemShowSchema = new MenuItem("Show Schema", JavaFXUtils.createIcon("/icons/script.png"));
		menuItemShowSchema.setOnAction(action -> SqlCodeArea.this.showSchemaPopOver());
		menuItemShowSchema.disableProperty().bind(this.isTextSelectedProperty().not());
		
		menu.getItems().addAll(new SeparatorMenuItem(), menuItemHistory, menuItemShowSchema);

		return menu;
	}

	private void showSchemaPopOver() {
		var table = this.getSelectedText();
		var schema = DbCash.getSchemaFor(table);
		
		if (schema == null) {
			return;
		}
		
		var codeArea = new SqlCodeArea(schema, false, false, true);
		var scrollPane = new VirtualizedScrollPane<SqlCodeArea>(codeArea);
		scrollPane.setPrefSize(600, 400);

		schemaPopOver = new CustomPopOver(scrollPane);
		schemaPopOver.setOnHidden(event -> schemaPopOver = null);
		schemaPopOver.show(this, this.getContextMenu().getX(), this.getContextMenu().getY());
	}
	
	public void setRunAction(Runnable action) {
		runAction = action;
	}
}
