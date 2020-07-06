package gr.sqlbrowserfx.nodes.codeareas.log;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;

import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.reactfx.Subscription;

import gr.sqlbrowserfx.nodes.ContextMenuOwner;
import gr.sqlbrowserfx.nodes.SearchAndReplacePopOver;
import gr.sqlbrowserfx.nodes.codeareas.HighLighter;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import javafx.geometry.Bounds;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;

public class LogCodeArea extends CodeArea implements ContextMenuOwner, HighLighter {

	private SearchAndReplacePopOver searchAndReplacePopOver;

	public LogCodeArea() {
		super();
		searchAndReplacePopOver = new SearchAndReplacePopOver(this, false);
		this.setEditable(false);
		this.enableHighlighting();
		this.setContextMenu(this.createContextMenu());
		this.setKeys();
	}
	
	@Override
	public void appendText(String text) {
		super.appendText(text);
		if (true) {
			this.moveTo(this.getLength());
			this.requestFollowCaret();
		}
	}
	
	private void setKeys() {
		this.setOnKeyPressed(keyEvent -> {
			if (keyEvent.isControlDown() && keyEvent.getCode() == KeyCode.F) {
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
	}
	@Override
	public void enableHighlighting() {
		@SuppressWarnings("unused")
		Subscription subscription = this.multiPlainChanges().successionEnds(Duration.ofMillis(100))
				.subscribe(ignore -> this.setStyleSpans(0, computeHighlighting(this.getText())));
	}
	
	@Override
	public StyleSpans<Collection<String>> computeHighlighting(String text) {
		Matcher matcher = LogAreaSyntax.PATTERN.matcher(text);
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
	
	@Override
	public ContextMenu createContextMenu() {
		ContextMenu menu = new ContextMenu();

		MenuItem menuItemCopy = new MenuItem("Copy", JavaFXUtils.icon("/res/copy.png"));
		menuItemCopy.setOnAction(event -> this.copy());

		MenuItem menuItemSearchAndReplace = new MenuItem("Search...", JavaFXUtils.icon("/res/magnify.png"));
		menuItemSearchAndReplace.setOnAction(action -> this.showSearchAndReplacePopup());

		menu.getItems().addAll(menuItemCopy,menuItemSearchAndReplace);
		return menu;
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
}
