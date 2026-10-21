package src.comitton.imageview;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.BitSet;

import src.comitton.common.DEF;
import src.comitton.common.Logcat;
import src.comitton.fileaccess.FileAccess;
import src.comitton.fileview.data.FileListItem;

/**
 * サーバ上の ZIP / RAR の各ページの圧縮データを、展開せずにファイルキャッシュへ書き込む.<br>
 * ImageManager のキャッシュスレッドとは別に動き、ImageManager のロックは取らない.<br>
 * 複数のスレッドがそれぞれ SMB のハンドルを開き、ページを {@link #CHUNK_SIZE} ごとに分けて同時に読む
 * (jcifs は 1 回の要求が 64KB までで、1 本だと往復待ちで頭打ちになるため).<br>
 * 読み終えた区画は覚えておくので、ページ移動で後回しになっても続きから読む.<br>
 * キャッシュファイル上の位置はアーカイブ内の位置と同じ (ImageManager.cheSeek と同じ対応).
 */
public class FileCacheDownloader {
	private static final String TAG = "FileCacheDownloader";
	private static final String PERF_TAG = "CachePerf";

	/** 1 回に読む大きさ. jcifs はこれを 64KB ずつの要求に分ける */
	static final int CHUNK_SIZE = 256 * 1024;
	/** 同時に読むスレッド数 */
	static final int THREAD_NUM = 3;
	/** 区画の読み込みに失敗したとき、ページを諦めるまでの回数 */
	private static final int MAX_RETRY = 3;
	/** 先読みする範囲. ImageManager の先読み範囲 (前方 4 ページ, 後方 2 ページの割合で 50 回) と同じ */
	private static final int CACHE_FPAGE = 4;
	private static final int CACHE_BPAGE = 2;
	private static final int CACHE_RANGE = 50;
	/** ZIP のローカルヘッダの拡張フィールドは最大 65535 バイト */
	private static final int MAX_ZIP_EXTRA = 0xFFFF;

	/** ImageManager から受け取る情報 */
	public interface Host {
		int currentPage();

		boolean isCached(int page);

		/** ページの全データがキャッシュに揃った. ワーカースレッドから呼ばれる */
		void onPageCached(int page);
	}

	/** 読み込み中の区画 */
	private static class Chunk {
		final int gen;
		final int page;
		final int index;
		final long pos;
		final int len;
		/** ZIP のローカルヘッダを読んでサイズを確定させる区画 */
		final boolean fixSize;

		Chunk(int gen, int page, int index, long pos, int len, boolean fixSize) {
			this.gen = gen;
			this.page = page;
			this.index = index;
			this.pos = pos;
			this.len = len;
			this.fixSize = fixSize;
		}
	}

	private final Activity mActivity;
	private final String mUri;
	private final String mUser;
	private final String mPass;
	private final File mCacheFile;
	private final boolean mZip;
	private final Host mHost;

	// 以下は mLock で保護
	private final Object mLock = new Object();
	private FileListItem[] mList;
	private BitSet[] mDone;
	private BitSet[] mBusy;
	private int[] mRetry;
	private boolean[] mFailed;
	/** reset() のたびに増やす. 古い一覧の区画の結果を捨てるため */
	private int mGen = 0;
	/** 画面側が待っているページ (最優先) */
	private int mUrgentPage = -1;
	/** キャッシュスレッドが待っているページ */
	private int mWantPage = -1;
	private boolean mRunning = true;

	private RandomAccessFile mCacheRaf;
	private FileChannel mCacheChannel;

	public FileCacheDownloader(@NonNull Activity activity, @NonNull String uri, @NonNull String user, @NonNull String pass,
							   @NonNull File cacheFile, @NonNull FileListItem[] list, boolean zip, @NonNull Host host) {
		mActivity = activity;
		mUri = uri;
		mUser = user;
		mPass = pass;
		mCacheFile = cacheFile;
		mZip = zip;
		mHost = host;
		initState(list);
	}

	private void initState(FileListItem[] list) {
		mList = list;
		mDone = new BitSet[list.length];
		mBusy = new BitSet[list.length];
		mRetry = new int[list.length];
		mFailed = new boolean[list.length];
	}

