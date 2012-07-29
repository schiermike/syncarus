package net.syncarus.action;

import net.syncarus.core.DiffController;
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
		if (!DiffController.aquireLock()) {
			MessageDialog.openWarning(null, "Application busy",
					"The requested operation cannot be executed due to other currently running operations!");
			return;
		}
		DiffController.releaseLock();

		DirSelectWizard wizard = new DirSelectWizard();
		if (wizard.open())
			new CompareAction().run();
	}
}
