package net.syncarus.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import net.syncarus.model.CancelationException;
import net.syncarus.model.SyncException;

import org.apache.commons.io.FileUtils;

/**
 * IO-function library providing functions for
 * <ul>
 * <li>copying files</li>
 * <li>deleting files</li>
 * <li>recursively copying directories</li>
 * <li>recursively deleting directories</li>
 * <li>comparing file modification dates</li>
 * <li>calculating sum of files contained in a directory</li>
 * <li>calculating sum of bytes contained in a directory</li>
 * <li>formatted output of byte sizes</li>
 * </ul>
 * It is heavily used by the <code>DifferentiationJob</code> and the
 * <code>SynchronisationJob</code>. This is also the main reason not to use
 * {@link FileUtils} functionality, which doesn't allow feedback operations.
 */
public class FileOperation {
	/**
	 * copies all files located at source to the destination located at target
	 * deleting old content
	 * 
	 * @param source
	 *            the source file/folder
	 * @param target
	 *            the target file/folder
	 * @throws IOException
	 * @throws Exception
	 */
	public static void copy(File source, File target, SyncTask runnable) throws IOException, CancelationException {
		if (target.exists())
			FileUtils.forceDelete(target);
		if (source.isFile())
			copyFile(source, target, runnable);
		else
			copyDir(source, target, runnable);
	}

	/**
	 * copies sourceFile to targetFile using a copy-buffer of 4096 bytes.
	 * 
	 * @param sourceFile
	 * @param targetFile
	 * @throws IOException
	 */
	private static void copyFile(File sourceFile, File targetFile, SyncTask runnable) throws IOException,
			CancelationException {
		DiffController.LOG.add("Copying file '" + sourceFile.getName() + "'");

		if (targetFile.exists())
			targetFile.delete();
		byte[] buffer = new byte[4096];
		FileInputStream fis = new FileInputStream(sourceFile);
		FileOutputStream fos = new FileOutputStream(targetFile);
		int len = 0;
		while ((len = Math.abs(fis.available())) > 0) {
			fis.read(buffer);
			if (len > buffer.length)
				len = buffer.length;
			fos.write(buffer, 0, len);
			try {
				if (runnable != null)
					runnable.worked(len);
			} catch (CancelationException e) {
				fis.close();
				fos.close();
				DiffController.LOG.add("Aborted copy process - deleting file '" + sourceFile.getName() + "'");
				// avoid having "half" files which are useless
				targetFile.delete();
				throw e;
			}
		}
		fis.close();
		fos.close();

		targetFile.setLastModified(sourceFile.lastModified());
	}

	/**
	 * creates directory targetDir and copies whole content of sourceDir to
	 * targetDir
	 * 
	 * @param sourceDir
	 * @param targetDir
	 * @throws IOException
	 */
	private static void copyDir(File sourceDir, File targetDir, SyncTask runnable) throws IOException,
			CancelationException {
		targetDir.mkdir();
		targetDir.setLastModified(sourceDir.lastModified());

		if (sourceDir.listFiles() == null)
			return;
		for (File subSourceFile : sourceDir.listFiles()) {
			File subTargetFile = new File(targetDir.getAbsolutePath() + File.separator + subSourceFile.getName());
			if (subSourceFile.isDirectory())
				copyDir(subSourceFile, subTargetFile, runnable);
			else
				copyFile(subSourceFile, subTargetFile, runnable);
		}
	}

	/**
	 * @param dir
	 * @return sum of file-sizes located in this directory.<br>
	 *         If object is a file, return size of this file
	 */
	public static long totalNumOfBytes(File object) {
		if (object.isFile())
			return object.length();
		return FileUtils.sizeOfDirectory(object);
	}

	/**
	 * @param dir
	 * @return number of files located in this directory.
	 */
	public static long totalNumOfFiles(File object) {
		if (object.isFile())
			return 1;

		int count = 0;
		File[] files = object.listFiles();
		if (files == null)
			throw new SyncException(SyncException.PATH_EXCEPTION, "Cannot access directory '"
					+ object.getAbsolutePath() + "'!");
		for (File f : files)
			count += FileOperation.totalNumOfFiles(f);
		return count;
	}

	/**
	 * @param parent
	 * @param child
	 * @return true when <code>child</code> is a sub-directory of
	 *         <code>parent</code>
	 */
	public static boolean isSubdirectory(File parent, File child) {
		return isSubdirectory(parent.getAbsolutePath(), child.getAbsolutePath());
	}

	/**
	 * @see #isSubdirectory(File, File)
	 * @param parent
	 * @param child
	 * @return
	 */
	public static boolean isSubdirectory(String parent, String child) {
		if (parent.equals(child))
			return true;
		if (!child.startsWith(parent))
			return false;
		if (child.charAt(parent.length()) == File.separatorChar)
			return true;
		return false;
	}
}
