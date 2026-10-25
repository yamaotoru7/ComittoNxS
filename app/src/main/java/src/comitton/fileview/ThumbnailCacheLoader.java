package src.comitton.fileview;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Handler;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import src.comitton.common.DEF;
import src.comitton.common.ImageAccess;
import src.comitton.common.Logcat;
import src.comitton.common.ThumbnailLoader;
import src.comitton.common.ThumbnailMemoryCache;
import src.comitton.common.WaitFor;
import src.comitton.config.SetEpubActivity;
import src.comitton.fileview.data.FileData;
import src.comitton.imageview.ImageManager;
import src.comitton.jni.CallImgLibrary;
import src.comitton.textview.TextManager;
import src.comitton.common.BackgroundExecutor;

public class ThumbnailCacheLoader extends ThumbnailLoader implements Runnable {
    private static final String TAG = "ThumbnailCacheLoader";

    private SharedPreferences mSp;

    private FileThumbnailLoader mFileThumbnailLoader;
    private FileRangeComparator mComparator;
    private FileData mCurrentFile;

    // ファイルキャッシュの展開と縮小を並列に行うスレッド数と、まとめて処理する数
    private static final int DECODE_THREADS = 3;
    private static final int DECODE_BATCH = DECODE_THREADS * 2;
    private static final ExecutorService sDecodeExecutor = Executors.newFixedThreadPool(DECODE_THREADS);

    private WaitFor mWaitFor;
    private boolean mSkip;
    private boolean mSleep;
    private boolean mSorting;

    public ThumbnailCacheLoader(AppCompatActivity activity, FileThumbnailLoader loader, String uri, String path, String user, String pass, Handler handler, long id, ArrayList<FileData> files, int sizeW, int sizeH, int cachenum, boolean hidden, int crop, int margin, boolean epubViewer) {
        super(activity, uri, path, handler, id, files, sizeW, sizeH, cachenum, crop, margin);
        int logLevel = Logcat.LOG_LEVEL_WARN;
        Logcat.d(logLevel, "開始します.");

        mSp = PreferenceManager.getDefaultSharedPreferences(activity != null ? activity.getApplicationContext() : null);
        mSkip = false;
        mSleep = false;
        mSorting = false;

        mFileThumbnailLoader = loader;
        mComparator = new FileRangeComparator();
        mComparator.setDispRange(mFirstIndex, mLastIndex);

        mWaitFor = new WaitFor(60000);

        // スレッド起動
        Logcat.d(logLevel, "スレッドを開始します.");
        Thread thread = new Thread(this);
        thread.start();
        Logcat.d(logLevel, "終了します.");
    }

    // 解放
    public void releaseThumbnail() {
    }

    // スレッド停止
    public void breakThread() {
        int logLevel = Logcat.LOG_LEVEL_WARN;
        Logcat.d(logLevel, "開始します.");
        super.breakThread();
    }

    protected void interruptThread() {
        int logLevel = Logcat.LOG_LEVEL_WARN;
        Logcat.d(logLevel,"開始します.");
        mSkip = true;
        if (mWaitFor != null) {
            mWaitFor.interrupt();
        }
    }

