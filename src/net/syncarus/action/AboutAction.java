package net.syncarus.action;

import java.net.URL;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

import net.syncarus.rcp.ResourceRegistry;

public class AboutAction extends SyncarusAction {
	public AboutAction() {
		setText("Visit syncarus.net to check for updates.");
		setIcon(ResourceRegistry.IMAGE_ABOUT);
	}
	
	@Override
	public void run() {
		IWorkbenchBrowserSupport browserSupport = PlatformUI.getWorkbench().getBrowserSupport();
		try {
			IWebBrowser browser = browserSupport.createBrowser("someId");
			browser.openURL(new URL("http://www.syncarus.net"));
		} catch (Exception e) {}
	}
}
