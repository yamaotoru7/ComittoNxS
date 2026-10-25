package src.comitton.fileview;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.widget.Toast;

import jp.dip.muracoro.comittonx.R;
import src.comitton.common.DEF;
import src.comitton.common.Logcat;
import src.comitton.common.ThumbnailLoader;
import src.comitton.fileaccess.FileAccess;
import src.comitton.fileaccess.FileAccessException;
import src.comitton.fileview.data.FileData;
import src.comitton.fileview.filelist.ServerSelect;

/**
 * ファイルの削除、リネーム、関連設定・キャッシュの削除など
 * ファイルIOおよび関連状態の操作責務を担当するヘルパークラス
 */
public class FileOperationHelper {
	private static final int LOG_LEVEL = Logcat.LOG_LEVEL_WARN;

	// ファイル操作はバックグラウンドで実行するので、メッセージは UI スレッドで表示する
	private static void showToast(Activity activity, CharSequence text) {
		activity.runOnUiThread(() -> {
			if (!activity.isFinishing() && !activity.isDestroyed()) {
				Toast.makeText(activity, text, Toast.LENGTH_LONG).show();
			}
		});
	}

	/**
	 * ファイルを削除し、関連する設定・サムネイルキャッシュをクリアする
	 */
	public static boolean deleteFile(Activity activity, ServerSelect server, String baseUri, String path,
									 FileData fileData, int thumbW, int thumbH, SharedPreferences sp) {
		if (fileData == null) {
			return false;
		}
		String user = server != null ? server.getUser() : "";
		String pass = server != null ? server.getPass() : "";
		String uri = DEF.relativePath(activity, baseUri, path, fileData.getName());
		Logcat.d(LOG_LEVEL, "ファイルを削除します: " + uri);
		try {
			boolean isDeleted = FileAccess.delete(activity, uri, user, pass);
			if (isDeleted) {
				Logcat.d(LOG_LEVEL, "ファイルを削除できました。");
				String file = DEF.createUrl(uri, user, pass);
				Editor ed = sp.edit();
				ed.remove(file + "#date");
				ed.remove(file + "#maxpage");
				ed.remove(file);
				ed.remove(file + "META-INF/container.xml" + "#maxpage");
				ed.remove(file + "META-INF/container.xml");
				ed.apply();
				if (fileData.getType() == FileData.FILETYPE_DIR) {
					ThumbnailLoader.deleteThumbnailCache(activity, ThumbnailLoader.makeDirCode(uri, fileData.getDate(), thumbW, thumbH));
				}
				else {
					ThumbnailLoader.deleteThumbnailCache(activity, ThumbnailLoader.makeFileCode(fileData.getName(), thumbW, thumbH));
				}
				return true;
			} else {
				Logcat.e(LOG_LEVEL, activity.getResources().getString(R.string.delErrorMsg));
				showToast(activity, activity.getText(R.string.delErrorMsg) + "\n" + uri);
				return false;
			}
		} catch (FileAccessException e) {
			showToast(activity, e.getLocalizedMessage());
			return false;
		}
	}

	/**
	 * ファイル名を変更し、関連する設定・サムネイルキャッシュの名前も移行する
	 */
	public static boolean renameFile(Activity activity, ServerSelect server, String baseUri, String path,
									 FileData fileData, String newName, int thumbW, int thumbH, SharedPreferences sp) {
		if (fileData == null || newName == null || newName.isEmpty()) {
			return false;
		}
		String user = server != null ? server.getUser() : "";
		String pass = server != null ? server.getPass() : "";
		String fromfile = fileData.getName();
		int filetype = fileData.getType();

		String finalName = newName;
		if (filetype != FileData.FILETYPE_DIR) {
			int index = fromfile.lastIndexOf('.');
			if (index >= 0) {
				finalName = newName + fromfile.substring(index);
			}
		}

		try {
			String uri = DEF.relativePath(activity, baseUri, path);
			Logcat.d(LOG_LEVEL, "ファイル名を変更します: uri=" + uri);
			boolean ret = FileAccess.renameTo(activity, uri, fromfile, finalName, user, pass);
			if (filetype == FileData.FILETYPE_DIR && !finalName.endsWith("/")) {
				finalName = finalName + "/";
			}
			if (ret) {
				fileData.setName(activity, finalName);

				String fromUri = DEF.relativePath(activity, baseUri, path, fromfile);
				String toUri = DEF.relativePath(activity, baseUri, path, finalName);
				String from = DEF.createUrl(fromUri, user, pass);
				String to = DEF.createUrl(toUri, user, pass);

				Editor ed = sp.edit();
				int nowdate = sp.getInt(from + "#date", 0);
				if (nowdate != 0) {
					ed.putInt(to + "#date", nowdate);
				}
				int maxpage = sp.getInt(from + "#maxpage", DEF.PAGENUMBER_NONE);
				if (maxpage != DEF.PAGENUMBER_NONE) {
					ed.putInt(to + "#maxpage", maxpage);
				}
				int state = sp.getInt(from, DEF.PAGENUMBER_UNREAD);
				if (state != DEF.PAGENUMBER_UNREAD) {
					ed.putInt(to, state);
				}
				int maxpage_epub = sp.getInt(from + "META-INF/container.xml" + "#maxpage", DEF.PAGENUMBER_NONE);
				if (maxpage_epub != DEF.PAGENUMBER_NONE) {
					ed.putInt(to + "META-INF/container.xml" + "#maxpage", maxpage_epub);
				}
				int state_epub = sp.getInt(from + "META-INF/container.xml", DEF.PAGENUMBER_UNREAD);
				if (state_epub != DEF.PAGENUMBER_UNREAD) {
					ed.putInt(to + "META-INF/container.xml", state_epub);
				}
				ed.remove(from + "#date");
				ed.remove(from + "#maxpage");
				ed.remove(from);
				ed.remove(from + "META-INF/container.xml" + "#maxpage");
				ed.remove(from + "META-INF/container.xml");
				ed.apply();
				if (filetype == FileData.FILETYPE_DIR) {
					// 名前を変えてもフォルダ自身の更新日時は変わらない
					ThumbnailLoader.renameThumbnailCache(activity, ThumbnailLoader.makeDirCode(fromUri, fileData.getDate(), thumbW, thumbH), ThumbnailLoader.makeDirCode(toUri, fileData.getDate(), thumbW, thumbH));
				}
				else {
					ThumbnailLoader.renameThumbnailCache(activity, ThumbnailLoader.makeFileCode(fromfile, thumbW, thumbH), ThumbnailLoader.makeFileCode(finalName, thumbW, thumbH));
				}
				return true;
			} else {
				Logcat.e(LOG_LEVEL, activity.getText(R.string.renameErrorMsg) + ": " + DEF.relativePath(activity, baseUri, path, fromfile));
				showToast(activity, activity.getText(R.string.renameErrorMsg) + "\n" + DEF.relativePath(activity, baseUri, path, fromfile));
				return false;
			}
		} catch (FileAccessException e) {
			showToast(activity, e.getClass().getSimpleName() + ": " + e.getLocalizedMessage());
			return false;
		}
	}
}
