package src.comitton.expandview;

import android.content.SharedPreferences;
import android.os.Handler;

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

public class ExpandFileStatusLoader extends ThumbnailLoader implements Runnable {
    private static final String TAG = "FileStatusLoader";

    private SharedPreferences mSp;

    private final String mCmpFile;
    private String mUser;
    private String mPass;
    private final boolean mHidden;

    private ImageManager mImageMgr;
    // 解析中の TextManager (中断用). 解析スレッドだけが生成・解放する
    private volatile TextManager mTextMgr;
    private final FileRangeComparator mComparator;
    private FileData mCurrentFile;

    private WaitFor mWaitFor;
    private volatile boolean mSkip;

    public ExpandFileStatusLoader(AppCompatActivity activity, ImageManager imgMgr, String uri, String path, String cmpFile, String user, String pass, Handler handler, ArrayList<FileData> files, boolean hidden) {
        super(activity, uri, path, handler, 0, files, 0, 0, 0, 0, 0);
        int logLevel = Logcat.LOG_LEVEL_WARN;
        Logcat.d(logLevel, "開始します.");

        mSp = PreferenceManager.getDefaultSharedPreferences(activity != null ? activity.getApplicationContext() : null);
        mSkip = false;

        mImageMgr = imgMgr;
        mCmpFile = cmpFile;
        mUser = user;
        mPass = pass;
        mHidden = hidden;

        mComparator = new FileRangeComparator();
        mComparator.setDispRange(mFirstIndex, mLastIndex);

        for (int i = 0; i < files.size(); ++i) {
            // 削除したらindexが変わるのでfilesで検査してmFilesから削除する
            if (files.get(i).getType() == FileData.FILETYPE_IMG) {
                removeFile(files.get(i));
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
                Collections.sort(mFiles, mComparator);
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
        String uri = DEF.createUrl(DEF.relativePath(activity, currentPath, mCmpFile), mUser, mPass);
        Logcat.v(logLevel,"開始します. uri=" + uri);

        SharedPreferences.Editor ed = mSp.edit();
        String dateKey = uri + "#date";
        String maxpageKey;
        String stateKey;
        String textFile;

        switch (fileData.getType()) {
            case FileData.FILETYPE_TXT:
                maxpageKey = uri + name + "#maxpage";
                stateKey = uri + name;
                textFile = name;
                break;
            case FileData.FILETYPE_EPUB_SUB:
                maxpageKey = uri + "META-INF/container.xml" + "#maxpage";
                stateKey = uri + "META-INF/container.xml";
                textFile = "META-INF/container.xml";
                break;
            default:
                // なにもしない
                return false;
        }

        maxpage = mSp.getInt(maxpageKey, DEF.PAGENUMBER_NONE);
        state = mSp.getInt(stateKey, DEF.PAGENUMBER_UNREAD);
        Logcat.v(logLevel,"開始します. maxpage=" + maxpage + ", state=" + state);
        try {
            if (state != DEF.PAGENUMBER_UNREAD) {
                // 未読じゃなければ
                nowdate = mSp.getInt(dateKey, 0);
                date = fileData.getDate();
                if (nowdate != date / 1000 || maxpage == DEF.PAGENUMBER_NONE) {
                    // タイムスタンプが変更されているか、maxpageが保存されていなければ
                    TextManager textMgr = new TextManager(mImageMgr, textFile, mUser, mPass, mHandler, activity, fileData.getType());
                    mTextMgr = textMgr;
                    if (mSkip) {
                        // 生成中に中断が要求された
                        textMgr.setBreakTrigger();
                    }
                    try {
                        textMgr.formatTextFile(mTextWidth, mTextHeight, mHeadSize, mBodySize, mRubiSize, mSpaceW, mSpaceH, mMarginW, mMarginH, mPicSize, mFontFile, mAscMode);
                        if (mSkip) {
                            // 中断されたので結果は途中までしかない
                            return false;
                        }
                        maxpage = textMgr.length();
                    } finally {
                        mTextMgr = null;
                        textMgr.release();
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
