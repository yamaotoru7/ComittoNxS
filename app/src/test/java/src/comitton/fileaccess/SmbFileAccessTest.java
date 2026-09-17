package src.comitton.fileaccess;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SmbFileAccessTest {

	@Test
	public void testParseUri() {
		assertArrayEquals(new String[]{"host", "share", "path/to"}, SmbFileAccess.parseUri("smb://host/share/path/to"));
		assertArrayEquals(new String[]{"host", "share", ""}, SmbFileAccess.parseUri("smb://host/share"));
		assertArrayEquals(new String[]{"host", "", ""}, SmbFileAccess.parseUri("smb://host"));
		assertArrayEquals(new String[]{"192.168.1.1", "books", "a.zip"}, SmbFileAccess.parseUri("smb://192.168.1.1/books/a.zip"));
		// smb:// 以外は空
		assertArrayEquals(new String[]{"", "", ""}, SmbFileAccess.parseUri("/storage/emulated/0/"));
	}

	@Test
	public void testParseUser() {
		assertArrayEquals(new String[]{"WORKGROUP", "user"}, SmbFileAccess.parseUser("WORKGROUP;user"));
		assertArrayEquals(new String[]{"", "user"}, SmbFileAccess.parseUser("user"));
		assertArrayEquals(new String[]{"", ""}, SmbFileAccess.parseUser(""));
	}

	@Test
	public void testRelativePath() {
		String base = "smb://host/share/dir/";
		assertEquals("smb://host/share/dir/a.zip", SmbFileAccess.relativePath(base, "a.zip"));
		assertEquals("smb://host/share/dir/a/b.zip", SmbFileAccess.relativePath(base, "a//b.zip"));
		assertEquals("smb://host/share/dir/sub/", SmbFileAccess.relativePath(base, "sub/x/../"));
		// base の外へは出られない
		assertEquals("", SmbFileAccess.relativePath(base, "../other/"));
		assertEquals("", SmbFileAccess.relativePath(base, "smb://evil/share/"));
		// base の中を指す絶対パスはそのまま
		assertEquals("smb://host/share/dir/c.zip", SmbFileAccess.relativePath(base, "smb://host/share/dir/c.zip"));
	}

	@Test
	public void testFilenameAndParent() {
		assertEquals("a.zip", SmbFileAccess.filename("smb://host/share/dir/a.zip"));
		assertEquals("dir/", SmbFileAccess.filename("smb://host/share/dir/"));
		assertEquals("", SmbFileAccess.filename(""));
		assertEquals("smb://host/share/dir/", SmbFileAccess.parent("smb://host/share/dir/a.zip"));
		assertEquals("smb://host/share/", SmbFileAccess.parent("smb://host/share/dir/"));
	}
}
