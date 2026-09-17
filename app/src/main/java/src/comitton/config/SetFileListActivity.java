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
import src.comitton.config.seekbar.FontMainSeekbar;
import src.comitton.config.seekbar.FontSubSeekbar;
import src.comitton.config.seekbar.FontTileSeekbar;
import src.comitton.config.seekbar.FontTitleSeekbar;
import src.comitton.config.seekbar.ItemMarginSeekbar;
import src.comitton.config.seekbar.ListThumbSeekbar;
import src.comitton.config.seekbar.MenuLongTapSeekbar;
import src.comitton.config.seekbar.ToolbarSeekbar;

public class SetFileListActivity extends BasePreferenceActivity {

	@Override
	protected PreferenceFragmentCompat createPreferenceFragment() {
		return new SetFileListFragment();
	}

	public static class SetFileListFragment extends PreferenceFragmentCompat implements OnSharedPreferenceChangeListener {
		private ListPreference mListRota;
		private ListPreference mListSort;
		private ListPreference mBackMode;

		private ListPreference mMaxLines;
		private ListPreference mFileDelMenu;
		private ListPreference mFileRenMenu;

		private FontTitleSeekbar mFontTitle;
		private FontMainSeekbar mFontMain;
		private FontSubSeekbar mFontSub;
		private FontTileSeekbar mFontTile;
		private ItemMarginSeekbar mItemMrgn;
		private MenuLongTapSeekbar mMenuLongTap;

		private ListPreference mThumbCache;
		private ListPreference mThumbSortType;
		private ListPreference mThumbCrop;
		private ListPreference mThumbMargin;

		private ToolbarSeekbar mToolbarSeek;
		private ListThumbSeekbar mListThumbSeek;

		private ThumbnailPreference mThumbnail;

		@Override
		public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
			setPreferencesFromResource(R.xml.filelist, rootKey);

			mListRota  = findPreference(DEF.KEY_LISTROTA);
			mListSort  = findPreference(DEF.KEY_LISTSORT);
			mBackMode  = findPreference(DEF.KEY_BACKMODE);
			mMaxLines = findPreference(DEF.KEY_MAX_LINES);
			mFileDelMenu = findPreference(DEF.KEY_FILEDELMENU);
			mFileRenMenu = findPreference(DEF.KEY_FILERENMENU);
			mFontTitle = findPreference(DEF.KEY_FONTTITLE);
			mFontMain  = findPreference(DEF.KEY_FONTMAIN);
			mFontSub   = findPreference(DEF.KEY_FONTSUB);
			mFontTile  = findPreference(DEF.KEY_FONTTILE);
			mItemMrgn  = findPreference(DEF.KEY_ITEMMRGN);
			mThumbnail = findPreference(DEF.KEY_THUMBSEEK);
			mThumbCache = findPreference(DEF.KEY_THUMBCACHE);
			mThumbSortType = findPreference(DEF.KEY_THUMBSORTTYPE);
			mThumbCrop = findPreference(DEF.KEY_THUMBCROP);
			mThumbMargin = findPreference(DEF.KEY_THUMBMARGIN);
			mToolbarSeek = findPreference(DEF.KEY_TOOLBARSEEK);
			mListThumbSeek = findPreference(DEF.KEY_LISTTHUMBSEEK);
			mMenuLongTap   = findPreference(DEF.KEY_MENULONGTAP);
		}

