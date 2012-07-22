package net.syncarus.model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to build up a tree handling the detected differences
 * between source and target location. Differences are encoded using predefined
 * static int codes. The tree consists of DiffNodes which stand for single
 * folders whereas the fileMaps hold (string,int)-pairs which define the files
 * in the folder (relative-path, code).
 */
public class DiffNode implements Comparable<DiffNode> {
	private final DiffNode parent;
	private final boolean isDirectory;
	private final String relativePath;
	private final List<DiffNode> children = new ArrayList<DiffNode>();
	private DiffStatus status = DiffStatus.UNKNOWN;

	/**
	 * Constructor is used to initialise root-Nodes without parents
	 */
	public DiffNode() {
		parent = null;
		isDirectory = true;
		relativePath = File.separator;
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
	public DiffNode createChildNode(String relativePath, boolean isDirectory, DiffStatus status) {
		return new DiffNode(this, isDirectory, relativePath, status);
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

		if (this.parent != null)
			parent.addChildNode(this);
	}

	private void addChildNode(DiffNode child) {
		children.add(child);
	}

	/**
	 * @return Returns true when the source or the target file object (depending
	 *         on the state) is a directory
	 */
	public boolean isDirectory() {
		return isDirectory;
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
}
