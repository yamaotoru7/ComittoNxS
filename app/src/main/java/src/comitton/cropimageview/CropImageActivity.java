package src.comitton.cropimageview;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import jp.dip.muracoro.comittonx.R;
import src.comitton.common.DEF;
import src.comitton.common.Logcat;
import src.comitton.dialog.LoadingProgressDialog;
import src.comitton.imageview.ImageManager;


public class CropImageActivity extends AppCompatActivity implements Runnable, TextWatcher, CropImageView.CropCallback{
    private String mFile;	        	// ZIPファイル名
    private String mPath;				// ベースuriからのパス名 (SAF未対応 (ComittoNxS v3.0.0 未満))
    private String mURI;		            // ベースディレクトリのuri
    private String mUser;			    // SMB認証用
    private String mPass;			    // SMB認証用
    private String mCropPath;           // 画像を切り取るファイル名
    private int mPage;                  // 画像を切り取るページ


    private CropImageView mCropImageView;
    private LoadingProgressDialog mProgress;
    private EditText mEditAspectRatio;
    float mAspectRatio;

    private Thread mThread;
    private volatile Bitmap mBitmap;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cropimage);
        src.comitton.common.SystemUiHelper.setupEdgeToEdge(this);
        src.comitton.common.SystemUiHelper.applySystemWindowInsets(findViewById(android.R.id.content), true, true, true, true);

        Intent intent = getIntent();
        mCropPath = intent.getStringExtra("uri");   // 画像を切り取るファイル名
        if(mCropPath == null) {
            mURI = intent.getStringExtra("Uri");    // ベースディレクトリのURI
            mPath = intent.getStringExtra("Path");  // ベースURIからの相対パス名
            mFile = intent.getStringExtra("File");  // ZIPファイル名
            mUser = intent.getStringExtra("User");  // SMB認証用
            mPass = intent.getStringExtra("Pass");  // SMB認証用
            mPage = intent.getIntExtra("Page", 0);  // ページ番号
        }
//        mUri = Uri.parse("file://" + uri);
        mAspectRatio = intent.getFloatExtra("aspectRatio", 3.0f / 4.0f);

        mCropImageView = (CropImageView) findViewById(R.id.cropImageView);
        mCropImageView.setAspectRatio(mAspectRatio);
        mCropImageView.setCallback(this);

        mProgress = new LoadingProgressDialog(this, R.style.MyDialog);
        mProgress.setIndeterminate(true);
        mProgress.setMessage("Loading...");
        mProgress.setProgressStyle(LoadingProgressDialog.STYLE_SPINNER);
        mProgress.show();

        mThread = new Thread(this);
        mThread.start();


        // 以下はイベントハンドラ
        Button cropButton = (Button) findViewById(R.id.btn_ok);
        cropButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBitmap == null) {
                    // まだ読み込み中
                    return;
                }
                // フレーム内をクロップ
                mCropImageView.Crop(mCropPath);
                Intent intent = new Intent();
                intent.setData(Uri.parse("file://" + mCropPath));
