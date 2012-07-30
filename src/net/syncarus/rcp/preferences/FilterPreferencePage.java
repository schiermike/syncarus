package net.syncarus.rcp.preferences;

import net.syncarus.rcp.SyncarusPlugin;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
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
		addField(new RegExFieldEditor(getFieldEditorParent()));
	}

	@Override
	public void init(IWorkbench workbench) {
		// nothing to initialise
	}

	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return SyncarusPlugin.getInstance().getPreferenceStore();
	}
}
