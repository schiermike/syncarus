package net.syncarus.test;

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Test;

public class CreateRandomDirectoryStructure {

	private static Random rand = new Random(0);

	private static String genName(boolean dir) {
		String name = "";
		int len = 6;
		while (len-- > 0)
			name += ('A' + rand.nextInt(26));
		return (dir ? "dir_" : "file_") + name;
	}

	private static File baseDir;

	@BeforeClass
	public static void setup() throws IOException {
		baseDir = new File(System.getProperty("java.io.tmpdir"), "syncarus");
		FileUtils.deleteDirectory(baseDir);
		baseDir.mkdir();
	}

	@Test
	public void tmpDirectoryExists() {
		assertTrue(baseDir.exists());
		assertTrue(baseDir.isDirectory());
		assertTrue(baseDir.canWrite());
	}

	@Test
	public void createRandomFileStructure() throws IOException {
		File srcDir = new File(baseDir, "syncarus_A");
		assertTrue(srcDir.mkdir());
		createSrcFolder(srcDir, 1.0);

		File destDir = new File(baseDir, "syncarus_B");
		assertTrue(destDir.mkdir());
		createDestFolder(srcDir, destDir);
	}
	
	@Test
	public void createDifferenceThatCanOnlyBeDetectedWithChecksums() throws IOException {
		File srcDir = new File(baseDir, "test_A");
		assertTrue(srcDir.mkdir());
		File destDir = new File(baseDir, "test_B");
		assertTrue(destDir.mkdir());
		File fileA = new File(srcDir, "almost_identical.txt");
		File fileB = new File(destDir, "almost_identical.txt");
		int size = rand.nextInt(10000);
		writeRandomContent(fileA, size);
		writeRandomContent(fileB, size);
		fileB.setLastModified(fileA.lastModified());
		
		// fileA and fileB should have the same size, same date, but different content
	}

	private static final double PROP_FOLDER = 0.145;
	private static final double PROP_FILE = 0.85;

	private void createSrcFolder(File srcDir, double cf) throws IOException {
		while (true) {
			double r = rand.nextFloat();
			if (r < PROP_FOLDER * cf) {
				File subDir = new File(srcDir, genName(true));
				subDir.mkdir();
				createSrcFolder(subDir,  cf-0.03);
				continue;
			}
			if (r < (PROP_FOLDER + PROP_FILE) * cf) {
				File file = new File(srcDir, genName(false));
				writeRandomContent(file);
				continue;
			}
			return;
		}
	}
	
	private void writeRandomContent(File file) throws IOException {
		writeRandomContent(file, rand.nextInt(200));
	}
	
	private void writeRandomContent(File file, int length) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		char buffer[] = new char[1024];
		while (length-->0) {
			buffer[0] = (char)('A' + rand.nextInt(26));
			writer.write(buffer);
		}
		writer.close();
	}

	private void createDestFolder(File srcDir, File destDir) throws IOException {
		for (File f : srcDir.listFiles()) {
			double r = rand.nextFloat();
			if (r < 0.05) {
				// file/folder only available in the source folder
				continue;
			}
			if (r < 0.95) {
				if (f.isFile()) {
					// simply copy the file
					FileUtils.copyFileToDirectory(f, destDir, true);
					manipulateFiles(f, new File(destDir, f.getName()));
				} else if (rand.nextFloat() < 0.1) {
					// copy the directory, nothing else to be done here
					FileUtils.copyDirectory(f,  destDir, true);
				} else {
					// create and empty directory and step into it
					File subDestDir = new File(destDir, f.getName());
					subDestDir.mkdir();
					createDestFolder(f, subDestDir);
				}
				continue;
			}
			// file/folder only available in the target folder
			delete(f);
		}
	}
	
	/**
	 * Precondition: both files exist.
	 * Now manipulate them
	 * @param srcFile
	 * @param destFile
	 * @throws IOException 
	 */
	private void manipulateFiles(File srcFile, File destFile) throws IOException {
		double r = rand.nextFloat();
		
		if (r < 0.05) {
			// same content, source newer time
			srcFile.setLastModified(destFile.lastModified() + 4000);
		} else if (r < 0.1) {
			// same content, destination newer time
			srcFile.setLastModified(destFile.lastModified() - 6000);
		} else if (r < 0.2) {
			// different content, source newer time
			writeRandomContent(destFile);
			srcFile.setLastModified(destFile.lastModified() + 8000);
		} else if (r < 0.3) {
			// different content, destination newer time
			writeRandomContent(destFile);
			srcFile.setLastModified(destFile.lastModified() - 10000);
		} else if (r < 0.31) {
			// different content, same time
			writeRandomContent(destFile);
			srcFile.setLastModified(destFile.lastModified());
		}
		// equal files
	}

	/**
	 * deletes the file, or, if it is a directory, recursively deletes that directory.
	 * @param file
	 * @throws IOException 
	 */
	private static void delete(File file) throws IOException  {
		if (file.isFile())
			file.delete();
		else
			FileUtils.deleteDirectory(file);
	}

}
