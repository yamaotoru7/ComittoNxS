package src.comitton.config;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import jp.dip.muracoro.comittonx.R;
import src.comitton.common.DEF;
import src.comitton.config.color.ColorCntSetting;
import src.comitton.config.color.ColorGuiSetting;
import src.comitton.config.color.ColorHitSetting;
import src.comitton.config.color.ColorMgnSetting;
import src.comitton.config.color.ColorTvbSetting;
import src.comitton.config.color.ColorTvgSetting;
import src.comitton.config.color.ColorTvtSetting;
import src.comitton.config.color.ColorTxtCntSetting;
import src.comitton.config.color.ColorTxtGuiSetting;
import src.comitton.config.color.ColorTxtMgnSetting;
import src.comitton.helpview.HelpActivity;

public class SetImageTextColorActivity extends BasePreferenceActivity {

	public static final int[] GradationName =
		{ R.string.txgrad00		// グラデーションなし
		, R.string.txgrad01		// 左上→右下
		, R.string.txgrad02		// 上  →下
		, R.string.txgrad03		// 右上→左下
		, R.string.txgrad04		// 右  →左
		, R.string.txgrad05		// 右下→左下
		, R.string.txgrad06		// 下  →上
		, R.string.txgrad07		// 左下→右上
		, R.string.txgrad08 };	// 左  →右

	@Override
	protected PreferenceFragmentCompat createPreferenceFragment() {
		return new SetImageTextColorFragment();
	}

	public static class SetImageTextColorFragment extends PreferenceFragmentCompat implements OnSharedPreferenceChangeListener {
		private ColorMgnSetting mMgnColor;
		private ColorCntSetting mCntColor;
		private ColorGuiSetting mGuiColor;

		private ColorTxtMgnSetting mTxtMgnColor;
		private ColorTxtCntSetting mTxtCntColor;
		private ColorTxtGuiSetting mTxtGuiColor;

		private ColorTvtSetting mTvtColor;
		private ColorTvbSetting mTvbColor;
		private ColorTvgSetting mTvgColor;

		private ColorHitSetting mHitColor;

		private ListPreference mGradation;

		@Override
		public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
			setPreferencesFromResource(R.xml.imagetextcolor, rootKey);

			mMgnColor = findPreference(DEF.KEY_MGNRGB);
			mCntColor = findPreference(DEF.KEY_CNTRGB);
			mGuiColor = findPreference(DEF.KEY_GUIRGB);

			mTxtMgnColor = findPreference(DEF.KEY_TX_MGNRGB);
			mTxtCntColor = findPreference(DEF.KEY_TX_CNTRGB);
			mTxtGuiColor = findPreference(DEF.KEY_TX_GUIRGB);

			mTvtColor = findPreference(DEF.KEY_TX_TVTRGB);
			mTvbColor = findPreference(DEF.KEY_TX_TVBRGB);
			mTvgColor = findPreference(DEF.KEY_TX_TVGRGB);

			mHitColor = findPreference(DEF.KEY_TX_HITRGB);

			mGradation  = findPreference(DEF.KEY_TX_GRADATION);