	public void start() throws IOException {
		// ImageManager.fileCacheInit が作ったファイルに、別のハンドルで書き込む
		mCacheRaf = new RandomAccessFile(mCacheFile, "rw");
		mCacheChannel = mCacheRaf.getChannel();
		for (int i = 0; i < THREAD_NUM; i++) {
			Thread t = new Thread(this::workerLoop, "comitton-download-" + i);
			t.setPriority(Thread.MIN_PRIORITY);
			t.start();
		}
	}

	public void close() {
		synchronized (mLock) {
			mRunning = false;
			mLock.notifyAll();
		}
		// SMB の読み込み中に割り込むと共有の接続を壊しかねないので interrupt はしない.
		// 書き込み中のワーカーがいても、閉じたチャネルへの書き込みは例外で終わるだけ
		try {
			if (mCacheRaf != null) {
				mCacheRaf.close();
			}
		}
		catch (IOException ignored) {
		}
	}

	/** ページの並びが変わった. 読み込み状態を捨てて新しい一覧で読み直す */
	public void reset(@NonNull FileListItem[] list) {
		synchronized (mLock) {
			mGen++;
			initState(list);
			mUrgentPage = -1;
			mWantPage = -1;
			mLock.notifyAll();
		}
	}

	/** 現在ページが変わったなど、優先順位を見直してほしいときに呼ぶ */
	public void wake() {
		synchronized (mLock) {
			mLock.notifyAll();
		}
	}

	/** 諦めたページ. 呼び出し側はサーバから直接読む */
	public boolean isFailed(int page) {
		synchronized (mLock) {
			return page >= 0 && page < mFailed.length && mFailed[page];
		}
	}

	/** 読み込み済みのバイト数 (進捗表示用) */
	public long doneBytes(int page) {
		synchronized (mLock) {
			if (page < 0 || page >= mList.length || mDone[page] == null) {
				return 0;
			}
			long total = 0;
			BitSet done = mDone[page];
			int len = mList[page].cmplen;
			for (int i = done.nextSetBit(0); i >= 0; i = done.nextSetBit(i + 1)) {
				total += Math.min(CHUNK_SIZE, len - (long) i * CHUNK_SIZE);
			}
			return Math.max(total, 0);
		}
	}

	/** ページの読み込みを待つ間の中断条件 */
	public interface Cancel {
		boolean isCanceled();
	}

	/** 待っている間の進捗通知 */
	public interface Progress {
		void onProgress(long done, long total);
	}

	/**
	 * ページがキャッシュに揃うまで待つ.
	 * @param urgent true なら画面側の要求として最優先、false ならキャッシュスレッドの要求として次に優先する
	 * @return 揃ったら true. 諦めた・中断された・閉じられたら false
	 */
	public boolean waitPage(int page, boolean urgent, @NonNull Cancel cancel, Progress progress) {
		long lastNotice = 0;
		synchronized (mLock) {
			if (urgent) {
				mUrgentPage = page;
			}
			else {
				mWantPage = page;
			}
			mLock.notifyAll();
		}
		try {
			while (true) {
				if (mHost.isCached(page)) {
					return true;
				}
				synchronized (mLock) {
					if (!mRunning || page >= mFailed.length || mFailed[page]) {
						return false;
					}
				}
				if (cancel.isCanceled()) {
					return false;
				}
				long now = System.currentTimeMillis();
				if (progress != null && now - lastNotice >= 200) {
					lastNotice = now;
					progress.onProgress(doneBytes(page), mList[page].cmplen);
				}
				synchronized (mLock) {
					if (!mHost.isCached(page)) {
						try {
							// 区画が終わるたびに起こされる. 中断条件を見るため長くは待たない
							mLock.wait(50);
						}
						catch (InterruptedException e) {
							return false;
						}
					}
				}
			}
		}
		finally {
			synchronized (mLock) {
				if (urgent && mUrgentPage == page) {
					mUrgentPage = -1;
				}
				else if (!urgent && mWantPage == page) {
					mWantPage = -1;
				}
			}
		}
	}

