package src.comitton.config;

import java.io.File;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import jp.dip.muracoro.comittonx.R;
import src.comitton.common.DEF;
import src.comitton.config.seekbar.TextFontBodySeekbar;
import src.comitton.config.seekbar.TextFontInfoSeekbar;
import src.comitton.config.seekbar.TextFontRubiSeekbar;
import src.comitton.config.seekbar.TextFontTopSeekbar;
import src.comitton.config.seekbar.TextMarginHSeekbar;
import src.comitton.config.seekbar.TextMarginWSeekbar;
import src.comitton.config.seekbar.TextScrlRngHSeekbar;
import src.comitton.config.seekbar.TextScrlRngWSeekbar;
import src.comitton.config.seekbar.TextSpaceHSeekbar;
import src.comitton.config.seekbar.TextSpaceWSeekbar;
import src.comitton.helpview.HelpActivity;

public class SetTextActivity extends BasePreferenceActivity {

	@Override
	protected PreferenceFragmentCompat createPreferenceFragment() {
		return new SetTextFragment();
	}

	public static class SetTextFragment extends PreferenceFragmentCompat implements OnSharedPreferenceChangeListener {
		private TextFontTopSeekbar mFontTop;
		private TextFontBodySeekbar mFontBody;
		private TextFontRubiSeekbar mFontRubi;
		private TextFontInfoSeekbar mFontInfo;
		private TextSpaceWSeekbar mSpaceW;
		private TextSpaceHSeekbar mSpaceH;
		private TextMarginWSeekbar mMarginW;
		private TextMarginHSeekbar mMarginH;

		private TextScrlRngWSeekbar mScrlRngW;
		private TextScrlRngHSeekbar mScrlRngH;

		private ListPreference mViewRota;
		private ListPreference mIniScale;
		private ListPreference mInitView;
		private ListPreference mPaper;
		private ListPreference mPicSize;

		private ListPreference	mFontName;
		private ListPreference mViewPt;
		private ListPreference mVolKey;
		private ListPreference mLastPage;
		private ListPreference mPageSel;

		private OperationPreference mTapPattern;
		private TimeAndBatteryPreference mTimeAndBattery;

		private Resources mResources;

		@Override
		public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
			setPreferencesFromResource(R.xml.text, rootKey);

			mFontTop  = findPreference(DEF.KEY_TX_FONTTOP);
			mFontBody = findPreference(DEF.KEY_TX_FONTBODY);
			mFontRubi = findPreference(DEF.KEY_TX_FONTRUBI);
			mFontInfo = findPreference(DEF.KEY_TX_FONTINFO);
			mSpaceW   = findPreference(DEF.KEY_TX_SPACEW);
			mSpaceH   = findPreference(DEF.KEY_TX_SPACEH);
			mMarginW  = findPreference(DEF.KEY_TX_MARGINW);
			mMarginH  = findPreference(DEF.KEY_TX_MARGINH);

			mIniScale = findPreference(DEF.KEY_TX_INISCALE);
			mInitView = findPreference(DEF.KEY_TX_INITVIEW);
			mViewRota = findPreference(DEF.KEY_TX_VIEWROTA);
			mPaper    = findPreference(DEF.KEY_TX_PAPER);
			mPicSize  = findPreference(DEF.KEY_TX_PICSIZE);

			mScrlRngW = findPreference(DEF.KEY_TX_SCRLRNGW);
			mScrlRngH = findPreference(DEF.KEY_TX_SCRLRNGH);

			mFontName = findPreference(DEF.KEY_TX_FONTNAME);
			if (mFontName != null) {
				mFontName.setTitle(mFontName.getTitle().toString().replace("[sdcard]", "[" + Environment.getExternalStorageDirectory().getAbsolutePath() + "]"));
			}

			mTapPattern = findPreference(DEF.KEY_TAPPATTERN);

			mViewPt = findPreference(DEF.KEY_VIEWPT);
			mVolKey = findPreference(DEF.KEY_VOLKEY);

			mLastPage = findPreference(DEF.KEY_LASTPAGE);
			mPageSel = findPreference(DEF.KEY_TX_PAGESELECT);

			mTimeAndBattery = findPreference(DEF.KEY_TIMEANDBATTERY);

			mResources = getResources();

			String fontpath = DEF.getFontDirectory();
			CharSequence[] items;
			CharSequence[] values;

