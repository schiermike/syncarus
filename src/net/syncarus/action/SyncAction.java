package net.syncarus.action;

import net.syncarus.core.SyncTask;
import net.syncarus.rcp.ResourceRegistry;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.ui.PlatformUI;

/**
 * This action starts the <code>SynchronisationJob</code> after several checks.<br>
 * Preconditions are that the lock can be acquired and the user-confirmation was
 * positive.
 */
public class SyncAction extends SyncViewAction {
	public SyncAction() {
		setText("Apply changes");
		setIcon(ResourceRegistry.IMAGE_SYNCHRONIZE);
	}

	@Override
	public void run() {
		
		if (getTreeViewer().getCheckedElements().length == 0) {
			MessageDialog.openInformation(getSyncView().getSite().getShell(), "Synchronization", 
					"No files or folders are selected for synchronization!");
			return;
		}
		
		if (!aquireLock())
			return;

		if (!MessageDialog.openConfirm(null, "Synchronization", "The following action will synchronize "
				+ "all checked nodes in the differences-tree\n\nDepending on the selection, this action may take up "
				+ "to several minutes!\nDo you want to proceed?")) {
			releaseLock();
			return;
		}

		try {
			ProgressMonitorDialog pmd = new ProgressMonitorDialog(PlatformUI.getWorkbench().getDisplay()
					.getActiveShell());
			SyncTask job = new SyncTask(getSyncView(), getCheckedElements());

			getSyncView().getProtocol().add("Starting synchronization process.");
			pmd.run(true, true, job);
			getSyncView().getProtocol().add("Finished synchronization process.");

			new CompareAction().run();
		} catch (Exception e) {
			getSyncView().getProtocol().add("Synchronization process failed.");
			getPlugin().logError("Scheduling the synchronisation-task failed", e);
			releaseLock();
		}
	}
}
