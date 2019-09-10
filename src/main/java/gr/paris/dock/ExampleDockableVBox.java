package gr.paris.dock;

import org.dockfx.DockNode;

import javafx.scene.layout.VBox;

public class ExampleDockableVBox extends VBox implements Dockable {

	DockNode thisDockNode;
	
	public ExampleDockableVBox() {
		super();
		thisDockNode = new DockNode(this);
	}

	@Override
	public DockNode asDockNode() {
		return thisDockNode;
	}

	
}
