package src.comitton.imageview;

import java.util.GregorianCalendar;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.RandomAccessFile;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.zip.ZipInputStream;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.pdf.PdfRenderer;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.util.Log;

import jp.dip.muracoro.comittonx.R;
import src.comitton.common.DEF;
import src.comitton.common.Logcat;
import src.comitton.config.SetFileListActivity;
import src.comitton.fileaccess.FileAccess;
import src.comitton.fileview.data.FileData;
import src.comitton.fileaccess.FileAccessException;
import src.comitton.fileaccess.WorkStream;
import src.comitton.jni.CallImgLibrary;
import src.comitton.jni.CallJniLibrary;
import src.comitton.fileview.data.FileListItem;
import src.comitton.fileaccess.RarInputStream;
import src.comitton.textview.TextManager;
import src.comitton.common.BackgroundExecutor;

import android.annotation.SuppressLint;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

public class ImageManager extends InputStream implements Runnable {
	private static final String TAG = "ImageManager";

	public static final int FILETYPE_ZIP = 100;
	public static final int FILETYPE_RAR = 200;

	public static final int OPENMODE_VIEW = 0;
	public static final int OPENMODE_LIST = 1;
	public static final int OPENMODE_TEXTVIEW = 2;
	public static final int OPENMODE_THUMBNAIL = 3;
	public static final int OPENMODE_THUMBSORT = 4;

	public static final int OFFSET_LCL_SIGNA_LEN = 0;
	public static final int OFFSET_LCL_BFLAG_LEN = 6;
	public static final int OFFSET_LCL_FTIME_LEN = 10;
	public static final int OFFSET_LCL_FDATE_LEN = 12;
	public static final int OFFSET_LCL_CRC32_LEN = 14;
	public static final int OFFSET_LCL_CDATA_LEN = 18;
	public static final int OFFSET_LCL_OSIZE_LEN = 22;
	public static final int OFFSET_LCL_FNAME_LEN = 26;
	public static final int OFFSET_LCL_EXTRA_LEN = 28;

	public static final int OFFSET_CTL_SIGNA_LEN = 0;
	public static final int OFFSET_CTL_BFLAG_LEN = 8;
	public static final int OFFSET_CTL_FTIME_LEN = 12;
	public static final int OFFSET_CTL_FDATE_LEN = 14;
	public static final int OFFSET_CTL_CDATA_LEN = 20;
	public static final int OFFSET_CTL_OSIZE_LEN = 24;
	public static final int OFFSET_CTL_FNAME_LEN = 28;
	public static final int OFFSET_CTL_EXTRA_LEN = 30;
	public static final int OFFSET_CTL_CMENT_LEN = 32;
	public static final int OFFSET_CTL_LOCAL_LEN = 42;
	public static final int OFFSET_CTL_FNAME = 46;

	public static final int OFFSET_TRM_SIGNA_LEN = 0;
	public static final int OFFSET_TRM_CNTRL_LEN = 16;
	// 終端レコードの全エントリ数 (ZIP64 なら 0xFFFF)
	public static final int OFFSET_TRM_TOTAL_LEN = 10;

	// RAR Format
	public static final int RAR_HTYPE_MARK = 0x72;
	public static final int RAR_HTYPE_MAIN = 0x73;
	public static final int RAR_HTYPE_FILE = 0x74;
	public static final int RAR_HTYPE_SUB = 0x7a;
	public static final int RAR_HTYPE_OLD = 0x7e;

	public static final byte RAR_METHOD_STORING = 0x30;

	public static final int OFFSET_RAR_HCRC = 0;
	public static final int OFFSET_RAR_HTYPE = 2;
	public static final int OFFSET_RAR_HFLAGS = 3;
	public static final int OFFSET_RAR_HSIZE = 5;
	public static final int OFFSET_RAR_ASIZE = 7;

	// Marker Block
	// Archive Header
	public static final int OFFSET_RAR_RESV1 = 7; // 2bytes
	public static final int OFFSET_RAR_RESV2 = 9; // 4bytes

	// FileHeader
	public static final int OFFSET_RAR_PKSIZE = 7; // 4bytes
	public static final int OFFSET_RAR_UNSIZE = 11; // 4bytes
	public static final int OFFSET_RAR_HOSTOS = 15; // 1byte
	public static final int OFFSET_RAR_FCRC = 16; // 4bytes
	public static final int OFFSET_RAR_FTIME = 20; // 2bytes
	public static final int OFFSET_RAR_FDATE = 22; // 2bytes
	public static final int OFFSET_RAR_UNPVER = 24; // 1byte
	public static final int OFFSET_RAR_METHOD = 25; // 1byte
	public static final int OFFSET_RAR_FNSIZE = 26; // 2bytes
	public static final int OFFSET_RAR_ATTRIB = 28; // 4bytes
	public static final int OFFSET_RAR_FNAME = 32; // OFFSET_RAR_FNSIZE
//	public static final int OFFSET_RAR_HPSIZE  = 32;	// 4bytes
//	public static final int OFFSET_RAR_HUSIZE  = 36;	// 4bytes
//	public static final int OFFSET_RAR_SALT    = xx;	// 8bytes
//	public static final int OFFSET_RAR_EXTTIME = xx;	// variable

	public static final int FILESORT_NONE = 0;
	public static final int FILESORT_NAME_UP = 1;
	public static final int FILESORT_NAME_DOWN = 2;

	public static final int SIZE_LOCALHEADER = 30;
	public static final int SIZE_CENTHEADER = 46;
	// ZIP64 終端ロケータと ZIP64 終端レコード (可変部分を除く) の大きさ
	private static final int SIZE_ZIP64_LOCATOR = 20;
	private static final int SIZE_ZIP64_EOCD = 56;
	public static final int SIZE_TERMHEADER = 22;
	public static final int SIZE_EXTRAHEADER1 = 16;
	public static final int SIZE_EXTRAHEADER2 = 12;

	public static final int SIZE_BITFLAG = 12;

	public static final int BIS_BUFFSIZE = 100 * 1024;//Buffered Input Streamのバッファサイズ

	private static final int SIZE_BUFFER = 1024;
//	private static final int SIZE_RARHEADER = 7;
//	private static final int SIZE_RAR_HIGHSIZE = 8;

	private static final int CACHEMODE_NONE = 0;
	private static final int CACHEMODE_FILE = 1;
//	private static final int CACHEMODE_MEM = 2;

	private static final int FROMTYPE_CACHE = 2;
	private static final int FROMTYPE_LOCAL = 3;
	private static final int FROMTYPE_SERVER = 4;

	private static final int BLOCKSIZE = 128 * 1024;

	private static final int DISPMODE_DUAL = 1;
	private static final int DISPMODE_HALF = 2;
	private static final int DISPMODE_EXCHANGE = 3;

	private static final int HOKAN_DOTS = 4;

	private static final int ROTATE_NORMAL = 0;
//	private static final int ROTATE_90DEG = 1;
	private static final int ROTATE_180DEG = 2;
//	private static final int ROTATE_270DEG = 3;

	private final int THUMBNAIL_BUFFSIZE = 5;

	private final AppCompatActivity mActivity;
	private TextManager mTextManager;
	private int mPageWay;
	private int mQuality;
	private boolean mPseLand;

	private int mHostType;
	private short mFileType;
	private final int mFileSort;
	private int mOpenMode;

	private final boolean mHidden;

	private int mCacheMode;
	private int mCurrentPage;
	private int mLoadingPage;
	private boolean mCurrentSingle;
	private final Handler mHandler;
	private int mFromType;

	private long mStartTime;
	private int mMsgCount;
	private int mReadSize;
	private int mDataSize;
	private boolean mCheWriteFlag;
	private volatile boolean mThreadLoading = true;

	private Thread mThread;
	private volatile boolean mRunningFlag = false;
	private volatile boolean mTerminate = false;
	private volatile boolean mCloseFlag = false;
	// close() の多重実行防止 (onDestroy と終了処理の両方から呼ばれる)
	private final AtomicBoolean mCloseRequested = new AtomicBoolean(false);
	private volatile boolean mCacheBreak;
	private volatile boolean mCacheSleep;
	private final Object mLock;

	// キャッシュスレッドの待機と起床 (Thread.sleep の代わり)
	private final Object mCacheSignal = new Object();
	private boolean mCacheSignaled = false;	// mCacheSignal で保護
	// 画面側のページ読み込み・スケーリングの実行数と最後に終わった時刻 (uptimeMillis)
	private final AtomicInteger mUserOps = new AtomicInteger(0);
	// サーバ上の ZIP / RAR をファイルキャッシュへ並行して落とすスレッド群 (表示モードのみ)
	private volatile FileCacheDownloader mDownloader = null;
	private volatile long mUserOpEnd = 0;
	// 画面側の操作が終わってからキャッシュ読み込みを再開するまでの待ち時間
	// (ページ 2 枚の読み込みとスケーリングの合間に割り込まないため)
	private static final long USER_QUIET_MILLIS = 100;

	/** URIとパスとファイル名 */
	private final String mPath;
	private final String mFileName;
	private final String mFilePath;
	private final String mUser;
	private final String mPass;
	public FileListItem[] mFileList = null;
	private int mMaxCmpLength;
	private int mMaxOrgLength;
	private boolean mLoadImage = false;

	private PdfRenderer mPdfRenderer = null;
	private RarInputStream mRarStream = null;
	// RAR 展開用のネイティブハンドル (cmpClose で解放)
	private volatile long mRarHandle = 0;
	// 一覧作成中に見つけたパスワード付きエントリ数と、ヘッダ自体が暗号化されているか
	private int mRarEncryptedCount = 0;
	private boolean mRarHeadersEncrypted = false;

	private final int mMaxThreadNum;
	private final String mRarCharset;
	private int mCacheIndex = DEF.ERROR_CODE_CACHE_INDEX_OUT_OF_RANGE;

	/** BitmapFactory のデコード先として使い回す作業Bitmap(mLock 配下でのみ使用) */
	private Bitmap mDecodeBitmap = null;
	/** ネイティブへ画像データを渡す際の読み込みバッファ(mLock 配下でのみ使用) */
	private final byte[] mReadBuffer = new byte[BIS_BUFFSIZE];

	/** 読み込み時間の計測ログ (デバッグビルドのみ). adb logcat -s CachePerf で見る */
	private static final String PERF_TAG = "CachePerf";
	// cmpRead がサーバから読んだ量と時間 (mLock 配下で更新)
	private long mPerfNetBytes;
	// 一覧解析の内訳 (エントリ解析全体, うちファイル名の文字コード変換, 種別判定)
	private long mPerfEntryNanos;
	private long mPerfNameNanos;
	private long mPerfTypeNanos;
	private long mPerfNetNanos;
	private int mPerfNetCalls;

	static void perf(String msg) {
		if (DEF.DEBUG) {
			Log.i(PERF_TAG, "[" + Thread.currentThread().getName() + "] " + msg);
		}
	}

	static long perfMs(long startNanos) {
		return (System.nanoTime() - startNanos) / 1000000L;
	}

	/** キャッシュスレッドを起こす */
	private void wakeCacheThread() {
		synchronized (mCacheSignal) {
			mCacheSignaled = true;
			mCacheSignal.notifyAll();
		}
	}

	/** wakeCacheThread() が呼ばれるか millis 経過するまで待つ */
	private void waitCacheSignal(long millis) {
		synchronized (mCacheSignal) {
			if (!mCacheSignaled && millis > 0) {
				try {
					mCacheSignal.wait(millis);
				}
				catch (InterruptedException ignored) {
				}
			}
			mCacheSignaled = false;
		}
	}

	/** キャッシュスレッドの処理中に、画面側の読み込み要求が来た (またはページ一覧などで休止を求められた) */
	private boolean isCacheThreadCanceled() {
		return (mCacheBreak || mCacheSleep) && Thread.currentThread() == mThread;
	}

	private void userOpStart() {
		mUserOps.incrementAndGet();
	}

	private void userOpEnd() {
		mUserOpEnd = SystemClock.uptimeMillis();
		mUserOps.decrementAndGet();
		wakeCacheThread();
	}

	private void perfReset() {
		mPerfNetBytes = 0;
		mPerfNetNanos = 0;
		mPerfNetCalls = 0;
	}

	private String perfNet() {
		long ms = mPerfNetNanos / 1000000L;
		long kbps = ms > 0 ? mPerfNetBytes * 1000L / 1024L / ms : 0;
		return "net=" + (mPerfNetBytes / 1024) + "KB/" + ms + "ms/" + mPerfNetCalls + "calls(" + kbps + "KB/s)";
	}

	@SuppressLint("SuspiciousIndentation")
    public ImageManager(AppCompatActivity activity, String path, String cmpfile, String user, String pass, int sort, Handler handler, boolean hidden, int openmode, int maxthread) {
		int logLevel = Logcat.LOG_LEVEL_WARN;
		Logcat.d(logLevel, "開始します. path=" + path + ", cmpfile=" + cmpfile);

        mActivity = activity;
		mFileList = null;
		mFilePath = DEF.relativePath(mActivity, path, cmpfile);
		mPath = path;
		mFileName = cmpfile;
		mUser = user;
		mPass = pass;
		mTerminate = false;
		mCacheBreak = false;
		mCacheSleep = false;
		// 自分自身 (InputStream) ではなく専用のオブジェクトで排他する
		mLock = new Object();
		mRunningFlag = true;
		mCloseFlag = false;
		mHandler = handler;
		mFileSort = sort;
		mHidden = hidden;
		mOpenMode = openmode;

 		// スレッド数
 		mMaxThreadNum = maxthread;

		//mMemSize = memsize;
		//mMemNextPages = memnext;
		//mMemPrevPages = memprev;
		//LoadImageList(memsize, memnext, memprev);

		mRarCharset = new String( "UTF-8" );
		Logcat.d(logLevel, "終了します.");
	}

	public void LoadImageList(int memsize, int memnext, int memprev) {
		int logLevel = Logcat.LOG_LEVEL_WARN;
		Logcat.d(logLevel, "開始します. memsize=" + memsize + ", memnext=" + memnext + ", memprev=" + memprev);
		mMemSize = memsize;
		mMemNextPages = memnext;
		mMemPrevPages = memprev;

		try {

			mHostType = FileAccess.accessType(mFilePath);
			mFileType = FileData.getType(mActivity, mFilePath);
			Logcat.d(logLevel, "mHostType=" + mHostType + ", mFileType=" + mFileType);

			if (mFileType != FileData.FILETYPE_DIR && mFileType != FileData.FILETYPE_IMG && mFileType != FileData.FILETYPE_PDF && mFileType != FileData.FILETYPE_ARC && mFileType != FileData.FILETYPE_EPUB) {
				// ファイルタイプが不正な場合
				if (mOpenMode == OPENMODE_TEXTVIEW) {
					// テキストビュワーから呼ばれたなら、FILETYPE_IMGに偽装する
					mFileType = FileData.FILETYPE_IMG;
				}
				else {
					// テキストビュワーから呼ばれていなければエラーを返す
					Logcat.e(logLevel, "ファイルタイプが不正です. mFileType=" + mFileType + ", mFilePath=" + mFilePath);
					throw new IOException(TAG + ": LoadImageList: ファイルタイプが不正です. mFileType=" + mFileType + ", mFilePath=" + mFilePath);
				}
			}

			if (mFileType == FileData.FILETYPE_DIR) {
				DirFileList(mFilePath, mUser, mPass);
			}
			else if (mFileType == FileData.FILETYPE_IMG) {
				fileAccessInit(mFilePath);
				ImageFileList(mFilePath, mUser, mPass);
			}
			else if (mFileType == FileData.FILETYPE_PDF) {
				PdfFileList(mFilePath, mUser, mPass);
			}
			else if (mEpubOrder && mFileType == FileData.FILETYPE_EPUB) {
				fileAccessInit(mFilePath);
				epubFileList();
			}
			else {
				long perfStart = System.nanoTime();
				fileAccessInit(mFilePath);
				perf("list: open time=" + perfMs(perfStart) + "ms");
				perfStart = System.nanoTime();
				cmpFileList();
				perf("list: cmpFileList time=" + perfMs(perfStart) + "ms entries=" + (mFileList != null ? mFileList.length : 0));
			}

			if (mCloseFlag) {
				// クローズされてたら初期化しない
				return;
			}

			long perfInit = System.nanoTime();
			// メモリキャッシュの初期化(JNI)
			Logcat.d(logLevel, MessageFormat.format("メモリキャッシュを初期化します. MemoryCacheInit({0}, {1}, {2}, {3}, {4})", new Object[]{mMemSize, mMemNextPages, mMemPrevPages, mFileList.length, mMaxOrgLength}));
			if (!MemoryCacheInit(mMemSize, mMemNextPages, mMemPrevPages, mFileList.length, mMaxOrgLength)) {
				mFileList = new FileListItem[0];
				mLoadImage = true;
				return;
			}

			if (mCloseFlag) {
				// 初期化中にクローズされてたら解放する
				int returnCode = CallImgLibrary.ImageTerminate(mActivity, mHandler, mCacheIndex);
				return;
			}

			fileCacheInit(mFileList.length, mHostType != DEF.ACCESS_TYPE_LOCAL);
			startDownloader();
			startCacheRead();
			perf("list: cache init time=" + perfMs(perfInit) + "ms");

		}
		catch (IOException ex) {
			mFileList = new FileListItem[0];
			Logcat.e(logLevel, "", ex);
			Message message = new Message();
			message.what = DEF.HMSG_ERROR;
			message.obj = ex.getMessage();
			mHandler.sendMessage(message);
		}
		Logcat.d(logLevel, "終了します. ");
		mLoadImage = true;
	}

	public int mEpubMode = TextManager.EPUB_MODE_ALL_IMAGE;

	private void epubFileList() throws IOException {
		int logLevel = Logcat.LOG_LEVEL_WARN;
		Logcat.d(logLevel, "開始します.");
		int tmpOpenMode = mOpenMode;

		// ファイル一覧を取得
		mOpenMode = OPENMODE_TEXTVIEW;
		fileAccessInit(mFilePath);
		cmpFileList();
		mOpenMode = tmpOpenMode;

		// EPUBファイルを解析
		mTextManager = new TextManager(this, "META-INF/container.xml", mUser, mPass, mHandler, mActivity, FileData.FILETYPE_EPUB);
		mTextManager.mEpubMode = mEpubMode;
		mFileList = mTextManager.getEpubImageList();
		mTextManager.release();
		mTextManager = null;
		Logcat.d(logLevel, "終了します.");
	}

	private void cmpFileList() throws IOException {
		int logLevel = Logcat.LOG_LEVEL_WARN;
		Logcat.d(logLevel, "開始します.");

		int thumbSortType = 0;
		// 圧縮ファイル読み込み
		byte[] buf = new byte[SIZE_BUFFER];
		int readSize = 0;
		long cmppos = 0;
		long orgpos = 0;
		long headpos = 0;
		int count = 0;
		int maxcmplen = 0;
		int maxorglen = 0;
		List<FileListItem> list = new ArrayList<FileListItem>();
		byte [] cdhBuf = null;
		long perfStart = System.nanoTime();
		long fileLength = cmpDirectLength();
		long cdhLength = 0;		//central directory header length
		perf("list: length time=" + perfMs(perfStart) + "ms");

		boolean rar5 = false;

		sendProgress(0, count);

		// ZIPかRARか判定
		Logcat.d(logLevel, "読み込みます.");
		cmpDirectRead(buf, 0, SIZE_BUFFER);
		if (buf[0] == 'P' && buf[1] == 'K'){
			mFileType = FILETYPE_ZIP;
			Logcat.d(logLevel, "ZIPファイルです.");
		}
		else if (buf[0] == 'R' && buf[1] == 'a' && buf[2] == 'r') {
			mFileType = FILETYPE_RAR;
			if (mOpenMode == OPENMODE_THUMBSORT) {
				// RARファイルかつソートありのサムネイル取得であればソート条件を取得
				SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mActivity);
				thumbSortType = SetFileListActivity.getThumbSortType(sp);
				Logcat.d(logLevel, "thumbSortType=" + thumbSortType);
			}
			Logcat.d(logLevel, "RARファイルです.");
		}
		else {
			DEF.sendMessage(mActivity.getString(R.string.UnknownArchiveFormat) + "\n" + mFileName, Toast.LENGTH_LONG, mHandler);
			mFileList = new FileListItem[0];
			return;
		}
		cmpDirectSeek(0);

		if (mFileType == FILETYPE_ZIP) {
			// 圧縮されたファイル情報取得
			perfStart = System.nanoTime();
			headpos = zipSearchCentral();
			perf("list: search central time=" + perfMs(perfStart) + "ms");
			perfStart = System.nanoTime();
			Logcat.d(logLevel, MessageFormat.format("ZIPセントラルディレクトリヘッダ位置: fileLength={0}, headpos={1}", new Object[]{fileLength, headpos}));
			if (headpos == 0) {
				cmpDirectSeek(0);
			} else {
				//central directory headerが見つかった場合、一括でバッファに読み込む
				// ZIPはファイル情報が1か所にまとめて格納されている
				cdhLength = fileLength - headpos;
				cdhBuf = new byte[(int) cdhLength];
				cmpDirectRead(cdhBuf, 0, (int) cdhLength);
				cmpDirectSeek(headpos);
				Logcat.d(logLevel, MessageFormat.format("セントラルディレクトリヘッダをバッファに読み込みます: cdhLength={0}", new Object[]{cdhLength}));
			}
			perf("list: read central time=" + perfMs(perfStart) + "ms len=" + cdhLength);
		}
		long perfParse = System.nanoTime();

		// 簡易的なRAR5判定
		if (mFileType == FILETYPE_RAR) {
			byte[] sigbuff = new byte[8];
			cmpDirectRead( sigbuff, 0, 8 );
			if( sigbuff[0] == 0x52 && sigbuff[1] == 0x61 && sigbuff[2] == 0x72 && sigbuff[3] == 0x21 &&
				sigbuff[4] == 0x1a && sigbuff[5] == 0x07 && sigbuff[6] == 0x01 && sigbuff[7] == 0x00 ){
				rar5 = true;
				Logcat.d(logLevel, "RAR5です.");
			}
			else {
				Logcat.d(logLevel, "RAR5ではありません.");
			}
			cmpDirectSeek(0);
		}

		FileListItem fl = null;
		boolean stop = false;
		try {
			while (!stop) {

				if (!mRunningFlag) {
					mFileList = new FileListItem[0];
					return;
				}

				if (Logcat.isDebugEnabled(logLevel)) Logcat.d(logLevel, MessageFormat.format("ループ実行: count={0}, fileLength={1}, headpos={2}", new Object[]{count, fileLength, headpos}));
				if(headpos != 0){
					if(headpos < fileLength) {
						readSize = (int) ((fileLength - headpos >= 1024) ? 1024 : fileLength - headpos);
						int pos = (int) (cdhLength - (fileLength - headpos));
						buf = Arrays.copyOfRange(cdhBuf, pos, readSize+pos);
						if (Logcat.isDebugEnabled(logLevel)) Logcat.d(logLevel, MessageFormat.format("バッファをコピーします: count={0}, readSize={1}, pos={2}", new Object[]{count, readSize, pos}));
					}else
						readSize = -1;
				}else {
					// RARはファイル情報とファイルデータが交互に格納されている
					readSize = cmpDirectRead(buf, 0, SIZE_BUFFER);
					if (Logcat.isDebugEnabled(logLevel)) Logcat.d(logLevel, MessageFormat.format("バッファを読み込みました: count={0}, readSize={1}", new Object[]{count, readSize}));
				}
				if (readSize <= 0) {
					// ファイル終了
					break;
				}

				if (mFileType == FILETYPE_ZIP) {
					// 通常バージョンで読み込み
					if (headpos == 0) {
						fl = zipFileListItem(buf, cmppos, orgpos, readSize, true);
						fl.sizefixed = true;
					} else {
						long perfEntry = System.nanoTime();
						fl = zipFileListOldItemLite(buf, orgpos, readSize);
						mPerfEntryNanos += System.nanoTime() - perfEntry;
						if (fl != null) {
							fl.sizefixed = false;
						}
					}
				} else if (mFileType == FILETYPE_RAR) {
					// RAR読み込み
					if (rar5) {
						// RAR5
						fl = rar5FileListItem(buf, cmppos, orgpos, readSize);
					} else {
						// RAR4.x以前
						fl = rarFileListItem(buf, cmppos, orgpos, readSize);
					}
				} else {
					// 読み込み不可
					return;
				}

				if (fl != null) {
					Logcat.d(logLevel, "ファイルデータの取得に成功しました. count=" + count + ", fl.name=" + fl.name);

					// 対象ファイル判定
					if (fl.name != null && fl.name.length() > 4 && fl.orglen > 0 && fl.cmplen > 0) {
						if (!mHidden || !DEF.checkHiddenFile(fl.name)) {
							long perfType = System.nanoTime();
							fl.type = FileData.getType(mActivity, fl.name);
							mPerfTypeNanos += System.nanoTime() - perfType;
							fl.exttype = FileData.getExtType(mActivity, fl.name);
							boolean use = true;
							if (fl.type == FileData.FILETYPE_ARC || fl.type == FileData.FILETYPE_PARENT || fl.type == FileData.FILETYPE_DIR) {
								use = false;
							}
							else if (fl.type == FileData.FILETYPE_TXT && mOpenMode != OPENMODE_LIST && mOpenMode != OPENMODE_TEXTVIEW) {
								use = false;
							}
							else if (fl.type == FileData.FILETYPE_EPUB) {
								use = false;
							}
							else if (fl.type == FileData.FILETYPE_EPUB_SUB) {
								if (mOpenMode == OPENMODE_TEXTVIEW) {
									use = true;
								}
								else if (mOpenMode == OPENMODE_LIST) {
									// リストに表示しないものもページ数の計算に使うのでtrue
									use = true;
									//if (!fl.name.equals("META-INF/container.xml")) {
									//	use = false;
									//}
								}
								else {
									use = false;
								}
							}
							else if (fl.type == FileData.FILETYPE_NONE) {
								use = false;
							}

							if (use && fl.encrypted) {
								// パスワード付きエントリは展開できないので一覧に入れない
								use = false;
								mRarEncryptedCount++;
							}

							if (use) {
								// リストへ登録
								list.add(fl);
								if (mFileType == FILETYPE_RAR) {
									if (maxcmplen < fl.cmplen - fl.header) {
										// 最大サイズを求める
										maxcmplen = fl.cmplen - fl.header;
									}
								}
								if (maxorglen < fl.orglen) {
									// 最大サイズを求める
									maxorglen = fl.orglen;
								}
								if (mFileType == FILETYPE_RAR) {
									// RARファイルの時
									if (mOpenMode == OPENMODE_THUMBNAIL && list.size() >= 5) {
										// ソートなしのサムネイル取得であれば先頭5ファイルで終了
										Logcat.d(logLevel, "ソートなし. thumbSortType=" + thumbSortType + ", mHostType=" + mHostType);
										break;
									}
									else if (mOpenMode == OPENMODE_THUMBSORT && list.size() >= 5) {
										// ソートありのサムネイル取得であればソート条件で分岐
										Logcat.d(logLevel, "ソートあり. thumbSortType=" + thumbSortType + ", mHostType=" + mHostType);

										switch (thumbSortType) {
											case 0:
												// ソートなし
												Logcat.d(logLevel, "ソートあり. case 0: SET stop = true");
												stop = true;
												break;
											case 1:
												if (mHostType != DEF.ACCESS_TYPE_LOCAL) {
													// ローカルだけソートあり
													Logcat.d(logLevel, "ソートあり. case 1: SET stop = true");
													stop = true;
												}
												break;
											case 2:
												if (mHostType != DEF.ACCESS_TYPE_LOCAL && mHostType != DEF.ACCESS_TYPE_SMB) {
													// ローカルとSMBはソートあり
													Logcat.d(logLevel, "ソートあり. case 2: SET stop = true");
													stop = true;
												}
												break;
											default:
												// すべてソートあり
												break;
										}
									}
								}
							}
						}
					}

					// 次のファイルへ
					cmppos += fl.cmplen;
					orgpos += fl.orglen;
					if (mFileType == FILETYPE_ZIP && headpos > 0) {
						// 旧タイプのZIPの場合はセントラルヘッダをアクセス
						// (読み込み済みのバッファから取り出すのでシークは不要)
						headpos += fl.header;
					}
					else {
						cmpDirectSeek(cmppos);
						if (Logcat.isDebugEnabled(logLevel)) Logcat.d(logLevel, MessageFormat.format("シークします: count={0}, cmppos={1}", new Object[]{count, cmppos}));
					}

					count++;
					if (!sendProgress(0, count)) {
						mFileList = new FileListItem[0];
						return;
					}
				}
				else {
					Logcat.d(logLevel, "ファイルデータの取得に失敗しました. count=" + count + ", fl=null");
					break;
				}
			}
		} catch (Exception e) {
			if (mCloseFlag) {
				Logcat.w(logLevel, "圧縮ファイルの解析がキャンセルされました. count=" + count, e);
			}
			else {
				Logcat.e(logLevel, "圧縮ファイルの解析でエラーになりました. count=" + count, e);
			}
		}

