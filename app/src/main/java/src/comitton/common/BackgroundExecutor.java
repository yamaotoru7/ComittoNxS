package src.comitton.common;

import androidx.annotation.NonNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
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
}
