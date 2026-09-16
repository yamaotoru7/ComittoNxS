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
import src.comitton.config.seekbar.AutoPlaySeekbar;
import src.comitton.config.seekbar.LongTapSeekbar;
import src.comitton.config.seekbar.ScalingSeekbar;
import src.comitton.config.seekbar.ScrlRngHSeekbar;
import src.comitton.config.seekbar.ScrlRngWSeekbar;
import src.comitton.config.seekbar.WAdjustSeekbar;
import src.comitton.config.seekbar.WScalingSeekbar;
import src.comitton.helpview.HelpActivity;

public class SetImageDetailActivity extends BasePreferenceActivity {

	@Override
	protected PreferenceFragmentCompat createPreferenceFragment() {
		return new SetImageDetailFragment();
	}

	public static class SetImageDetailFragment extends PreferenceFragmentCompat implements OnSharedPreferenceChangeListener {
		private WAdjustSeekbar mWAdjust;
		private WScalingSeekbar mWScaling;
		private ScalingSeekbar mScaling;
		private LongTapSeekbar mLongTap;
		private ScrlRngWSeekbar mScrlRngW;
		private ScrlRngHSeekbar mScrlRngH;
		private AutoPlaySeekbar mAutoPlay;
		private ListPreference mMaxThread;
		private ListPreference mLoupeSize;

		@Override
		public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
			setPreferencesFromResource(R.xml.imagedetail, rootKey);

			mLongTap   = findPreference(DEF.KEY_LONGTAP);
			mWAdjust   = findPreference(DEF.KEY_WADJUST);
			mWScaling   = findPreference(DEF.KEY_WSCALING);
			mScaling   = findPreference(DEF.KEY_SCALING);
			mScrlRngW  = findPreference(DEF.KEY_SCRLRNGW);
			mScrlRngH  = findPreference(DEF.KEY_SCRLRNGH);
			mAutoPlay = findPreference(DEF.KEY_AUTOPLAY);
			mMaxThread = findPreference(DEF.KEY_MAXTHREAD);
			mLoupeSize = findPreference(DEF.KEY_LOUPESIZE);

			// 項目選択
			Preference onlineHelp = findPreference(DEF.KEY_IDTLHELP);
			if (onlineHelp != null) {
				onlineHelp.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(@NonNull Preference preference) {
						if (getActivity() == null) {
							return false;
						}
						// Activityの遷移
						Resources res = getResources();
						String url = res.getString(R.string.url_imagedetail);	// 設定画面
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

				// シークバー
				if (mLongTap != null) mLongTap.setSummary(getLongTapSummary(sharedPreferences));
				if (mWAdjust != null) mWAdjust.setSummary(getWAdjustSummary(sharedPreferences));
				if (mWScaling != null) mWScaling.setSummary(getWScalingSummary(sharedPreferences));
				if (mScaling != null) mScaling.setSummary(getScalingSummary(sharedPreferences));
				if (mScrlRngW != null) mScrlRngW.setSummary(getScrlRngWSummary(sharedPreferences));
				if (mScrlRngH != null) mScrlRngH.setSummary(getScrlRngHSummary(sharedPreferences));
				if (mAutoPlay != null) mAutoPlay.setSummary(getAutoPlaySummary(sharedPreferences));
				if (mMaxThread != null) mMaxThread.setSummary(getMaxThreadSummary(sharedPreferences));
				if (mLoupeSize != null) mLoupeSize.setSummary(getLoupeSizeSummary(sharedPreferences));
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
			if (key.equals(DEF.KEY_LONGTAP)) {
				if (mLongTap != null) mLongTap.setSummary(getLongTapSummary(sharedPreferences));
			} else if (key.equals(DEF.KEY_WADJUST)) {
				if (mWAdjust != null) mWAdjust.setSummary(getWAdjustSummary(sharedPreferences));
			} else if (key.equals(DEF.KEY_WSCALING)) {
				if (mWScaling != null) mWScaling.setSummary(getWScalingSummary(sharedPreferences));
			} else if (key.equals(DEF.KEY_SCALING)) {
				if (mScaling != null) mScaling.setSummary(getScalingSummary(sharedPreferences));
			} else if (key.equals(DEF.KEY_SCRLRNGW)) {
				if (mScrlRngW != null) mScrlRngW.setSummary(getScrlRngWSummary(sharedPreferences));
			} else if (key.equals(DEF.KEY_SCRLRNGH)) {
				if (mScrlRngH != null) mScrlRngH.setSummary(getScrlRngHSummary(sharedPreferences));
			} else if (key.equals(DEF.KEY_AUTOPLAY)) {
				if (mAutoPlay != null) mAutoPlay.setSummary(getAutoPlaySummary(sharedPreferences));
			} else if (key.equals(DEF.KEY_MAXTHREAD)) {
				if (mMaxThread != null) mMaxThread.setSummary(getMaxThreadSummary(sharedPreferences));
			} else if (key.equals(DEF.KEY_LOUPESIZE)) {
				if (mLoupeSize != null) mLoupeSize.setSummary(getLoupeSizeSummary(sharedPreferences));
			}
		}

		private String getLongTapSummary(SharedPreferences sharedPreferences){
			int val = getLongTap(sharedPreferences);
			Resources res = getResources();
			String summ1 = res.getString(R.string.msecSumm1);
			return DEF.getMSecStr200(val, summ1);
		}

		private String getWAdjustSummary(SharedPreferences sharedPreferences){
			int val = getWAdjust(sharedPreferences);
			Resources res = getResources();
			String summ1 = res.getString(R.string.aspcSumm1);
			String summ2 = res.getString(R.string.aspcSumm2);
			return DEF.getAdjustStr(val, summ1, summ2);
		}

		private String getWScalingSummary(SharedPreferences sharedPreferences){
			int val = getWScaling(sharedPreferences);
			Resources res = getResources();
			String summ1 = res.getString(R.string.scalSumm1);
			String summ2 = res.getString(R.string.scalSumm2);
			return DEF.getWScalingStr(val, summ1, summ2);
		}

		private String getScalingSummary(SharedPreferences sharedPreferences){
			int val = getScaling(sharedPreferences);
			Resources res = getResources();
			String summ1 = res.getString(R.string.scalSumm1);
			String summ2 = res.getString(R.string.scalSumm2);
			return DEF.getScalingStr(val, summ1, summ2);
		}

		private String getScrlRngWSummary(SharedPreferences sharedPreferences) {
			int val = getScrlRngW(sharedPreferences);
			Resources res = getResources();
			String summ1 = res.getString(R.string.srngSumm1);
			String summ2 = res.getString(R.string.srngSumm2);
			return DEF.getScrlRangeStr(val, summ1, summ2);
		}

		private String getScrlRngHSummary(SharedPreferences sharedPreferences) {
			int val = getScrlRngH(sharedPreferences);
			Resources res = getResources();
			String summ1 = res.getString(R.string.srngSumm1);
			String summ2 = res.getString(R.string.srngSumm2);
			return DEF.getScrlRangeStr(val, summ1, summ2);
		}

		private String getAutoPlaySummary(SharedPreferences sharedPreferences) {
			int val = getAutoPlay(sharedPreferences);
			Resources res = getResources();
			String summ1 = res.getString(R.string.msecSumm1);
			return DEF.getAutoPlayStr(val, summ1);
		}

		private String getMaxThreadSummary(SharedPreferences sharedPreferences){
			int val = getMaxThread(sharedPreferences);
			String str;
			Resources res = getResources();
			str = res.getString(MaxThread[val]);
			if (val == 0) {
				int threads = Runtime.getRuntime().availableProcessors();
				if (threads > 7) {
					threads = 7;
				}
				else if (threads <= 0) {
					threads = 1;
				}
				str += " (" + res.getString(MaxThread[threads]) + ")";
			}
			return str;
		}

		private String getLoupeSizeSummary(SharedPreferences sharedPreferences){
			int val = getLoupeSize(sharedPreferences);
			String str;
			Resources res = getResources();
			str = res.getString(LoupeSize[val]);
			return str;
		}
	}