		perf("list: parse time=" + perfMs(perfParse) + "ms count=" + count
				+ " entry=" + mPerfEntryNanos / 1000000 + "ms name=" + mPerfNameNanos / 1000000 + "ms type=" + mPerfTypeNanos / 1000000 + "ms"
				+ (list.isEmpty() ? "" : " sample=" + list.get(0).name));
		perfStart = System.nanoTime();
		sort(list);
		perf("list: sort time=" + perfMs(perfStart) + "ms");
		mFileList = list.toArray(new FileListItem[0]);
		if (mFileType == FILETYPE_RAR && list.isEmpty() && (mRarHeadersEncrypted || mRarEncryptedCount > 0)) {
			// パスワード付き RAR は展開できない
			throw new IOException(mActivity.getString(R.string.rarEncrypted) + "\n" + mFileName);
		}
		// RARであればメモリ確保
		if (mFileType == FILETYPE_RAR) {
			if (mRarHandle == 0) {
				mRarHandle = CallJniLibrary.rarOpen();
			}
			if (mRarHandle == 0) {
				throw new IOException("Memory Alloc Error.");
			}
			int ret = CallJniLibrary.rarAlloc(mRarHandle, maxcmplen, maxorglen);
			if (ret != CallJniLibrary.RAR_OK) {
				throw new IOException("Memory Alloc Error.");
			}
		}
		mMaxCmpLength = maxcmplen;
		mMaxOrgLength = maxorglen;

