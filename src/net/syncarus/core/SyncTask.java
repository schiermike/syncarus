package net.syncarus.core;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import net.syncarus.gui.SyncView;
import net.syncarus.model.CancelationException;
import net.syncarus.model.DiffNode;
import net.syncarus.model.DiffStatus;
import net.syncarus.model.SyncException;
import net.syncarus.rcp.SyncarusPlugin;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.PlatformUI;

/**
 * This job gets all checked nodes from the difference tree and processes them
 * iteratively.
 */
public class SyncTask implements IRunnableWithProgress {
	private class GuiRefresher implements Runnable {
		int messageType = 0;
		String title;
		String message;
		Exception error;

		@Override
		public void run() {
			syncView.update();
			switch (messageType) {
			case 0:
				MessageDialog.openInformation(null, title, message);
				break;
			case 1:
				SyncarusPlugin.logError(message, error);
				break;
			case 2:
				MessageDialog.openWarning(null, title, message);
				break;
			}
		}
	}

	private List<DiffNode> diffNodeList = new ArrayList<DiffNode>();
	private IProgressMonitor monitor = null;

	private long numOfBytesTotal = 0;
	private long numOfBytesProcessed = 0;
	private int worked = 0;
	private final SyncView syncView;

	/**
	 * @param syncView
	 * @param name
	 *            name of the job which will be displayed in the process-view
	 * @param treeViewer
	 *            treeViewer of the main-view where the user selects the nodes
	 *            which should be processed
	 */
	public SyncTask(SyncView syncView, List<DiffNode> diffNodes) {
		this.syncView = syncView;
		diffNodeList.addAll(diffNodes);
	}

	/**
	 * Collect all checked nodes from the DifferenceTree using a separated
	 * <code>DiffNodeCollectorThread</code>.<br>
	 * Then, <code>calcNumOfBytesToCopy(List)</code> will calculate the number
	 * of bytes to copy. Now, the actual synchronisation is started with
	 * <code>synchronise(List)</code>.<br>
	 * This method can be interrupted by an IO-Error or by user-cancellation.
	 * After such an event, a call to <code>cleanupDiffTree()</code> removes all
	 * temporary CLEAN-nodes.<br>
	 * Finally, another separated thread refreshes the DifferenceTree.
	 * 
	 * @param monitor
	 *            this monitor is used to inform the user about the progress
	 */
	@Override
	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		this.monitor = monitor;
		GuiRefresher guiRefresher = new GuiRefresher();

