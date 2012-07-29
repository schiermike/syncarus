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
			} else if (!DiffControl.getRootDiffNode().hasChildren() && !monitor.isCanceled())
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
	 * Checks whether both source and target paths are set i.e. whether the
	 * <code>DifferenceController</code> has been properly initialised. <br>
	 * The DifferenceTree is build with the help of the
	 * <code>DifferenceController</code> and the treeViewer is updated. <br>
	 * When cancelled, the part-diffTree which has already been built up is
	 * cleaned from useless CLEAN-nodes and presented to the user.
	 */
	@Override
	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		if (!DiffControl.isInitialized())
			return;
		try {
			// hold the monitor in a class-variable to be accessible in the
			// recursion
			this.monitor = monitor;
			monitor.beginTask("Differentiaton", WORK_MAX);

			monitor.subTask("Calculating size of datasets to compare");
			DiffControl.LOG.add("Calculating size of datasets to compare");
			filesTotal = FileOperation.totalNumOfFiles(DiffControl.toFileA(File.separator));

			DiffControl.LOG.add("Comparing source and target directories");
			monitor.subTask("Differentiation of source and target directories");

			GuiRefresher guiRefresher = new GuiRefresher();

			try {
				DiffControl.resetRootDiffNode();
				buildDiffTreeRecursively(File.separator, DiffControl.getRootDiffNode());
				monitor.done();
			} catch (CancelationException e) {
				// Differentiation was aborted - remove loose clean nodes - it
				// is very likely that such nodes exist after an exception
				DiffControl.cleanupDiffTree();
			} catch (IOException e) {
				guiRefresher.setException(e);
				DiffControl.cleanupDiffTree();
			}

			// report result
			PlatformUI.getWorkbench().getDisplay().syncExec(guiRefresher);
		} catch (RuntimeException e) {
			SyncarusPlugin.logError("Exception occured during differenation process.", e);
		} finally {
			DiffControl.releaseLock();
		}
	}

	/**
	 * builds a tree consisting of nodes and lists which represent the
	 * difference between source and target location. Nodes represent folders
	 * which have differences somewhere below and list-entries represent
	 * file-differences or folders which will be copied, removed, etc. Every
	 * list-entry and node is identified by its relative path.<br>
	 * The mechanism generates temporary nodes with status CLEAN and removes
	 * them again after the recursion when there were no differences i.e. when
	 * no children were appended.
	 * 
	 * @param relativePath
	 *            the relative path of source and target
	 * @param localNode
	 *            the node holding the difference-information at this recursion
	 *            level
	 */
	private void buildDiffTreeRecursively(String relativePath, DiffNode localNode) throws CancelationException,
			IOException {
		Set<String> rightPathSet = new HashSet<String>();

		File left = DiffControl.toFileA(relativePath);
		File right = DiffControl.toFileB(relativePath);

		// null denotes an I/O error
		if (left.listFiles() == null)
			throw new IOException("The directory '" + left.getAbsolutePath() + "' causes an I/O error!");

		// null denotes an I/O error
		if (right.listFiles() == null)
			throw new IOException("The directory '" + right.getAbsolutePath() + "' causes an I/O error!");

		// copy all children of localRootRight to a HashMap
		for (File rightChild : right.listFiles()) {
			String relativePathChild = DiffControl.getRelativePath(rightChild);
			rightPathSet.add(relativePathChild);
		}

		for (File leftChild : left.listFiles()) {
			String relativePathChild = DiffControl.getRelativePath(leftChild);
			if (!DiffControl.fileFilter.isValid(leftChild.getName()))
				continue;

			if (!rightPathSet.contains(relativePathChild)) {
				// if target doesn't contain this file/folder
				// add this file/folder to localNode with appropriate status
				localNode.createChildNode(relativePathChild, leftChild.isDirectory(), DiffStatus.COPY_TO_B);
			} else {
				// target also contains file/folder with the same name
				// remove that file/folder from the targetMap because it is also
				// in the source
				rightPathSet.remove(relativePathChild);
				File rightChild = DiffControl.toFileB(relativePathChild);

				if (leftChild.isDirectory()) {
					// add a node with status clean and exec the recursion on
					// this folder
					DiffNode childNode = localNode.createChildNode(relativePathChild, true, DiffStatus.CLEAN);
					buildDiffTreeRecursively(relativePathChild, childNode);
					// when there were no differences - no subNodes were created
					// and this node is removed again
					if (!childNode.hasChildren())
						localNode.removeChildNode(childNode);
				} else {
					DiffStatus status = compareFiles(leftChild, rightChild);
					if (DiffControl.syncTimestamps && status == DiffStatus.TOUCH) {
						File oldFile, newFile;
						if (leftChild.lastModified() < rightChild.lastModified()) {
							oldFile = leftChild;
							newFile = rightChild;
						} else {
							oldFile = rightChild;
							newFile = leftChild;
						}
						SyncTask.touchFile(oldFile, newFile);
					} else if (status != DiffStatus.CLEAN)
						localNode.createChildNode(relativePathChild, false, status);
				}
			}

			// increment process monitor
			worked(leftChild);
		}

		// add remaining file on right side to difference-tree
		for (String relativePathChild : rightPathSet) {
			File rightFile = DiffControl.toFileB(relativePathChild);
			if (!DiffControl.fileFilter.isValid(rightFile.getName()))
				continue;

			localNode.createChildNode(relativePathChild, rightFile.isDirectory(), DiffStatus.REMOVE_FROM_B);
		}
	}

	private DiffStatus compareFiles(File leftFile, File rightFile) throws IOException {
		if (leftFile.length() == rightFile.length() && leftFile.lastModified() != rightFile.lastModified()
				&& DiffControl.syncTimestampsWithoutChecksum)
			return DiffStatus.TOUCH;

		if (leftFile.lastModified() < rightFile.lastModified()) {
			if (FileUtils.contentEquals(leftFile, rightFile))
				return DiffStatus.TOUCH;

			// target file is newer -> source file will be overwritten
			return DiffStatus.REPLACE_A;
		} else if (leftFile.lastModified() > rightFile.lastModified()) {
			if (FileUtils.contentEquals(leftFile, rightFile))
				return DiffStatus.TOUCH;

			// source file is newer -> target file will be overwritten
			return DiffStatus.REPLACE_B;
		} else if (leftFile.length() != rightFile.length()) {
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