		Logcat.d(logLevel, "終了します. mMaxCmpLength=" + mMaxCmpLength + ", mMaxOrgLength=" + mMaxOrgLength);
	}

	public boolean sendProgress(int type, int count) {
		// 10ファイル単位で通知
		//if (count % 10 == 0) {
			if (!mRunningFlag) {
				return false;
			}
			Message message = new Message();
			message.what = DEF.HMSG_PROGRESS;
			message.arg1 = count;
			message.arg2 = type;
			mHandler.sendMessage(message);
		//}
		return true;
	}

	private long zipSearchCentral() throws IOException {
		long fileLength = cmpDirectLength();
		int pos = -1;
		int retsize;
		int buffSize = 1024;
		byte[] buff = new byte[buffSize];

		if (fileLength < SIZE_TERMHEADER) {
			throw new IOException("Broken Zip File.");
		}

		long filePos = fileLength - buffSize;
		for (int i = 0 ; i < 33 ; i ++) {
			if (filePos < 0) {
				// サイズが小さい場合はファイル先頭から
				buffSize = (int) (buff.length + filePos);
				filePos = 0;
			}

			// 終端コードへ移動
			cmpDirectSeek(filePos);
			retsize = cmpDirectRead(buff, 0, buffSize);
			if (retsize < buffSize) {
				throw new IOException("File Access Error.");
			}

			int sig;
			for (pos = buffSize - SIZE_TERMHEADER; pos >= 0; pos--) {
				if (buff[pos] == 0x50) {
					sig = getInt(buff, pos + OFFSET_TRM_SIGNA_LEN);
					if (sig == 0x06054b50) {
						break;
					}
				}
			}
			if (pos >= 0) {
				break;
			}
			if (filePos == 0) {
				// 先頭まできた
				break;
			}
			filePos -= (buffSize - (SIZE_TERMHEADER - 1));
		}
		if (pos < 0) {
			// ヘッダがおかしい
			throw new IOException("Central header is not found.");
		}
		// 位置は符号なし 32bit. 2GB を超える ZIP で負にならないよう long で扱う
		long posCentral = getInt(buff, pos + OFFSET_TRM_CNTRL_LEN) & 0xFFFFFFFFL;
		if (posCentral == 0xFFFFFFFFL || (getShort(buff, pos + OFFSET_TRM_TOTAL_LEN) & 0xFFFF) == 0xFFFF) {
			// ZIP64 なら、終端レコードの直前にある ZIP64 終端ロケータから本当の位置を読む
			long pos64 = zip64CentralPosition(filePos + pos, fileLength);
			if (pos64 >= 0) {
				posCentral = pos64;
			}
		}
		if (posCentral >= fileLength || posCentral < 0) {
			// ヘッダがおかしい、けどとりあえず先頭から読み込ませてみる
			//throw new IOException("Broken Zip File.");
			posCentral = 0;
		}

		// セントラルヘッダに移動
		cmpDirectSeek(posCentral);
		return posCentral;
	}

	/**
	 * ZIP64 終端ロケータと ZIP64 終端レコードからセントラルディレクトリの位置を得る.
	 * @param eocdPos 終端レコード (PK) のファイル内位置
	 * @return 位置. ZIP64 でなければ -1
	 */
	private long zip64CentralPosition(long eocdPos, long fileLength) throws IOException {
		if (eocdPos < SIZE_ZIP64_LOCATOR) {
			return -1;
		}
		byte[] loc = new byte[SIZE_ZIP64_LOCATOR];
		cmpDirectSeek(eocdPos - SIZE_ZIP64_LOCATOR);
		if (cmpDirectRead(loc, 0, loc.length) < loc.length || getInt(loc, 0) != 0x07064b50) {
			return -1;
		}
		long recPos = getLong(loc, 8);
		if (recPos < 0 || recPos + SIZE_ZIP64_EOCD > fileLength) {
			return -1;
		}
		byte[] rec = new byte[SIZE_ZIP64_EOCD];
		cmpDirectSeek(recPos);
		if (cmpDirectRead(rec, 0, rec.length) < rec.length || getInt(rec, 0) != 0x06064b50) {
			return -1;
		}
		return getLong(rec, 48);
	}

	// 8バイト数値取得
	private static long getLong(byte[] b, int pos) {
		long val = 0;
		for (int i = 7; i >= 0; i--) {
			val = (val << 8) | (b[pos + i] & 0xFFL);
		}
		return val;
	}

	/**
	 * セントラルディレクトリの拡張フィールドにある ZIP64 情報 (ID 0x0001) からローカルヘッダの位置を得る.
	 * 値は元サイズ・圧縮サイズ・位置の順に、32bit 側が 0xFFFFFFFF のものだけが入っている.
	 * @return 位置. 見つからなければ -1
	 */
	private static long zip64LocalOffset(byte[] buf, int extraPos, int extraLen, boolean orgFull, boolean cmpFull) {
		int end = Math.min(extraPos + extraLen, buf.length);
		int p = extraPos;
		while (p + 4 <= end) {
			int id = (buf[p] & 0xFF) | ((buf[p + 1] & 0xFF) << 8);
			int len = (buf[p + 2] & 0xFF) | ((buf[p + 3] & 0xFF) << 8);
			int data = p + 4;
			if (id == 0x0001) {
				int q = data + (orgFull ? 8 : 0) + (cmpFull ? 8 : 0);
				if (q + 8 <= Math.min(data + len, end)) {
					return getLong(buf, q);
				}
				return -1;
			}
			p = data + len;
		}
		return -1;
	}

	private int getExtraSize(byte [] buf){
		int sig = getInt(buf, OFFSET_LCL_SIGNA_LEN);
		if (sig != 0x04034b50) {
			// LocalFileHeaderじゃない
			return 0;
		}
		return getShort(buf, OFFSET_LCL_EXTRA_LEN);
	}

	private int getCompressedSize(byte [] buf){
		int sig = getInt(buf, OFFSET_LCL_SIGNA_LEN);
		if (sig != 0x04034b50) {
			// LocalFileHeaderじゃない
			return 0;
		}
		int bflag = getShort(buf, OFFSET_LCL_BFLAG_LEN);
		int lenCmp = getInt(buf, OFFSET_LCL_CDATA_LEN);
		int lenFName = getShort(buf, OFFSET_LCL_FNAME_LEN);
		int lenExtra = getShort(buf, OFFSET_LCL_EXTRA_LEN);
		int cmplen = SIZE_LOCALHEADER + lenFName + lenExtra + ((bflag & 0x0004) != 0 ? SIZE_BITFLAG : 0) + lenCmp;
		return cmplen;
	}

	public FileListItem zipFileListItem(byte buf[], long cmppos, long orgpos, int readsize, boolean isCmpSum) {
		int logLevel = Logcat.LOG_LEVEL_WARN;
		Logcat.d(logLevel, "開始します.");

		int sig = getInt(buf, OFFSET_LCL_SIGNA_LEN);
		int bflag = getShort(buf, OFFSET_LCL_BFLAG_LEN);
		int ftime = getShort(buf, OFFSET_LCL_FTIME_LEN);
		int fdate = getShort(buf, OFFSET_LCL_FDATE_LEN);
		int crc32 = getInt(buf, OFFSET_LCL_CRC32_LEN);
		int lenCmp = getInt(buf, OFFSET_LCL_CDATA_LEN);
		int lenOrg = getInt(buf, OFFSET_LCL_OSIZE_LEN);
		int lenFName = getShort(buf, OFFSET_LCL_FNAME_LEN);
		int lenExtra = getShort(buf, OFFSET_LCL_EXTRA_LEN);

		if (readsize < SIZE_LOCALHEADER) {
			// データ不正
			return null;
		}
		if (sig != 0x04034b50) {
			// データの終わり
			return null;
		}

		String name = "";
		if (readsize >= lenFName + SIZE_LOCALHEADER) {
			// ファイル名までのデータがあり
			try {
				name = DEF.toUTF8(buf, SIZE_LOCALHEADER, lenFName);
			}
			catch (Exception e) {
				name = "Unknown";
			}
		}

		int yy = ((fdate >> 9) & 0x7F) + 80;
		int mm = ((fdate >> 5) & 0x0F) - 1;
		int dd = fdate & 0x1F;
		int hh = (ftime >> 11) & 0x1F;
		int nn = (ftime >> 5) & 0x3F;
		int ss = (ftime & 0x1F) * 2;
		Date d = new GregorianCalendar(yy + 1900, mm, dd, hh, nn, ss).getTime();

		FileListItem file = new FileListItem();
		file.name = name;
		file.cmppos = cmppos;
		file.orgpos = orgpos;
		file.cmplen = SIZE_LOCALHEADER + lenFName + lenExtra + ((bflag & 0x0004) != 0 ? SIZE_BITFLAG : 0) + (isCmpSum ? lenCmp : 0);
		file.orglen = lenOrg;
		file.version = (byte) (((bflag & 0x0008) != 0 && lenCmp == 0 && crc32 == 0) ? 1 : 0); // バージョン 0:通常、 1:古い
		file.dtime = d.getTime();
//		file.bmpsize = 0;

		Logcat.d(logLevel, "終了します. name=" + name + ", size=" + lenOrg + ", date=" + d.getTime());
		return file;
	}

	public FileListItem zipFileListOldItem(byte buf[], long orgpos, int readsize) throws IOException {
		int sig = getInt(buf, OFFSET_CTL_SIGNA_LEN);

		if (readsize < SIZE_CENTHEADER) {
			// データ不正
			return null;
		}
		if (sig != 0x02014b50) {
			// セントラルディレクトリヘッダでなければデータの終わり
			return null;
		}

		int lenCmp = getInt(buf, OFFSET_CTL_CDATA_LEN);
		int lenOrg = getInt(buf, OFFSET_CTL_OSIZE_LEN);
		int lenFName = getShort(buf, OFFSET_CTL_FNAME_LEN);
		int lenExtra = getShort(buf, OFFSET_CTL_EXTRA_LEN);
		int lenComent = getShort(buf, OFFSET_CTL_CMENT_LEN);
		int lclOffset = getInt(buf, OFFSET_CTL_LOCAL_LEN);

		cmpDirectSeek(lclOffset);
		readsize = cmpDirectRead(buf, 0, SIZE_BUFFER);
		FileListItem file = zipFileListItem(buf, lclOffset, orgpos, readsize, false);
		if (file != null) {
			file.cmplen += lenCmp;
			file.orglen = lenOrg;
			file.version = 1; // バージョン 0:通常、 1:古い
			file.header = SIZE_CENTHEADER + lenFName + lenExtra + lenComent;
		}
		return file;
	}

	public FileListItem zipFileListOldItemLite(byte[] buf, long orgpos, int readsize) throws IOException {
		int logLevel = Logcat.LOG_LEVEL_WARN;
		Logcat.d(logLevel, "開始します.");

		int sig = getInt(buf, OFFSET_CTL_SIGNA_LEN);
		if (readsize < SIZE_CENTHEADER) {
			// データ不正
			return null;
		}
		if (sig != 0x02014b50) {
			// セントラルディレクトリヘッダでなければデータの終わり
			return null;
		}

		int bflag = getShort(buf, OFFSET_CTL_BFLAG_LEN);
		int lenCmp = getInt(buf, OFFSET_CTL_CDATA_LEN);
		int lenOrg = getInt(buf, OFFSET_CTL_OSIZE_LEN);
		int lenFName = getShort(buf, OFFSET_CTL_FNAME_LEN);
		int lenExtra = getShort(buf, OFFSET_CTL_EXTRA_LEN);
		int lenComent = getShort(buf, OFFSET_CTL_CMENT_LEN);
		long lclOffset = getInt(buf, OFFSET_CTL_LOCAL_LEN) & 0xFFFFFFFFL;
		if (lclOffset == 0xFFFFFFFFL) {
			// ZIP64: 本当の位置は拡張フィールドにある
			long offset64 = zip64LocalOffset(buf, OFFSET_CTL_FNAME + lenFName, lenExtra, lenOrg == -1, lenCmp == -1);
			if (offset64 >= 0) {
				lclOffset = offset64;
			}
		}
		int ftime = getShort(buf, OFFSET_CTL_FTIME_LEN);
		int fdate = getShort(buf, OFFSET_CTL_FDATE_LEN);

		int yy = ((fdate >> 9) & 0x7F) + 80;
		int mm = ((fdate >> 5) & 0x0F) - 1;
		int dd = fdate & 0x1F;
		int hh = (ftime >> 11) & 0x1F;
		int nn = (ftime >> 5) & 0x3F;
		int ss = (ftime & 0x1F) * 2;
		Date d = new GregorianCalendar(yy + 1900, mm, dd, hh, nn, ss).getTime();

		FileListItem file = new FileListItem();
		//ローカルファイルヘッダの拡張フィールドのサイズはセントラルディレクトリヘッダからは取得出来ないので、実際の読み込み時に辻褄を合わせる
		file.cmplen = SIZE_LOCALHEADER + lenFName + 0/*lenExtra*/ + ((bflag & 0x0004) != 0 ? SIZE_BITFLAG : 0) + lenCmp;
		file.orglen = lenOrg;
		file.cmppos = lclOffset;
		file.orgpos = orgpos;
		file.version = 1; // バージョン 0:通常、 1:古い
		file.header = SIZE_CENTHEADER + lenFName + lenExtra + lenComent;
		file.dtime = d.getTime();
		// ファイル名までのデータがあり
		try {
			long perfStart = System.nanoTime();
			file.name = DEF.toUTF8(buf, OFFSET_CTL_FNAME, lenFName);
			mPerfNameNanos += System.nanoTime() - perfStart;
		}
		catch (Exception e) {
			file.name = "Unknown";
		}
		return file;
	}

	public FileListItem rar5FileListItem(byte[] buf, long cmppos, long orgpos, int readsize) throws IOException {
		int logLevel = Logcat.LOG_LEVEL_WARN;
		Logcat.d(logLevel, "開始します.");

		// ヘッダを読み込み、ファイルヘッダだけをFileListItemとして返す
		// シグネチャ(マーカーブロック)やアーカイブヘッダーなどは読み飛ばす
		// VINTは2byteまでと決め打ちして簡略化 > vint取得関数を実装
		int pos = 0;
		VintData vint;
		int headpos = 0;

		// シグネチャ判定
		if( cmppos == 0 ){
			if( buf[0] == 0x52 && buf[1] == 0x61 && buf[2] == 0x72 && buf[3] == 0x21 &&
				buf[4] == 0x1a && buf[5] == 0x07 && buf[6] == 0x01 && buf[7] == 0x00 ){

//				Logcat.d( logLevel, "RAR5 Signature." );

				// ファイル情報ではない
				FileListItem file = new FileListItem();
				file.name = null;
				file.cmppos = 0;
				file.orgpos = 0;
				file.cmplen = 8;
				file.orglen = 0;
//				imgfile.bmpsize = 0;
				file.width = 0;
				file.height = 0;
				return file;
			}
		}

		// Header CRC32
		long hcrc = getInt( buf, 0 );
		pos += 4;

		// Header size
		vint = readVint( buf, pos );
		int hsize = vint.vint;
		pos += vint.count;
		// Header size以前のサイズ
		headpos = pos;

		// Header type
		vint = readVint( buf, pos );
		int htype = vint.vint;
		pos += vint.count;

		// Header flags
		vint = readVint( buf, pos );
		int hflags = vint.vint;
		pos += vint.count;

		// Extra area size(flags is set 0x0001)
		int hextra = 0;
		if( (hflags & 0x0001) != 0 ){
			vint = readVint( buf, pos );
			hextra = vint.vint;
			pos += vint.count;
		}

		// Data size(flags is set 0x0002)
		int hdata = 0;
		if( (hflags & 0x0002) != 0 ){
			vint = readVint( buf, pos );
			hdata = vint.vint;
			pos += vint.count;
		}

		if( htype == 4 ){
			// アーカイブ暗号化ヘッダ: 以降のヘッダは暗号化されていて読めない
			mRarHeadersEncrypted = true;
			throw new IOException("RAR5 headers are encrypted.");
		}

		// ファイルヘッダー以外はスキップ
		if( htype != 2 ){
//			Logcat.d( logLevel, "RAR5 not FileHeader." );

			// ファイル情報ではない
			FileListItem file = new FileListItem();
			file.name = null;
			file.cmppos = 0;
			file.orgpos = 0;
			file.cmplen = hdata + hsize + headpos;
			file.orglen = 0;
//			file.bmpsize = 0;
			file.width = 0;
			file.height = 0;
			return file;
		}


		// File flags
		vint = readVint( buf, pos );
		int fflag = vint.vint;
		pos += vint.count;

		// Unpacked size
		vint = readVint( buf, pos );
		int lenOrg = vint.vint;
		pos += vint.count;

		// Attributes
		vint = readVint( buf, pos );
		pos += vint.count;

		// mtime
		int ftime = 0;
		if( (fflag & 0x0002) != 0 ){
			ftime = getInt( buf, pos );
			pos += 4;
		}
		Date d = new Date( ftime );

		// Data CRC32
		int dcrc = getInt( buf, pos );
		pos += 4;

		// Compression information
		vint = readVint( buf, pos );
		int cinfo = vint.vint;
		pos += vint.count;

		// Host OS
		vint = readVint( buf, pos );
		pos += vint.count;

		// Name length
		vint = readVint( buf, pos );
		int fnlen = vint.vint;
		pos += vint.count;

		// Name
		String name = "";
		if( readsize >= pos + fnlen ){
			// ファイル名までのデータがあり
			try {
				for( int i = 0; i < fnlen; ++i ){
					if( buf[ pos + i ] == 0 ){
						fnlen = i;
						break;
					}
				}
				name = new String( buf, pos, fnlen, mRarCharset );
				//name = DEF.toUTF8( buf, pos, fnlen);
			}
			catch( Exception e ){
				name = "Unknown";
			}
		}

		// 暗号化レコード (extra area の type 0x01) があればパスワード付き
		boolean encrypted = false;
		if( hextra > 0 ){
			int extraPos = headpos + hsize - hextra;
			int extraEnd = headpos + hsize;
			if( extraPos >= pos + fnlen && extraEnd <= readsize ){
				while( extraPos < extraEnd ){
					VintData recSize = readVint( buf, extraPos );
					int recStart = extraPos + recSize.count;
					if( recSize.vint <= 0 || recStart + recSize.vint > extraEnd ){
						break;
					}
					VintData recType = readVint( buf, recStart );
					if( recType.vint == 0x01 ){
						encrypted = true;
						break;
					}
					extraPos = recStart + recSize.vint;
				}
			}
		}

		// 圧縮アルゴリズム: 0=RAR5, 1=RAR7 (RAR5 互換フラグがあれば RAR5)
		int algo = cinfo & 0x3f;
		byte unpVer;
		long winSize;
		if( algo == 0 ){
			unpVer = 50;
			winSize = 0x20000L << ((cinfo >>> 10) & 0x0f);
		}
		else if( algo == 1 ){
			unpVer = (cinfo & 0x00100000) != 0 ? (byte) 50 : (byte) 70;
			winSize = 0x20000L << ((cinfo >>> 10) & 0x1f);
			winSize += winSize / 32 * ((cinfo >>> 15) & 0x1f);
		}
		else {
			// 未対応のアルゴリズム. 展開時にエラーになる
			unpVer = 0;
			winSize = 0;
		}

		FileListItem file = new FileListItem();
		file.name = name;
		file.cmppos = cmppos;
		file.orgpos = orgpos;
		file.cmplen = hdata + hsize + headpos;//lenCmp + hsize;
		file.orglen = lenOrg;
		file.header = hsize + headpos;
		file.version = unpVer;
		file.winsize = winSize;
		file.encrypted = encrypted;
		file.nocomp = (cinfo & 0x0380) == 0;	// 無圧縮か
//		file.bmpsize = 0;
		file.dtime = d.getTime();
		return file;
	}

	private class VintData {
		public int vint;
		public int count;

		public VintData() {
			init();
		}

		public void init() {
			vint = 0;
			count = 0;
		}
	};

	private VintData readVint(byte[] buf, int pos ) {
		int dat;
		VintData data = new VintData();

		while( true ){
			dat = buf[ pos + data.count ];
			dat &= 0x0FF;
			data.vint += ((dat & ~0x80) << (data.count * 7));
			data.count++;
			if( (dat & 0x80) == 0 ){
				break;
			}
		}

		return data;
	}

	public FileListItem rarFileListItem(byte[] buf, long cmppos, long orgpos, int readsize) throws IOException {
		int logLevel = Logcat.LOG_LEVEL_WARN;
		Logcat.d(logLevel, "開始します.");
		int hcrc = getShort(buf, OFFSET_RAR_HCRC);
		int htype = buf[OFFSET_RAR_HTYPE];
		int hflags = getShort(buf, OFFSET_RAR_HFLAGS);
		int hsize = getShort(buf, OFFSET_RAR_HSIZE);
		int asize = (hflags & 0x8000) == 0 ? 0 : getInt(buf, OFFSET_RAR_ASIZE);

		boolean skip = false;
//		boolean oldFormat = false;

		if (htype != RAR_HTYPE_FILE) {
			if (htype == RAR_HTYPE_MARK) {
				if (hcrc != 0x6152 || hflags != 0x1a21 || hsize != 0x0007) {
					// ヘッダがおかしい
					throw new IOException("This RAR File Format is not supported.");
				}
			}
			else if (htype == RAR_HTYPE_OLD) {
				if (hcrc == 0x4552 && (hflags & 0x00FF) == 0x005e) {
//					oldFormat = true;
				}
			}
			else {
				// RAR_HTYPE_MAIN
				if (htype == RAR_HTYPE_MAIN && (hflags & 0x0080) != 0) {
					// MHD_PASSWORD: 以降のヘッダは暗号化されていて読めない
					mRarHeadersEncrypted = true;
					throw new IOException("RAR headers are encrypted.");
				}
			}
			skip = true;
		}
		else {
			if ((hflags & 0x0100) != 0) {
				// 巨大ファイルの時は見ない
				skip = true;
			}
		}

		if (skip) {
			if (hsize + asize <= 5) {
				throw new IOException("File is broken.");
			}
			// ファイル情報ではない
			FileListItem imgfile = new FileListItem();
			imgfile.name = null;
			imgfile.cmppos = 0;
			imgfile.orgpos = 0;
			imgfile.cmplen = hsize + asize;
			imgfile.orglen = 0;
//			imgfile.bmpsize = 0;
			imgfile.width = 0;
			imgfile.height = 0;
			return imgfile;
		}

		// ファイル情報取得
		int lenFName = getShort(buf, OFFSET_RAR_FNSIZE);
		int posFName = OFFSET_RAR_FNAME;
		String name = "";

		if (readsize >= lenFName + posFName) {
			// ファイル名までのデータがあり
			try {
				for (int i = 0; i < lenFName; i++) {
					if (buf[posFName + i] == 0) {
						lenFName = i;
						break;
					}
				}
				name = DEF.toUTF8(buf, posFName, lenFName);
			}
			catch (Exception e) {
				name = "Unknown";
			}
		}

		int lenCmp = getInt(buf, OFFSET_RAR_PKSIZE);
		int lenOrg = getInt(buf, OFFSET_RAR_UNSIZE);
		byte rarVer = buf[OFFSET_RAR_UNPVER];
		byte method = buf[OFFSET_RAR_METHOD];

		int ftime = getShort(buf, OFFSET_RAR_FTIME);
		int fdate = getShort(buf, OFFSET_RAR_FDATE);

		int yy = ((fdate >> 9) & 0x7F) + 80;
		int mm = ((fdate >> 5) & 0x0F) - 1;
		int dd = fdate & 0x1F;
		int hh = (ftime >> 11) & 0x1F;
		int nn = (ftime >> 5) & 0x3F;
		int ss = (ftime & 0x1F) * 2;
		Date d = new GregorianCalendar(yy + 1900, mm, dd, hh, nn, ss).getTime();

		// Image Fileのみ採用
		FileListItem imgfile = new FileListItem();
		imgfile.name = name;
		imgfile.cmppos = cmppos;
		imgfile.orgpos = orgpos;
		imgfile.cmplen = lenCmp + hsize;
		imgfile.orglen = lenOrg;
		imgfile.header = hsize;
		imgfile.version = rarVer;
		imgfile.nocomp = method == RAR_METHOD_STORING;
		// LHD_WINDOWMASK から辞書サイズ, LHD_PASSWORD からパスワードの有無
		imgfile.winsize = 0x10000L << ((hflags & 0x00e0) >> 5);
		imgfile.encrypted = (hflags & 0x0004) != 0;
//		imgfile.bmpsize = 0;
		imgfile.dtime = d.getTime();
		return imgfile;
	}

	private void DirFileList(String path, String user, String pass) throws IOException {
		int logLevel = Logcat.LOG_LEVEL_WARN;
		Logcat.d(logLevel, "開始します. path=" + path);
		int maxorglen = 0;

		// ファイルリストを作成
		List<FileListItem> list = new ArrayList<FileListItem>();

		dirListFiles(path, user, pass);

		if (!mRunningFlag) {
			return;
		}

		int count = 0;
		while (true) {
			FileListItem fl = dirGetFileListItem();
			if (fl == null) {
				break;
			}
			list.add(fl);

			if (maxorglen < fl.orglen) {
				maxorglen = fl.orglen;
			}
			// 読込通知
			count++;
			if (count % 10 == 0) {
				if (!mRunningFlag) {
					mFileList = new FileListItem[0];
					return;
				}
				Message message = new Message();
				message.what = DEF.HMSG_PROGRESS;
				message.arg1 = count;
				message.arg2 = 0;
				mHandler.sendMessage(message);
			}
		}

		sort(list);
		mFileList = (FileListItem[]) list.toArray(new FileListItem[0]);
		mMaxOrgLength = maxorglen;
		Logcat.d(logLevel, "終了します. ");
	}

	@SuppressLint("SuspiciousIndentation")
    private void ImageFileList(String path, String user, String pass) throws IOException {
		int logLevel = Logcat.LOG_LEVEL_WARN;
		Logcat.d(logLevel, "開始します. path=" + path);
		mFileList = new FileListItem[1];
		FileListItem filelist = new FileListItem();
		mFileList[0] = filelist;

		filelist.type = FileData.getType(mActivity, mFilePath);
		filelist.exttype = FileData.getExtType(mActivity, mFilePath);
		filelist.name = FileData.getName(mFilePath);

		FileAccess fileAccess = new FileAccess(mActivity, mFilePath, mUser, mPass, mHandler);
        try {
            fileAccess.open("r");
        } catch (FileAccessException e) {
			Logcat.e(logLevel, "ファイルオープンに失敗しました.", e);
        }
        try {
            mFileList[0].orglen = (int)fileAccess.length();
        } catch (FileAccessException e) {
			Logcat.e(logLevel, "ファイルサイズの取得に失敗しました.", e);
        } finally {
			// サイズを取るためだけに開いたので閉じる
			try {
				fileAccess.close();
			} catch (IOException e) {
				Logcat.w(logLevel, "", e);
			}
        }

		mMaxOrgLength = mFileList[0].orglen;
		Logcat.d(logLevel, "終了します. path=" + path + ", length=" + mMaxOrgLength);
	}

	private void PdfFileList(String path, String user, String pass) throws IOException {
		int logLevel = Logcat.LOG_LEVEL_WARN;
		int maxPage = 0;
		mMaxOrgLength = 0;

		Logcat.d(logLevel, "開始します. path=" + path + ", user=" + user);

		if (mPdfRenderer == null) {
			Logcat.d(logLevel, "PdfRendererを取得します.");
            ParcelFileDescriptor parcelFileDescriptor = null;
            try {
				// ParcelFileDescriptorインスタンスを作成する。
                parcelFileDescriptor = FileAccess.openParcelFileDescriptor(mActivity, path, user, pass, mActivity, mHandler);
				//ParcelFileDescriptorインスタンスを使用しPdfRendererをインスタンス化する。
				mPdfRenderer = new PdfRenderer(parcelFileDescriptor);
            } catch (Exception e) {
				Logcat.e(logLevel, "PDFの読み込みに失敗しました.", e);
            }
		}
		if (mPdfRenderer == null) {
			Logcat.e(logLevel, "mPdfRendererがnullです.");
			mFileList = new FileListItem[0];
			return;
		}

		// ファイルリストを作成
		maxPage = mPdfRenderer.getPageCount();
		mFileList = new FileListItem[maxPage];

		for (int page = 0; page < maxPage; page++) {

			FileListItem filelist = new FileListItem();
			filelist.type = FileData.FILETYPE_PDF;
			filelist.exttype = FileData.EXTTYPE_PDF;
			filelist.name = "Page" + (page+1);
			filelist.orglen = 0; // ファイルリスト読込中
			mFileList[page] = filelist;

			// 読込通知
			if ((page+1) % 10 == 0) {
				if (!mRunningFlag) {
					mFileList = new FileListItem[0];
					return;
				}
				Message message = new Message();
				message.what = DEF.HMSG_PROGRESS;
				message.arg1 = (page+1);
				message.arg2 = 0;
				mHandler.sendMessage(message);
			}
		}
		Logcat.d(logLevel, "終了します.");
	}

	// ソート実行
	public void sort(List<FileListItem> list) {
		if (mFileSort != FILESORT_NONE) {
			Collections.sort(list, new ZipComparator());
		}
	}

	// ソート用比較関数
	public class ZipComparator implements Comparator<FileListItem> {
		public int compare(FileListItem file1, FileListItem file2) {
			int result;
			result = DEF.compareFileName(file1.name, file2.name, DEF.SORT_BY_FILE_TYPE);
			if (mFileSort == FILESORT_NAME_DOWN) {
				result *= -1;
			}
			return result;
		}
	}

	// ファイルパスを返す
	public String getFilePath() {
		return mFilePath;
	}

	// ファイルタイプを返す
	public int getFileType() {
		return mFileType;
	}

	// 最大ファイルサイズ(圧縮時)を返す
	public int getMaxCmpLength() {
		return mMaxCmpLength;
	}

	// 最大ファイルサイズ(解凍時)を返す
	public int getMaxOrgLength() {
		return mMaxOrgLength;
	}

	// バックグラウンドのキャッシュ読み込みを止める
	public void setCacheSleep(boolean sleep) {
		mCacheSleep = sleep;
	}

	// 読み込み処理中断
	public void setBreakTrigger() {
		mRunningFlag = false;
	}

	// 読み込み処理中断
	public void unsetBreakTrigger() {
		mRunningFlag = true;
	}

	// ファイル数を返す
	public int length() {
		if (mFileList != null) {
			return mFileList.length;
		}
		return 0;
	}

	// ファイルを閉じる
	public void closeFiles() {
		int logLevel = Logcat.LOG_LEVEL_WARN;
		try {
			close();
		}
		catch (IOException e) {
			Logcat.e(logLevel, "", e);
		}
	}

	// ロック用オブジェクト取得
	public Object getLockObject() {
		return mLock;
	}

	// ページ選択時に表示する文字列を作成
	public String createPageStr(int page) {
		// パラメタチェック
		if (mFileList == null || (page < 0 || mFileList.length <= page)) {
			return "";
		}

		String strPath = mFilePath;
		if (mHostType == DEF.ACCESS_TYPE_SMB) {
			int idx = strPath.indexOf("@");
			if (idx >= 0) {
				strPath = "smb://" + strPath.substring(idx + 1);
			}
		}
		else if (mHostType == DEF.ACCESS_TYPE_SAF || mHostType == DEF.ACCESS_TYPE_PICKER) {
			strPath = FileAccess.filename(mActivity, strPath);
		}

		String pageStr;
		pageStr = (page + 1) + " / " + mFileList.length + "\n" + strPath + "\n" + mFileList[page].name;
		return pageStr;
	}

	public void startCacheRead() throws FileNotFoundException {
		if (mOpenMode != OPENMODE_VIEW) {
			// リスト取得モードの時は読み込み不要
			return;
		}
		// キャッシュ読込みスレッド開始
		perf("startCacheRead pages=" + (mFileList != null ? mFileList.length : 0) + " host=" + mHostType);
		mThread = new Thread(this, "comitton-cache");
		mThread.setPriority(Thread.MIN_PRIORITY);
		mThread.start();
	}

	/**
	 * サーバ上の ZIP / RAR を表示するときは、ファイルキャッシュへのダウンロードを別スレッドで始める.
	 */
	private void startDownloader() {
		int logLevel = Logcat.LOG_LEVEL_WARN;
		if (mOpenMode != OPENMODE_VIEW || !mCheEnable || mHostType == DEF.ACCESS_TYPE_LOCAL
				|| (mFileType != FILETYPE_ZIP && mFileType != FILETYPE_RAR) || mFileList == null || mFileList.length == 0) {
			return;
		}
		FileCacheDownloader downloader = new FileCacheDownloader(mActivity, mFilePath, mUser, mPass,
				new File(cacheFilePath()), mFileList, mFileType == FILETYPE_ZIP, new FileCacheDownloader.Host() {
			@Override
			public int currentPage() {
				return mCurrentPage;
			}

			@Override
			public boolean isCached(int page) {
				return cheGetCacheFlag(page);
			}

			@Override
			public void onPageCached(int page) {
				cheSetCacheFlag(page);
				wakeCacheThread();
			}
		});
		try {
			downloader.start();
			mDownloader = downloader;
		}
		catch (IOException e) {
			// ダウンロードスレッドなしでも、キャッシュスレッドが 1 ページずつ落とす
			Logcat.e(logLevel, "ダウンロードを開始できません.", e);
			downloader.close();
		}
	}

	// 画像の並びを逆にする
	public void reverseOrder() {
		mCacheBreak = true;
		CallImgLibrary.ImageCancel(mActivity, mHandler, mCacheIndex, 1);
		synchronized (mLock) {
			if (!mCloseFlag) {
				CallImgLibrary.ImageCancel(mActivity, mHandler, mCacheIndex, 0);
				if (mFileList != null) {
					FileListItem[] newlist = new FileListItem[mFileList.length];

					int num = mFileList.length;
					CallImgLibrary.ImageScaleFree(mActivity, mHandler, mCacheIndex, -1, -1);
					for (int i = 0; i < num; i++) {
						if (mCloseFlag) {
							break;
						}
						CallImgLibrary.ImageFree(mActivity, mHandler, mCacheIndex, i);

						newlist[num - i - 1] = mFileList[i];

						// キャッシュ状態初期化
						mMemCacheFlag[i] = new MemCacheFlag();
						if (mCheCacheFlag != null) {
							mCheCacheFlag[i] = false;
						}
					}
					mFileList = newlist;
					FileCacheDownloader downloader = mDownloader;
					if (downloader != null) {
						downloader.reset(newlist);
					}
				}
			}
		}
	}

	// キャッシュをクリアする
	public void clearMemCache() {
		mCacheBreak = true;
		CallImgLibrary.ImageCancel(mActivity, mHandler, mCacheIndex, 1);
		synchronized (mLock) {
			if (!mCloseFlag) {
				CallImgLibrary.ImageCancel(mActivity, mHandler, mCacheIndex, 0);
				CallImgLibrary.ImageScaleFree(mActivity, mHandler, mCacheIndex, -1, -1);

				if (mFileList != null) {
					for (int i = 0; i < mFileList.length; i++) {
						if (mCloseFlag) {
							break;
						}
						// キャッシュ状態初期化
						mMemCacheFlag[i] = new MemCacheFlag();
					}
				}
			}
		}
	}

	public void run() {
		int logLevel = Logcat.LOG_LEVEL_WARN;
		Logcat.d(logLevel, "開始します.");

		// 読込用バッファ
		final int CACHE_FPAGE = 4;
		final int CACHE_BPAGE = 2;
		final int CACHE_RANGE = 50;
		boolean isError = false;
		boolean fMemCacheExec = mMemPriority != null && mMemPriority.length > 0;
		int prevReadPage = -1;
		int sleepTimer;

		// 最初のページの表示が終わると userOpEnd() で起こされる
		sleepTimer = 1000;

		// キャッシュ読込
		while (mRunningFlag && !isError) {

			// 処理中断フラグ
			boolean fContinue = false;

			// 待機中に起こされたら (ページ移動など) すぐに再開する
			waitCacheSignal(sleepTimer);
			// 他のスレッドが mLock を取れるよう、処理の合間に少しだけ空ける
			sleepTimer = 5;

			boolean fMemCacheWrite = false;
			int page = -1;
			if (mCacheBreak || mCacheSleep) {
				perf("cache " + (mCacheBreak ? "break" : "sleep") + " current=" + mCurrentPage);
				mCacheBreak = false;
				if (mMemPriority != null && mMemPriority.length > 0) {
					fMemCacheExec = true;
					prevReadPage = -1;
				}

				if (mCacheSleep) {
					waitCacheSignal(300);
				}
				else {
					// 画面側の読み込みとスケーリングが終わり、少し経つまで待つ
					while (mRunningFlag && !mCacheSleep) {
						long rest = USER_QUIET_MILLIS - (SystemClock.uptimeMillis() - mUserOpEnd);
						if (mUserOps.get() == 0 && rest <= 0) {
							break;
						}
						waitCacheSignal(mUserOps.get() > 0 ? 1000 : rest);
					}
				}
				sleepTimer = 0;
				continue;
			}

			synchronized (mLock) {
				if (mCloseFlag) {
					break;
				}
				long perfScan = System.nanoTime();
				if (fMemCacheExec) {
					// 表示中のページの元画像 (ルーペ・拡大表示で使う) を先に戻す
					int restore = displayPageWithoutSource();
					if (restore >= 0 && prevReadPage != restore) {
						page = restore;
						prevReadPage = restore;
						fMemCacheWrite = true;
					}
					int iPrio;
					for (iPrio = 1; iPrio < mMemPriority.length && page == -1; iPrio++) {
						if (mCloseFlag) {
							break;
						}
						if (mCacheBreak) {
							// キャッシュ処理中断
							break;
						}

						if (!fMemCacheExec) {
							// メモリキャッシュ不可ならそれ以上しない
							break;
						}

						// チェック対象ページ
						int chkPage = mMemPriority[iPrio] + mCurrentPage;
						int chkPage2 = mMemPriority[iPrio] + mCurrentPage + (mMemPriority[iPrio] >= 0 ? 1 : -1);

						if (0 <= chkPage2 && chkPage2 < mFileList.length && mFileList[chkPage2].width <= 0) {
							// 先にファイルキャッシュへ落としておき、サイズはローカルから読む
							// (サーバから先頭だけ読むと、後で本読み込みでもう一度読むことになる)
							if (needDownload(chkPage2)) {
								ensureFileCache(chkPage2, false, true);
								if (mCacheBreak) {
									// 中断された
									break;
								}
							}
							perfReset();
							long perfStart = System.nanoTime();
							int perfRet = SizeCheckImage(chkPage2);
							perf("sizecheck2 page=" + chkPage2 + " ret=" + perfRet + " time=" + perfMs(perfStart) + "ms " + perfNet());
							if (perfRet < 0) {
								Logcat.e(logLevel, "SizeCheckImage(chkPage2) < 0, chkPage2=" + chkPage2);
								break;
							}
						}

						if (0 <= chkPage && chkPage < mFileList.length) {
							// 範囲内の時だけチェック
							// 表示中でないページは、表示用画像があれば元画像を読み直さない
							if (!mMemCacheFlag[chkPage].fSource && (!mMemCacheFlag[chkPage].hasScale() || isDisplayPage(chkPage))) {
								// サイズ取得とメモリキャッシュ確保は LoadImage 内で
								// 本読み込みと同じストリームを使って行う(再シーク・再展開を避ける)
								if (prevReadPage != chkPage) {
									// Logcat.d(logLevel, "current" + mCurrentPage + ", chkPage" + chkPage + ", prevPage=" + prevReadPage);
									page = chkPage;
									prevReadPage = chkPage;
									fMemCacheWrite = true;
									fContinue = false;
								}
								else {
									// 直前に読み込みに失敗したページなら終了
									// Logcat.d(logLevel, "chkPage" + chkPage + ", prevReadPage=" + prevReadPage);
									fMemCacheExec = false;
								}
								// 対象を決めたら結果によらずこれ以上探さない
								break;
							}
							else if (mCurrentPage >= 0 && mCurrentPage < mFileList.length) {
								// スケーリング処理を通知
								try {
									if (isDualView()) {
										// 並べて表示
										int p;
										int page1 = -1;
										int page2 = -1;	// ターゲット

										if (chkPage < mCurrentPage) {
											// 前方向
											for (p = mCurrentPage - 1 ; p >= chkPage ; p --) {	// 1ページ前からチェック
												if (mCloseFlag) {
													break;
												}
												if (!DEF.checkPortrait(mFileList[p].width, mFileList[p].height, mScrRotate)) {
													// 横
													page1 = p;
													page2 = -1;
												}
												else {
													// 左ページは縦
													if (p == 0 || !DEF.checkPortrait(mFileList[p - 1].width, mFileList[p - 1].height, mScrRotate)) {
														// 左ページが先頭ページ 又は 右ページが横長なら左ページ単体とする
														page1 = p;
														page2 = -1;
													}
													else {
														if (mTopSingle != 0 && p == 1) {
															// 先頭単独ON かつ 右ページが先頭ページなら左ページ単体とする
															page1 = p;
															page2 = -1;
														}
														else {
															// 右ページも縦長なら並べて見開き
															page1 = p - 1;
															page2 = p;
															p --;
														}
													}
												}
											}
										}
										else {
											// 後方向
											for (p = mCurrentPage ; p <= chkPage ; p ++) {	// 1ページ前からチェック
												if (mCloseFlag) {
													break;
												}
												if (!DEF.checkPortrait(mFileList[p].width, mFileList[p].height, mScrRotate) || (p == mCurrentPage && mCurrentSingle)) {
													// 横長 又は 先頭が単ページ指定
													page1 = p;
													page2 = -1;
												}
												else {
													// 左ページは縦
													if (p >= mFileList.length - 1 || !DEF.checkPortrait(mFileList[p + 1].width, mFileList[p + 1].height, mScrRotate)) {
														// 右ページが最終ページ 又は 左ページが横なら右ページ単体とする
														page1 = p;
														page2 = -1;
													}
													else {
														// 左ページも縦長なら並べて見開き
														page1 = p;
														page2 = p + 1;
														p ++;
													}
												}
											}
										}

										if (page1 != -1 && page2 == -1) {
											// 単ページ
											if (mMemCacheFlag[page1].fSource) {
												// 通知
												//sendMessage(mHandler, DEF.HMSG_CACHE, 0, 2, null);
												if (!ImageScaling(page1, -1, ImageData.HALF_NONE, 0, null, null)) {
													// スケール失敗
													fMemCacheExec = false;
												}
											}
										}
										else if (page1 != -1 && page2 != -1) {
											if (mMemCacheFlag[page1].fSource && mMemCacheFlag[page2].fSource) {
												// 縦長なら左ページの可能性
												// 左表紙は左右反転
												if (mPageWay != DEF.PAGEWAY_RIGHT) {
													int page3 = page1;
													page1 = page2;
													page2 = page3;
												}
												// 通知
												//sendMessage(mHandler, DEF.HMSG_CACHE, 0, 2, null);
												if (!ImageScaling(page1, page2, ImageData.HALF_NONE, ImageData.HALF_NONE, null, null)) {
													// スケール失敗
													fMemCacheExec = false;
												}
											}
										}
									}
									else if (isHalfView() && !DEF.checkPortrait(mFileList[chkPage].width, mFileList[chkPage].height, mScrRotate)) {
										if (mMemCacheFlag[chkPage].fSource) {
											// 左側のみ単独表示
											if (!mMemCacheFlag[chkPage].fScale[ImageData.HALF_LEFT]) {
												// 通知
												//sendMessage(mHandler, DEF.HMSG_CACHE, 0, 2, null);
												if (!ImageScaling(chkPage, -1, ImageData.HALF_LEFT, ImageData.HALF_NONE, null, null)) {
													// スケール失敗
													fMemCacheExec = false;
												}
												// スケールしたら結果によらずこれ以上探さない
												fContinue = true;
											}
											// 右側のみ単独表示
											if (!mCacheBreak && fMemCacheExec) {
												if (!mMemCacheFlag[chkPage].fScale[ImageData.HALF_RIGHT]) {
													// 通知
													//sendMessage(mHandler, DEF.HMSG_CACHE, 0, 2, null);
													if (!ImageScaling(chkPage, -1, ImageData.HALF_RIGHT, ImageData.HALF_NONE, null, null)) {
														// スケール失敗
														fMemCacheExec = false;
													}
													// スケールしたら結果によらずこれ以上探さない
													fContinue = true;
												}
											}
										}
									}
									else {
										// 単独表示
										if (mMemCacheFlag[chkPage].fSource) {
											if (!mMemCacheFlag[chkPage].fScale[ImageData.HALF_NONE]) {
												// 通知
												//sendMessage(mHandler, DEF.HMSG_CACHE, 0, 2, null);
												if (!ImageScaling(chkPage, -1, ImageData.HALF_NONE, ImageData.HALF_NONE, null, null)) {
													// スケール失敗
													fMemCacheExec = false;
												}
												// スケールしたら結果によらずこれ以上探さない
												fContinue = true;
											}
										}
									}
								}
								finally {
									// 読み込み完了
									//sendMessage(mHandler, DEF.HMSG_CACHE, -1, 0, null);
								}
							}
						}
						else {
							continue;
						}
					}
					if (iPrio >= mMemPriority.length) {
						fMemCacheExec = false;
						perf("memcache window done current=" + mCurrentPage);
					}
					// 表示用画像を作り終えたページの元画像を手放す
					dropFarSources();
//					Logcat.d(logLevel, "----  End  ----");
					if (page == -1 && perfMs(perfScan) >= 1) {
						perf("scale/scan current=" + mCurrentPage + " time=" + perfMs(perfScan) + "ms");
					}
				}
				if (fContinue) {
					// スケール作成した
					continue;
				}

				if (mCacheBreak) {
					// キャッシュ処理中断
					continue;
				}

				// キャッシュ対象ページ
				// ダウンロードスレッドがあれば、ファイルキャッシュはそちらで作る
				if (page == -1 && mHostType != DEF.ACCESS_TYPE_LOCAL && mDownloader == null) {
					// 読込ページを探す
					int startPage = mCurrentPage;
					int range;

					// 対象ページ
					page = -1;

					for (range = 0; range < CACHE_RANGE; range++) {
						if (mCloseFlag) {
							break;
						}
						if (mCacheBreak) {
							// キャッシュ処理中断
							break;
						}

						int st; // 検索範囲
						int ed;

						// 順方向
						st = startPage + CACHE_FPAGE * range + 1;
						ed = startPage + CACHE_FPAGE * (range + 1);
						if (mFileList != null && st < mFileList.length) {
							// 最終ページ以内
							if (ed >= mFileList.length) {
								// 範囲がはみ出していれば納める
								ed = mFileList.length - 1;
							}
							for (page = st; page <= ed; page++) {
								if (!cheGetCacheFlag(page)/* && memGetCacheState(page) == MEMCACHE_NONE */) {
									// 読込むページを見つけた
									break;
								}
							}
							if (page <= ed) {
								// キャッシュのないページを発見
								break;
							}
						}

						// 逆方向の割合は減らす
						st = startPage - CACHE_BPAGE * range - 1;
						ed = startPage - CACHE_BPAGE * (range + 1);
						if (st >= 0) {
							// 最終ページ以内
							if (ed < 0) {
								// 範囲がはみ出していれば納める
								ed = 0;
							}
							for (page = st; page >= ed; page--) {
								if (!cheGetCacheFlag(page)/* && memGetCacheState(page) != MEMCACHE_OK */) {
									// 読込むページを見つけた
									break;
								}
							}
							if (page >= ed) {
								// キャッシュのないページを発見
								break;
							}
						}
					}
					if (mCacheBreak) {
						// キャッシュ処理中断
						continue;
					}

					if (range >= CACHE_RANGE || page < 0 || mFileList.length <= page) {
						sleepTimer = 1000;
						continue;
					}
				}
				if (page != -1) {
					if (!mRunningFlag) {
						// closeされた場合
						break;
					}
					if (mCacheBreak) {
						// メインスレッドでビットマップの読込処理が入った
						continue;
					}

					// キャッシュ読み込みを通知
					// Logcat.d(logLevel, "Load p=" + page);
					sendMessage(mHandler, DEF.HMSG_CACHE, 0, fMemCacheWrite ? 1 : 0, null);

					if (fMemCacheWrite) {
						if (cheGetCacheFlag(page)) {
							// ファイルキャッシュあり
							mCheWriteFlag = false;
						}
						else {
							// ファイルキャッシュなしならキャッシュする
							mCheWriteFlag = true;
							long pos;
							int len;
							mLoadingPage = page;
							if (mFileType != FileData.FILETYPE_DIR) {
								pos = mFileList[page].cmppos;
								len = mFileList[page].cmplen;
							}
							else {
								pos = mFileList[page].orgpos;
								len = mFileList[page].orglen;
							}
							try {
								// ファイル書き込み準備
								cheSeek(pos, len, page);
							}
							catch (IOException e) {
								Logcat.e(logLevel, "ファイル書き込み準備に失敗しました.", e);
								// ファイルキャッシュしない
								mCheWriteFlag = false;
							}
						}

						try {
							ImageData id = LoadImage(page, false);
							if (id == null) {
								// 読み込み失敗ならメモリキャッシュを継続しない
								fMemCacheExec = false;
							}
						}
						catch (IOException e) {
							Logcat.e(logLevel, "run: LoadImageに失敗しました.", e);
						}
//							mThreadLoading = true;
//							Logcat.d(logLevel, "---- Start ----");n

					}
					else {
						// ファイルキャッシュする
						downloadToFileCache(page, false, true);
					}

					// キャッシュ読み込み完了を通知
					sendMessage(mHandler, DEF.HMSG_CACHE, -1, 0, null);
				}
				else {
					sleepTimer = 1000;
				}
			}
		}
		mTerminate = true;
		Logcat.d(logLevel, "終了します.");
	}

	private void sendMessage(Handler handler, int what, int arg1, int arg2, Object obj) {
//		Logcat.d(logLevel, "arg=" + arg1 + ", " + arg2);
		Message message = new Message();
		message.what = what;
		message.arg1 = arg1;
		message.arg2 = arg2;
		message.obj = obj;
		handler.sendMessage(message);
	}

	// 見開きモードか？
	private boolean isDualView() {
		if (mScrDispMode == DISPMODE_DUAL) {
			return true;
		}
		else if (mScrDispMode == DISPMODE_EXCHANGE) {
			if (!DEF.checkPortrait(mScrWidth, mScrHeight)) {
				return true;
			}
		}
		return false;
	}

	// 単ページモードか？
	private boolean isHalfView() {
		if (mScrDispMode == DISPMODE_HALF) {
			return true;
		}
		else if (mScrDispMode == DISPMODE_EXCHANGE) {
			if (DEF.checkPortrait(mScrWidth, mScrHeight)) {
				return true;
			}
		}
		return false;
	}

	public int getHostType() {
		return mHostType;
	}

	// ZIP内のファイル情報を返す
	public FileListItem[] getList() {
		return mFileList;
	}

	public int search(String name) {
		if (name != null && !name.isEmpty() /* mFileType == FILETYPE_DIR */) {
			for (int i = 0; i < mFileList.length; i++) {
				if (mFileList[i].name.equals(name)) {
					return i;
				}
			}
		}
		return -1;
	}

	// 現在ページを設定
	public void setCurrentPage(int page, boolean single) {
		// キャッシュ範囲などで使用
		mCurrentPage = page;
		mCurrentSingle = single;
		wakeCacheThread();
		FileCacheDownloader downloader = mDownloader;
		if (downloader != null) {
			downloader.wake();
		}
	}

	/**
	 * ページ一覧のサムネイル用に、ページの圧縮データをファイルキャッシュに揃える.
	 * mLock の外で呼ぶ (待っている間もロックを持たないので、ページ移動などを妨げない).
	 * 揃えば、続く LoadThumbnail はサーバではなくファイルキャッシュから読む.
	 * @return 揃った、または揃える必要がない (ローカルなど) なら true
	 */
	public boolean prepareThumbnail(int page, FileCacheDownloader.Cancel cancel) {
		FileCacheDownloader downloader = mDownloader;
		if (downloader == null || mFileList == null || page < 0 || page >= mFileList.length || !needDownload(page)) {
			return true;
		}
		long perfStart = System.nanoTime();
		boolean done = downloader.waitPage(page, FileCacheDownloader.PRIORITY_THUMB, cancel, null);
		perf("thumb wait page=" + page + (done ? " done" : " NOT-DONE") + " time=" + perfMs(perfStart) + "ms");
		return done;
	}

	/** ページ一覧でこれからサムネイルを作るページを知らせる (null で解除) */
	public void setThumbnailPages(int[] pages) {
		FileCacheDownloader downloader = mDownloader;
		if (downloader != null) {
			downloader.setThumbnailPages(pages);
		}
	}

	// ビットマップ読み込み開始
	public ImageData getImageData(int page) {
		// パラメタチェック
		if (mFileList == null || page < 0 || mFileList.length <= page) {
			return null;
		}

		ImageData id = null;
		if (mMemCacheFlag[page].hasAny()) {
			// メモリキャッシュあり (元画像を手放して表示用画像だけのこともある)
			id = new ImageData();
			id.Page = page;
			id.Width = mFileList[page].width;
			id.Height = mFileList[page].height;
			for (int i = 0; i < 3; i++) {
				if (mMemCacheFlag[page].fScale[i]) {
					id.HalfMode = i;
					id.SclWidth = mFileList[page].swidth[i];
					id.SclHeight = mFileList[page].sheight[i];
					id.FitWidth = mFileList[page].fwidth[i];
					id.FitHeight = mFileList[page].fheight[i];
				}
			}
		}
		return id;
	}

	// ビットマップ読み込み開始
	public ImageData loadBitmap(int page, boolean notice) throws IOException {
		int logLevel = Logcat.LOG_LEVEL_WARN;
		// パラメタチェック
		if (mFileList == null || page < 0 || mFileList.length <= page) {
			return null;
		}

		ImageData id = null;
		long perfStart = System.nanoTime();
		userOpStart();
		try {
			mCacheBreak = true;
			CallImgLibrary.ImageCancel(mActivity, mHandler, mCacheIndex, 1);
			synchronized (mLock) {
				long perfLockWait = perfMs(perfStart);
				boolean perfMemHit = !mCloseFlag && mMemCacheFlag[page].hasAny();
				if (!mCloseFlag) {
					mCacheBreak = false;
					CallImgLibrary.ImageCancel(mActivity, mHandler, mCacheIndex, 0);
					mThreadLoading = false;
					if (mMemCacheFlag[page].hasAny()) {
						// メモリキャッシュあり. 元画像がなくても表示用画像があれば読み込まない
						// (並びが変わるなどで作り直すときは ImageScaling が元画像を読み直す)
						id = new ImageData();
						id.Page = page;
						id.Width = mFileList[page].width;
						id.Height = mFileList[page].height;
					} else {
						// メモリキャッシュ無しなので読み込み
						mCheWriteFlag = false;
						if (mCacheMode != CACHEMODE_FILE && mHostType != DEF.ACCESS_TYPE_LOCAL) {
							// メモリキャッシュに保存できない場合はファイルキャッシュする
							mCheWriteFlag = true;
							long pos;
							int len;
							if (mFileType != FileData.FILETYPE_DIR) {
								pos = mFileList[page].cmppos;
								len = mFileList[page].cmplen;// + SIZE_CENTHEADER + SIZE_TERMHEADER;
							} else {
								pos = mFileList[page].orgpos;
								len = mFileList[page].orglen;
							}
							try {
								cheSeek(pos, len, page);
							} catch (IOException e) {
								Logcat.e(logLevel, "cheSeek Catch Exeption. ", e);
								mCheWriteFlag = false;
							}
						}
						if (!mCloseFlag) {
							id = LoadImage(page, notice);
						}
					}
					mThreadLoading = true;
				}
				perf("user page=" + page + (perfMemHit ? " memhit" : " miss") + " lockwait=" + perfLockWait + "ms total=" + perfMs(perfStart) + "ms");
			}
		}
		finally {
			userOpEnd();
		}
		return id;
	}

	// ビットマップ読み込み開始
	public void setLoadBitmapStart(int page, boolean notice) throws IOException {
		int logLevel = Logcat.LOG_LEVEL_WARN;
		Logcat.d(logLevel, "開始します. page=" + page + ", notice=" + notice);

		int from;

		// 読み込み位置を設定
		long pos;
		int len;
		if (mFileType != FileData.FILETYPE_DIR) {
			Logcat.d(logLevel, "FILETYPE_DIR以外 1");
			pos = mFileList[page].cmppos;
			len = mFileList[page].cmplen;// + SIZE_CENTHEADER + SIZE_TERMHEADER;
			mLoadingPage = page;
		}
		else {
			Logcat.d(logLevel, "FILETYPE_DIR 1");
			pos = mFileList[page].orgpos;
			len = mFileList[page].orglen;
		}
		mDataSize = len;

		if (cheGetCacheFlag(page)) {
			// ファイルキャッシュに存在
			Logcat.d(logLevel, "キャッシュにヒットした");
			mCacheMode = CACHEMODE_FILE;
			from = FROMTYPE_CACHE;
			// キャッシュファイルの読込設定
			cheSeek(pos, len, page);
		}
		else {
			// キャッシュに存在しない
			Logcat.d(logLevel, "キャッシュにヒットしなかった");
			mCacheMode = CACHEMODE_NONE;
			if (mHostType != DEF.ACCESS_TYPE_LOCAL) {
				Logcat.d(logLevel, "ローカル以外");
				from = FROMTYPE_SERVER;
			}
			else {
				Logcat.d(logLevel, "ローカル");
				from = FROMTYPE_LOCAL;
			}
			// 元ファイルの読込設定
			if (mFileType == FileData.FILETYPE_DIR) {
				Logcat.d(logLevel, "FILETYPE_DIR 2");
				dirSetPage(DEF.relativePath(mActivity, mFilePath, mFileList[page].name));
				if (mHostType != DEF.ACCESS_TYPE_LOCAL)
					cmpSeek(0, len);
			}
			else {
				Logcat.d(logLevel, "FILETYPE_DIR以外 2");
				cmpSeek(pos, len);
			}
		}

//		if (mCacheMode != CACHEMODE_MEM && mFileList[page].len + BIS_BUFFSIZE < mMemCache.length) {
//			mMemCacheSave = true;
//		}
//		else {
//			mMemCacheSave = false;
////			mMemCachePage = -1;
//		}
		if (!mThreadLoading && notice) {
			Logcat.d(logLevel, "mThreadLoading=false");
			// 0%
			sendHandler(DEF.HMSG_LOADING, from, 0, null);
		}

		mStartTime = System.currentTimeMillis();
		mReadSize = 0;
		mMsgCount = 0;
	}

	// ビットマップ読み込み終了
	public void setLoadBitmapEnd() throws IOException {
//		if (page != -1 && mMemWriteFlag) {
//			// 同時キャッシュ保存モードで正常終了した場合
//			cheSetCacheFlag(page);
//		}
		if (mFileType != FileData.FILETYPE_DIR) {
			// mFile.endPage();
		}
		else {
			dirEndPage();
		}
//		if (mMemCacheSave) {
//			// キャッシュをためた
//			mMemCacheLen = mMemCachePos;
//			mMemCachePage = page;
//		}
	}

	@Override
	public int read() throws IOException {
		return 0;
	}

	@Override
	public int read(byte[] buf, int off, int len) throws IOException {
		int logLevel = Logcat.LOG_LEVEL_WARN;
		Logcat.d(logLevel, MessageFormat.format("開始します. off={0}, len={1}", new Object[]{off, len}));
		if (isCacheThreadCanceled()) {
			// 画面側の読み込み要求が来たので、キャッシュスレッドのデコードを打ち切ってロックを空ける
			throw new InterruptedIOException("cache break");
		}
		int ret = 0;
		try {
			if (mCacheMode == CACHEMODE_FILE) {
				Logcat.d(logLevel, "CACHEMODE_FILE:");
				// ファイルキャッシュに存在する
				ret = cheRead(buf, off, len);
				Logcat.d(logLevel, MessageFormat.format("CACHEMODE_FILE: ret={0}", new Object[]{ret}));
			}
			else {
				// キャッシュに存在しない
				if (mFileType == FileData.FILETYPE_DIR || mFileType == FileData.FILETYPE_IMG) {
					Logcat.d(logLevel, "FILETYPE_DIR || FILETYPE_IMG:");
					ret = dirRead(buf, off, len);
					Logcat.d(logLevel, MessageFormat.format("FILETYPE_DIR || FILETYPE_IMG: ret={0}", new Object[]{ret}));
				}
				else {
					Logcat.d(logLevel, "OTHER:");
					ret = cmpRead(buf, off, len);
					Logcat.d(logLevel, MessageFormat.format("OTHER: ret={0}", new Object[]{ret}));
				}
			}
		}
		catch (Exception e) {
			if (mCloseFlag) {
				Logcat.w(logLevel, "キャンセルされました.", e);
			}
			else {
				Logcat.e(logLevel, "", e);
			}

			if (!mThreadLoading) {
				// ユーザ操作による読み込みの場合
				Message message = new Message();
				message.what = DEF.HMSG_ERROR;
				message.obj = e.getLocalizedMessage();
				mHandler.sendMessage(message);
			}
			// 壊れたデータを正常として返さず、呼び出し元に失敗を伝える
			if (e instanceof IOException) {
				throw (IOException) e;
			}
			throw new IOException(e.getLocalizedMessage(), e);
		}

		if (ret > 0) {
			mReadSize += ret;
		}
		long nowTime = System.currentTimeMillis();
		// ちょっとだけ更新頻度を増やしてみる
		if (!mThreadLoading && nowTime - mStartTime > (mMsgCount + 1) * 500) {
			mMsgCount++;
			int prog = mDataSize > 0 ? (int) ((long) mReadSize * 100 / mDataSize) : 0;
			int rate = (int) ((long) mReadSize * 10 / (nowTime - mStartTime));
			sendHandler(DEF.HMSG_LOADING, prog << 24 | 0x0100 | mFromType, rate, null);
		}
		Logcat.d(logLevel, MessageFormat.format("終了します. ret={0}", new Object[]{ret}));
		return ret;
	}

	@Override
	public void close() throws IOException {
		int logLevel = Logcat.LOG_LEVEL_WARN;
		Logcat.d(logLevel, "開始します. mCloseFlag=" + mCloseFlag);
		// 読み込みループを即座に止める
		mRunningFlag = false;
		wakeCacheThread();
		FileCacheDownloader downloader = mDownloader;
		if (downloader != null) {
			mDownloader = null;
			downloader.close();
		}
		if (!mCloseRequested.compareAndSet(false, true)) {
			// 既に閉じている
			return;
		}

		// たまに時間がかかるので非同期処理にする
		// 意図的にExceptionを発生させるためスレッドセーフにしない
		ExecutorService executor = BackgroundExecutor.get();
		executor.submit(new Runnable() {
			@Override
			public void run() {
				mRunningFlag = false;
				if (!mCloseFlag) {
					Logcat.v(logLevel, "mCloseFlag=false");
					mCloseFlag = true;
					if (mThread != null) {
						Logcat.v(logLevel, "mThread.interrupt()");
						mThread.interrupt();
						// スレッドの終了待ち
						for (int i = 0; i < 10 && !mTerminate; i++) {
							try {
								Thread.sleep(100);
							} catch (InterruptedException ignored) {

							}
						}
					} else {
						Logcat.v(logLevel, "mTerminate=true");
						mTerminate = true;
					}

					try {
						Logcat.v(logLevel, "cheClose()");
						cheClose();
						cmpClose();
						dirClose();
					}
					catch (IOException e) {
						// なにもしない
					}

					if (mRarStream != null) {
						mRarStream.close();
						mRarStream = null;
					}
					if (mPdfRenderer != null) {
						mPdfRenderer.close();
						mPdfRenderer = null;
					}
					Logcat.v(logLevel, "CallImgLibrary.ImageTerminate(mActivity, mHandler, mCacheIndex=" + mCacheIndex + ")");
					int returnCode = CallImgLibrary.ImageTerminate(mActivity, mHandler, mCacheIndex);
					Logcat.v(logLevel, "returnCode=" + returnCode);

					// デコード用の作業Bitmapを解放(デコード中に解放しないよう mLock を取る)
					synchronized (mLock) {
						if (mDecodeBitmap != null) {
							if (!mDecodeBitmap.isRecycled()) {
								mDecodeBitmap.recycle();
							}
							mDecodeBitmap = null;
						}
					}
				}
				else {
					Logcat.w(logLevel, "mCloseFlag=true");
				}
			}
		});

		Logcat.d(logLevel, "終了します. mCloseFlag=" + mCloseFlag);
	}

	// 4バイト数値取得
	public int getInt(byte[] b, int pos) {
		int val;
		val = ((int) b[pos] & 0x000000FF) | (((int) b[pos + 1] << 8) & 0x0000FF00) | (((int) b[pos + 2] << 16) & 0x00FF0000) | (((int) b[pos + 3] << 24) & 0xFF000000);

		return val;
	}

	// 2バイト数値取得
	public short getShort(byte[] b, int pos) {
		int val;
		val = ((int) b[pos] & 0x000000FF) | (((int) b[pos + 1] << 8) & 0x0000FF00);

		return (short) val;
	}

	// 通知
	private void sendHandler(int id, int arg1, int arg2, Object data) {
		Message message = new Message();
		message.what = id;
		message.arg1 = arg1;
		message.arg2 = arg2;
		message.obj = data;
		mHandler.sendMessage(message);
	}

	/*************************** FileCache ***************************/
	private RandomAccessFile mCheRndFile;
	private int mChePage;
	private int mCheSize;
	private int mChePos;
	private boolean[] mCheCacheFlag;
	private final Object mCheFlagLock = new Object();
	private boolean mCheEnable;

	public void fileCacheInit(int total, boolean isEnable) throws IOException {
		int logLevel = Logcat.LOG_LEVEL_WARN;
		Logcat.d(logLevel, "開始します. total=" + total + ", isEnable=" + isEnable);
		mCheEnable = isEnable;

		// サーバアクセス時はファイルキャッシュも行う
		if (isEnable) {
			// キャッシュ読込モードオン
			String file = cacheFilePath();
			String path = DEF.getBaseDirectory() + "thumb/";
			try {
				new File(path).mkdirs();
				new File(file).delete();
				mCheRndFile = new RandomAccessFile(file, "rw");
			}
			catch (Exception e) {
				mCheEnable = false;
				Logcat.d(logLevel, "", e);
				Message message = new Message();
				message.what = DEF.HMSG_ERROR;
				message.obj = "Open Error.(" + path + ")";
				mHandler.sendMessage(message);
			}
		}

		// 参照先
		if (mCheEnable) {
			mCheCacheFlag = new boolean[total];
			// キャッシュ済みフラグ初期化
            Arrays.fill(mCheCacheFlag, false);
		}
		Logcat.d(logLevel, "終了します.");
	}

	private static String cacheFilePath() {
		return DEF.getBaseDirectory() + "comittona.cache";
	}

	// ダウンロードスレッドからも更新されるのでロックを取る
	// (フラグが立ったら、ダウンロード側で確定したエントリサイズも見えるようにするため)
	public void cheSetCacheFlag(int index) {
		synchronized (mCheFlagLock) {
			if (mCheCacheFlag != null && index >= 0 && index < mCheCacheFlag.length) {
				mCheCacheFlag[index] = true;
			}
		}
	}

	public boolean cheGetCacheFlag(int index) {
		synchronized (mCheFlagLock) {
			if (mCheCacheFlag != null && index >= 0 && index < mCheCacheFlag.length) {
				return mCheCacheFlag[index];
			}
			return false;
		}
	}

	public int cheRead(byte[] buf, int off, int len) throws IOException {
		if (!mRunningFlag) {
			throw new IOException("ImageManaget: cheRead: User Canceled.");
		}
		if (!mCheEnable) {
			return -1;
		}
		if (mCheSize == mChePos) {
			return -1;
		}
		int ret = 0;
		int size = len;
		if (size > mCheSize - mChePos) {
			size = mCheSize - mChePos;
		}
		if (size > 0) {
			ret = mCheRndFile.read(buf, off, size);
		}
		if (mChePos == 0 && mFileType == FILETYPE_ZIP) {
			// SHIFT-JISで読込み
			if (ret >= OFFSET_LCL_FNAME_LEN + 2) {
				int lenFName = getShort(buf, OFFSET_LCL_FNAME_LEN);

				if (ret >= SIZE_LOCALHEADER + lenFName) {
					for (int i = 0; i < lenFName - 4; i++) {
						buf[off + SIZE_LOCALHEADER + i] = '0';
					}
				}
			}
		}
		mChePos += ret;
		return ret;
	}

	public void cheWrite(byte[] buf, int off, int len) throws IOException {
		if (!mCheEnable) {
			return;
		}

		if (len > mCheSize - mChePos) {
			len = mCheSize - mChePos;
		}
		if (len > 0) {
			mCheRndFile.write(buf, off, len);
			mChePos += len;
		}
		if (mCheSize == mChePos) {
			cheSetCacheFlag(mChePage);
//			Logcat.d(logLevel, "Page:" + mChePage + " Cache OK (" + mCheSize + "/" + mChePos + ")" + (mThreadLoading ? "Sub" : "Main"));
		}
	}

	public void cheSeek(long pos, int size, int page) throws IOException {
		if (!mCheEnable) {
			return;
		}
		//Logcat.d(logLevel, "開始します.");
		// エントリーサイズ
		mCheSize = size;
		mChePos = 0;
		mChePage = page;
		if (mFileType != FileData.FILETYPE_PDF) {
			mCheRndFile.seek(pos);
		}
		//Logcat.d(logLevel, "終了します.");
	}

	public void cheClose() throws IOException {
		if (mCheRndFile != null) {
			mCheRndFile.close();
			mCheRndFile = null;
		}
	}

	/**
	 * 先にファイルキャッシュへ落としてからデコードするページか.
	 * サーバ上の ZIP / RAR で、まだキャッシュしていないページが対象.
	 */
	private boolean needDownload(int page) {
		return mCheEnable && mHostType != DEF.ACCESS_TYPE_LOCAL
				&& (mFileType == FILETYPE_ZIP || mFileType == FILETYPE_RAR)
				&& !cheGetCacheFlag(page);
	}

	/**
	 * ページの圧縮データをファイルキャッシュに揃える. mLock 配下で呼ぶこと.
	 * ダウンロードスレッドがあればその完了を待ち (ダウンロード側は mLock を取らない)、なければこのスレッドで読む.
	 * @param interruptible true ならキャッシュスレッドからの呼び出しとして、画面側の読み込み要求 (mCacheBreak) で中断する.
	 *                      false なら画面側の要求として最優先で読ませる
	 * @return キャッシュが揃ったら true
	 */
	private boolean ensureFileCache(int page, boolean notice, boolean interruptible) {
		FileCacheDownloader downloader = mDownloader;
		if (downloader == null || downloader.isFailed(page)) {
			return downloadToFileCache(page, notice, interruptible);
		}
		long perfStart = System.nanoTime();
		final boolean progress = notice && !mThreadLoading;
		if (progress) {
			// 0%
			sendHandler(DEF.HMSG_LOADING, FROMTYPE_SERVER, 0, null);
		}
		final long startTime = System.currentTimeMillis();
		boolean done = downloader.waitPage(page, interruptible ? FileCacheDownloader.PRIORITY_WANT : FileCacheDownloader.PRIORITY_URGENT,
				() -> !mRunningFlag || mCloseFlag || (interruptible && (mCacheBreak || mCacheSleep)),
				!progress ? null : (doneBytes, total) -> {
					long elapsed = Math.max(1, System.currentTimeMillis() - startTime);
					int prog = total > 0 ? (int) (doneBytes * 100 / total) : 0;
					int rate = (int) (doneBytes * 10 / elapsed);
					sendHandler(DEF.HMSG_LOADING, prog << 24 | 0x0100 | FROMTYPE_SERVER, rate, null);
				});
		perf("wait page=" + page + " current=" + mCurrentPage + (done ? " done" : " NOT-DONE") + " time=" + perfMs(perfStart) + "ms");
		if (!done && downloader.isFailed(page) && mRunningFlag && !(interruptible && (mCacheBreak || mCacheSleep))) {
			// ダウンロードスレッドが諦めたページはこのスレッドで読む
			return downloadToFileCache(page, notice, interruptible);
		}
		return done;
	}

	/**
	 * ページの圧縮データを展開せずにファイルキャッシュへ書き込む. mLock 配下で呼ぶこと.
	 * @param interruptible true なら画面側の読み込み要求 (mCacheBreak) で中断する
	 * @return キャッシュが揃ったら true
	 */
	private boolean downloadToFileCache(int page, boolean notice, boolean interruptible) {
		int logLevel = Logcat.LOG_LEVEL_WARN;
		if (cheGetCacheFlag(page)) {
			return true;
		}
		long pos;
		int len;
		if (mFileType != FileData.FILETYPE_DIR) {
			pos = mFileList[page].cmppos;
			len = mFileList[page].cmplen;
		}
		else {
			pos = mFileList[page].orgpos;
			len = mFileList[page].orglen;
		}

		perfReset();
		long perfStart = System.nanoTime();
		byte[] buf = mReadBuffer;
		try {
			// ファイル読み込み準備
			setLoadBitmapStart(page, notice);
			mCheWriteFlag = true;
			cheSeek(pos, len, page);

			// ZIP は最初の読み込みでローカルヘッダの拡張フィールドぶんサイズが伸びるので、
			// 残りサイズではなくキャッシュ済みフラグか終端で止める
			while (mRunningFlag && !mCloseFlag && !cheGetCacheFlag(page)) {
				if (interruptible && (mCacheBreak || mCacheSleep)) {
					// メインスレッドでビットマップの読込処理が入った
					break;
				}
				if (read(buf, 0, buf.length) <= 0) {
					// ファイル終端
					break;
				}
			}
		}
		catch (Exception e) {
			Logcat.e(logLevel, "", e);
		}
		finally {
			mCheWriteFlag = false;
			try {
				setLoadBitmapEnd();
			}
			catch (IOException e) {
				Logcat.e(logLevel, "", e);
			}
		}
		boolean done = cheGetCacheFlag(page);
		perf("download page=" + page + " current=" + mCurrentPage + " len=" + len + (done ? " done" : " ABORTED") + " time=" + perfMs(perfStart) + "ms " + perfNet());
		return done;
	}

	/*************************** CompressAccess ***************************/
	private WorkStream mWorkStream;
	private int mCmpSize;
	private int mCmpPos;

	public void fileAccessInit(String uri) throws IOException {
		int logLevel = Logcat.LOG_LEVEL_WARN;
		Logcat.d(logLevel, "開始します. uri=" + uri);
		// 参照先
		mWorkStream = new WorkStream(mActivity, uri, mUser, mPass, mHandler);
		Logcat.d(logLevel, "終了します. uri=" + uri);
	}

	public int cmpDirectRead(byte[] buf, int off, int len) throws IOException {
		return mWorkStream.read(buf, off, len);
	}

	public void cmpDirectSeek(long pos) throws IOException {
		int logLevel = Logcat.LOG_LEVEL_WARN;
		Logcat.d(logLevel, "開始します.");
		// エントリーサイズ
		mWorkStream.seek(pos);
	}

	public long cmpDirectTell() throws IOException {
		// エントリーサイズ
		return mWorkStream.getFilePointer();
	}

	public long cmpDirectLength() throws IOException {
		int logLevel = Logcat.LOG_LEVEL_WARN;
		Logcat.d(logLevel, "開始します.");
		long fileLength = 0;

		// エントリーサイズ
		if (mWorkStream != null) {
			fileLength = mWorkStream.length();
		}
		else {
			Logcat.e(logLevel, "mWorkStream が null.");
		}

		if ((fileLength & 0xFFFFFFFF00000000L) == fileLength) {
			fileLength = (fileLength >> 32) & 0x00000000FFFFFFFFL;
		}
		Logcat.d(logLevel, "終了します. filelength=" + fileLength);
		return fileLength;
	}

	public int cmpRead(byte[] buf, int off, int len) throws IOException {
		if (!mRunningFlag) {
			throw new IOException(TAG + ": cmpDirectLength: User Canceled.");
		}
		else if (mCmpSize <= mCmpPos) {
			// throw new IOException("This file format is not supported.");
			return -1;
		}

		int ret = 0;
		int size = len;
		if (size > mCmpSize - mCmpPos) {
			size = mCmpSize - mCmpPos;
		}
		if (size > 0) {
			long perfStart = System.nanoTime();
			ret = mWorkStream.read(buf, off, size);
			mPerfNetNanos += System.nanoTime() - perfStart;
			mPerfNetCalls++;
			if (ret > 0) {
				mPerfNetBytes += ret;
			}
		}
		if (ret <= 0) {
			return -1;
		}
		if (mCmpPos == 0 && mFileType == FILETYPE_ZIP) {
			// SHIFT-JISで読込み
			if (ret >= OFFSET_LCL_FNAME_LEN + 2) {
				int lenFName = getShort(buf, OFFSET_LCL_FNAME_LEN);

				if (ret >= SIZE_LOCALHEADER + lenFName) {
					String name = DEF.toUTF8(buf, SIZE_LOCALHEADER, lenFName);
					for (int i = 0; i < lenFName - 4; i++) {
						buf[off + SIZE_LOCALHEADER + i] = '0';
					}
					// セントラルディレクトリヘッダからはExtraSizeが取得出来ないので
					// ここでローカルヘッダの情報を取得して適宜更新する
					if(!mFileList[mLoadingPage].sizefixed) {
						mCmpSize += getExtraSize(buf);
						mFileList[mLoadingPage].cmplen = mCmpSize;
						mFileList[mLoadingPage].sizefixed = true;
					}
					if(mCheWriteFlag)
						mCheSize = mCmpSize;
				}
			}
		}
		if (mCheWriteFlag) {
			// ファイルにキャッシュ
			cheWrite(buf, off, ret);
		}
		mCmpPos += ret;
		return ret;
	}

	public void cmpSeek(long pos, int size) throws IOException {
		int logLevel = Logcat.LOG_LEVEL_WARN;
		Logcat.d(logLevel, "開始します.");
		// エントリーサイズ
		mCmpSize = size;
		mCmpPos = 0;
		if (mFileType != FileData.FILETYPE_PDF) {
			if (mWorkStream != null) {
				Logcat.d(logLevel, "mWorkStream != null");
				mWorkStream.seek(pos);
			}
			else {
				if (mCloseFlag) {
					Logcat.w(logLevel, "キャンセルされました. mWorkStream == null");
				}
				else {
					Logcat.e(logLevel, "mWorkStream == null");
				}
				throw new IOException();
			}
		}
	}

	public void cmpClose() throws IOException {
		if (mWorkStream != null) {
			mWorkStream.close();
			mWorkStream = null;
		}
		if (mFileType == FILETYPE_RAR) {
			// RARの領域解放
			long handle = mRarHandle;
			mRarHandle = 0;
			if (handle != 0) {
				CallJniLibrary.rarClose(handle);
			}
		}
	}

	/*************************** DirAccess ***************************/
	private BufferedInputStream mDirStream;
	private ArrayList<FileData> mFiles;

	private int mDirIndex;
	private int mDirOrgPos;

	public void dirListFiles(String uri, String user, String pass) throws IOException {
		int logLevel = Logcat.LOG_LEVEL_WARN;
		Logcat.d(logLevel, "開始します. uri=" + uri);
		mDirIndex = 0;
		mDirOrgPos = 0;
		try {
			mFiles = FileAccess.listFiles(mActivity, uri, user, pass, mHandler);
		}
		catch (FileAccessException e) {
			throw new IOException(TAG + ": dirListFiles: " + e.getLocalizedMessage());
		}
	}

	public FileListItem dirGetFileListItem() throws IOException {
		int logLevel = Logcat.LOG_LEVEL_WARN;
		Logcat.d(logLevel, "開始します.");
		while (true) {
			FileData fileData = null;
			String name = "";
			String uri = "";
			boolean isDirectory = false;
			long size = 0;

			if (mDirIndex < 0 || mFiles == null || mFiles.size() <= mDirIndex) {
				break;
			}
			fileData = mFiles.get(mDirIndex);
			name = fileData.getName();
			isDirectory = name.endsWith("/");
			size = fileData.getSize();

			mDirIndex++;
			if (!isDirectory) {
				// 通常のファイル
				if (mHidden && DEF.checkHiddenFile(name)) {
					continue;
				}

				short type = FileData.getType(mActivity, name);
				short exttype = FileData.getExtType(mActivity, name);
				boolean use = true;
				if (type == FileData.FILETYPE_TXT && mOpenMode != OPENMODE_LIST && mOpenMode != OPENMODE_TEXTVIEW) {
					use = false;
				}
				else if (type == FileData.FILETYPE_EPUB_SUB && mOpenMode != OPENMODE_TEXTVIEW) {
					use = false;
				}
				else if (type == FileData.FILETYPE_ARC || type == FileData.FILETYPE_EPUB || type == FileData.FILETYPE_PDF || type == FileData.FILETYPE_NONE) {
					use = false;
				}

				if (use) {
					Logcat.d(logLevel, "mDirIndex=" + mDirIndex + ", name=" + name);
					FileListItem file = new FileListItem();
					file.name = name;
					file.type = type;
					file.exttype = exttype;
					file.cmppos = 0;
					file.orgpos = mDirOrgPos;
					file.cmplen = 0;
					file.orglen = (int) size;
					mDirOrgPos += size;
					return file;
				}
			}
		}
		return null;
	}

	public void dirSetPage(String imagefile) throws IOException {
		mWorkStream = new WorkStream(mActivity, imagefile, mUser, mPass, mHandler);
	}

	public void dirEndPage() throws IOException {
		if (mWorkStream != null) {
			mWorkStream.close();
			mWorkStream = null;
		}
	}

	public int dirRead(byte[] buf, int off, int len) throws IOException {
		int logLevel = Logcat.LOG_LEVEL_WARN;
		if (!mRunningFlag) {
			Logcat.e(logLevel, "User Canceled.");
			throw new IOException(TAG + ": dirEndPage: User Canceled.");
		}

		int ret = mWorkStream.read(buf, off, len);

		if (mCheWriteFlag) {
			// ファイルにキャッシュ
			cheWrite(buf, off, ret);
		}
		return ret;
	}

	public void dirClose() throws IOException {
		if (mWorkStream != null) {
			mWorkStream.close();
			mWorkStream = null;
		}
	}

	/*************************** MemoryCache ***************************/
	public static final byte MEMCACHE_NONE = 0;
