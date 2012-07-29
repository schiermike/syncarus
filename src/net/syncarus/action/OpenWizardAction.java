package net.syncarus.action;

import net.syncarus.core.DiffControl;
import net.syncarus.core.DirSelectWizard;
import net.syncarus.rcp.ResourceRegistry;

import org.eclipse.jface.dialogs.MessageDialog;

public class OpenWizardAction extends SyncViewAction {
	
	public OpenWizardAction() {
		setText("Choose directories");
		setIcon(ResourceRegistry.IMAGE_SELECT_LOCATION);
	}

	@Override
	public void run() {
		if (!DiffControl.aquireLock()) {
			MessageDialog.openWarning(null, "Application busy",
					"The requested operation cannot be executed due to other currently running operations!");
			return;
		}
		DiffControl.releaseLock();

		DirSelectWizard wizard = new DirSelectWizard();
		if (wizard.open())
			new CompareAction().run();
	}
}
