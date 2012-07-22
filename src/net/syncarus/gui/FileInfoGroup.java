package net.syncarus.gui;

import java.io.File;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.Date;

import net.syncarus.rcp.ResourceRegistry;
import net.syncarus.rcp.SyncarusPlugin;

import org.apache.commons.io.FileUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

public class FileInfoGroup extends Group {

	private Label modTimeLabel;
	private Label modTimeLabel2;
	private Label sizeLabel;
	private Label sizeLabel2;
	private Label locationLabel2;
	private Label locationLabel;

	public FileInfoGroup(Composite parent) {
		super(parent, SWT.NONE);

		ResourceRegistry rr = SyncarusPlugin.getInstance().getResourceRegistry();
		Font normalFont = rr.getFont(ResourceRegistry.FONT_8);
		Font boldFont = rr.getFont(ResourceRegistry.FONT_BOLD_8);

		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		setLayout(layout);
		setFont(normalFont);

		modTimeLabel = new Label(this, SWT.NONE);
		modTimeLabel.setLayoutData(new GridData(100, SWT.DEFAULT));
		modTimeLabel.setFont(normalFont);
		modTimeLabel.setText("Last modified:");

		modTimeLabel2 = new Label(this, SWT.WRAP);
		modTimeLabel2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		modTimeLabel2.setFont(boldFont);

		sizeLabel = new Label(this, SWT.NONE);
		sizeLabel.setFont(normalFont);
		sizeLabel.setText("Size:");

		sizeLabel2 = new Label(this, SWT.WRAP);
		sizeLabel2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		sizeLabel2.setFont(boldFont);

		locationLabel = new Label(this, SWT.NONE);
		locationLabel.setFont(normalFont);
		locationLabel.setText("Location:");

		locationLabel2 = new Label(this, SWT.WRAP);
		locationLabel2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		locationLabel2.setFont(boldFont);
	}

	public void setInfo(File file) {
		if (!file.exists()) {
			setVisible(false);
			return;
		}
		
		GridData sizeLabelGridData = (GridData) sizeLabel.getLayoutData();
		GridData sizeLabelGridData2 = (GridData) sizeLabel2.getLayoutData();

		modTimeLabel2.setText(getDateString(file.lastModified()));
		if (file.isFile()) {
			sizeLabel2.setText(formatByteSize(file.length()));
			sizeLabelGridData.heightHint = -1;
			sizeLabelGridData2.heightHint = -1;
		} else {
			sizeLabelGridData.heightHint = 0;
			sizeLabelGridData2.heightHint = 0;
		}
		locationLabel2.setText(file.getAbsolutePath());
		setVisible(true);
	}

	private static String formatByteSize(long byteSize) {
		double size = byteSize; // use floating point representation
		DecimalFormat df = new DecimalFormat("0.##");

		if (byteSize > FileUtils.ONE_GB)
			return df.format(size / FileUtils.ONE_GB) + " GB";

		if (byteSize > FileUtils.ONE_MB)
			return df.format(size / FileUtils.ONE_MB) + " MB";

		if (byteSize > FileUtils.ONE_KB)
			return df.format(size / FileUtils.ONE_KB) + " KB";

		return Long.toString(byteSize) + " bytes";
	}

	/**
	 * @param time
	 * @return a formatted date string showing date, day, and time 00:00:00
	 */
	private static String getDateString(long time) {
		return DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.MEDIUM).format(new Date(time));
	}

	@Override
	protected void checkSubclass() {
		// has to be overwritten to circumvent the no-subclassing restriction
	}
}