			File[] files = new File(fontpath).listFiles();
			if (files == null) {
				items = new CharSequence[1];
				values = new CharSequence[1];
			} else {
				int i = 1;
				for (File file : files) {
					if (file != null && file.isFile()) {
						i ++;
					}
				}
				items = new CharSequence[i];
				values = new CharSequence[i];

				i = 1;
				for (File file : files) {
					if (file != null && file.isFile()) {
						if (i < items.length) {
							items[i] = file.getName();
							values[i] = file.getName();
							i ++;
						}
					}
				}
			}

			Resources res = getResources();
			items[0] = res.getString(R.string.defaultFont);
			values[0] = "";

			if (mFontName != null) {
				mFontName.setEntries(items);
				mFontName.setEntryValues(values);
				mFontName.setDefaultValue(values[0]);
			}

			// 項目選択
			Preference onlineHelp = findPreference(DEF.KEY_TEXTHELP);
			if (onlineHelp != null) {
				onlineHelp.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(@NonNull Preference preference) {
						if (getActivity() == null) {
							return false;
						}
						// Activityの遷移
						Resources res = getResources();
						String url = res.getString(R.string.url_text);	// 設定画面
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

				if (mFontTop != null) mFontTop.setSummary(getFontTopSummary(sharedPreferences));
				if (mFontBody != null) mFontBody.setSummary(getFontBodySummary(sharedPreferences));
				if (mFontRubi != null) mFontRubi.setSummary(getFontRubiSummary(sharedPreferences));
				if (mFontInfo != null) mFontInfo.setSummary(getFontInfoSummary(sharedPreferences));
				if (mSpaceW != null) mSpaceW.setSummary(getSpaceWSummary(sharedPreferences));
				if (mSpaceH != null) mSpaceH.setSummary(getSpaceHSummary(sharedPreferences));
				if (mMarginW != null) mMarginW.setSummary(getMarginWSummary(sharedPreferences));
				if (mMarginH != null) mMarginH.setSummary(getMarginHSummary(sharedPreferences));

				if (mIniScale != null) mIniScale.setSummary(getIniScaleSummary(sharedPreferences));
				if (mInitView != null) mInitView.setSummary(getInitViewSummary(sharedPreferences));
				if (mViewRota != null) mViewRota.setSummary(getViewRotaSummary(sharedPreferences));
				if (mPaper != null) mPaper.setSummary(getPaperSummary(sharedPreferences));
				if (mPicSize != null) mPicSize.setSummary(getPicSizeSummary(sharedPreferences));

				if (mScrlRngW != null) mScrlRngW.setSummary(getScrlRngWSummary(sharedPreferences));
				if (mScrlRngH != null) mScrlRngH.setSummary(getScrlRngHSummary(sharedPreferences));

				if (mFontName != null) mFontName.setSummary(getFontNameSummary(sharedPreferences));

				if (mTapPattern != null) mTapPattern.setSummary(SetImageText.getTapPatternSummary(mResources, sharedPreferences));

				if (mViewPt != null) mViewPt.setSummary(SetImageText.getViewPtSummary(mResources, sharedPreferences));
				if (mVolKey != null) mVolKey.setSummary(SetImageText.getVolKeySummary(mResources, sharedPreferences));
				if (mLastPage != null) mLastPage.setSummary(SetImageText.getLastPageSummary(mResources, sharedPreferences));
				if (mPageSel != null) mPageSel.setSummary(SetImageText.getTxPageSelectSummary(mResources, sharedPreferences));
				if (mTimeAndBattery != null) mTimeAndBattery.setSummary(getTimeSummary(sharedPreferences));
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
			if (key.equals(DEF.KEY_TX_INISCALE)) {
				if (mIniScale != null) mIniScale.setSummary(getIniScaleSummary(sharedPreferences));
			} else if (key.equals(DEF.KEY_TX_INITVIEW)) {
				if (mInitView != null) mInitView.setSummary(getInitViewSummary(sharedPreferences));
			} else if (key.equals(DEF.KEY_TX_VIEWROTA)) {
				if (mViewRota != null) mViewRota.setSummary(getViewRotaSummary(sharedPreferences));
			} else if (key.equals(DEF.KEY_TX_PAPER)) {
				if (mPaper != null) mPaper.setSummary(getPaperSummary(sharedPreferences));
			} else if (key.equals(DEF.KEY_TX_PICSIZE)) {
				if (mPicSize != null) mPicSize.setSummary(getPicSizeSummary(sharedPreferences));
			} else if (key.equals(DEF.KEY_TX_FONTTOP)) {
				if (mFontTop != null) mFontTop.setSummary(getFontTopSummary(sharedPreferences));
			} else if (key.equals(DEF.KEY_TX_FONTBODY)) {
				if (mFontBody != null) mFontBody.setSummary(getFontBodySummary(sharedPreferences));
			} else if (key.equals(DEF.KEY_TX_FONTRUBI)) {
				if (mFontRubi != null) mFontRubi.setSummary(getFontRubiSummary(sharedPreferences));
			} else if (key.equals(DEF.KEY_TX_FONTINFO)) {
				if (mFontInfo != null) mFontInfo.setSummary(getFontInfoSummary(sharedPreferences));
			} else if (key.equals(DEF.KEY_TX_SPACEW)) {
				if (mSpaceW != null) mSpaceW.setSummary(getSpaceWSummary(sharedPreferences));
			} else if (key.equals(DEF.KEY_TX_SPACEH)) {
				if (mSpaceH != null) mSpaceH.setSummary(getSpaceHSummary(sharedPreferences));
			} else if (key.equals(DEF.KEY_TX_MARGINW)) {
				if (mMarginW != null) mMarginW.setSummary(getMarginWSummary(sharedPreferences));
			} else if (key.equals(DEF.KEY_TX_MARGINH)) {
				if (mMarginH != null) mMarginH.setSummary(getMarginHSummary(sharedPreferences));
			} else if (key.equals(DEF.KEY_TX_SCRLRNGW)) {
				if (mScrlRngW != null) mScrlRngW.setSummary(getScrlRngWSummary(sharedPreferences));
			} else if (key.equals(DEF.KEY_TX_SCRLRNGH)) {
				if (mScrlRngH != null) mScrlRngH.setSummary(getScrlRngHSummary(sharedPreferences));
			} else if (key.equals(DEF.KEY_TX_FONTNAME)) {
				if (mFontName != null) mFontName.setSummary(getFontNameSummary(sharedPreferences));
			} else if (key.equals(DEF.KEY_TAPPATTERN) || key.equals(DEF.KEY_TAPRATE)) {
				if (mTapPattern != null) mTapPattern.setSummary(SetImageText.getTapPatternSummary(mResources, sharedPreferences));
			} else if (key.equals(DEF.KEY_VIEWPT)) {
				if (mViewPt != null) mViewPt.setSummary(SetImageText.getViewPtSummary(mResources, sharedPreferences));
			} else if (key.equals(DEF.KEY_VOLKEY)) {
				if (mVolKey != null) mVolKey.setSummary(SetImageText.getVolKeySummary(mResources, sharedPreferences));
			} else if (key.equals(DEF.KEY_LASTPAGE)) {
				if (mLastPage != null) mLastPage.setSummary(SetImageText.getLastPageSummary(mResources, sharedPreferences));
			} else if (key.equals(DEF.KEY_TX_PAGESELECT)) {
				if (mPageSel != null) mPageSel.setSummary(SetImageText.getTxPageSelectSummary(mResources, sharedPreferences));
			} else if (key.equals(DEF.KEY_TIMEDISP) || key.equals(DEF.KEY_TIMEFORMAT) || key.equals(DEF.KEY_TIMEPOS) || key.equals(DEF.KEY_TIMESIZE) || key.equals(DEF.KEY_TIMECOLOR)) {
				if (mTimeAndBattery != null) mTimeAndBattery.setSummary(getTimeSummary(sharedPreferences));
			}
		}

		private String getViewRotaSummary(SharedPreferences sharedPreferences){
			int val = getViewRota(sharedPreferences);
			Resources res = getResources();
			return res.getString(RotateName[val]);
		}

		private String getIniScaleSummary(SharedPreferences sharedPreferences){
			int val = getIniScale(sharedPreferences);
			Resources res = getResources();
			return res.getString(ScaleName[val]);
		}

		private String getInitViewSummary(SharedPreferences sharedPreferences){
			int val = getInitView(sharedPreferences);
			Resources res = getResources();
			return res.getString(ViewName[val]);
		}

		private String getPaperSummary(SharedPreferences sharedPreferences){
			int val = getPaper(sharedPreferences);
			Resources res = getResources();
			return res.getString(PaperName[val]);
		}

		private String getPicSizeSummary(SharedPreferences sharedPreferences){
			int val = getPicSize(sharedPreferences);
			Resources res = getResources();
			return res.getString(PicSizeName[val]);
		}

		private String getAscModeSummary(SharedPreferences sharedPreferences){
			int val = getAscMode(sharedPreferences);
			Resources res = getResources();
			return res.getString(AscModeName[val]);
		}

		private String getFontNameSummary(SharedPreferences sharedPreferences){
			String val = getFontName(sharedPreferences);
			if (val != null && val.length() > 0) {
				return val;
			}
			Resources res = getResources();
			return res.getString(R.string.defaultFont);
		}

		private String getFontTopSummary(SharedPreferences sharedPreferences){
			int val = getFontTop(sharedPreferences);
			Resources res = getResources();
			String summ1 = res.getString(R.string.unitSumm1);
			return DEF.getFontSpStr(val, summ1);
		}

		private String getFontBodySummary(SharedPreferences sharedPreferences){
			int val = getFontBody(sharedPreferences);
			Resources res = getResources();
			String summ1 = res.getString(R.string.unitSumm1);
			return DEF.getFontSpStr(val, summ1);
		}

		private String getFontRubiSummary(SharedPreferences sharedPreferences){
			int val = getFontRubi(sharedPreferences);
			Resources res = getResources();
			String summ1 = res.getString(R.string.unitSumm1);
			return DEF.getFontSpStr(val, summ1);
		}

		private String getFontInfoSummary(SharedPreferences sharedPreferences){
			int val = getFontInfo(sharedPreferences);
			Resources res = getResources();
			String summ1 = res.getString(R.string.unitSumm1);
			return DEF.getFontSpStr(val, summ1);
		}

		private String getSpaceWSummary(SharedPreferences sharedPreferences){
			int val = getSpaceW(sharedPreferences);
			Resources res = getResources();
			String summ1 = res.getString(R.string.rangeSumm1);
			return DEF.getTextSpaceStr(val, summ1);
		}

		private String getSpaceHSummary(SharedPreferences sharedPreferences){
			int val = getSpaceH(sharedPreferences);
			Resources res = getResources();
			String summ1 = res.getString(R.string.rangeSumm1);
			return DEF.getTextSpaceStr(val, summ1);
		}

		private String getMarginWSummary(SharedPreferences sharedPreferences){
			int val = getMarginW(sharedPreferences);
			Resources res = getResources();
			String summ1 = res.getString(R.string.rangeSumm1);
			return DEF.getDispMarginStr(val, summ1);
		}

		private String getMarginHSummary(SharedPreferences sharedPreferences){
			int val = getMarginH(sharedPreferences);
			Resources res = getResources();
			String summ1 = res.getString(R.string.rangeSumm1);
			return DEF.getDispMarginStr(val, summ1);
		}

		private String getScrlRngWSummary(SharedPreferences sharedPreferences){
			int val = getScrlRngW(sharedPreferences);
			Resources res = getResources();
			String summ1 = res.getString(R.string.srngSumm1);
			String summ2 = res.getString(R.string.srngSumm2);
			return DEF.getScrlRangeStr(val, summ1, summ2);
		}

		private String getScrlRngHSummary(SharedPreferences sharedPreferences){
			int val = getScrlRngH(sharedPreferences);
			Resources res = getResources();
			String summ1 = res.getString(R.string.srngSumm1);
			String summ2 = res.getString(R.string.srngSumm2);
			return DEF.getScrlRangeStr(val, summ1, summ2);
		}

		private String getTimeSummary(SharedPreferences sharedPreferences){
			boolean disp = getTimeDisp(sharedPreferences);
			int format = getTimeFormat(sharedPreferences);
			int pos = getTimePos(sharedPreferences);
			int size = getTimeSize(sharedPreferences);
			int color = getTimeColor(sharedPreferences);
			Resources res = getResources();

			String summ;
			if (disp) {
				summ = res.getString(TimeFormatName[format])
						+ ", " + res.getString(TimePosName[pos])
						+ ", " + DEF.getPnumSizeStr(size, res.getString(R.string.unitSumm1))
						+ ", " + res.getString(PnumColorName[color]);
			}
			else {
				summ = res.getString(R.string.pnumnodisp);
			}
			return summ;
		}
	}

