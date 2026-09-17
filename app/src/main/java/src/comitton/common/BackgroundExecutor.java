package src.comitton.common;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * アプリ全体で共有するバックグラウンド実行用のスレッドプール.<br>
 * 以前は呼び出しごとに {@code Executors.newSingleThreadExecutor()} を作って shutdown していなかったため、
 * 非デーモンスレッドが溜まっていた. こちらはアイドル 60 秒でスレッドが回収される.<br>
 * 共有プールなので {@code shutdown()} を呼んではいけない.
 */
public final class BackgroundExecutor {
	/** UI スレッドからネットワーク処理の完了を待つ上限 (秒) */
	public static final long UI_WAIT_TIMEOUT_SEC = 30;

	private static final AtomicInteger sCount = new AtomicInteger(1);

	private static final ExecutorService POOL = Executors.newCachedThreadPool(new ThreadFactory() {
		@Override
		public Thread newThread(@NonNull Runnable r) {
			Thread t = new Thread(r, "comitton-bg-" + sCount.getAndIncrement());
			t.setDaemon(true);
			return t;
		}
	});

	private BackgroundExecutor() {
	}

	@NonNull
	public static ExecutorService get() {
		return POOL;
	}

	public static void execute(@NonNull Runnable runnable) {
		POOL.execute(runnable);
	}

	/** {@link #callOffUiThread} で実行する入出力処理 */
	public interface IoCallable<T> {
		T call() throws IOException;
	}

	/**
	 * UI スレッド以外ならその場で、UI スレッドならプールで実行して最大 {@link #UI_WAIT_TIMEOUT_SEC} 秒待つ.<br>
	 * 処理が投げた IOException / RuntimeException はそのまま呼び出し元へ投げ直す.
	 * 待ち時間切れと割り込みは InterruptedIOException になる.
	 * @param where UI スレッドから呼ばれたときの警告ログに出す呼び出し元名
	 */
	public static <T> T callOffUiThread(@NonNull String where, @NonNull IoCallable<T> task) throws IOException {
		if (!DEF.isUiThread()) {
			return task.call();
		}
		DEF.warnUiThreadIo(where);
		Future<T> future = POOL.submit(task::call);
		try {
			return future.get(UI_WAIT_TIMEOUT_SEC, TimeUnit.SECONDS);
		} catch (ExecutionException e) {
			Throwable cause = e.getCause();
			if (cause instanceof IOException) {
				throw (IOException) cause;
			}
			if (cause instanceof RuntimeException) {
				throw (RuntimeException) cause;
			}
			if (cause instanceof Error) {
				throw (Error) cause;
			}
			throw new IOException(cause);
		} catch (TimeoutException e) {
			throw new InterruptedIOException(where + ": timed out");
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new InterruptedIOException(where + ": interrupted");
		}
	}
}
