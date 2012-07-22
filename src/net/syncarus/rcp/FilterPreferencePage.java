package net.syncarus.rcp;

import java.util.regex.Pattern;

import net.syncarus.core.FileFilter;

import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.ListEditor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * This preference page allows the user to enter wildcards for filtering
 * arbitrary files from the synchronisation process. This is convenient for
 * files, which are not relevant for the synchronisation, e.g. thumbs.db created
 * by windows explorer.
 */
public class FilterPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public FilterPreferencePage() {
		super(GRID);
	}

	@Override
	protected void createFieldEditors() {
		WildCardListEditor namesEditor = new WildCardListEditor(getFieldEditorParent());
		addField(namesEditor);
		namesEditor.setLabelText("File and directory names to ignore");
		namesEditor.setPreferenceName(FileFilter.NAMES_TO_IGNORE);
	}

	@Override
	public void init(IWorkbench workbench) {
		// nothing to initialise
	}

	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return SyncarusPlugin.getInstance().getPreferenceStore();
	}

	/**
	 * A field editor for the input of wildcards.
	 */
	private static class WildCardListEditor extends ListEditor {
		protected Composite parent;

		public WildCardListEditor(Composite parent) {
			this.parent = parent;
			doFillIntoGrid(parent, 1);
		}

		@Override
		protected String createList(String[] items) {
			return FileFilter.toPreferenceString(items);
		}

		@Override
		protected String getNewInputObject() {
			InputDialog dialog = new InputDialog(parent.getShell(), "New wildcard", "Please enter new name", "",
					new WildCardInputValidator());
			if (dialog.open() == Window.OK)
				return dialog.getValue();

			return null;
		}

		@Override
		protected String[] parseString(String stringList) {
			return FileFilter.fromPreferenceString(stringList);
		}

		/**
		 * Validates the input for a new name to ignore. Basically checks only
		 * if the given expression is a valid regular expression.
		 */
		private static class WildCardInputValidator implements IInputValidator {
			@Override
			public String isValid(String newText) {
				try {
					Pattern.compile(newText);
					return null;
				} catch (RuntimeException e) {
					return "Invalid regular expression";
				}
			}
		}
	}
}
