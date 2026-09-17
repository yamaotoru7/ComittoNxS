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
import src.comitton.config.seekbar.MemNextSeekbar;
import src.comitton.config.seekbar.MemPrevSeekbar;
import src.comitton.config.seekbar.MemSizeSeekbar;

public class SetCacheActivity extends BasePreferenceActivity {

	@Override
	protected PreferenceFragmentCompat createPreferenceFragment() {
		return new SetCacheFragment();
	}

	public static class SetCacheFragment extends PreferenceFragmentCompat implements OnSharedPreferenceChangeListener {
		private MemSizeSeekbar mMemSize;
		private MemNextSeekbar mMemNext;
		private MemPrevSeekbar mMemPrev;

		@Override
		public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
			setPreferencesFromResource(R.xml.cache, rootKey);

			mMemSize  = findPreference(DEF.KEY_MEMSIZE);
			mMemNext  = findPreference(DEF.KEY_MEMNEXT);
			mMemPrev  = findPreference(DEF.KEY_MEMPREV);
		}

		@Override
		public void onResume() {
			super.onResume();
			SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
			if (sharedPreferences != null) {
				sharedPreferences.registerOnSharedPreferenceChangeListener(this);

				// シークバー
				if (mMemSize != null) mMemSize.setSummary(getMemSizeSummary(sharedPreferences));
				if (mMemNext != null) mMemNext.setSummary(getMemNextSummary(sharedPreferences));
				if (mMemPrev != null) mMemPrev.setSummary(getMemPrevSummary(sharedPreferences));
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
			if (key.equals(DEF.KEY_MEMSIZE)) {
				if (mMemSize != null) mMemSize.setSummary(getMemSizeSummary(sharedPreferences));
			} else if (key.equals(DEF.KEY_MEMNEXT)) {
				if (mMemNext != null) mMemNext.setSummary(getMemNextSummary(sharedPreferences));
			} else if (key.equals(DEF.KEY_MEMPREV)) {
				if (mMemPrev != null) mMemPrev.setSummary(getMemPrevSummary(sharedPreferences));
			}
		}

		private String getMemSizeSummary(SharedPreferences sharedPreferences){
			int val = getMemSize(sharedPreferences);
			Resources res = getResources();
			String summ1 = res.getString(R.string.mSizeSumm1);
			String summ2 = res.getString(R.string.mSizeSumm2);
			return DEF.getMemSizeStr(val, summ1, summ2);
		}

		private String getMemNextSummary(SharedPreferences sharedPreferences){
			int val = getMemNext(sharedPreferences);
			Resources res = getResources();
			String summ1 = res.getString(R.string.mPageSumm1);
			return DEF.getMemPageStr(val, summ1);
		}

		private String getMemPrevSummary(SharedPreferences sharedPreferences){
			int val = getMemPrev(sharedPreferences);
			Resources res = getResources();
			String summ1 = res.getString(R.string.mPageSumm1);
			return DEF.getMemPageStr(val, summ1);
		}
	}

	// 設定の読込
	public static int getMemSize(SharedPreferences sharedPreferences){
		int num =  DEF.getInt(sharedPreferences, DEF.KEY_MEMSIZE, DEF.DEFAULT_MEMSIZE);
		return num;
	}

	public static int getMemNext(SharedPreferences sharedPreferences){
		int num =  DEF.getInt(sharedPreferences, DEF.KEY_MEMNEXT, DEF.DEFAULT_MEMNEXT);
		return num;
	}

	public static int getMemPrev(SharedPreferences sharedPreferences){
		int num =  DEF.getInt(sharedPreferences, DEF.KEY_MEMPREV, DEF.DEFAULT_MEMPREV);
		return num;
	}
}

