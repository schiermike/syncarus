package net.syncarus.model;

/**
 * Application specific exception defining several codes to better identify
 * occurring exceptions.
 */
public class SyncException extends RuntimeException {
	public static final int UNDEFINED = 0;
	public static final int PATH_EXCEPTION = 1;
	public static final int FILE_COMPARISON = 2;
	public static final int DATA_STRUCTURE_EXCEPTION = 3;
	public static final int INCONSISTENT_STATE_EXCEPTION = 4;
	public static final int THREAD_EXCEPTION = 5;
	public static final int FILE_OPERATION_EXCEPTION = 6;

	private static final long serialVersionUID = 4499679913230153044L;

	private int code = 0;

	public SyncException(int code, String message) {
		super(message);
		this.code = code;

	}

	public static String getStringForCode(int code) {
		switch (code) {
		case PATH_EXCEPTION:
			return "PATH EXCEPTIOIN";
		case FILE_COMPARISON:
			return "FILE COMPARISON";
		case DATA_STRUCTURE_EXCEPTION:
			return "DATA STRUCTURE EXCEPTION";
		case INCONSISTENT_STATE_EXCEPTION:
			return "INCONSISTENT STATE EXCEPTION";
		case THREAD_EXCEPTION:
			return "THREAD_EXCEPTION";
		case FILE_OPERATION_EXCEPTION:
			return "FILE_OPERATION_EXCEPTION";
		default:
			return "UNDEFINED";
		}
	}

	@Override
	public String toString() {
		return "SyncException occured: Exception code is " + code + " (" + SyncException.getStringForCode(code) + ")\n"
				+ super.toString();
	}
}
