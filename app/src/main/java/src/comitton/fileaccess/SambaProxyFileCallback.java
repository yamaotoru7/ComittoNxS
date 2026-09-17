package src.comitton.fileaccess;

import android.annotation.TargetApi;
import android.app.Activity;
import android.system.ErrnoException;
import android.system.OsConstants;

import androidx.annotation.NonNull;

import java.io.IOException;

import jcifs.smb.SmbException;
import jcifs.smb.SmbRandomAccessFile;
import src.comitton.common.Logcat;
import src.comitton.common.BackgroundExecutor;

@TargetApi(26)
public class SambaProxyFileCallback extends StorageManagerCompat.ProxyFileDescriptorCallbackCompat {
    private static final String TAG = "SambaProxyFileCallback";

    private final Activity mActivity;
    private SmbRandomAccessFile mSmbRandomAccessFile = null;

    public SambaProxyFileCallback(@NonNull final Activity activity, @NonNull final String uri, @NonNull final String user, @NonNull final String pass) {
        int logLevel = Logcat.LOG_LEVEL_WARN;
        Logcat.d(logLevel, "uri=" + uri + ", user=" + user);
        mActivity = activity;
        try {
            mSmbRandomAccessFile = SmbFileAccess.openRandomAccessFile(uri, user, pass, "r");
        } catch (IOException e) {
            Logcat.e(logLevel, "Constructor Error.", e);
        }
    }

    @Override
    public long onGetSize() throws ErrnoException {
        final SmbRandomAccessFile file = openedFile("onGetSize");
        try {
            return BackgroundExecutor.callOffUiThread(TAG, file::length);
        } catch (IOException e) {
            Logcat.e(Logcat.LOG_LEVEL_WARN, "Get File Size Error.", e);
            throw new ErrnoException("onGetSize", OsConstants.EIO);
        }
    }

    @Override
    public int onRead(long offset, int size, byte[] data) throws ErrnoException {
        final SmbRandomAccessFile file = openedFile("onRead");
        try {
            return BackgroundExecutor.callOffUiThread(TAG, () -> {
                file.seek(offset);
                return file.read(data, 0, size);
            });
        } catch (IOException e) {
            Logcat.e(Logcat.LOG_LEVEL_WARN, "File Read Error.", e);
            throw new ErrnoException("onRead", OsConstants.EIO);
        }
    }

    @Override
    public int onWrite(long offset, int size, byte[] data) throws ErrnoException {
        final SmbRandomAccessFile file = openedFile("onWrite");
        try {
            // 以前は失敗しても書けたことにしていた
            return BackgroundExecutor.callOffUiThread(TAG, () -> {
                file.seek(offset);
                file.write(data, 0, size);
                return size;
            });
        } catch (IOException e) {
            Logcat.e(Logcat.LOG_LEVEL_WARN, "File Write Error.", e);
            throw new ErrnoException("onWrite", OsConstants.EIO);
        }
    }

    /** コンストラクタで開けなかった場合は EBADF */
    @NonNull
    private SmbRandomAccessFile openedFile(@NonNull String function) throws ErrnoException {
        SmbRandomAccessFile file = mSmbRandomAccessFile;
        if (file == null) {
            throw new ErrnoException(function, OsConstants.EBADF);
        }
        return file;
    }

    @Override
    public void onFsync() {
        // Nothing to do
    }

    @Override
    public void onRelease() {
        final SmbRandomAccessFile file = mSmbRandomAccessFile;
        if (file == null) {
            return;
        }
        // 非同期処理にする
        BackgroundExecutor.execute(() -> {
            try {
                file.close();
            } catch (SmbException e) {
                Logcat.e(Logcat.LOG_LEVEL_WARN, "File Release Error.", e);
            }
        });
    }
}
