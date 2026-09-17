package src.comitton.fileaccess;

import java.io.IOException;
import java.io.InputStream;

import src.comitton.common.DEF;
import src.comitton.fileview.data.FileListItem;
import src.comitton.jni.CallJniLibrary;

import android.os.Handler;
import android.os.Message;

/**
 * RAR の 1 エントリを展開して読み出すストリーム.
 * <p>
 * 展開用のハンドル (CallJniLibrary.rarOpen) は呼び出し側が所有する. このクラスの close() では解放しない.
 */
public class RarInputStream extends InputStream {

	private final long mHandle;
	private final int mPage;

	public RarInputStream(long handle, InputStream is, int page, FileListItem fi) throws IOException {
		this(handle, is, page, fi, null);
	}

	/**
	 * @param is エントリのヘッダ位置から読み出せる入力
	 */
	public RarInputStream(long handle, InputStream is, int page, FileListItem fi, Handler handler) throws IOException {
		mHandle = handle;
		mPage = page;
		if (handle == 0) {
			throw new IOException("RAR handle is not open.");
		}
		if (fi.encrypted) {
			throw new IOException("Password protected RAR entries are not supported.");
		}

		int header = fi.header;
		int datasize = fi.cmplen - header;

		int ret = CallJniLibrary.rarInit(handle, datasize, fi.orglen, fi.version, fi.winsize, fi.nocomp);
		if (ret != CallJniLibrary.RAR_OK) {
			throw new IOException("RAR init error. code=" + ret);
		}

		byte[] buff = new byte[1024 * 100];
		// ヘッダを読み飛ばす
		skipFully(is, buff, header);

		int remain = datasize;
		int msgCount = 0;
		long startTime = System.currentTimeMillis();
		while (remain > 0) {
			int readsize = Math.min(buff.length, remain);
			int retsize = is.read(buff, 0, readsize);
			if (retsize <= 0) {
				// 途中で切れている. rarDecomp で検出される
				break;
			}
			remain -= retsize;
			int written = CallJniLibrary.rarWrite(handle, buff, 0, retsize);
			if (written < 0) {
				throw new IOException("RAR write error. code=" + written);
			}
			if (handler != null) {
				long nowTime = System.currentTimeMillis();
				if (nowTime - startTime > (msgCount + 1) * 200L) {
					msgCount++;
					int prog = (int) ((long) (datasize - remain) * 100 / Math.max(datasize, 1));
					int rate = (int) ((long) (datasize - remain) * 10 / Math.max(nowTime - startTime, 1));
					Message message = new Message();
					message.what = DEF.HMSG_LOADING;
					message.arg1 = prog << 24;
					message.arg2 = rate;
					message.obj = null;
					handler.sendMessage(message);
				}
			}
		}

		// 変換
		ret = CallJniLibrary.rarDecomp(handle);
		if (ret != CallJniLibrary.RAR_OK) {
			throw new IOException(decompErrorMessage(ret, fi));
		}
	}

	private static void skipFully(InputStream is, byte[] buff, int size) throws IOException {
		int remain = size;
		while (remain > 0) {
			int retsize = is.read(buff, 0, Math.min(buff.length, remain));
			if (retsize <= 0) {
				throw new IOException("RAR header is truncated.");
			}
			remain -= retsize;
		}
	}

	private static String decompErrorMessage(int code, FileListItem fi) {
		switch (code) {
			case CallJniLibrary.RAR_ERROR_TRUNCATED:
				return "RAR data is truncated. name=" + fi.name;
			case CallJniLibrary.RAR_ERROR_INCOMPLETE:
				return "RAR data is broken or uses an unsupported format (solid archive etc.). name=" + fi.name;
			case CallJniLibrary.RAR_ERROR_MALLOC:
				return "Not enough memory to unpack RAR. name=" + fi.name;
			default:
				return "RAR unpack error. code=" + code + ", name=" + fi.name;
		}
	}

	// 読み込み済みのページを返す
	public int getLoadPage() {
		return mPage;
	}

	// 現在の展開ファイルをもう一度最初から返す
	public void initSeek() {
		CallJniLibrary.rarInitSeek(mHandle);
	}

	@Override
	public int read() throws IOException {
		byte[] one = new byte[1];
		int ret = read(one, 0, 1);
		return ret <= 0 ? -1 : (one[0] & 0xFF);
	}

	@Override
	public int read(byte[] buf, int off, int len) throws IOException {
		if (len == 0) {
			return 0;
		}
		int ret = CallJniLibrary.rarRead(mHandle, buf, off, len);
		if (ret < 0) {
			throw new IOException("RAR read error. code=" + ret);
		}
		if (ret == 0) {
			ret = -1;
		}
		return ret;
	}

	@Override
	public void close() {
		// ハンドルは所有者 (ImageManager) が解放する
	}
}