		@Override
		public void onResume() {
			super.onResume();
			SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
			if (sharedPreferences != null) {
				sharedPreferences.registerOnSharedPreferenceChangeListener(this);

				if (mListRota != null) mListRota.setSummary(getListRotaSummary(sharedPreferences));
				if (mListSort != null) mListSort.setSummary(getListSortSummary(sharedPreferences));
				if (mBackMode != null) mBackMode.setSummary(getBackModeSummary(sharedPreferences));
				if (mMaxLines != null) mMaxLines.setSummary(getMaxLinesSummary(sharedPreferences));
				if (mFileDelMenu != null) mFileDelMenu.setSummary(getFileDelMenuSummary(sharedPreferences));
				if (mFileRenMenu != null) mFileRenMenu.setSummary(getFileRenMenuSummary(sharedPreferences));
				if (mFontTitle != null) mFontTitle.setSummary(getFontTitleSummary(sharedPreferences));
				if (mFontMain != null) mFontMain.setSummary(getFontMainSummary(sharedPreferences));
				if (mFontSub != null) mFontSub.setSummary(getFontSubSummary(sharedPreferences));
				if (mFontTile != null) mFontTile.setSummary(getFontTileSummary(sharedPreferences));
				if (mItemMrgn != null) mItemMrgn.setSummary(getItemMarginSummary(sharedPreferences));
				if (mThumbnail != null) mThumbnail.setSummary(getThumbnailSummary(sharedPreferences));
				if (mThumbCache != null) mThumbCache.setSummary(getThumbCacheSummary(sharedPreferences));
				if (mThumbSortType != null) mThumbSortType.setSummary(getThumbSortTypeSummary(sharedPreferences));
				if (mThumbCrop != null) mThumbCrop.setSummary(getThumbCropSummary(sharedPreferences));
				if (mThumbMargin != null) mThumbMargin.setSummary(getThumbMarginSummary(sharedPreferences));
				if (mToolbarSeek != null) mToolbarSeek.setSummary(getToolbarSeekSummary(sharedPreferences));
				if (mListThumbSeek != null) mListThumbSeek.setSummary(getListThumbSeekSummary(sharedPreferences));
				if (mMenuLongTap != null) mMenuLongTap.setSummary(getMenuLongTapSummary(sharedPreferences));
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
			if (key.equals(DEF.KEY_LISTROTA)) {
				if (mListRota != null) mListRota.setSummary(getListRotaSummary(sharedPreferences));
			} else if (key.equals(DEF.KEY_LISTSORT)) {
				if (mListSort != null) mListSort.setSummary(getListSortSummary(sharedPreferences));
			} else if (key.equals(DEF.KEY_FONTTITLE)) {
				if (mFontTitle != null) mFontTitle.setSummary(getFontTitleSummary(sharedPreferences));
			} else if (key.equals(DEF.KEY_FONTMAIN)) {
				if (mFontMain != null) mFontMain.setSummary(getFontMainSummary(sharedPreferences));
			} else if (key.equals(DEF.KEY_FONTSUB)) {
				if (mFontSub != null) mFontSub.setSummary(getFontSubSummary(sharedPreferences));
			} else if (key.equals(DEF.KEY_FONTTILE)) {
				if (mFontTile != null) mFontTile.setSummary(getFontTileSummary(sharedPreferences));
			} else if (key.equals(DEF.KEY_ITEMMRGN)) {
				if (mItemMrgn != null) mItemMrgn.setSummary(getItemMarginSummary(sharedPreferences));
			} else if (key.equals(DEF.KEY_BACKMODE)) {
				if (mBackMode != null) mBackMode.setSummary(getBackModeSummary(sharedPreferences));
			} else if (key.equals(DEF.KEY_THUMBSIZEW) || key.equals(DEF.KEY_THUMBSIZEH)) {
				if (mThumbnail != null) mThumbnail.setSummary(getThumbnailSummary(sharedPreferences));
			} else if (key.equals(DEF.KEY_THUMBCACHE)) {
				if (mThumbCache != null) mThumbCache.setSummary(getThumbCacheSummary(sharedPreferences));
			} else if (key.equals(DEF.KEY_THUMBSORTTYPE)) {
				if (mThumbSortType != null) mThumbSortType.setSummary(getThumbSortTypeSummary(sharedPreferences));
			} else if (key.equals(DEF.KEY_THUMBCROP)) {
				if (mThumbCrop != null) mThumbCrop.setSummary(getThumbCropSummary(sharedPreferences));
			} else if (key.equals(DEF.KEY_THUMBMARGIN)) {
				if (mThumbMargin != null) mThumbMargin.setSummary(getThumbMarginSummary(sharedPreferences));
			} else if (key.equals(DEF.KEY_TOOLBARSEEK)) {
				if (mToolbarSeek != null) mToolbarSeek.setSummary(getToolbarSeekSummary(sharedPreferences));
			} else if (key.equals(DEF.KEY_LISTTHUMBSEEK)) {
				if (mListThumbSeek != null) mListThumbSeek.setSummary(getListThumbSeekSummary(sharedPreferences));
			} else if (key.equals(DEF.KEY_MAX_LINES)) {
				if (mMaxLines != null) mMaxLines.setSummary(getMaxLinesSummary(sharedPreferences));
			} else if (key.equals(DEF.KEY_FILEDELMENU)) {
				if (mFileDelMenu != null) mFileDelMenu.setSummary(getFileDelMenuSummary(sharedPreferences));
			} else if (key.equals(DEF.KEY_FILERENMENU)) {
				if (mFileRenMenu != null) mFileRenMenu.setSummary(getFileRenMenuSummary(sharedPreferences));
			} else if (key.equals(DEF.KEY_MENULONGTAP)) {
				if (mMenuLongTap != null) mMenuLongTap.setSummary(getMenuLongTapSummary(sharedPreferences));
			}
		}

		private String getListRotaSummary(SharedPreferences sharedPreferences){
			int val = getListRota(sharedPreferences);
			Resources res = getResources();
			return res.getString(RotateName[val]);
		}

		private String getListSortSummary(SharedPreferences sharedPreferences){
			int val = getListSort(sharedPreferences);
			Resources res = getResources();
			return res.getString(ListSortName[val]);
		}

		private String getMaxLinesSummary(SharedPreferences sharedPreferences){
			int val = getMaxLines(sharedPreferences);
			return String.valueOf(val);
		}

		private String getFileDelMenuSummary(SharedPreferences sharedPreferences){
			int val = getFileDelMenu(sharedPreferences);
			Resources res = getResources();
			return res.getString(ShowMenu[val]);
		}

		private String getFileRenMenuSummary(SharedPreferences sharedPreferences){
			int val = getFileRenMenu(sharedPreferences);
			Resources res = getResources();
			return res.getString(ShowMenu[val]);
		}

		private String getFontTitleSummary(SharedPreferences sharedPreferences){
			int val = getFontTitle(sharedPreferences);
			Resources res = getResources();
			String summ1 = res.getString(R.string.unitSumm1);
			return DEF.getFontSpStr(val, summ1);
		}

		private String getFontMainSummary(SharedPreferences sharedPreferences){
			int val = getFontMain(sharedPreferences);
			Resources res = getResources();
			String summ1 = res.getString(R.string.unitSumm1);
			return DEF.getFontSpStr(val, summ1);
		}

		private String getFontSubSummary(SharedPreferences sharedPreferences){
			int val = getFontSub(sharedPreferences);
			Resources res = getResources();
			String summ1 = res.getString(R.string.unitSumm1);
			return DEF.getFontSpStr(val, summ1);
		}

		private String getFontTileSummary(SharedPreferences sharedPreferences){
			int val = getFontTile(sharedPreferences);
			Resources res = getResources();
			String summ1 = res.getString(R.string.unitSumm1);
			return DEF.getFontSpStr(val, summ1);
		}

		private String getItemMarginSummary(SharedPreferences sharedPreferences){
			int val = getItemMargin(sharedPreferences);
			Resources res = getResources();
			String summ1 = res.getString(R.string.unitSumm1);
			return DEF.getMarginSpStr(val, summ1);
		}

		private String getBackModeSummary(SharedPreferences sharedPreferences){
			int val = getBackMode(sharedPreferences);
			Resources res = getResources();
			return res.getString(BackModeName[val]);
		}

		private String getThumbnailSummary(SharedPreferences sharedPreferences){
			int w = getThumbSizeW(sharedPreferences);
			int h = getThumbSizeH(sharedPreferences);
			Resources res = getResources();
			String summ1 = res.getString(R.string.rangeSumm1);
			return DEF.getThumbnailStr(w, h, summ1);
		}

		private String getToolbarSeekSummary(SharedPreferences sharedPreferences){
			int size = getToolbarSize(sharedPreferences);
			Resources res = getResources();
			String summ1 = res.getString(R.string.unitSumm1);
			return DEF.getToolbarSeekStr(size, summ1);
		}

		private String getListThumbSeekSummary(SharedPreferences sharedPreferences){
			int size = getListThumbSizeH(sharedPreferences);
			Resources res = getResources();
			String summ1 = res.getString(R.string.rangeSumm1);
			return DEF.getListThumbSeekStr(size, summ1);
		}

		private String getThumbCacheSummary(SharedPreferences sharedPreferences){
			int val = getThumbCache(sharedPreferences);
			Resources res = getResources();
			return res.getString(ThumCacheName[val]);
		}

		private String getThumbSortTypeSummary(SharedPreferences sharedPreferences){
			int val = getThumbSortType(sharedPreferences);
			Resources res = getResources();
			return res.getString(ThumSortTypeName[val]);
		}

		private String getThumbCropSummary(SharedPreferences sharedPreferences){
			int val = getThumbCrop(sharedPreferences);
			Resources res = getResources();
			return res.getString(ThumCropName[val]);
		}

		private String getThumbMarginSummary(SharedPreferences sharedPreferences){
			int val = getThumbMargin(sharedPreferences);
			Resources res = getResources();
			return res.getString(ThumMarginName[val]);
		}

		private String getMenuLongTapSummary(SharedPreferences sharedPreferences){
			int val = getMenuLongTap(sharedPreferences);
			Resources res = getResources();
			String summ1 = res.getString(R.string.msecSumm1);
			return DEF.getMSecStr100(val, summ1);
		}
	}

