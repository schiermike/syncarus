package net.syncarus.rcp.preferences;

import java.util.regex.Pattern;

import net.syncarus.core.FileFilter;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Widget;

/**
 * A field editor for the input of regular expressions.
 */
public class RegExFieldEditor extends FieldEditor {

	private List list;
	private Composite buttonBox;
	private Button addButton;
	private Button editButton;
	private Button removeButton;

	private SelectionListener selectionListener;

	@Override
	protected void adjustForNumColumns(int numColumns) {
		Control control = getLabelControl();
		((GridData) control.getLayoutData()).horizontalSpan = numColumns;
		((GridData) list.getLayoutData()).horizontalSpan = numColumns - 1;
	}

	private void createButtons(Composite box) {
		addButton = createPushButton(box, "Add");
		editButton = createPushButton(box, "Edit");
		removeButton = createPushButton(box, "Remove");
	}

	private Button createPushButton(Composite parent, String text) {
		Button button = new Button(parent, SWT.PUSH);
		button.setText(text);
		button.setFont(parent.getFont());
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		int widthHint = convertHorizontalDLUsToPixels(button, IDialogConstants.BUTTON_WIDTH);
		data.widthHint = Math.max(widthHint, button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
		button.setLayoutData(data);
		button.addSelectionListener(getSelectionListener());
		return button;
	}

	public void createSelectionListener() {
		selectionListener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				Widget widget = event.widget;
				if (widget == addButton) {
					addPressed();
				} else if (widget == editButton) {
					editPressed();
				} else if (widget == removeButton) {
					removePressed();
				} else if (widget == list) {
					selectionChanged();
				}
			}
		};
	}

	@Override
	protected void doFillIntoGrid(Composite parent, int numColumns) {
		Control control = getLabelControl(parent);
		GridData gd = new GridData();
		gd.horizontalSpan = numColumns;
		control.setLayoutData(gd);

		list = getListControl(parent);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.verticalAlignment = GridData.FILL;
		gd.horizontalSpan = numColumns - 1;
		gd.grabExcessHorizontalSpace = true;
		list.setLayoutData(gd);

		buttonBox = getButtonBoxControl(parent);
		gd = new GridData();
		gd.verticalAlignment = GridData.BEGINNING;
		buttonBox.setLayoutData(gd);
	}

	@Override
	protected void doLoad() {
		if (list == null)
			return;

		String s = getPreferenceStore().getString(getPreferenceName());
		String[] array = FileFilter.fromPreferenceString(s);
		for (int i = 0; i < array.length; i++) {
			list.add(array[i]);
		}
	}

	@Override
	protected void doLoadDefault() {
		if (list == null)
			return;

		list.removeAll();
		String s = getPreferenceStore().getDefaultString(getPreferenceName());
		String[] array = FileFilter.fromPreferenceString(s);
		for (int i = 0; i < array.length; i++) {
			list.add(array[i]);
		}
	}

	@Override
	protected void doStore() {
		String s = FileFilter.toPreferenceString(list.getItems());
		if (s != null)
			getPreferenceStore().setValue(getPreferenceName(), s);
	}

	public Composite getButtonBoxControl(Composite parent) {
		if (buttonBox == null) {
			buttonBox = new Composite(parent, SWT.NULL);
			GridLayout layout = new GridLayout();
			layout.marginWidth = 0;
			buttonBox.setLayout(layout);
			createButtons(buttonBox);
			buttonBox.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent event) {
					addButton = null;
					removeButton = null;
					buttonBox = null;
				}
			});

		} else {
			checkParent(buttonBox, parent);
		}

		selectionChanged();
		return buttonBox;
	}

	public List getListControl(Composite parent) {
		if (list == null) {
			list = new List(parent, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL);
			list.setFont(parent.getFont());
			list.addSelectionListener(getSelectionListener());
			list.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent event) {
					list = null;
				}
			});
		} else {
			checkParent(list, parent);
		}
		return list;
	}

	@Override
	public int getNumberOfControls() {
		return 2;
	}

	private SelectionListener getSelectionListener() {
		if (selectionListener == null) {
			createSelectionListener();
		}
		return selectionListener;
	}
	
	private void addPressed() {
		InputDialog dialog = new InputDialog(buttonBox.getShell(), "New regular expression",
				"Please enter a new regular expression", "", new WildCardInputValidator());
		if (dialog.open() != Window.OK)
			return;

		String input = dialog.getValue();
		int index = list.getSelectionIndex();
		if (index >= 0)
			list.add(input, index + 1);
		else
			list.add(input, 0);

		selectionChanged();
	}
	
	private void editPressed() {
		String value = list.getItem(list.getSelectionIndex());
		InputDialog dialog = new InputDialog(buttonBox.getShell(), "Edit regular expression",
				"Modify the regular expression", value, new WildCardInputValidator());
		if (dialog.open() != Window.OK)
			return;
		
		value = dialog.getValue();
		list.setItem(list.getSelectionIndex(), value);
	}

	private void removePressed() {
		setPresentsDefaultValue(false);
		int index = list.getSelectionIndex();
		if (index >= 0) {
			list.remove(index);
			selectionChanged();
		}
	}

	protected void selectionChanged() {
		int index = list.getSelectionIndex();
		editButton.setEnabled(index >= 0);
		removeButton.setEnabled(index >= 0);
	}

	@Override
	public void setFocus() {
		if (list != null) {
			list.setFocus();
		}
	}

	/*
	 * @see FieldEditor.setEnabled(boolean,Composite).
	 */
	public void setEnabled(boolean enabled, Composite parent) {
		super.setEnabled(enabled, parent);
		getListControl(parent).setEnabled(enabled);
		addButton.setEnabled(enabled);
		removeButton.setEnabled(enabled);
	}

	public RegExFieldEditor(Composite parent) {
		init(FileFilter.NAMES_TO_IGNORE, "File and directory names to ignore");
		createControl(parent);
		doFillIntoGrid(parent, 1);
	}

	/**
	 * Validates the input for a new name to ignore. Basically checks only if
	 * the given expression is a valid regular expression.
	 */
	private static class WildCardInputValidator implements IInputValidator {
		@Override
		public String isValid(String newText) {
			try {
				Pattern.compile(newText);
				return null;
			} catch (RuntimeException e) {
				return "Invalid regular expression";
			}
		}
	}
}
