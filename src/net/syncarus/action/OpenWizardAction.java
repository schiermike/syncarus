package net.syncarus.action;

import net.syncarus.core.DirSelectWizard;
import net.syncarus.rcp.ResourceRegistry;

import org.eclipse.jface.dialogs.MessageDialog;

public class OpenWizardAction extends SyncViewAction {
	
	public OpenWizardAction() {
		setText("Select locations");
		setIcon(ResourceRegistry.IMAGE_SELECT_LOCATION);
	}

	@Override
	public void run() {
		if (!getSyncView().aquireLock()) {
			MessageDialog.openWarning(null, "Application busy",
					"The requested operation cannot be executed due to other currently running operations!");
			return;
		}
		getSyncView().releaseLock();

		DirSelectWizard wizard = new DirSelectWizard(getPlugin());
		if (wizard.open())
			new CompareAction().run();
	}
}