 	public static final int[] ListSortName =
		{ R.string.lsort00		// ソートなし
		, R.string.lsort01		// ファイル名順(ディレクトリ混在)
		, R.string.lsort02		// ファイル名順(ディレクトリ分離)
		, R.string.lsort03		// 新しい順(ディレクトリ混在)
		, R.string.lsort04		// 新しい順(ディレクトリ分離)
		, R.string.lsort05		// 古い順(ディレクトリ混在)
		, R.string.lsort06 };	// 古い順(ディレクトリ分離)
	public static final int[] FileSortName =
		{ R.string.fsort00		// ソートなし
		, R.string.fsort01		// ファイル名昇順
		, R.string.fsort02 };	// ファイル名降順
	public static final int[] RotateName =
		{ R.string.rota00		// 回転あり
		, R.string.rota01		// 縦固定
		, R.string.rota02 };	// 横固定
	public static final int[] BackModeName =
		{ R.string.bkmode00		// アプリ終了
		, R.string.bkmode01		// 親ディレクトリ
		, R.string.bkmode02 };	// 遷移を戻る
	public static final int[] ShowMenu =
		{ R.string.showmenu00		// 表示しない
		, R.string.showmenu01		// 表示する
		, R.string.showmenu02		// ローカルのみ表示
		, R.string.showmenu03 };	// サーバのみ表示
	public static final int[] ThumCacheName =
		{ R.string.thumbcache00		// 保持しない
		, R.string.thumbcache01		// 100まで
		, R.string.thumbcache02		// 500まで
		, R.string.thumbcache03		// 1000まで
		, R.string.thumbcache04 };	// 手動で削除
	public static final int[] ThumSortTypeName =
		{ R.string.thumbsorttype00		// なし
		, R.string.thumbsorttype01		// ローカル
		, R.string.thumbsorttype02		// ローカルとSMB
		, R.string.thumbsorttype03 };	// すべて
	public static final int[] ThumCropName =
			{ R.string.thumbcrop00		// 中央
			, R.string.thumbcrop01		// 左
			, R.string.thumbcrop02		// 右
			, R.string.thumbcrop03	// 幅に合わせる
			, R.string.thumbcrop04 };	// 縦の表紙カバーに合わせる
	public static final int[] ThumMarginName =
			{ R.string.mgncut00		// なし
			, R.string.mgncut01		// 弱
			, R.string.mgncut02		// 中
			, R.string.mgncut03		// 強
			, R.string.mgncut04		// 特上
			, R.string.mgncut05 };		// 最強

