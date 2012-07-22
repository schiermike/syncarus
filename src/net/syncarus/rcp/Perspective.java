package net.syncarus.rcp;

import net.syncarus.gui.DiffPropertiesView;
import net.syncarus.gui.LogView;
import net.syncarus.gui.SyncView;

import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

/**
 * This Perspective is the only one existing in the program. It has disabled
 * move- and close- control elements, defines one stand-alone view and has no
 * editor area.
 */
public class Perspective implements IPerspectiveFactory {
	@Override
	public void createInitialLayout(IPageLayout layout) {
		layout.setEditorAreaVisible(false);
		layout.setFixed(true);
		layout.addView(SyncView.ID, IPageLayout.LEFT, 1f, layout.getEditorArea());
		layout.addView(DiffPropertiesView.ID, IPageLayout.RIGHT, 0.5f, SyncView.ID);
		layout.addView(LogView.ID, IPageLayout.BOTTOM, 0.65f, DiffPropertiesView.ID);
	}
}
