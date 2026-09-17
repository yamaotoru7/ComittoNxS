package src.comitton.config;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
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
import src.comitton.config.color.ColorAftSetting;
import src.comitton.config.color.ColorBakSetting;
import src.comitton.config.color.ColorBefSetting;
import src.comitton.config.color.ColorCurSetting;
import src.comitton.config.color.ColorDirSetting;
import src.comitton.config.color.ColorImgSetting;
import src.comitton.config.color.ColorInfSetting;
import src.comitton.config.color.ColorMrkSetting;
import src.comitton.config.color.ColorNowSetting;
import src.comitton.config.color.ColorTibSetting;
import src.comitton.config.color.ColorTitSetting;
import src.comitton.config.color.ColorTlbSetting;
import src.comitton.config.color.ColorTldSetting;
import src.comitton.config.color.ColorTxtSetting;

public class SetFileColorActivity extends BasePreferenceActivity {

	@Override
	protected PreferenceFragmentCompat createPreferenceFragment() {
		return new SetFileColorFragment();
	}

	public static class SetFileColorFragment extends PreferenceFragmentCompat implements OnSharedPreferenceChangeListener {
		private ListPreference mPreset;
		private Preference mCustomUpdate;

		private ColorTxtSetting mTxtColor;
		private ColorDirSetting mDirColor;
		private ColorBefSetting mBefColor;
		private ColorNowSetting mNowColor;
		private ColorAftSetting mAftColor;
		private ColorImgSetting mImgColor;
		private ColorInfSetting mInfColor;
		private ColorMrkSetting mMrkColor;
		private ColorBakSetting mBakColor;
		private ColorCurSetting mCurColor;

		private ColorTitSetting mTitColor;
		private ColorTibSetting mTibColor;
		private ColorTldSetting mTldColor;
		private ColorTlbSetting mTlbColor;

		private SharedPreferences mSharedPreferences;

		@Override
		public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
			setPreferencesFromResource(R.xml.filecolor, rootKey);

			mPreset = findPreference(DEF.KEY_PRESET);

			mTxtColor = findPreference(DEF.KEY_TXTRGB);
			mDirColor = findPreference(DEF.KEY_DIRRGB);
			mBefColor = findPreference(DEF.KEY_BEFRGB);
			mNowColor = findPreference(DEF.KEY_NOWRGB);
			mAftColor = findPreference(DEF.KEY_AFTRGB);
			mImgColor = findPreference(DEF.KEY_IMGRGB);
			mInfColor = findPreference(DEF.KEY_INFRGB);
			mMrkColor = findPreference(DEF.KEY_MRKRGB);
			mBakColor = findPreference(DEF.KEY_BAKRGB);
			mCurColor = findPreference(DEF.KEY_CURRGB);

			mTitColor = findPreference(DEF.KEY_TITRGB);
			mTibColor = findPreference(DEF.KEY_TIBRGB);
			mTldColor = findPreference(DEF.KEY_TLDRGB);
			mTlbColor = findPreference(DEF.KEY_TLBRGB);
		}

		@Override
		public void onResume() {
			super.onResume();
			mSharedPreferences = getPreferenceScreen().getSharedPreferences();
			if (mSharedPreferences != null) {
				mSharedPreferences.registerOnSharedPreferenceChangeListener(this);

				// 色設定
				int val = getPreset(mSharedPreferences);
				if (mPreset != null) mPreset.setSummary(getPresetSummary(val));
				setEnableViews(val == 0);

				updateSummarys();
			}
		}

