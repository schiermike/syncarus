package net.syncarus.core;

import java.io.File;

import net.syncarus.model.DiffNode;
import net.syncarus.model.DiffStatus;
import net.syncarus.model.SyncException;

/**
 * This controller is responsible for providing a lock mechanism to
 * avoid having multiple jobs running at the same time.
 */
public class DiffController {
	private static DiffNode rootDiffNode;

	private static boolean actionLock = false;
	public static FileFilter fileFilter;
	public static boolean syncTimestamps = false;
	public static boolean syncTimestampsWithoutChecksum = false;
	public static final Log LOG = new Log();

	/**
	 * acquire the UI-Lock needed for long-running jobs - there is only one of
	 * these jobs allowed at the same time
	 * 
	 * @return <code>true</code> on success, else <code>false</code> when
	 *         another job holds the lock
	 */
	public static synchronized boolean aquireLock() {
		if (actionLock)
			return false;
		actionLock = true;
		return true;
	}

	/**
	 * release an acquired lock of a job
	 * 
	 * @see #aquireLock()
	 * @throws SyncException
	 *             when the lock has already been released
	 */
	public static synchronized void releaseLock() {
		if (!actionLock)
			throw new SyncException(SyncException.THREAD_EXCEPTION,
					"Couldn't release lock because it has already been released before!");
		actionLock = false;
	}

	/**
	 * Checks whether the root directories have been set (via
	 * <code>initialise</code>) and exist
	 * 
	 * @return true on success, else false
	 */
	public static boolean isInitialized() {
		return rootDiffNode != null;
	}

	/**
	 * Initialises the Controller by converting paths locations A and B to a
	 * unique form which only has '/' as folder-separators. Also checks validity
	 * of locations
	 * 
	 * @param rootADir
	 * @param rootBDir
	 */
	public static void initialize(File rootADir, File rootBDir) {
		rootDiffNode = new DiffNode(rootADir, rootBDir);
		LOG.add("rootA='" + rootDiffNode.getAbsolutePathA() + "', rootB='" + rootDiffNode.getAbsolutePathB() + "'");
	}

	/**
	 * forget about old differentiation-results and provide a clean
	 * <code>rootDiffNode</code> for a new differentiation process.
	 */
	public static void resetRootDiffNode() {
		rootDiffNode = new DiffNode(rootDiffNode.getAbsoluteFileA(), rootDiffNode.getAbsoluteFileB());
	}

	/**
	 * @return the current rootDiffNode or null, when {@link DiffController} hasn't
	 *         been initialised.
	 */
	public static DiffNode getRootDiffNode() {
		return rootDiffNode;
	}

	/**
	 * clean up the diffTree (if a rootDiffNode exists)<br>
	 * This means to remove all nodes having status CLEAN and no children.<br>
	 * This especially becomes necessary after a cancel-action of a running
	 * {@link DiffTask}
	 */
	public static void cleanupDiffTree() {
		if (rootDiffNode != null)
			recursivelyCleanupDiffTree(rootDiffNode);
	}

	public static File getDefaultDirectory() {
		if (File.listRoots().length == 0)
			throw new SyncException(SyncException.FILE_OPERATION_EXCEPTION, "No filesystem roots are available!");
		if (File.listRoots().length >= 2)
			return (File.listRoots()[1]); // don't select floppy drive, this is
											// usually the first one

		return File.listRoots()[0];
	}

	/**
	 * @see #cleanupDiffTree()
	 * @return true, when this node was deleted, else false
	 */
	private static boolean recursivelyCleanupDiffTree(DiffNode node) {
		// important: childNode.selfDestruction() also removes itself from list
		// -> index has to decremented
		if (node.hasChildren())
			for (int i = 0; i < node.getChildren().size(); i++)
				if (recursivelyCleanupDiffTree(node.getChildren().get(i)))
					i--;

		// if node has no children (this may happen after recursion step) and it
		// is clean, remove it
		if (!node.hasChildren() && node.getStatus() == DiffStatus.CLEAN) {
			node.remove();
			return true;
		}
		return false;
	}
}
