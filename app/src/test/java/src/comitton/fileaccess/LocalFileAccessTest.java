package src.comitton.fileaccess;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
	public void testRelativePath() {
		assertEquals("/a/b/d.zip", LocalFileAccess.relativePath("/a/b/c.zip", "d.zip"));
		assertEquals("/a/b/x/y", LocalFileAccess.relativePath("/a/b/", "x//y"));
		assertEquals("/a/c/", LocalFileAccess.relativePath("/a/b/", "../c/"));
		assertEquals("/a/", LocalFileAccess.relativePath("/a/b/", ".."));
		assertEquals("/abs/file.zip", LocalFileAccess.relativePath("/a/b/", "/abs/file.zip"));
	}

	@Test
	public void testIsSafeName() {
		assertTrue(FileAccess.isSafeName("book.zip"));
		assertTrue(FileAccess.isSafeName("folder/"));
		assertTrue(FileAccess.isSafeName("..hidden"));
		assertFalse(FileAccess.isSafeName(null));
		assertFalse(FileAccess.isSafeName(""));
		assertFalse(FileAccess.isSafeName("/"));
		assertFalse(FileAccess.isSafeName("."));
		assertFalse(FileAccess.isSafeName(".."));
		assertFalse(FileAccess.isSafeName("../"));
		assertFalse(FileAccess.isSafeName("../evil.zip"));
		assertFalse(FileAccess.isSafeName("/abs.zip"));
		assertFalse(FileAccess.isSafeName("a/b.zip"));
		assertFalse(FileAccess.isSafeName("a\\b.zip"));
		assertFalse(FileAccess.isSafeName("a\0b"));
		assertFalse(FileAccess.isSafeName("folder//"));
	}

	@Test
	public void testAccessType() {
		assertEquals(DEF.ACCESS_TYPE_LOCAL, FileAccess.accessType("/storage/emulated/0/test.zip"));
		assertEquals(DEF.ACCESS_TYPE_SMB, FileAccess.accessType("smb://192.168.1.1/share"));
		assertEquals(DEF.ACCESS_TYPE_SAF, FileAccess.accessType("content://com.android.externalstorage.documents/document/primary%3A"));
	}
}