		@Override
		public void onPause() {
			super.onPause();
			if (mSharedPreferences != null) {
				mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
			}
		}

		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
			if (key == null) return;
			// 色
			if (key.equals(DEF.KEY_PRESET)) {
				// プリセット選択
				int val = getPreset(sharedPreferences);
				if (mPreset != null) mPreset.setSummary(getPresetSummary(val));
				setEnableViews(val == 0);
			} else if (key.equals(DEF.KEY_TXTRGB)) {
				if (mTxtColor != null) mTxtColor.setSummary(getColorSummary(getTxtColor(sharedPreferences, true)));
			} else if (key.equals(DEF.KEY_DIRRGB)) {
				if (mDirColor != null) mDirColor.setSummary(getColorSummary(getDirColor(sharedPreferences, true)));
			} else if (key.equals(DEF.KEY_BEFRGB)) {
				if (mBefColor != null) mBefColor.setSummary(getColorSummary(getBefColor(sharedPreferences, true)));
			} else if (key.equals(DEF.KEY_NOWRGB)) {
				if (mNowColor != null) mNowColor.setSummary(getColorSummary(getNowColor(sharedPreferences, true)));
			} else if (key.equals(DEF.KEY_AFTRGB)) {
				if (mAftColor != null) mAftColor.setSummary(getColorSummary(getAftColor(sharedPreferences, true)));
			} else if (key.equals(DEF.KEY_IMGRGB)) {
				if (mImgColor != null) mImgColor.setSummary(getColorSummary(getImgColor(sharedPreferences, true)));
			} else if (key.equals(DEF.KEY_INFRGB)) {
				if (mInfColor != null) mInfColor.setSummary(getColorSummary(getInfColor(sharedPreferences, true)));
			} else if (key.equals(DEF.KEY_MRKRGB)) {
				if (mMrkColor != null) mMrkColor.setSummary(getColorSummary(getMrkColor(sharedPreferences, true)));
			} else if (key.equals(DEF.KEY_BAKRGB)) {
				if (mBakColor != null) mBakColor.setSummary(getColorSummary(getBakColor(sharedPreferences, true)));
			} else if (key.equals(DEF.KEY_CURRGB)) {
				if (mCurColor != null) mCurColor.setSummary(getColorSummary(getCurColor(sharedPreferences, true)));
			} else if (key.equals(DEF.KEY_TITRGB)) {
				if (mTitColor != null) mTitColor.setSummary(getColorSummary(getTitColor(sharedPreferences, true)));
			} else if (key.equals(DEF.KEY_TIBRGB)) {
				if (mTibColor != null) mTibColor.setSummary(getColorSummary(getTibColor(sharedPreferences, true)));
			} else if (key.equals(DEF.KEY_TLDRGB)) {
				if (mTldColor != null) mTldColor.setSummary(getColorSummary(getTldColor(sharedPreferences, true)));
			} else if (key.equals(DEF.KEY_TLBRGB)) {
				if (mTlbColor != null) mTlbColor.setSummary(getColorSummary(getTlbColor(sharedPreferences, true)));
			}
		}

		private void setEnableViews(boolean enable) {
			if (mCustomUpdate != null) mCustomUpdate.setEnabled(!enable);
			if (mTxtColor != null) mTxtColor.setEnabled(enable);
			if (mDirColor != null) mDirColor.setEnabled(enable);
			if (mBefColor != null) mBefColor.setEnabled(enable);
			if (mNowColor != null) mNowColor.setEnabled(enable);
			if (mAftColor != null) mAftColor.setEnabled(enable);
			if (mImgColor != null) mImgColor.setEnabled(enable);
			if (mInfColor != null) mInfColor.setEnabled(enable);
			if (mMrkColor != null) mMrkColor.setEnabled(enable);
			if (mBakColor != null) mBakColor.setEnabled(enable);
			if (mCurColor != null) mCurColor.setEnabled(enable);
			if (mTitColor != null) mTitColor.setEnabled(enable);
			if (mTibColor != null) mTibColor.setEnabled(enable);
			if (mTldColor != null) mTldColor.setEnabled(enable);
			if (mTlbColor != null) mTlbColor.setEnabled(enable);
		}

		private void updateSummarys() {
			if (mSharedPreferences == null) return;
			if (mTxtColor != null) mTxtColor.setSummary(getColorSummary(getTxtColor(mSharedPreferences, true)));
			if (mDirColor != null) mDirColor.setSummary(getColorSummary(getDirColor(mSharedPreferences, true)));
			if (mBefColor != null) mBefColor.setSummary(getColorSummary(getBefColor(mSharedPreferences, true)));
			if (mNowColor != null) mNowColor.setSummary(getColorSummary(getNowColor(mSharedPreferences, true)));
			if (mAftColor != null) mAftColor.setSummary(getColorSummary(getAftColor(mSharedPreferences, true)));
			if (mImgColor != null) mImgColor.setSummary(getColorSummary(getImgColor(mSharedPreferences, true)));
			if (mInfColor != null) mInfColor.setSummary(getColorSummary(getInfColor(mSharedPreferences, true)));
			if (mMrkColor != null) mMrkColor.setSummary(getColorSummary(getMrkColor(mSharedPreferences, true)));
			if (mBakColor != null) mBakColor.setSummary(getColorSummary(getBakColor(mSharedPreferences, true)));
			if (mCurColor != null) mCurColor.setSummary(getColorSummary(getCurColor(mSharedPreferences, true)));
			if (mTitColor != null) mTitColor.setSummary(getColorSummary(getTitColor(mSharedPreferences, true)));
			if (mTibColor != null) mTibColor.setSummary(getColorSummary(getTibColor(mSharedPreferences, true)));
			if (mTldColor != null) mTldColor.setSummary(getColorSummary(getTldColor(mSharedPreferences, true)));
			if (mTlbColor != null) mTlbColor.setSummary(getColorSummary(getTlbColor(mSharedPreferences, true)));
		}

		private String getPresetSummary(int val) {
			Resources res = getResources();
			return res.getString(mPresetName[val]);
		}

		private String getColorSummary(int color) {
			return String.format("#%06X", (0xFFFFFF & color));
		}
	}

	static final int[] mPresetName =
	{ R.string.preset00		// カスタム
	, R.string.preset01		// 標準（黒）
	, R.string.preset02		// 標準（白）
	, R.string.preset03		// 桜
	, R.string.preset04		// 藍
	, R.string.preset05		// 若葉
	, R.string.preset06		// 蜜柑
	, R.string.preset07 };	// 墨

	static final int PRESET_TXT = 0;
	static final int PRESET_DIR = 1;
	static final int PRESET_BEF = 2;
	static final int PRESET_NOW = 3;
	static final int PRESET_AFT = 4;
	static final int PRESET_IMG = 5;
	static final int PRESET_INF = 6;
	static final int PRESET_MRK = 7;
	static final int PRESET_BAK = 8;
	static final int PRESET_CUR = 9;
	static final int PRESET_TIT = 10;
	static final int PRESET_TIB = 11;
	static final int PRESET_TLD = 12;
	static final int PRESET_TLB = 13;

	static final int[][] mPresetColor =
	//   ----TXT----, ---DIR----, ---BEF----, ---NOW----, ---AFT----, ---IMG----, ---INF----, ---MRK----, ---BAK----, ---Cur----, ---TIT----, ---TIB----, ---TLD----, ---TLB----
	{
/*標*/	{ 0xFFFFFFFF, 0xFF00FF00, 0xFFFFFFFF, 0xFF00FFFF, 0xFF808080, 0xFFFFFF00, 0xFF9F9F9F, 0xFFFFFF00, 0xFF000000, 0xFF0080FF, 0xFFFFFFFF, 0xFF202020, 0xFF000000, 0xFF808080 },
/*黒*/ 	{ 0xFFFFFFFF, 0xFF00FF00, 0xFFFFFFFF, 0xFF00FFFF, 0xFF808080, 0xFFFFFF00, 0xFF9F9F9F, 0xFFC00000, 0xFF000000, 0xFF0040C0, 0xFFFFFFFF, 0xFF202020, 0xFF404040, 0xFFA0A0A0 },
/*白*/	{ 0xFF000000, 0xFF008039, 0xFF000000, 0xFF1C5593, 0xFF808080, 0xFFB17A25, 0xFF3B373A, 0xFFFFFF40, 0xFFF0F0F0, 0xFF00C0FF, 0xFF232323, 0xFF8A8A8A, 0xFFD0D0D0, 0xFF606060 },
/*桜*/	{ 0xFFDF1E6B, 0xFF5C9A00, 0xFFDF1E6B, 0xFFE99697, 0xFF968E8F, 0xFF8A67A7, 0xFFA3B0C1, 0xFFFFFF7E, 0xFFFFE5E9, 0xFF89CFFF, 0xFFFFEDF1, 0xFFC77E90, 0xFF64263A, 0xFFE7AABC },
/*藍*/	{ 0xFF00217A, 0xFF2D5155, 0xFF00237B, 0xFFD93D4A, 0xFF968E8F, 0xFF4B418C, 0xFF393FD3, 0xFFF1FF82, 0xFFE9EFFF, 0xFFF5B5CE, 0xFFFAF2FF, 0xFF000B4B, 0xFF111656, 0xFF889EB0 },
/*葉*/	{ 0xFF10832A, 0xFF354E83, 0xFF10832A, 0xFF16A686, 0xFF82A18A, 0xFF92B410, 0xFF228034, 0xFFFDFB9A, 0xFFF2FFF5, 0xFFA2DAFF, 0xFFFAFFF2, 0xFF165826, 0xFF153A10, 0xFFA2B99F }, // O
/*橙*/	{ 0xFF8E6216, 0xFFA98A34, 0xFF8E6216, 0xFFCC9E43, 0xFFA7966D, 0xFFB07028, 0xFF88661B, 0xFFFFCEE3, 0xFFFFFAF2, 0xFF8DFFBB, 0xFFFFFEF3, 0xFFC36214, 0xFF663B10, 0xFFDDA268 }, // O
/*墨*/	{ 0xFFDEDEE1, 0xFF2DA6C8, 0xFFDEDED1, 0xFF76AFC3, 0xFF4A4A4B, 0xFFC5C123, 0xFF9F9F9F, 0xFF004D24, 0xFF282828, 0xFF1F4594, 0xFFFFFFFF, 0xFF202020, 0xFF3A3A3F, 0xFF6F767C }
	};



	// 設定の読込（リストビュー）
	public static int getPreset(SharedPreferences sp) {
		int val = Integer.parseInt(sp.getString(DEF.KEY_PRESET, "1"));
		return val;
	}

