package net.syncarus.rcp;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

/**
 * This advisor configures the workbench window, sets its title and disables
 * several features
 */
public class ApplicationWorkbenchWindowAdvisor extends WorkbenchWindowAdvisor {
	private static final String settingsGUISection = "window";
	private static final String settingsGUIWidth = "width";
	private static final String settingsGUIHeight = "height";
	private static final String settingsGUIPosX = "xpos";
	private static final String settingsGUIPosY = "ypos";

	public ApplicationWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
		super(configurer);
	}

	@Override
	public ActionBarAdvisor createActionBarAdvisor(IActionBarConfigurer configurer) {
		return new ApplicationActionBarAdvisor(configurer);
	}

	@Override
	public void preWindowOpen() {
		IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
		configurer.setTitle("Syncarus");
	}

	@Override
	public void postWindowCreate() {
		IDialogSettings dialogSettings = SyncarusPlugin.getInstance().getDialogSettings();
		IDialogSettings guiSection = dialogSettings.getSection(settingsGUISection);
		int x = 50;
		int y = 50;
		int w = 1000;
		int h = 700;
		if (guiSection != null) {
			w = guiSection.getInt(settingsGUIWidth);
			h = guiSection.getInt(settingsGUIHeight);
			x = guiSection.getInt(settingsGUIPosX);
			y = guiSection.getInt(settingsGUIPosY);
		}
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell().setLocation(x, y);
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell().setSize(w, h);
	}

	@Override
	public boolean preWindowShellClose() {
		IDialogSettings dialogSettings = SyncarusPlugin.getInstance().getDialogSettings();
		IDialogSettings windowSection = dialogSettings.getSection(settingsGUISection);
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		if (windowSection == null)
			windowSection = dialogSettings.addNewSection(settingsGUISection);
		windowSection.put(settingsGUIPosX, shell.getLocation().x);
		windowSection.put(settingsGUIPosY, shell.getLocation().y);
		windowSection.put(settingsGUIWidth, shell.getSize().x);
		windowSection.put(settingsGUIHeight, shell.getSize().y);

		return super.preWindowShellClose();
	}

}
