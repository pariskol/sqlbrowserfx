package gr.sqlbrowserfx.dock;

import org.dockfx.DockNode;

public interface Dockable {

	/**
	 * Returns Node wrapped in DockNode.
	 * To use this interface you have to keep a DockNode instance as member in class.
	 * Then you can dock the node by calling node.asDockNode().dock(...)
	 * 
	 * @return
	 */
	public DockNode asDockNode();
}
