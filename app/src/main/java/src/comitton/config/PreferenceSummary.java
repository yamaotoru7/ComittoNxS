package src.comitton.config;

import android.content.Context;
import android.text.TextUtils;

import jp.dip.muracoro.comittonx.R;

/**
 * 設定項目のサマリを「説明 + 現在値」の形に組み立てる共通処理。
 *
 * XML の android:summary を説明文として保持し、実行時に setSummary() で渡される現在値を
 * 2 行目に付け足す。説明が無い項目(summary 未指定 / "dummy")は従来通り現在値だけを表示する。
 */
public final class PreferenceSummary {
	/** 旧来の「実行時に値で上書きする」ことを示すプレースホルダ */
	private static final String PLACEHOLDER = "dummy";

	private PreferenceSummary() {
	}

	/** 説明として使えない(未指定またはプレースホルダ)なら true */
	public static boolean isPlaceholder(CharSequence text) {
		return text == null || text.length() == 0 || PLACEHOLDER.contentEquals(text);
	}

	/** XML のサマリを説明として採用し、無ければ fallback(ダイアログの説明文など)を使う */
	public static CharSequence pickDescription(CharSequence xmlSummary, CharSequence fallback) {
		if (!isPlaceholder(xmlSummary)) {
			return xmlSummary;
		}
		if (!isPlaceholder(fallback)) {
			return fallback;
		}
		return null;
	}

	/**
	 * 説明と現在値を合成する。
	 * @param description 説明文(無ければ null)
	 * @param value 現在値の表示文字列
	 */
	public static CharSequence compose(Context context, CharSequence description, CharSequence value) {
		if (isPlaceholder(description)) {
			return value;
		}
		if (value == null || value.length() == 0 || TextUtils.equals(value, description)) {
			return description;
		}
		return description + "\n" + context.getString(R.string.pref_value_format, value);
	}
}