	public static final int[] ScaleName =
		{ R.string.selsize00	// 元のサイズで表示
		, R.string.selsize01	// 幅に合わせて表示
		, R.string.selsize02	// 高さに合わせて表示
		, R.string.selsize03 };	// 全体を表示
	public static final int[] ViewName =
		{ R.string.selview01	// 見開き表示
		, R.string.selview02	// 単ページ表示
		, R.string.selview03 };	// 連続表示
	public static final int[] RotateName =
		{ R.string.rota00		// 回転あり
		, R.string.rota01		// 縦固定
		, R.string.rota02		// 横固定
		, R.string.rota03 };	// 縦固定(90°回転)
	public static final int[] PaperName =
		{ R.string.paper00		// 縦画面サイズ
		, R.string.paper01		// 800×1280
		, R.string.paper02		// 720×1280
		, R.string.paper03		// 540×960
		, R.string.paper04 };	// 480×800
	public static final int[] PicSizeName =
		{ R.string.picsize00	// 元画像サイズ
		, R.string.picsize01	// 2倍
		, R.string.picsize02	// 3倍
		, R.string.picsize03	// 4倍
		, R.string.picsize04	// 画面サイズに拡大
		, R.string.picsize05	// 画面サイズに拡大(余白無視)
		, R.string.picsize06	// 画面サイズに拡大(見開き対応)
		, R.string.picsize07 };	// 画面サイズに拡大(見開き対応:余白無視)
	public static final int[] AscModeName =
		{ R.string.ascmode00	// 縦表示
		, R.string.ascmode01	// 横表示
		, R.string.ascmode02 };	// 2桁は縦
	public static final int[] TimePosName =
		{ R.string.pnumpos00	// 左上
		, R.string.pnumpos01	// 中央上
		, R.string.pnumpos02	// 右上
		, R.string.pnumpos03	// 左下
		, R.string.pnumpos04	// 中央下
		, R.string.pnumpos05 };	// 右下
	public static final int[] PnumColorName =
		{ R.string.pnumcolor00		// 白
		, R.string.pnumcolor01 };		// 黒
	public static final int[] TimeFormatName =
		{ R.string.timeformat00		// 24:00
		, R.string.timeformat01		// 24:00 [100%]
		, R.string.timeformat02		// 24:00 [100%] [AC]
		, R.string.timeformat03		// 24:00
		, R.string.timeformat04		// 24:00 [100%]
		, R.string.timeformat05 };	// 24:00 [100%] [AC]

