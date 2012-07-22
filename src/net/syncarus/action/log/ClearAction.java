package net.syncarus.action.log;

import net.syncarus.action.SyncarusAction;
import net.syncarus.core.DiffControl;
import net.syncarus.rcp.ResourceRegistry;

public class ClearAction extends SyncarusAction {

	public ClearAction() {
		setText("Clear log history");
		setIcon(ResourceRegistry.IMAGE_CLEAR_LOG);
		setToolTipText("Clear all old log entries");
	}

	@Override
	public void run() {
		DiffControl.LOG.clear();
	}
}
