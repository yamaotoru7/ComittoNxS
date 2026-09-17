package src.comitton.jni;

/**
 * RAR 展開 (unrar) の JNI.
 * <p>
 * rarOpen() で作ったハンドルごとにバッファと展開状態を持つ. 使い終わったら rarClose() で解放する.
 * 1 つのハンドルは 1 エントリずつ rarInit → rarWrite → rarDecomp → rarRead の順に使う.
 */
public class CallJniLibrary {
	static {
		// UNRARライブラリ
		System.loadLibrary ("comitton");
	}

	// callUnrar.cpp と同じ値
	public static final int RAR_OK = 0;
	public static final int RAR_ERROR_INVALID_HANDLE = -1;
	public static final int RAR_ERROR_INVALID_SIZE = -2;
	public static final int RAR_ERROR_UNPACK = -3;
	public static final int RAR_ERROR_INCOMPLETE = -4;
	public static final int RAR_ERROR_TRUNCATED = -5;
	public static final int RAR_ERROR_MALLOC = -1001;

	/** @return ハンドル. 失敗時は 0 */
	public static native long rarOpen();
	public static native int rarAlloc(long handle, int cmplen, int orglen);
	/** @param winsize エントリの辞書サイズ (バイト). 0 以下なら既定値 */
	public static native int rarInit(long handle, int cmplen, int orglen, int rarver, long winsize, boolean nocomp);
	public static native int rarWrite(long handle, byte[] data, int off, int len);
	public static native int rarDecomp(long handle);
	public static native int rarRead(long handle, byte[] data, int off, int len);
	public static native int rarInitSeek(long handle);
	public static native void rarClose(long handle);
}
