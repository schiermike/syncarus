package net.syncarus.core;

import java.io.File;

import net.syncarus.gui.SyncView;
import net.syncarus.model.DiffNode;
import net.syncarus.model.SyncException;
import net.syncarus.rcp.SyncarusPlugin;

import org.eclipse.jface.operation.IRunnableWithProgress;

public abstract class SyncarusTask implements IRunnableWithProgress {
	
	private SyncView syncView;
	protected int worked = 0;

	public SyncarusTask(SyncView syncView) {
		this.syncView = syncView;
	}
	
	public SyncView getSyncView() {
		return syncView;
	}
	
	public SyncarusPlugin getPlugin() {
		return getSyncView().getPlugin();
	}
	
	public DiffNode getRootNode() {
		return getPlugin().getRootNode();
	}
	
	public Protocol getProtocol() {
		return getPlugin().getProtocol();
	}

	public Settings getSettings() {
		return getPlugin().getSettings();
	}
	
	protected void touchFile(File oldFile, File newFile) {
		syncView.getProtocol().add("Touching file '" + oldFile.getAbsolutePath() + "'");

		boolean changedWritePerms = false;
		if (!newFile.canWrite()) {
			if (!newFile.setWritable(true))
				throw new SyncException(SyncException.FILE_OPERATION_EXCEPTION, "Couldn't modify timestamp of file '"
						+ newFile + "'");
			changedWritePerms = true;
		}

		try {
			if (!newFile.setLastModified(oldFile.lastModified()))
				throw new SyncException(SyncException.FILE_OPERATION_EXCEPTION,
						"Couldn't modify the modification date of file '" + newFile + "'");
		} finally {
			if (changedWritePerms)
				newFile.setWritable(false);
		}
	}
}