	// 設定の読込
	public static int getPinScale(SharedPreferences sharedPreferences){
		int val = DEF.getInt(sharedPreferences,DEF.KEY_PinchScaleText, "100");
		if( val < 0 || val > 250 ){
			val = 100;
		}
		return val;
	}

	public static int getViewRota(SharedPreferences sharedPreferences){
		int val = DEF.getInt(sharedPreferences, DEF.KEY_TX_VIEWROTA, "0");
		return val;
	}

	public static int getIniScale(SharedPreferences sharedPreferences){
		int val = DEF.getInt(sharedPreferences, DEF.KEY_TX_INISCALE, "2");
		return val;
	}

	public static int getInitView(SharedPreferences sharedPreferences){
		int val = DEF.getInt(sharedPreferences, DEF.KEY_TX_INITVIEW, "2");
		return val;
	}

	public static int getPaper(SharedPreferences sharedPreferences){
		int val = DEF.getInt(sharedPreferences, DEF.KEY_TX_PAPER, "0");
		return val;
	}

	public static int getPicSize(SharedPreferences sharedPreferences){
		int val = DEF.getInt(sharedPreferences, DEF.KEY_TX_PICSIZE, "0");
		return val;
	}

	public static int getBkLight(SharedPreferences sharedPreferences){
		int val = DEF.getInt(sharedPreferences, DEF.KEY_TX_BKLIGHT, "11");
		return val;
	}

