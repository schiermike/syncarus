package net.syncarus.model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to build up a tree handling the detected differences
 * between both locations. Differences are encoded using predefined static int
 * codes. The tree consists of DiffNodes which stand for single folders whereas
 * the fileMaps hold (string,int)-pairs which define the files in the folder
 * (relative-path, code).
 */
public class DiffNode implements Comparable<DiffNode> {
	private final DiffNode parent;
	private final boolean isDirectory;
	private final boolean fileVsFolderConflict;
	private final String relativePath;
	private final List<DiffNode> children = new ArrayList<DiffNode>();
	private DiffStatus status = DiffStatus.UNKNOWN;

	// is determined when required
	private DiffNode root;
	private String absolutePathA;
	private String absolutePathB;

	/**
	 * Constructor is used to initialise root-Nodes without parents
	 */
	public DiffNode(String rootPathA, String rootPathB) {
		this(new File(rootPathA), new File(rootPathB));
	}

	public DiffNode(File rootPathA, File rootPathB) {
		parent = null;
		isDirectory = true;
		fileVsFolderConflict = false;
		relativePath = File.separator;

		if (!rootPathA.isDirectory() || !rootPathA.canWrite())
			throw new SyncException(SyncException.PATH_EXCEPTION, "Location A does not exist or is not writable.");

		if (!rootPathB.isDirectory() || !rootPathB.canWrite())
			throw new SyncException(SyncException.PATH_EXCEPTION, "Location B does not exist or is not writable.");

		absolutePathA = rootPathA.getAbsolutePath();
		if (absolutePathA.endsWith(File.separator)) // remove trailing '/'
			absolutePathA = absolutePathA.substring(0, absolutePathA.length() - 1);

		absolutePathB = rootPathB.getAbsolutePath();
		if (absolutePathB.endsWith(File.separator))
			absolutePathB = absolutePathB.substring(0, absolutePathB.length() - 1);

	}

	/**
	 * @param relativePath
	 *            relative path of this node
	 * @param parent
	 *            the parent node; nodes in this tree are connected
	 *            bi-directional
	 * @param status
	 *            status code belonging to this node
	 */
	public DiffNode createChildNode(File child, DiffStatus status) {
		String relativePath;
		if (child.getAbsolutePath().startsWith(getRootPathA()))
			relativePath = child.getAbsolutePath().substring(getRootPathA().length());
		else if (child.getAbsolutePath().startsWith(getRootPathB()))
			relativePath = child.getAbsolutePath().substring(getRootPathB().length());
		else
			throw new IllegalArgumentException("Cannot create child " + child.getAbsolutePath() + " of node " + this);
			
		return new DiffNode(this, child.isDirectory(), relativePath, status);
	}

	/**
	 * Constructor is used to initialise child-Nodes having parents for private
	 * use only
	 */
	private DiffNode(DiffNode parent, boolean isDirectory, String relativePath, DiffStatus status) {
		this.parent = parent;
		this.isDirectory = isDirectory;
		this.relativePath = relativePath;
		this.status = status;
		this.fileVsFolderConflict = status == DiffStatus.CONFLICT_FILEFOLDER;

		if (this.parent != null)
			parent.addChildNode(this);
	}

	private void addChildNode(DiffNode child) {
		children.add(child);
	}

	/**
	 * @return Returns true if either file object A or B (depending on the
	 *         state) is a directory
	 */
	public boolean isDirectory() {
		if (!fileVsFolderConflict)
			return isDirectory;
		switch (status) {
			case REPLACE_A:
			case CONFLICT_FILEFOLDER:
				return getAbsoluteFileA().isDirectory();
			case REPLACE_B:
				return getAbsoluteFileB().isDirectory();
			default:
				throw new SyncException(SyncException.INCONSISTENT_STATE_EXCEPTION,
						"Cannot determin the file-object type of the conflict DiffNode");
		}
	}

	/**
	 * remove a child from the childList
	 * 
	 * @param child
	 */
	public void removeChildNode(DiffNode child) {
		if (children == null)
			throw new SyncException(SyncException.DATA_STRUCTURE_EXCEPTION,
					"Cannot remove a child when there's no list to remove it from!");

		if (!children.remove(child))
			throw new SyncException(SyncException.DATA_STRUCTURE_EXCEPTION, "Child removal failed because child '"
					+ child.relativePath + "' couldn't be found in list!");
	}

