package src.comitton.fileview;

import android.content.SharedPreferences;
import android.os.Handler;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import src.comitton.common.DEF;
import src.comitton.common.Logcat;
import src.comitton.common.ThumbnailLoader;
import src.comitton.common.WaitFor;
import src.comitton.config.SetEpubActivity;
import src.comitton.fileview.data.FileData;
import src.comitton.imageview.ImageManager;
import src.comitton.textview.TextManager;
import src.comitton.common.BackgroundExecutor;

public class FileStatusLoader extends ThumbnailLoader implements Runnable {
    private static final String TAG = "FileStatusLoader";

    private SharedPreferences mSp;

    private final String mUser;
    private final String mPass;
    private final boolean mHidden;
    private final boolean mEpubViewer;
    private final boolean mEpubOrder;

    private volatile ImageManager mImageMgr;
    // 解析中のマネージャ (中断用). 解析スレッドだけが生成・解放する
    private volatile TextManager mTextMgr;
    private final FileTypeSortComparator mComparator;
    private FileData mCurrentFile;

    private WaitFor mWaitFor;
    private volatile boolean mSkip;

    public FileStatusLoader(AppCompatActivity activity, String uri, String path, String user, String pass, Handler handler, ArrayList<FileData> files, boolean hidden, boolean epubViewer) {
        super(activity, uri, path, handler, 0, files, 0, 0, 0, 0, 0);
        int logLevel = Logcat.LOG_LEVEL_WARN;
        Logcat.d(logLevel, "開始します.");

        mSp = PreferenceManager.getDefaultSharedPreferences(activity != null ? activity.getApplicationContext() : null);
        mSkip = false;

        mUser = user;
        mPass = pass;
        mHidden = hidden;
        mEpubViewer = epubViewer;
        mEpubOrder = SetEpubActivity.getEpubOrder(mSp);

        mComparator = new FileTypeSortComparator();
        mComparator.setDispRange(mFirstIndex, mLastIndex);

        for (int i = mFiles.size() - 1; i >= 0; i--) {
            if (mFiles.get(i).getType() == FileData.FILETYPE_PARENT || mFiles.get(i).getType() == FileData.FILETYPE_IMG) {
                removeFile(mFiles.get(i));
            }
        }

        readTextConfig();

        mWaitFor = new WaitFor(60000);

        // スレッド起動
        Thread thread = new Thread(this);
        thread.start();
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
        // 解析中なら中断させる (以前は別スレッドから参照を消して例外で止めていた)
        ImageManager imageMgr = mImageMgr;
        if (imageMgr != null) {
            imageMgr.setBreakTrigger();
        }
        TextManager textMgr = mTextMgr;
        if (textMgr != null) {
            textMgr.setBreakTrigger();
        }
        if (mWaitFor != null) {
            mWaitFor.interrupt();
        }
    }

    // 表示順に並べ替え、処理中のファイルより優先すべきものがあれば中断する
    private void sortAndInterrupt() {
        synchronized (mFileListLock) {
            if (mFiles != null && !mFiles.isEmpty()) {
                // mFiles の変更はすべて mFileListLock の中で行うので、ここで並べ替えても競合しない
                Collections.sort(mFiles, mComparator.copy());
                if (mComparator.compare(mFiles.get(0), mCurrentFile) < 0) {
                    interruptThread();
                }
            }
        }
    }

    public void update(FileData file) {
        synchronized (mFileListLock) {
            if (!mFiles.contains(file)) {
                mFiles.add(file);
            }
        }
        if (mWaitFor != null) {
            // 待機中なら新しいファイルを処理させる
            mWaitFor.interrupt();
        }

        // 並べ替えは非同期で行う
        BackgroundExecutor.execute(this::sortAndInterrupt);
    }

    // 表示中の範囲を設定
    public void setDispRange(int firstindex, int lastindex) {
        int logLevel = Logcat.LOG_LEVEL_WARN;
        Logcat.d(logLevel,"開始します. firstindex=" + firstindex + ", lastindex=" + lastindex);

        if (mFirstIndex != firstindex || mLastIndex != lastindex) {
            // スクロール位置に変化があったら実行する
            mFirstIndex = firstindex;
            mLastIndex = lastindex;
            mComparator.setDispRange(mFirstIndex, mLastIndex);

            // 並べ替えは非同期で行う
            BackgroundExecutor.execute(this::sortAndInterrupt);
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
            mSkip = false;
            mCurrentFile = null;
            synchronized (mFileListLock) {
                if (mFiles != null && !mFiles.isEmpty()) {
                    mCurrentFile = mFiles.get(0);
                }
            }
            if (mCurrentFile == null) {
                // ファイルがなくなったら新たにファイルが挿入されるまで待機する
                mWaitFor.sleep();
                continue;
            }

            boolean retCode = saveState(mCurrentFile);

            if (retCode) {
                DEF.sendMessage(mHandler, DEF.HMSG_FILE_STATUS, mCurrentFile.getIndex(), 0, mCurrentFile.getName());
            }

            if (!mSkip) {
                removeFile(mCurrentFile);
            }
        }
    }

