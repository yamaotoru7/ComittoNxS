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
import src.comitton.fileview.filelist.RecordList;
import src.comitton.helpview.HelpActivity;

public class SetRecorderActivity extends BasePreferenceActivity {

	public static final int[] HistNumName =
		{ R.string.histnum00	// 保存しない
		, R.string.histnum01	// 20
		, R.string.histnum02	// 40
		, R.string.histnum03	// 60
		, R.string.histnum04	// 80
		, R.string.histnum05 };	// 100

	@Override
	protected PreferenceFragmentCompat createPreferenceFragment() {
		return new SetRecorderFragment();
	}

	public static class SetRecorderFragment extends PreferenceFragmentCompat implements OnSharedPreferenceChangeListener {
		private ListPreference mHistNum;

		@Override
		public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
			setPreferencesFromResource(R.xml.recorder, rootKey);
			mHistNum = findPreference(DEF.KEY_HISTNUM);

			// 項目選択
			Preference onlineHelp = findPreference(DEF.KEY_RECHELP);
			if (onlineHelp != null) {
				onlineHelp.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(@NonNull Preference preference) {
						if (getActivity() == null) {
							return false;
						}
						// Activityの遷移
						Resources res = getResources();
						String url = res.getString(R.string.url_recordlist);	// 設定画面
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
				if (mHistNum != null) {
					mHistNum.setSummary(getHistNumSummary(sharedPreferences));
				}
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
			if (key != null && key.equals(DEF.KEY_HISTNUM)) {
				if (mHistNum != null) {
					mHistNum.setSummary(getHistNumSummary(sharedPreferences));
				}
			}
		}

		private String getHistNumSummary(SharedPreferences sharedPreferences){
			int val = getHistNum(sharedPreferences);
			Resources res = getResources();
			return res.getString(HistNumName[val]);
		}
	}

	// 設定の読込
	public static int getHistNum(SharedPreferences sharedPreferences){
		int val = DEF.getInt(sharedPreferences, DEF.KEY_HISTNUM, "1");
		if (val < 0 || val >= HistNumName.length){
			val = 1;
		}
		return val;
	}

	public static boolean getShowSelector(SharedPreferences sharedPreferences){
		boolean flag;
		flag =  DEF.getBoolean(sharedPreferences, DEF.KEY_SHOWSELECTOR, true);
		return flag;
	}

	public static boolean getDirectoryView(SharedPreferences sharedPreferences){
		boolean flag;
		flag =  DEF.getBoolean(sharedPreferences, DEF.KEY_RDIRVIEW, true);
		return flag;
	}

	public static boolean getServerView(SharedPreferences sharedPreferences){
		boolean flag;
		flag =  true;
		return flag;
	}

	public static boolean getBookmarkView(SharedPreferences sharedPreferences){
		boolean flag;
		flag =  DEF.getBoolean(sharedPreferences, DEF.KEY_RBMVIEW, true);
		return flag;
	}

	public static boolean getHistoryView(SharedPreferences sharedPreferences){
		boolean flag;
		flag =  DEF.getBoolean(sharedPreferences, DEF.KEY_RHISTVIEW, true);
		return flag;
	}

	public static boolean getMenuView(SharedPreferences sharedPreferences){
		boolean flag;
		flag =  true;
		return flag;
	}

	public static boolean getRecLocal(SharedPreferences sharedPreferences){
		boolean flag;
		flag =  DEF.getBoolean(sharedPreferences, DEF.KEY_RECLOCAL, true);
		return flag;
	}

	public static boolean getRecServer(SharedPreferences sharedPreferences){
		boolean flag;
		flag =  DEF.getBoolean(sharedPreferences, DEF.KEY_RECSAMBA, true);
		return flag;
	}


	// 表示するリストを返す
	public static short[] getListTypes(SharedPreferences sharedPreferences) {
		boolean[] listflag = {false, false, false, false, false};
		int listnum = 0;
		listflag[0] = getDirectoryView(sharedPreferences);
		listflag[1] = getServerView(sharedPreferences);
		listflag[2] = getBookmarkView(sharedPreferences);
		listflag[3] = getHistoryView(sharedPreferences);
		listflag[4] = getMenuView(sharedPreferences);
		for (int i = 0 ; i < listflag.length ; i ++) {
			listnum += listflag[i] ? 1 : 0;
		}

		short[] listtype = new short[listnum + 1];
		int index = 1;
		listtype[0] = RecordList.TYPE_FILELIST;
		for (int i = 0 ; i < listflag.length ; i ++) {
			if (listflag[i]) {
				// 表示する場合
				listtype[index] = (short)i;
				index ++;
			}
		}
		return listtype;
	}
}
