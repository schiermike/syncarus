package net.syncarus.gui;

import net.syncarus.core.DiffControl;
import net.syncarus.model.DiffNode;
import net.syncarus.rcp.ResourceRegistry;
import net.syncarus.rcp.SyncarusPlugin;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.part.ViewPart;

public class DiffPropertiesView extends ViewPart implements ISelectionChangedListener {
	public static final String ID = "net.syncarus.gui.DiffPropertiesView";

	private DiffNode currentNode;
	private Composite container;
	private Label leftRootLabel;
	private Label rightRootLabel;
	
	private FileInfoGroup infoGroup1;
	private FileInfoGroup infoGroup2;
	
	/**
	 * Create contents of the view part
	 * 
	 * @param parent
	 */
	@Override
	public void createPartControl(Composite parent) {
		ResourceRegistry rr = SyncarusPlugin.getInstance().getResourceRegistry();
		Font normalFont = rr.getFont(ResourceRegistry.FONT_8);
		Font boldFont = rr.getFont(ResourceRegistry.FONT_BOLD_8);

		container = new Composite(parent, SWT.NONE);
		GridLayout gridLayout = new GridLayout();
		container.setLayout(gridLayout);

		// ------------------

		Group rootGroup = new Group(container, SWT.NONE);
		GridLayout rootGroupLayout = new GridLayout();
		rootGroupLayout.numColumns = 2;
		rootGroup.setLayout(rootGroupLayout);
		rootGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		rootGroup.setFont(normalFont);
		rootGroup.setText("Root directory paths");

		Label l = new Label(rootGroup, SWT.NONE);
		l.setFont(normalFont);
		l.setText("Left:");

		leftRootLabel = new Label(rootGroup, SWT.WRAP);
		leftRootLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		leftRootLabel.setFont(boldFont);
		leftRootLabel.setText("not set");

		l = new Label(rootGroup, SWT.NONE);
		l.setFont(normalFont);
		l.setText("Right:");

		rightRootLabel = new Label(rootGroup, SWT.WRAP);
		rightRootLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		rightRootLabel.setFont(boldFont);
		rightRootLabel.setText("not set");

		infoGroup1 = new FileInfoGroup(container);
		infoGroup1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		infoGroup2 = new FileInfoGroup(container);
		infoGroup2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		initializeToolBar();

		SyncarusPlugin.getInstance().getSyncView().getViewer().addSelectionChangedListener(this);
	}

	@Override
	public void setFocus() {}

	private void initializeToolBar() {
		@SuppressWarnings("unused")
		IToolBarManager toolBarManager = getViewSite().getActionBars().getToolBarManager();
	}

	// is called by the treeViewer of the SyncView
	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		if (DiffControl.isInitialized()) {
			leftRootLabel.setText(DiffControl.leftRootPath);
			rightRootLabel.setText(DiffControl.rightRootPath);
		}
		
		IStructuredSelection selection = (IStructuredSelection) event.getSelection();
		if (selection.size() != 1)
			return;

		currentNode = (DiffNode) selection.getFirstElement();
		// FIXME: show correct labels for the groups, inspect the currentNode state to explain what will happen
		infoGroup1.setInfo(DiffControl.toLeftFile(currentNode));
		infoGroup2.setInfo(DiffControl.toRightFile(currentNode));
		
		container.layout();
	}
}
