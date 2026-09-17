package src.comitton.textview;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 青空文庫の注記「［＃…］」で名前が指定された文字を、描画用の文字コードに変換する.<br>
 * 以前は TextManager.analyzeComment の約 1700 行の else-if だった. 規則は先頭から順に調べ、最初に一致したものを使う.
 */
final class AozoraCharNames {

	/** 変換結果. code が 0 なら該当なし */
	static final class Result {
		static final Result NONE = new Result(0, null, 1);

		final int code;
		/** null 以外なら code は CHARDRAW_* で、この文字列を描く */
		final String extdata;
		/** 置き換える元の文字数 */
		final int len;

		Result(int code, String extdata, int len) {
			this.code = code;
			this.extdata = extdata;
			this.len = len;
		}
	}

	/** 名前の後ろの「、」や「］」以降 */
	private static final Pattern NAME_TAIL = Pattern.compile("[、］].*$");
	/** 縦中横の「」」以降 */
	private static final Pattern QUOTE_TAIL = Pattern.compile("」.*$");

	private enum Fallback {
		/** 表に無ければ該当なし */
		NONE,
		/** 表に無ければ、名前の最後の文字を指定の描き方で描く */
		LAST_CHAR,
	}

	private static final class Rule {
		final String key;
		final boolean suffix;
		/** 固定の結果. 名前の表を使う規則では null */
		final Result fixed;
		final int offset;
		final Pattern tail;
		final Map<String, Result> table;
		final Fallback fallback;
		final int fallbackCode;
		/** true なら置き換える文字数は名前の長さ (縦中横) */
		final boolean lenFromName;

		Rule(String key, boolean suffix, Result fixed, int offset, Pattern tail, Map<String, Result> table,
				Fallback fallback, int fallbackCode, boolean lenFromName) {
			this.key = key;
			this.suffix = suffix;
			this.fixed = fixed;
			this.offset = offset;
			this.tail = tail;
			this.table = table;
			this.fallback = fallback;
			this.fallbackCode = fallbackCode;
			this.lenFromName = lenFromName;
		}

		boolean matches(String comment) {
			return suffix ? comment.endsWith(key) : comment.startsWith(key);
		}

		Result convert(String comment) {
			if (fixed != null) {
				return fixed;
			}
			String name = tail.matcher(comment.substring(offset)).replaceAll("");
			Result result = table.get(name);
			if (result != null) {
				return lenFromName ? new Result(result.code, result.extdata, name.length()) : result;
			}
			if (fallback == Fallback.LAST_CHAR && !name.isEmpty()) {
				return new Result(fallbackCode, name.substring(name.length() - 1), 1);
			}
			return Result.NONE;
		}
	}

	private AozoraCharNames() {
	}

	/** 一致する規則が無ければ {@link Result#NONE} */
	@NonNull
	static Result lookup(@NonNull String comment) {
		for (Rule rule : RULES) {
			if (rule.matches(comment)) {
				return rule.convert(comment);
			}
		}
		return Result.NONE;
	}

	private static Rule fixed(String key, int code) {
		return new Rule(key, false, new Result(code, null, 1), 0, null, null, Fallback.NONE, 0, false);
	}

	private static Rule fixed(String key, int code, String extdata) {
		return new Rule(key, false, new Result(code, extdata, 1), 0, null, null, Fallback.NONE, 0, false);
	}

	/** 接頭辞に続く名前を表で引く. pairs は名前と文字の組 */
	private static Rule names(String key, String... pairs) {
		return new Rule(key, false, null, key.length(), NAME_TAIL, table(pairs), Fallback.NONE, 0, false);
	}

	/** names と同じだが、表に無い名前は最後の文字を code の描き方で描く */
	private static Rule namesOrLastChar(String key, int code, Map<String, Result> table) {
		return new Rule(key, false, null, key.length(), NAME_TAIL, table, Fallback.LAST_CHAR, code, false);
	}

	private static Map<String, Result> table(String... pairs) {
		Map<String, Result> map = new HashMap<>(pairs.length);
		for (int i = 0; i < pairs.length; i += 2) {
			if (pairs[i + 1].length() != 1) {
				throw new IllegalArgumentException(pairs[i]);
			}
			// 以前の else-if と同じく先に書いたものを優先する
			if (!map.containsKey(pairs[i])) {
				map.put(pairs[i], new Result(pairs[i + 1].charAt(0), null, 1));
			}
		}
		return map;
	}

