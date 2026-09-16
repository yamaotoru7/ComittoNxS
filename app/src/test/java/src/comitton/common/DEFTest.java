package src.comitton.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import src.comitton.fileview.data.FileData;

public class DEFTest {

	@Test
	public void testCompareFileNameNaturalOrder() {
		// 自然順ソート: 1 < 2 < 10
		assertTrue(DEF.compareFileName("vol1.zip", "vol2.zip") < 0);
		assertTrue(DEF.compareFileName("vol2.zip", "vol10.zip") < 0);
		assertTrue(DEF.compareFileName("vol10.zip", "vol1.zip") > 0);
		assertEquals(0, DEF.compareFileName("vol1.zip", "vol1.zip"));
	}

	@Test
	public void testCompareFileNameJapaneseVolume() {
		// 日本語巻名: 上 < 中 < 下
		assertTrue(DEF.compareFileName("上巻.zip", "中巻.zip") < 0);
		assertTrue(DEF.compareFileName("中巻.zip", "下巻.zip") < 0);
		assertTrue(DEF.compareFileName("上巻.zip", "下巻.zip") < 0);
	}

	@Test
	public void testCompareFileNameKanjiNumerals() {
		// 漢数字: 壱 < 弐 < 参
		assertTrue(DEF.compareFileName("第壱巻.zip", "第弐巻.zip") < 0);
		assertTrue(DEF.compareFileName("第弐巻.zip", "第参巻.zip") < 0);
	}

	@Test
	public void testCreateUrl() {
		// SMB URL 生成
		assertEquals("smb://user:pass@192.168.1.1/share",
				DEF.createUrl("smb://192.168.1.1/share", "user", "pass"));
		assertEquals("smb://user@192.168.1.1/share",
				DEF.createUrl("smb://192.168.1.1/share", "user", ""));
		assertEquals("smb://192.168.1.1/share",
				DEF.createUrl("smb://192.168.1.1/share", null, null));
		assertEquals("/storage/test",
				DEF.createUrl("/storage/test", "user", "pass"));
		assertEquals("", DEF.createUrl(null, "user", "pass"));
	}

	@Test
	public void testRemoveUTF8BOM() {
		assertEquals("Hello World", DEF.removeUTF8BOM("\uFEFFHello World"));
		assertEquals("Hello World", DEF.removeUTF8BOM("Hello World"));
		assertEquals("", DEF.removeUTF8BOM(""));
	}

	@Test
	public void testDivRoundUp() {
		assertEquals(4, DEF.divRoundUp(10, 3));
		assertEquals(3, DEF.divRoundUp(9, 3));
		assertEquals(0, DEF.divRoundUp(0, 3));
		assertEquals(1, DEF.divRoundUp(1, 3));
	}

	@Test
	public void testCheckExportKey() {
		assertFalse(DEF.checkExportKey("path"));
		assertFalse(DEF.checkExportKey("ImportSetting"));
		assertTrue(DEF.checkExportKey("/storage/emulated/0/novel.txt"));
		assertTrue(DEF.checkExportKey("smb://192.168.1.1/share"));
		assertFalse(DEF.checkExportKey("relative/path"));
		assertTrue(DEF.checkExportKey("general_setting_key"));
	}

	@Test
	public void testCheckTonlyExportKey() {
		assertTrue(DEF.checkTonlyExportKey("/local/path"));
		assertTrue(DEF.checkTonlyExportKey("smb://server/path"));
		assertTrue(DEF.checkTonlyExportKey("smb-server-alias"));
		assertFalse(DEF.checkTonlyExportKey("regular_setting"));
	}

	@Test
	public void testCalcScale() {
		// calcScale(int w, int h, int type, int lw, int lh)
		int scale1 = DEF.calcScale(100, 100, FileData.EXTTYPE_JPG, 100, 100);
		assertEquals(1, scale1);

		int scale2 = DEF.calcScale(200, 200, FileData.EXTTYPE_JPG, 100, 100);
		assertEquals(2, scale2);

		int scale4 = DEF.calcScale(400, 400, FileData.EXTTYPE_JPG, 100, 100);
		assertEquals(4, scale4);

		int scale8 = DEF.calcScale(800, 800, FileData.EXTTYPE_JPG, 100, 100);
		assertEquals(8, scale8);
	}
}
