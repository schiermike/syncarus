package net.syncarus.core;

import java.io.File;

import net.syncarus.gui.WizardPageDirChoose;
import net.syncarus.rcp.ResourceRegistry;
import net.syncarus.rcp.SyncarusPlugin;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;

/**
 * This wizard is used to let the user select both locations A and B.
 */
public class DirSelectWizard extends Wizard {
	private WizardPageDirChoose rootASelectPage;
	private WizardPageDirChoose rootBSelectPage;

	private WizardDialog dialog;
	private final SyncarusPlugin plugin;

	/**
	 * adds 2 WizardPages of type <code>WizardPageDirChoose</code> to the
	 * wizard.
	 */
	@Override
	public void addPages() {
		setWindowTitle("Choose the two locations that should be synchronized");
		addPage(rootASelectPage);
		addPage(rootBSelectPage);
	}

	/**
	 * Initialises the wizard by creating two wizard-pages.
	 * @param syncarusPlugin 
	 */
	public DirSelectWizard(SyncarusPlugin plugin) {
		this.plugin = plugin;
		File rootADir = null;
		File rootBDir = null;
		if (plugin.isInitialized()) {
			rootADir = plugin.getRootNode().getAbsoluteFileA();
			rootBDir = plugin.getRootNode().getAbsoluteFileB();
		}

		ResourceRegistry rr = SyncarusPlugin.getInstance().getResourceRegistry();

		rootASelectPage = new WizardPageDirChoose(rootADir);
		rootASelectPage.setImageDescriptor(rr.getImageDescriptor(ResourceRegistry.IMAGE_WIZARD_A));
		rootASelectPage.setTitle("Select location A");

		rootBSelectPage = new WizardPageDirChoose(rootBDir);
		rootBSelectPage.setImageDescriptor(rr.getImageDescriptor(ResourceRegistry.IMAGE_WIZARD_B));
		rootBSelectPage.setTitle("Select location B");

		this.dialog = new WizardDialog(null, this);
		dialog.setPageSize(450, 300);
	}

	/**
	 * Customised order of pages in the wizard
	 */
	@Override
	public IWizardPage getNextPage(IWizardPage page) {
		if (page.equals(rootASelectPage))
			return rootBSelectPage;

		return null;
	}

	public boolean open() {
		return dialog.open() == 0;
	}

	/**
	 * Returns the dialog.
	 * 
	 * @return Returns the dialog.
	 */
	public WizardDialog getDialog() {
		return dialog;
	}

	/**
	 * only when the installation reaches the last page, finishing is allowed
	 */
	@Override
	public boolean canFinish() {
		WizardPage wizardPage = (WizardPage) this.getDialog().getCurrentPage();

		if (rootASelectPage.getDir() == null) {
			wizardPage.setErrorMessage("Please select a directory for location A.");
			return false;
		}
		if (rootBSelectPage.getDir() == null) {
			wizardPage.setErrorMessage("Please select another directory for location B.");
			return false;
		}

		if (FileOperation.isSubdirectory(rootASelectPage.getDir(), rootBSelectPage.getDir())
				|| FileOperation.isSubdirectory(rootBSelectPage.getDir(), rootASelectPage.getDir())) {
			wizardPage.setErrorMessage("The directories should not be subdirectories of each other!");
			return false;
		}

		wizardPage.setErrorMessage(null);
		return true;
	}

	/**
	 * Initialises the {@link DiffController} with the two new paths and starts the
	 * Differentiation process via running the appropriate action
	 */
	@Override
	public boolean performFinish() {
		plugin.initialize(rootASelectPage.getDir(), rootBSelectPage.getDir());
		return true;
	}
}