	private static Map<String, Result> with(Map<String, Result> map, String name, int code, String extdata) {
		if (!map.containsKey(name)) {
			map.put(name, new Result(code, extdata, 1));
		}
		return map;
	}

	private static final Rule[] RULES = {
			fixed("［＃感嘆符二つ", '‼'),
			fixed("［＃感嘆符三つ", TextManager.CHARDRAW_YOKO, "!!!"),
			fixed("［＃疑問符二つ", '⁇'),
			fixed("［＃疑問符感嘆符", '⁈'),
			fixed("［＃感嘆符疑問符", '⁉'),
			fixed("［＃疑問符一つ感嘆符二つ", TextManager.CHARDRAW_YOKO, "?!!"),
			fixed("［＃逆感嘆符", '¡'),
			fixed("［＃逆疑問符", '¿'),
			fixed("［＃始め二重括弧", '｟'),
			fixed("［＃終わり二重括弧", '｠'),
			fixed("［＃始め二重きっこう（亀甲）括弧", '〘'),
			fixed("［＃終わり二重きっこう（亀甲）括弧", '〙'),
			fixed("［＃始めすみ付き括弧（白）", '〖'),
			fixed("［＃終わりすみ付き括弧（白）", '〗'),
			fixed("［＃始め二重山括弧引用記号，始めギュメ", '«'),
			fixed("［＃終わり二重山括弧引用記号，終わりギュメ", '»'),
			fixed("［＃二の字点", '〻'),
			fixed("［＃ます記号", '〼'),
			fixed("［＃コト", 'ヿ'),
			fixed("［＃より", 'ゟ'),
			namesOrLastChar("［＃濁点付き", TextManager.CHARDRAW_DAKUTEN, table(
					"片仮名ワ", "ヷ", "片仮名ヰ", "ヸ", "片仮名ヱ", "ヹ", "片仮名ヲ", "ヺ", "平仮名う", "ゔ", "小書き平仮名つ", "っ")),
			namesOrLastChar("［＃半濁点付き", TextManager.CHARDRAW_HANDAKU, with(table(), "小書き片仮名フ", TextManager.CHARDRAW_KOGAKIHANDAKU, "フ")),
			namesOrLastChar("［＃小書き", TextManager.CHARDRAW_KOGAKI, table()),
			names("［＃丸",
					"ア", "㋐", "イ", "㋑", "ウ", "㋒", "エ", "㋓", "オ", "㋔", "カ", "㋕",
					"キ", "㋖", "ク", "㋗", "ケ", "㋘", "コ", "㋙", "サ", "㋚", "シ", "㋛",
					"ス", "㋜", "セ", "㋝", "ソ", "㋞", "タ", "㋟", "チ", "㋠", "ツ", "㋡",
					"テ", "㋢", "ト", "㋣", "ナ", "㋤", "ニ", "㋥", "ヌ", "㋦", "ネ", "㋧",
					"ノ", "㋨", "ハ", "㋩", "ヒ", "㋪", "フ", "㋫", "ヘ", "㋬", "ホ", "㋭",
					"マ", "㋮", "ミ", "㋯", "ム", "㋰", "メ", "㋱", "モ", "㋲", "ヤ", "㋳",
					"ユ", "㋴", "ヨ", "㋵", "ラ", "㋶", "リ", "㋷", "ル", "㋸", "レ", "㋹",
					"ロ", "㋺", "ワ", "㋻", "ヰ", "㋼", "ヱ", "㋽", "ヲ", "㋾", "月", "㊊",
					"火", "㊋", "水", "㊌", "木", "㊍", "金", "㊎", "土", "㊏", "日", "㊐",
					"株", "㊑", "有", "㊒", "社", "㊓", "名", "㊔", "特", "㊕", "財", "㊖",
					"祝", "㊗", "労", "㊘", "秘", "㊙", "男", "㊚", "女", "㊛", "適", "㊜",
					"優", "㊝", "印", "㊞", "注", "㊟", "項", "㊠", "休", "㊡", "写", "㊢",
					"正", "㊣", "付き上", "㊤", "付き中", "㊥", "付き下", "㊦", "付き左", "㊧", "付き右", "㊨",
					"医", "㊩", "宗", "㊪", "学", "㊫", "監", "㊬", "企", "㊭", "資", "㊮",
					"協", "㊯", "夜", "㊰", "A大文字", "Ⓐ", "B大文字", "Ⓑ", "C大文字", "Ⓒ", "D大文字", "Ⓓ",
					"E大文字", "Ⓔ", "F大文字", "Ⓕ", "G大文字", "Ⓖ", "H大文字", "Ⓗ", "I大文字", "Ⓘ", "J大文字", "Ⓙ",
					"K大文字", "Ⓚ", "L大文字", "Ⓛ", "M大文字", "Ⓜ", "N大文字", "Ⓝ", "O大文字", "Ⓞ", "P大文字", "Ⓟ",
					"Q大文字", "Ⓠ", "R大文字", "Ⓡ", "S大文字", "Ⓢ", "T大文字", "Ⓣ", "U大文字", "Ⓤ", "V大文字", "Ⓥ",
					"W大文字", "Ⓦ", "X大文字", "Ⓧ", "Y大文字", "Ⓨ", "Z大文字", "Ⓩ", "A小文字", "ⓐ", "B小文字", "ⓑ",
					"C小文字", "ⓒ", "D小文字", "ⓓ", "E小文字", "ⓔ", "F小文字", "ⓕ", "G小文字", "ⓖ", "H小文字", "ⓗ",
					"I小文字", "ⓘ", "J小文字", "ⓙ", "K小文字", "ⓚ", "L小文字", "ⓛ", "M小文字", "ⓜ", "N小文字", "ⓝ",
					"O小文字", "ⓞ", "P小文字", "ⓟ", "Q小文字", "ⓠ", "R小文字", "ⓡ", "S小文字", "ⓢ", "T小文字", "ⓣ",
					"U小文字", "ⓤ", "V小文字", "ⓥ", "W小文字", "ⓦ", "X小文字", "ⓧ", "Y小文字", "ⓨ", "Z小文字", "ⓩ",
					"付きマイナス", "⊖", "中黒", "⦿", "一", "㊀", "二", "㊁", "三", "㊂", "四", "㊃",
					"五", "㊄", "八", "㊇", "十", "㊉", "10", "⑩", "11", "⑪", "12", "⑫",
					"13", "⑬", "14", "⑭", "15", "⑮", "16", "⑯", "17", "⑰", "18", "⑱",
					"19", "⑲", "20", "⑳", "21", "㉑", "22", "㉒", "23", "㉓", "24", "㉔",
					"25", "㉕", "26", "㉖", "27", "㉗", "28", "㉘", "29", "㉙", "30", "㉚",
					"31", "㉛", "32", "㉜", "33", "㉝", "34", "㉞", "35", "㉟", "36", "㊱",
					"37", "㊲", "38", "㊳", "39", "㊴", "40", "㊵", "41", "㊶", "42", "㊷",
					"43", "㊸", "44", "㊹", "45", "㊺", "46", "㊻", "47", "㊼", "48", "㊽",
					"49", "㊾", "50", "㊿", "0", "⓪", "1", "①", "2", "②", "3", "③",
					"4", "④", "5", "⑤", "6", "⑥", "7", "⑦", "8", "⑧", "9", "⑨"),
			names("［＃ローマ数字",
					"1小文字", "ⅰ", "2小文字", "ⅱ", "3小文字", "ⅲ", "4小文字", "ⅳ", "5小文字", "ⅴ", "6小文字", "ⅵ",
					"7小文字", "ⅶ", "8小文字", "ⅷ", "9小文字", "ⅸ", "10小文字", "ⅹ", "11小文字", "ⅺ", "12小文字", "ⅻ",
					"10", "Ⅹ", "11", "Ⅺ", "12", "Ⅻ", "1", "Ⅰ", "2", "Ⅱ", "3", "Ⅲ",
					"4", "Ⅳ", "5", "Ⅴ", "6", "Ⅵ", "7", "Ⅶ", "8", "Ⅷ", "9", "Ⅸ"),
			names("［＃黒丸",
					"10", "❿", "11", "⓫", "12", "⓬", "13", "⓭", "14", "⓮", "15", "⓯",
					"16", "⓰", "17", "⓱", "18", "⓲", "19", "⓳", "20", "⓴", "1", "❶",
					"2", "❷", "3", "❸", "4", "❹", "5", "❺", "6", "❻", "7", "❼",
					"8", "❽", "9", "❾"),
			names("［＃二重丸",
					"10", "⓾", "1", "⓵", "2", "⓶", "3", "⓷", "4", "⓸", "5", "⓹",
					"6", "⓺", "7", "⓻", "8", "⓼", "9", "⓽"),
			names("［＃キャロン付き",
					"A小文字", "ǎ", "I小文字", "ǐ", "O小文字", "ǒ", "u小文字", "ǔ", "L小文字", "ľ", "S小文字", "š",
					"T小文字", "ť", "Z小文字", "ž", "C小文字", "č", "E小文字", "ě", "D小文字", "ď", "N小文字", "ň",
					"R小文字", "ř", "A", "Ǎ", "O", "Ǒ", "L", "Ľ", "S", "Š", "T", "Ť",
					"Z", "Ž", "C", "Č", "E", "Ě", "D", "Ď", "N", "Ň", "R", "Ř"),
			fixed("［＃マクロンとダイエレシス付きU小文字", 'ǖ'),
			fixed("［＃アキュートアクセントとダイエレシス付きU小文字", 'ǘ'),
			fixed("［＃キャロンとダイエレシス付きU小文字", 'ǚ'),
			fixed("［＃グレーブアクセントとダイエレシス付きU小文字", 'ǜ'),
			fixed("［＃アイスランド語ETH小文字", 'ð'),
			fixed("［＃アイスランド語ETH", 'Ð'),
			fixed("［＃アイスランド語THORN小文字", 'þ'),
			fixed("［＃アイスランド語THORN", 'Þ'),
			fixed("［＃オゴネク付きA小文字", 'ą'),
			fixed("［＃オゴネク付きA", 'Ą'),
			fixed("［＃上ドット付きZ小文字", 'ż'),
			fixed("［＃上ドット付きZ", 'Ż'),
			fixed("［＃ブリーブ付きA小文字", 'ă'),
			fixed("［＃ブリーブ付きA", 'Ă'),
			fixed("［＃オゴネク付きE小文字", 'ę'),
			fixed("［＃オゴネク付きE", 'Ę'),
			fixed("［＃ダブルアキュートアクセント付きO小文字", 'ő'),
			fixed("［＃ダブルアキュートアクセント付きO", 'Ő'),
			fixed("［＃ダブルアキュートアクセント付きU小文字", 'ű'),
			fixed("［＃ダブルアキュートアクセント付きU", 'Ű'),
			fixed("［＃ブリーブ付きU小文字", 'ŭ'),
			fixed("［＃ブリーブ付きU", 'Ŭ'),
			fixed("［＃フック付きM小文字，有声唇歯鼻音", 'ɱ'),
			fixed("［＃フック付きV小文字，有声唇歯接近音", 'ʋ'),
			fixed("［＃フィッシュフック付きR小文字，有声歯茎弾き音", 'ɾ'),
			fixed("［＃発音記号", 'ʃ'),
			fixed("［＃ベルト付きL小文字，無声歯茎側面摩擦音", 'ɬ'),
			fixed("［＃LEZH小文字，有声歯茎側面摩擦音", 'ɮ'),
			fixed("［＃ターンドR小文字，無声歯茎接近音", 'ɹ'),
			fixed("［＃レトロフレックスフック付きT小文字，無声そり舌破裂音", 'ʈ'),
			fixed("［＃テール付きD小文字，有声そり舌破裂音", 'ɖ'),
			fixed("［＃レトロフレックスフック付きN小文字，有声そり舌鼻音", 'ɳ'),
			fixed("［＃テール付きR小文字，有声そり舌弾き音", 'ɽ'),
			fixed("［＃フック付きS小文字，無声そり舌摩擦音", 'ʂ'),
			fixed("［＃レトロフレックスフック付きZ小文字，有声そり舌摩擦音", 'ʐ'),
			fixed("［＃フック付きターンドR小文字，有声そり舌接近音", 'ɻ'),
			fixed("［＃レトロフレックスフック付きL小文字，有声そり舌側面接近音", 'ɭ'),
			fixed("［＃ストローク付きドットなしJ小文字，有声硬口蓋破裂音", 'ɟ'),
			fixed("［＃左フック付きN小文字，有声硬口蓋鼻音", 'ɲ'),
			fixed("［＃クロスドテール付きJ小文字，有声硬口蓋摩擦音", 'ʝ'),
			fixed("［＃ターンドY小文字，有声硬口", 'ʎ'),
			fixed("［＃スクリプトG小文字，有声軟口蓋破裂音", 'ɡ'),
			fixed("［＃ロングレッグ付きターンドM小文字，有声軟口蓋接近音", 'ɰ'),
			fixed("［＃インバーテッドRスモールキャピタル，有声口蓋垂摩擦音", 'ʁ'),
			fixed("［＃リバースドグロッタルストップ，有声咽頭摩擦音", 'ʕ'),
			fixed("［＃グロッタルストップ，無声声門破裂音", 'ʔ'),
			fixed("［＃フック付きH小文字，有声声門摩擦音", 'ɦ'),
			fixed("［＃両唇吸着音", 'ʘ'),
			fixed("［＃硬口蓋歯茎吸着音", 'ǂ'),
			fixed("［＃フック付きB小文字，有声両唇内破音", 'ɓ'),
			fixed("［＃フック付きD小文字，有声歯茎内破音", 'ɗ'),
			fixed("［＃フックとストローク付きドットなしJ小文字，有声硬口蓋内破音", 'ʄ'),
			fixed("［＃フック付きG小文字，有声軟口蓋内破音", 'ɠ'),
			fixed("［＃フック付きG大文字，有声口蓋垂内破音", 'Ɠ'),
			fixed("［＃バー付きU小文字，円唇中舌狭母音", 'ʉ'),
			fixed("［＃リバースドE小文字，非円唇中舌狭・中段母音", 'ɘ'),
			fixed("［＃バー付きO小文字，円唇中舌狭・中段母音", 'ɵ'),
			fixed("［＃リバースドオープンE小文字，非円唇中舌広・中段母音", 'ɜ'),
			fixed("［＃クローズドリバースドオープンE小文字，円唇中舌広・中段母音", 'ɞ'),
			fixed("［＃ターンドA小文字，非円唇中舌狭めの広母音", 'ɐ'),
			fixed("［＃ターンドM小文字，非円唇後舌狭母音", 'ɯ'),
			fixed("［＃UPSILON小文字，円唇後舌広めの狭母音", 'ʊ'),
			fixed("［＃ベビーガンマ，非円唇後舌狭中段母音", 'ɤ'),
			fixed("［＃ターンドV小文字，非円唇後舌広・中段母音", 'ʌ'),
			fixed("［＃スクリプトA小文字，非円唇後舌広母音", 'ɑ'),
			fixed("［＃ターンドALPHA小文字，円唇後舌広母音", 'ɒ'),
			fixed("［＃ターンドW小文字，無声両唇・軟口蓋摩擦音", 'ʍ'),
			fixed("［＃ターンドH小文字，有声両唇・硬口蓋接近音", 'ɥ'),
			fixed("［＃ストローク付きリバースドグロッタルストップ，有声咽頭蓋摩擦音", 'ʢ'),
			fixed("［＃ストローク付きグロッタルストップ，咽頭蓋破裂音", 'ʡ'),
			fixed("［＃カール付きC小文字，歯茎・硬口蓋摩擦音", 'ɕ'),
			fixed("［＃カール付きZ小文字，歯茎・硬口蓋摩擦音", 'ʑ'),
			fixed("［＃ロングレッグ付きターンドR小文字，歯茎側面弾き音", 'ɺ'),
			fixed("［＃フック付きHENG小文字，無声後部歯茎軟口蓋摩擦音", 'ɧ'),
			fixed("［＃フック付きSCHWA", 'ɚ'),
			fixed("［＃グレーブアクセント付きAE小文字", 'æ'),
			fixed("［＃アキュートアクセント付きAE小文字", 'ǽ'),
			fixed("［＃グレーブアクセント付きスクリプトA小文字", 'ὰ'),
			fixed("［＃グレーブアクセント付きオープンO小文字", 'ɔ'),
			fixed("［＃アキュートアクセント付きオープンO小文字", 'ɔ'),
			fixed("［＃グレーブアクセント付きターンドV小文字", 'ʌ'),
			fixed("［＃アキュートアクセント付きターンドV小文字", 'ʌ'),
			fixed("［＃グレーブアクセント付きSCHWA小文字", 'ə'),
			fixed("［＃グレーブアクセントとフック付きSCHWA小文字", 'ɚ'),
			fixed("［＃アキュートアクセントとフック付きSCHWA小文字", 'ɚ'),
			fixed("［＃「v」は下線（_）付き", 'v'),
			fixed("［＃ギリシア小文字ファイナルSIGMA", 'ς'),
			fixed("［＃グレーブアクセント付きEPSILON小文字", 'ὲ'),
			fixed("［＃アキュートアクセント付きEPSILON小文字", 'έ'),
			fixed("［＃部分集合の否定（逆方向）", '⊅'),
			fixed("［＃部分集合の否定", '⊄'),
			fixed("［＃真部分集合2（逆方向）", '⊋'),
			fixed("［＃真部分集合2", '⊊'),
			fixed("［＃要素の否定，元の否定", '∉'),
			fixed("［＃空集合", '∅'),
			fixed("［＃射影的関係", '⌅'),
			fixed("［＃背景的関係", '⌆'),
			fixed("［＃直和", '⊕'),
			fixed("［＃テンソル積", '⊗'),
			fixed("［＃平行の否定", '∦'),
			fixed("［＃平行四辺形", '▱'),
			fixed("［＃平行", '∥'),
			fixed("［＃合同否定", '≢'),
			fixed("［＃漸進的に等しい，ホモトープ", '≃'),
			fixed("［＃同形", '≅'),
			fixed("［＃同相", '≈'),
			fixed("［＃小さいか大きい", '≶'),
			fixed("［＃大きいか小さい", '≷'),
			fixed("［＃同等", '↔'),
			fixed("［＃小さいか等しいか大きい", '⋚'),
			fixed("［＃大きいか等しいか小さい", '⋛'),
			fixed("［＃負又は正符号", '∓'),
			fixed("［＃アレフ", 'ℵ'),
			fixed("［＃エイチバー", 'ℏ'),
			fixed("［＃2プラス", '⧺'),
			fixed("［＃3プラス", '⧻'),
			fixed("［＃直角三角", '⊿'),
			fixed("［＃直角", '∟'),
			fixed("［＃経路積分記号", '∮'),
			fixed("［＃２分の１", '½'),
			fixed("［＃３分の１", '⅓'),
			fixed("［＃３分の２", '⅔'),
			fixed("［＃４分の１", '¼'),
			fixed("［＃４分の３", '¾'),
			fixed("［＃５分の１", '⅕'),
			fixed("［＃ＨＰ，ホースパワー（馬力）", '㏋'),
			fixed("［＃リットル", 'ℓ'),
			fixed("［＃モー", '℧'),
			fixed("［＃不特定通貨記号", '¤'),
			fixed("［＃ユーロ記号", '€'),
			fixed("［＃「オンス」の単位記号", '℥'),
			fixed("［＃破断線", '¦'),
			fixed("［＃著作権表示記号", '©'),
			fixed("［＃登録商標記号", '®'),
			fixed("［＃女性序数標識", 'ª'),
			fixed("［＃男性序数標識", 'º'),
			fixed("［＃ダブルアステ", '⁑'),
			fixed("［＃アステリズム", '⁂'),
			fixed("［＃左半黒丸", '◐'),
			fixed("［＃右半黒丸", '◑'),
			fixed("［＃下半黒丸", '◒'),
			fixed("［＃上半黒丸", '◓'),
			fixed("［＃右向三角", '▷'),
			fixed("［＃右向黒三角", '▶'),
			fixed("［＃左向三角", '◁'),
			fixed("［＃左向黒三角", '◀'),
			fixed("［＃スペード（白）", '♤'),
			fixed("［＃スペード", '♠'),
			fixed("［＃ダイヤ（白）", '♢'),
			fixed("［＃ダイヤ", '♦'),
			fixed("［＃ハート（白）", '♡'),
			fixed("［＃ハート", '♥'),
			fixed("［＃クラブ（白）", '♧'),
			fixed("［＃クラブ", '♣'),
			fixed("［＃白将棋駒", '☖'),
			fixed("［＃黒将棋駒", '☗'),
			fixed("［＃郵便マーク", '〠'),
			fixed("［＃電話マーク", '☎'),
			fixed("［＃晴マーク", '☀'),
			fixed("［＃曇マーク", '☁'),
			fixed("［＃雨マーク", '☂'),
			fixed("［＃雪マーク", '☃'),
			fixed("［＃温泉マーク", '♨'),
			fixed("［＃指示マーク", '☞'),
			fixed("［＃チェックマーク", '✓'),
			fixed("［＃コマンド記号", '⌘'),
			fixed("［＃空白記号", '␣'),
			fixed("［＃リターン記号（リターン）", '⏎'),
			fixed("［＃四つ菱", '❖'),
			fixed("［＃白ビュレット", '◦'),
			fixed("［＃ビュレット", '•'),
			fixed("［＃白ゴマ", '﹆'),
			fixed("［＃ゴマ", '﹅'),
			fixed("［＃蛇の目", '◉'),
			fixed("［＃歌記号", '〽'),
			fixed("［＃左上向矢印", '↖'),
			fixed("［＃右上向矢印", '↗'),
			fixed("［＃右下向矢印", '↘'),
			fixed("［＃左下向矢印", '↙'),
			fixed("［＃右矢印左矢印", '⇄'),
			fixed("［＃左向白矢印", '⇦'),
			fixed("［＃上向白矢印", '⇧'),
			fixed("［＃右向白矢印", '⇨'),
			fixed("［＃下向白矢印", '⇩'),
			fixed("［＃曲がり矢印上がる", '⤴'),
			fixed("［＃曲がり矢印下がる", '⤵'),
			fixed("［＃四分音符", '♩'),
			fixed("［＃連こう（桁）付き八分音符", '♫'),
			fixed("［＃連こう（桁）付き十六分音符", '♬'),
			fixed("［＃ナチュラル", '♮'),
			names("［＃全角",
					"括弧付き株", "㈱", "括弧付き有", "㈲", "括弧付き代", "㈹", "株式会社", "㍿", "ミリ", "㍉", "キロ", "㌔",
					"センチ", "㌢", "メートル", "㍍", "グラム", "㌘", "トン", "㌧", "アール", "㌃", "ヘクタール", "㌶",
					"リットル", "㍑", "ワット", "㍗", "カロリー", "㌍", "ドル", "㌦", "セント", "㌣", "パーセント", "㌫",
					"ミリバール", "㍊", "ページ", "㌻", "MM", "㎜", "CM", "㎝", "KM", "㎞", "MG", "㎎",
					"KG", "㎏", "CC", "㏄", "M2", "㎡", "NO", "№", "KK", "㏍", "TEL", "℡",
					"元号明治", "㍾", "元号大正", "㍽", "元号昭和", "㍼", "元号平成", "㍻"),
			fixed("［＃「卍」を左右反転したもの", '卐'),
			fixed("［＃AE小文字", 'æ'),
			// 「（1）」は縦中横 など. 置き換える文字数は括弧を含む名前の長さ
			new Rule("は縦中横］", true, null, 3, QUOTE_TAIL, table(
					"（1）", "⑴", "（2）", "⑵", "（3）", "⑶", "（4）", "⑷", "（5）", "⑸", "（6）", "⑹",
					"（7）", "⑺", "（8）", "⑻", "（9）", "⑼", "（10）", "⑽", "（11）", "⑾", "（12）", "⑿",
					"（13）", "⒀", "（14）", "⒁", "（15）", "⒂", "（16）", "⒃", "（17）", "⒄", "（18）", "⒅",
					"（19）", "⒆", "（20）", "⒇"),
					Fallback.NONE, 0, true),
	};
}
