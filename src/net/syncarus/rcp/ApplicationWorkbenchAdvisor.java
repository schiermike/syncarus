package net.syncarus.rcp;

import java.io.File;

import net.syncarus.core.DiffControl;
import net.syncarus.core.FileFilter;
import net.syncarus.gui.SyncView;
import net.syncarus.model.SyncException;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IWorkbenchPreferenceConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.IWorkbenchConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

/**
 * This advisor controls workbench startup and defines the initial window
 * perspective.
 */
public class ApplicationWorkbenchAdvisor extends WorkbenchAdvisor {
	private static final String PERSPECTIVE_ID = "net.syncarus.rcp.Perspective";

	private static final String SETTINGS_PATHS = "path";
	private static final String SETTINGS_ROOT_A_PATH = "root_a";
	private static final String SETTINGS_ROOT_B_PATH = "root_b";

	@Override
	public WorkbenchWindowAdvisor createWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
		return new ApplicationWorkbenchWindowAdvisor(configurer);
	}

	@Override
	public String getInitialWindowPerspectiveId() {
		return PERSPECTIVE_ID;
	}

	@Override
	public void eventLoopException(Throwable exception) {
		MessageDialog.openError(null, "EventLoopException occured", "Error: " + exception.toString());
		super.eventLoopException(exception);
	}

	@Override
	public void initialize(IWorkbenchConfigurer configurer) {
		IPreferenceStore prefStore = PlatformUI.getPreferenceStore();
		prefStore.setValue(IWorkbenchPreferenceConstants.SHOW_TRADITIONAL_STYLE_TABS, true);
		prefStore.setValue(IWorkbenchPreferenceConstants.SHOW_MEMORY_MONITOR, false);
		prefStore.setValue(IWorkbenchPreferenceConstants.SHOW_MULTIPLE_EDITOR_TABS, false);
		prefStore.setValue(IWorkbenchPreferenceConstants.DISABLE_NEW_FAST_VIEW, true);
		prefStore.setValue(IWorkbenchPreferenceConstants.SHOW_PROGRESS_ON_STARTUP, false);
	}

	@Override
	public void postStartup() {
		DiffControl.fileFilter = new FileFilter(SyncarusPlugin.getInstance().getPreferenceStore());

		IDialogSettings dialogSettings = SyncarusPlugin.getInstance().getDialogSettings();
		IDialogSettings pathSection = dialogSettings.getSection(SETTINGS_PATHS);
		if (pathSection != null) {
			String rootAPath = pathSection.get(SETTINGS_ROOT_A_PATH);
			String rootBPath = pathSection.get(SETTINGS_ROOT_B_PATH);
			if (rootAPath != null && rootBPath != null)
				try {
					DiffControl.initialize(new File(rootAPath), new File(rootBPath));
				} catch (SyncException e) {
					MessageDialog.openError(null, "Location error", e.getMessage());
				}
		}

		getSyncView().update();
		super.postStartup();
	}

	private SyncView getSyncView() {
		return (SyncView) getWorkbenchConfigurer().getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.findView(SyncView.ID);
	}

	@Override
	public boolean preShutdown() {
		IDialogSettings dialogSettings = SyncarusPlugin.getInstance().getDialogSettings();
		IDialogSettings pathSection = dialogSettings.getSection(SETTINGS_PATHS);
		if (DiffControl.isInitialized()) {
			if (pathSection == null)
				pathSection = dialogSettings.addNewSection(SETTINGS_PATHS);
			pathSection.put(SETTINGS_ROOT_A_PATH, DiffControl.rootA);
			pathSection.put(SETTINGS_ROOT_B_PATH, DiffControl.rootB);
		}
		return super.preShutdown();
	}
}
