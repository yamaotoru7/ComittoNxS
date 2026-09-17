package src.comitton.dialog;

import src.comitton.common.SystemUiHelper;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import jp.dip.muracoro.comittonx.R;

public class LoadingProgressDialog extends AlertDialog {
	public static final int STYLE_SPINNER = 0;
	public static final int STYLE_HORIZONTAL = 1;

	private TextView mMessageView;
	private CharSequence mMessage = "";

	public LoadingProgressDialog(Context context) {
		super(context);
		init();
	}

	public LoadingProgressDialog(Context context, int themeResId) {
		super(context, themeResId);
		init();
	}

	private void init() {
		supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
		setCancelable(false);
		setCanceledOnTouchOutside(false);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Context context = getContext();
		float density = context.getResources().getDisplayMetrics().density;
		int pad = (int) (18 * density);

		LinearLayout layout = new LinearLayout(context);
		layout.setOrientation(LinearLayout.HORIZONTAL);
		layout.setGravity(Gravity.CENTER_VERTICAL);
		layout.setPadding(pad, pad, pad, pad);

		ProgressBar progressBar = new ProgressBar(context);
		LinearLayout.LayoutParams pbParams = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT
		);
		pbParams.rightMargin = pad;
		layout.addView(progressBar, pbParams);

		mMessageView = new TextView(context);
		mMessageView.setText(mMessage);
		mMessageView.setTextSize(15);
		LinearLayout.LayoutParams tvParams = new LinearLayout.LayoutParams(
				0,
				ViewGroup.LayoutParams.WRAP_CONTENT,
				1.0f
		);
		layout.addView(mMessageView, tvParams);

		setView(layout);
		super.onCreate(savedInstanceState);
	}

	@Override
	public void setMessage(CharSequence message) {
		mMessage = message != null ? message : "";
		if (mMessageView != null) {
			mMessageView.setText(mMessage);
		}
	}

	public void setProgressStyle(int style) {
		// no-op for compatibility
	}

	public void setIndeterminate(boolean indeterminate) {
		// no-op for compatibility
	}

	@Override
	public void show() {
		Window window = getWindow();
		if (window != null) {
			window.setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
		}
		super.show();
		if (window != null) {
			Context context = getContext();
			if (context instanceof Activity) {
				Activity act = (Activity) context;
				SystemUiHelper.copySystemBars(act, window);
			}
			window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
		}
	}
}
