package gr.sqlbrowserfx.nodes.codeareas.typescript;

import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;

import gr.sqlbrowserfx.nodes.ContextMenuOwner;
import gr.sqlbrowserfx.nodes.codeareas.AutoCompleteCodeArea;
import gr.sqlbrowserfx.nodes.codeareas.HighLighter;
import gr.sqlbrowserfx.nodes.sqlpane.CustomPopOver;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;

public class TypeScriptCodeArea extends AutoCompleteCodeArea<TypeScriptCodeAreaSyntaxProvider> implements ContextMenuOwner, HighLighter {

	protected MenuItem menuItemRun;
	private CustomPopOver sqlQueryPopOver;
	
	public TypeScriptCodeArea() {
		this(null);
	}
	
	public TypeScriptCodeArea(String text) {
		this(text, true, true, false);
	}

	public TypeScriptCodeArea(String text, boolean editable, boolean withMenu, boolean autoFormat) {
		super(text, editable, withMenu, autoFormat);
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
	
	@Override
	protected TypeScriptCodeAreaSyntaxProvider initSyntaxProvider() {
		return new TypeScriptCodeAreaSyntaxProvider();
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
}