	public static int getAscMode(SharedPreferences sharedPreferences){
		int val = DEF.getInt(sharedPreferences, DEF.KEY_TX_ASCMODE, 2);
		return val;
	}

	public static String getFontName(SharedPreferences sharedPreferences){
		return sharedPreferences.getString(DEF.KEY_TX_FONTNAME, "");
	}

	public static int getFontTop(SharedPreferences sharedPreferences){
		int num =  DEF.getInt(sharedPreferences, DEF.KEY_TX_FONTTOP, DEF.DEFAULT_TX_FONTTOP);
		return num;
	}

	public static int getFontBody(SharedPreferences sharedPreferences){
		int num =  DEF.getInt(sharedPreferences, DEF.KEY_TX_FONTBODY, DEF.DEFAULT_TX_FONTBODY);
		return num;
	}

	public static int getFontRubi(SharedPreferences sharedPreferences){
		int num =  DEF.getInt(sharedPreferences, DEF.KEY_TX_FONTRUBI, DEF.DEFAULT_TX_FONTRUBI);
		return num;
	}

	public static int getFontInfo(SharedPreferences sharedPreferences){
		int num =  DEF.getInt(sharedPreferences, DEF.KEY_TX_FONTINFO, DEF.DEFAULT_TX_FONTINFO);
		return num;
	}

