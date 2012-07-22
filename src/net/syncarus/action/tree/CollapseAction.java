package net.syncarus.action.tree;

import net.syncarus.action.SyncViewAction;
import net.syncarus.rcp.ResourceRegistry;

public class CollapseAction extends SyncViewAction {
	
	public CollapseAction() {
		setText("Collapse tree");
		setIcon(ResourceRegistry.IMAGE_COLLAPSE_TREE);
		setToolTipText("Collapse the comparison tree");
	}

	@Override
	public void run() {
		getTreeViewer().collapseAll();
	}
}
