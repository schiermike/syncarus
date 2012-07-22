package net.syncarus.action.tree;

import net.syncarus.action.SyncViewAction;
import net.syncarus.rcp.ResourceRegistry;

public class ExpandAction extends SyncViewAction {
	
	public ExpandAction() {
		setText("Expand tree");
		setIcon(ResourceRegistry.IMAGE_EXPAND_TREE);
		setToolTipText("Expand the comparison tree");
	}

	@Override
	public void run() {
		getTreeViewer().expandAll();
	}
}