    /**
     * 既読情報に変更が必要なら変更する
     * @param fileData
     * @return 既読情報に変化があったらtrue、それ以外はfalse
     */
    private boolean saveState(FileData fileData) {
        int logLevel = Logcat.LOG_LEVEL_WARN;
        Logcat.d(logLevel,"開始します. fileData=" + fileData.getName());

        if (fileData.getType() == FileData.FILETYPE_IMG){
            return false;
        }

        AppCompatActivity activity = getActivity();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return false;
        }

        int maxpage;
        int state;
        long date;
        long nowdate;

        String currentPath = DEF.relativePath(activity, mURI, mPath);
        String name = fileData.getName();
        String uri = DEF.createUrl(DEF.relativePath(activity, currentPath, name), mUser, mPass);
        Logcat.d(logLevel,"開始します. uri=" + uri);

        SharedPreferences.Editor ed = mSp.edit();
        String dateKey = uri + "#date";
        String maxpageKey;
        String stateKey;
        String cmpFile;
        String textFile;
        int openmode;

        switch (fileData.getType()) {
            case FileData.FILETYPE_TXT:
                maxpageKey = uri + "#maxpage";
                stateKey = uri;
                cmpFile = "";
                textFile = name;
                openmode = ImageManager.OPENMODE_TEXTVIEW;
                break;
            case FileData.FILETYPE_ARC, FileData.FILETYPE_PDF, FileData.FILETYPE_DIR:
                maxpageKey = uri + "#maxpage";
                stateKey = uri;
                cmpFile = name;
                textFile = null;
                openmode = ImageManager.OPENMODE_VIEW;
                break;
            case FileData.FILETYPE_EPUB:
                if (mEpubViewer == DEF.TEXT_VIEWER) {
                    maxpageKey = uri + "META-INF/container.xml" + "#maxpage";
                    stateKey = uri + "META-INF/container.xml";
                    cmpFile = name;
                    textFile = "META-INF/container.xml";
                    openmode = ImageManager.OPENMODE_TEXTVIEW;
                    break;
                }
                else {
                    maxpageKey = uri + "#maxpage";
                    stateKey = uri;
                    cmpFile = name;
                    textFile = null;
                    openmode = ImageManager.OPENMODE_VIEW;
                    break;
                }
            default:
                // なにもしない
                return false;
        }

        maxpage = mSp.getInt(maxpageKey, DEF.PAGENUMBER_NONE);
        state = mSp.getInt(stateKey, DEF.PAGENUMBER_UNREAD);
        try {
            if (state != DEF.PAGENUMBER_UNREAD) {
                // 未読じゃなければ
                nowdate = mSp.getInt(dateKey, 0);
                date = fileData.getDate();
                if (nowdate != date / 1000 || maxpage == DEF.PAGENUMBER_NONE) {
                    // タイムスタンプが変更されているか、maxpageが保存されていなければ
                    ImageManager imageMgr = new ImageManager(activity, currentPath, cmpFile, mUser, mPass, 0, mHandler, mHidden, openmode, 1);
                    TextManager textMgr = null;
                    mImageMgr = imageMgr;
                    try {
                        if (mSkip) {
                            // 生成中に中断が要求された
                            return false;
                        }
                        if (openmode == ImageManager.OPENMODE_VIEW) {
                            imageMgr.setEpubOrder(mEpubOrder);
                        }
                        imageMgr.LoadImageList(0, 0, 0);
                        Logcat.v(logLevel, "new ImageManager() cacheIndex=" + imageMgr.getCacheIndex());
                        if (mSkip) {
                            return false;
                        }

                        if (openmode == ImageManager.OPENMODE_TEXTVIEW) {
                            textMgr = new TextManager(imageMgr, textFile, mUser, mPass, mHandler, activity, fileData.getType());
                            mTextMgr = textMgr;
                            if (mSkip) {
                                return false;
                            }
                            textMgr.formatTextFile(mTextWidth, mTextHeight, mHeadSize, mBodySize, mRubiSize, mSpaceW, mSpaceH, mMarginW, mMarginH, mPicSize, mFontFile, mAscMode);
                            maxpage = textMgr.length();
                        } else {
                            maxpage = imageMgr.length();
                        }
                        if (mSkip) {
                            // 中断されたので結果は途中までしかない
                            return false;
                        }
                    } finally {
                        mTextMgr = null;
                        mImageMgr = null;
                        if (textMgr != null) {
                            textMgr.release();
                        }
                        imageMgr.closeFiles();
                    }

                    ed.putInt(maxpageKey, maxpage);
                    ed.putInt(dateKey, (int) ((date / 1000)));
                    ed.apply();
                }
                if (state == DEF.PAGENUMBER_READ && maxpage >= 0) {
                    ed.putInt(stateKey, Math.max(maxpage - 1, 0));
                    ed.apply();
                }

                return true;
            }
        }
        catch (Exception e) {
            Logcat.w(logLevel, "処理が中断されました. ", e);
        }
        return false;
    }

}
