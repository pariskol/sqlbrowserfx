
package gr.sqlbrowserfx.nodes.codeareas.log;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;

import org.controlsfx.control.PopOver;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;

import gr.sqlbrowserfx.nodes.ContextMenuOwner;
import gr.sqlbrowserfx.nodes.InputMapOwner;
import gr.sqlbrowserfx.nodes.SearchAndReplacePopOver;
import gr.sqlbrowserfx.nodes.codeareas.HighLighter;
import gr.sqlbrowserfx.nodes.codeareas.sql.SimpleLineNumberFactory;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;

public class LogCodeArea extends CodeArea implements ContextMenuOwner, InputMapOwner, HighLighter {

	private final SearchAndReplacePopOver searchAndReplacePopOver;
	private final SimpleBooleanProperty showLinesProperty = new SimpleBooleanProperty(true);
	private final SimpleBooleanProperty followCarretProperty = new SimpleBooleanProperty(true);
	private PopOver goToLinePopOver = null;
	private final LogCodeAreaSyntaxProvider syntaxProvider = new LogCodeAreaSyntaxProvider();



	public LogCodeArea() {
		super();
		searchAndReplacePopOver = new SearchAndReplacePopOver(this, false);
		this.setEditable(false);
		this.enableHighlighting();
		this.setContextMenu(this.createContextMenu());
		this.setInputMap();
		this.showLinesProperty.addListener((ob,ov,nv) -> enableShowLineNumbers(nv));
		
		this.setOnMouseClicked(mouseEvent -> this.onMouseClicked());
	}
	
	@Override
	public void enableShowLineNumbers(boolean enable) {
		if (enable) {
			this.setParagraphGraphicFactory(new SimpleLineNumberFactory(this));
		}
		else {
			this.setParagraphGraphicFactory(null);
		}
	}
	
	@Override
	public void appendText(String text) {
		super.appendText(text);
		if (this.followCarretProperty.get()) {
			this.moveTo(this.getLength());
			this.requestFollowCaret();
		}
	}
	
	@Override
	public void setInputMap() {
		Nodes.addInputMap(this, 
				InputMap.consume(
				EventPattern.keyPressed(KeyCode.F, KeyCombination.CONTROL_DOWN),
				action -> this.showSearchAndReplacePopup()
        ));
		Nodes.addInputMap(this,InputMap.consume(
				EventPattern.keyPressed(KeyCode.L, KeyCombination.CONTROL_DOWN),
				action -> this.goToLineAction()
        ));
	}
	
	protected void onMouseClicked() {
		if (goToLinePopOver != null) {
			goToLinePopOver.hide();
		}
	}
	
	@SuppressWarnings("unused")
	@Deprecated
	private void setKeys() {
		this.setOnKeyPressed(keyEvent -> {
			if (keyEvent.isControlDown() && keyEvent.getCode() == KeyCode.F) {
				this.showSearchAndReplacePopup();
			}
		});
	}
	
	@Override
	public void enableHighlighting() {
		this.multiPlainChanges().successionEnds(Duration.ofMillis(100))
				.subscribe(ignore -> this.setStyleSpans(0, computeHighlighting(this.getText())));
	}
	
	@Override
	public StyleSpans<Collection<String>> computeHighlighting(String text) {
		var matcher = syntaxProvider.getPatternMatcher(text);
		var lastKwEnd = 0;
		var spansBuilder = new StyleSpansBuilder<Collection<String>>();
		while (matcher.find()) {
			var styleClass = matcher.group("KEYWORD") != null ? "keyword"
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
	
	@Override
	public ContextMenu createContextMenu() {
		var menu = new ContextMenu();

		var menuItemCopy = new MenuItem("Copy", JavaFXUtils.createIcon("/icons/copy.png"));
		menuItemCopy.setOnAction(event -> this.copy());

		var menuItemSearchAndReplace = new MenuItem("Search...", JavaFXUtils.createIcon("/icons/magnify.png"));
		menuItemSearchAndReplace.setOnAction(action -> this.showSearchAndReplacePopup());

		var menuItemGoToLine = new MenuItem("Go to line...", JavaFXUtils.createIcon("/icons/next.png"));
		menuItemGoToLine.setOnAction(action -> this.goToLineAction());
		
		menu.getItems().addAll(menuItemCopy,menuItemSearchAndReplace, menuItemGoToLine);
		return menu;
	}
	
	private void goToLineAction() {
		if (goToLinePopOver != null) {
			return;
		}
		
		var textField = new TextField();
		textField.setPromptText("Go to line ...");
		textField.setOnKeyPressed(keyEvent -> {
			if (keyEvent.getCode() == KeyCode.ENTER) {
				if (textField.getText().isEmpty()) {
					return;
				}
				
				var targetParagraph = Integer.parseInt(textField.getText()) - 1;
				if (targetParagraph > 0 && targetParagraph < this.getParagraphs().size()) {
					this.moveTo(targetParagraph, 0);
					this.requestFollowCaret();
					goToLinePopOver.hide();
					goToLinePopOver = null;
				}
			}
		});
		goToLinePopOver = new PopOver(textField);
		goToLinePopOver.setOnHidden(event -> goToLinePopOver = null);
		goToLinePopOver.setArrowSize(0);
		var boundsInScene = this.localToScreen(this.getBoundsInLocal());
		goToLinePopOver.show(this, boundsInScene.getMaxX() - goToLinePopOver.getWidth() - 200, boundsInScene.getMinY());
	}
	
	protected void showSearchAndReplacePopup() {
		if (!this.getSelectedText().isEmpty()) {
			searchAndReplacePopOver.getFindField().setText(this.getSelectedText());
			searchAndReplacePopOver.getFindField().selectAll();
		}
		var boundsInScene = this.localToScreen(this.getBoundsInLocal());
		searchAndReplacePopOver.show(this, boundsInScene.getMaxX() - searchAndReplacePopOver.getWidth(),
				boundsInScene.getMinY());
	}
	
	public SimpleBooleanProperty showLinesProperty() {
		return showLinesProperty;
	}
	
	public SimpleBooleanProperty followCarretProperty() {
		return followCarretProperty;
	}
}
