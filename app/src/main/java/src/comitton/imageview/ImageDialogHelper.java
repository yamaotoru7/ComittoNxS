package src.comitton.imageview;

import jp.dip.muracoro.comittonx.R;
import src.comitton.dialog.CloseDialog;
import src.comitton.dialog.CloseDialog.CloseListenerInterface;

/**
 * ImageActivity からダイアログ表示・ライフサイクル制御を分離するヘルパークラス
 */
public class ImageDialogHelper {
	private final ImageActivity mActivity;
	private CloseDialog mCloseDialog;

	public ImageDialogHelper(ImageActivity activity) {
		this.mActivity = activity;
	}

	public void showCloseDialog(int layout) {
		if (mCloseDialog != null) {
			return;
		}
		mCloseDialog = new CloseDialog(mActivity, R.style.MyDialog);
		mCloseDialog.setTitleText(layout);
		mCloseDialog.setCloseListear(new CloseListenerInterface() {
			@Override
			public void onCloseSelect(int select, boolean resume, boolean mark) {
				if (select != CloseDialog.CLICK_CANCEL) {
					mActivity.finishActivity(select, resume, mark);
				}
			}

			@Override
			public void onClose() {
				mCloseDialog = null;
			}
		});
		mCloseDialog.show();
	}

	public void dismissDialogs() {
		if (mCloseDialog != null) {
			mCloseDialog.dismiss();
			mCloseDialog = null;
		}
	}
}
