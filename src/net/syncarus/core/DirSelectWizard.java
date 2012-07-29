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
 * This wizard is used to get source and target directories from the user.
 */
public class DirSelectWizard extends Wizard {
	// source path chooser
	private WizardPageDirChoose leftRootSelectPage = null;

	// target path chooser
	private WizardPageDirChoose rightRootSelectPage = null;

	private WizardDialog dialog;

	/**
	 * adds 2 WizardPages of type <code>WizardPageDirChoose</code> to the
	 * wizard.
	 */
	@Override
	public void addPages() {
		setWindowTitle("Choose synchronisation source and target folders");
		addPage(leftRootSelectPage);
		addPage(rightRootSelectPage);
	}

	/**
	 * Initialises the wizard by creating two wizard-pages for source and target
	 * directory input.<br>
	 * Also sets the size of the Dialog to 600x300.
	 */
	public DirSelectWizard() {
		File leftRootDir = null;
		File rightRootDir = null;
		if (DiffControl.isInitialized()) {
			leftRootDir = new File(DiffControl.rootA);
			rightRootDir = new File(DiffControl.rootB);
		}

		ResourceRegistry rr = SyncarusPlugin.getInstance().getResourceRegistry();

		leftRootSelectPage = new WizardPageDirChoose(leftRootDir);
		leftRootSelectPage.setImageDescriptor(rr.getImageDescriptor(ResourceRegistry.IMAGE_WIZARD_LEFT));
		leftRootSelectPage.setTitle("Choose Synchronisation Source");
		leftRootSelectPage
				.setMessage("Choose an appropriate synchronisation source were potentially new data can be found!");

		rightRootSelectPage = new WizardPageDirChoose(rightRootDir);
		rightRootSelectPage.setImageDescriptor(rr.getImageDescriptor(ResourceRegistry.IMAGE_WIZARD_RIGHT));
		rightRootSelectPage.setTitle("Choose Synchronisation Target");
		rightRootSelectPage
				.setMessage("Choose an appropriate synchronisation target were mostly backup data can be found!");

		this.dialog = new WizardDialog(null, this);
		dialog.setPageSize(450, 300);
	}

	/**
	 * Customised order of pages in the wizard
	 */
	@Override
	public IWizardPage getNextPage(IWizardPage page) {
		if (page.equals(leftRootSelectPage))
			return rightRootSelectPage;

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

		if (leftRootSelectPage.getDir() == null) {
			wizardPage.setErrorMessage("The first directory has not been chosen yet.");
			return false;
		}
		if (rightRootSelectPage.getDir() == null) {
			wizardPage.setErrorMessage("The second directory has not been chosen yet.");
			return false;
		}

		if (FileOperation.isSubdirectory(leftRootSelectPage.getDir(), rightRootSelectPage.getDir())
				|| FileOperation.isSubdirectory(rightRootSelectPage.getDir(), leftRootSelectPage.getDir())) {
			wizardPage.setErrorMessage("The directories should not be subdirectories of each other!");
			return false;
		}

		wizardPage.setErrorMessage(null);
		return true;
	}

	/**
	 * Initialises the {@link DiffControl} with the two new paths and starts the
	 * Differentiation process via running the appropriate action
	 */
	@Override
	public boolean performFinish() {
		DiffControl.initialize(leftRootSelectPage.getDir(), rightRootSelectPage.getDir());
		return true;
	}
}
