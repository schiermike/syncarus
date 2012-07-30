package net.syncarus.rcp.preferences;

/**
 * This preference page allows the user to enter wildcards for filtering
 * arbitrary files from the synchronisation process. This is convenient for
 * files, which are not relevant for the synchronisation, e.g. thumbs.db created
 * by windows explorer.
 */
public class FilterPreferencePage extends SyncarusPreferencePage {

	@Override
	protected void createFieldEditors() {
		addField(new RegExFieldEditor(getFieldEditorParent()));
	}
}
