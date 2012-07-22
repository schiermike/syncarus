package net.syncarus.gui;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * WizardPage which controls a <code>WizardCompositeDirChoose</code>.
 */
public class WizardPageDirChoose extends WizardPage {
	private File directory = null;

	private DirSelectComposite dirChooseComposite;
	private Text locationText;
	private Label freeMemLabel;

	public WizardPageDirChoose(File initialDirectory) {
		super("WizardPageDirChoose");
		this.directory = initialDirectory;
		this.setPageComplete(false);
	}

	/**
	 * Initialises the WizardPage.
	 */
	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));

		dirChooseComposite = new DirSelectComposite(composite, SWT.NONE);
		dirChooseComposite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true, 2, 1));
		dirChooseComposite.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setDirectory(dirChooseComposite.getCurrentDirectory());
			}
		});

		Label locationLabel = new Label(composite, SWT.NONE);
		locationLabel.setText("Location:");
		locationText = new Text(composite, SWT.BORDER);
		locationText.setEditable(false);
		locationText.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));

		new Label(composite, SWT.NONE); // placeholder
		freeMemLabel = new Label(composite, SWT.NONE);
		freeMemLabel.setAlignment(SWT.RIGHT);
		freeMemLabel.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));

		setControl(composite);

		if (directory != null)
			setDirectory(directory);
	}

	/**
	 * method is only called by a SelectionListener on the treeViewer to update
	 * status and error messages on the wizard page
	 * 
	 * @param directory
	 */
	private void setDirectory(File directory) {
		if (!dirChooseComposite.getCurrentDirectory().equals(directory))
			dirChooseComposite.setCurrentDirectory(directory);

		locationText.setText(directory.getAbsolutePath());
		long free = directory.getFreeSpace();
		String sizeText = free == 0 ? "" : FileUtils.byteCountToDisplaySize(free);
		freeMemLabel.setText(sizeText + " free space");

		if (!directory.exists()) {
			setErrorMessage("This location does not exist!");
			setPageComplete(false);
		} else if (!directory.canWrite()) {
			setErrorMessage("You need write-priviledges for this location!");
			setPageComplete(false);
		} else {
			this.directory = directory;

			setErrorMessage(null);
			setPageComplete(true);
		}
	}

	public File getDir() {
		return directory;
	}
}
