package src.comitton.textview;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;

public class AozoraCharNamesTest {

	private static void assertChar(char expected, String comment) {
		AozoraCharNames.Result result = AozoraCharNames.lookup(comment);
		assertEquals(comment, expected, result.code);
		assertNull(comment, result.extdata);
		assertEquals(comment, 1, result.len);
	}

	@Test
	public void fixedNames() {
		assertChar('‼', "［＃感嘆符二つ、1-8-75］");
		assertChar('卐', "［＃「卍」を左右反転したもの、U+5350］");
		AozoraCharNames.Result yoko = AozoraCharNames.lookup("［＃感嘆符三つ］");
		assertEquals(TextManager.CHARDRAW_YOKO, yoko.code);
		assertEquals("!!!", yoko.extdata);
	}

	@Test
	public void namedTables() {
		assertChar('㋐', "［＃丸ア］");
		assertChar('ⅻ', "［＃ローマ数字12小文字、1-13-32］");
		assertChar('Ⅻ', "［＃ローマ数字12］");
		assertChar('⓾', "［＃二重丸10］");
		// 表に無い名前は該当なし (後ろの規則へは進まない)
		assertSame(AozoraCharNames.Result.NONE, AozoraCharNames.lookup("［＃丸ン］"));
	}

	@Test
	public void longerNamesAreNotHiddenByShorterOnes() {
		assertChar('Ð', "［＃アイスランド語ETH、1-9-24］");
		assertChar('ð', "［＃アイスランド語ETH小文字、1-9-67］");
		assertChar('∥', "［＃平行、1-2-82］");
		assertChar('∦', "［＃平行の否定、1-2-83］");
		assertChar('▱', "［＃平行四辺形］");
	}

	@Test
	public void lastCharFallback() {
		AozoraCharNames.Result dakuten = AozoraCharNames.lookup("［＃濁点付き片仮名カ、1-5-86］");
		assertEquals(TextManager.CHARDRAW_DAKUTEN, dakuten.code);
		assertEquals("カ", dakuten.extdata);
		assertChar('ヷ', "［＃濁点付き片仮名ワ］");

		AozoraCharNames.Result handakuFu = AozoraCharNames.lookup("［＃半濁点付き小書き片仮名フ］");
		assertEquals(TextManager.CHARDRAW_KOGAKIHANDAKU, handakuFu.code);
		assertEquals("フ", handakuFu.extdata);

		AozoraCharNames.Result kogaki = AozoraCharNames.lookup("［＃小書き片仮名ト］");
		assertEquals(TextManager.CHARDRAW_KOGAKI, kogaki.code);
		assertEquals("ト", kogaki.extdata);

		assertSame(AozoraCharNames.Result.NONE, AozoraCharNames.lookup("［＃小書き］"));
	}

	@Test
	public void tateChuYokoUsesNameLength() {
		AozoraCharNames.Result one = AozoraCharNames.lookup("［＃「（1）」は縦中横］");
		assertEquals('⑴', one.code);
		assertEquals(3, one.len);
		AozoraCharNames.Result twenty = AozoraCharNames.lookup("［＃「（20）」は縦中横］");
		assertEquals('⒇', twenty.code);
		assertEquals(4, twenty.len);
		assertSame(AozoraCharNames.Result.NONE, AozoraCharNames.lookup("［＃「ABC」は縦中横］"));
	}

	@Test
	public void unknown() {
		assertSame(AozoraCharNames.Result.NONE, AozoraCharNames.lookup("［＃改ページ］"));
		assertSame(AozoraCharNames.Result.NONE, AozoraCharNames.lookup("abc"));
	}
}
