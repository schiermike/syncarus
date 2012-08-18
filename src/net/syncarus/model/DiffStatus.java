package net.syncarus.model;

/**
 * This enumeration defines all possible states known to a <code>DiffNode</code>
 * .
 */
public enum DiffStatus {
	/** initial state */
	UNKNOWN,

	/** file/folder at location A will be copied to location B */
	COPY_TO_B,

	/** file/folder at location B will be copied to location A */
	COPY_TO_A,

	/** file at location A will overwrite older file at location B */
	REPLACE_B,

	/** file at location B will overwrite older file at location A */
	REPLACE_A,

	/** file/folder at location A will be removed */
	REMOVE_FROM_A,

	/** file/folder at location B will be removed */
	REMOVE_FROM_B,

	/**
	 * both files at locations A and B seem to be identical but have
	 * different modification dates; the modification date of the newer
	 * file will be set back to that of the older file
	 */
	TOUCH,

	/** folder is clean - no differences below */
	CLEAN,

	/**
	 * some conflict occurred - example: same file-modification date but
	 * different size
	 */
	CONFLICT_TIME,
	
	/**
	 * in both locations, an element exists with the same name, but one is a file and the other one a folder
	 */
	CONFLICT_FILEFOLDER;

	/**
	 * @return the inverted status<br>
	 *         example1: the opposite of "copy from A to B" is
	 *         "delete A"<br>
	 *         example2: the opposite of "overwrite A" is
	 *         "overwrite B"
	 */
	public DiffStatus getInvertedDiffStatus() {
		switch (this) {
		case COPY_TO_A:
			return REMOVE_FROM_B;
		case REMOVE_FROM_A:
			return COPY_TO_B;

		case COPY_TO_B:
			return REMOVE_FROM_A;
		case REMOVE_FROM_B:
			return COPY_TO_A;

		case REPLACE_A:
			return REPLACE_B;
		case REPLACE_B:
			return REPLACE_A;

		case TOUCH:
			return TOUCH;

		case CONFLICT_TIME:
			return REPLACE_B;
			
		case CONFLICT_FILEFOLDER:
			return REPLACE_B;

		case CLEAN:
			return CLEAN;

		default:
			throw new SyncException(SyncException.INCONSISTENT_STATE_EXCEPTION, "Can't invert that status(" + this
					+ ")!");
		}
	}
}
