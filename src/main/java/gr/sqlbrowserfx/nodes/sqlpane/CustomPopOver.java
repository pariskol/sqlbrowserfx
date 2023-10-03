package gr.sqlbrowserfx.nodes.sqlpane;

import org.controlsfx.control.PopOver;

import javafx.scene.Node;

public class CustomPopOver extends PopOver{

	public CustomPopOver() {
		this(null);
	}
	
	public CustomPopOver(Node node) {
		super(node);
		this.setDetachable(false);
		this.setArrowSize(0);
		this.setHideOnEscape(true);
		this.setAutoFix(true);
	}
}
