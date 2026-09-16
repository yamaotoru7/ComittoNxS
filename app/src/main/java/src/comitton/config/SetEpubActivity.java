package src.comitton.config;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceFragmentCompat;

import jp.dip.muracoro.comittonx.R;
import src.comitton.common.DEF;

public class SetEpubActivity extends BasePreferenceActivity {

	@Override
	protected PreferenceFragmentCompat createPreferenceFragment() {
		return new SetEpubFragment();
	}

	public static class SetEpubFragment extends PreferenceFragmentCompat {
		@Override
		public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
			setPreferencesFromResource(R.xml.epub, rootKey);
		}
	}

	// 設定の読込
	public static boolean getViewer(SharedPreferences sharedPreferences){
		boolean flag;
		flag =  DEF.getBoolean(sharedPreferences, DEF.KEY_EP_VIEWER, false);
		return flag;
	}

	// 設定の読込
	public static boolean getEpubOrder(SharedPreferences sharedPreferences){
		boolean flag;
		flag =  DEF.getBoolean(sharedPreferences, DEF.KEY_EP_ORDER, true);
		return flag;
	}

	// 設定の読込
	public static boolean getEpubThumb(SharedPreferences sharedPreferences){
		boolean flag;
		flag =  DEF.getBoolean(sharedPreferences, DEF.KEY_EP_THUMB, true);
		return flag;
	}
}

