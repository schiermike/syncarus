package net.syncarus.gui;

import net.syncarus.action.tree.CheckAllAction;
import net.syncarus.action.tree.CollapseAction;
import net.syncarus.action.tree.ExpandAction;
import net.syncarus.core.Protocol;
import net.syncarus.model.SyncException;
import net.syncarus.rcp.SyncarusPlugin;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

/**
 * Main view of the application. Consists of a central DifferenceTree, its
 * context-menu and toolbar-actions. <br>
 */
public class SyncView extends ViewPart {
	private SyncTreeViewer viewer;
	private Action unCheckAllNodesAction;
	private Action collapseTreeAction;
	private Action expandTreeAction;
	private boolean actionLock = false;

	public static final String ID = "net.syncarus.gui.SyncView";

	/**
	 * Create contents of the view part
	 * 
	 * @param parent
	 */
	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(new FillLayout());

		viewer = new SyncTreeViewer(parent);
		viewer.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		createActions();
		initializeToolBar();
	}

	/**
	 * Create the actions
	 */
	private void createActions() {
		expandTreeAction = new ExpandAction();
		collapseTreeAction = new CollapseAction();
		unCheckAllNodesAction = new CheckAllAction();
	}

	/**
	 * Initialise the toolbar by adding the created actions
	 */
	private void initializeToolBar() {
		IToolBarManager toolbarManager = getViewSite().getActionBars().getToolBarManager();
		toolbarManager.add(collapseTreeAction);
		toolbarManager.add(expandTreeAction);
		toolbarManager.add(unCheckAllNodesAction);
	}

	@Override
	public void setFocus() {
		viewer.getControl().setFocus();
	}

	/**
	 * @return the {@link SyncTreeViewer}
	 */
	public SyncTreeViewer getViewer() {
		return viewer;
	}
	
	public SyncarusPlugin getPlugin() {
		return SyncarusPlugin.getInstance();
	}
	
	public Protocol getProtocol() {
		return getPlugin().getProtocol();
	}

	public void update() {
		viewer.update(getPlugin().getRootNode());
	}
	
	/**
	 * acquire the UI-Lock needed for long-running jobs - there is only one of
	 * these jobs allowed at the same time
	 * 
	 * @return <code>true</code> on success, else <code>false</code> when
	 *         another job holds the lock
	 */
	public synchronized boolean aquireLock() {
		if (actionLock)
			return false;
		actionLock = true;
		return true;
	}

	/**
	 * release an acquired lock of a job
	 * 
	 * @see #aquireLock()
	 * @throws SyncException
	 *             when the lock has already been released
	 */
	public synchronized void releaseLock() {
		if (!actionLock)
			throw new SyncException(SyncException.THREAD_EXCEPTION,
					"Couldn't release lock because it has already been released before!");
		actionLock = false;
	}
}