			// 項目選択
			Preference onlineHelp = findPreference(DEF.KEY_ITCLRHELP);
			if (onlineHelp != null) {
				onlineHelp.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(@NonNull Preference preference) {
						if (getActivity() == null) {
							return false;
						}
						// Activityの遷移
						Resources res = getResources();
						String url = res.getString(R.string.url_imagetextcolor);	// 設定画面
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

				// 色設定
				if (mMgnColor != null) mMgnColor.setSummary(getColorSummary(getMgnColor(sharedPreferences)));
				if (mCntColor != null) mCntColor.setSummary(getColorSummary(getCntColor(sharedPreferences)));
				if (mGuiColor != null) mGuiColor.setSummary(getColorSummary(getGuiColor(sharedPreferences)));

				if (mTxtMgnColor != null) mTxtMgnColor.setSummary(getColorSummary(getTxtMgnColor(sharedPreferences)));
				if (mTxtCntColor != null) mTxtCntColor.setSummary(getColorSummary(getTxtCntColor(sharedPreferences)));
				if (mTxtGuiColor != null) mTxtGuiColor.setSummary(getColorSummary(getTxtGuiColor(sharedPreferences)));

				if (mTvtColor != null) mTvtColor.setSummary(getColorSummary(getTvtColor(sharedPreferences)));
				if (mTvbColor != null) mTvbColor.setSummary(getColorSummary(getTvbColor(sharedPreferences)));
				if (mTvgColor != null) mTvgColor.setSummary(getColorSummary(getTvgColor(sharedPreferences)));

				if (mHitColor != null) mHitColor.setSummary(getColorSummary(getHitColor(sharedPreferences)));

				if (mGradation != null) mGradation.setSummary(getGradationSummary(sharedPreferences));
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
			// 色
			if (key.equals(DEF.KEY_MGNRGB)) {
				if (mMgnColor != null) mMgnColor.setSummary(getColorSummary(getMgnColor(sharedPreferences)));
			} else if (key.equals(DEF.KEY_CNTRGB)) {
				if (mCntColor != null) mCntColor.setSummary(getColorSummary(getCntColor(sharedPreferences)));
			} else if (key.equals(DEF.KEY_GUIRGB)) {
				if (mGuiColor != null) mGuiColor.setSummary(getColorSummary(getGuiColor(sharedPreferences)));
			} else if (key.equals(DEF.KEY_TX_MGNRGB)) {
				if (mTxtMgnColor != null) mTxtMgnColor.setSummary(getColorSummary(getTxtMgnColor(sharedPreferences)));
			} else if (key.equals(DEF.KEY_TX_CNTRGB)) {
				if (mTxtCntColor != null) mTxtCntColor.setSummary(getColorSummary(getTxtCntColor(sharedPreferences)));
			} else if (key.equals(DEF.KEY_TX_GUIRGB)) {
				if (mTxtGuiColor != null) mTxtGuiColor.setSummary(getColorSummary(getTxtGuiColor(sharedPreferences)));
			} else if (key.equals(DEF.KEY_TX_TVTRGB)) {
				if (mTvtColor != null) mTvtColor.setSummary(getColorSummary(getTvtColor(sharedPreferences)));
			} else if (key.equals(DEF.KEY_TX_TVBRGB)) {
				if (mTvbColor != null) mTvbColor.setSummary(getColorSummary(getTvbColor(sharedPreferences)));
			} else if (key.equals(DEF.KEY_TX_TVGRGB)) {
				if (mTvgColor != null) mTvgColor.setSummary(getColorSummary(getTvgColor(sharedPreferences)));
			} else if (key.equals(DEF.KEY_TX_GRADATION)) {
				if (mGradation != null) mGradation.setSummary(getGradationSummary(sharedPreferences));
			} else if (key.equals(DEF.KEY_TX_HITRGB)) {
				if (mHitColor != null) mHitColor.setSummary(getColorSummary(getHitColor(sharedPreferences)));
			}
		}

		private String getGradationSummary(SharedPreferences sharedPreferences){
			int val = getGradation(sharedPreferences);
			Resources res = getResources();
			return res.getString(GradationName[val]);
		}

		private String getColorSummary(int val) {
			String[] str = { "Red", "Green", "Blue" };
			StringBuilder result = new StringBuilder();
			for (int i = 0; i < 3; i++) {
				int v = ((val >> 8 * (2 - i)) & 0x000000FF);
				result.append(str[i]).append("=").append(v);
				if (i != 2) {
					result.append(", ");
				}
			}
			result.append(String.format(" (%1$06X)", val & 0x00FFFFFF));
			return result.toString();
		}
	}

	// 設定の読込
	public static int getMgnColor(SharedPreferences sp){
		int val = DEF.getColorValue(sp, DEF.KEY_MGNCOLOR, DEF.KEY_MGNRGB, 22);
		return val;
	}

	// 中央余白の色
	public static int getCntColor(SharedPreferences sp){
		int val = DEF.getColorValue(sp, DEF.KEY_CNTCOLOR, DEF.KEY_CNTRGB, 22);
		return val;
	}

	public static int getGuiColor(SharedPreferences sp){
		int val = DEF.getGuideValue(sp, null, DEF.KEY_GUIRGB, 1);
		return val & 0x00FFFFFF | 0x80000000;
	}

	public static int getTxtMgnColor(SharedPreferences sp){
		int val = DEF.getColorValue(sp, null, DEF.KEY_TX_MGNRGB, 22);
		return val;
	}

	// 中央余白の色
	public static int getTxtCntColor(SharedPreferences sp){
		int val = DEF.getColorValue(sp, null, DEF.KEY_TX_CNTRGB, 22);
		return val;
	}

	public static int getTxtGuiColor(SharedPreferences sp){
		int val = DEF.getGuideValue(sp, null, DEF.KEY_TX_GUIRGB, 1);
		return val & 0x00FFFFFF | 0x80000000;
	}

	public static int getTvtColor(SharedPreferences sp){
		int val = sp.getInt(DEF.KEY_TX_TVTRGB, DEF.COLOR_TX_TVTRGB);
		return val;
	}

	// 背景１
	public static int getTvbColor(SharedPreferences sp){
		int val = sp.getInt(DEF.KEY_TX_TVBRGB, DEF.COLOR_TX_TVBRGB);
		return val;
	}

	// 背景２
	public static int getTvgColor(SharedPreferences sp){
		int val = sp.getInt(DEF.KEY_TX_TVGRGB, DEF.COLOR_TX_TVGRGB);
		return val;
	}

	// 検索ヒット
	public static int getHitColor(SharedPreferences sp){
		int val = sp.getInt(DEF.KEY_TX_HITRGB, DEF.COLOR_TX_HITRGB);
		return val;
	}

	// グラデーション有無
	public static int getGradation(SharedPreferences sharedPreferences){
		int val = DEF.getInt(sharedPreferences, DEF.KEY_TX_GRADATION, "1");
		return val;
	}
}

