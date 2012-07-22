package net.syncarus.core;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

public class FileFilter {
	public static final String NAMES_TO_IGNORE = "names_to_ignore";
	public static final String SEPARATOR = "|";

	private List<Pattern> namePattern = new ArrayList<Pattern>();
	private IPropertyChangeListener listener;
	private IPreferenceStore preferenceStore;

	public FileFilter(IPreferenceStore preferenceStore) {
		this.preferenceStore = preferenceStore;
		listener = new PropertyChangeListener();
		preferenceStore.addPropertyChangeListener(listener);

		updateNamePattern();
	}

	private void updateNamePattern() {
		namePattern.clear();
		for (String item : fromPreferenceString(preferenceStore.getString(NAMES_TO_IGNORE)))
			namePattern.add(Pattern.compile(item));
	}

	public static String[] fromPreferenceString(String preferenceString) {
		List<String> items = new ArrayList<String>();

		StringTokenizer tokenizer = new StringTokenizer(preferenceString, SEPARATOR);
		while (tokenizer.hasMoreTokens())
			items.add(tokenizer.nextToken());

		return items.toArray(new String[items.size()]);
	}

	/**
	 * The corresponding method for
	 * {@link FileFilter#fromPreferenceString(String)}. Converts the array of
	 * string into a single string.
	 * 
	 * @param items
	 *            The items.
	 * @return The string.
	 */
	public static String toPreferenceString(String[] items) {
		StringBuilder builder = new StringBuilder();
		for (String item : items) {
			builder.append(item);
			builder.append(SEPARATOR);
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
	 * Listens to changes in to properties and loads the patterns if necessary.
	 */
	private class PropertyChangeListener implements IPropertyChangeListener {
		@Override
		public void propertyChange(PropertyChangeEvent event) {
			if (event.getProperty().equals(NAMES_TO_IGNORE))
				updateNamePattern();
		}
	}
}
