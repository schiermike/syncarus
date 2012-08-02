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

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.PlatformUI;

/**
 * This job builds a Difference Tree showing all the detected differences. <br>
 */
public class DiffTask extends SyncarusTask {
	private static final int WORK_MAX = 1000;

	private IProgressMonitor monitor = null;

	private class GuiRefresher implements Runnable {
		private IOException ioException = null;

		public void setException(IOException e) {
			ioException = e;
		}

		@Override
		public void run() {
			getSyncView().update();
			if (ioException != null) {
				MessageDialog.openWarning(null, "Differentiation stopped",
						"Errors occured: " + ioException.getMessage());
			} else if (!getRootNode().hasChildren() && !monitor.isCanceled())
				MessageDialog.openInformation(null, "Differentiation finished", "No changes have been found!");
		}
	}

	private long totalFilesToProcess;
	private long filesProcessedSoFar;

	public DiffTask(SyncView syncView) {
		super(syncView);
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
		if (!getPlugin().isInitialized())
			return;
		try {
			// hold the monitor in a class-variable to be accessible in the
			// recursion
			this.monitor = monitor;
			monitor.beginTask("Differentiaton", WORK_MAX);

			String taskDescription = "Analysing directory structure of A";
			monitor.subTask(taskDescription);
			getProtocol().add(taskDescription);
			totalFilesToProcess = FileOperation.totalNumOfFiles(getRootNode().getAbsoluteFileA());
			
			taskDescription = "Analysing directory structure of B";
			monitor.subTask(taskDescription);
			getProtocol().add(taskDescription);
			totalFilesToProcess += FileOperation.totalNumOfFiles(getRootNode().getAbsoluteFileB());

			taskDescription = "Comparing directory content of A and B";
			monitor.subTask(taskDescription);
			getProtocol().add(taskDescription);

			GuiRefresher guiRefresher = new GuiRefresher();

			try {
				getPlugin().resetRootNode();
				createNodeTree(getRootNode());
			} catch (CancelationException e) {
				// Differentiation was aborted - remove loose clean nodes - it
				// is very likely that such nodes exist after an exception
				getRootNode().clean();
			} catch (IOException e) {
				guiRefresher.setException(e);
				getRootNode().clean();
			}
			
			taskDescription = "Filtering results";
			monitor.subTask(taskDescription);
			getProtocol().add(taskDescription);
			filter(getRootNode());
			
			monitor.done();

			// report result
			PlatformUI.getWorkbench().getDisplay().syncExec(guiRefresher);
		} catch (RuntimeException e) {
			getPlugin().logError("Exception occured during differenation process.", e);
		} finally {
			getSyncView().releaseLock();
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
	private void createNodeTree(DiffNode localNode) throws CancelationException, IOException {
		Set<String> pathBSet = new HashSet<String>();

		// copy all children of localRootRight to a HashMap
		for (File childB : localNode.listFilesB()) {
			String relativePathChild = localNode.getRelativePath() + File.separator + childB.getName();
			pathBSet.add(relativePathChild);
		}

		for (File childA : localNode.listFilesA()) {
			String relativePathChild = localNode.getRelativePath() + File.separator + childA.getName();

			if (!pathBSet.remove(relativePathChild)) {
				// only location A contains this file/folder
				localNode.createChildNode(childA, DiffStatus.COPY_TO_B);
				worked(FileOperation.totalNumOfFiles(childA));
			} else {
				// both locations contain that file/folder
				File childB = new File(localNode.getAbsoluteFileB(), childA.getName());
				compareChildren(localNode, childA, childB);
			}
		}

		for (String relativePathChild : pathBSet) {
			// only location B contains this file/folder
			File childB = new File(localNode.getRootPathB(), relativePathChild);
			localNode.createChildNode(childB, DiffStatus.REMOVE_FROM_B);
			worked(FileOperation.totalNumOfFiles(childB));
		}
	}
	
	private void compareChildren(DiffNode localNode, File childA, File childB) throws CancelationException, IOException {
		if (childA.isDirectory()) {
			// add a node with status clean and check the folders' contents
			DiffNode childNode = localNode.createChildNode(childA, DiffStatus.CLEAN);
			createNodeTree(childNode);
			// when there were no differences, no nodes should be there and we can safely remove the childNode again
			if (!childNode.hasChildren()) {
				localNode.removeChildNode(childNode);
			}
		} else {
			DiffStatus status = compareFiles(childA, childB);
			if (getSettings().shouldImplicitlySyncTimestamps() && status == DiffStatus.TOUCH) {
				if (childA.lastModified() < childB.lastModified()) {
					touchFile(childA, childB);
				} else {
					touchFile(childB, childA);
				}
				
			} else if (status != DiffStatus.CLEAN) {
				localNode.createChildNode(childA, status);
			}
			worked(2);
		}
	}
	
	private DiffStatus compareFiles(File fileA, File fileB) throws IOException {
		if (fileA.length() == fileB.length() && 
				fileA.lastModified() != fileB.lastModified() && 
				getSettings().shouldChecksumIfPotentiallyEqual())
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
		
		if (getSettings().shouldAlwaysChecksum() && !FileUtils.contentEquals(fileA, fileB))
			return DiffStatus.CONFLICT;
		
		return DiffStatus.CLEAN;
	}
	
	private void filter(DiffNode node) {
		for (int i = 0; i < node.getChildren().size(); i++) {
			DiffNode child = node.getChildren().get(i);
			if (!getPlugin().getSettings().isValid(child.getName())) {
				child.remove();
				i--;
			} else if (child.hasChildren())
				filter(child);
		}
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
	private void worked(long filesProcessed) throws CancelationException {
		if (monitor.isCanceled())
			throw new CancelationException();

		filesProcessedSoFar += filesProcessed;

		int step = (int) ((filesProcessedSoFar * WORK_MAX) / totalFilesToProcess) - worked;
		if (step == 0)
			return;

		monitor.worked(step);
		worked += step;
	}
}
