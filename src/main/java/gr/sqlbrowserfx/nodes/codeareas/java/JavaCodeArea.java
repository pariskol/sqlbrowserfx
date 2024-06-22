package gr.sqlbrowserfx.nodes.codeareas.java;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;
import org.slf4j.LoggerFactory;

import gr.sqlbrowserfx.LoggerConf;
import gr.sqlbrowserfx.nodes.ContextMenuOwner;
import gr.sqlbrowserfx.nodes.codeareas.AutoCompleteCodeArea;
import gr.sqlbrowserfx.nodes.codeareas.HighLighter;
import gr.sqlbrowserfx.nodes.codeareas.Keyword;
import gr.sqlbrowserfx.nodes.codeareas.KeywordType;
import gr.sqlbrowserfx.nodes.codeareas.TextAnalyzer;
import gr.sqlbrowserfx.nodes.codeareas.sql.SqlCodeArea;
import gr.sqlbrowserfx.nodes.sqlpane.CustomPopOver;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;

public class JavaCodeArea extends AutoCompleteCodeArea<JavaCodeAreaSyntaxProvider> implements ContextMenuOwner, HighLighter, TextAnalyzer {

	private Set<String> variablesAliases = new HashSet<>();
	private final JavaCodeAreaSyntaxProvider syntaxProvider = new JavaCodeAreaSyntaxProvider();

	private Thread textAnalyzerDaemon;
	protected MenuItem menuItemRun;
	private CustomPopOver sqlQueryPopOver;
	
	public JavaCodeArea() {
		this(null);
	}
	
	public JavaCodeArea(String text) {
		this(text, true, true, false);
	}

	public JavaCodeArea(String text, boolean editable, boolean withMenu, boolean autoFormat) {
		super(text, editable, withMenu, autoFormat);
		this.startTextAnalyzerDaemon();
	}


	private void initTextAnalyzerDaemon() {
		this.textAnalyzerDaemon = new Thread(() -> {
			while (!textAnalyzerDaemon.isInterrupted()) {
				try {
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
	
	private boolean isSqlQueryPopOverShowing() {
		return sqlQueryPopOver != null && sqlQueryPopOver.isShowing();
	}


	@Override
	protected void onMouseClicked() {
		super.onMouseClicked();
		if(isSqlQueryPopOverShowing()) {
			sqlQueryPopOver.hide();
		}
	}
	
	@Override
	public void appendText(String text) {
		super.appendText(text);
	}
	
	@Override
	public void paste() {
		super.paste();
	}
	
	private Set<String> analyzeTextForVariables(String text) {
		var newVariables = new HashSet<String>();
		var words = text.split("\\W+");
		for (var i = 0; i < words.length; i++) {
			var word = words[i];
			if (word.equals("new")) {
					newVariables.add(words[i - 1]);
			}
			else if (word.equals("var")) {
				newVariables.add(words[i + 1]);
			}
			else if (!word.isEmpty()
					&& Arrays
							.asList("int", "byte", "short", "long", "float", "double", "boolean", "char", "String",
									"Boolean", "Double", "Float", "Integer", "Long", "Short", "Byte", "Character")
							.contains(word)) {
				newVariables.add(words[i + 1]);
			}
		}
		return newVariables;
	}

	@Override
	protected JavaCodeAreaSyntaxProvider initSyntaxProvider() {
		return new JavaCodeAreaSyntaxProvider();
	}
	
	@Override
	public void setInputMap() {
		if (!isEditable()) {
			return;
		}
		
		super.setInputMap();
		var autocomplete = InputMap.consume(
				EventPattern.keyPressed(KeyCode.SPACE, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN),
				action -> this.autoCompleteAction(new KeyEvent(KeyEvent.KEY_PRESSED, null, null, KeyCode.SPACE, true, true, false, false))
        );
        Nodes.addInputMap(this, autocomplete);
	}

	@Override
	public ContextMenu createContextMenu() {
		var menu = super.createContextMenu();
		
		var menuItemEditSqlQuery = new MenuItem("Edit as sql query", JavaFXUtils.createIcon("/icons/thunder.png"));
		menuItemEditSqlQuery.setOnAction(event -> {
			sqlQueryPopOver = new CustomPopOver();
			var sqlCodeArea = new SqlCodeArea(this.getSelectedText());
			sqlCodeArea.setWrapText(true);
			sqlCodeArea.startTextAnalyzerDaemon();
			var scrollPane = new VirtualizedScrollPane<SqlCodeArea>(sqlCodeArea);
			scrollPane.setPrefSize(600, 400);
			sqlQueryPopOver.setContentNode(scrollPane);
			sqlQueryPopOver.setOnHidden(hiddenEvent -> {
				var newQuery = sqlCodeArea.getText();
				if (!newQuery.equals(this.getSelectedText())) {
					this.replaceSelection(newQuery);
				}
				sqlCodeArea.stopTextAnalyzerDaemon();
			});
			sqlQueryPopOver.show(this, this.getContextMenu().getX(), this.getContextMenu().getY());

		});
		menu.getItems().addAll(new SeparatorMenuItem(), menuItemEditSqlQuery);
		
		return menu;
	}

	
	@Override
	protected List<Keyword> getQuerySuggestions(String query) {
		return
				Stream.concat(
					variablesAliases.stream().map(v -> new Keyword(v, KeywordType.VARIABLE)), 
					syntaxProvider.getKeywords().stream()
					)
				.filter(keyword -> keyword != null && keyword.getKeyword().startsWith(query))
				.collect(Collectors.toList());
	}
}
