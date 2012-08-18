package net.syncarus.gui;

import net.syncarus.action.ExploreDirectoryAction;
import net.syncarus.action.tree.FlipCheckStatusAction;
import net.syncarus.action.tree.SwitchStatusAction;
import net.syncarus.model.DiffNode;
import net.syncarus.model.SyncException;
import net.syncarus.rcp.ResourceRegistry;
import net.syncarus.rcp.SyncarusPlugin;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;

public class SyncTreeViewer extends CheckboxTreeViewer {
	private Action switchStatusAction;
	private Action exploreAAction;
	private Action exploreBAction;
	private Action flipCheckStatusAction;

	/**
	 * provide the treeViewer with diffNodes
	 */
	private class TreeContentProvider extends ArrayContentProvider implements IStructuredContentProvider,
			ITreeContentProvider {
		@Override
		public Object[] getElements(Object inputElement) {
			return getChildren(inputElement);
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			DiffNode node = (DiffNode) parentElement;

			if (!node.hasChildren())
				return new Object[0];

			return node.getChildren().toArray();
		}

		@Override
		public Object getParent(Object element) {
			DiffNode node = (DiffNode) element;
			if (node.getParent() != null)
				return node.getParent();

			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			return ((DiffNode) element).hasChildren();
		}
	}

	/**
	 * provide the treeViewer with appropriate texts and labels.<br>
	 * the text is simply the name of a diffNode whereas the image depends on
	 * the state
	 * 
	 * @see #getImage(Object)
	 */
	private class TreeLabelProvider extends LabelProvider {
		@Override
		public String getText(Object element) {
			return ((DiffNode) element).getName();
		}

		@Override
		public Image getImage(Object element) {
			DiffNode node = (DiffNode) element;
			String imageKey;

			if (node.isDirectory()) {
				switch (node.getStatus()) {
				case CLEAN:
					imageKey = ResourceRegistry.IMAGE_DIR_NORMAL;
					break;
				case COPY_TO_A:
					imageKey = ResourceRegistry.IMAGE_DIR_ADD_A;
					break;
				case COPY_TO_B:
					imageKey = ResourceRegistry.IMAGE_DIR_ADD_B;
					break;
				case REMOVE_FROM_A:
					imageKey = ResourceRegistry.IMAGE_DIR_REMOVE_A;
					break;
				case REMOVE_FROM_B:
					imageKey = ResourceRegistry.IMAGE_DIR_REMOVE_B;
					break;
				default:
					throw new SyncException(SyncException.INCONSISTENT_STATE_EXCEPTION,
							"Directory has an unknown/unimplemented state(" + node.getStatus() + ")!");
				}
			} else {
				switch (node.getStatus()) {
				case CLEAN:
					throw new SyncException(SyncException.INCONSISTENT_STATE_EXCEPTION,
							"A file can't have state clean!");
				case COPY_TO_A:
					imageKey = ResourceRegistry.IMAGE_FILE_ADD_A;
					break;
				case COPY_TO_B:
					imageKey = ResourceRegistry.IMAGE_FILE_ADD_B;
					break;
				case REPLACE_A:
					imageKey = ResourceRegistry.IMAGE_FILE_REPLACE_A;
					break;
				case REPLACE_B:
					imageKey = ResourceRegistry.IMAGE_FILE_REPLACE_B;
					break;
				case REMOVE_FROM_A:
					imageKey = ResourceRegistry.IMAGE_FILE_REMOVE_A;
					break;
				case REMOVE_FROM_B:
					imageKey = ResourceRegistry.IMAGE_FILE_REMOVE_B;
					break;
				case TOUCH:
					imageKey = ResourceRegistry.IMAGE_FILE_TOUCH;
					break;
				case CONFLICT_TIME:
				case CONFLICT_FILEFOLDER:
					imageKey = ResourceRegistry.IMAGE_FILE_CONFLICT;
					break;
				default:
					throw new SyncException(SyncException.INCONSISTENT_STATE_EXCEPTION,
							"Directory has an unknown/unimplemented state(" + node.getStatus() + ")!");
				}
			}
			return SyncarusPlugin.getInstance().getResourceRegistry().getImage(imageKey);
		}
	}

	/**
	 * sorts treeViewer-content as follows:
	 * <ul>
	 * <li>isDirectory()</li>
	 * <li>node1.compareTo(node2)</li>
	 * </ul>
	 */
	private class Sorter extends ViewerSorter {
		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			DiffNode node1 = (DiffNode) e1;
			DiffNode node2 = (DiffNode) e2;
			if (node1.isDirectory() != node2.isDirectory()) {
				// directory comes first!
				if (node1.isDirectory())
					return -1;

				return 1;
			}
			// else both are files or directories
			return node1.compareTo(node2);
		}
	}

	public SyncTreeViewer(Composite parent) {
		super(parent, SWT.MULTI | SWT.BORDER);

		setLabelProvider(new TreeLabelProvider());
		setContentProvider(new TreeContentProvider());
		setSorter(new Sorter());
		this.getControl().setFont(SyncarusPlugin.getInstance().getResourceRegistry().getFont(ResourceRegistry.FONT_8));

		createActions();
		createListeners();

		initializeTreeViewerMenu();
		setInput(null);
	}

	private void createActions() {
		switchStatusAction = new SwitchStatusAction(this);
		exploreAAction = new ExploreDirectoryAction(this, true);
		exploreBAction = new ExploreDirectoryAction(this, false);
		flipCheckStatusAction = new FlipCheckStatusAction(this);
	}

	private void createListeners() {
		addCheckStateListener(new ICheckStateListener() {
			@Override
			public void checkStateChanged(final CheckStateChangedEvent e) {
				setSubtreeChecked(e.getElement(), e.getChecked());
			}
		});
	}

	/**
	 * sets a new rootNode in the tree viewer and refreshs the status bar
	 * @param rootNode 
	 */
	public void update(DiffNode rootNode) {
		setSelection(new StructuredSelection());
		setInput(rootNode);
	}

	/**
	 * add a <code>MenuListener</code> to create dynamic context-menus depending
	 * on the selection
	 */
	private void initializeTreeViewerMenu() {
		MenuManager menuManager = new MenuManager();
		Menu menu = menuManager.createContextMenu(getControl());
		getControl().setMenu(menu);

		// before opening menu, remove all old entries
		menuManager.setRemoveAllWhenShown(true);

		menuManager.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(IMenuManager manager) {
				if (switchStatusAction.isEnabled())
					manager.add(switchStatusAction);
				if (flipCheckStatusAction.isEnabled())
					manager.add(flipCheckStatusAction);
				if (exploreAAction.isEnabled())
					manager.add(exploreAAction);
				if (exploreBAction.isEnabled())
					manager.add(exploreBAction);
			}
		});
	}
}
