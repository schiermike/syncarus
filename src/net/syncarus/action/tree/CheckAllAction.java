package net.syncarus.action.tree;

import net.syncarus.action.SyncViewAction;
import net.syncarus.core.DiffControl;
import net.syncarus.model.DiffNode;
import net.syncarus.rcp.ResourceRegistry;

public class CheckAllAction extends SyncViewAction {
	private boolean checked = false;

	public CheckAllAction() {
		setText("Check all nodes");
		setIcon(ResourceRegistry.IMAGE_SELECT_TREE);
	}

	@Override
	public void run() {
		if (!DiffControl.isInitialized())
			return;

		setText(checked ? "Check all nodes" : "Uncheck all nodes");
		checked = !checked;

		for (DiffNode node : DiffControl.getRootDiffNode().getChildren())
			getTreeViewer().setSubtreeChecked(node, checked);
	}
}