//
//	public static int getTitleColor(SharedPreferences sp) {
//		int val = Integer.parseInt(sp.getString(DEF.KEY_TITLECLR, "0"));
//		return val;
//	}
//
//	public static int getToolbarColor(SharedPreferences sp) {
//		int val = Integer.parseInt(sp.getString(DEF.KEY_TOOLBCLR, "0"));
//		return val;
//	}

	// 設定の読込（Activityからのアクセス）
	public static int getTxtColor(SharedPreferences sp) {
		return getTxtColor(sp, false);
	}

	public static int getDirColor(SharedPreferences sp) {
		return getDirColor(sp, false);
	}

	public static int getBefColor(SharedPreferences sp) {
		return getBefColor(sp, false);
	}

	public static int getNowColor(SharedPreferences sp) {
		return getNowColor(sp, false);
	}

	public static int getAftColor(SharedPreferences sp) {
		return getAftColor(sp, false);
	}

	public static int getImgColor(SharedPreferences sp) {
		return getImgColor(sp, false);
	}

	public static int getInfColor(SharedPreferences sp) {
		return getInfColor(sp, false);
	}

	public static int getMrkColor(SharedPreferences sp) {
		return getMrkColor(sp, false);
	}

	public static int getBakColor(SharedPreferences sp) {
		return getBakColor(sp, false);
	}

	public static int getCurColor(SharedPreferences sp) {
		return getCurColor(sp, false);
	}

	public static int getTitColor(SharedPreferences sp) {
		return getTitColor(sp, false);
	}

	public static int getTibColor(SharedPreferences sp) {
		return getTibColor(sp, false);
	}

	public static int getTldColor(SharedPreferences sp) {
		return getTldColor(sp, false);
	}

	public static int getTlbColor(SharedPreferences sp) {
		return getTlbColor(sp, false);
	}

	// 設定の読込（スライダー）
	public static int getTxtColor(SharedPreferences sp, boolean summary) {
		return getColor(sp, DEF.KEY_TXTCOLOR, DEF.KEY_TXTRGB, PRESET_TXT, 1, summary);
	}

	public static int getDirColor(SharedPreferences sp, boolean summary) {
		return getColor(sp, DEF.KEY_DIRCOLOR, DEF.KEY_DIRRGB, PRESET_DIR, 12, summary);
	}

	public static int getBefColor(SharedPreferences sp, boolean summary) {
		int val = getColor(sp, DEF.KEY_BEFCOLOR, DEF.KEY_BEFRGB, PRESET_BEF, 1, summary);
		return val;
	}

	public static int getNowColor(SharedPreferences sp, boolean summary) {
		int val = getColor(sp, DEF.KEY_NOWCOLOR, DEF.KEY_NOWRGB, PRESET_NOW, 7, summary);
		return val;
	}

	public static int getAftColor(SharedPreferences sp, boolean summary) {
		int val = getColor(sp, DEF.KEY_AFTCOLOR, DEF.KEY_AFTRGB, PRESET_AFT, 8, summary);
		return val;
	}

	public static int getImgColor(SharedPreferences sp, boolean summary) {
		int val = getColor(sp, DEF.KEY_IMGCOLOR, DEF.KEY_IMGRGB, PRESET_IMG, 13, summary);
		return val;
	}

	public static int getInfColor(SharedPreferences sp, boolean summary) {
		int val = getColor(sp, DEF.KEY_INFCOLOR, DEF.KEY_INFRGB, PRESET_INF, 15, summary);
		return val;
	}

	public static int getMrkColor(SharedPreferences sp, boolean summary) {
		int val = getColor(sp, null, DEF.KEY_MRKRGB, PRESET_MRK, 20, summary);
		return val;
	}

	public static int getBakColor(SharedPreferences sp, boolean summary) {
		int val = getColor(sp, DEF.KEY_BAKCOLOR, DEF.KEY_BAKRGB, PRESET_BAK, 0, summary);
		return val;
	}

	public static int getCurColor(SharedPreferences sp, boolean summary) {
		int val = getColor(sp, null, DEF.KEY_CURRGB, PRESET_CUR, 9, summary);
		return val;
	}

	public static int getTitColor(SharedPreferences sp, boolean summary) {
		int val = getColor(sp, null, DEF.KEY_TITRGB, PRESET_TIT, 1, summary);
		return val;
	}

	public static int getTibColor(SharedPreferences sp, boolean summary) {
		int val = getColor(sp, null, DEF.KEY_TIBRGB, PRESET_TIB, 8, summary);
		return val;
	}

	public static int getTldColor(SharedPreferences sp, boolean summary) {
		int val = getColor(sp, null, DEF.KEY_TLDRGB, PRESET_TLD, 0, summary);
		return val;
	}

	public static int getTlbColor(SharedPreferences sp, boolean summary) {
		int val = getColor(sp, null, DEF.KEY_TLBRGB, PRESET_TLB, 15, summary);
		return val;
	}

	private static int getColor(SharedPreferences sp, String keyColor, String keyRGB, int index, int def, boolean summary) {
		int preset = getPreset(sp);
		int val;

		if (preset == 0 || summary) {
			val = DEF.getColorValue(sp, keyColor, keyRGB, def);
		}
		else {
			val = mPresetColor[preset][index];
		}
		return val;
	}

	private String getPresetSummary(int val) {
		Resources res = getResources();
		return res.getString(mPresetName[val]);
	}

	private String getColorSummary(int val) {
		String[] str = { "Red", "Green", "Blue" };
		String result = "";
		for (int i = 0; i < 3; i++) {
			int v = ((val >> 8 * (2 - i)) & 0x000000FF);
			result += str[i] + "=" + v;
			if (i != 2) {
				result += ", ";
			}
		}
		result += String.format(" (%1$06X)", val & 0x00FFFFFF);
		return result;
	}
}
