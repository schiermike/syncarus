package net.syncarus.rcp;

import java.io.PrintWriter;
import java.io.StringWriter;

import net.syncarus.gui.SyncView;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class SyncarusPlugin extends AbstractUIPlugin {
	public static final String PLUGIN_ID = "Syncarus";
	private static SyncarusPlugin instance;
	private static ResourceRegistry resourceRegistry;

	public static SyncarusPlugin getInstance() {
		return instance;
	}

	public SyncarusPlugin() {
		instance = this;
	}

	public ResourceRegistry getResourceRegistry() {
		if (resourceRegistry == null)
			resourceRegistry = new ResourceRegistry();
		return resourceRegistry;
	}
	
	public SyncView getSyncView() {
		return (SyncView)getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(SyncView.ID);
	}

	public static boolean isOSWindows() {
		return System.getProperty("os.name").toLowerCase().startsWith("windows");
	}

	public static void logError(String message, Throwable e) {
		ILog logger = instance.getLog();
		logger.log(new Status(IStatus.ERROR, PLUGIN_ID, message, e));
		StringWriter stringWriter = new StringWriter();
		String errorText = "";
		if (e != null) {
			e.printStackTrace(new PrintWriter(stringWriter));
			errorText = stringWriter.getBuffer().toString();
		}

		// show error within the context of the SWT thread
		Runnable runnable = new Runnable() {
			private String errorText;
			private String errorTitle;

			@Override
			public void run() {
				new MessageDialog(null, errorTitle, null, errorText, MessageDialog.ERROR,
						new String[] { IDialogConstants.OK_LABEL }, 0) {
					@Override
					protected boolean isResizable() {
						return true;
					}
				}.open();
			}

			Runnable setErrorText(String errorTitle, String errorText) {
				this.errorTitle = errorTitle;
				this.errorText = errorText;
				return this;
			}
		}.setErrorText(message, errorText);

		Display.getDefault().syncExec(runnable);
	}
}
