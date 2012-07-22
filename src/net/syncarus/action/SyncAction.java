package net.syncarus.action;

import net.syncarus.core.DiffControl;
import net.syncarus.core.SyncTask;
import net.syncarus.rcp.ResourceRegistry;
import net.syncarus.rcp.SyncarusPlugin;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.ui.PlatformUI;


/**
 * This action starts the <code>SynchronisationJob</code> after several
 * checks.<br>
 * Preconditions are that the lock can be acquired and the user-confirmation
 * was positive.
 */
public class SyncAction extends SyncViewAction
{
	public SyncAction()
	{
		setText("Make changes");
		setIcon(ResourceRegistry.IMAGE_SYNCHRONIZE);
	}

	@Override
	public void run()
	{
		if (!DiffControl.aquireLock())
		{
			MessageDialog.openWarning(null, "Application is busy", "The requested operation cannot be executed due to other currently running operations!");
			return;
		}

		if (!MessageDialog.openConfirm(null, "Data set synchronization", "The following action will synchronize all checked nodes in the differences-tree\n\nDepending on the selection, this action may take up to several minutes!\nDo you want to proceed?"))
		{
			DiffControl.releaseLock();
			return;
		}

		try
		{
			ProgressMonitorDialog pmd = new ProgressMonitorDialog(PlatformUI.getWorkbench().getDisplay().getActiveShell());
			SyncTask job = new SyncTask(getSyncView(), getCheckedElements());
			
			DiffControl.LOG.add("Starting synchronization process.");
			pmd.run(true, true, job);
			DiffControl.LOG.add("Finished synchronization process.");
			
			new CompareAction().run();
		}
		catch (Exception e)
		{
			DiffControl.LOG.add("Synchronization process failed.");
			SyncarusPlugin.logError("Scheduling the synchronisation-task failed", e);
		}
	}
}
