package src.comitton.config;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Arrays;

import jp.dip.muracoro.comittonx.R;
import src.comitton.common.DEF;
import src.comitton.common.Logcat;
import src.comitton.helpview.HelpActivity;

public class SetCommonActivity extends BasePreferenceActivity {

	public static final int[] RotateBtnName =
		{ R.string.rotabtn00	// 使用しない
		, R.string.rotabtn01	// フォーカスキー
		, R.string.rotabtn02 };	// シャッターキー

	@Override
	protected PreferenceFragmentCompat createPreferenceFragment() {
		return new SetCommonFragment();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		loadSettings(PreferenceManager.getDefaultSharedPreferences(this));
	}

	public static class SetCommonFragment extends PreferenceFragmentCompat implements OnSharedPreferenceChangeListener {
		private ListPreference mRotateBtn;
		private ListPreference mCharset;

		private EditTextPreference mPriorityWord01;
		private EditTextPreference mPriorityWord02;
		private EditTextPreference mPriorityWord03;
		private EditTextPreference mPriorityWord04;
		private EditTextPreference mPriorityWord05;
		private EditTextPreference mPriorityWord06;
		private EditTextPreference mPriorityWord07;
		private EditTextPreference mPriorityWord08;
		private EditTextPreference mPriorityWord09;
		private EditTextPreference mPriorityWord10;

		@Override
		public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
			setPreferencesFromResource(R.xml.common, rootKey);

			mRotateBtn  = findPreference(DEF.KEY_ROTATEBTN);
			mCharset    = findPreference(DEF.KEY_CHARSET);

			mPriorityWord01 = findPreference(DEF.KEY_SORT_PRIORITY_WORD_01);
			mPriorityWord02 = findPreference(DEF.KEY_SORT_PRIORITY_WORD_02);
			mPriorityWord03 = findPreference(DEF.KEY_SORT_PRIORITY_WORD_03);
			mPriorityWord04 = findPreference(DEF.KEY_SORT_PRIORITY_WORD_04);
			mPriorityWord05 = findPreference(DEF.KEY_SORT_PRIORITY_WORD_05);
			mPriorityWord06 = findPreference(DEF.KEY_SORT_PRIORITY_WORD_06);
			mPriorityWord07 = findPreference(DEF.KEY_SORT_PRIORITY_WORD_07);
			mPriorityWord08 = findPreference(DEF.KEY_SORT_PRIORITY_WORD_08);
			mPriorityWord09 = findPreference(DEF.KEY_SORT_PRIORITY_WORD_09);
			mPriorityWord10 = findPreference(DEF.KEY_SORT_PRIORITY_WORD_10);

