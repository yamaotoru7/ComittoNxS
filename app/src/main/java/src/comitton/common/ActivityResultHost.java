package src.comitton.common;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * 非推奨の startActivityForResult / onActivityResult を Activity Result API に置き換えるための仲介.
 * <p>
 * 既存の requestCode ごとの結果処理をそのまま使えるように、起動時の requestCode を覚えておき、
 * 結果が返ったら {@link Callback#onActivityResultCompat} に渡す.
 * 同時に結果待ちにできるのは 1 件だけ (このアプリでは画面遷移とピッカーを重ねて起動しない).
 */
public final class ActivityResultHost {

	/** 結果を受け取る Activity が実装する */
	public interface Callback {
		void onActivityResultCompat(int requestCode, int resultCode, @Nullable Intent data);
	}

	/** 他のクラスから結果付きで起動するために Activity が実装する */
	public interface Launcher {
		void launchForResult(@NonNull Intent intent, int requestCode);
	}

	private static final String STATE_PENDING = "ActivityResultHost.pending";
	private static final int NONE = Integer.MIN_VALUE;

	private final ActivityResultLauncher<Intent> mLauncher;
	private int mPendingRequestCode = NONE;

	/**
	 * Activity のフィールド初期化など、STARTED より前に生成すること.
	 */
	public ActivityResultHost(@NonNull ComponentActivity activity, @NonNull Callback callback) {
		mLauncher = activity.registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
			int requestCode = mPendingRequestCode;
			mPendingRequestCode = NONE;
			if (requestCode != NONE) {
				callback.onActivityResultCompat(requestCode, result.getResultCode(), result.getData());
			}
		});
	}

	public void launch(@NonNull Intent intent, int requestCode) {
		mPendingRequestCode = requestCode;
		mLauncher.launch(intent);
	}

	/** onCreate で呼ぶ (プロセスが作り直された場合に結果を受け取れるように) */
	public void restore(@Nullable Bundle savedInstanceState) {
		if (savedInstanceState != null && savedInstanceState.containsKey(STATE_PENDING)) {
			mPendingRequestCode = savedInstanceState.getInt(STATE_PENDING, NONE);
		}
	}

	/** onSaveInstanceState で呼ぶ */
	public void save(@NonNull Bundle outState) {
		if (mPendingRequestCode != NONE) {
			outState.putInt(STATE_PENDING, mPendingRequestCode);
		}
	}

	/**
	 * Activity が {@link Launcher} なら Activity Result API で、そうでなければ従来の方法で起動する.
	 */
	@SuppressWarnings("deprecation")
	public static void launch(@NonNull Activity activity, @NonNull Intent intent, int requestCode) {
		if (activity instanceof Launcher) {
			((Launcher) activity).launchForResult(intent, requestCode);
		}
		else {
			activity.startActivityForResult(intent, requestCode);
		}
	}
}
