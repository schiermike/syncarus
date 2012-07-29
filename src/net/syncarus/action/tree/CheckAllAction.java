package net.syncarus.action.tree;

import net.syncarus.action.SyncViewAction;
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
		DiffNode rootNode = (DiffNode) getTreeViewer().getInput();
		if (rootNode == null)
			return;

		setText(checked ? "Check all nodes" : "Uncheck all nodes");
		checked = !checked;
		
		for (DiffNode node : rootNode.getChildren())
			getTreeViewer().setSubtreeChecked(node, checked);
	}
}
