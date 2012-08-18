package net.syncarus.test;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.BeforeClass;
import org.junit.Test;

public class Charsets {
	
	private static File baseDir;
	
	@BeforeClass
	public static void createDirectory() throws IOException {
		baseDir = new File(System.getProperty("java.io.tmpdir"), "syncarus");
		if (baseDir.exists())
			FileUtils.cleanDirectory(baseDir);
		else
			baseDir.mkdir();
	}
	
	@Test
	public void listEncodings() throws UnsupportedEncodingException  {
		assert Charset.availableCharsets().containsKey("UTF-8");
		String slash = "/";
		assertEquals(1, slash.getBytes("UTF-8").length);
		assertEquals(47, slash.getBytes("UTF-8")[0]);
		assertEquals('/', slash.getBytes("UTF-8")[0]);
	}
	
	@Test
	public void createWeirdFile() throws IOException {
		int r = 10000;
		while (r-- > 0) {
			String name = RandomStringUtils.random(10);
			name = name.replaceAll("/|\n", "_");
			File f = new File(System.getProperty("java.io.tmpdir"), "syncarus" + File.separatorChar + name);
			f.createNewFile();
		}
	}
}
