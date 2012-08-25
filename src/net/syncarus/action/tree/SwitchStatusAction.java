package net.syncarus.action.tree;

import net.syncarus.action.SyncViewAction;
import net.syncarus.gui.SyncTreeViewer;
import net.syncarus.model.DiffNode;
import net.syncarus.rcp.ResourceRegistry;

public class SwitchStatusAction extends SyncViewAction {
	
	public SwitchStatusAction(SyncTreeViewer viewer) {
		setText("Switch state");
		setIcon(ResourceRegistry.IMAGE_SWITCH_STATUS);
	}

	@Override
	public boolean isEnabled() {
		return !getSelectedNodes().isEmpty();
	}

	@Override
	public void run() {
		for (DiffNode node : getSelectedNodes())
			switchStatus(node);
		getTreeViewer().refresh();
		getTreeViewer().setSelection(getTreeViewer().getSelection());
	}

	// recursively switch all states of nodes
	private void switchStatus(DiffNode node) {
		node.setStatus(node.getStatus().getInvertedDiffStatus());
		if (node.isDirectory())
			for (DiffNode subNode : node.getChildren())
				switchStatus(subNode);
	}
}
