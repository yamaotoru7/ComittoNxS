package src.comitton.common;

import android.app.Activity;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.activity.ComponentActivity;
import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/**
 * Android 15 (Edge-to-Edge) および全画面・システムバー制御のためのヘルパークラス
 */
public class SystemUiHelper {

    /**
     * Edge-to-Edge を有効化し、ノッチ・カットアウト領域まで全画面表示可能にする
     */
    public static void setupEdgeToEdge(Activity activity) {
        if (activity == null) return;
        Window window = activity.getWindow();

        // Android 9 (API 28) 以降: カットアウト領域（ノッチ・パンチホール）への全画面表示を許可
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams params = window.getAttributes();
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            window.setAttributes(params);
        }

        if (activity instanceof ComponentActivity) {
            EdgeToEdge.enable((ComponentActivity) activity);
        } else {
            WindowCompat.setDecorFitsSystemWindows(window, false);
        }
    }

    /**
     * 指定されたビューにシステムバー（およびカットアウト）のインセットをパディングとして適用
     */
    public static void applySystemWindowInsets(View view, boolean top, boolean bottom, boolean left, boolean right) {
        if (view == null) return;
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            int pTop = top ? bars.top : 0;
            int pBottom = bottom ? bars.bottom : 0;
            int pLeft = left ? bars.left : 0;
            int pRight = right ? bars.right : 0;
            v.setPadding(pLeft, pTop, pRight, pBottom);
            return insets;
        });
    }

    /**
     * ビュワー用: 表示中のシステムバー（ステータスバー・ナビゲーションバー）の分だけ
     * パディングを適用する。バーを隠している間はパディング０になり全画面描画される。
     * カットアウト領域は描画に使うためパディングしない。
     */
    public static void applyVisibleSystemBarInsets(View view) {
        if (view == null) return;
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            int[] types = {
                WindowInsetsCompat.Type.statusBars(),
                WindowInsetsCompat.Type.navigationBars(),
                WindowInsetsCompat.Type.captionBar()
            };
            Insets bars = Insets.NONE;
            for (int type : types) {
                if (insets.isVisible(type)) {
                    bars = Insets.max(bars, insets.getInsets(type));
                }
            }
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
    }

    /**
     * ビュワー等のシステムバー（ナビゲーションバー・ステータスバー）の表示・非表示を制御
     *
     * @param activity 対象Activity
     * @param hideNav ナビゲーションバーを隠すか (mImmEnable)
     * @param hideStatus ステータスバーを隠すか (mNotice)
     */
    public static void updateSystemBars(Activity activity, boolean hideNav, boolean hideStatus) {
        if (activity == null) return;
        Window window = activity.getWindow();
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
        if (controller == null) {
            return;
        }

        controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

        int hideTypes = 0;
        int showTypes = 0;

        if (hideNav) {
            hideTypes |= WindowInsetsCompat.Type.navigationBars();
        } else {
            showTypes |= WindowInsetsCompat.Type.navigationBars();
        }

        if (hideStatus) {
            hideTypes |= WindowInsetsCompat.Type.statusBars();
        } else {
            showTypes |= WindowInsetsCompat.Type.statusBars();
        }

        if (hideTypes != 0) {
            controller.hide(hideTypes);
        }
        if (showTypes != 0) {
            controller.show(showTypes);
        }
    }

    /**
     * 全てのシステムバーを再表示する（設定画面遷移前など）
     */
    public static void showAllSystemBars(Activity activity) {
        if (activity == null) return;
        Window window = activity.getWindow();
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
        if (controller != null) {
            controller.show(WindowInsetsCompat.Type.systemBars());
        }
    }
}
