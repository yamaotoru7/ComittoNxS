package src.comitton.fileaccess;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import src.comitton.common.DEF;

public class LocalFileAccessTest {

	@Test
	public void testFilename() {
		assertEquals("novel.txt", LocalFileAccess.filename("/storage/emulated/0/Books/novel.txt"));
		assertEquals("subfolder/", LocalFileAccess.filename("/folder/subfolder/"));
		assertEquals("file.zip", LocalFileAccess.filename("file.zip"));
	}

	@Test
	public void testParent() {
		assertEquals("/storage/emulated/0/Books/", LocalFileAccess.parent("/storage/emulated/0/Books/novel.txt"));
		assertEquals("/storage/emulated/0/", LocalFileAccess.parent("/storage/emulated/0/Books/"));
	}

	@Test
	public void testAccessType() {
		assertEquals(DEF.ACCESS_TYPE_LOCAL, FileAccess.accessType("/storage/emulated/0/test.zip"));
		assertEquals(DEF.ACCESS_TYPE_SMB, FileAccess.accessType("smb://192.168.1.1/share"));
		assertEquals(DEF.ACCESS_TYPE_SAF, FileAccess.accessType("content://com.android.externalstorage.documents/document/primary%3A"));
	}
}