//	public static final byte MEMCACHE_LOCK  = 1;
	public static final byte MEMCACHE_ORG = 2;
	public static final byte MEMCACHE_SCALE = 3;
//	public static final byte MEMCACHE_CHECK = 4;

	private int mMemSize = 0;
	private int mMemPrevPages = 0;
	private int mMemNextPages = 0;
	private MemCacheFlag[] mMemCacheFlag;
	private int[] mMemPriority;

	private static class MemCacheFlag {
		public boolean fSource = false;
		public boolean[] fScale = { false, false, false };
		// 表示用画像を作ったときに並べたページ (単ページなら -1). 元画像なしで使い回せるかの判定用
		public int[] partner = { -1, -1, -1 };

		boolean hasScale() {
			return fScale[0] || fScale[1] || fScale[2];
		}

		boolean hasAny() {
			return fSource || hasScale();
		}
	}

	/**
	 * 表示中でないページの元画像を手放し、表示用画像だけ残す.
	 * 元画像は 1 ピクセル 4 バイトで画面よりずっと大きいことが多く、残すと先読みできる枚数が減る.
	 * 元画像が必要になったら (ルーペ表示、表示用画像の作り直し) ファイルキャッシュから読み直す.
	 */
	private void dropFarSources() {
		if (mMemCacheFlag == null || mFileList == null) {
			return;
		}
		int count = 0;
		for (int i = 0; i < mMemCacheFlag.length; i++) {
			MemCacheFlag flag = mMemCacheFlag[i];
			if (flag.fSource && flag.hasScale() && !isDisplayPage(i) && i != mScalingPage1 && i != mScalingPage2) {
				if (CallImgLibrary.ImageFreeSource(mActivity, mHandler, mCacheIndex, i) == 0) {
					flag.fSource = false;
					count++;
				}
			}
		}
		if (count > 0) {
			perf("drop sources count=" + count + " current=" + mCurrentPage + " free=" + CallImgLibrary.ImageGetFreeSize(mActivity, mHandler, mCacheIndex));
		}
	}

	// ImageScaling 中のページ. 並べるページの元画像を途中で手放さないため
	private int mScalingPage1 = -1;
	private int mScalingPage2 = -1;

	/** 元画像を持っていない表示中のページ. なければ -1 */
	private int displayPageWithoutSource() {
		int[] pages = { mCurrentPage, mCurrentPage + 1 };
		for (int p : pages) {
			if (0 <= p && p < mMemCacheFlag.length && isDisplayPage(p) && !mMemCacheFlag[p].fSource) {
				return p;
			}
		}
		return -1;
	}

	/** 元画像がなければ読み直す. mLock 配下で呼ぶこと */
	private boolean ensureSource(int page) {
		if (page < 0 || mMemCacheFlag[page].fSource) {
			return true;
		}
		perf("reload source page=" + page + " current=" + mCurrentPage);
		try {
			return LoadImage(page, false) != null;
		}
		catch (IOException e) {
			return false;
		}
	}

	/** page の half 用の表示用画像が、partner と並べた状態で作ってあるか */
	private boolean hasScaleFor(int page, int half, int partner) {
		MemCacheFlag flag = mMemCacheFlag[page];
		return flag.fScale[half] && flag.partner[half] == partner
				&& mFileList[page].swidth[half] > 0 && mFileList[page].sheight[half] > 0;
	}

	private void fillScaleInfo(ImageData img, int page, int half, int fitHalf) {
		if (img == null) {
			return;
		}
		img.SclWidth = mFileList[page].swidth[half];
		img.SclHeight = mFileList[page].sheight[half];
		img.FitWidth = mFileList[page].fwidth[fitHalf];
		img.FitHeight = mFileList[page].fheight[fitHalf];
	}

	private boolean MemoryCacheInit(int memsize, int next, int prev, int total, long maxorglen) {
		int logLevel = Logcat.LOG_LEVEL_WARN;
		Logcat.d(logLevel, "開始します. memsize=" + memsize + ", prev=" + prev + ", total=" + total + ", maxorglen=" + maxorglen);

		mMemNextPages = next;
		if (mMemNextPages == 0) {
			mMemNextPages = 1;
		}
		mMemPrevPages = prev;

		Logcat.d(logLevel, MessageFormat.format("コマンドを実行します. CallImgLibrary.ImageInitialize({0}, {1}, {2}, {3})", new Object[]{maxorglen, memsize, mFileList.length, mMaxThreadNum}));
		mCacheIndex = CallImgLibrary.ImageInitialize(mActivity, mHandler, maxorglen, memsize, mFileList.length, mMaxThreadNum);
		if (mCacheIndex < 0) {
			Logcat.e(logLevel, MessageFormat.format("メモリキャッシュの初期化に失敗しました. mCacheIndex={0}", new Object[]{mCacheIndex}));
			Logcat.w(logLevel, "メモリの解放は済んでいるが Out Of Memory が防げない.");
			Logcat.d(logLevel, "メモリ利用状況.\n" + DEF.getMemoryString(mActivity));
			return false;
		}
		Logcat.d(logLevel, MessageFormat.format("メモリキャッシュの初期化に成功しました. mCacheIndex={0}", new Object[]{mCacheIndex}));

		// 配列初期化
		mMemCacheFlag = new MemCacheFlag[total];
		for (int i = 0; i < total; i++) {
			// キャッシュ状態初期化
			mMemCacheFlag[i] = new MemCacheFlag();
		}

		// 優先順位保持
		mMemPriority = new int[prev + next + 1];
		int prevIdx = 0;
		int nextIdx = 0;
		boolean fCacheNext;
		mMemPriority[0] = 0;
		for (int i = 1; i < prev + next + 1; i++) {
			if (mMemPrevPages == 0) {
				// 前頁方向にキャッシュしない
				fCacheNext = true;
			} else if (mMemNextPages == 0) {
				// 次頁方向にキャッシュしない
				fCacheNext = false;
			} else if (nextIdx < 2 && mMemNextPages >= 2) {
				// 次の2ページだけは優先的に読込
				fCacheNext = true;
			} else if (-prevIdx * 1000 / mMemPrevPages >= nextIdx * 1000 / mMemNextPages) {
				// 次頁の読込みが少ないので読込み
				fCacheNext = true;
			} else {
				// 前頁の読込みが少ないので読込み
				fCacheNext = false;
			}

			if (fCacheNext) {
				// 次ページ方向
				nextIdx++;
				mMemPriority[i] = nextIdx;
			} else {
				// 前ページ方向
				prevIdx--;
				mMemPriority[i] = prevIdx;
			}
		}

		Logcat.d(logLevel, "終了します.");
		return true;
	}

	private MemCacheFlag memGetCacheState(int page) {
		return mMemCacheFlag[page];
	}

