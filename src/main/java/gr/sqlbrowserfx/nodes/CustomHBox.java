package gr.sqlbrowserfx.nodes;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.HBox;

public class CustomHBox extends HBox {

    public CustomHBox(Node... nodes) {
        super(4, nodes);
        setAlignment(Pos.CENTER_LEFT);
    }

    public CustomHBox() {
        super(4);
    }

}
