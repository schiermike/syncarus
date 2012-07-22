package net.syncarus.gui;

import java.text.DateFormat;
import java.util.Date;

import net.syncarus.action.log.ClearAction;
import net.syncarus.action.log.SwitchAction;
import net.syncarus.core.DiffControl;
import net.syncarus.core.Log;
import net.syncarus.rcp.ResourceRegistry;
import net.syncarus.rcp.SyncarusPlugin;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.part.ViewPart;

public class LogView extends ViewPart implements Log.ChangeListener {
	public static final String ID = "net.syncarus.gui.LogView";
	private Table table;
	private DateFormat dateFormat = DateFormat.getTimeInstance(DateFormat.MEDIUM);

	@Override
	public void createPartControl(Composite parent) {
		table = new Table(parent, SWT.H_SCROLL | SWT.V_SCROLL);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.setFont(SyncarusPlugin.getInstance().getResourceRegistry().getFont(ResourceRegistry.FONT_8));

		TableColumn timeColumn = new TableColumn(table, SWT.NONE);
		timeColumn.setText("Time");
		timeColumn.setWidth(60);

		TableColumn textColumn = new TableColumn(table, SWT.NONE);
		textColumn.setText("Information");
		textColumn.setWidth(100);
		textColumn.setMoveable(true);

		initializeToolBar();

		DiffControl.LOG.addListener(this);
	}

	@Override
	public void setEnabled(boolean enabled) {
		table.setEnabled(enabled);
	}

	/**
	 * Initialize the toolbar by adding the created actions
	 */
	private void initializeToolBar() {
		IToolBarManager toolbarManager = getViewSite().getActionBars().getToolBarManager();
		toolbarManager.add(new SwitchAction());
		toolbarManager.add(new ClearAction());
	}

	@Override
	public void setFocus() {
		table.setFocus();
	}

	@Override
	public void dispose() {
		DiffControl.LOG.removeListener(this);
		super.dispose();
	}

	@Override
	public void newEntry(String message, Date timestamp) {
		TableItem item = new TableItem(table, SWT.NONE);
		item.setText(new String[] { dateFormat.format(timestamp), message });
		table.setSelection(item);
	}

	@Override
	public void clear() {
		table.removeAll();
	}
}