//	public boolean memSaveCache(int page) {
//		// イメージをメモリキャッシュに保存
//		mFileList[page].width = bm.getWidth();
//		mFileList[page].height = bm.getHeight();
////		mFileList[page].bmpsize = mFileList[page].width * mFileList[page].height * 2;
//
//		int ret = CallImgLibrary.ImageSave(page, bm);
//		if (ret == 0) {
//			mMemCacheFlag[page] = MEMCACHE_OK;
//			return true;
//		}
//		return false;
//	}

	public boolean memFreeCache(int page) {
		// メモリキャッシュを解放
		int ret = CallImgLibrary.ImageFree(mActivity, mHandler, mCacheIndex, page);
		if (ret == 0) {
			mMemCacheFlag[page].fSource = false;
			mMemCacheFlag[page].fScale[0] = false;
			mMemCacheFlag[page].fScale[1] = false;
			mMemCacheFlag[page].fScale[2] = false;
			return true;
		}
		return false;
	}

	// キャッシュ書き込みするページを指定
	public boolean memWriteLock(int page, int half, boolean sclMode) {
		if (!sclMode && mMemCacheFlag[page].fSource) {
			// 元画像読み込みなのに未キャッシュじゃない場合
			return false;
		}
//		else if (sclMode && mMemCacheFlag[page] != MEMCACHE_ORG) {
//			// スケーリング用なのに元画像のみじゃない場合
//			return false;
//		}

//		// 自身が範囲外なら終了
//		if (page < mCurrentPage - mMemPrevPages || mCurrentPage + mMemNextPages < page) {
//			return false;
//		}

		boolean fClear = true;
		int clearIdx = mMemPriority.length - 1;

		int lineCount;
		int useCount;
		if (!sclMode) {
			// 元画像モード
			lineCount = (BLOCKSIZE / (mFileList[page].width + HOKAN_DOTS));
			useCount = (int) (Math.ceil((double) mFileList[page].height / (double) lineCount));
			if (mMemCacheFlag[page].hasScale()) {
				// 元画像だけ読み直す. 表示用画像は残す
				CallImgLibrary.ImageFreeSource(mActivity, mHandler, mCacheIndex, page);
			}
			else {
				CallImgLibrary.ImageFree(mActivity, mHandler, mCacheIndex, page);
			}
		}
		else {
			// スケールモード
			lineCount = BLOCKSIZE / (mFileList[page].swidth[half] + HOKAN_DOTS);
			useCount = (int) (Math.ceil((double) mFileList[page].sheight[half] / (double) lineCount));
			CallImgLibrary.ImageScaleFree(mActivity, mHandler, mCacheIndex, page, half);
		}

		for (int loop = 0; fClear; loop++) {
			// 未使用領域で割り当て
			// ブロック数を求める
			int freeCount = CallImgLibrary.ImageGetFreeSize(mActivity, mHandler, mCacheIndex);
			if (freeCount >= useCount) {
				// 領域が足りた
			return true;
		}

			if (loop == 0) {
				// 表示中でないページの元画像を手放す (表示用画像は残るので読み直しは少ない)
				dropFarSources();
				if (CallImgLibrary.ImageGetFreeSize(mActivity, mHandler, mCacheIndex) >= useCount) {
					return true;
				}
				// 初回は範囲外を全て消す
				for (int i = 0; i < mMemCacheFlag.length; i++) {
					if (i < mCurrentPage - mMemPrevPages || mCurrentPage + mMemNextPages < i) {
						if (mMemCacheFlag[i].hasAny()) {
							// メモリ使用中であれば解放
							mMemCacheFlag[i].fSource = false;
							if (memFreeCache(i)) {
								// 解放する物があった

							}
						}
					}
				}
			}
			else {
				// 自身が範囲外なら終了
				if (page < mCurrentPage - mMemPrevPages || mCurrentPage + mMemNextPages < page) {
					return false;
				}

				// 範囲外を消しましょう
				fClear = false;
				int clr = -1;
				while (clearIdx >= 0) {
					clr = mCurrentPage + mMemPriority[clearIdx];
					clearIdx--;
					if (clr == page) {
						// ロックしたい対象ページまできてしまったらループ終了
						break;
					}
					if (0 <= clr && clr < mMemCacheFlag.length) {
						if (isDisplayPage(clr)) {
							// 表示中のページを追い出すと画面が消えるので対象外
							continue;
						}
						if (mMemCacheFlag[clr].hasAny()) {
							mMemCacheFlag[clr].fSource = false;
							if (memFreeCache(clr)) {
								// 解放する物があった
								fClear = true;
								break;
							}
						}
					}
				}
			}
		}
		// 領域不足でロックできず
		return false;
	}

	public Bitmap GetBitmapFromPath(Activity activity, String filepath, Handler handler) {
		int logLevel = Logcat.LOG_LEVEL_WARN;

		int ret = 0;
		int width = 0;
		int height = 0;
		WorkStream ws = null;
		File fileObj = null;
		int extType = 0;
		long orglen = 0;
		Bitmap bm = null;
		try {
			Logcat.d(logLevel, "イメージファイルを開きます. filepath=" + filepath);

			//String path = filepath.substring(0, filepath.lastIndexOf("/") + 1);
			//String file = filepath.substring(filepath.lastIndexOf("/") + 1);

			BitmapFactory.Options option = new BitmapFactory.Options();
			option.inJustDecodeBounds = true;

			Logcat.d(logLevel, "サイズ取得(BitmapFactory)を実行します. filepath=" + filepath);
			BitmapFactory.decodeFile(filepath, option);
			width = option.outWidth;
			height = option.outHeight;

			if (width > 0 && height > 0) {
				Logcat.d(logLevel, "サイズ取得(BitmapFactory)に成功しました.");
			} else {
				Logcat.d(logLevel, "サイズ取得(BitmapFactory)に失敗しました.");
				Logcat.d(logLevel, "WorkStreamを作成します. filepath=" + filepath);
				ws = new WorkStream(activity, filepath, "", "", handler);
				Logcat.d(logLevel, "Fileオブジェクトを作成します. uri=" + filepath);
				try {
					fileObj = new File(filepath);
				} catch (Exception e) {
					Logcat.e(logLevel, "Fileオブジェクトを作成中にエラーが発生しました.", e);
					throw new RuntimeException(e);
				}
				Logcat.d(logLevel, "ファイルサイズを取得します.");
				try {
					orglen = fileObj.length();
				} catch (Exception e) {
					Logcat.e(logLevel, "ファイルサイズの取得中にエラーが発生しました.", e);
					throw new RuntimeException(e);
				}
				if (orglen == 0) {
					Logcat.e(logLevel, "ファイルサイズの取得に失敗しました.");
				}
				Logcat.d(logLevel, "ファイルタイプを取得します.");
				extType = FileData.getExtType(activity, filepath);
				int[] imagesize = new int[2];
				Logcat.d(logLevel, "サイズ取得(Native)を実行します. type=" + extType + ", orglen=" + orglen);
				ret = SizeCheckImage(ws, -1, extType, orglen, imagesize);
				if (ret == 0 && imagesize[0] > 0 && imagesize[1] > 0) {
					Logcat.d(logLevel, "サイズ取得(Native)に成功しました.");
					width = imagesize[0];
					height = imagesize[1];
				} else {
					Logcat.e(logLevel, "サイズ取得(Native)に失敗しました.");
					return null;
				}
			}
			Logcat.d(logLevel, "イメージファイルのサイズ. width=" + width + ", height=" + height);

			// 縮小してファイル読込
			Logcat.d(logLevel, "イメージデータを取得します.");
			option.inJustDecodeBounds = false;
			option.inPreferredConfig = Config.RGB_565;
			Logcat.d(logLevel, "イメージデータ取得(BitmapFactory)を実行します. pathname=" + filepath);
			try {
				bm = BitmapFactory.decodeFile(filepath, option);
			} catch (Exception e) {
				Logcat.e(logLevel, "イメージデータ取得(BitmapFactory)でエラーが発生しました.", e);
				return null;
			}
			if (bm != null) {
				Logcat.d(logLevel, "イメージデータ取得(BitmapFactory)に成功しました.");
			} else {
				Logcat.d(logLevel, "イメージデータ取得(BitmapFactory)に失敗しました.");
				ws.seek(0);
				bm = GetBitmapNativeMain(ws, -1, extType, 1, orglen, width, height, bm);
				if (bm != null) {
					Logcat.d(logLevel, "イメージデータ取得(Native)に成功しました.");
				} else {
					Logcat.e(logLevel, "イメージデータ取得(Native)に失敗しました.");
					return null;
				}
			}
			if (bm == null) {
				Logcat.e(logLevel, "イメージファイルを取得できませんでした.");
				// NoImageであればステータス設定
				return null;
				//CallImgLibrary.ThumbnailSetNone(mID, index);
			} else {
				Logcat.d(logLevel, "イメージファイルを取得できました.");
			}
		} catch (Exception e) {
			Logcat.e(logLevel, "エラーが発生しました.", e);
			throw new RuntimeException(e);
		}
		return bm;
	}

	@SuppressLint("Range")
    private int SizeCheckImage(int page) {
		int logLevel = Logcat.LOG_LEVEL_WARN;
		Logcat.d(logLevel, "開始します. page=" + page);
		int returnCode = 0;
		//Logcat.e(logLevel, "page=" + page + ", type=" + type);
		int width = -1;
		int height = -1;

		if (page < 0 || mFileList.length <= page) {
			// 範囲外
			Logcat.e(logLevel, "pageが範囲外です. page=" + page + ", length=" + mFileList.length);
			return -1;
		}

		Logcat.v(logLevel, "page=" + page + ", name=" + mFileList[page].name + ", size=" + mFileList[page].orglen);

		if (mFileList[page].width > 0) {
			return -2;
		}

		if (mFileList[page].o_width == 0) {
			boolean fError = false;

			mCheWriteFlag = false;

			try {
				Logcat.v(logLevel, "setLoadBitmapStartを開始します.");
				setLoadBitmapStart(page, false);
				if (mFileType == FileData.FILETYPE_PDF) {
					Logcat.d(logLevel, "PDFファイルです.");
					//ページ番号を指定してPdfRenderer.Pageインスタンスを取得する。
					PdfRenderer.Page pdfPage = mPdfRenderer.openPage(page);
					if (mScrWidth == 0 || mScrHeight == 0) {
						// サムネイル作成なので、元のサイズを返す
						width = pdfPage.getWidth();
						height = pdfPage.getHeight();
					} else {
						// 取得したサイズのままだと画質が悪いため、大きなサイズに変換する
						int maxsize = Math.min(3000, Math.max(mScrWidth, mScrHeight));
						if (pdfPage.getWidth() > pdfPage.getHeight()) {
							width = maxsize;
							height = maxsize * pdfPage.getHeight() / pdfPage.getWidth();
						} else {
							width = maxsize * pdfPage.getWidth() / pdfPage.getHeight();
							height = maxsize;
						}
					}
					Logcat.v(logLevel, "PDF: pdfPage.getWidth()=" + pdfPage.getWidth() + ", pdfPage.getHeight()=" + pdfPage.getHeight() + ", " + mFileList[page].name);
					Logcat.v(logLevel, "PDF: width=" + width + ", height=" + height + ", " + mFileList[page].name);
					//PdfRenderer.Pageを閉じる、この処理を忘れると次回読み込む時に例外が発生する。
					pdfPage.close();
					if (width <= 0 || height <= 0) {
						fError = true;
					}
				}
				else {
					BufferedInputStream inputStream = openPageStream(page);
					inputStream.mark(mFileList[page].orglen + 1);
					returnCode = SizeCheckImageFromStream(page, inputStream);
					if (returnCode < 0) {
						Logcat.e(logLevel, "SizeCheckImageFromStream に失敗しました. returnCode=" + returnCode + ", " + mFileList[page].name);
					}
					else {
						width = mFileList[page].o_width;
						height = mFileList[page].o_height;
					}
				}
			}
			catch (IOException e) {
				if (mCloseFlag) {
					Logcat.w(logLevel, "キャンセルされました.", e);
				}
				else {
					Logcat.e(logLevel, "エラーになりました.", e);
				}
				fError = true;
			}

			Logcat.v(logLevel, "サイズを取得しました. width=" + width + ", height=" + height);
			try {
				setLoadBitmapEnd();
			}
			catch (Exception e) {
				Logcat.e(logLevel, "setLoadBitmapEndでエラーになりました.", e);
				fError = true;
			}

			if (fError) {
				return -5;
			}
			if (returnCode < 0) {
				return returnCode;
			}
			if (mFileType == FileData.FILETYPE_PDF) {
				applyImageSize(page, width, height);
			}
		}
		if (mFileList[page].width <= 0) {
			// 元サイズは既知で縮小率だけ未確定の場合
			applyImageSize(page, mFileList[page].o_width, mFileList[page].o_height);
		}
		Logcat.d(logLevel, "終了します. " + mFileList[page].name);
		return returnCode;
	}

	/**
	 * ページのエントリを読むストリームを開く。
	 * 呼び出し前に setLoadBitmapStart() で読み込み位置を設定しておくこと。
	 * @return mark/reset 可能な BufferedInputStream
	 */
	private BufferedInputStream openPageStream(int page) throws IOException {
		InputStream is;
		if (mFileType == FILETYPE_ZIP) {
			// メモリキャッシュ読込時のみZIP展開する
			// ファイルキャッシュを作成するときはZIP展開不要
			ZipInputStream zipStream = new ZipInputStream(new BufferedInputStream(this, BIS_BUFFSIZE));
			zipStream.getNextEntry();
			is = zipStream;
		}
		else if (mFileType == FILETYPE_RAR) {
			// メモリキャッシュ読込時のみRAR展開する
			// ファイルキャッシュを作成するときはRAR展開不要
			if (mRarStream == null || mRarStream.getLoadPage() != page) {
				mRarStream = new RarInputStream(mRarHandle, new BufferedInputStream(this, BIS_BUFFSIZE), page, mFileList[page]);
			}
			else {
				mRarStream.initSeek();
			}
			is = mRarStream;
		}
		else {
			is = this;
		}
		return new BufferedInputStream(is, BIS_BUFFSIZE);
	}

	/**
	 * 元画像サイズから縮小率と読み込み後サイズを確定する
	 */
	private void applyImageSize(int page, int width, int height) {
		mFileList[page].o_width = width;
		mFileList[page].o_height = height;
		int scale = DEF.calcScale(width, height, mFileList[page].exttype, 3200, 3200);
		scale = limitScaleByMemory(page, width, height, scale);
		mFileList[page].scale = scale;
		mFileList[page].width = DEF.divRoundUp(width, scale);
		mFileList[page].height = DEF.divRoundUp(height, scale);
	}

	/**
	 * 画像1枚がキャッシュブロック何個ぶんになるかを返す(ネイティブ側の詰め方と同じ計算)
	 */
	private static int calcBlocksFor(int width, int height) {
		if (width <= 0 || height <= 0) {
			return 0;
		}
		int linesPerBlock = BLOCKSIZE / (width + HOKAN_DOTS);
		if (linesPerBlock <= 0) {
			return Integer.MAX_VALUE;
		}
		return (height + linesPerBlock - 1) / linesPerBlock;
	}

	/**
	 * メモリキャッシュが小さい設定でも元画像が収まるよう、縮小率を引き上げる。
	 * 表示用のスケール画像2枚分(見開き)を差し引いた残りの半分を元画像1枚の上限とする。
	 * AVIF/JXL はネイティブデコーダが縮小率を扱わないため対象外。
	 */
	private int limitScaleByMemory(int page, int width, int height, int scale) {
		int extType = mFileList[page].exttype;
		if (extType == FileData.EXTTYPE_AVIF || extType == FileData.EXTTYPE_JXL) {
			return scale;
		}
		if (mMemSize <= 0) {
			return scale;
		}
		// ネイティブ側: buffnum = memsize(MB) * 8 / sizeof(LONG) → 512KB ブロック数
		int totalBlocks = mMemSize * 2;
		if (totalBlocks <= 0) {
			return scale;
		}

		int sclBlocks = 0;
		if (mScrWidth > 0 && mScrHeight > 0) {
			// 縦横どちらの持ち方でも足りるよう大きい方を採る
			sclBlocks = Math.max(calcBlocksFor(mScrWidth, mScrHeight), calcBlocksFor(mScrHeight, mScrWidth));
		}
		// 表示用スケール画像2枚分を差し引き、見開き表示なら元画像2枚が入るよう半分にする
		int budget = totalBlocks - sclBlocks * 2;
		if (isDualView()) {
			budget /= 2;
		}
		budget = Math.max(budget, totalBlocks / 4);
		budget = Math.max(budget, 1);

		int limited = scale;
		while (calcBlocksFor(DEF.divRoundUp(width, limited), DEF.divRoundUp(height, limited)) > budget) {
			// BitmapFactory の inSampleSize は 2 のべき乗に丸められるので倍々にする
			int next = Integer.highestOneBit(limited) * 2;
			if (next > 64) {
				break;
			}
			limited = next;
		}
		if (limited != scale) {
			Logcat.w(Logcat.LOG_LEVEL_WARN, "メモリキャッシュが小さいため縮小率を引き上げます. page=" + page + ", size=" + width + "x" + height + ", scale=" + scale + "→" + limited + ", budget=" + budget + "/" + totalBlocks + " blocks");
		}
		return limited;
	}

	/**
	 * 表示中(またはその見開き相手)のページか。これらの元画像は先読みのために追い出さない。
	 */
	private boolean isDisplayPage(int page) {
		if (page == mCurrentPage) {
			return true;
		}
		if (isDualView() && !mCurrentSingle && page == mCurrentPage + 1) {
			return true;
		}
		return false;
	}

	/**
	 * 開いているストリームから画像サイズだけを取得する。
	 * 呼び出し側で inputStream.mark(orglen + 1) 済みであること。終了時はストリームを mark 位置へ戻す。
	 * @return 0: 成功 / マイナス: 失敗
	 */
	private int SizeCheckImageFromStream(int page, BufferedInputStream inputStream) throws IOException {
		int logLevel = Logcat.LOG_LEVEL_WARN;
		int width = -1;
		int height = -1;

		int extType = FileData.getExtType(mActivity, mFileList[page].name);
		if (extType != FileData.EXTTYPE_AVIF && extType != FileData.EXTTYPE_JXL) {
			// ヘッダだけ読んでサイズを得る(エントリ全体は展開しない)
			BitmapFactory.Options option = new BitmapFactory.Options();
			option.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(new BufferedInputStream(inputStream), null, option);
			width = option.outWidth;
			height = option.outHeight;
		}

		if ((width <= 0 || height <= 0) && mFileList[page].o_width > 0 && mFileList[page].o_height > 0) {
			// 元サイズが既知なら再取得不要
			width = mFileList[page].o_width;
			height = mFileList[page].o_height;
		}

		if (width <= 0 || height <= 0) {
			Logcat.v(logLevel, "BitmapFactoryでサイズ取得できなかったのでネイティブで取得します. " + mFileList[page].name);
			inputStream.reset();
			int[] imagesize = new int[2];
			int returnCode = SizeCheckImage(page, inputStream, imagesize);
			if (returnCode < 0 || imagesize[0] <= 0 || imagesize[1] <= 0) {
				Logcat.e(logLevel, "SizeCheckImage(3) に失敗しました. " + mFileList[page].name);
				return -4;
			}
			width = imagesize[0];
			height = imagesize[1];
		}

		// 本読み込みに備えて先頭へ戻す
		inputStream.reset();
		applyImageSize(page, width, height);
		return 0;
	}

	private int SizeCheckImage(int page, InputStream inputStream, int[] imagesize) {
		int logLevel = Logcat.LOG_LEVEL_WARN;
		Logcat.d(logLevel, "開始します. page=" + page + ", " + mFileList[page].name);
		int returnCode = 0;

		if (page < -1 || mFileList.length <= page) {
			// 範囲外
			return -6;
		}
		if (mFileList[page].width > 0) {
			return -7;
		}

		if (mFileList[page].o_width == 0) {
			returnCode = SizeCheckImage(inputStream, page, mFileList[page].exttype, mFileList[page].orglen, imagesize);
			if (returnCode < 0) {
				Logcat.e(logLevel, "SizeCheckImage(5) の実行に失敗しました.");
				return -8;
			}
			Logcat.d(logLevel, "SizeCheckImage(5) の実行に成功しました.");
		}
		Logcat.d(logLevel, "終了します. " + mFileList[page].name);
		return returnCode;
	}

	public int SizeCheckImage(InputStream inputStream, int page, int type, long orglen, int[] imagesize) {
		int logLevel = Logcat.LOG_LEVEL_WARN;
		int returnCode = 0;
		Logcat.d(logLevel, MessageFormat.format("開始します. page={0}, type={1}, orglen={2}", new Object[]{page, type, orglen}));

		// 読み込み準備
		Logcat.d(logLevel, MessageFormat.format("コマンドを実行します. CallImgLibrary.ImageSetFileSize({0})", new Object[]{orglen}));
		returnCode = CallImgLibrary.ImageSetFileSize(mActivity, mHandler, mCacheIndex, orglen);
		if (returnCode < 0) {
			Logcat.e(logLevel, "ImageSetFileSize に失敗しました. return=" + returnCode);
			return returnCode;
		}
		byte[] data = new byte[100 * 1024];
		int total = 0;
		while (true) {
			int size = 0;
			try {
				if (!mRunningFlag) {
					return DEF.RETURN_CODE_TERMINATED;
				}

				Logcat.d(logLevel, MessageFormat.format("データを読み込みます. page={0}, type={1}, orglen={2}, off={3}, size={4}", new Object[]{page, type, orglen, 0, data.length}));
				size = inputStream.read(data, 0, data.length);
				Logcat.d(logLevel, MessageFormat.format("データ取得サイズ. page={0}, type={1}, orglen={2}, size={3}, total={4}", new Object[]{page, type, orglen, size, total}));
				if (size <= 0) {
					if (total != orglen) {
						Logcat.w(logLevel, MessageFormat.format("データ取得サイズが0以下です. page={0}, type={1}, orglen={2}, size={3}, total={4}", new Object[]{page, type, orglen, size, total}));
						return DEF.RETURN_CODE_ERROR_READ_DATA;
					}
					else {
						Logcat.d(logLevel, MessageFormat.format("必要なデータを読み終えました. page={0}, type={1}, orglen={2}, size={3}, total={4}", new Object[]{page, type, orglen, size, total}));
					}
					break;
				}
			} catch (IOException e) {
				if (total != orglen) {
					Logcat.e(logLevel, "データ取得でエラーになりました.", e);
				} else {
					//必要なデータを読み終えていた場合に抜ける
					Logcat.d(logLevel, "必要なデータを読み終えました. IOException: " + e.getLocalizedMessage());
				}
				return DEF.RETURN_CODE_ERROR_READ_DATA;
			}
			Logcat.d(logLevel, MessageFormat.format("コマンドを実行します. CallImgLibrary.ImageSetData(data, {0})", new Object[]{size}));
			int ret = CallImgLibrary.ImageSetData(mActivity, mHandler, mCacheIndex, data, size);
			if (ret  < 0) {
				Logcat.e(logLevel, MessageFormat.format("コマンドの実行に失敗しました. CallImgLibrary.ImageSetData(data, {0})", new Object[]{size}));
				return ret;
			}
			total += size;
		}
		if (total < orglen) {
			Logcat.e(logLevel, MessageFormat.format("InputStream から取得したサイズが足りませんでした. total={0}, orglen={1}", new Object[]{total, orglen}));
			return -12;
		} else {
			// 画像のサイズを取得する
			Logcat.d(logLevel, MessageFormat.format("コマンドを実行します. CallImgLibrary.ImageGetSize({0}, imagesize)", new Object[]{type}));
			returnCode = CallImgLibrary.ImageGetSize(mActivity, mHandler, mCacheIndex, type, imagesize);
			if (returnCode < 0 || imagesize[0] < 0 || imagesize[1] < 0) {
				Logcat.e(logLevel, MessageFormat.format("画像サイズの取得に失敗しました. returnCode={0}, imagesize[0]={1}, imagesize[1]={2}", new Object[]{returnCode, imagesize[0], imagesize[1]}));
				return returnCode;
			}
		}
		Logcat.d(logLevel, "終了します.");
		return returnCode;
	}

	public Bitmap GetBitmapNative(InputStream inputStream, int page, int scale, Bitmap bm) {
		int logLevel = Logcat.LOG_LEVEL_WARN;

		Logcat.d(logLevel, "開始します. page=" + page + ", scale=" + scale + ", " + mFileList[page].name);

		if (page < 0 || mFileList.length <= page) {
			// 範囲外
			Logcat.e(logLevel, "Page is out of range. " + mFileList[page].name);
			return null;
		}
		if (mFileList[page].width <= 0 || mFileList[page].height <= 0) {
			Logcat.e(logLevel, "Image size is invalid. width=" + mFileList[page].width + ", height=" + mFileList[page].height + ", " + mFileList[page].name);
			return null;
		}

		bm = GetBitmapNativeMain(inputStream, page, mFileList[page].exttype, scale, mFileList[page].orglen, mFileList[page].width, mFileList[page].height, bm);
		if (bm == null) {
			Logcat.e(logLevel, "GetBitmapNativeMain() failed. " + mFileList[page].name);
		}
		Logcat.d(logLevel, "終了します. Bitmap=" + bm + ", " + mFileList[page].name);
		return bm;
	}

	public Bitmap GetBitmapNativeMain(InputStream inputStream, int page, int exttype, int scale, long orglen, int width, int height, Bitmap bm) {
		int logLevel = Logcat.LOG_LEVEL_WARN;
		Logcat.d(logLevel, "開始します. page=" + page + ", exttype=" + exttype + ", scale=" + scale);

		int ret = 0;
		int returnCode = 0;

		// 読み込み準備
		returnCode = CallImgLibrary.ImageSetFileSize(mActivity, mHandler, mCacheIndex, orglen);
		if (returnCode < 0) {
			Logcat.e(logLevel, "ImageSetFileSize failed. return=" + returnCode);
			return null;
		}
		byte[] data = new byte[100 * 1024];
		int total = 0;
		while (true) {
			int size = 0;
			try {
				if (!mRunningFlag) {
					return null;
				}

				size = inputStream.read(data, 0, data.length);
				if (size <= 0) {
					if (total != orglen) {
						Logcat.w(logLevel, MessageFormat.format("データ取得サイズが0以下です. size={0}, total={1}, orglen={2}", new Object[]{size, total, orglen}));
						break;
					}
					else {
						Logcat.d(logLevel, MessageFormat.format("必要なデータを読み終えています. size={0}, total={1}, orglen={2}", new Object[]{size, total, orglen}));
						break;
					}
				}
			} catch (IOException e) {
				if (total != orglen) {
					String msg = null;
					if (e.getLocalizedMessage() != null) {
						msg = e.getLocalizedMessage();
					}
					if (msg == null) {
						msg = "";
					}
					Logcat.e(logLevel, "Catch IOException: " + msg);
					return null;
				} else {
					//必要なデータを読み終えていた場合に抜ける
					break;
				}
			}
			returnCode = CallImgLibrary.ImageSetData(mActivity, mHandler, mCacheIndex, data, size);
			if (returnCode < 0) {
				Logcat.e(logLevel, "ImageSetData failed. return=" + returnCode);
				return null;
			}
			total += size;
		}
		if (total < orglen) {
			Logcat.e(logLevel, "bufferd size is short. total=" + total + ", orglen=" + orglen);
			return null;
		} else {
			bm = Bitmap.createBitmap(width, height, Config.RGB_565);
			try {
				Logcat.d(logLevel, "CallImgLibrary.ImageGetBitmap start. exttype=" + exttype + ", scale=" + scale);
				ret = CallImgLibrary.ImageGetBitmap(mActivity, mHandler, mCacheIndex, exttype, scale, bm);
			} catch (Exception e) {
				Logcat.e(logLevel, "CallImgLibrary.ImageGetBitmap error.", e);
				return null;
			}

		//SaveFile(bm);
			if (ret < 0) {
				Logcat.e(logLevel, "CallImgLibrary.ImageConvertBitmap() failed. return=" + ret);
				return null;
			}
			if (scale != 1) {
				int Outwidth = width / scale;
				int Outheight = height / scale;
				Logcat.d(logLevel, "Bitmap.createScaledBitmap start. width=" + Outwidth + ", height=" + Outheight);
				Bitmap scaled = Bitmap.createScaledBitmap(bm, Outwidth, Outheight, true);
				if (scaled == null) {
					Logcat.e(logLevel, "Bitmap.createScaledBitmap failed.");
					return null;
				}
				if (scaled != bm) {
					// 縮小前の元Bitmapは不要なので即解放
					bm.recycle();
				}
				bm = scaled;
			}
		}
		if (bm == null) {
			Logcat.w(logLevel, "Bitmap is null.");
		}
		Logcat.d(logLevel, "終了します. Bitmap=" + bm);
		return bm;
	}

	private ImageData LoadImage(int page, boolean notice) throws IOException {
		int logLevel = Logcat.LOG_LEVEL_WARN;
		Logcat.d(logLevel, "開始します. page=" + page + ", notice=" + notice);
		ImageData id = null;

		if (mMemCacheFlag[page].fSource) {
			id = new ImageData();
			id.Page = page;
			id.Width = mFileList[page].width;
			id.Height = mFileList[page].height;
			id.SclWidth = 0;// mFileList[page].swidth[half];
			id.SclHeight = 0;// mFileList[page].sheight[half];
			return id;
		}

//		Logcat.d(logLevel, "start");

		if (mFileType == FileData.FILETYPE_PDF) {
			// PDF はサイズ取得にストリームを使わないので従来通り
			if (mFileList[page].width <= 0) {
				SizeCheckImage(page);
			}
			if (!memWriteLock(page, 0, false)) {
				return null;
			}
			try {
				setLoadBitmapStart(page, notice);
				id = LoadPdfImageData(page, mPdfRenderer);
			}
			catch (IOException e) {
				Logcat.e(logLevel, "", e);
			}
		}
		else {
			boolean perfFromFile = cheGetCacheFlag(page);
			boolean downloaded = false;
			if (needDownload(page)) {
				// サーバから受信しながらデコードするより、全部ファイルキャッシュに落としてから
				// ローカルでデコードするほうが速い (受信中のデコード待ちでネットワークが止まらない)
				// キャッシュスレッドの読み込みは画面側の操作で中断する
				boolean interruptible = Thread.currentThread() == mThread;
				downloaded = ensureFileCache(page, notice, interruptible);
				if (!downloaded && ((interruptible && (mCacheBreak || mCacheSleep)) || !mRunningFlag || mCloseFlag)) {
					// 中断された
					return null;
				}
				// それ以外の失敗は従来どおりサーバから読みながらデコードする
			}
			perfReset();
			long perfStart = System.nanoTime();
			long perfSize = 0;
			try {
				// ダウンロード時に進捗を出したので、ここでは 0% を出し直さない
				setLoadBitmapStart(page, notice && !downloaded);
				BufferedInputStream inputStream = openPageStream(page);
				inputStream.mark(mFileList[page].orglen + 1);

				boolean fReady = true;
				if (mFileList[page].width <= 0) {
					// サイズ未確定なら同じストリームで境界だけ先に読む(再シーク・再展開を避ける)
					long perfSizeStart = System.nanoTime();
					if (SizeCheckImageFromStream(page, inputStream) < 0) {
						Logcat.e(logLevel, "サイズ取得に失敗しました. " + mFileList[page].name);
						fReady = false;
					}
					perfSize = perfMs(perfSizeStart);
				}

				if (fReady && !memWriteLock(page, 0, false)) {
					// メモリキャッシュを確保できない
					fReady = false;
				}

				if (fReady) {
					id = LoadImageData(page, inputStream, mFileList[page].orglen);
				}
			}
			catch (IOException e) {
				Logcat.e(logLevel, "", e);
			}
			perf("load page=" + page + " current=" + mCurrentPage + (perfFromFile ? " src=filecache" : " src=server")
					+ " cmplen=" + mFileList[page].cmplen + (id != null ? " ok" : " FAILED")
					+ " total=" + perfMs(perfStart) + "ms sizecheck=" + perfSize + "ms " + perfNet()
					+ (perfFromFile ? "" : (cheGetCacheFlag(page) ? " cached" : " NOT-CACHED")));
		}

		try {
			setLoadBitmapEnd();
		}
		catch (Exception e) {
			Logcat.e(logLevel, "", e);
		}

//		Logcat.d(logLevel, "end");
		// ファイルクローズは不要
		return id;
	}

	public byte[] loadExpandData(String filename) {
		int logLevel = Logcat.LOG_LEVEL_WARN;
		Logcat.d(logLevel, "開始します. filename=" + filename + ", mFileList.length=" + mFileList.length);

		int page = -1;

		// データを探す
		for (int i = 0; i < mFileList.length; i++) {
			if (filename.equals(mFileList[i].name)) {
				page = i;
				break;
			}
		}

		if (page < 0) {
			// 見つからないなら名前に変換してみる (AccessTypeがPICKERでテキストファイルオープンの時)
			String name = FileData.getName(filename);
			for (int i = 0; i < mFileList.length; i++) {
				if (name.equals(mFileList[i].name)) {
					page = i;
					break;
				}
			}
		}
		if (page < 0) {
			// 範囲外
			Logcat.e(logLevel, "ファイルがみつかりませんでした. filename=" + filename);
			return null;
		}

		boolean fError = false;
		byte[] result = new byte[mFileList[page].orglen];
		Logcat.d(logLevel, MessageFormat.format("page={0} mFileList[page].orglen={1}", new Object[]{page, mFileList[page].orglen}));

		try {
			setLoadBitmapStart(page, false);
			if (mFileType == FILETYPE_ZIP) {
				// メモリキャッシュ読込時のみZIP展開する
				// ファイルキャッシュを作成するときはZIP展開不要
				Logcat.d(logLevel, "mFileType == FILETYPE_ZIP");
				ZipInputStream zipStream = new ZipInputStream(new BufferedInputStream(this, BIS_BUFFSIZE));
				zipStream.getNextEntry();
				CacheInputStream cis = new CacheInputStream(zipStream);
				cis.read(result, 0, result.length);
//				zipStream.closeEntry();
			}
			else if (mFileType == FILETYPE_RAR) {
				// メモリキャッシュ読込時のみRAR展開する
				// ファイルキャッシュを作成するときはRAR展開不要
				Logcat.d(logLevel, "mFileType == FILETYPE_RAR");
				mRarStream = new RarInputStream(mRarHandle, new BufferedInputStream(this, BIS_BUFFSIZE), page, mFileList[page]);
				mRarStream.read(result, 0, result.length);
			}
			else {
				Logcat.d(logLevel, MessageFormat.format("mFileType == 圧縮ファイル以外 result.length={0}", new Object[]{result.length}));
				this.read(result, 0, result.length);
			}
		}
		catch (IOException e) {
			Logcat.e(logLevel, "", e);
			fError = true;
		}

		try {
			setLoadBitmapEnd();
		}
		catch (Exception e) {
			Logcat.e(logLevel, "", e);
			fError = true;
		}

		if (fError) {
			return null;
		}
		return result;
	}

	public void getImageSize(String filename, Point pt) throws IOException {
		int logLevel = Logcat.LOG_LEVEL_WARN;
		Logcat.d(logLevel,"filename=" + filename);
		int page = -1;
		pt.x = 0;
		pt.y = 0;

		// データを探す
		for (int i = 0; i < mFileList.length; i++) {
			if (filename.equals(mFileList[i].name)) {
				page = i;
				break;
			}
		}

		if (page < 0) {
			// 範囲外
			Logcat.e(logLevel,"File not found. filename=" + filename);
			return;
		}

		//
		if (mFileList[page].width <= 0) {
			SizeCheckImage(page);
		}

		pt.x = mFileList[page].width;
		pt.y = mFileList[page].height;
		// ファイルクローズは不要
		return;
	}

	// テキスト用
	public Bitmap loadBitmapByName(String filename) throws IOException {
		int logLevel = Logcat.LOG_LEVEL_VERBOSE;
		Logcat.d(logLevel,"filename=" + filename);

		int page = -1;

		// データを探す
		for (int i = 0; i < mFileList.length; i++) {
			Logcat.v(logLevel,"mFileList[" + i + "].name=" + mFileList[i].name);
			if (filename.equals(mFileList[i].name)) {
				page = i;
				break;
			}
		}

		if (page < 0) {
			// 範囲外
			Logcat.e(logLevel,"File not found. filename=" + filename);
			return null;
		}

		BitmapFactory.Options option = new BitmapFactory.Options();
		Bitmap bm = null;

		option.inJustDecodeBounds = false;
		option.inPreferredConfig = Config.ARGB_8888;
		InputStream inputStream;

		ZipInputStream zipStream = null;
		try {
			setLoadBitmapStart(page, false);
			if (mFileType == FILETYPE_ZIP) {
				// メモリキャッシュ読込時のみZIP展開する
				// ファイルキャッシュを作成するときはZIP展開不要
				zipStream = new ZipInputStream(new BufferedInputStream(this, BIS_BUFFSIZE));
				zipStream.getNextEntry();
				inputStream = new CacheInputStream(zipStream);
				// ファイル破損時に無限ループするのでコメント化
				// zipStream.closeEntry();
			}
			else if (mFileType == FILETYPE_RAR) {
				// メモリキャッシュ読込時のみRAR展開する
				// ファイルキャッシュを作成するときはRAR展開不要
				if (mRarStream == null || mRarStream.getLoadPage() != page) {
					mRarStream = new RarInputStream(mRarHandle, new BufferedInputStream(this, BIS_BUFFSIZE), page, mFileList[page]);
				}
				else {
					mRarStream.initSeek();
				}
				inputStream = mRarStream;
			}
			else {
				inputStream = this;
			}
			bm = BitmapFactory.decodeStream(inputStream, null, option);
		}
		catch (IOException e) {
			Logcat.e(logLevel,"", e);
		}

		try {
			setLoadBitmapEnd();
		}
		catch (Exception e) {
			Logcat.e(logLevel, "", e);
		}

		// ファイルクローズは不要
		return bm;
	}

	// サムネイルに近い縮尺を求める
	public Bitmap LoadEpubThumbnail(int width, int height) throws IOException {
		int logLevel = Logcat.LOG_LEVEL_WARN;
		Logcat.d(logLevel, "開始します. width=" + width + ", height=" + height);
		mEpubOrder = true;
		mEpubMode = TextManager.EPUB_MODE_COVER;

		LoadImageList(0, 0, 0);

		if (mFileList != null || mFileList.length != 0) {
			Logcat.d(logLevel, "mFileList[0].name=" + mFileList[0].name);
		}

		return LoadThumbnail(0, width, height);
	}

	// サムネイルに近い縮尺を求める
	public Bitmap LoadThumbnail(int page, int width, int height) throws IOException {
		int logLevel = Logcat.LOG_LEVEL_WARN;
		Logcat.d(logLevel, "開始します. page=" + page + ", width=" + width + ", height=" + height);

		if (mFileList == null || mFileList.length == 0) {
			LoadImageList(0, 0, 0);
		}

		if (mFileList == null || mFileList.length == 0) {
			return null;
		}

		if (mFileList[page].width <= 0) {
			SizeCheckImage(page);
		}
		Logcat.d(logLevel, "開始します. page=" + page + ", mFileList[page].width=" + mFileList[page].width + ", mFileList[page].height=" + mFileList[page].height);

		int sampleSize = 1;
		if (width != 0 && height != 0) {
			sampleSize = DEF.calcThumbnailScale(mFileList[page].width, mFileList[page].height, width, height);
		}
		else {
			sampleSize = 1;
		}
		Logcat.d(logLevel, "LoadThumbnailMain を開始します. page=" + page + ", sampleSize=" + sampleSize);
		Bitmap bm = LoadThumbnailMain(page, sampleSize);
		if (Logcat.isDebugEnabled(logLevel)) Logcat.d(logLevel, "LoadThumbnailMain を終了します. page=" + page + ", bm.getWidth()=" + (bm != null ? bm.getWidth() : -1) + ", bm.getHeight()=" + (bm != null ? bm.getHeight() : -1));
		Logcat.d(logLevel, "終了します.");
		return bm;
	}

	// サムネイル用に画像読込み
	public Bitmap LoadThumbnailMain(int page, int sampleSize) {
		int logLevel = Logcat.LOG_LEVEL_WARN;
		BitmapFactory.Options option = new BitmapFactory.Options();
		Bitmap bm = null;

		option.inJustDecodeBounds = false;
		option.inPreferredConfig = Config.RGB_565;
		option.inSampleSize = sampleSize;
		InputStream inputStream = null;

		ZipInputStream zipStream;

		Logcat.d(logLevel, "開始します. page=" + page + ", sampleSize=" + sampleSize + ", filename=" + mFileList[page].name);

		try {
			setLoadBitmapStart(page, false);
			if (mFileType == FileData.FILETYPE_PDF) {
				Logcat.v(logLevel, "PDFファイルを開きます. filename=" + mFileList[page].name);
				//ページ番号を指定してPdfRenderer.Pageインスタンスを取得する。
				int Outwidth;
				int Outheight;
				//ページ番号を指定してPdfRenderer.Pageインスタンスを取得する。
				PdfRenderer.Page pdfPage = mPdfRenderer.openPage(page);
				try {
					Outwidth = Math.max(1, pdfPage.getWidth() / sampleSize);
					Outheight = Math.max(1, pdfPage.getHeight() / sampleSize);

					//PdfRenderer.Pageの情報を使って空の描画用Bitmapインスタンスを作成する。
					bm = Bitmap.createBitmap(pdfPage.getWidth() , pdfPage.getHeight() , Config.ARGB_8888);
					// PDFをレンダリングする前にBitmapを白く塗る。
					bm.eraseColor(Color.WHITE);
					//空のBitmapにPDFの内容を描画する。
					pdfPage.render(bm , null,null , PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
				} finally {
					//PdfRenderer.Pageを閉じる、この処理を忘れると次回読み込む時に例外が発生する。
					pdfPage.close();
				}
				{
					Logcat.v(logLevel, "PDFファイルのレンダリングに成功しました.");
					Logcat.v(logLevel, "ビットマップのサイズを変更します. width=" + Outwidth + ", height=" + Outheight);
					Bitmap rendered = bm;
					bm = Bitmap.createScaledBitmap(rendered, Outwidth, Outheight, true);
					if (bm != rendered) {
						// ページ全体の大きなビットマップはもう使わない
						rendered.recycle();
					}
					if (bm == null) {
						Logcat.e(logLevel, "ビットマップのサイズの変更に失敗しました. filename=" + mFileList[page].name);
						return null;
					}
					else {
						Logcat.v(logLevel, "ビットマップのサイズの変更に成功しました.");
						return bm;
					}
				}
			}
			else if (mFileType == FILETYPE_ZIP) {
				// メモリキャッシュ読込時のみZIP展開する
				// ファイルキャッシュを作成するときはZIP展開不要
				zipStream = new ZipInputStream(new BufferedInputStream(this, BIS_BUFFSIZE));
				zipStream.getNextEntry();
				inputStream = new BufferedInputStream(zipStream);
				// ファイル破損時に無限ループするのでコメント化
				// zipStream.closeEntry();
			}
			else if (mFileType == FILETYPE_RAR) {
				// メモリキャッシュ読込時のみRAR展開する
				// ファイルキャッシュを作成するときはRAR展開不要
				if (mRarStream == null || mRarStream.getLoadPage() != page) {
					mRarStream = new RarInputStream(mRarHandle, new BufferedInputStream(this, BIS_BUFFSIZE), page, mFileList[page]);
				}
				else {
					mRarStream.initSeek();
				}
				inputStream = new BufferedInputStream(mRarStream);
			}
			else {
				inputStream = new BufferedInputStream(this);
			}

			inputStream.mark(mFileList[page].orglen + 1);

			if (FileData.getExtType(mActivity, mFileList[page].name) != FileData.EXTTYPE_AVIF && FileData.getExtType(mActivity, mFileList[page].name) != FileData.EXTTYPE_JXL) {
				bm = BitmapFactory.decodeStream(new BufferedInputStream(inputStream), null, option);
			}
			if (bm != null) {
				// なにもしない
				Logcat.v(logLevel, "BitmapFactory decode succeed. filename=" + mFileList[page].name);
			}
			else {
				Logcat.v(logLevel, "BitmapFactory decode failed. filename=" + mFileList[page].name);
				try {
					inputStream.reset();
				} catch (IOException e) {
					Logcat.e(logLevel, "inputStream.reset() failed. filename=" + mFileList[page].name);
					inputStream.close();
					return null;
				}
				bm = GetBitmapNative(inputStream, page, sampleSize, bm);
				if (bm == null) {
					Logcat.e(logLevel, "GetBitmapNative failed. filename=" + mFileList[page].name);
					return null;
				}
				Logcat.v(logLevel, "GetBitmapNative succeed. filename=" + mFileList[page].name);
			}
		}
		catch (IOException e) {
			if (mCloseFlag) {
				Logcat.w(logLevel, "キャンセルされました.", e);
			}
			else {
				Logcat.e(logLevel, "", e);
			}
		}

		try {
			setLoadBitmapEnd();
		}
		catch (Exception e) {
			Logcat.e(logLevel, "", e);
		}

		if (bm != null && bm.getConfig() != Config.RGB_565) {
			Bitmap source = bm;
			bm = source.copy(Config.RGB_565, true);
			if (bm != source) {
				source.recycle();
			}
		}

		if (bm == null) {
			Logcat.w(logLevel, "Bitmap が NULL です. filename=" + mFileList[page].name);
		}

		Logcat.d(logLevel, "終了します. filename=" + mFileList[page].name);
		// ファイルクローズは不要
		return bm;
	}

	private ImageData LoadPdfImageData(int page, PdfRenderer mPdfRenderer) {
		int logLevel = Logcat.LOG_LEVEL_WARN;
		int ret = 0;
		ImageData id = null;
		//ページ番号を指定してPdfRenderer.Pageインスタンスを取得する。
		PdfRenderer.Page pdfPage = mPdfRenderer.openPage(page);
		//PdfRenderer.Pageの情報を使って空の描画用Bitmapインスタンスを作成する。
		Logcat.d(logLevel, "BitmapSize pdfPage.getWidth()=" + pdfPage.getWidth() + ", pdfPage.getHeight()=" + pdfPage.getHeight() + ", " + mFileList[page].name);
		Logcat.d(logLevel, "BitmapSize  mFileList[page].width=" + mFileList[page].o_width + ", mFileList[page].height =" + mFileList[page].o_height + ", " + mFileList[page].name);
		Bitmap bm = Bitmap.createBitmap(mFileList[page].o_width , mFileList[page].o_height, Config.ARGB_8888);
		// PDFをレンダリングする前にBitmapを白く塗る。
		Canvas canvas = new Canvas(bm);
		canvas.drawColor(Color.WHITE);
		canvas.drawBitmap(bm, 0, 0, null);
		//空のBitmapにPDFの内容を描画する。
		pdfPage.render(bm , null,null , PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
		//PdfRenderer.Pageを閉じる、この処理を忘れると次回読み込む時に例外が発生する。
		pdfPage.close();
		if (bm != null) {
			Logcat.d(logLevel, "BitmapFactory decode succeed. " + mFileList[page].name);
			ret = CallImgLibrary.ImageSetPage(mActivity, mHandler, mCacheIndex, page, 0);
			if (ret < 0) {
				Logcat.e(logLevel, "CallImgLibrary.ImageSetPage failed. return=" + ret + ", " + mFileList[page].name);
				return null;
			}
			if (mFileList[page].scale != 1) {
				int Outwidth = mFileList[page].width / mFileList[page].scale;
				int Outheight = mFileList[page].height / mFileList[page].scale;
				Logcat.d(logLevel, "Bitmap.createScaledBitmap start. width=" + Outwidth + ", height=" + Outheight);
				Bitmap scaled = Bitmap.createScaledBitmap(bm, Outwidth, Outheight, true);
				if (scaled == null) {
					Logcat.e(logLevel, "Bitmap.createScaledBitmap failed.");
					bm.recycle();
					return null;
				}
				if (scaled != bm) {
					// 縮小前の元Bitmapは不要なので即解放
					bm.recycle();
				}
				bm = scaled;
			}

			Logcat.d(logLevel, "CallImgLibrary.ImageSetPage succeed. " + mFileList[page].name);
			ret = CallImgLibrary.ImageSetBitmap(mActivity, mHandler, mCacheIndex, bm);
			if (ret < 0) {
				Logcat.e(logLevel, "ImageSetBitmap failed. return=" +ret + ", " + mFileList[page].name);
				return null;
			}
			Logcat.d(logLevel, "CallImgLibrary.ImageSetBitmap succeed. " + mFileList[page].name);
			bm.recycle();
			// 読み込み成功
			mMemCacheFlag[page].fSource = true;
			id = new ImageData();
			id.Page = page;
			id.Width = mFileList[page].width;
			id.Height = mFileList[page].height;
		}

		return id;
	}

	// ビュワー表示用の画像読込
	/**
	 * BitmapFactory でデコードする。可能なら前回の作業Bitmapを inBitmap で再利用し、
	 * ページごとの大きな確保と GC を避ける。
	 * 呼び出し側で inputStream.mark() 済みであること(再利用失敗時に reset する)。
	 */
	private Bitmap decodeBitmapReusable(BufferedInputStream inputStream, BitmapFactory.Options option, int width, int height) throws IOException {
		int logLevel = Logcat.LOG_LEVEL_WARN;
		Bitmap bm = null;
		option.inMutable = true;

		Bitmap reuse = mDecodeBitmap;
		long needBytes = (long) width * (long) height * 4L;
		if (reuse != null && !reuse.isRecycled() && reuse.isMutable() && width > 0 && height > 0 && reuse.getAllocationByteCount() >= needBytes) {
			option.inBitmap = reuse;
			try {
				bm = BitmapFactory.decodeStream(new BufferedInputStream(inputStream), null, option);
			}
			catch (IllegalArgumentException e) {
				// 再利用条件を満たさない場合は通常確保にフォールバック
				Logcat.d(logLevel, "inBitmap での再利用に失敗しました. 通常確保します.");
				bm = null;
			}
			if (bm == null) {
				inputStream.reset();
				option.inBitmap = null;
			}
		}

		if (bm == null) {
			bm = BitmapFactory.decodeStream(new BufferedInputStream(inputStream), null, option);
		}

		if (bm != null && bm != mDecodeBitmap) {
			// 新しく確保したら次回以降の作業Bitmapとして保持する
			if (mDecodeBitmap != null && !mDecodeBitmap.isRecycled()) {
				mDecodeBitmap.recycle();
			}
			mDecodeBitmap = bm;
		}
		return bm;
	}

	// ビュワー表示用の画像読込
	/**
	 * @param inputStream openPageStream() で開いたストリーム(mark/reset 可能で先頭位置にあること)
	 */
	private ImageData LoadImageData(int page, BufferedInputStream inputStream, int orglen) throws IOException {
		int logLevel = Logcat.LOG_LEVEL_WARN;
		int ret = 0;

		BitmapFactory.Options option = new BitmapFactory.Options();
		Bitmap bm = null;
		option.inJustDecodeBounds = false;
		option.inPreferredConfig = Config.ARGB_8888;
		option.inSampleSize = mFileList[page].scale;

		ImageData id = null;

		Logcat.d(logLevel, "Start. " + mFileList[page].name);

		inputStream.mark(orglen + 1);
		int extType = FileData.getExtType(mActivity, mFileList[page].name);
		if (extType != FileData.EXTTYPE_AVIF && extType != FileData.EXTTYPE_JXL) {
			bm = decodeBitmapReusable(inputStream, option, mFileList[page].width, mFileList[page].height);
		}
		if (isCacheThreadCanceled()) {
			// 途中で打ち切ったデコード結果 (下の方が欠けた画像) は使わない
			return null;
		}
		if (bm != null) {
			Logcat.v(logLevel, "BitmapFactory decode succeed. " + mFileList[page].name);
			ret = CallImgLibrary.ImageSetPage(mActivity, mHandler, mCacheIndex, page, 0);
			if (ret < 0) {
				Logcat.e(logLevel, "CallImgLibrary.ImageSetPage failed. return=" + ret + ", " + mFileList[page].name);
				return null;
			}
			Logcat.v(logLevel, "CallImgLibrary.ImageSetPage succeed. " + mFileList[page].name);
			ret = CallImgLibrary.ImageSetBitmap(mActivity, mHandler, mCacheIndex, bm);
			if (ret < 0) {
				Logcat.e(logLevel, "ImageSetBitmap failed. return=" +ret + ", " + mFileList[page].name);
				return null;
			}
			Logcat.v(logLevel, "CallImgLibrary.ImageSetBitmap succeed. " + mFileList[page].name);
			// bm は次ページのデコードで再利用するので recycle しない
			// 読み込み成功
			mMemCacheFlag[page].fSource = true;
			id = new ImageData();
			id.Page = page;
			id.Width = mFileList[page].width;
			id.Height = mFileList[page].height;
		}
		else {
			Logcat.v(logLevel, "BitmapFactory decode failed. " + mFileList[page].name);
			try {
				inputStream.reset();
			} catch (IOException e) {
				Logcat.e(logLevel, "inputStream.reset() failed. " + mFileList[page].name, e);
				inputStream.close();
				return null;
			}

			// 読み込み準備
			Logcat.v(logLevel, MessageFormat.format("コマンドを実行します. CallImgLibrary.ImageSetPage({0}, {1}), ret={2} {3}", new Object[]{page, orglen, ret, mFileList[page].name}));
			ret = CallImgLibrary.ImageSetPage(mActivity, mHandler, mCacheIndex, page, orglen);
			if (ret  < 0) {
				Logcat.e(logLevel, MessageFormat.format("コマンドの実行に失敗しました. CallImgLibrary.ImageSetPage({0}, {1}), ret={2} {3}", new Object[]{page, orglen, ret, mFileList[page].name}));
				return null;
			}

//			Logcat.d(logLevel, "start: page=" + page);
			byte[] data = mReadBuffer;
			int total = 0;
			while (true) {
				int size = 0;
				try {
					size = inputStream.read(data, 0, data.length);
					if (size <= 0) {
						break;
					}
				}
				catch (IOException e) {
					if(total != orglen) {
						Logcat.e(logLevel, "catch IOException. " + mFileList[page].name, e);
						throw new IOException("Can't read file.");
					}
					else {
						//必要なデータを読み終えていた場合に抜ける
						break;
					}
				}
				// メモリセットも中断する
				if (mCacheBreak || !mRunningFlag) {
					Logcat.e(logLevel, "User Canceled in LoadImageData. " + mFileList[page].name);
					// throw new IOException("User Canceled in LoadImageData.");
					return null;
				}
				CallImgLibrary.ImageSetData(mActivity, mHandler, mCacheIndex, data, size);
				total += size;
			}

			//		long sttime = SystemClock.uptimeMillis();
			if (total < orglen) {
				Logcat.e(logLevel, "bufferd size is short. total=" + total + ", orglen=" + orglen + ", " + mFileList[page].name);
				mFileList[page].error = true;
				throw new IOException("File is broken.");
			} else {
				int returnCode = 0;
				// 画像を取得してバッファに格納する
				returnCode = CallImgLibrary.ImageConvert(mActivity, mHandler, mCacheIndex, mFileList[page].exttype, mFileList[page].scale);
				if (returnCode >= 0 && mFileList[page].width > 0 && mFileList[page].height > 0) {
					Logcat.v(logLevel, "CallImgLibrary.ImageConvert succeed. " + mFileList[page].name);
					// 読み込み成功
					mMemCacheFlag[page].fSource = true;
					id = new ImageData();
					id.Page = page;
					id.Width = mFileList[page].width;
					id.Height = mFileList[page].height;
				} else {
					Logcat.e(logLevel, "CallImgLibrary.ImageConvert failed. return=" + returnCode + ", " + mFileList[page].name);
					mFileList[page].error = true;
				}
			}
		}
//		Logcat.i(logLevel, "time: " + (int)(SystemClock.uptimeMillis() - sttime));
		Logcat.v(logLevel, "End. " + mFileList[page].name);
		return id;
	}

	private int mScrWidth;
	private int mScrHeight;
	private int mScrCenter;
	private int mScrScaleMode;
	private int mScrDispMode;
	private int mScrAlgoMode;
	private int mScrRotate;
	private int mScrScale; // 任意倍率
	private int mScrWAdjust; // 幅調整縮小
	private int mScrWidthScale; // 幅調整
	private int mScrImgScale; // 拡大
	private int mMarginCut; // 余白削除
	private int mMarginCutColor; // 余白削除の色
	private int mSharpen;	// シャープ化
	private int mInvert;	// カラー反転
	private int mGray;		// グレースケール
	private int mMoire;		// モアレ軽減
	private int mTopSingle;	// 先頭単ページ
	private int mGamma;		// ガンマ補正
	private int mBright;	// 明るさ

	private boolean mScrFitDual;
	private boolean mScrNoExpand;
	private boolean mEpubOrder;
	private int mZoomType;	// ルーペ表示の拡大率

	// 画面変更
	public void setViewSize(int width, int height) {
		mScrWidth = width;
		mScrHeight = height;

		CallImgLibrary.ImageScaleFree(mActivity, mHandler, mCacheIndex, -1, -1);
		if (mFileList != null && mMemCacheFlag != null) {
			for (int i = 0; i < mFileList.length; i++) {
				if (mMemCacheFlag[i] != null && (mMemCacheFlag[i].fScale[0] || mMemCacheFlag[i].fScale[1] || mMemCacheFlag[i].fScale[2])) {
					// 要チェックにする
					mMemCacheFlag[i].fScale[0] = false;
					mMemCacheFlag[i].fScale[1] = false;
					mMemCacheFlag[i].fScale[2] = false;
				}
			}
		}
	}

	public void setEpubOrder(boolean epubOrder) {
		mEpubOrder = epubOrder;
	}

	// 設定変更
	public void setConfig(int mode, int center, boolean fFitDual, int dispMode, boolean noExpand, int algoMode, int rotate, int wadjust, int wscale, int scale, int pageway, int mgncut, int mgncutcolor, int quality, int bright, int gamma, int sharpen, boolean invert, boolean gray, boolean pseland, boolean moire, boolean topsingle, boolean scaleinit, boolean epubOrder, int zoomtype) {
		int logLevel = Logcat.LOG_LEVEL_WARN;
		Logcat.d(logLevel, "wscale=" + wscale + ", scale=" + scale);
		mScrScaleMode = mode;
		if (scaleinit) {
			mScrScale = scale;	// 初期化
		}
		mScrCenter = center;
		mScrFitDual = fFitDual;
		mScrDispMode = dispMode;
		mScrNoExpand = noExpand;
		mScrAlgoMode = algoMode;
		mScrRotate = rotate;
		mScrWAdjust = wadjust;
		mScrWidthScale = wscale;
		mScrImgScale = scale;
		mPageWay = pageway;
		mMarginCut = mgncut;
		mMarginCutColor = mgncutcolor;
		mBright = bright;
		mGamma = gamma;
		mSharpen = sharpen;
		mInvert = invert ? 1 : 0;
		mGray = gray ? 1 : 0;
		mQuality = quality;
		mPseLand = pseland;
		mMoire = moire ? 1 : 0;
		mTopSingle = topsingle ? 1 : 0;
		mEpubOrder = epubOrder;
		mZoomType = zoomtype;

		if (mCacheIndex >= 0) {
			freeScaleCache();
		}
	}

	public void setImageScale(int scale) {
		mScrScale = scale;
//		mScrScaleMode = DEF.SCALE_PINCH;
		freeScaleCache();
	}

	private void freeScaleCache() {
		mCacheBreak = true;
		CallImgLibrary.ImageCancel(mActivity, mHandler, mCacheIndex, 1);
		synchronized (mLock) {
			if (!mCloseFlag) {
				CallImgLibrary.ImageCancel(mActivity, mHandler, mCacheIndex, 0);
				CallImgLibrary.ImageScaleFree(mActivity, mHandler, mCacheIndex, -1, -1);
				if (mFileList != null && mMemCacheFlag != null) {
					for (int i = 0; i < mFileList.length; i++) {
						if (mCloseFlag) {
							break;
						}
						if (mMemCacheFlag[i].fScale[0] || mMemCacheFlag[i].fScale[1] || mMemCacheFlag[i].fScale[2]) {
							// 要チェックにする
							mMemCacheFlag[i].fScale[0] = false;
							mMemCacheFlag[i].fScale[1] = false;
							mMemCacheFlag[i].fScale[2] = false;
						}
					}
				}
			}
		}
	}

	public boolean ImageScalingSync(int page1, int page2, int half1, int half2, ImageData img1, ImageData img2) {
		boolean ret = false;
		long perfStart = System.nanoTime();
		userOpStart();
		try {
			mCacheBreak = true;
			CallImgLibrary.ImageCancel(mActivity, mHandler, mCacheIndex, 1);
			synchronized (mLock) {
				long perfLockWait = perfMs(perfStart);
				if (!mCloseFlag) {
					CallImgLibrary.ImageCancel(mActivity, mHandler, mCacheIndex, 0);
					ret = ImageScaling(page1, page2, half1, half2, img1, img2);
				}
				perf("user scale page=" + page1 + "," + page2 + " lockwait=" + perfLockWait + "ms total=" + perfMs(perfStart) + "ms");
			}
		}
		finally {
			userOpEnd();
		}
		return ret;
	}

	/**
	 * イメージを並べて作成
	 */
	public boolean ImageScaling(int page1, int page2, int half1, int half2, ImageData img1, ImageData img2) {
		mScalingPage1 = page1;
		mScalingPage2 = page2;
		try {
			return ImageScalingMain(page1, page2, half1, half2, img1, img2);
		}
		finally {
			mScalingPage1 = -1;
			mScalingPage2 = -1;
		}
	}

	private boolean ImageScalingMain(int page1, int page2, int half1, int half2, ImageData img1, ImageData img2) {
		int logLevel = Logcat.LOG_LEVEL_WARN;
		Logcat.d(logLevel, "Page=" + page1 + ", Half=" + half1 + ", ■■■■■ ■■■■■ 開始 ■■■■■ ■■■■■ ");

		if (mScrWidth == 0 || mScrHeight == 0) {
			return false;
		}

		// 元画像を手放したページでも、同じ並びで作った表示用画像があればそのまま使う
		// (設定や画面サイズが変わると freeScaleCache / setViewSize で表示用画像ごと消える)
		boolean noSource = !mMemCacheFlag[page1].fSource || (page2 >= 0 && !mMemCacheFlag[page2].fSource);
		if (noSource) {
			if (hasScaleFor(page1, half1, page2) && (page2 < 0 || hasScaleFor(page2, half2, page1))) {
				fillScaleInfo(img1, page1, half1, half1);
				if (page2 >= 0) {
					// 下の処理と同じく 2 ページ目の fwidth も half1 の位置を使う
					fillScaleInfo(img2, page2, half2, half1);
				}
				return true;
			}
			// 作り直すので元画像を読み直す (読めなかったページは下の処理で飛ばされる)
			if (ensureSource(page1) && page2 >= 0) {
				ensureSource(page2);
			}
		}

		int[] src_x = {0, 0}; // 映像オリジナルサイズ
		int[] src_y = {0, 0};
		int[] adj_x = {0, 0}; // 映像拡大縮小後サイズ
		int[] adj_y = {0, 0};
		int view_x; // 1～2画像のまとめたサイズ
		int view_y;
		int disp_x = mScrWidth; // 画面の横サイズ
		int disp_x2 = disp_x - mScrCenter; // 2ページ目の横サイズ
		int disp_y = mScrHeight; // 画面の縦サイズ
		boolean fWidth;
		int pseland = mPseLand ? 1 : 0;

		int[] size = {0, 0}; // 画像の完成サイズの戻り値
		int[] margin = {0, 0, 0, 0}; // 余白サイズの戻り値, 左, 右, 上, 下
		int[] left = {0, 0};
		int[] right = {0, 0};
		int[] top = {0, 0};
		int[] bottom = {0, 0};

		// 画面サイズ
		disp_x = mScrWidth;
		disp_y = mScrHeight;
		Logcat.v(logLevel, "Page=" + page1 + ", Half=" + half1 + ", 画面サイズ disp_x=" + disp_x + ", disp_y=" + disp_y);

		// 画像1の情報
		if (mScrRotate == ROTATE_NORMAL || mScrRotate == ROTATE_180DEG) {
			src_x[0] = mFileList[page1].width;
			src_y[0] = mFileList[page1].height;
		} else {
			src_x[0] = mFileList[page1].height;
			src_y[0] = mFileList[page1].width;
		}
		Logcat.v(logLevel, "Page=" + page1 + ", Half=" + half1 + ", 元画像:P1 src_x1=" + src_x[0] + ", src_y1=" + src_y[0]);

		if (mMarginCut != 0) {
			// 余白カットありの場合
			// 余白のサイズを計測
			if (CallImgLibrary.GetMarginSize(mActivity, mHandler, mCacheIndex, page1, half1, 0, mMarginCut, mMarginCutColor, margin) > 0) {
				left[0] = margin[0];
				right[0] = margin[1];
				top[0] = margin[2];
				bottom[0] = margin[3];
			}
		}
		Logcat.v(logLevel, "Page=" + page1 + ", Half=" + half1 + ", マージン:P1 左=" + left[0] + ", 右=" + right[0] + ", 上=" + top[0] + ", 下=" + bottom[0]);

		if (page2 != -1) {
			// 画像2の情報
			if (mScrRotate == ROTATE_NORMAL || mScrRotate == ROTATE_180DEG) {
				src_x[1] = mFileList[page2].width;
				src_y[1] = mFileList[page2].height;
			} else {
				src_x[1] = mFileList[page2].height;
				src_y[1] = mFileList[page2].width;
			}
			Logcat.v(logLevel, "Page=" + page2 + ", Half=" + half2 + ", 元画像:P2 src_x2=" + src_x[1] + ", src_y2=" + src_y[1]);

			if (mMarginCut != 0) {
				// 余白カットありの場合
				// 余白のサイズを計測
				if (CallImgLibrary.GetMarginSize(mActivity, mHandler, mCacheIndex, page2, half2, 0, mMarginCut, mMarginCutColor, margin) > 0) {
					left[1] = margin[0];
					right[1] = margin[1];
					top[1] = margin[2];
					bottom[1] = margin[3];
				}
			}
			CallImgLibrary.GetMarginSize(mActivity, mHandler, mCacheIndex, page2, half2, 0, mMarginCut, mMarginCutColor, margin);
			Logcat.v(logLevel, "Page=" + page2 + ", Half=" + half1 + ", マージン:P2 左=" + left[1] + ", 右=" + right[1] + ", 上=" + top[1] + ", 下=" + bottom[1]);
		}

		// カットしてサイズがマイナスになったらプラスに戻す
		if (src_y[0] - top[0] - bottom[0] <= 20) {
			top[0] = (src_y[0] / 2) - 10;
			bottom[0] = (src_y[0] / 2) - 10;
		}
		if (src_x[0] - left[0] - right[0] <= 20) {
			left[0] = (src_x[0] / 2) - 10;
			right[0] = (src_x[0] / 2) - 10;
		}
		if (page2 != -1) {
			if (src_y[1] - top[1] - bottom[1] <= 20) {
				top[1] = (src_y[1] / 2) - 10;
				bottom[1] = (src_y[1] / 2) - 10;
			}
			if (src_x[1] - left[1] - right[1] <= 20) {
				left[1] = (src_x[1] / 2) - 10;
				right[1] = (src_x[1] / 2) - 10;
			}
		}

		if (mMarginCut != 0 && mMarginCut != 6) {
			// 余白カットありで縦横比を維持の場合

			if (page2 != -1) {
				if (mScrFitDual) {
					// 高さを揃える場合
					// 上下のカット率を少ないほうに合わせる
					if (top[0] * 1000 / src_y[0] > top[1] * 1000 / src_y[1]) {
						top[0] = top[1] * src_y[0] / src_y[1];
					} else {
						top[1] = top[0] * src_y[1] / src_y[1];
					}
					if (bottom[0] * 1000 / src_y[0] > bottom[1] * 1000 / src_y[1]) {
						bottom[0] = bottom[1] * src_y[0] / src_y[1];
					} else {
						bottom[1] = bottom[0] * src_y[1] / src_y[0];
					}

					Logcat.v(logLevel, "Page=" + page1 + ", Half=" + half1 + ", 上下を揃える:P1 左=" + left[0] + ", 右=" + right[0] + ", 上=" + top[0] + ", 下=" + bottom[0]);
					Logcat.v(logLevel, "Page=" + page2 + ", Half=" + half1 + ", 上下を揃える:P2 左=" + left[1] + ", 右=" + right[1] + ", 上=" + top[1] + ", 下=" + bottom[1]);
				}
			}

			// 横幅が画面の縦横比より細い場合、横のカットを戻す
			int work_x = (disp_x > disp_y) ? disp_x / 2 : disp_x;
			if (left[0] + right[0] > 0) {
				int x = (src_x[0] - left[0] - right[0]);
				int y = (src_y[0] - top[0] - bottom[0]);
				if (x * 1000 / work_x < y * 1000 / disp_y) {
					int margin_x = (int) ((float) src_x[0] - ((float) y * ((float) work_x / (float) disp_y)));
					margin_x = Math.max(0, margin_x);
					left[0] = margin_x * left[0] / (left[0] + right[0]);
					right[0] = margin_x - left[0];
				}
			}
			if (page2 != -1) {
				if (left[1] + right[1] > 0) {
					int x = (src_x[1] - left[1] - right[1]);
					int y = (src_y[1] - top[1] - bottom[1]);
					if (x * 1000 / work_x < y * 1000 / disp_y) {
						int margin_x = (int) ((float) src_x[1] - ((float) y * ((float) work_x / (float) disp_y)));
						margin_x = Math.max(0, margin_x);
						left[1] = margin_x * left[1] / (left[1] + right[1]);
						right[1] = margin_x - left[1];
					}
				}
			}

			// 横幅が画面の縦横比より太い場合、縦のカットを戻す
			if (top[0] + bottom[0] > 0) {
				int x = (src_x[0] - left[0] - right[0]);
				int y = (src_y[0] - top[0] - bottom[0]);
				if (x * 1000 / work_x > y * 1000 / disp_y) {
					int margin_y = (int) ((float) src_y[0] - ((float) x * ((float) disp_y / (float) work_x)));
					margin_y = Math.max(0, margin_y);
					top[0] = margin_y * top[0] / (top[0] + bottom[0]);
					bottom[0] = margin_y - top[0];
				}
			}
			if (page2 != -1) {
				if (top[1] + bottom[1] > 0) {
					int x = (src_x[1] - left[1] - right[1]);
					int y = (src_y[1] - top[1] - bottom[1]);
					if (x * 1000 / work_x > y * 1000 / disp_y) {
						int margin_y = (int) ((float) src_y[1] - ((float) x * ((float) disp_y / (float) work_x)));
						margin_y = Math.max(0, margin_y);
						top[1] = margin_y * top[1] / (top[1] + bottom[1]);
						bottom[1] = margin_y - top[1];
					}
				}
			}

			Logcat.v(logLevel, "Page=" + page1 + ", Half=" + half1 + ", 画面の比率に近づける:P1 左=" + left[0] + ", 右=" + right[0] + ", 上=" + top[0] + ", 下=" + bottom[0]);
			Logcat.v(logLevel, "Page=" + page2 + ", Half=" + half1 + ", 画面に比率に近づける:P2 左=" + left[1] + ", 右=" + right[1] + ", 上=" + top[1] + ", 下=" + bottom[1]);

			if (page2 != -1) {
				// 左右の画像の縦横比を揃える
				int x0 = src_x[0] - left[0] - right[0];
				int x1 = src_x[1] - left[1] - right[1];
				int y0 = src_y[0] - top[0] - bottom[0];
				int y1 = src_y[1] - top[1] - bottom[1];
				if (x0 * 1000 / y0 > x1 * 1000 / y1) {
					int width = x0 * y1 / y0;
					if (src_x[1] > width) {
						if (left[1] + right[1] != 0) {
							left[1] = (src_x[1] - width) * left[1] / (left[1] + right[1]);
							right[1] = (src_x[1] - width) - left[1];
						}
					} else {
						left[1] = 0;
						right[1] = 0;
					}
					Logcat.v(logLevel, "Page=" + page1 + ", Half=" + half1 + ", 左右を揃える:P1 width=" + width + ", src_x=" + src_x[1] + ", 左=" + left[1] + ", 右=" + right[1]);
				} else {
					int width = x1 * y0 / y1;
					if (src_x[0] > width) {
						if (left[0] + right[0] != 0) {
							left[0] = (src_x[0] - width) * left[0] / (left[0] + right[0]);
							right[0] = (src_x[0] - width) - left[0];
						}
					} else {
						left[0] = 0;
						right[0] = 0;
					}
					Logcat.v(logLevel, "Page=" + page1 + ", Half=" + half1 + ", 左右を揃える:P1 width=" + width + ", src_x=" + src_x[0] + ", 左=" + left[0] + ", 右=" + right[0]);
				}

				Logcat.v(logLevel, "Page=" + page1 + ", Half=" + half1 + ", 左右を揃える:P1 左=" + left[0] + ", 右=" + right[0] + ", 上=" + top[0] + ", 下=" + bottom[0]);
				Logcat.v(logLevel, "Page=" + page2 + ", Half=" + half1 + ", 左右を揃える:P2 左=" + left[1] + ", 右=" + right[1] + ", 上=" + top[1] + ", 下=" + bottom[1]);
			}

			// 画像が横長なら左右のカットを同じにする
			if (src_x[0] > src_y[0]) {
				left[0] = Math.min(left[0], right[0]);
				right[0] = left[0];
			}


			// 元画像の縦横比をカット後の値にする
			src_x[0] = (src_x[0] - left[0] - right[0]);
			src_y[0] = (src_y[0] - top[0] - bottom[0]);
			src_x[1] = (src_x[1] - left[1] - right[1]);
			src_y[1] = (src_y[1] - top[1] - bottom[1]);

		}

		if (img1 != null) {
			img1.CutLeft = left[0];
			img1.CutRight = right[0];
			img1.CutTop = top[0];
			img1.CutBottom = bottom[0];
		}
		if (img2 != null) {
			img2.CutLeft = left[1];
			img2.CutRight = right[1];
			img2.CutTop = top[1];
			img2.CutBottom = bottom[1];
		}

		if (mMarginCut == 6) {
			// 余白削除モードが縦横比無視の場合
			// 元画像の縦横比を画面サイズにする
			src_x[0] = disp_x;
			src_y[0] = disp_y;
			src_x[1] = disp_x2;
			src_y[1] = disp_y;

		}

		// 画面縦横比調整
		if (mScrWAdjust != 100) {
			if (disp_x > disp_y) {
				// 横持ちの時
				src_x[0] = src_x[0] * 100 / mScrWAdjust;
			} else {
				// 縦持ちの時
				src_x[0] = src_x[0] * mScrWAdjust / 100;
			}
		}
		// 画像幅調整
		if (mScrWidthScale != 100) {
			src_x[0] = src_x[0] * mScrWidthScale / 100;
		}

		if (half1 != 0) {
			// 半分にする
			src_x[0] = (src_x[0] + 1) / 2;
		}
		Logcat.v(logLevel, "Page=" + page1 + ", Half=" + half1 + ", アスペクト比調整:P1 src_x1=" + src_x[0] + ", src_y1=" + src_y[0]);
		adj_x[0] = src_x[0];
		adj_y[0] = src_y[0];


		if (page2 != -1) {
			// 画面縦横比調整
			if (mScrWAdjust != 100) {
				if (disp_x2 > disp_y) {
					// 横持ちの時
					src_x[1] = src_x[1] * 100 / mScrWAdjust;
				} else {
					// 縦持ちの時
					src_x[1] = src_x[1] * mScrWAdjust / 100;
				}
			}
			// 画像幅調整
			if (mScrWidthScale != 100) {
				src_x[1] = src_x[1] * mScrWidthScale / 100;
			}

			if (half2 != 0) {
				// 半分にする
				src_x[1] = (src_x[1] + 1) / 2;
			}
			Logcat.v(logLevel, "Page=" + page1 + ", Half=" + half1 + ", アスペクト比調整:P1 src_x2=" + src_x[1] + ", src_y2=" + src_y[1]);
			adj_x[1] = src_x[1];
			adj_y[1] = src_y[1];
		}

		// 高さを揃える必要がある
		if (mScrFitDual) {
			// 拡大あり
			if (src_y[0] > src_y[1] && src_y[1] != 0) {
				adj_x[1] = src_x[1] * src_y[0] / src_y[1];
				adj_y[1] = src_y[0];
			} else if (src_y[0] < src_y[1] && src_y[0] != 0) {
				adj_x[0] = src_x[0] * src_y[1] / src_y[0];
				adj_y[0] = src_y[1];
			}
		}


		// 1～2映像を足したサイズ
		int src_cx = adj_x[0] + adj_x[1];
		int src_cy = adj_y[0] > adj_y[1] ? adj_y[0] : adj_y[1];
		Logcat.v(logLevel, "Page=" + page1 + ", Half=" + half1 + ", 左右サイズ揃えP1 adj_x=" + adj_x[0] + ", adj_y=" + adj_y[0]);
		Logcat.v(logLevel, "Page=" + page2 + ", Half=" + half2 + ", 左右サイズ揃えP2 adj_x=" + adj_x[1] + ", adj_y=" + adj_y[1]);
		Logcat.v(logLevel, "Page=" + page1 + ", Half=" + half1 + ",左右サイズ合計 src_cx=" + src_cx + ", src_cy=" + src_cy);

		// サイズ0だと0除算なので終了
		if (src_cx == 0 || src_cy == 0) {
			return false;
		}

		if (mScrScaleMode == DEF.SCALE_ORIGINAL) {
			// 元サイズのまま
			view_x = src_cx;
			view_y = src_cy;
		} else if (mScrScaleMode == DEF.SCALE_FIT_ALLMAX) {
			// 縦横比無視で拡大
			view_x = disp_x;
			view_y = disp_y;
		} else if (mScrScaleMode == DEF.SCALE_FIT_SPRMAX) {
			// 縦横比無視で拡大（見開き対応）
			if (DEF.checkPortrait(disp_x, disp_y)) {
				// 縦画面
				view_x = DEF.checkPortrait(src_cx, src_cy) ? disp_x : disp_x * 2;
			} else {
				// 横画面
				view_x = DEF.checkPortrait(src_cx, src_cy) ? disp_x / 2 : disp_x;
			}
			view_y = disp_y;
		} else if (mScrScaleMode == DEF.SCALE_FIT_WIDTH2) {
			// 幅基準（見開き対応）
			if (DEF.checkPortrait(disp_x, disp_y)) {
				// 縦画面
				view_x = DEF.checkPortrait(src_cx, src_cy) ? disp_x : disp_x * 2;
			} else {
				// 横画面
				view_x = DEF.checkPortrait(src_cx, src_cy) ? disp_x / 2 : disp_x;
			}
			view_y = src_cy * view_x / src_cx;
		} else if (mScrScaleMode == DEF.SCALE_FIT_ALL2) {
			// 全体表示（見開き対応）
			Logcat.v(logLevel, "Page=" + page1 + ", Half=" + half1 + ", 全体表示（見開き対応）");
			int dispwk_x;
			if (DEF.checkPortrait(disp_x, disp_y)) {
				// 縦画面
				dispwk_x = DEF.checkPortrait(src_cx, src_cy) ? disp_x : disp_x * 2;
				Logcat.v(logLevel, "Page=" + page1 + ", Half=" + half1 + ", 縦画面 dispwk_x=" + dispwk_x);
			} else {
				// 横画面
				dispwk_x = DEF.checkPortrait(src_cx, src_cy) ? disp_x / 2 : disp_x;
				Logcat.v(logLevel, "Page=" + page1 + ", Half=" + half1 + ", 横画面 dispwk_x=" + dispwk_x);
			}

			if (dispwk_x * 1000 / src_cx < disp_y * 1000 / src_cy) {
				// Y方向よりもX方向の方が拡大率が小さく画面いっぱいになる
				// 幅基準
				view_x = dispwk_x;
				view_y = src_cy * dispwk_x / src_cx;
				// 誤差(1ピクセル大きい)を修正
				view_y = Math.min(view_y, disp_y);
				Logcat.v(logLevel, "Page=" + page1 + ", Half=" + half1 + ", 幅基準 view_x=" + view_x + ", view_y=" + view_y);
			} else {
				// 高さ基準
				view_x = src_cx * disp_y / src_cy;
				// 誤差(1ピクセル大きい)を修正
				view_x = Math.min(view_x, dispwk_x);
				view_y = disp_y;
				Logcat.v(logLevel, "Page=" + page1 + ", Half=" + half1 + ", 高さ基準 view_x=" + view_x + ", view_y=" + view_y);
			}

		} else if (mScrScaleMode == DEF.SCALE_FIT_ALL) {
			if (disp_x * 1000 / src_cx < disp_y * 1000 / src_cy) {
				// Y方向よりもX方向の方が拡大率が小さく画面いっぱいになる
				// 幅基準
				view_x = disp_x;
				view_y = src_cy * disp_x / src_cx;
				// 誤差(1ピクセル大きい)を修正
				view_y = Math.min(view_y, disp_y);
			} else {
				// 高さ基準
				view_x = src_cx * disp_y / src_cy;
				// 誤差(1ピクセル大きい)を修正
				view_x = Math.min(view_x, disp_x);
				view_y = disp_y;
			}
		} else {

			if (mScrScaleMode == DEF.SCALE_FIT_WIDTH) {
				// 幅にあわせる
				fWidth = true;
			} else {
				// 高さにあわせる
				fWidth = false;
			}

			if (fWidth) {
				// 幅基準
				view_x = disp_x;
				view_y = src_cy * disp_x / src_cx;
			} else {
				// 高さ基準
				view_x = src_cx * disp_y / src_cy;
				view_y = disp_y;
			}
		}

		if (view_x > src_cx && mScrNoExpand) {
			// 拡大かつ拡大しないモードのときは元サイズのまま
			view_x = src_cx;
			view_y = src_cy;
		}

		Logcat.v(logLevel, "Page=" + page1 + ", Half=" + half1 + ", 表示方法を反映 view_x=" + view_x + ", view_y=" + view_y);

		int[] width = new int[2];
		int[] height = new int[2];
		int[] fitwidth = new int[2];
		int[] fitheight = new int[2];

		// サイズ算出 & リサイズ
		width[0] = view_x * adj_x[0] / (adj_x[0] + adj_x[1]);
		height[0] = view_y * adj_y[0] / src_cy;

		if (page2 >= 0) {
			width[1] = view_x - width[0];
			height[1] = view_y * adj_y[1] / src_cy;
		}

		// 拡大しすぎの時は抑える
		int limit = Math.max(mScrWidth, mScrHeight) * 3;
		for (int i = 0 ; i < 2 ; i ++) {
    		if (mScrScaleMode == DEF.SCALE_FIT_HEIGHT) {
    			if (width[i] > limit) {
    				// 高さに合わせる場合の幅は画面の長辺の2倍まで
    				height[i] = height[i] * limit / width[i] ;
    				width[i] = limit;
    			}
    		}
    		if (mScrScaleMode == DEF.SCALE_FIT_WIDTH) {
    			if (height[i] > limit) {
    				// 幅さに合わせる場合の高さ画面の長辺の2倍まで
    				width[i] = width[i] * limit / height[i];
    				height[i] = limit;
    			}
    		}
    		if (page2 < 0) {
    			// 2ページ目はない
    			break;
    		}
		}

		Logcat.v(logLevel, "Page=" + page1 + ", Half=" + half1 + ", 指定サイズP1 width=" + width[0] + ", height=" + height[0]);
		Logcat.v(logLevel, "Page=" + page2 + ", Half=" + half2 + ", 指定サイズP2 width=" + width[1] + ", height=" + height[1]);

		// 任意スケールの設定前に100%状態のサイズを保持
		fitwidth[0] = width[0];
		fitheight[0] = height[0];
		fitwidth[1] = width[1];
		fitheight[1] = height[1];

		mFileList[page1].fwidth[half1] = fitwidth[0];
		mFileList[page1].fheight[half1] = fitheight[0];
		if (page2 != -1) {
			mFileList[page2].fwidth[half1] = fitwidth[1];
			mFileList[page2].fheight[half1] = fitheight[1];
		}
		// 任意スケールは結果に対して設定
		width[0] = width[0] * mScrScale / 100;
		height[0] = height[0] * mScrScale / 100;

		if (page2 >= 0) {
			width[1] = width[1] * mScrScale / 100;
			height[1] = height[1] * mScrScale / 100;
		}

		if (page1 >= 0 && mMemCacheFlag[page1].fSource && width[0] > 0 && height[0] > 0) {
			Logcat.v(logLevel, "Page=" + page1 + ", Half=" + half1 + ", ソース読み込み済み");
			if (mFileList[page1].swidth[half1] == width[0] && mFileList[page1].sheight[half1] == height[0] && mMemCacheFlag[page1].fScale[half1]) {
				Logcat.v(logLevel, "Page=" + page1 + ", Half=" + half1 + ", 画像作成済み");
				mMemCacheFlag[page1].partner[half1] = page2;
				if (img1 != null) {
					img1.SclWidth = width[0];
					img1.SclHeight = height[0];
					img1.FitWidth = fitwidth[0];
					img1.FitHeight = fitheight[0];
				}
			}
			else {
				Logcat.v(logLevel, "Page=" + page1 + ", Half=" + half1 + ", 画像作成開始");
				mFileList[page1].swidth[half1] = width[0];
				mFileList[page1].sheight[half1] = height[0];
				if (memWriteLock(page1, half1, true)) {
					// スケール作成
					sendMessage(mHandler, DEF.HMSG_CACHE, 0, 2, null);
//					long sttime = SystemClock.uptimeMillis();
					int param = CallImgLibrary.ImageScaleParam(mInvert, mGray, 0, mMoire, pseland);
					if (CallImgLibrary.ImageScale(mActivity, mHandler, mCacheIndex, page1, half1, width[0], height[0], left[0], right[0], top[0], bottom[0], mScrAlgoMode, mScrRotate, mMarginCut, mMarginCutColor, mSharpen, mBright, mGamma, param, size) >= 0) {
						Logcat.v(logLevel, "Page=" + page1 + ", Half=" + half1 + ", 完成サイズP1 size_w=" + size[0] + ", size_h=" + size[1]);
						mMemCacheFlag[page1].fScale[half1] = true;
						mMemCacheFlag[page1].partner[half1] = page2;
						if (img1 != null) {
							img1.SclWidth = width[0];
							img1.SclHeight = height[0];
							img1.FitWidth = fitwidth[0];
							img1.FitHeight = fitheight[0];
						}
					}
					else {
						mFileList[page1].swidth[half1] = 0;
						mFileList[page1].sheight[half1] = 0;
					}
//					Logcat.i(logLevel, "time: " + (int)(SystemClock.uptimeMillis() - sttime));
					sendMessage(mHandler, DEF.HMSG_CACHE, -1, 0, null);
				}
			}
			if (img1 != null) {
				Logcat.v(logLevel, "Page=" + page1 + ", Half=" + half1
						+ ", Width=" + img1.Width + ", Height=" + img1.Height
						+ ", FitWidth=" + img1.FitWidth + ", FitHeight=" + img1.FitHeight
						+ ", SclWidth=" + img1.SclWidth + ", SclHeight=" + img1.SclHeight);
			}
		}

		if (page2 >= 0 && mMemCacheFlag[page2].fSource && width[1] > 0 && height[1] > 0) {
			// 見開き時
			if (mFileList[page2].swidth[half2] == width[1] && mFileList[page2].sheight[half2] == height[1] && mMemCacheFlag[page2].fScale[half2]) {
				mMemCacheFlag[page2].partner[half2] = page1;
				if (img2 != null) {
					img2.SclWidth = width[1];
					img2.SclHeight = height[1];
					img2.FitWidth = fitwidth[1];
					img2.FitHeight = fitheight[1];
				}
			}
			else {
				mFileList[page2].swidth[half2] = width[1];
				mFileList[page2].sheight[half2] = height[1];
				if (memWriteLock(page2, half2, true)) {
					// スケール作成
					sendMessage(mHandler, DEF.HMSG_CACHE, 0, 2, null);
//					long sttime = SystemClock.uptimeMillis();
					int param = CallImgLibrary.ImageScaleParam(mInvert, mGray, 0, mMoire, pseland);
					if (CallImgLibrary.ImageScale(mActivity, mHandler, mCacheIndex, page2, half2, width[1], height[1], left[1], right[1], top[1], bottom[1], mScrAlgoMode, mScrRotate, mMarginCut, mMarginCutColor, mSharpen, mBright, mGamma, param, size) >= 0) {
						Logcat.d(logLevel, "Page=" + page2 + ", Half=" + half2 + ", 完成サイズP2 size_w=" + size[0] + ", size_h=" + size[1]);
						mMemCacheFlag[page2].fScale[half2] = true;
						mMemCacheFlag[page2].partner[half2] = page1;
						if (img2 != null) {
							img2.SclWidth = width[1];
							img2.SclHeight = height[1];
							img2.FitWidth = fitwidth[1];
							img2.FitHeight = fitheight[1];
						}
					}
					else {
						mFileList[page2].swidth[half2] = 0;
						mFileList[page2].sheight[half2] = 0;
					}
//					Logcat.i(logLevel, "time: " + (int)(SystemClock.uptimeMillis() - sttime));
					sendMessage(mHandler, DEF.HMSG_CACHE, -1, 0, null);
				}
			}
			if (img2 != null) {
				Logcat.v(logLevel, "Page=" + page2 + ", Half=" + half2
						+ ", Width=" + img2.Width + ", Height=" + img2.Height
						+ ", FitWidth=" + img2.FitWidth + ", FitHeight=" + img2.FitHeight
						+ ", SclWidth=" + img2.SclWidth + ", SclHeight=" + img2.SclHeight);
			}
		}

		//		Logcat.d(logLevel, "end: p1=" + page1 + ", p2=" + page2 + ", h1=" + half1 + ", h2=" + half2);
		return true;
	}

	// 指定した保存ファイル名で指定ページを書き出し
	// nameがnullなら元のファイル名で
	public String decompFile(int page, String name) {
		int logLevel = Logcat.LOG_LEVEL_WARN;
		if (page < 0 || mFileList.length <= page) {
			// 範囲外
			return null;
		}

		String resultPath = null;
		mCacheBreak = true;
		CallImgLibrary.ImageCancel(mActivity, mHandler, mCacheIndex, 1);
		synchronized (mLock) {
			if (!mCloseFlag) {
				CallImgLibrary.ImageCancel(mActivity, mHandler, mCacheIndex, 0);

				// キャッシュ読込モードオン
				new File(DEF.getBaseDirectory()).mkdirs();
				new File(DEF.getBaseDirectory() + "share/").mkdirs();

				if (name == null) {
					name = new String();
					name = mFileList[page].name;
					if (mFileType == FileData.FILETYPE_PDF) {
						name += ".jpg";
					}
				}
				name = name.replace("\\", "_");
				name = name.replace("/", "_");
				String file = DEF.getBaseDirectory() + "share/" + name;
				new File(file).delete();

				BufferedOutputStream os;
				try {
					os = new BufferedOutputStream(new FileOutputStream(file), 500 * 1024);
				} catch (FileNotFoundException e) {
					Logcat.e(logLevel, "", e);
					return null;
				}
				byte[] buff = new byte[BIS_BUFFSIZE];

				boolean written = false;
				try {
					mCheWriteFlag = false;
					setLoadBitmapStart(page, false);

					if (mFileType == FileData.FILETYPE_PDF) {
						Logcat.d(logLevel, "PDFファイルを開きます.");
						//ページ番号を指定してPdfRenderer.Pageインスタンスを取得する。
						PdfRenderer.Page pdfPage = mPdfRenderer.openPage(page);
						Bitmap bm;
						try {
							//PdfRenderer.Pageの情報を使って空の描画用Bitmapインスタンスを作成する。
							bm = Bitmap.createBitmap(pdfPage.getWidth() , pdfPage.getHeight() , Config.ARGB_8888);
							// PDFをレンダリングする前にBitmapを白く塗る。
							bm.eraseColor(Color.WHITE);
							//空のBitmapにPDFの内容を描画する。
							pdfPage.render(bm , null,null , PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
						} finally {
							//PdfRenderer.Pageを閉じる、この処理を忘れると次回読み込む時に例外が発生する。
							pdfPage.close();
						}
						Logcat.d(logLevel, "PDFファイルのレンダリングに成功しました.");
						Logcat.d(logLevel, "JPG形式で保存します.");
						// jpegで保存
						bm.compress(Bitmap.CompressFormat.JPEG, 100, os);
						bm.recycle();
					}
					else if (mFileType == FILETYPE_ZIP) {
						// メモリキャッシュ読込時のみZIP展開する
						// ファイルキャッシュを作成するときはZIP展開不要
						ZipInputStream zipStream = new ZipInputStream(new BufferedInputStream(this, BIS_BUFFSIZE));
						zipStream.getNextEntry();
						int readsum = 0;
						while (mRunningFlag) {
							if (mCloseFlag) {
								break;
							}
							int readsize = zipStream.read(buff, 0, buff.length);
							if (readsize <= 0) {
								break;
							} else
								readsum += readsize;
							os.write(buff, 0, readsize);
							// ロード経過をcallback
							long nowTime = System.currentTimeMillis();
							if (nowTime - mStartTime > (mMsgCount + 1) * 200) {
								mMsgCount++;
								int prog = mDataSize > 0 ? (int) ((long) readsum * 100 / mDataSize) : 0;
								int rate = (int) ((long) readsum * 10 / (nowTime - mStartTime));
								sendHandler(DEF.HMSG_LOADING, prog << 24, rate, null);
							}
						}
					} else if (mFileType == FILETYPE_RAR) {
						// メモリキャッシュ読込時のみRAR展開する
						// ファイルキャッシュを作成するときはRAR展開不要
						mRarStream = new RarInputStream(mRarHandle, new BufferedInputStream(this, BIS_BUFFSIZE), page, mFileList[page], mHandler);
						while (mRunningFlag) {
							if (mCloseFlag) {
								break;
							}
							int readsize = mRarStream.read(buff, 0, buff.length);
							if (readsize <= 0) {
								break;
							}
							os.write(buff, 0, readsize);
						}
					} else {
						while (mRunningFlag) {
							if (mCloseFlag) {
								break;
							}
							int readsize = this.read(buff, 0, buff.length);
							if (readsize <= 0) {
								break;
							}
							os.write(buff, 0, readsize);
						}
					}
					os.flush();
					written = true;
					resultPath = file;
				} catch (IOException e) {
					Logcat.e(logLevel, "", e);
					resultPath = null;
				} finally {
					// 例外時もファイルを閉じる
					try {
						os.close();
					} catch (IOException e) {
						Logcat.e(logLevel, "", e);
						resultPath = null;
					}
					if (!written) {
						// 書きかけのファイルは残さない
						new File(file).delete();
					}
				}

				try {
					setLoadBitmapEnd();
				} catch (Exception e) {
					Logcat.e(logLevel, "", e);
					resultPath = null;
				}
			}
		}
		return resultPath;
	}

	// 指定ページをファイルに書き出し
	public String decompFile(int page) {
		return decompFile(page, null);
	}

	// 共有ファイルを削除する
	public void deleteShareCache() {
		int logLevel = Logcat.LOG_LEVEL_WARN;
		Logcat.d(logLevel, "開始します.");
		// キャッシュ保存先
		String path = DEF.getBaseDirectory() + "share/";

		// ファイルのリスト取得
		File[] files = new File(path).listFiles();
		if (files == null || files.length == 0) {
			// ファイルなし
			Logcat.d(logLevel, "ファイルがありません.");
			return;
		}

		// ファイルのリストを全て削除
		for (File file : files) {
			file.delete();
		}
	}

	public class CacheInputStream extends InputStream {
		InputStream	mInputStream;

		public CacheInputStream(InputStream is) throws IOException {
			mInputStream = is;
		}

		@Override
		public int read() throws IOException {
			// 自動生成されたメソッド・スタブ
			return 0;
		}

		@Override
		public int read(byte[] buf, int off, int len) throws IOException {
			int size = len;
			int total = 0;
			int ret = 0;
			while (size > 0) {
				ret = mInputStream.read(buf, off + total, size);
				if (!mRunningFlag) {
					return DEF.RETURN_CODE_TERMINATED;
				}
				if (ret <= 0) {
					break;
				}
				total += ret;
				size -= ret;
			}
			if (total == 0 && ret < 0) {
				return -1;
			}
			return total;
		}
	}

	public int getCacheIndex() {
		return mCacheIndex;
	}
}