	public static final int[] MaxThread =
		{ R.string.maxthread00		// 自動
		, R.string.maxthread01		// 1スレッド
		, R.string.maxthread02		// 2スレッド
		, R.string.maxthread03		// 3スレッド
		, R.string.maxthread04		// 4スレッド
		, R.string.maxthread05		// 5スレッド
		, R.string.maxthread06		// 6スレッド
		, R.string.maxthread07		// 7スレッド
		, R.string.maxthread08 };	// 8スレッド
	public static final int[] LoupeSize =
		{
			R.string.loupesize00,
			R.string.loupesize01,
			R.string.loupesize02
		};

	// 設定の読込
	public static int getLongTap(SharedPreferences sharedPreferences){
		int num;
		num = DEF.getInt(sharedPreferences, DEF.KEY_LONGTAP, DEF.DEFAULT_LONGTAP);
		return num;
	}

	public static int getWAdjust(SharedPreferences sharedPreferences){
		int num;
		num = DEF.getInt(sharedPreferences, DEF.KEY_WADJUST, DEF.DEFAULT_WADJUST);
		return num;
	}

	public static int getWScaling(SharedPreferences sharedPreferences){
		int num;
		num = DEF.getInt(sharedPreferences, DEF.KEY_WSCALING, DEF.DEFAULT_WSCALING);
		return num;
	}

	public static int getScaling(SharedPreferences sharedPreferences){
		int num;
		num = DEF.getInt(sharedPreferences, DEF.KEY_SCALING, DEF.DEFAULT_SCALING);
		return num;
	}

	public static int getScrlRngW(SharedPreferences sharedPreferences){
		int num;
		num = DEF.getInt(sharedPreferences, DEF.KEY_SCRLRNGW, DEF.DEFAULT_SCRLRNGW);
		return num;
	}

	public static int getScrlRngH(SharedPreferences sharedPreferences){
		int num;
		num = DEF.getInt(sharedPreferences, DEF.KEY_SCRLRNGH, DEF.DEFAULT_SCRLRNGH);
		return num;
	}

	public static int getAutoPlay(SharedPreferences sharedPreferences) {
		int num;
		num = DEF.getInt(sharedPreferences, DEF.KEY_AUTOPLAY, DEF.DEFAULT_AUTOPLAY);
		return num;
	}

	public static boolean getAccessLamp(SharedPreferences sharedPreferences){
		boolean flag;
		flag =  DEF.getBoolean(sharedPreferences, DEF.KEY_ACCESSLAMP, true);
		return flag;
	}

	public static int getMaxThread(SharedPreferences sharedPreferences){
		int val;
		val = DEF.getInt(sharedPreferences, DEF.KEY_MAXTHREAD, "2");
		if (val <= 0 || val >= MaxThread.length){
			val = 1;
		}
		return val;
	}

	public static int getLoupeSize(SharedPreferences sharedPreferences){
		int val;
		val = DEF.getInt(sharedPreferences, DEF.KEY_LOUPESIZE, "0");
		if (val < 0 || val >= LoupeSize.length){
			val = 0;
		}
		return val;
	}
}
