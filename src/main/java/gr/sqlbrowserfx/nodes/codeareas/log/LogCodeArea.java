
package gr.sqlbrowserfx.nodes.codeareas.log;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;

import org.controlsfx.control.PopOver;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;
import org.reactfx.Subscription;

import gr.sqlbrowserfx.nodes.ContextMenuOwner;
import gr.sqlbrowserfx.nodes.SearchAndReplacePopOver;
import gr.sqlbrowserfx.nodes.codeareas.HighLighter;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Bounds;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;

public class LogCodeArea extends CodeArea implements ContextMenuOwner, HighLighter {

	private SearchAndReplacePopOver searchAndReplacePopOver;
	private SimpleBooleanProperty showLinesProperty = new SimpleBooleanProperty(true);
	private SimpleBooleanProperty followCarretProperty = new SimpleBooleanProperty(true);
	private PopOver goToLinePopOver = null;
	private LogCodeAreaSyntaxProvider syntaxProvider = new LogCodeAreaSyntaxProvider();



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
		if (enable)
			this.setParagraphGraphicFactory(LineNumberFactory.get(this));
		else
			this.setParagraphGraphicFactory(null);
	}
	
	@Override
	public void appendText(String text) {
		super.appendText(text);
		if (true) {
			this.moveTo(this.getLength());
			this.requestFollowCaret();
		}
	}
	
	protected void setInputMap() {
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
		if (goToLinePopOver != null)
			goToLinePopOver.hide();
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
		@SuppressWarnings("unused")
		Subscription subscription = this.multiPlainChanges().successionEnds(Duration.ofMillis(100))
				.subscribe(ignore -> this.setStyleSpans(0, computeHighlighting(this.getText())));
	}
	
	@Override
	public StyleSpans<Collection<String>> computeHighlighting(String text) {
		Matcher matcher = syntaxProvider.getPatternMatcher(text);
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
	
	@Override
	public ContextMenu createContextMenu() {
		ContextMenu menu = new ContextMenu();

		MenuItem menuItemCopy = new MenuItem("Copy", JavaFXUtils.createIcon("/icons/copy.png"));
		menuItemCopy.setOnAction(event -> this.copy());

		MenuItem menuItemSearchAndReplace = new MenuItem("Search...", JavaFXUtils.createIcon("/icons/magnify.png"));
		menuItemSearchAndReplace.setOnAction(action -> this.showSearchAndReplacePopup());

		MenuItem menuItemGoToLine = new MenuItem("Go to line...", JavaFXUtils.createIcon("/icons/next.png"));
		menuItemGoToLine.setOnAction(action -> this.goToLineAction());
		
		menu.getItems().addAll(menuItemCopy,menuItemSearchAndReplace, menuItemGoToLine);
		return menu;
	}
	
	private void goToLineAction() {
		if (goToLinePopOver != null)
			return;
		
		TextField textField = new TextField();
		textField.setPromptText("Go to line ...");
		textField.setOnKeyPressed(keyEvent -> {
			if (keyEvent.getCode() == KeyCode.ENTER) {
				if (textField.getText().isEmpty())
					return;
				
				int targetParagraph = Integer.parseInt(textField.getText()) - 1;
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
		Bounds boundsInScene = this.localToScreen(this.getBoundsInLocal());
		goToLinePopOver.show(this, boundsInScene.getMaxX() - goToLinePopOver.getWidth() - 200, boundsInScene.getMinY());
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
	
	public SimpleBooleanProperty showLinesProperty() {
		return showLinesProperty;
	}
	
	public SimpleBooleanProperty followCarretProperty() {
		return followCarretProperty;
	}
}
