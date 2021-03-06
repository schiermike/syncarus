package net.syncarus.action.log;

import net.syncarus.action.SyncarusAction;
import net.syncarus.rcp.ResourceRegistry;

public class SwitchAction extends SyncarusAction {

	public SwitchAction() {
		super(AS_CHECK_BOX);
		disableLogging();
	}

	@Override
	public void run() {
		if (isChecked())
			enableLogging();
		else
			disableLogging();
	}

	private void enableLogging() {
		setToolTipText("Disable logging facility");
		setIcon(ResourceRegistry.IMAGE_ENABLE);
		getPlugin().getProtocol().setEnabled(true);
	}

	private void disableLogging() {
		setToolTipText("Enable logging facility");
		setIcon(ResourceRegistry.IMAGE_DISABLE);
		getPlugin().getProtocol().setEnabled(false);
	}
}
