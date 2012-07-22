package net.syncarus.action;

import net.syncarus.rcp.ResourceRegistry;
import net.syncarus.rcp.SyncarusPlugin;

import org.eclipse.jface.action.Action;

public abstract class SyncarusAction extends Action {

	public SyncarusAction() {
		this(AS_PUSH_BUTTON);
	}

	public SyncarusAction(int style) {
		super("SyncarusAction", style);
	}

	protected void setIcon(String icon) {
		ResourceRegistry rr = SyncarusPlugin.getInstance().getResourceRegistry();
		setImageDescriptor(rr.getImageDescriptor(icon));
	}
}
