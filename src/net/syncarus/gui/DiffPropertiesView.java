package net.syncarus.gui;

import java.util.Iterator;

import net.syncarus.model.DiffNode;
import net.syncarus.rcp.SyncarusPlugin;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.part.ViewPart;

public class DiffPropertiesView extends ViewPart implements ISelectionChangedListener {
	public static final String ID = "net.syncarus.gui.DiffPropertiesView";

	private Composite container;
	private ScrolledComposite scrolledComposite;

	/**
	 * Create contents of the view part
	 * 
	 * @param parent
	 */
	@Override
	public void createPartControl(Composite parent) {
		scrolledComposite = new ScrolledComposite(parent, SWT.V_SCROLL);
		container = new Composite(scrolledComposite, SWT.NONE);
		container.setBackground(container.getDisplay().getSystemColor(SWT.COLOR_BLACK));
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		layout.verticalSpacing = 8;
		container.setLayout(layout);

		scrolledComposite.setContent(container);
		scrolledComposite.setExpandHorizontal(true);
		scrolledComposite.setExpandVertical(true);
		scrolledComposite.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				scrolledLayout();
			}
		});

		initializeToolBar();

		SyncarusPlugin.getInstance().getSyncView().getViewer().addSelectionChangedListener(this);
	}

	@Override
	public void setFocus() {
	}

	private void initializeToolBar() {
		@SuppressWarnings("unused")
		IToolBarManager toolBarManager = getViewSite().getActionBars().getToolBarManager();
	}

	// is called by the treeViewer of the SyncView
	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		// remove all existing elements from the view container
		for (Control control : container.getChildren())
			control.dispose();

		IStructuredSelection selection = (IStructuredSelection) event.getSelection();
		if (!selection.isEmpty()) {
			for (@SuppressWarnings("rawtypes")
			Iterator iter = selection.iterator(); iter.hasNext();) {
				DiffNode node = (DiffNode) iter.next();
				NodeInfoComposite comp = new NodeInfoComposite(container, node);
				comp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			}
		}

		scrolledLayout();
	}

	private void scrolledLayout() {
		Point p = container.computeSize(scrolledComposite.getSize().x - 20, SWT.DEFAULT);
		scrolledComposite.setMinSize(p);
		container.layout();
	}
}
