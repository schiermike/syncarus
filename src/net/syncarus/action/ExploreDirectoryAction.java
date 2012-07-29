package net.syncarus.action;

import java.io.IOException;

import net.syncarus.core.DiffControl;
import net.syncarus.gui.SyncTreeViewer;
import net.syncarus.model.DiffNode;
import net.syncarus.model.DiffStatus;
import net.syncarus.rcp.ResourceRegistry;
import net.syncarus.rcp.SyncarusPlugin;

public class ExploreDirectoryAction extends SyncViewAction {
	private final boolean isLeftSideAction;

	public ExploreDirectoryAction(SyncTreeViewer viewer, boolean isLeftSideAction) {
		setText(isLeftSideAction ? "Show left folder in Explorer" : "Show right folder in Explorer");
		setIcon(ResourceRegistry.IMAGE_SHOW_IN_EXPLORER);
		this.isLeftSideAction = isLeftSideAction;
	}

	@Override
	public boolean isEnabled() {
		if (getSelectedNodes().size() != 1)
			return false;

		DiffNode node = getSelectedNodes().get(0);

		if (node.isDirectory()) {
			DiffStatus status = node.getStatus();

			if (status == DiffStatus.CLEAN)
				return true;

			if (isLeftSideAction && (status == DiffStatus.COPY_TO_B || status == DiffStatus.REMOVE_FROM_A))
				return true;

			if (!isLeftSideAction && (status == DiffStatus.COPY_TO_A || status == DiffStatus.REMOVE_FROM_B))
				return true;
		}

		return false;
	}

	@Override
	public void run() {
		if (getSelectedNodes().isEmpty())
			return;

		DiffNode node = getSelectedNodes().get(0);

		// get path and convert it to an UNC path for windows to execute
		if (isLeftSideAction)
			exec(DiffControl.toFileA(node).getAbsolutePath());
		else
			exec(DiffControl.toFileB(node).getAbsolutePath());
	}

	private void exec(String path) {
		IOException ex;
		if (SyncarusPlugin.isOSWindows()) {
			try {
				Runtime.getRuntime().exec("explorer /n,/e," + path);
				return;
			} catch (IOException e) {
				ex = e;
			}
		} else {
			try {
				Runtime.getRuntime().exec(new String[] { "nautilus", path });
				return;
			} catch (IOException e) {
				ex = e;
			}

			try {
				Runtime.getRuntime().exec(new String[] { "konqueror", path });
				return;
			} catch (IOException e) {
				ex = e;
			}
		}

		SyncarusPlugin.logError("Opening a file browser window failed.", ex);
	}
}