	// 設定の読込
	// フォントサイズ(px)
	public static int getListRota(SharedPreferences sharedPreferences){
		int val = DEF.getInt(sharedPreferences, DEF.KEY_LISTROTA, "0");
		if( val < 0 || val > 2 ){
			val = 0;
		}
		return val;
	}

	public static int getListSort(SharedPreferences sharedPreferences){
		int val = DEF.getInt(sharedPreferences, DEF.KEY_LISTSORT, "2");
		if(val < 0 || val > ListSortName.length){
			val = 0;
		}
		return val;
	}

	public static int getMaxLines(SharedPreferences sharedPreferences){
		int val = DEF.getInt(sharedPreferences, DEF.KEY_MAX_LINES, "2");
		if(val <= 0 || val > 10){
			val = 2;
		}
		return val;
	}

	public static int getFileDelMenu(SharedPreferences sharedPreferences){
		int val = DEF.getInt(sharedPreferences, DEF.KEY_FILEDELMENU, "2");
		if(val < 0 || val >= ShowMenu.length){
			val = 2;
		}
		return val;
	}

	public static int getFileRenMenu(SharedPreferences sharedPreferences){
		int val = DEF.getInt(sharedPreferences, DEF.KEY_FILERENMENU, "0");
		if(val < 0 || val >= ShowMenu.length){
			val = 0;
		}
		return val;
	}

