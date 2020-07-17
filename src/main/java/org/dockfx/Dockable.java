package org.dockfx;

public interface Dockable {

	/**
	 * Returns Node wrapped in DockNode.
	 * To use this interface you have to keep a DockNode instance as member of class.
	 * Then you can dock the node by calling node.asDockNode().dock(...)
	 * 
	 * @return
	 */
	public DockNode asDockNode();
}
