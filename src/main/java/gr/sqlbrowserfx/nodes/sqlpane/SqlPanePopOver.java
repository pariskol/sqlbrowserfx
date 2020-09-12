package gr.sqlbrowserfx.nodes.sqlpane;

import org.controlsfx.control.PopOver;

import javafx.scene.Node;

public class SqlPanePopOver extends PopOver{

	public SqlPanePopOver() {
		this(null);
	}
	
	public SqlPanePopOver(Node node) {
		super(node);
		this.setDetachable(false);
		this.setArrowSize(0);
	}
}