			// 項目選択
			Preference onlineHelp = findPreference(DEF.KEY_COMMHELP);
			if (onlineHelp != null) {
				onlineHelp.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(@NonNull Preference preference) {
						if (getActivity() == null) {
							return false;
						}
						// Activityの遷移
						Resources res = getResources();
						String url = res.getString(R.string.url_common);	// 設定画面
						Intent intent = new Intent(getActivity(), HelpActivity.class);
						intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						intent.putExtra("Url", url);
						startActivity(intent);
						return true;
					}
				});
			}
		}

		@Override
		public void onResume() {
			super.onResume();
			SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
			if (sharedPreferences != null) {
				sharedPreferences.registerOnSharedPreferenceChangeListener(this);

				if (mRotateBtn != null) mRotateBtn.setSummary(getRotateBtnSummary(sharedPreferences));
				if (mCharset != null) mCharset.setSummary(getCharsetSummary(sharedPreferences));

				if (mPriorityWord01 != null) mPriorityWord01.setSummary(sharedPreferences.getString(DEF.KEY_SORT_PRIORITY_WORD_01, ""));
				if (mPriorityWord02 != null) mPriorityWord02.setSummary(sharedPreferences.getString(DEF.KEY_SORT_PRIORITY_WORD_02, ""));
				if (mPriorityWord03 != null) mPriorityWord03.setSummary(sharedPreferences.getString(DEF.KEY_SORT_PRIORITY_WORD_03, ""));
				if (mPriorityWord04 != null) mPriorityWord04.setSummary(sharedPreferences.getString(DEF.KEY_SORT_PRIORITY_WORD_04, ""));
				if (mPriorityWord05 != null) mPriorityWord05.setSummary(sharedPreferences.getString(DEF.KEY_SORT_PRIORITY_WORD_05, ""));
				if (mPriorityWord06 != null) mPriorityWord06.setSummary(sharedPreferences.getString(DEF.KEY_SORT_PRIORITY_WORD_06, ""));
				if (mPriorityWord07 != null) mPriorityWord07.setSummary(sharedPreferences.getString(DEF.KEY_SORT_PRIORITY_WORD_07, ""));
				if (mPriorityWord08 != null) mPriorityWord08.setSummary(sharedPreferences.getString(DEF.KEY_SORT_PRIORITY_WORD_08, ""));
				if (mPriorityWord09 != null) mPriorityWord09.setSummary(sharedPreferences.getString(DEF.KEY_SORT_PRIORITY_WORD_09, ""));
				if (mPriorityWord10 != null) mPriorityWord10.setSummary(sharedPreferences.getString(DEF.KEY_SORT_PRIORITY_WORD_10, ""));
			}
		}

		@Override
		public void onPause() {
			super.onPause();
			SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
			if (sharedPreferences != null) {
				sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
			}
		}

		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
			if (key == null) return;
			if (key.equals(DEF.KEY_ROTATEBTN)) {
				if (mRotateBtn != null) mRotateBtn.setSummary(getRotateBtnSummary(sharedPreferences));
			} else if (key.equals(DEF.KEY_CHARSET)) {
				if (mCharset != null) mCharset.setSummary(getCharsetSummary(sharedPreferences));
			} else if (key.equals(DEF.KEY_SORT_PRIORITY_WORD_01)) {
				if (mPriorityWord01 != null) mPriorityWord01.setSummary(sharedPreferences.getString(DEF.KEY_SORT_PRIORITY_WORD_01, ""));
			} else if (key.equals(DEF.KEY_SORT_PRIORITY_WORD_02)) {
				if (mPriorityWord02 != null) mPriorityWord02.setSummary(sharedPreferences.getString(DEF.KEY_SORT_PRIORITY_WORD_02, ""));
			} else if (key.equals(DEF.KEY_SORT_PRIORITY_WORD_03)) {
				if (mPriorityWord03 != null) mPriorityWord03.setSummary(sharedPreferences.getString(DEF.KEY_SORT_PRIORITY_WORD_03, ""));
			} else if (key.equals(DEF.KEY_SORT_PRIORITY_WORD_04)) {
				if (mPriorityWord04 != null) mPriorityWord04.setSummary(sharedPreferences.getString(DEF.KEY_SORT_PRIORITY_WORD_04, ""));
			} else if (key.equals(DEF.KEY_SORT_PRIORITY_WORD_05)) {
				if (mPriorityWord05 != null) mPriorityWord05.setSummary(sharedPreferences.getString(DEF.KEY_SORT_PRIORITY_WORD_05, ""));
			} else if (key.equals(DEF.KEY_SORT_PRIORITY_WORD_06)) {
				if (mPriorityWord06 != null) mPriorityWord06.setSummary(sharedPreferences.getString(DEF.KEY_SORT_PRIORITY_WORD_06, ""));
			} else if (key.equals(DEF.KEY_SORT_PRIORITY_WORD_07)) {
				if (mPriorityWord07 != null) mPriorityWord07.setSummary(sharedPreferences.getString(DEF.KEY_SORT_PRIORITY_WORD_07, ""));
			} else if (key.equals(DEF.KEY_SORT_PRIORITY_WORD_08)) {
				if (mPriorityWord08 != null) mPriorityWord08.setSummary(sharedPreferences.getString(DEF.KEY_SORT_PRIORITY_WORD_08, ""));
			} else if (key.equals(DEF.KEY_SORT_PRIORITY_WORD_09)) {
				if (mPriorityWord09 != null) mPriorityWord09.setSummary(sharedPreferences.getString(DEF.KEY_SORT_PRIORITY_WORD_09, ""));
			} else if (key.equals(DEF.KEY_SORT_PRIORITY_WORD_10)) {
				if (mPriorityWord10 != null) mPriorityWord10.setSummary(sharedPreferences.getString(DEF.KEY_SORT_PRIORITY_WORD_10, ""));
			}
		}

		private String getRotateBtnSummary(SharedPreferences sharedPreferences){
			int val = getRotateBtn(sharedPreferences);
			Resources res = getResources();
			return res.getString(RotateBtnName[val]);
		}
	}

	// 設定の読込
	public static int getRotateBtn(SharedPreferences sharedPreferences){
		int val = DEF.getInt(sharedPreferences, DEF.KEY_ROTATEBTN, "1");
		if (val < 0 || val >= RotateBtnName.length){
			val = 0;
		}
		return val;
	}

	public static int getCharset(SharedPreferences sharedPreferences){
		int val = DEF.getInt(sharedPreferences, DEF.KEY_CHARSET, "1");
		if (val < 0 || val >= DEF.CharsetList.length){
			val = 1;
		}
		return val;
	}

	public static boolean getHiddenFile(SharedPreferences sharedPreferences){
		boolean flag;
		flag =  DEF.getBoolean(sharedPreferences, DEF.KEY_HIDDENFILE, true);
		return flag;
	}

	public static String getCharsetSummary(SharedPreferences sharedPreferences){
		int val = getCharset(sharedPreferences);
		return DEF.CharsetList[val];
	}

	public static void loadSettings(SharedPreferences sharedPreferences) {
		int logLevel = Logcat.LOG_LEVEL_WARN;
		DEF.CHAR_DETECT = sharedPreferences.getBoolean(DEF.KEY_CHAR_DETECT, true);
		DEF.CHARSET = getCharsetSummary(sharedPreferences);

		DEF.SORT_BY_IGNORE_WIDTH = sharedPreferences.getBoolean(DEF.KEY_SORT_BY_IGNORE_WIDTH, true);
		DEF.SORT_BY_IGNORE_CASE = sharedPreferences.getBoolean(DEF.KEY_SORT_BY_IGNORE_CASE, true);
		DEF.SORT_BY_SYMBOL = sharedPreferences.getBoolean(DEF.KEY_SORT_BY_SYMBOL, true);
		DEF.SORT_BY_NATURAL_NUMBERS = sharedPreferences.getBoolean(DEF.KEY_SORT_BY_NATURAL_NUMBERS, true);
		DEF.SORT_BY_KANJI_NUMERALS = sharedPreferences.getBoolean(DEF.KEY_SORT_BY_KANJI_NUMERALS, true);
		DEF.SORT_BY_JAPANESE_VOLUME_NAME = sharedPreferences.getBoolean(DEF.KEY_SORT_BY_JAPANESE_VOLUME_NAME, true);
		DEF.SORT_BY_FILE_TYPE = sharedPreferences.getBoolean(DEF.KEY_SORT_BY_FILE_TYPE, true);

		ArrayList<String> priorityWords = new ArrayList<String>();
		String word = "";
		word = sharedPreferences.getString(DEF.KEY_SORT_PRIORITY_WORD_01, "cover");
		if (word.length() > 0) {
			priorityWords.add(word);
		}
		word = sharedPreferences.getString(DEF.KEY_SORT_PRIORITY_WORD_02, "");
		if (word.length() > 0) {
			priorityWords.add(word);
		}
		word = sharedPreferences.getString(DEF.KEY_SORT_PRIORITY_WORD_03, "");
		if (word.length() > 0) {
			priorityWords.add(word);
		}
		word = sharedPreferences.getString(DEF.KEY_SORT_PRIORITY_WORD_04, "");
		if (word.length() > 0) {
			priorityWords.add(word);
		}
		word = sharedPreferences.getString(DEF.KEY_SORT_PRIORITY_WORD_05, "");
		if (word.length() > 0) {
			priorityWords.add(word);
		}
		word = sharedPreferences.getString(DEF.KEY_SORT_PRIORITY_WORD_06, "");
		if (word.length() > 0) {
			priorityWords.add(word);
		}
		word = sharedPreferences.getString(DEF.KEY_SORT_PRIORITY_WORD_07, "");
		if (word.length() > 0) {
			priorityWords.add(word);
		}
		word = sharedPreferences.getString(DEF.KEY_SORT_PRIORITY_WORD_08, "");
		if (word.length() > 0) {
			priorityWords.add(word);
		}
		word = sharedPreferences.getString(DEF.KEY_SORT_PRIORITY_WORD_09, "");
		if (word.length() > 0) {
			priorityWords.add(word);
		}
		word = sharedPreferences.getString(DEF.KEY_SORT_PRIORITY_WORD_10, "");
		if (word.length() > 0) {
			priorityWords.add(word);
		}
		DEF.PRIORITY_WORDS = priorityWords.toArray(new String[0]);

		Logcat.d(logLevel, "DEF.PRIORITY_WORDS=" + Arrays.toString(DEF.PRIORITY_WORDS));
	}
}

