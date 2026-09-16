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

    private void initAttrs(Context context, AttributeSet attrs) {
        if (attrs != null) {
            for (int i = 0; i < attrs.getAttributeCount(); i++) {
                String name = attrs.getAttributeName(i);
                if ("dialogTitle".equals(name)) {
                    int resId = attrs.getAttributeResourceValue(i, 0);
                    mDialogTitle = resId != 0 ? context.getString(resId) : attrs.getAttributeValue(i);
                } else if ("dialogMessage".equals(name)) {
                    int resId = attrs.getAttributeResourceValue(i, 0);
                    mDialogMessage = resId != 0 ? context.getString(resId) : attrs.getAttributeValue(i);
                } else if ("positiveButtonText".equals(name)) {
                    int resId = attrs.getAttributeResourceValue(i, 0);
                    mPositiveButtonText = resId != 0 ? context.getString(resId) : attrs.getAttributeValue(i);
                } else if ("negativeButtonText".equals(name)) {
                    int resId = attrs.getAttributeResourceValue(i, 0);
                    mNegativeButtonText = resId != 0 ? context.getString(resId) : attrs.getAttributeValue(i);
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
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        CharSequence title = getDialogTitle();
        if (title != null) {
            builder.setTitle(title);
        }

        View contentView = onCreateDialogView();
        if (contentView != null) {
            onBindDialogView(contentView);
            builder.setView(contentView);
        }

        CharSequence posText = mPositiveButtonText != null ? mPositiveButtonText : context.getString(android.R.string.ok);
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
