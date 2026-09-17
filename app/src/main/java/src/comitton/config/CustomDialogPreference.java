package src.comitton.config;

import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;

public abstract class CustomDialogPreference extends Preference {
    protected CharSequence mDialogTitle;
    protected CharSequence mDialogMessage;
    protected CharSequence mPositiveButtonText;
    protected CharSequence mNegativeButtonText;
    protected int mDialogLayoutResId;
    protected AlertDialog mDialog;

    public CustomDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttrs(context, attrs);
    }

    public CustomDialogPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttrs(context, attrs);
    }

    /**
     * 文字列属性を取得する。リソース参照なら解決し、"@null" 指定("@0" として渡ってくる)は空文字にする。
     */
    private static CharSequence readTextAttr(Context context, AttributeSet attrs, int index) {
        int resId = attrs.getAttributeResourceValue(index, 0);
        if (resId != 0) {
            return context.getString(resId);
        }
        String value = attrs.getAttributeValue(index);
        if (value == null || value.startsWith("@")) {
            // "@null" / "@0" は「指定なし」
            return "";
        }
        return value;
    }

    private void initAttrs(Context context, AttributeSet attrs) {
        if (attrs != null) {
            for (int i = 0; i < attrs.getAttributeCount(); i++) {
                String name = attrs.getAttributeName(i);
                if ("dialogTitle".equals(name)) {
                    mDialogTitle = readTextAttr(context, attrs, i);
                } else if ("dialogMessage".equals(name)) {
                    mDialogMessage = readTextAttr(context, attrs, i);
                } else if ("positiveButtonText".equals(name)) {
                    mPositiveButtonText = readTextAttr(context, attrs, i);
                } else if ("negativeButtonText".equals(name)) {
                    mNegativeButtonText = readTextAttr(context, attrs, i);
                } else if ("dialogLayout".equals(name)) {
                    mDialogLayoutResId = attrs.getAttributeResourceValue(i, 0);
                }
            }
        }
    }

    public void setDialogLayoutResource(int dialogLayoutResId) {
        mDialogLayoutResId = dialogLayoutResId;
    }

    public int getDialogLayoutResource() {
        return mDialogLayoutResId;
    }

    public void setDialogTitle(CharSequence dialogTitle) {
        mDialogTitle = dialogTitle;
    }

    public CharSequence getDialogTitle() {
        return mDialogTitle != null ? mDialogTitle : getTitle();
    }

    public void setDialogMessage(CharSequence dialogMessage) {
        mDialogMessage = dialogMessage;
    }

    public CharSequence getDialogMessage() {
        return mDialogMessage;
    }

    public void setPositiveButtonText(CharSequence positiveButtonText) {
        mPositiveButtonText = positiveButtonText;
    }

    public CharSequence getPositiveButtonText() {
        return mPositiveButtonText;
    }

    public void setNegativeButtonText(CharSequence negativeButtonText) {
        mNegativeButtonText = negativeButtonText;
    }

    public CharSequence getNegativeButtonText() {
        return mNegativeButtonText;
    }

    @Override
    protected void onClick() {
        showDialog(null);
    }

    public void showDialog() {
        showDialog(null);
    }

    protected void showDialog(android.os.Bundle state) {
        Context context = getContext();
        // アプリ共通のダイアログテーマを使う(Lightテーマのまま生成すると白地に白文字になる)
        AlertDialog.Builder builder = new AlertDialog.Builder(context, jp.dip.muracoro.comittonx.R.style.MyDialog);

        CharSequence title = getDialogTitle();
        if (title != null) {
            builder.setTitle(title);
        }

        View contentView = onCreateDialogView();
        if (contentView != null) {
            onBindDialogView(contentView);
            // AppCompat の AlertDialog はカスタムビューに余白を付けないので、
            // タイトルやボタンと同じ dialogPreferredPadding ぶん左右に余白を付ける
            android.widget.FrameLayout wrapper = new android.widget.FrameLayout(context);
            int horizontal = resolveDialogPadding(context);
            int vertical = (int) (8 * context.getResources().getDisplayMetrics().density);
            wrapper.setPadding(horizontal, vertical, horizontal, vertical);
            wrapper.addView(contentView, new android.widget.FrameLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
            builder.setView(wrapper);
        }

        // OK ボタンは常に必要なので、未指定/空なら標準の OK を使う
        CharSequence posText = (mPositiveButtonText != null && mPositiveButtonText.length() > 0) ? mPositiveButtonText : context.getString(android.R.string.ok);
        if (posText != null && !posText.equals("@null") && !posText.equals("")) {
            builder.setPositiveButton(posText, (dialog, which) -> {
                onDialogClosed(true);
            });
        }

        CharSequence negText = mNegativeButtonText != null ? mNegativeButtonText : context.getString(android.R.string.cancel);
        if (negText != null && !negText.equals("@null") && !negText.equals("")) {
            builder.setNegativeButton(negText, (dialog, which) -> {
                onDialogClosed(false);
            });
        }

        mDialog = builder.create();
        mDialog.show();
    }

    /**
     * ダイアログ標準の左右余白(dialogPreferredPadding)をピクセルで返す。取得できなければ 24dp。
     */
    private static int resolveDialogPadding(Context context) {
        android.util.TypedValue value = new android.util.TypedValue();
        android.content.res.Resources.Theme theme = new androidx.appcompat.view.ContextThemeWrapper(context, jp.dip.muracoro.comittonx.R.style.MyDialog).getTheme();
        if (theme.resolveAttribute(androidx.appcompat.R.attr.dialogPreferredPadding, value, true)) {
            return (int) value.getDimension(context.getResources().getDisplayMetrics());
        }
        return (int) (24 * context.getResources().getDisplayMetrics().density);
    }

    public void dismissDialog() {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
            mDialog = null;
        }
    }

    public AlertDialog getDialog() {
        return mDialog;
    }

    protected View onCreateDialogView() {
        if (mDialogLayoutResId != 0) {
            return android.view.LayoutInflater.from(getContext()).inflate(mDialogLayoutResId, null);
        }
        return null;
    }

    protected void onBindDialogView(View view) {
    }

    protected void onDialogClosed(boolean positiveResult) {
    }
}
