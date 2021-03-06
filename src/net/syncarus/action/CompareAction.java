package net.syncarus.action;

import net.syncarus.core.DiffTask;
import net.syncarus.rcp.ResourceRegistry;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.ui.PlatformUI;

/**
 * This action starts the <code>DifferentiationJob</code> after several checks.<br>
 * Preconditions are that the <code>DifferenceController</code> has to be
 * initialised, the lock has to be acquired and the user-confirmation has to be
 * positive.
 */
public class CompareAction extends SyncViewAction {
	
	public CompareAction() {
		setText("Compare directories");
		setIcon(ResourceRegistry.IMAGE_COMPARE_LOCATIONS);
	}

	/**
	 * Check if <code>DifferenceController</code> has been properly initialised,
	 * whether a lock could have been acquired and if the user confirmation is
	 * positive.<br>
	 * After these checks, a new instance of <code>DifferentiationJob</code> is
	 * scheduled.
	 * 
	 * @param action
	 *            parameter will be ignored
	 */
	@Override
	public void run() {
		if (!getSyncView().getPlugin().isInitialized()) {
			MessageDialog.openInformation(null, "No paths set", "First you have to define locations A and B before" +
					" comparing their content!");
			return;
		}

		if (!aquireLock())
			return;

		if (!MessageDialog.openConfirm(null, "Comparison", "The following action will remove all " +
				"non-synchronized changes done to the difference-tree.\n\nIt may take several minutes depending on " +
				"the size of the data sets.")) {
			releaseLock();
			return;
		}

		ProgressMonitorDialog pmd = new ProgressMonitorDialog(PlatformUI.getWorkbench().getDisplay().getActiveShell());
		try {
			getSyncView().getProtocol().add("Starting directory comparison.");
			pmd.run(true, true, new DiffTask(getSyncView()));
			getSyncView().getProtocol().add("Finished directory comparison.");
		} catch (Exception e) {
			getSyncView().getProtocol().add("Directory comparison failed.");
			getPlugin().logError("Scheduling the comparison-task failed", e);
			releaseLock();
		}
	}
}
