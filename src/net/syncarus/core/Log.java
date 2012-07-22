package net.syncarus.core;

import java.util.Date;
import java.util.EventListener;
import java.util.LinkedList;

import org.eclipse.swt.widgets.Display;

public class Log {
	public static interface ChangeListener extends EventListener {
		public void newEntry(String message, Date timestamp);

		public void clear();

		public void setEnabled(boolean enabled);
	}

	private static class Entry {
		public Entry(String message, Date timestamp) {
			this.message = message;
			this.timestamp = timestamp;
		}

		String message;
		Date timestamp;
	}

	private final LinkedList<Entry> logList = new LinkedList<Entry>();
	private final LinkedList<ChangeListener> listeners = new LinkedList<ChangeListener>();
	private boolean enabled = true;

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
		clear();
	}

	public synchronized void add(String message) {
		Date timestamp = new Date();
		logList.add(new Entry(message, timestamp));
		if (!enabled)
			return;
		syncNotify();
	}

	private synchronized void syncNotify() {
		// notify listeners
		if (Display.getCurrent() == null) {
			Display.getDefault().syncExec(new Runnable() {
				@Override
				public void run() {
					notifyListeners();
				}
			});
		} else
			notifyListeners();
	}

	private void notifyListeners() {
		for (ChangeListener listener : listeners)
			listener.setEnabled(this.enabled);
		if (logList.isEmpty()) {
			for (ChangeListener listener : listeners)
				listener.clear();
		} else {
			Entry entry = logList.getLast();
			for (ChangeListener listener : listeners)
				listener.newEntry(entry.message, entry.timestamp);
		}
	}

	public void addListener(ChangeListener listener) {
		listeners.add(listener);
	}

	public void removeListener(ChangeListener listener) {
		listeners.remove(listener);
	}

	public synchronized void clear() {
		logList.clear();
		syncNotify();
	}
}
