package gr.paris.dock;

import org.dockfx.DockNode;

public interface Dockable {

	/**
	 * Returns current Node wrapped in DockNode.
	 * 
	 * @return
	 */
	public DockNode asDockNode();
}