	/**
	 * Calls {@link #removeChildNode(DiffNode)} on this node's parent.
	 */
	public void remove() {
		parent.removeChildNode(this);
	}

	/**
	 * @return status of this DiffNode
	 */
	public DiffStatus getStatus() {
		return status;
	}

	/**
	 * @return the number of children of this DiffNode
	 */
	public long countChildren() {
		if (!hasChildren())
			return 0;

		long sum = 0;
		for (DiffNode subNode : children)
			sum = sum + subNode.countChildren() + 1;
		return sum;
	}

	/**
	 * @return all subNodes of this node - at least an empty set
	 */
	public List<DiffNode> getChildren() {
		if (children == null)
			return new ArrayList<DiffNode>();

		return children;
	}

	/**
	 * @return the parent node of this node or null if there is no such parent
	 */
	public DiffNode getParent() {
		return parent;
	}
	
	/**
	 * Remove this node and/or all child nodes that have the status {@link DiffStatus#CLEAN}.
	 * @return true, when this node was deleted, else false
	 */
	public boolean clean() {
		// important: childNode.selfDestruction() also removes itself from list
		// -> index has to decremented
		if (hasChildren())
			for (int i = 0; i < getChildren().size(); i++)
				if (getChildren().get(i).clean())
					i--;

		// if node has no children (this may happen after recursion step) and it
		// is clean, remove it
		if (!hasChildren() && getStatus() == DiffStatus.CLEAN) {
			remove();
			return true;
		}
		return false;
	}

	@Override
	public String toString() {
		String temp = "DiffNode: <";
		if (isDirectory)
			temp += "DIR";
		else
			temp += "FILE";
		temp += "> " + relativePath + " " + status;
		return temp;
	}

	/**
	 * @return true if this node has children, else false
	 */
	public boolean hasChildren() {
		if (children == null)
			return false;
		return children.size() != 0;
	}

	/**
	 * @return the relative path of this node in the diffTree
	 */
	public String getRelativePath() {
		return relativePath;
	}

	/**
	 * @return the String after the last occurrence of '/' in the relative path
	 */
	public String getName() {
		int lastOcc = relativePath.lastIndexOf(File.separator);
		if (lastOcc >= relativePath.length() - 1)
			return "";

		return relativePath.substring(lastOcc + 1);
	}

	public void setStatus(DiffStatus status) {
		this.status = status;
	}

	/**
	 * compares the relative paths using a case-insensitive string comparison
	 * 
	 * @param arg
	 * @return Returns the result of the comparison.
	 */
	@Override
	public int compareTo(DiffNode other) {
		return relativePath.compareToIgnoreCase(other.getRelativePath());
	}

	private DiffNode getRoot() {
		if (root == null) {
			DiffNode node = this;
			while (node.parent != null)
				node = node.parent;
			root = node;
		}
		return root;
	}
	
	public String getRootPathA() {
		if (getRoot().getAbsolutePathA() == null)
			throw new IllegalStateException("The absolute path A of the root node is not set.");
		return getRoot().getAbsolutePathA();
	}
	
	public String getRootPathB() {
		if (getRoot().getAbsolutePathB() == null)
			throw new IllegalStateException("The absolute path B of the root node is not set.");
		return getRoot().getAbsolutePathB();
	}

	public String getAbsolutePathA() {
		if (absolutePathA == null)
			absolutePathA = getRootPathA() + relativePath;
		return absolutePathA;
	}

	public String getAbsolutePathB() {
		if (absolutePathB == null)
			absolutePathB = getRootPathB() + relativePath;
		return absolutePathB;
	}

	public File getAbsoluteFileA() {
		return new File(getAbsolutePathA());
	}

	public File getAbsoluteFileB() {
		return new File(getAbsolutePathB());
	}

	public File getNewerFile() {
		File a = getAbsoluteFileA();
		File b = getAbsoluteFileB();
		return a.lastModified() > b.lastModified() ? a : b;
	}
	
	public File getOlderFile() {
		File a = getAbsoluteFileA();
		File b = getAbsoluteFileB();
		return a.lastModified() < b.lastModified() ? a : b;
	}

	public File[] listFilesA() {
		return getAbsoluteFileA().listFiles();
	}
	
	public File[] listFilesB() {
		return getAbsoluteFileB().listFiles();
	}
}