	public static int getFontTitle(SharedPreferences sharedPreferences){
		int num =  DEF.getInt(sharedPreferences, DEF.KEY_FONTTITLE, DEF.DEFAULT_FONTTITLE);
		return num;
	}

	public static int getFontMain(SharedPreferences sharedPreferences){
		int num =  DEF.getInt(sharedPreferences, DEF.KEY_FONTMAIN, DEF.DEFAULT_FONTMAIN);
		return num;
	}

	public static int getFontSub(SharedPreferences sharedPreferences){
		int num =  DEF.getInt(sharedPreferences, DEF.KEY_FONTSUB, DEF.DEFAULT_FONTSUB);
		return num;
	}

	public static int getFontTile(SharedPreferences sharedPreferences){
		int num =  DEF.getInt(sharedPreferences, DEF.KEY_FONTTILE, DEF.DEFAULT_FONTTILE);
		return num;
	}

	public static int getItemMargin(SharedPreferences sharedPreferences){
		int num =  DEF.getInt(sharedPreferences, DEF.KEY_ITEMMRGN, DEF.DEFAULT_ITEMMARGIN);
		return num;
	}

	public static int getThumbSizeW(SharedPreferences sharedPreferences){
		int num = DEF.getInt(sharedPreferences, DEF.KEY_THUMBSIZEW, DEF.DEFAULT_THUMBSIZEW);
		return num;
	}

	public static int getThumbSizeH(SharedPreferences sharedPreferences){
		int num = DEF.getInt(sharedPreferences, DEF.KEY_THUMBSIZEH, DEF.DEFAULT_THUMBSIZEH);
		return num;
	}

	public static int getToolbarSize(SharedPreferences sharedPreferences){
		int val = DEF.getInt(sharedPreferences, DEF.KEY_TOOLBARSEEK, DEF.DEFAULT_TOOLBARSEEK);
		return val;
	}
	public static int getListThumbSizeH(SharedPreferences sharedPreferences){
		int val = DEF.getInt(sharedPreferences, DEF.KEY_LISTTHUMBSEEK, DEF.DEFAULT_LISTTHUMBSIZEH);
		return val;
	}


//	public static int getFontSize(SharedPreferences sharedPreferences){
//		int val = DEF.getInt(sharedPreferences, DEF.KEY_FONTSIZE, "2"));
//		if( val < 0 || val > 4 ){
//			val = 2;
//		}
//		return val;
//	}

	public static int getViewRota(SharedPreferences sharedPreferences){
		int val = DEF.getInt(sharedPreferences, DEF.KEY_VIEWROTA, "0");
		if( val < 0 || val > 3 ){
			val = 0;
		}
		return val;
	}

	public static int getFileSort(SharedPreferences sharedPreferences){
		int val = DEF.getInt(sharedPreferences, DEF.KEY_FILESORT, "1");
		if( val < 0 || val > 2 ){
			val = 0;
		}
		return val;
	}

	public static int getViewPt(SharedPreferences sharedPreferences){
		int val = DEF.getInt(sharedPreferences, DEF.KEY_VIEWPT, "0");
		if( val < 0 || val > 4 ){
			val = 0;
		}
		return val;
	}

	public static int getIniScale(SharedPreferences sharedPreferences){
		int val = DEF.getInt(sharedPreferences, DEF.KEY_INISCALE, DEF.DEFAULT_INISCHALE);
		if( val < 0 || val > 7 ){
			val = 5;
		}
		return val;
	}

	public static int getPageWay(SharedPreferences sharedPreferences){
		int val = DEF.getInt(sharedPreferences, DEF.KEY_PAGEWAY, "0");
		if( val < 0 || val > 2 ){
			val = 0;
		}
		return val;
	}

	public static int getInitView(SharedPreferences sharedPreferences){
		int val = DEF.getInt(sharedPreferences, DEF.KEY_INITVIEW, DEF.DEFAULT_INITVIEW);
		if( val < 0 || val > 3 ){
			val = 0;
		}
		return val;
	}

	public static int getBackMode(SharedPreferences sharedPreferences){
		int val = DEF.getInt(sharedPreferences, DEF.KEY_BACKMODE, "0");
		if (val < 0 || val > BackModeName.length){
			val = 0;
		}
		return val;
	}

