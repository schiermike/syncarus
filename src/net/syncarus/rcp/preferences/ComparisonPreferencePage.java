package net.syncarus.rcp.preferences;

import net.syncarus.core.Settings;

import org.eclipse.jface.preference.BooleanFieldEditor;

public class ComparisonPreferencePage extends SyncarusPreferencePage {

	private BooleanFieldEditor syncTimestampsEditor;
	private BooleanFieldEditor alwaysChecksumEditor;
	private BooleanFieldEditor checksumIfPotentiallyEqualEditor;

	@Override
	protected void createFieldEditors() {
		syncTimestampsEditor = new BooleanFieldEditor(Settings.PREFKEY_IMPLICITLY_SYNC_TIMESTAMPS, 
				"Implicitly synchronize file modification dates when their content is equal.\nThis will set the timestamp of files in location A equal to the timestamp of files in location B if their content is equal.", getFieldEditorParent());
		
		checksumIfPotentiallyEqualEditor = new BooleanFieldEditor(Settings.PREFKEY_CHECKSUM_IF_POTENTIALLY_EQUAL, 
				"For timestamp synchronization, should it be assumed that files of equal size have the same content?\nThis might significantly speed up the timestamp synchronization process as no checksums have to be calculated.\nHowever, changes of files of equal size can no longer be detected.", 
				getFieldEditorParent());
		
		alwaysChecksumEditor = new BooleanFieldEditor(Settings.PREFKEY_ALWAYS_CHECKSUM, "Always compare file " +
				"contents. This might significantly slow down the comparison process", getFieldEditorParent());
		
		addField(syncTimestampsEditor);
		addField(checksumIfPotentiallyEqualEditor);
		addField(alwaysChecksumEditor);
	}
}
