package net.syncarus.core;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;

import net.syncarus.gui.SyncView;
import net.syncarus.model.CancelationException;
import net.syncarus.model.DiffNode;
import net.syncarus.model.DiffStatus;
import net.syncarus.rcp.SyncarusPlugin;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.PlatformUI;

/**
 * This job builds a Difference Tree showing all the detected differences. <br>
 */
public class DiffTask implements IRunnableWithProgress {
	private static final int WORK_MAX = 1000;

	private IProgressMonitor monitor = null;

	private class GuiRefresher implements Runnable {
		private IOException ioException = null;

		public void setException(IOException e) {
			ioException = e;
		}

		@Override
		public void run() {
			syncView.update();
			if (ioException != null) {
				MessageDialog.openWarning(null, "Differentiation stopped",
						"Errors occured: " + ioException.getMessage());
			} else if (!DiffController.getRootDiffNode().hasChildren() && !monitor.isCanceled())
				MessageDialog.openInformation(null, "Differentiation finished", "No changes have been found!");
		}
	}

	private long filesTotal;
	private long filesProcessed;
	private int worked = 0;
	private final SyncView syncView;

	public DiffTask(SyncView syncView) {
		this.syncView = syncView;
	}

	/**
	 * Checks whether both root paths are set i.e. whether the
	 * <code>DifferenceController</code> has been properly initialised. <br>
	 * The DifferenceTree is build with the help of the
	 * <code>DifferenceController</code> and the treeViewer is updated. <br>
	 * When cancelled, the part-diffTree which has already been built up is
	 * cleaned from useless CLEAN-nodes and presented to the user.
	 */
	@Override
	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		if (!DiffController.isInitialized())
			return;
		try {
			// hold the monitor in a class-variable to be accessible in the
			// recursion
			this.monitor = monitor;
			monitor.beginTask("Differentiaton", WORK_MAX);

			monitor.subTask("Calculating size of datasets to compare");
			DiffController.LOG.add("Calculating size of datasets to compare");
			filesTotal = FileOperation.totalNumOfFiles(DiffController.getRootDiffNode().getAbsoluteFileA());

			DiffController.LOG.add("Comparing directories");
			monitor.subTask("Comparison of locations A and B");

			GuiRefresher guiRefresher = new GuiRefresher();

			try {
				DiffController.resetRootDiffNode();
				buildDiffTreeRecursively(DiffController.getRootDiffNode());
				monitor.done();
			} catch (CancelationException e) {
				// Differentiation was aborted - remove loose clean nodes - it
				// is very likely that such nodes exist after an exception
				DiffController.cleanupDiffTree();
			} catch (IOException e) {
				guiRefresher.setException(e);
				DiffController.cleanupDiffTree();
			}

			// report result
			PlatformUI.getWorkbench().getDisplay().syncExec(guiRefresher);
		} catch (RuntimeException e) {
			SyncarusPlugin.logError("Exception occured during differenation process.", e);
		} finally {
			DiffController.releaseLock();
		}
	}

	/**
	 * builds a tree consisting of nodes and lists which represent the
	 * difference between locations A and B. Nodes represent folders
	 * which have differences somewhere below and list-entries represent
	 * file-differences or folders which will be copied, removed, etc. Every
	 * list-entry and node is identified by its relative path.<br>
	 * The mechanism generates temporary nodes with status CLEAN and removes
	 * them again after the recursion when there were no differences i.e. when
	 * no children were appended.
	 * 
	 * @param localNode
	 */
	private void buildDiffTreeRecursively(DiffNode localNode) throws CancelationException,
			IOException {
		Set<String> pathBSet = new HashSet<String>();

		File dirA = localNode.getAbsoluteFileA();
		File dirB = localNode.getAbsoluteFileB();

		// null denotes an I/O error
		if (dirA.listFiles() == null)
			throw new IOException("The directory '" + dirA.getAbsolutePath() + "' causes an I/O error!");

		// null denotes an I/O error
		if (dirB.listFiles() == null)
			throw new IOException("The directory '" + dirB.getAbsolutePath() + "' causes an I/O error!");

		// copy all children of localRootRight to a HashMap
		for (File childB : dirB.listFiles()) {
			String relativePathChild = localNode.getRelativePath() + File.separator + childB.getName();
			pathBSet.add(relativePathChild);
		}

		for (File childA : dirA.listFiles()) {
			String relativePathChild = localNode.getRelativePath() + File.separator + childA.getName();
			if (!DiffController.fileFilter.isValid(childA.getName()))
				continue;

			if (!pathBSet.contains(relativePathChild)) {
				// if location B doesn't contain this file/folder
				// add this file/folder to localNode with appropriate status
				localNode.createChildNode(relativePathChild, childA.isDirectory(), DiffStatus.COPY_TO_B);
			} else {
				// location B also contains file/folder with the same name
				// remove that file/folder from the location B map because it is also
				// in location A
				pathBSet.remove(relativePathChild);
				File childB = new File(localNode.getAbsoluteFileB(), childA.getName());

				if (childA.isDirectory()) {
					// add a node with status clean and exec the recursion on
					// this folder
					DiffNode childNode = localNode.createChildNode(relativePathChild, true, DiffStatus.CLEAN);
					buildDiffTreeRecursively(childNode);
					// when there were no differences - no subNodes were created
					// and this node is removed again
					if (!childNode.hasChildren())
						localNode.removeChildNode(childNode);
				} else {
					DiffStatus status = compareFiles(childA, childB);
					if (DiffController.syncTimestamps && status == DiffStatus.TOUCH) {
						File oldFile, newFile;
						if (childA.lastModified() < childB.lastModified()) {
							oldFile = childA;
							newFile = childB;
						} else {
							oldFile = childB;
							newFile = childA;
						}
						SyncTask.touchFile(oldFile, newFile);
					} else if (status != DiffStatus.CLEAN)
						localNode.createChildNode(relativePathChild, false, status);
				}
			}

			// increment process monitor
			worked(childA);
		}

		// add remaining file from side B to the difference-tree
		for (String relativePathChild : pathBSet) {
			File childB = new File(localNode.getRoot().getAbsoluteFileB(), relativePathChild);
			if (!DiffController.fileFilter.isValid(childB.getName()))
				continue;

			localNode.createChildNode(relativePathChild, childB.isDirectory(), DiffStatus.REMOVE_FROM_B);
		}
	}

	private DiffStatus compareFiles(File fileA, File fileB) throws IOException {
		if (fileA.length() == fileB.length() && fileA.lastModified() != fileB.lastModified()
				&& DiffController.syncTimestampsWithoutChecksum)
			return DiffStatus.TOUCH;

		if (fileA.lastModified() < fileB.lastModified()) {
			if (FileUtils.contentEquals(fileA, fileB))
				return DiffStatus.TOUCH;

			// file B is newer -> file A will be overwritten
			return DiffStatus.REPLACE_A;
		} else if (fileA.lastModified() > fileB.lastModified()) {
			if (FileUtils.contentEquals(fileA, fileB))
				return DiffStatus.TOUCH;

			// file A is newer -> file B will be overwritten
			return DiffStatus.REPLACE_B;
		} else if (fileA.length() != fileB.length()) {
			// files have same change date but different size -> conflict
			return DiffStatus.CONFLICT;
		}
		return DiffStatus.CLEAN;
	}

	/**
	 * help-function which is heavily used by
	 * <code>buildDiffTreeRecursively</code>.<br>
	 * It increments the number of processed files by <code>numOfFiles</code>
	 * and updates the monitor appropriately.<br>
	 * It also checks for user-cancel inputs and throws a
	 * <code>RuntimeException</code> on a cancel() which is caught in the
	 * <code>run()</code>-Method of this Job.
	 * 
	 * @param numOfFiles
	 */
	private void worked(File file) throws CancelationException {
		if (monitor.isCanceled())
			throw new CancelationException();

		if (!file.isFile())
			return;

		filesProcessed++;

		int step = (int) ((filesProcessed * WORK_MAX) / filesTotal) - worked;
		if (step == 0)
			return;

		monitor.worked(step);
		worked += step;
	}
}
