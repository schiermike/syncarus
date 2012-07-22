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

			if (isLeftSideAction && (status == DiffStatus.MOVE_TO_RIGHT_SIDE || status == DiffStatus.REMOVE_LEFT))
				return true;

			if (!isLeftSideAction && (status == DiffStatus.MOVE_TO_LEFT || status == DiffStatus.REMOVE_RIGHT))
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
			exec(DiffControl.toLeftFile(node).getAbsolutePath());
		else
			exec(DiffControl.toRightFile(node).getAbsolutePath());
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