	/** 次に読む区画を選んで読み込み中にする. 無ければ null. mLock 配下で呼ぶ */
	private Chunk nextChunkLocked() {
		int total = mList.length;
		if (total == 0) {
			return null;
		}
		Chunk c;
		if ((c = chunkOfLocked(mUrgentPage)) != null) {
			return c;
		}
		if ((c = chunkOfLocked(mWantPage)) != null) {
			return c;
		}
		int cur = Math.max(0, Math.min(mHost.currentPage(), total - 1));
		if ((c = chunkOfLocked(cur)) != null) {
			return c;
		}
		for (int range = 0; range < CACHE_RANGE; range++) {
			// 順方向
			int st = cur + CACHE_FPAGE * range + 1;
			int ed = Math.min(cur + CACHE_FPAGE * (range + 1), total - 1);
			for (int p = st; p <= ed; p++) {
				if ((c = chunkOfLocked(p)) != null) {
					return c;
				}
			}
			// 逆方向の割合は減らす
			st = cur - CACHE_BPAGE * range - 1;
			ed = Math.max(cur - CACHE_BPAGE * (range + 1), 0);
			for (int p = st; p >= ed; p--) {
				if ((c = chunkOfLocked(p)) != null) {
					return c;
				}
			}
		}
		return null;
	}

	/** ページの中で次に読む区画. 無ければ null. mLock 配下で呼ぶ */
	private Chunk chunkOfLocked(int page) {
		if (page < 0 || page >= mList.length || mFailed[page] || mHost.isCached(page)) {
			return null;
		}
		FileListItem item = mList[page];
		if (item.cmplen <= 0) {
			return null;
		}
		if (mDone[page] == null) {
			mDone[page] = new BitSet();
			mBusy[page] = new BitSet();
		}
		BitSet done = mDone[page];
		BitSet busy = mBusy[page];
		if (mZip && !item.sizefixed) {
			// 先頭の区画でローカルヘッダの拡張フィールドを読むまでサイズが確定しない
			if (done.get(0) || busy.get(0)) {
				return null;
			}
			busy.set(0);
			// 拡張フィールドの分も含めて読めるよう、確定前のサイズに最大長を足して読む
			// (はみ出した分は次のエントリの正しいデータなので書いても問題ない)
			int len = (int) Math.min(CHUNK_SIZE, (long) item.cmplen + MAX_ZIP_EXTRA);
			return new Chunk(mGen, page, 0, item.cmppos, len, true);
		}
		int count = (int) (((long) item.cmplen + CHUNK_SIZE - 1) / CHUNK_SIZE);
		for (int i = 0; i < count; i++) {
			if (!done.get(i) && !busy.get(i)) {
				busy.set(i);
				long off = (long) i * CHUNK_SIZE;
				int len = (int) Math.min(CHUNK_SIZE, item.cmplen - off);
				return new Chunk(mGen, page, i, item.cmppos + off, len, false);
			}
		}
		return null;
	}

	private void workerLoop() {
		int logLevel = Logcat.LOG_LEVEL_WARN;
		byte[] buf = new byte[CHUNK_SIZE];
		FileAccess file = null;
		try {
			while (true) {
				Chunk chunk = null;
				synchronized (mLock) {
					while (mRunning && (chunk = nextChunkLocked()) == null) {
						try {
							mLock.wait(1000);
						}
						catch (InterruptedException ignored) {
						}
					}
					if (!mRunning) {
						return;
					}
				}

				long start = System.nanoTime();
				int read = -1;
				try {
					if (file == null) {
						file = open();
					}
					read = readFully(file, chunk.pos, buf, chunk.len);
				}
				catch (IOException e) {
					Logcat.w(logLevel, "区画の読み込みに失敗しました. page=" + chunk.page, e);
					closeQuietly(file);
					file = null;
				}
				finishChunk(chunk, buf, read, start);
			}
		}
		finally {
			closeQuietly(file);
		}
	}

