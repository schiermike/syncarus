package net.syncarus.action;

import java.util.ArrayList;
import java.util.List;

import net.syncarus.gui.SyncView;
import net.syncarus.model.DiffNode;
import net.syncarus.rcp.SyncarusPlugin;

import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.IStructuredSelection;

public abstract class SyncViewAction extends SyncarusAction {

	public SyncView getSyncView() {
		return SyncarusPlugin.getInstance().getSyncView();
	}
	
	protected CheckboxTreeViewer getTreeViewer() {
		return getSyncView().getViewer();
	}
	
	public List<DiffNode> getSelectedNodes() {
		List<DiffNode> diffNodes = new ArrayList<DiffNode>();
		for (Object object : ((IStructuredSelection) getTreeViewer().getSelection()).toArray())
			diffNodes.add((DiffNode) object);

		return diffNodes;
	}

	protected List<DiffNode> getCheckedElements() {
		List<DiffNode> diffNodes = new ArrayList<DiffNode>();
		for (Object object : getTreeViewer().getCheckedElements())
			diffNodes.add((DiffNode) object);

		return diffNodes;
	}
}
