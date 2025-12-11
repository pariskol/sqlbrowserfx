package gr.sqlbrowserfx.nodes;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

public class CustomVBox extends VBox {
	
	public CustomVBox(Node... nodes) {
		super(4, nodes);
		this.setPadding(new Insets(2));
	}
	
	public CustomVBox() {
		super(4);
		this.setPadding(new Insets(2));
	}
	
}
