package gr.sqlbrowserfx.nodes;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.FlowPane;

public class CustomFlowPane extends FlowPane {

	public CustomFlowPane(Node... nodes) {
		super(4, 4, nodes);
		this.setPadding(new Insets(2));
	}
	
	public CustomFlowPane() {
		super(4, 4);
		this.setPadding(new Insets(2));
	}
}
