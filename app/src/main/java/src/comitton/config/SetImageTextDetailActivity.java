package src.comitton.config;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import jp.dip.muracoro.comittonx.R;
import src.comitton.common.DEF;
import src.comitton.config.seekbar.CenterSeekbar;
import src.comitton.config.seekbar.ClickAreaSeekbar;
import src.comitton.config.seekbar.EffectTimeSeekbar;
import src.comitton.config.seekbar.GradationSeekbar;
import src.comitton.config.seekbar.MarginSeekbar;
import src.comitton.config.seekbar.MomentModeSeekbar;
import src.comitton.config.seekbar.PageRangeSeekbar;
import src.comitton.config.seekbar.ScrollSeekbar;
import src.comitton.config.seekbar.TapRangeSeekbar;
import src.comitton.config.seekbar.VolScrlSeekbar;

public class SetImageTextDetailActivity extends BasePreferenceActivity {

	@Override
	protected PreferenceFragmentCompat createPreferenceFragment() {
		return new SetImageTextDetailFragment();
	}

	public static class SetImageTextDetailFragment extends PreferenceFragmentCompat implements OnSharedPreferenceChangeListener {
		private ScrollSeekbar mScroll;
		private ClickAreaSeekbar mClickArea;
		private PageRangeSeekbar mPageRange;
		private TapRangeSeekbar mTapRange;
		private MarginSeekbar mMargin;
		private CenterSeekbar mCenter;
		private GradationSeekbar mGradation;
		private VolScrlSeekbar mVolScrl;
		private EffectTimeSeekbar mEffectTime;
		private MomentModeSeekbar mMomentMode;

		@Override
		public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
			setPreferencesFromResource(R.xml.imagetextdetail, rootKey);

			mScroll = findPreference(DEF.KEY_SCROLL);
			mClickArea = findPreference(DEF.KEY_CLICKAREA);
			mPageRange = findPreference(DEF.KEY_PAGERANGE);
			mTapRange = findPreference(DEF.KEY_TAPRANGE);
			mMargin = findPreference(DEF.KEY_MARGIN);
			mCenter = findPreference(DEF.KEY_CENTER);
			mGradation = findPreference(DEF.KEY_GRADATION);
			mVolScrl = findPreference(DEF.KEY_VOLSCRL);
			mEffectTime = findPreference(DEF.KEY_EFFECTTIME);
			mMomentMode = findPreference(DEF.KEY_MOMENTMODE);
		}

		@Override
		public void onResume() {
			super.onResume();
			SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
			if (sharedPreferences != null) {
				sharedPreferences.registerOnSharedPreferenceChangeListener(this);

				// シークバー
				if (mScroll != null) mScroll.setSummary(getScrollSummary(sharedPreferences));
				if (mClickArea != null) mClickArea.setSummary(getClickAreaSummary(sharedPreferences));
				if (mPageRange != null) mPageRange.setSummary(getPageRangeSummary(sharedPreferences));
				if (mTapRange != null) mTapRange.setSummary(getTapRangeSummary(sharedPreferences));
				if (mMargin != null) mMargin.setSummary(getMarginSummary(sharedPreferences));
				if (mCenter != null) mCenter.setSummary(getCenterSummary(sharedPreferences));
				if (mGradation != null) mGradation.setSummary(getGradationSummary(sharedPreferences));
				if (mVolScrl != null) mVolScrl.setSummary(getVolScrlSummary(sharedPreferences));
				if (mEffectTime != null) mEffectTime.setSummary(getEffectTimeSummary(sharedPreferences));
				if (mMomentMode != null) mMomentMode.setSummary(getMomentModeSummary(sharedPreferences));
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
			// シークバー
			if (key.equals(DEF.KEY_SCROLL)) {
				if (mScroll != null) mScroll.setSummary(getScrollSummary(sharedPreferences));
			} else if (key.equals(DEF.KEY_CLICKAREA)) {
				if (mClickArea != null) mClickArea.setSummary(getClickAreaSummary(sharedPreferences));
			} else if (key.equals(DEF.KEY_PAGERANGE)) {
				if (mPageRange != null) mPageRange.setSummary(getPageRangeSummary(sharedPreferences));
			} else if (key.equals(DEF.KEY_TAPRANGE)) {
				if (mTapRange != null) mTapRange.setSummary(getTapRangeSummary(sharedPreferences));
			} else if (key.equals(DEF.KEY_MARGIN)) {
				if (mMargin != null) mMargin.setSummary(getMarginSummary(sharedPreferences));
			} else if (key.equals(DEF.KEY_CENTER)) {
				if (mCenter != null) mCenter.setSummary(getCenterSummary(sharedPreferences));
			} else if (key.equals(DEF.KEY_GRADATION)) {
				if (mGradation != null) mGradation.setSummary(getGradationSummary(sharedPreferences));
			} else if (key.equals(DEF.KEY_VOLSCRL)) {
				if (mVolScrl != null) mVolScrl.setSummary(getVolScrlSummary(sharedPreferences));
			} else if (key.equals(DEF.KEY_EFFECTTIME)) {
				if (mEffectTime != null) mEffectTime.setSummary(getEffectTimeSummary(sharedPreferences));
			} else if (key.equals(DEF.KEY_MOMENTMODE)) {
				if (mMomentMode != null) mMomentMode.setSummary(getMomentModeSummary(sharedPreferences));
			}
		}

		private String getScrollSummary(SharedPreferences sharedPreferences) {
			int val = getScroll(sharedPreferences);
			Resources res = getResources();
			String summ1 = res.getString(R.string.scrlSumm1);
			String summ2 = res.getString(R.string.scrlSumm2);
			return DEF.getScrollStr(val, summ1, summ2);
		}

		private String getClickAreaSummary(SharedPreferences sharedPreferences) {
			int val = getClickArea(sharedPreferences);
			Resources res = getResources();
			String summ1 = res.getString(R.string.unitSumm1);
			return DEF.getClickAreaStr(val, summ1);
		}

		private String getPageRangeSummary(SharedPreferences sharedPreferences) {
			int val = getPageRange(sharedPreferences);
			Resources res = getResources();
			String summ1 = res.getString(R.string.unitSumm1);
			return DEF.getPageRangeStr(val, summ1);
		}

		private String getTapRangeSummary(SharedPreferences sharedPreferences) {
			int val = getTapRange(sharedPreferences);
			Resources res = getResources();
			String summ1 = res.getString(R.string.unitSumm1);
			return DEF.getTapRangeStr(val, summ1);
		}

		private String getMarginSummary(SharedPreferences sharedPreferences) {
			int val = getMargin(sharedPreferences);
			Resources res = getResources();
			String summ1 = res.getString(R.string.rangeSumm1);
			return DEF.getMarginStr(val, summ1);
		}

		private String getCenterSummary(SharedPreferences sharedPreferences) {
			int val = getCenter(sharedPreferences);
			Resources res = getResources();
			String summ1 = res.getString(R.string.centSumm1);
			return DEF.getCenterStr(val, summ1);
		}

		private String getGradationSummary(SharedPreferences sharedPreferences) {
			int val = getGradation(sharedPreferences);
			return DEF.getGradationStr(val);
		}

		private String getVolScrlSummary(SharedPreferences sharedPreferences) {
			int val = getVolScrl(sharedPreferences);
			Resources res = getResources();
			String summ1 = res.getString(R.string.unitSumm1);
			return DEF.getScrlSpeedStr(val, summ1);
		}

		private String getEffectTimeSummary(SharedPreferences sharedPreferences) {
			int val = getEffectTime(sharedPreferences);
			Resources res = getResources();
			String summ1 = res.getString(R.string.msecSumm1);
			return DEF.getEffectTimeStr(val, summ1);
		}

		private String getMomentModeSummary(SharedPreferences sharedPreferences) {
			int val = getMomentMode(sharedPreferences);
			Resources res = getResources();
			String summ1 = res.getString(R.string.mmentSumm1);
			String summ2 = res.getString(R.string.mmentSumm2);
			return DEF.getMomentModeStr(val, summ1, summ2);
		}
	}

