package gr.sqlbrowserfx.dock.nodes;

import org.dockfx.DockNode;
import org.dockfx.DockPane;
import org.dockfx.Dockable;

import gr.sqlbrowserfx.nodes.LogConsolePane;
import gr.sqlbrowserfx.utils.JavaFXUtils;

public class DLogConsolePane extends LogConsolePane implements Dockable {
	private DockNode thisDockNode = null;
	private DockPane dockPane;

	public DLogConsolePane(DockPane dockPane) {
		this.dockPane = dockPane;
	}
	
	@Override
	public DockNode asDockNode() {
		if (thisDockNode == null) {
			thisDockNode = new DockNode(dockPane, this, "Log", JavaFXUtils.createIcon("/icons/monitor.png"), 600.0, 400.0);
			thisDockNode.setOnClose(() -> this.stopTailing());
		}
		return thisDockNode;
	}

}
