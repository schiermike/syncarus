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

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.PlatformUI;

/**
 * This job gets all checked nodes from the difference tree and processes them
 * iteratively.
 */
public class SyncTask extends SyncarusTask {
	private class GuiRefresher implements Runnable {
		int messageType = 0;
		String title;
		String message;
		Exception error;

		@Override
		public void run() {
			getSyncView().update();
			switch (messageType) {
			case 0:
				MessageDialog.openInformation(null, title, message);
				break;
			case 1:
				getPlugin().logError(message, error);
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

	/**
	 * @param syncView
	 * @param name
	 *            name of the job which will be displayed in the process-view
	 * @param treeViewer
	 *            treeViewer of the main-view where the user selects the nodes
	 *            which should be processed
	 */
	public SyncTask(SyncView syncView, List<DiffNode> diffNodes) {
		super(syncView);
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
			getRootNode().clean();
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
			getSyncView().releaseLock();
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
			case COPY_TO_A:
			case REPLACE_A:
				File fileB = node.getAbsoluteFileB();
				if (fileB.isFile())
					numOfBytesTotal += fileB.length();
				else
					numOfBytesTotal += FileOperation.totalNumOfBytes(fileB);
				break;
			case COPY_TO_B:
			case REPLACE_B:
				File fileA = node.getAbsoluteFileA();
				if (fileA.isFile())
					numOfBytesTotal += fileA.length();
				else
					numOfBytesTotal += FileOperation.totalNumOfBytes(fileA);
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
			if ((node.getStatus() == DiffStatus.CLEAN) || (node.getStatus() == DiffStatus.CONFLICT_TIME)
					|| (node.getStatus() == DiffStatus.UNKNOWN))
				continue;

			File fileA = node.getAbsoluteFileA();
			File fileB = node.getAbsoluteFileB();
			// process all other nodes
			switch (node.getStatus()) {
			case COPY_TO_A:
				FileOperation.copy(fileB, fileA, this);
				node.remove();
				break;

			case COPY_TO_B:
				FileOperation.copy(fileA, fileB, this);
				node.remove();
				break;

			case REPLACE_A:
				FileUtils.forceDelete(fileA);
				FileOperation.copy(fileB, fileA, this);
				node.remove();
				break;

			case REPLACE_B:
				FileUtils.forceDelete(fileB);
				FileOperation.copy(fileA, fileB, this);
				node.remove();
				break;

			// the older file gets the change date of the newer one - same
			// contents are assumed
			case TOUCH:
				if (FileUtils.isFileNewer(fileA, fileB))
					touchFile(fileB, fileA);
				else if (FileUtils.isFileNewer(fileB, fileA))
					touchFile(fileA, fileB);
				else
					throw new SyncException(SyncException.INCONSISTENT_STATE_EXCEPTION,
							"It wasn't necessary to touch a file because both files '" + fileA.getAbsolutePath()
									+ "' and '" + fileB.getAbsolutePath() + "' have same change-date!");
				node.remove();
				break;

			case REMOVE_FROM_A:
				getProtocol().add("Deleting '" + fileA.getAbsolutePath() + "'");
				FileUtils.forceDelete(fileA);
				node.remove();
				break;

			case REMOVE_FROM_B:
				getProtocol().add("Deleting '" + fileB.getAbsolutePath() + "'");
				FileUtils.forceDelete(fileB);
				node.remove();
				break;

			default:
				throw new SyncException(SyncException.INCONSISTENT_STATE_EXCEPTION, "Unknown state detected("
						+ node.getStatus() + ")!");
			}
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