//                intent.putExtra("uri", mUri.toString());
                setResult(RESULT_OK, intent);
                finish();
            }
        });
        mEditAspectRatio = (EditText) findViewById(R.id.edit_aspect);
        mEditAspectRatio.setText(String.valueOf(mAspectRatio));
        mEditAspectRatio.addTextChangedListener(this);
        Button cancelButton = (Button) findViewById(R.id.btn_cancel);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
        // アス比減少・増加ボタン
        Button btnMinus = (Button) findViewById(R.id.btn_minus);
        btnMinus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAspectRatio -= 0.01f;
                mEditAspectRatio.setText(String.format("%.3f", mAspectRatio));
            }
        });
        Button btnPlus = (Button) findViewById(R.id.btn_plus);
        btnPlus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAspectRatio += 0.01f;
                mEditAspectRatio.setText(String.format("%.3f", mAspectRatio));
            }
        });

        // 左右移動ボタン
        Button btnLeft = (Button) findViewById(R.id.btn_left);
        btnLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCropImageView.move(-0.01f);
            }
        });
        Button btnRight = (Button) findViewById(R.id.btn_right);
        btnRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCropImageView.move(0.01f);
            }
        });
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void run() {
        int logLevel = Logcat.LOG_LEVEL_WARN;
        int result = DEF.HMSG_ERROR;
        ImageManager imageMgr = null;
        try {
            if (mCropPath == null) {
                imageMgr = new ImageManager(this, mPath, mFile, mUser, mPass, ImageManager.FILESORT_NAME_UP, handler, true, ImageManager.OPENMODE_THUMBSORT, 1);
                imageMgr.LoadImageList(0, 0, 0);
                mCropPath = imageMgr.decompFile(mPage, null);
            }
            if (mCropPath != null) {
                Logcat.d(logLevel, "イメージファイルを開きます. mURI=" + mURI + ", mCropPath=" + mCropPath);
                Bitmap bitmap;
                if (imageMgr != null) {
                    bitmap = imageMgr.GetBitmapFromPath(this, mCropPath, null);
                }
                else {
                    // ファイルパス指定で起動された場合は ImageManager を使わずに読み込む
                    bitmap = BitmapFactory.decodeFile(mCropPath);
                }
                if (bitmap == null) {
                    Logcat.e(logLevel, "ビットマップ取得に失敗しました.");
                }
                else {
                    Logcat.d(logLevel, "ビットマップ取得に成功しました.");
                    if (bitmap.getHeight() > 1000) {
                        Logcat.d(logLevel, "ビットマップを縮小します.");
                        float dsH = (float) 1000 / (float) bitmap.getHeight();
                        Bitmap scaled = Bitmap.createScaledBitmap(bitmap, (int) (bitmap.getWidth() * dsH), (int) (bitmap.getHeight() * dsH), true);
                        if (scaled != bitmap) {
                            bitmap.recycle();
                        }
                        bitmap = scaled;
                    }
                    mBitmap = bitmap;
                    result = DEF.HMSG_LOAD_END;
                }
            }
        } catch (Exception e) {
            // ワーカースレッドで例外を投げるとプロセスが落ちるので、エラーとして画面を閉じる
            Logcat.e(logLevel, "エラーが発生しました.", e);
        } finally {
            if (imageMgr != null) {
                imageMgr.closeFiles();
            }
        }
        mProgress.dismiss();
        handler.sendEmptyMessage(result);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        if (mProgress != null && mProgress.isShowing()) {
            mProgress.dismiss();
        }
    }

    public void cropCallback(float aspectRatio){
        mEditAspectRatio.setText(String.format("%.3f", aspectRatio));
    }

    private final Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message message){
            if (isFinishing() || isDestroyed()) {
                return;
            }
            switch (message.what){
                case DEF.HMSG_LOADING: // ロード状況表示
                    mProgress.setMessage( String.valueOf(message.arg1 >> 24) + "%\n" + ((float) message.arg2 / 10) + "KB/S");
                    break;
                case DEF.HMSG_LOAD_END:
                    mCropImageView.setImageBitmap(mBitmap);
                    break;
                case DEF.HMSG_ERROR:
                    setResult(RESULT_CANCELED);
                    finish();
                    break;
                case DEF.HMSG_PROGRESS:
                    if (message.obj != null) {
                        mProgress.setMessage(message.obj.toString());
                    }
                    break;
                case DEF.HMSG_WORKSTREAM:
                    // ファイルアクセスの表示
                    break;
            }
        }
    };

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        try {
            mAspectRatio = Float.parseFloat(s.toString());
        } catch (NumberFormatException e) {
            // 入力途中 (空文字や "." のみ) は無視する
            return;
        }
        if(mAspectRatio < 0.1f)
            mAspectRatio = 0.1f;
        mCropImageView.setAspectRatio(mAspectRatio);
    }
}