	public static int getSpaceW(SharedPreferences sharedPreferences){
		int num =  DEF.getInt(sharedPreferences, DEF.KEY_TX_SPACEW, DEF.DEFAULT_TX_SPACEW);
		return num;
	}

	public static int getSpaceH(SharedPreferences sharedPreferences){
		int num =  DEF.getInt(sharedPreferences, DEF.KEY_TX_SPACEH, DEF.DEFAULT_TX_SPACEH);
		return num;
	}

	public static int getMarginW(SharedPreferences sharedPreferences){
		int num =  DEF.getInt(sharedPreferences, DEF.KEY_TX_MARGINW, DEF.DEFAULT_TX_MARGINW);
		return num;
	}

	public static int getMarginH(SharedPreferences sharedPreferences){
		int num =  DEF.getInt(sharedPreferences, DEF.KEY_TX_MARGINH, DEF.DEFAULT_TX_MARGINH);
		return num;
	}

	public static boolean getNotice(SharedPreferences sharedPreferences){
		boolean flag;
		flag =  DEF.getBoolean(sharedPreferences, DEF.KEY_TX_NOTICE, true);
		return flag;
	}

	public static boolean getNoSleep(SharedPreferences sharedPreferences){
		boolean flag;
		flag =  DEF.getBoolean(sharedPreferences, DEF.KEY_TX_NOSLEEP, false);
		return flag;
	}

	public static boolean getCenterMargin(SharedPreferences sharedPreferences){
		boolean flag;
		flag =  DEF.getBoolean(sharedPreferences, DEF.KEY_TX_CMARGIN, true);
		return flag;
	}

	public static boolean getCenterShadow(SharedPreferences sharedPreferences){
		boolean flag;
		flag =  DEF.getBoolean(sharedPreferences, DEF.KEY_TX_CSHADOW, false);
		return flag;
	}

	public static boolean getEffect(SharedPreferences sharedPreferences){
		boolean flag;
		flag =  DEF.getBoolean(sharedPreferences, DEF.KEY_TX_EFFECT, true);
		return flag;
	}

	public static int getScrlRngW(SharedPreferences sharedPreferences){
		int num;
		num = DEF.getInt(sharedPreferences, DEF.KEY_TX_SCRLRNGW, DEF.DEFAULT_TX_SCRLRNGW);
		return num;
	}

	public static int getScrlRngH(SharedPreferences sharedPreferences){
		int num;
		num = DEF.getInt(sharedPreferences, DEF.KEY_TX_SCRLRNGH, DEF.DEFAULT_TX_SCRLRNGH);
		return num;
	}


	public static int getTimeFormat(SharedPreferences sharedPreferences){
		int val = DEF.getInt(sharedPreferences, DEF.KEY_TIMEFORMAT, DEF.DEFAULT_TIMEFORMAT);
		if( val < 0 || val >= TimeFormatName.length){
			val = 1;
		}
		return val;
	}

	public static int getTimePos(SharedPreferences sharedPreferences){
		int val = DEF.getInt(sharedPreferences, DEF.KEY_TIMEPOS, DEF.DEFAULT_TIMEPOS);
		if( val < 0 || val >= TimePosName.length){
			val = 5;
		}
		return val;
	}

	public static int getTimeSize(SharedPreferences sharedPreferences){
		int val = DEF.getInt(sharedPreferences, DEF.KEY_TIMESIZE, DEF.DEFAULT_TIMESIZE);
		return val;
	}

	public static boolean getTimeDisp(SharedPreferences sharedPreferences){
		boolean flag;
		flag =  DEF.getBoolean(sharedPreferences, DEF.KEY_TIMEDISP, false);
		return flag;
	}

	public static int getTimeColor(SharedPreferences sharedPreferences){
		int val = DEF.getInt(sharedPreferences, DEF.KEY_TIMECOLOR, DEF.DEFAULT_TIMECOLOR);
		if( val < 0 || val >= PnumColorName.length){
			val = 1;
		}
		return val;
	}
}