		try {
			monitor.beginTask("Synchronisation", 1000);
			monitor.subTask("Calculating number bytes to copy");
			calcNumOfBytesToCopy(diffNodeList);

			monitor.subTask("Synchronisation in progress");
			// Synchronise selected nodes and destroy processed nodes afterwards
			synchronize(diffNodeList);
			// now remove CLEAN directories having no children
			DiffControl.cleanupDiffTree();
			monitor.done();

			guiRefresher.messageType = 0;
			guiRefresher.title = "Synchronisation completed";
			guiRefresher.message = "Synchronisation completed.";
		} catch (SyncException e) {
			guiRefresher.messageType = 1;
			guiRefresher.title = "Synchronisation Error Occured";
			guiRefresher.error = e;
			return;
		} catch (IOException e) {
			guiRefresher.messageType = 1;
			guiRefresher.title = "IO-Error Occured";
			guiRefresher.error = e;
			return;
		} catch (CancelationException e) {
			guiRefresher.messageType = 2;
			guiRefresher.title = "Synchronisation Cancelled";
			guiRefresher.message = "User canceled the Synchronisation process!";
			return;
		} finally {
			PlatformUI.getWorkbench().getDisplay().syncExec(guiRefresher);
			DiffControl.releaseLock();
		}
	}

	/**
	 * sum up all bytes which will be copied ignoring TOUCH, CONFLICT and REMOVE
	 * states
	 * 
	 * @param diffNodeList
	 *            nodes whose sizes will be summed up
	 */
	private void calcNumOfBytesToCopy(List<DiffNode> diffNodeList) {
		for (DiffNode node : diffNodeList) {
			switch (node.getStatus()) {
			case MOVE_TO_LEFT:
			case OVERWRITE_LEFT:
				File rightFile = DiffControl.toRightFile(node);
				if (rightFile.isFile())
					numOfBytesTotal += rightFile.length();
				else
					numOfBytesTotal += FileOperation.totalNumOfBytes(rightFile);
				break;
			case MOVE_TO_RIGHT_SIDE:
			case OVERWRITE_RIGHT:
				File leftFile = DiffControl.toLeftFile(node);
				if (leftFile.isFile())
					numOfBytesTotal += leftFile.length();
				else
					numOfBytesTotal += FileOperation.totalNumOfBytes(leftFile);
				break;
			}
		}
	}

	/**
	 * Core-functionality: Process the whole list depending on the state of the
	 * nodes.
	 * 
	 * @param diffNodeList
	 *            nodes which will be copied / removed / touched etc.
	 * @throws IOException
	 *             this may happen due to a file-lock etc.
	 * @throws CancelationException
	 */
	private void synchronize(List<DiffNode> diffNodeList) throws IOException, CancelationException {
		for (DiffNode node : diffNodeList) {
			// skip all CLEAN, CONFLICT and UNKNOWN stated nodes
			if ((node.getStatus() == DiffStatus.CLEAN) || (node.getStatus() == DiffStatus.CONFLICT)
					|| (node.getStatus() == DiffStatus.UNKNOWN))
				continue;

			File leftFile = DiffControl.toLeftFile(node);
			File rightFile = DiffControl.toRightFile(node);
			// process all other nodes
			switch (node.getStatus()) {
			case MOVE_TO_LEFT:
				FileOperation.copy(rightFile, leftFile, this);
				node.remove();
				break;

			case MOVE_TO_RIGHT_SIDE:
				FileOperation.copy(leftFile, rightFile, this);
				node.remove();
				break;

			case OVERWRITE_LEFT:
				if (node.isDirectory())
					throw new SyncException(SyncException.INCONSISTENT_STATE_EXCEPTION, "A DiffNode has state "
							+ node.getStatus() + " but is a directory - this can't be!");

				FileOperation.copy(rightFile, leftFile, this);
				node.remove();
				break;

			case OVERWRITE_RIGHT:
				if (node.isDirectory())
					throw new SyncException(SyncException.INCONSISTENT_STATE_EXCEPTION, "A DiffNode has state "
							+ node.getStatus() + " but is a directory - this can't be!");

				FileOperation.copy(leftFile, rightFile, this);
				node.remove();
				break;

			// the older file gets the change date of the newer one - same
			// contents are assumed
			case TOUCH:
				if (FileUtils.isFileNewer(leftFile, rightFile))
					touchFile(rightFile, leftFile);
				else if (FileUtils.isFileNewer(rightFile, leftFile))
					touchFile(leftFile, rightFile);
				else
					throw new SyncException(SyncException.INCONSISTENT_STATE_EXCEPTION,
							"It wasn't necessary to touch a file because both files '" + leftFile.getAbsolutePath()
									+ "' and '" + rightFile.getAbsolutePath() + "' have same change-date!");
				node.remove();
				break;

			case REMOVE_LEFT:
				DiffControl.LOG.add("Deleting '" + leftFile.getAbsolutePath() + "'");
				FileUtils.forceDelete(leftFile);
				node.remove();
				break;

			case REMOVE_RIGHT:
				DiffControl.LOG.add("Deleting '" + rightFile.getAbsolutePath() + "'");
				FileUtils.forceDelete(rightFile);
				node.remove();
				break;

			default:
				throw new SyncException(SyncException.INCONSISTENT_STATE_EXCEPTION, "Unknown state detected("
						+ node.getStatus() + ")!");
			}
		}
	}

	public static void touchFile(File oldFile, File newFile) {
		DiffControl.LOG.add("Touching file '" + oldFile.getAbsolutePath() + "'");

		boolean changedWritePerms = false;
		if (!newFile.canWrite()) {
			if (!newFile.setWritable(true))
				throw new SyncException(SyncException.FILE_OPERATION_EXCEPTION, "Couldn't modify timestamp of file '"
						+ newFile + "'");
			changedWritePerms = true;
		}

		try {
			if (!newFile.setLastModified(oldFile.lastModified()))
				throw new SyncException(SyncException.FILE_OPERATION_EXCEPTION,
						"Couldn't modify the modification date of file '" + newFile + "'");
		} finally {
			if (changedWritePerms)
				newFile.setWritable(false);
		}
	}

	/**
	 * help-function of <code>synchronise(List)</code>.<br>
	 * Adds numOfBytes to a class-internal counter. This <code>long</code>
	 * counter will be mapped onto the <code>int</code>-value of the
	 * monitor.worked() parameter.
	 * 
	 * @param numOfBytes
	 *            number of bytes which just have been copied.
	 * @throws CancelationException
	 */
	public void worked(long numOfBytes) throws CancelationException {
		if (monitor.isCanceled())
			throw new CancelationException();

		numOfBytesProcessed += numOfBytes;

		int step = 0;
		if (numOfBytesTotal != 0) {
			step = (int) ((numOfBytesProcessed * 1000) / numOfBytesTotal) - worked;
		}

		if (step == 0)
			return;

		monitor.worked(step);
		worked += step;
	}
}
