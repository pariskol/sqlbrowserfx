package gr.sqlbrowserfx.nodes.codeareas;

import gr.sqlbrowserfx.nodes.InputMapOwner;
import gr.sqlbrowserfx.nodes.SearchAndReplacePopOver;
import javafx.event.Event;
import javafx.geometry.Bounds;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;

public class SearchableCodeArea extends CodeArea implements InputMapOwner {

    private final SearchAndReplacePopOver searchAndReplacePopOver;

    public SearchableCodeArea() {
        searchAndReplacePopOver = new SearchAndReplacePopOver(this, false);
        setOnMouseClicked(event -> searchAndReplacePopOver.hide());
        setInputMap();
    }

    private void showSearchAndReplacePopup() {
        if (!this.getSelectedText().isEmpty()) {
            searchAndReplacePopOver.getFindField().setText(this.getSelectedText());
            searchAndReplacePopOver.getFindField().selectAll();
        }
        Bounds boundsInScene = this.localToScreen(this.getBoundsInLocal());
        searchAndReplacePopOver.show(getParent(), boundsInScene.getMaxX() - 400, boundsInScene.getMinY());
    }

    @Override
    public void setInputMap() {
        var searchAndReplace = InputMap.consume(
                EventPattern.keyPressed(KeyCode.F, KeyCombination.CONTROL_DOWN),
                action -> this.showSearchAndReplacePopup());

        Nodes.addInputMap(this, searchAndReplace);
    }
}
