package net.syncarus.gui;

import java.io.File;

import net.syncarus.core.DiffControl;
import net.syncarus.model.DiffNode;
import net.syncarus.rcp.ResourceRegistry;
import net.syncarus.rcp.SyncarusPlugin;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class NodeInfoComposite extends Composite {
	private ResourceRegistry rr = SyncarusPlugin.getInstance().getResourceRegistry();

	public NodeInfoComposite(Composite parent, DiffNode node) {
		super(parent, SWT.NONE);
		setBackground(getDisplay().getSystemColor(SWT.COLOR_WHITE));
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		setLayout(layout);
		
		Color green = rr.getColor(ResourceRegistry.COLOR_GREEN);
		Color red = rr.getColor(ResourceRegistry.COLOR_RED);
		Color orange = rr.getColor(ResourceRegistry.COLOR_ORANGE);
		
		switch (node.getStatus()) {
		case COPY_TO_A:
			addFileInfo(DiffControl.toFileB(node), green);
			addLabel("will be copied to side A.");
			break;
		case COPY_TO_B:
			addFileInfo(DiffControl.toFileA(node), green);
			addLabel("will be copied to side B.");
			break;
		case REMOVE_FROM_A:
			addFileInfo(DiffControl.toFileA(node), red);
			addLabel("will be removed.");
			break;
		case REMOVE_FROM_B:
			addFileInfo(DiffControl.toFileB(node), red);
			addLabel("will be removed.");
			break;
		case REPLACE_A:
			addFileInfo(DiffControl.toFileA(node), red);
			addLabel("will be replaced by");
			addFileInfo(DiffControl.toFileB(node), green);
			break;
		case REPLACE_B:
			addFileInfo(DiffControl.toFileB(node), red);
			addLabel("will be replaced by");
			addFileInfo(DiffControl.toFileA(node), green);
			break;
		case CONFLICT:
			addFileInfo(DiffControl.toFileA(node), orange);
			addLabel("Cannot determine which file is newer!");
			addFileInfo(DiffControl.toFileB(node), orange);
			break;
		case TOUCH:
			addFileInfo(DiffControl.newerFile(node), red);
			addLabel("will have the same modification date as");
			addFileInfo(DiffControl.olderFile(node), green);
			break;
		default:
			throw new IllegalArgumentException("Cannot display DiffStatus " + node.getStatus() + " in DiffPropertiesView");
		}
	}
	
	private void addLabel(String text) {
		Label label = new Label(this, SWT.NONE);
		label.setText(text);
		label.setAlignment(SWT.CENTER);
		label.setFont(rr.getFont(ResourceRegistry.FONT_BOLD_8));
		GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gridData.minimumWidth = 300;
		label.setLayoutData(gridData);
		label.setBackground(getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
	}
	
	private void addFileInfo(File file, Color bgColor) {
		FileInfoGroup group = new FileInfoGroup(this, file);
		group.setBackground(bgColor);
		group.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
	}
	
}