	private FileAccess open() throws IOException {
		FileAccess file = new FileAccess(mActivity, mUri, mUser, mPass, null);
		file.openRandomAccessFile("r");
		return file;
	}

	private static void closeQuietly(FileAccess file) {
		if (file != null) {
			try {
				file.close();
			}
			catch (IOException ignored) {
			}
		}
	}

	/** pos から len バイト読む. ファイル終端で短くなることがある */
	private int readFully(FileAccess file, long pos, byte[] buf, int len) throws IOException {
		file.seek(pos);
		int total = 0;
		while (total < len) {
			int ret = file.read(buf, total, len - total);
			if (ret <= 0) {
				break;
			}
			total += ret;
		}
		return total;
	}

	/** 読み終えた区画を書き込み、状態を更新する */
	private void finishChunk(Chunk chunk, byte[] buf, int read, long start) {
		int logLevel = Logcat.LOG_LEVEL_WARN;
		boolean ok = false;
		int len = chunk.len;
		if (read > 0) {
			synchronized (mLock) {
				if (chunk.gen != mGen) {
					// 一覧が変わったので捨てる
					return;
				}
				FileListItem item = mList[chunk.page];
				if (chunk.fixSize) {
					// ImageManager.cmpRead と同じく、拡張フィールドの長さを足してサイズを確定する
					if (read >= ImageManager.SIZE_LOCALHEADER && getInt(buf, ImageManager.OFFSET_LCL_SIGNA_LEN) == 0x04034b50) {
						item.cmplen += getShort(buf, ImageManager.OFFSET_LCL_EXTRA_LEN);
					}
					item.sizefixed = true;
				}
				// この区画として有効な長さ
				len = (int) Math.min(chunk.len, item.cmplen - (long) chunk.index * CHUNK_SIZE);
			}
			if (read >= len) {
				try {
					// 書き込みは位置指定なので、他のスレッドと並行してよい
					mCacheChannel.write(ByteBuffer.wrap(buf, 0, read), chunk.pos);
					ok = true;
				}
				catch (IOException e) {
					Logcat.w(logLevel, "キャッシュへの書き込みに失敗しました. page=" + chunk.page, e);
				}
			}
		}

		boolean completed = false;
		synchronized (mLock) {
			if (chunk.gen != mGen) {
				return;
			}
			mBusy[chunk.page].clear(chunk.index);
			if (ok) {
				mDone[chunk.page].set(chunk.index);
				int count = (int) (((long) mList[chunk.page].cmplen + CHUNK_SIZE - 1) / CHUNK_SIZE);
				completed = mDone[chunk.page].cardinality() >= count && mDone[chunk.page].nextClearBit(0) >= count;
			}
			else if (++mRetry[chunk.page] >= MAX_RETRY) {
				// 諦めて、呼び出し側でサーバから直接読ませる
				mFailed[chunk.page] = true;
			}
			if (completed) {
				// 以降の読み込み状態は不要
				mDone[chunk.page] = null;
				mBusy[chunk.page] = null;
			}
		}
		if (completed) {
			mHost.onPageCached(chunk.page);
		}
		synchronized (mLock) {
			mLock.notifyAll();
		}
		if (DEF.DEBUG) {
			long ms = (System.nanoTime() - start) / 1000000L;
			if (!ok) {
				Log.i(PERF_TAG, "[" + Thread.currentThread().getName() + "] chunk FAILED page=" + chunk.page + " index=" + chunk.index + " read=" + read + " time=" + ms + "ms");
			}
			else if (completed) {
				Log.i(PERF_TAG, "[" + Thread.currentThread().getName() + "] cached page=" + chunk.page + " current=" + mHost.currentPage());
			}
		}
	}

	private static int getInt(byte[] b, int pos) {
		return (b[pos] & 0xFF) | ((b[pos + 1] & 0xFF) << 8) | ((b[pos + 2] & 0xFF) << 16) | ((b[pos + 3] & 0xFF) << 24);
	}

	private static int getShort(byte[] b, int pos) {
		return (b[pos] & 0xFF) | ((b[pos + 1] & 0xFF) << 8);
	}
}