	// 設定の読込
	public static boolean getImmEnable(SharedPreferences sharedPreferences){
		return DEF.getBoolean(sharedPreferences, DEF.KEY_IMMENABLE, false);
	}

	public static boolean getOldMenu(SharedPreferences sharedPreferences){
		return DEF.getBoolean(sharedPreferences, DEF.KEY_OLDMENU, false);
	}

	public static boolean getPinchEnable(SharedPreferences sharedPreferences){
		return DEF.getBoolean(sharedPreferences, DEF.KEY_PINCHENABLE, true);
	}

	public static boolean getBottomFile(SharedPreferences sharedPreferences){
		return DEF.getBoolean(sharedPreferences, DEF.KEY_BOTTOMFILE, true);
	}

	public static int getScroll(SharedPreferences sharedPreferences) {
		int num = DEF.getInt(sharedPreferences, DEF.KEY_SCROLL, DEF.DEFAULT_SCROLL);
		return num;
	}

	public static int getClickArea(SharedPreferences sharedPreferences) {
		int num;
		num = DEF.getInt(sharedPreferences, DEF.KEY_CLICKAREA, DEF.DEFAULT_CLICKAREA);
		return num;
	}

	public static int getPageRange(SharedPreferences sharedPreferences) {
		int num;
		num = DEF.getInt(sharedPreferences, DEF.KEY_PAGERANGE, DEF.DEFAULT_PAGERANGE);
		return num;
	}

	public static int getTapRange(SharedPreferences sharedPreferences) {
		int num;
		num = DEF.getInt(sharedPreferences, DEF.KEY_TAPRANGE, DEF.DEFAULT_TAPRANGE);
		return num;
	}

	public static int getMargin(SharedPreferences sharedPreferences) {
		int num;
		num = DEF.getInt(sharedPreferences, DEF.KEY_MARGIN, DEF.DEFAULT_MARGIN);
		return num;
	}

	public static int getCenter(SharedPreferences sharedPreferences) {
		int num;
		num = DEF.getInt(sharedPreferences, DEF.KEY_CENTER, DEF.DEFAULT_CENTER);
		return num;
	}

	public static int getGradation(SharedPreferences sharedPreferences) {
		int num;
		num = DEF.getInt(sharedPreferences, DEF.KEY_GRADATION, DEF.DEFAULT_GRADATION);
		return num;
	}

	public static int getVolScrl(SharedPreferences sharedPreferences) {
		int num;
		num = DEF.getInt(sharedPreferences, DEF.KEY_VOLSCRL, DEF.DEFAULT_VOLSCRL);
		return num;
	}

	public static int getEffectTime(SharedPreferences sharedPreferences) {
		int num;
		num = DEF.getInt(sharedPreferences, DEF.KEY_EFFECTTIME, DEF.DEFAULT_EFFECTTIME);
		return num;
	}

	public static int getMomentMode(SharedPreferences sharedPreferences) {
		int num;
		num = DEF.getInt(sharedPreferences, DEF.KEY_MOMENTMODE, DEF.DEFAULT_MOMENTMODE);
		return num;
	}
}
