package net.syncarus.action;

import net.syncarus.core.DiffController;
import net.syncarus.core.DiffTask;
import net.syncarus.rcp.ResourceRegistry;
import net.syncarus.rcp.SyncarusPlugin;

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
		if (!DiffController.isInitialized()) {
			MessageDialog.openInformation(null, "No paths set",
					"First you have to define locations A and B before comparing their content!");
			return;
		}

		if (!DiffController.aquireLock()) {
			MessageDialog.openWarning(null, "Application is busy",
					"The requested operation cannot be executed due to other currently running operations!");
			return;
		}

		if (!MessageDialog
				.openConfirm(
						null,
						"Data set comparison",
						"The following action will remove all non-synchronized changes done to the difference-tree.\n\nIt may take several minutes depending on the size of the data sets.\nDo you want to proceed?")) {
			DiffController.releaseLock();
			return;
		}

		DiffController.syncTimestamps = MessageDialog
				.openQuestion(
						null,
						"Timestamp synchronization",
						"Should the file comparison process implicitly synchronize timestamps?\nThis will set the timestamp of files in location A equal to the timestamp of files in location B if their content is equal.");
		DiffController.syncTimestampsWithoutChecksum = false;
		if (DiffController.syncTimestamps)
			DiffController.syncTimestampsWithoutChecksum = MessageDialog
					.openQuestion(
							null,
							"Timestamp synchronization",
							"For timestamp synchronization, should it be assumed that files of equal size have the same content?\nThis might significantly speed up the timestamp synchronization process as no checksums have to be calculated. However, changes of files of equal size can no longer be detected.");

		ProgressMonitorDialog pmd = new ProgressMonitorDialog(PlatformUI.getWorkbench().getDisplay().getActiveShell());
		try {
			DiffController.LOG.add("Starting directory comparison.");
			pmd.run(true, true, new DiffTask(getSyncView()));
			DiffController.LOG.add("Finished directory comparison.");
		} catch (Exception e) {
			DiffController.LOG.add("Directory comparison failed.");
			SyncarusPlugin.logError("Scheduling the comparison-task failed", e);
		}
	}
}
