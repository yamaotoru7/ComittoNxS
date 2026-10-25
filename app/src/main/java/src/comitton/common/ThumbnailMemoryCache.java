package src.comitton.common;

import android.graphics.Bitmap;
import android.util.LruCache;

/**
 * 縮小済みのサムネイルをフォルダを移動しても残しておくメモリキャッシュ
 * ネイティブのサムネイル領域は表示中のフォルダの分しか持たないので、戻ってきたフォルダは
 * ここから取り出してファイルキャッシュの展開と縮小を飛ばす
 */
public class ThumbnailMemoryCache {
	private static final int MAX_BYTES = (int) Math.min(32L * 1024 * 1024, Runtime.getRuntime().maxMemory() / 8);

	private static final LruCache<String, Bitmap> sCache = new LruCache<String, Bitmap>(MAX_BYTES) {
		@Override
		protected int sizeOf(String key, Bitmap value) {
			return value.getByteCount();
		}
	};

	// 切り抜きと余白の設定でも縮小結果が変わるのでキーに含める
	private static String makeKey(String pathcode, int crop, int margin) {
		return pathcode + "#" + crop + "#" + margin;
	}

	public static Bitmap get(String pathcode, int crop, int margin) {
		return sCache.get(makeKey(pathcode, crop, margin));
	}

	public static void put(String pathcode, int crop, int margin, Bitmap bm) {
		sCache.put(makeKey(pathcode, crop, margin), bm);
	}

	// ファイルキャッシュを書き換え・削除したときに呼ぶ
	public static void remove(String pathcode) {
		String prefix = pathcode + "#";
		for (String key : sCache.snapshot().keySet()) {
			if (key.startsWith(prefix)) {
				sCache.remove(key);
			}
		}
	}

	public static void clear() {
		sCache.evictAll();
	}
}
