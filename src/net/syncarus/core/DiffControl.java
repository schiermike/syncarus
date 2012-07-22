package net.syncarus.core;

import java.io.File;

import net.syncarus.model.DiffNode;
import net.syncarus.model.DiffStatus;
import net.syncarus.model.SyncException;

/**
 * It stores the <code>rootDiffNode</code> and source and target directories.<br>
 * This controller is furthermore responsible for providing a lock mechanism to
 * avoid having multiple jobs running at the same time.
 */
public class DiffControl {
	// '/'-separated paths
	public static String leftRootPath = null;
	public static String rightRootPath = null;

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
	 * Checks whether source and target directories have been set (via
	 * <code>initialise</code>) and exist
	 * 
	 * @return true on success, else false
	 */
	public static boolean isInitialized() {
		if (leftRootPath == null || rightRootPath == null)
			return false;
		if (!new File(leftRootPath).exists() || !new File(rightRootPath).exists())
			return false;
		return true;
	}

	/**
	 * Initialises the Controller by converting source and target paths to a
	 * unique form which only has '/' as folder-separators. Also checks validity
	 * of locations
	 * 
	 * @param leftRootDir
	 * @param rightRootDir
	 */
	public static void initialize(File leftRootDir, File rightRootDir) {
		// reformat path-string using '/' as separator char
		leftRootPath = leftRootDir.getAbsolutePath();
		if (leftRootPath.endsWith(File.separator)) // remove trailing '/'
			leftRootPath = leftRootPath.substring(0, leftRootPath.length() - 1);

		rightRootPath = rightRootDir.getAbsolutePath();
		if (rightRootPath.endsWith(File.separator))
			rightRootPath = rightRootPath.substring(0, rightRootPath.length() - 1);

		LOG.add("left-root='" + leftRootPath + "', right-root='" + rightRootPath + "'");

		resetRootDiffNode();
	}

	/**
	 * forget about old differentiation-results and provide a clean
	 * <code>rootDiffNode</code> for a new differentiation process.
	 */
	public static void resetRootDiffNode() {
		if (!isInitialized())
			throw new SyncException(SyncException.FILE_COMPARISON, "The synchronization paths are not properly set!");
		rootDiffNode = new DiffNode();
	}

	/**
	 * converts the absolute path of a file to a relative path using the paths
	 * of sourceFolder and targetFolder as a base. <br>
	 * The relative path must either be empty or be of the form "/bla/ble". "/"
	 * or "bla/bli" or "/bla/lib/blu/" are not allowed
	 * 
	 * @param absolutePath
	 *            a file/folder being a sub-element of source or target path
	 * @throws SyncException
	 *             when <code>file</code> isn't a subfile/sub-folder of source
	 *             or target path
	 * @return the relative path of the <code>file</code>
	 */
	public static String getRelativePath(String absolutePath) {
		String relativePath;
		// detect whether the file is from the target or the source location and
		// remove the leading path
		if (FileOperation.isSubdirectory(leftRootPath, absolutePath)) {
			relativePath = absolutePath.substring(leftRootPath.length());
		} else if (FileOperation.isSubdirectory(rightRootPath, absolutePath)) {
			relativePath = absolutePath.substring(rightRootPath.length());
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

	/**
	 * @return a file object to the absolute source
	 */
	public static File toLeftFile(DiffNode node) {
		return toLeftFile(node.getRelativePath());
	}

	public static File toLeftFile(String relativePath) {
		return new File(leftRootPath + relativePath);
	}

	/**
	 * @return a file object to the absolute target
	 */
	public static File toRightFile(DiffNode node) {
		return toRightFile(node.getRelativePath());
	}

	public static File toRightFile(String relativePath) {
		return new File(rightRootPath + relativePath);
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
