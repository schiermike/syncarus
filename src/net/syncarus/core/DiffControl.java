package net.syncarus.core;

import java.io.File;

import net.syncarus.model.DiffNode;
import net.syncarus.model.DiffStatus;
import net.syncarus.model.SyncException;

/**
 * This controller is responsible for providing a lock mechanism to
 * avoid having multiple jobs running at the same time.
 */
public class DiffControl {
	// '/'-separated paths
	public static String rootA = null;
	public static String rootB = null;

	private static DiffNode rootDiffNode = null;

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
		if (rootA == null || rootB == null)
			return false;
		if (!new File(rootA).exists() || !new File(rootB).exists())
			return false;
		return true;
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
		// reformat path-string using '/' as separator char
		rootA = rootADir.getAbsolutePath();
		if (rootA.endsWith(File.separator)) // remove trailing '/'
			rootA = rootA.substring(0, rootA.length() - 1);

		rootB = rootBDir.getAbsolutePath();
		if (rootB.endsWith(File.separator))
			rootB = rootB.substring(0, rootB.length() - 1);

		LOG.add("rootA='" + rootA + "', rootB='" + rootB + "'");

		resetRootDiffNode();
	}

	/**
	 * forget about old differentiation-results and provide a clean
	 * <code>rootDiffNode</code> for a new differentiation process.
	 */
	public static void resetRootDiffNode() {
		if (!isInitialized())
			throw new SyncException(SyncException.FILE_COMPARISON, "Selecting the locations failed!\n" + 
					"Location A: " + (rootA==null ? "not set" : rootA.toString()) + "\n" + 
					"Location B: " + (rootB==null ? "not set" : rootB.toString()));
		rootDiffNode = new DiffNode();
	}

	/**
	 * converts the absolute path of a file to a relative path using the paths
	 * of rootA and rootB as a base. <br>
	 * The relative path must either be empty or be of the form "/bla/ble". "/"
	 * or "bla/bli" or "/bla/lib/blu/" are not allowed
	 * 
	 * @param absolutePath
	 *            a file/folder being a sub-element of root path A or B
	 * @throws SyncException
	 *             when <code>file</code> isn't a subfile/sub-folder of either 
	 *             root path A or B
	 * @return the relative path of the <code>file</code>
	 */
	public static String getRelativePath(String absolutePath) {
		String relativePath;
		if (FileOperation.isSubdirectory(rootA, absolutePath)) {
			relativePath = absolutePath.substring(rootA.length());
		} else if (FileOperation.isSubdirectory(rootB, absolutePath)) {
			relativePath = absolutePath.substring(rootB.length());
		} else
			throw new SyncException(SyncException.PATH_EXCEPTION, "Error when trying to associate the path '"
					+ absolutePath + "' to one synchronization side!");

		// now, the path has to have a '/' followed by a path or nothing
		if (!relativePath.startsWith(File.separator))
			throw new SyncException(SyncException.PATH_EXCEPTION, "Strange path format detected: '" + relativePath
					+ "'!");

		return relativePath;
	}

	public static String getRelativePath(File file) {
		return getRelativePath(file.getAbsolutePath());
	}

	/**
	 * @return the current rootDiffNode or null, when {@link DiffControl} hasn't
	 *         been initialised.
	 */
	public static DiffNode getRootDiffNode() {
		return rootDiffNode;
	}

	public static File toFileA(DiffNode node) {
		return toFileA(node.getRelativePath());
	}

	public static File toFileA(String relativePath) {
		return new File(rootA + relativePath);
	}

	public static File toFileB(DiffNode node) {
		return toFileB(node.getRelativePath());
	}

	public static File toFileB(String relativePath) {
		return new File(rootB + relativePath);
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

	public static File newerFile(DiffNode node) {
		return toFileA(node).lastModified() < toFileB(node).lastModified() ? toFileB(node) : toFileA(node);
	}

	public static File olderFile(DiffNode node) {
		return toFileA(node).lastModified() > toFileB(node).lastModified() ? toFileB(node) : toFileA(node);
	}
}