	public static int getThumbCache(SharedPreferences sharedPreferences){
		int val = DEF.getInt(sharedPreferences, DEF.KEY_THUMBCACHE, "2");
		if (val < 0 || val >= ThumCacheName.length){
			val = 2;
		}
		return val;
	}

	public static int getThumbSortType(SharedPreferences sharedPreferences){
		int val = DEF.getInt(sharedPreferences, DEF.KEY_THUMBSORTTYPE, "2");
		if (val < 0 || val >= ThumSortTypeName.length){
			val = 2;
		}
		return val;
	}

	public static int getThumbCrop(SharedPreferences sharedPreferences){
		int val = DEF.getInt(sharedPreferences, DEF.KEY_THUMBCROP, "0");
		if (val < 0 || val >= ThumCropName.length){
			val = 0;
		}
		return val;
	}

	public static int getThumbMargin(SharedPreferences sharedPreferences){
		int val = DEF.getInt(sharedPreferences, DEF.KEY_THUMBMARGIN, "0");
		if (val < 0 || val >= ThumMarginName.length){
			val = 0;
		}
		return val;
	}

	public static int getThumbCacheNum(SharedPreferences sharedPreferences){
		int val = getThumbCache(sharedPreferences);
		int num = 500;
		switch (val) {
			case 0:
				num = 0;
				break;
			case 1:
				num = 100;
				break;
			case 2:
				num = 500;
				break;
			case 3:
				num = 1000;
				break;
			case 4:
				num = -1;
				break;
		}
		return num;
	}

	public static boolean getTapExpand(SharedPreferences sharedPreferences){
		boolean flag;
		flag = DEF.getBoolean(sharedPreferences, DEF.KEY_TAPEXPAND, false);
		return flag;
	}

	public static boolean getEpubViewer(SharedPreferences sharedPreferences){
		boolean flag;
		flag =  DEF.getBoolean(sharedPreferences, DEF.KEY_EP_VIEWER, false);
		return flag;
	}

	public static boolean getThumbnail(SharedPreferences sharedPreferences){
		boolean flag;
		flag =  DEF.getBoolean(sharedPreferences, DEF.KEY_THUMBNAIL, false);
		return flag;
	}

	public static boolean getCrearTop(SharedPreferences sharedPreferences){
		boolean flag;
		flag =  DEF.getBoolean(sharedPreferences, DEF.KEY_CLEARTOP, false);
		return flag;
	}

	public static boolean getExtension(SharedPreferences sharedPreferences){
		boolean flag;
		flag =  DEF.getBoolean(sharedPreferences, DEF.KEY_EXTENSION, true);
		return flag;
	}

	public static boolean getSplitFilename(SharedPreferences sharedPreferences){
		boolean flag;
		flag =  DEF.getBoolean(sharedPreferences, DEF.KEY_SPLIT_FILENAME, true);
		return flag;
	}

	public static boolean getThumbnailSort(SharedPreferences sharedPreferences){
		boolean flag;
		flag =  DEF.getBoolean(sharedPreferences, DEF.KEY_THUMBSORT, false);
		return flag;
	}

	public static boolean getParentMove(SharedPreferences sharedPreferences){
		boolean flag;
		flag =  DEF.getBoolean(sharedPreferences, DEF.KEY_PARENTMOVE, true);
		return flag;
	}

	public static boolean getShowToolbar(SharedPreferences sharedPreferences){
		boolean flag;
		flag =  DEF.getBoolean(sharedPreferences, DEF.KEY_SHOWTOOLBAR, true);
		return flag;
	}

	public static boolean getToolbarName(SharedPreferences sharedPreferences){
		boolean flag;
		flag =  DEF.getBoolean(sharedPreferences, DEF.KEY_TOOLBARNAME, true);
		return flag;
	}

	public static boolean getThumbnailTap(SharedPreferences sharedPreferences){
		boolean flag;
		flag =  DEF.getBoolean(sharedPreferences, DEF.KEY_THUMBNAILTAP, true);
		return flag;
	}

	public static int getMenuLongTap(SharedPreferences sharedPreferences){
		int num;
		num = DEF.getInt(sharedPreferences, DEF.KEY_MENULONGTAP, DEF.DEFAULT_MENULONGTAP);
		return num;
	}

	// 設定を保存
	public static void setThumbnail(SharedPreferences sharedPreferences, boolean value){
		Editor ed = sharedPreferences.edit();
	}
}
