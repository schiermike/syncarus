package net.syncarus.rcp;

import net.syncarus.action.AboutAction;
import net.syncarus.action.CompareAction;
import net.syncarus.action.OpenWizardAction;
import net.syncarus.action.SyncAction;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.ICoolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;

public class ApplicationActionBarAdvisor extends ActionBarAdvisor {
	private IAction differentiationAction;
	private IAction openWizardAction;
	private IAction synchronizationAction;
	private IAction showPreferencesAction;
	private IAction showAboutAction;

	public ApplicationActionBarAdvisor(IActionBarConfigurer configurer) {
		super(configurer);
	}

	@Override
	protected void fillCoolBar(ICoolBarManager coolBar) {
		final ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT);
		coolBar.setLockLayout(true);
		coolBar.add(toolBarManager);

		toolBarManager.add(openWizardAction);
		toolBarManager.add(differentiationAction);
		toolBarManager.add(synchronizationAction);
		toolBarManager.add(new Separator());
		toolBarManager.add(showPreferencesAction);
		toolBarManager.add(showAboutAction);
	}

	@Override
	protected void makeActions(IWorkbenchWindow window) {
		ResourceRegistry rr = SyncarusPlugin.getInstance().getResourceRegistry();

		differentiationAction = new CompareAction();
		openWizardAction = new OpenWizardAction();
		synchronizationAction = new SyncAction();

		showPreferencesAction = ActionFactory.PREFERENCES.create(window);
		showPreferencesAction.setImageDescriptor(rr.getImageDescriptor(ResourceRegistry.IMAGE_PREFERENCES));

		showAboutAction = new AboutAction();
	}
}