    // 表示中の範囲を設定
    public void setDispRange(int firstindex, int lastindex) {
        int logLevel = Logcat.LOG_LEVEL_WARN;
        Logcat.d(logLevel,"開始します. firstindex=" + firstindex + ", lastindex=" + lastindex);

        if (mFirstIndex != firstindex || mLastIndex != lastindex) {
            Logcat.v(logLevel,"表示範囲に変化があります.");
            // スクロール位置に変化があったら実行する
            mFirstIndex = firstindex;
            mLastIndex = lastindex;
            mComparator.setDispRange(mFirstIndex, mLastIndex);

            if (!mSorting) {
                // 中でループさせたいので非同期処理にする
                ExecutorService executor = BackgroundExecutor.get();
                executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (mFileListLock) {
                            mSorting = true;
                            if (mFiles != null && !mFiles.isEmpty()) {
                                // 一覧の変更はすべて mFileListLock の中で行うので、並べ替えは競合しない
                                Collections.sort(mFiles, mComparator);
                                if (CallImgLibrary.ThumbnailCheckAll(mID) != 0) {
                                    // メモリキャッシュに全部入り切れていない時は実行する
                                    interruptThread();
                                }
                            }
                            mSorting = false;
                        }
                    }
                });
            }
        }
        Logcat.d(logLevel,"終了します.");
    }

    // スレッド開始
    public void run() {
        int logLevel = Logcat.LOG_LEVEL_WARN;
        Logcat.d(logLevel, "開始します. mPath=" + mPath);

        if (mFiles.isEmpty()) {
            return;
        }

        while(!mThreadBreak) {
            AppCompatActivity activity = getActivity();
            if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
                break;
            }
            Logcat.v(logLevel,"while: 開始します. mFirstIndex=" + mFirstIndex + ", mLastIndex=" + mLastIndex);

            mSleep = false;
            mSkip = false;

            int i = 0;
            while (i < mFiles.size() && !mThreadBreak && !mSkip) {
                if (CallImgLibrary.ThumbnailCheckAll(mID) == 0) {
                    // メモリキャッシュに全部入り切れたら
                    // ファイルキャッシュが更新されるまで待機する
                    Logcat.v(logLevel,"while: 全てのサムネイルがメモリキャッシュに格納されています.");
                    mSleep = true;
                    break;
                }

                // まだメモリキャッシュにないものを表示の中心に近い順に集める
                ArrayList<FileData> batch = new ArrayList<>(DECODE_BATCH);
                while (i < mFiles.size() && batch.size() < DECODE_BATCH) {
                    FileData file = mFiles.get(i++);
                    if (CallImgLibrary.ThumbnailCheck(mID, file.getIndex()) > 0) {
                        // 既に読み込み済み
                        continue;
                    }
                    batch.add(file);
                }
                if (batch.isEmpty()) {
                    mSleep = true;
                    break;
                }

                // ファイルキャッシュの展開と縮小は並列に行う
                ArrayList<Future<Bitmap>> results = new ArrayList<>(batch.size());
                for (FileData file : batch) {
                    results.add(sDecodeExecutor.submit(() -> decodeCache(file)));
                }

                // メモリキャッシュへの格納は表示に近い順に行う
                boolean full = false;
                for (int j = 0; j < batch.size(); ++j) {
                    mCurrentFile = batch.get(j);
                    Bitmap bm;
                    try {
                        bm = results.get(j).get();
                    } catch (InterruptedException | ExecutionException e) {
                        bm = null;
                    }
                    if (full || mThreadBreak) {
                        // 残りの展開が終わるのを待つだけ
                        continue;
                    }
                    try {
                        if (storeMemory(mCurrentFile, bm)) {
                            Logcat.v(logLevel,"while: 変更を通知します. index=" + mCurrentFile.getIndex() + ", mCurrentFile=" + mCurrentFile.getName());
                            DEF.sendMessage(mHandler, DEF.HMSG_THUMBNAIL, mCurrentFile.getIndex(), 0, mCurrentFile.getName());
                        }
                    }
                    catch (CacheException e) {
                        Logcat.v(logLevel,"while: CacheException: ループを抜けます.  index=" + mCurrentFile.getIndex() + ", mCurrentFile=" + mCurrentFile.getName());
                        full = true;
                    }
                }

                if (full || i >= mFiles.size()) {
                    mSleep = true;
                    break;
                }
            }

            if (mSleep) {
                // メモリキャッシュが空けられなかったかファイルを最後まで処理したら
                // スクロールするかファイルキャッシュが更新されるまで待機する
                Logcat.v(logLevel,"while: スリープします.");
                mWaitFor.sleep();
                Logcat.v(logLevel,"while: スリープを解除します.");
            }
            Logcat.v(logLevel,"while: 先頭に戻ります.");
        }
        Logcat.d(logLevel, "終了します.");
    }

    /**
     * サムネイルを格納できる形で取り出す. メモリに残っていればそれを、なければファイルキャッシュを展開して縮小する
     * 複数スレッドから呼ばれる
     * @param fileData
     * @return 縮小済みのビットマップ. ファイルキャッシュがなければ null
     */
    private Bitmap decodeCache(FileData fileData) {
        int logLevel = Logcat.LOG_LEVEL_WARN;
        Logcat.d(logLevel, "開始します. fileData=" + fileData.getName());

        AppCompatActivity activity = getActivity();
        if (activity == null || activity.isFinishing() || activity.isDestroyed() || mThreadBreak) {
            return null;
        }

        String pathcode = getCacheCode(activity, fileData);
        Bitmap bm = ThumbnailMemoryCache.get(pathcode, mThumbCrop, mThumbMargin);
        if (bm != null) {
            Logcat.d(logLevel,"メモリに残っていました. fileData=" + fileData.getName());
            return bm;
        }

        // キャッシュから読込
        if (!checkThumbnailCache(pathcode)) {
            return null;
        }
        bm = loadThumbnailCache(pathcode);
        if (bm == null) {
            Logcat.d(logLevel,"ファイルキャッシュが空でした.スキップします. fileData=" + fileData.getName());
            return null;
        }
        Logcat.d(logLevel,"ファイルキャッシュがありました. fileData=" + fileData.getName());
        bm = prepareBitmap(bm, mThumbSizeW, mThumbSizeH);
        if (bm == null) {
            return null;
        }
        ThumbnailMemoryCache.put(pathcode, mThumbCrop, mThumbMargin, bm);
        if (logLevel <= Logcat.LOG_LEVEL_DEBUG) {
            bm = ImageAccess.setText(bm, String.valueOf(fileData.getIndex()), Color.RED, DEF.ALIGN_TOP);
        }
        return bm;
    }

    /**
     * 縮小済みのビットマップデータをメモリキャッシュに格納する
     * @param fileData
     * @param bm
     * @return メモリキャッシュに変動があればtrue、それ以外はfalse
     * @exception CacheException メモリキャッシュが空けられなかった場合
     */
    private boolean storeMemory(FileData fileData, Bitmap bm) throws CacheException {
        if (bm == null) {
            return false;
        }
        // 表示中のものは表示範囲外のものを追い出してでも格納する
        boolean priority = mFirstIndex <= fileData.getIndex() && fileData.getIndex() <= mLastIndex;
        return mFileThumbnailLoader.storeMemory(fileData.getIndex(), bm, priority);
    }

}
