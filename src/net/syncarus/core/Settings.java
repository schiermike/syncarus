package net.syncarus.core;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

public class Settings {
	public static final String PREFKEY_FILTER = "syncarus_filter_regexp";
	public static final String PREFKEY_IMPLICITLY_SYNC_TIMESTAMPS = "syncarus_implicitly_sync_timestamps";
	public static final String PREFKEY_CHECKSUM_IF_POTENTIALLY_EQUAL = "syncarus_checksum_if_potentially_equal";
	public static final String PREFKEY_ALWAYS_CHECKSUM = "syncarus_always_checksum";
	private static final String FILTER_SEPARATOR = " #|# ";

	private IPreferenceStore preferenceStore;
	
	private List<Pattern> namePattern = new ArrayList<Pattern>();

	public Settings(IPreferenceStore preferenceStore) {
		this.preferenceStore = preferenceStore;
		preferenceStore.addPropertyChangeListener(new IPropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent event) {
				if (event.getProperty().startsWith("syncarus_"))
					update();
			}
		});

		update();
	}

	private void update() {
		namePattern.clear();
		for (String item : filterFromPreferenceString(preferenceStore.getString(PREFKEY_FILTER)))
			namePattern.add(Pattern.compile(item));
	}

	public static String[] filterFromPreferenceString(String preferenceString) {
		List<String> items = new ArrayList<String>();

		StringTokenizer tokenizer = new StringTokenizer(preferenceString, FILTER_SEPARATOR);
		while (tokenizer.hasMoreTokens())
			items.add(tokenizer.nextToken());

		return items.toArray(new String[items.size()]);
	}

	/**
	 * The corresponding method for
	 * {@link Settings#filterFromPreferenceString(String)}. Converts the array of
	 * string into a single string.
	 * 
	 * @param items
	 *            The items.
	 * @return The string.
	 */
	public static String filterToPreferenceString(String[] items) {
		StringBuilder builder = new StringBuilder();
		for (String item : items) {
			builder.append(item);
			builder.append(FILTER_SEPARATOR);
		}

		return builder.toString();
	}

	/**
	 * Checks if the file should be ignored or not.
	 * 
	 * @param fileName
	 *            The file to process.
	 * @return <code>true</code> if the file should be respected in the
	 *         synchronisation process, <code>false</code> if not.
	 */
	public boolean isValid(String fileName) {
		for (Pattern pattern : namePattern)
			if (pattern.matcher(fileName).matches())
				return false;

		return true;
	}
	
	/**
	 * If <code>true</code>: If there are two files that seem to be equal 
	 * but that have different modification dates, the modification date 
	 * of the newer file is set to the modification date of the older file.
	 */
	public boolean shouldImplicitlySyncTimestamps() {
		String value = preferenceStore.getString(PREFKEY_IMPLICITLY_SYNC_TIMESTAMPS);
		if (value.isEmpty())
			return false;
		return Boolean.valueOf(value);
	}
	
	/**
	 * If <code>false</code>: If two files have the same modification date
	 * and the same length, it is assumed that they are equal.
	 * If <code>true</code>: Checksums over both files are calculated
	 * and compared to ensure that both files are indeed equal.
	 */
	public boolean shouldAlwaysChecksum() {
		String value = preferenceStore.getString(PREFKEY_ALWAYS_CHECKSUM);
		if (value.isEmpty())
			return false;
		return Boolean.valueOf(value);
	}
	
	/**
	 * If <code>false</code>: If two files have different modification 
	 * dates but the same length, it is assumed that they are equal.
	 * If <code>true</code>: Checksums over both files are calculated
	 * and compared to ensure that both files are indeed equal.
	 */
	public boolean shouldChecksumIfPotentiallyEqual() {
		String value = preferenceStore.getString(PREFKEY_CHECKSUM_IF_POTENTIALLY_EQUAL);
		if (value.isEmpty())
			return false;
		return Boolean.valueOf(value);
	}
}
