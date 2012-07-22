package net.syncarus.model;

/**
 * This enumeration defines all possible states known to a <code>DiffNode</code>
 * .
 */
public enum DiffStatus {
	/** initial state */
	UNKNOWN,

	/** source file/folder will be copied to target location */
	MOVE_TO_RIGHT_SIDE,

	/** target file/folder will be copied to source location */
	MOVE_TO_LEFT,

	/** source file will overwrite older file in target location */
	OVERWRITE_RIGHT,

	/** target file will overwrite older file in source location */
	OVERWRITE_LEFT,

	/** source file/folder will be removed in source location */
	REMOVE_LEFT,

	/** source file/folder will be removed in target location */
	REMOVE_RIGHT,

	/**
	 * source and target files have different change dates but same size -
	 * touching the new file with the old date should help here
	 */
	TOUCH,

	/** folder is clean - no differences below */
	CLEAN,

	/**
	 * some conflict occurred - example: same file-modification date but
	 * different size
	 */
	CONFLICT;

	/**
	 * @return the inverted status<br>
	 *         example1: the opposite of "copy from source to target" is
	 *         "delete source"<br>
	 *         example2: the opposite of "overwrite source" is
	 *         "overwrite target"
	 */
	public DiffStatus getInvertedDiffStatus() {
		switch (this) {
		case MOVE_TO_LEFT:
			return REMOVE_RIGHT;
		case REMOVE_LEFT:
			return MOVE_TO_RIGHT_SIDE;

		case MOVE_TO_RIGHT_SIDE:
			return REMOVE_LEFT;
		case REMOVE_RIGHT:
			return MOVE_TO_LEFT;

		case OVERWRITE_LEFT:
			return OVERWRITE_RIGHT;
		case OVERWRITE_RIGHT:
			return OVERWRITE_LEFT;

		case TOUCH:
			return TOUCH;

		case CONFLICT:
			return OVERWRITE_RIGHT;

		case CLEAN:
			return CLEAN;

		default:
			throw new SyncException(SyncException.INCONSISTENT_STATE_EXCEPTION, "Can't invert that status(" + this
					+ ")!");
		}
	}
}
