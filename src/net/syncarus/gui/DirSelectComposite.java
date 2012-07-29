package net.syncarus.gui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.filechooser.FileSystemView;

import net.syncarus.core.DiffController;
import net.syncarus.rcp.ResourceRegistry;
import net.syncarus.rcp.SyncarusPlugin;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TypedListener;

/**
 * Useful component allowing the user to choose a directory form a treeViewer.
 */
public class DirSelectComposite extends Composite {
	private TableViewer viewer;

	public DirSelectComposite(Composite parent, int style) {
		super(parent, style);
		setBackground(new Color(this.getDisplay(), 255, 0, 0));
		setLayout(new FillLayout());

		buildListViewer();
	}

	/**
	 * build a new listViewer where the initial root is the second element of
	 * java.io.File.listRoots() if there is only one root, use this one.
	 */
	private void buildListViewer() {
		viewer = new TableViewer(this);

		viewer.setLabelProvider(new FileLabelProvider());

		viewer.setContentProvider(new FileContentProvider());

		viewer.setSorter(new ViewerSorter() {
			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				EncapsulatedFile ef1 = (EncapsulatedFile) e1;
				EncapsulatedFile ef2 = (EncapsulatedFile) e2;
				return ef1.compareTo(ef2);
			}
		});

		viewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				IStructuredSelection selection = (IStructuredSelection) event.getSelection();
				setCurrentDirectory(((EncapsulatedFile) selection.getFirstElement()).getFile());
			}
		});
		setCurrentDirectory(DiffController.getDefaultDirectory());
	}

	/**
	 * @param directory
	 *            set this directory as new root of the treeViewer
	 */
	public void setCurrentDirectory(File directory) {
		viewer.setInput(directory);
		viewer.getTable().setSelection(viewer.getTable().getItem(0));
		notifyListeners(SWT.Selection, null);
	}

	public void addSelectionListener(SelectionListener listener) {
		checkWidget();
		if (listener == null)
			return;
		TypedListener typedListener = new TypedListener(listener);
		addListener(SWT.Selection, typedListener);
		addListener(SWT.DefaultSelection, typedListener);
	}

	/**
	 * @return the currently selected directory
	 */
	public File getCurrentDirectory() {
		return (File) viewer.getInput();
	}

}

/**
 * Helper class which encapsulates a file and the information whether this file
 * is a root (example: C:\, A:\, ..) or if it's a parent (first element in
 * treeViewer - UPDIR)
 */
class EncapsulatedFile implements Comparable<EncapsulatedFile> {
	private File file = null;
	private boolean isParent = false;
	private boolean isRoot;

	/**
	 * Constructor for the root of root Files (parent of '/', 'C:\', ...)
	 */
	public EncapsulatedFile() {
		this.file = new File("");
		this.isParent = true;
		this.isRoot = false;
	}

	/**
	 * Constructor for root Files ('/', 'C:\', ...)
	 * 
	 * @param file
	 */
	public EncapsulatedFile(File file) {
		this.file = file;
		this.isParent = false;
		this.isRoot = true;
	}

	/**
	 * Constructor for regular, existing files and directories
	 * 
	 * @param file
	 * @param isParent
	 */
	public EncapsulatedFile(File file, boolean isParent) {
		this.file = file;
		this.isParent = isParent;
		this.isRoot = false;
	}

	public File getFile() {
		return file;
	}

	/**
	 * compares two encapsulated files where the order is as follows:
	 * <ul>
	 * <li>isParent()</li>
	 * <li>compare case-insensitive directory names</li>
	 * </ul>
	 * 
	 * @param other
	 * @return result of comparison
	 */
	@Override
	public int compareTo(EncapsulatedFile other) {

		if (isParent())
			return -1;
		if (other.isParent())
			return 1;

		return file.getName().compareToIgnoreCase(other.getFile().getName());
	}

	public boolean isParent() {
		return isParent;
	}

	public boolean isRoot() {
		return isRoot;
	}
}

class FileContentProvider extends ArrayContentProvider implements IStructuredContentProvider {
	@Override
	public Object[] getElements(Object inputElement) {
		File file = (File) inputElement;

		List<EncapsulatedFile> fileList = new ArrayList<EncapsulatedFile>();

		if (!file.exists()) // top - level, list drives
		{
			for (File rootFile : File.listRoots())
				fileList.add(new EncapsulatedFile(rootFile));
		} else {
			if (file.getParentFile() == null) // go to drive selection
				fileList.add(new EncapsulatedFile());
			else
				// go one dir up
				fileList.add(new EncapsulatedFile(file.getParentFile(), true));

			if (file.listFiles() != null)
				for (File subFile : file.listFiles())
					if (subFile.isDirectory())
						fileList.add(new EncapsulatedFile(subFile, false));
		}

		return fileList.toArray();
	}
}

class FileLabelProvider extends LabelProvider {

	@Override
	public String getText(Object element) {
		EncapsulatedFile encapsFile = (EncapsulatedFile) element;
		if (encapsFile.isParent())
			return "";

		if (encapsFile.isRoot()) {
			String rootName = FileSystemView.getFileSystemView().getSystemDisplayName(encapsFile.getFile());
			if (rootName == null || rootName.equals(""))
				rootName = encapsFile.getFile().getAbsolutePath();
			return rootName;
		}

		String name = encapsFile.getFile().getName();
		if (name == null || name.equals(""))
			name = encapsFile.getFile().getAbsolutePath();
		return name;
	}

	@Override
	public Image getImage(Object element) {
		EncapsulatedFile file = (EncapsulatedFile) element;

		String imageKey = null;

		if (file.isParent())
			imageKey = ResourceRegistry.IMAGE_DIR_UP;
		else if (file.isRoot()) {
			if (FileSystemView.getFileSystemView().isFloppyDrive(file.getFile()))
				imageKey = ResourceRegistry.IMAGE_DIR_FLOPPY;
			else
				imageKey = ResourceRegistry.IMAGE_DIR_HDD;
		} else
			imageKey = ResourceRegistry.IMAGE_DIR_NORMAL;

		return SyncarusPlugin.getInstance().getResourceRegistry().getImage(imageKey);
	}
}