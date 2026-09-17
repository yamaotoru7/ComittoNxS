# ComittoNxS 課題一覧

2026-09-17 時点のコードベース全体レビューで抽出した課題です。
lint (`:app:lintDebug`) とユニットテスト (`:app:testDebugUnitTest`) の実行結果、および各ファイルの直接確認に基づいています。

## 使い方

- **状態** 列は `未対応` / `対応中` / `対応済` / `対応不要` のいずれかにしてください。
- **コメント** 列には、対応不要とする理由や対応時のメモを自由に書いてください。
- 行番号はレビュー時点のものです。編集で前後にずれます。

## 計測値 (2026-09-17)

| 項目 | 結果 |
|---|---|
| ユニットテスト | 12 件 すべて成功 (DEFTest 9、LocalFileAccessTest 3) |
| lint Error | 23 件 |
| lint Warning | 545 件 (UnusedResources 138、RtlHardcoded 115 が大半) |
| Java ソース | 約 6.7 万行。TextManager 6363 行、ImageManager 5102 行、ImageActivity 4770 行 |

---

## A. 優先度高: クラッシュ・データ損失・セキュリティ

| ID | 重要度 | 場所 | 内容 | 状態 | コメント |
|---|---|---|---|---|---|
| A-01 | 高 | `fileaccess/FileAccess.java:692` | `date()` の中身が `delete(...)` になっている。現在は未使用だが、呼んだ瞬間にファイルが消える。削除するか正しく実装する。 | 対応済 | メソッドを削除 |
| A-02 | 高 | `fileview/filelist/ServerSelect.java:386`, `common/DEF.java:2437-2460`, `DEF.java:2347-2361`, `config/ExportSettingPreference.java:177-206` | SMB パスワードが SharedPreferences に平文保存される。`createUrl` が `smb://user:pass@host/...` を作りしおり・既読位置のキーに使うため、設定エクスポートで本ごとにパスワードが平文ファイルへ書き出される。EncryptedSharedPreferences 等へ移行し、キーから認証情報を除き、`smb-pass*` をエクスポート対象外にする。 | 未対応 | |
| A-03 | 高 | `ServerSelect.java:74,353,380`, `SmbFileAccess.java:255,277,344,631,720`, `SambaProxyFileCallback.java:29`, `WorkStream.java:33,43`, `EditServerDialog.java:93` | パスワードをログ文字列に含めている。現在はログレベル NONE で抑止されているだけ。ログから `pass` を除去する。 | 未対応 | |
| A-04 | 高 | `fileaccess/SmbFileAccess.java:121-136` | `jcifs.smb.client.minVersion = SMB1`。署名設定もコメントアウト。既定を SMB202 にしレガシー NAS 向けはオプトインにする。`SingletonContext.init` は初回しか効かない点にも注意。 | 未対応 | |
| A-05 | 高 | `imageview/ImageManager.java:2018, 2046` | `page < 0 && mFileList.length <= page` は成立し得ない。範囲外ページで ArrayIndexOutOfBounds。`\|\|` に修正し `mFileList == null` も扱う。 | 対応済 | 条件を `||` に修正し null も考慮 |
| A-06 | 高 | `jni/comitton/ImageJXL.cpp:155-158` | `if (buffer == nullptr) free(buffer)` と判定が逆で、デコード結果バッファ (width×height×3) が毎回リークする。 | 対応済 | null 判定を反転 |
| A-07 | 高 | `jni/comitton/ImageAVIF.cpp:31-68, 137-139` | `memset(&rgb)` より前の `goto cleanup` で未初期化の `rgb` を `avifRGBImageFreePixels` に渡す。`decoder == nullptr` のパスで `avifDecoderDestroy(nullptr)` を呼ぶ。壊れた AVIF でクラッシュ。宣言時に初期化し null ガードを入れる。 | 対応済 | rgb を宣言時にゼロ初期化、decoder の null ガード追加 |
| A-08 | 高 | `jni/comitton/ImageScale.cpp:618-627`, `Image.h:66` | `gSclLinesPtr` は MAX_LINES (6400) 固定だが `RefreshSclLinesPtr` は元画像の高さでループするため、縦長画像で余白カット・回転・ぼかしを使うとヒープ破壊。境界チェックか動的確保にする。 | 未対応 | |
| A-09 | 高 | `textview/TextManager.java:1194-1207`, `:1280`, `:1335`, `:1722` | `loadExpandData` が null を返した後も `mInputBuff.length` を評価し NPE。`TextLoad.run()` に try/catch がなくプロセスごと落ちる。 | 未対応 | |
| A-10 | 高 | `cropimageview/CropImageActivity.java:152-179, 222-227` | URI 起動時に `imageMgr` が null のまま呼び、bitmap null も未チェック。例外を RuntimeException で再スロー。アスペクト比入力欄の空文字で `Float.parseFloat` が例外。 | 未対応 | |
| A-11 | 高 | `imageview/ImageManager.java:2236-2245` | `read()` が例外時に `Arrays.fill(buf, off, len - off, 0)` (第 3 引数は toIndex) でゼロ埋めし `len - off` を返す。壊れたデータが正常として描画される。`-1` を返すか IOException を投げる。 | 対応済 | IOException を送出するように変更 |
| A-12 | 高 | `fileview/FileSelectActivity.java:2879-2886` | しおり一括削除の `substring(0, urilen + 1)` が 1 文字長く一致しない。`key.length() == urilen` で例外。`regionMatches` に置き換える。 | 対応済 | regionMatches に置換 |
| A-13 | 高 | `fileview/FileSelectActivity.java:396-405`, `AndroidManifest.xml` | ACTION_VIEW / SEND の `content://` を `getPath()` でローカルパス扱い。API 24 以降 `file://` は来ないため共有受け口が機能していない。scheme で分岐し SafFileAccess を使う。 | 未対応 | |
| A-14 | 高 | `jni/comitton/callUnrar.cpp:162-165, 190-193` | 格納エントリで `OrigLen > CompLen` のヘッダーはヒープ越え読み。`rarDecomp` は常に 0 を返し unrar のエラーが Java に届かない。暗号化 RAR はごみ画素で「成功」する。 | 未対応 | |
| A-15 | 高 | `jni/comitton/callUnrar.cpp:12-26, 241-252` | unrar の状態 (`FromBuff/ToBuff/DataIO/Unp`) がプロセスグローバルでロックなし。`rarClose` の `free` と別スレッドの `rarWrite` が競合すると use-after-free。 | 未対応 | |

## B. 優先度中: リーク・パフォーマンス・設計

| ID | 重要度 | 場所 | 内容 | 状態 | コメント |
|---|---|---|---|---|---|
| B-01 | 中 | `imageview/ImageActivity.java:647-655, 699-717, 3161-3180` | 読込中に戻る・キャンセルすると `closeFiles()` が呼ばれず、ネイティブキャッシュ・SMB ストリーム・PdfRenderer・RarInputStream が漏れる。`onDestroy` でも閉じていない。 | 未対応 | |
| B-02 | 中 | `common/GuideView.java:296, 1357-1400`, `imageview/MyImageView.java:179-235` | `draw` 内の `setTimeString` が毎回 `invalidate` → 更新スレッド → `draw` の無限再描画ループ。MAX_PRIORITY スレッドでバッテリー情報の Binder 呼び出しと日付整形を回し続ける。1 分タイマーで更新し、変化時のみ invalidate する。 | 未対応 | |
| B-03 | 中 | `SmbFileAccess.java` (299,373,411,533,675,741,786,830), `FileAccess.java` (308,335,384,431,468,573,662,743), `SambaProxyFileCallback.java:57,84,121`, `ExpandFileStatusLoader.java:114-170`, `ImageManager.java:2262-2330` | `Executors.newSingleThreadExecutor()` を呼び出しごとに生成し `shutdown` しない。非デーモンスレッドが溜まる。共有 Executor に統一する。 | 未対応 | |
| B-04 | 中 | `FileAccess.java:706-731` ほか | UI スレッドで `future.get()` をタイムアウトなしで待つ。NetworkOnMainThread は避けられるが ANR になる。非同期化する。 | 未対応 | |
| B-05 | 中 | `ImageActivity.java:616`, `TextActivity.java:455`, `FileSelectActivity.java:2902` | UI スレッドでブロッキング IO (`FileAccess.date`、しおり整理ダイアログの SMB `exists` ループ)。ワーカーへ移す。 | 未対応 | |
| B-06 | 中 | `FileAccess.java:308-313, 466-496`, `WorkStream.java:74-81` | `seek()` が UI スレッドでは fire-and-forget、`close()` は常に非同期で他スレッドの `read()` と競合する。 | 未対応 | |
| B-07 | 中 | `ImageManager.java:187-198`, `ImageActivity.java`, `TextManager.java:98`, `MyTextView.java:131-177`, `MyImageView.java:121`, `ThumbnailView.java:59`, `ThumbnailLoader.java:43`, `DownloadDialog.java:47` | スレッド間で共有する break/running フラグに `volatile` がひとつもない。キャンセルが効かない可能性。`volatile` か `AtomicBoolean` にする。 | 未対応 | |
| B-08 | 中 | `ImageActivity.java:699-717`, `TextActivity.java:611-624`, `ExpandActivity.java:340-356` | `onDestroy` で `removeCallbacksAndMessages` を呼ばず、ワーカースレッドも止めない。破棄後の Activity にメッセージが届く。`handleMessage` 冒頭で `isFinishing()/isDestroyed()` を確認する。 | 未対応 | |
| B-09 | 中 | `ImageActivity.java:779-810`, `ImageManager.java:264-343` | `ZipLoad.run` に try/catch がなく、非チェック例外で `HMSG_READ_END` が送られず進捗ダイアログが固まる。 | 未対応 | |
| B-10 | 中 | `textview/MyTextView.java:716, 720, 1590, 2375` | `recycle()` がひとつもない。2 画面モードでは描画ごとにフルサイズ半分ビットマップを生成。バックバッファも再生成時に旧ビットマップを解放しない。 | 未対応 | |
| B-11 | 中 | `imageview/MyImageView.java:1181, 1203-1211` | `onSizeChanged` のリトライループに `break` がなく毎回 3 回 `createBitmap`。旧ビットマップ未 recycle。 | 未対応 | |
| B-12 | 中 | `ImageManager.java:3846-3875, 3931, 4941-5020` | PDF サムネイル・共有パスで Bitmap を生成して recycle しない。`drawBitmap` で自身に描画。`FileOutputStream` が例外時に閉じない。 | 未対応 | |
| B-13 | 中 | `common/Logcat.java`, `FileAccess.java:373, 395`, `ImageManager.java:2197-2258, 4317-4899`, `ImageManager.java:3823`, `ThumbnailView.java:536` | Logcat 呼び出し 1274 か所は全て無音だが、引数の文字列連結や `MessageFormat.format` は常に実行される。`FileAccess.read` ではログ引数の `length()` で SMB 往復が 4 回増える。`bm.getWidth()` をログ引数に含めて null 時に NPE。R8 の `-assumenosideeffects` を `Logcat` に適用するか、hot path にレベルガードを入れる。 | 未対応 | |
| B-14 | 中 | `res/xml/*.xml` vs `res/xml-ja/*.xml`, `config/SetImageActivity.java:397, 429`, `SetCommonActivity.java:164`, `SetFileListActivity.java:368-485`, `SetImageText.java:48-135` | 設定のデフォルト値が xml / xml-ja / Java の 3 系統で食い違う (IniScale 5/2、AlgoMode 4/2、HiddenFile false/true など)。英語ロケールでは設定画面の表示と動作が一致しない。単一の情報源にする。 | 未対応 | |
| B-15 | 中 | `res/xml-ja/*.xml` (12 ファイル), `res/xml/config.xml` | 設定画面をロケールごとに丸ごと複製。title 233 個・summary 207 個がリテラルで `@string` 参照ゼロ。キー名も drift (`ImTxDetailSet` vs `ImageTextDetailSet`)。strings.xml へ寄せて xml-ja を廃止する。 | 未対応 | |
| B-16 | 中 | `config/CustomDialogPreference.java:134-183`, `AndroidManifest.xml:81-152` | カスタム設定ダイアログに状態保存がなく Set*Activity に configChanges もないため、回転で編集内容が消え window leak になる。`PreferenceDialogFragmentCompat` へ移行する。 | 未対応 | |
| B-17 | 中 | `fileaccess/SafFileAccess.java:91, 116, 187, 193, 209, 336, 527-531` | `ContentProviderClient` 未解放、ループ内 cursor の閉じ漏れ、`FileAccessException` を生成して `throw` し忘れ、`openRandomAccessFile` が常に `"r"`、`createFile` が失敗でも true。 | 未対応 | |
| B-18 | 中 | `fileaccess/LocalFileAccess.java:433, 460`, `dialog/EditServerDialog.java:243, 253` | Android 10 の SAF 代替で `URLEncoder.encode` がスペースを `+` にし、スペース入りパスの操作が失敗。`EXTRA_INITIAL_URI` を String で渡しており無視される。 | 未対応 | |
| B-19 | 中 | `LocalFileAccess.java:229, 339, 350`, `SmbFileAccess.java:633` | リネーム・mkdir・createFile が `/` 先頭や `..` を弾かずパストラバーサルを許す。 | 未対応 | |
| B-20 | 中 | `fileaccess/WorkStream.java:94`, `SafRandomAccessFile.java:75` | `mPos += off` とバッファオフセットをファイル位置に加算。`off != 0` で読む呼び出し元 (`ImageManager.java:2502, 2552, 2715`) で位置がずれる。 | 未対応 | |
| B-21 | 中 | `SambaProxyFileCallback.java:32`, `StorageManagerCompat.java:66` | 読み取り専用 PFD なのに `"rw"` で開く。API 26 未満ではファイル全体を byte[] に読み込み OOM。 | 未対応 | |
| B-22 | 中 | `SmbFileAccess.java:171-176, 200, 342, 389, 490, 617` | `smbFile()` が null を返すのに呼び出し側が null チェックせず NPE。 | 未対応 | |
| B-23 | 中 | `expandview/ExpandFileStatusLoader.java:88-103, 270-297` | 別スレッドから `mTextMgr` を null にして意図的に例外を起こしキャンセルしている。`setBreakTrigger` + interrupt に置き換える。 | 未対応 | |
| B-24 | 中 | `textview/TextActivity.java:427-460, 672-678, 2509-2556` | intent extras を null チェック前に参照。`mPath == null` で半初期化のまま `TextLoad` が走る。`reloadText` が読込中でも二重起動する。 | 未対応 | |
| B-25 | 中 | `dialog/PageSelectDialog.java:35`, `dialog/PageThumbnail.java:26`, `common/ThumbnailLoader.java:60-84, 317-350`, `fileview/filelist/FileSelectList.java:54`, `RecordList.java:61` | static な可変状態 (`mIsOpened`、ローダーの設定値、Activity 参照)。並行ローダー間で上書きされ、Activity が静的に保持される (lint StaticFieldLeak)。 | 未対応 | |
| B-26 | 中 | `jni/comitton/callText.cpp:56-66` | `SetTextImage` が `-5`/`-6` で戻るときに `AndroidBitmap_unlockPixels` していない。 | 未対応 | |
| B-27 | 中 | `jni/comitton/callUnrar.cpp:92, 191`, `ImageManager.java:1028` | 辞書サイズ固定 4MB・非 solid 前提・`version=50` 固定のため、大辞書 RAR5 や RAR7 のエントリが無警告で壊れる。 | 未対応 | |
| B-28 | 中 | `jni/comitton/ImageCommon.cpp:151-322`, `callImage.cpp:957-1002` | `DrawScaleBitmap` が矩形をビットマップに対して検証せず、`d_cx/d_cy == 0` でゼロ除算。フォーマットチェックなし。 | 未対応 | |
| B-29 | 中 | `jni/comitton/ImageThumbnail.cpp:178, 202, 247-248, 391-393, 563, 586` | `bmp_width` が 0 または 4096 超でゼロ除算。`ThumbnailDraw` が保存幅で行コピーし小さい描画先を越える。一部関数に index 境界チェックなし。mutex 外でのアクセス。 | 未対応 | |
| B-30 | 中 | `ImageManager.java:2253, 4987` | `mDataSize == 0` でゼロ除算。キャッシュスレッドで非チェック例外となりプロセスが落ちる。 | 未対応 | |
| B-31 | 中 | `textview/TextManager.java:6030-6049, 6185-6231` | `loadBitmap` で例外時に `WorkStream` を閉じない。`searchText` が `release()` 後に NPE し、`substring().equals` の O(n·m) 割り当て。 | 未対応 | |

## C. 優先度低: ビルド・リポジトリ・整理

| ID | 重要度 | 場所 | 内容 | 状態 | コメント |
|---|---|---|---|---|---|
| C-01 | 低 | `app/build.gradle:52` | release が常に `signingConfigs.debug` で署名される。`release.gradle` があっても切り替わらない。存在時に `signingConfigs.release` を使う。 | 対応不要 | ローカル用なので |
| C-02 | 低 | `app/build.gradle:24`, `gradle.properties` | `BUILD_DATE = System.currentTimeMillis()` で毎回 BuildConfig が変わり、`org.gradle.caching=true` が無意味になる。git のコミット日時か固定値にする。 | 未対応 | コミット日時にする |
| C-03 | 低 | `AndroidManifest.xml:5, 13, 15, 29, 158-159, 167-172` | `MANAGE_MEDIA` 未使用、`INSTALL_SHORTCUT` は旧 API 専用、`READ/WRITE_EXTERNAL_STORAGE` に maxSdkVersion なし、`installLocation="preferExternal"` はネイティブライブラリ持ちアプリに有害、`requestLegacyExternalStorage` は無効、`android.max_aspect` は無視される、FileProvider の `readPermission="true"` は権限名として無意味、`<intent-filter android:scheme>` は不正属性。 | 未対応 | |
| C-04 | 低 | `AndroidManifest.xml:169`, `FileSelectActivity.java:526-536, 731-735` | `MANAGE_EXTERNAL_STORAGE` は Play ポリシー制約あり。拒否時の結果ハンドラが空で無言で失敗する。SAF 対応済みなら任意化を検討。 | 未対応 | |
| C-05 | 低 | `res/values/style.xml:6, 12` | `windowOptOutEdgeToEdgeEnforcement=true` は targetSdk 36 で消える一時属性。README の Edge-to-Edge 対応と矛盾。 | 未対応 | |
| C-06 | 低 | `common/DEF.java:48-50`, `dialog/Information.java:437-439`, `download_count.bat` | 更新チェックとダウンロード数が上流 ComittoNxX を見ている (URL に typo `ComittonxX` もあり)。バージョン体系が違うため誤検出する。 | 未対応 | |
| C-07 | 低 | `jni/dav1d/CMakeLists.txt:7`, `jni/libavif/CMakeLists.txt:4`, `jni/libjxl/CMakeLists.txt:3` | FetchContent の `GIT_TAG` が可変タグ。unrar だけ SHA256 固定。コミットハッシュで固定する。 | 未対応 | |
| C-08 | 低 | `jni/dav1d/CMakeLists.txt:26, 43-69`, `jni/unrar/CMakeLists.txt:52-72`, `jni/unrar/patch-unrar.sh:6` | meson 失敗が fatal でなくリンク時に不可解なエラーになる。Unix PATH の `":;"` typo。sed の `+?` は GNU sed 非対応。 | 未対応 | |
| C-09 | 低 | `jni/libjxl/CMakeLists.txt:23, 27, 41-49`, `jni/CMakeLists.txt` | `--allow-multiple-definition` で重複シンボルを隠している。`brotlienc` は不要。`JPEGXL_ENABLE_BENCHMARK` 二重設定。最適化・visibility・LTO の明示なし。 | 未対応 | |
| C-10 | 低 | lint UnusedResources | 未使用リソース drawable 53、layout 9、string 56、color 11、array 8。`ic_mic_*`、`comittonxx_arm32/arm64/x64/x86`、`navi_*`、`seek_thumb_*` 等。 | 未対応 | |
| C-11 | 低 | `res/layout/listview.xml`, `layout-land/listview.xml`, `recordlist.xml`, `recorditem.xml` | 削除済みクラス (`ToolbarView`、`TileView` 等) を参照する未使用レイアウト。lint Error MissingClass 13 件の原因。削除する。 | 未対応 | |
| C-12 | 低 | `assets/help.css`, `res/values/strings.xml:92`, `ImageActivity.java:3284`, `TextActivity.java:1943`, `assets/history*.html` | ヘルプ削除の残骸 (`help.css`、`helpMenu`)。`history*.html` にバージョン履歴がない。 | 未対応 | |
| C-13 | 低 | `ExpandActivity.java:360`, `ImageActivity.java:3914`, `TextActivity.java:2494`, `ImageConfigDialog.java:501`, `TextConfigDialog.java:382` | lint Error MissingSuperCall: `onActivityResult` / `onDismiss` で super 未呼出。 | 未対応 | |
| C-14 | 低 | `ExpandActivity.java:328`, `FileSelectActivity.java:1288, 1327`, `ImageActivity.java:928, 983`, `TextActivity.java:727, 782` | lint GestureBackNavigation: `onKeyDown` で戻るキーを横取りしており、予測型戻るの対応が部分的。 | 未対応 | |
| C-15 | 低 | `dialog/ToolbarEditDialog.java:422-458`, `cropimageview/CropImageView.java:26` | lint Error: `setId(1)` 等のリテラル ID (ResourceType)、`ImageView` 直接継承 (AppCompatCustomView)。 | 未対応 | |
| C-16 | 低 | 各所 | 非推奨 API: `startActivityForResult` (FileSelectActivity, ImageActivity, TextActivity, ExpandActivity, EditServerDialog)、`setSystemUiVisibility`、`getDefaultDisplay`、`Vibrator.vibrate(long)`、`Handler()` 引数なし (CropImageActivity:190)、`Environment.getExternalStorageDirectory`。`FileSelectActivity.java:659-668` の `ActivityResultLauncher` は空のまま。 | 未対応 | |
| C-17 | 低 | `app/build.gradle:104-125` | 依存の古さ: jcifs 2.1.39 (lint は 3.0.3 提示。保守状況から jcifs-ng も候補)、material 1.12.0、core 1.15.0、appcompat 1.7.0、commons-io 2.18.0、juniversalchardet 2.0.4、compileSdk 35 (36 可)。ExpandableLayout は JitPack で未保守。 | 未対応 | |
| C-18 | 低 | `.idea/deploymentTargetSelector.xml`, `.idea/kotlinc.xml`, `gradle.properties:4`, `.gitignore` | `.gitignore` 記載済みなのに追跡されているファイル。Java 専用なのに kotlin 設定。`.serena/` が未追跡のまま。 | 未対応 | |
| C-19 | 低 | `README.md` | 「アプリ内ヘルプが古い」はヘルプ削除済みで不一致。「32bit 向け注意」はリリースが arm64 のみで不一致。 | 未対応 | |
| C-20 | 低 | `app/build.gradle:88, 98`, リポジトリ直下 | `lint.abortOnError=false`、`-Xlint` は deprecation のみ、CI なし。`assembleDebug` + `lint` + `testDebugUnitTest` の GitHub Actions を追加すると B-14 / C-11 / C-13 の再発を防げる。 | 未対応 | |
| C-21 | 低 | `app/proguard-rules.pro:17` | `-keepattributes SourceFile,LineNumberTable` がコメントアウトのままで、リリースのクラッシュに行番号が出ない。 | 未対応 | |
| C-22 | 低 | `app/src/test/**`, `app/build.gradle:84` | テスト 12 件は文字列ヘルパーのみ。`returnDefaultValues=true` は Android API の誤呼び出しを黙って通す。次に価値の高いテスト: `relativePath` の `..` 処理、`renameTo`/`mkdir` の名前検証、WorkStream の位置追跡 (B-20)、しおりプレフィックス照合 (A-12)、`SmbFileAccess.parseUri/parseUser`、RarInputStream ヘッダー解析。 | 未対応 | |
| C-23 | 低 | `res/layout/cropimage.xml:87, 94`, `addbookmarkdialog.xml:44` ほか | ハードコードされた UI 文字列 ("Crop", "Cancel")、非推奨 `android:singleLine`、`0px` 指定。`values-night` なし。 | 未対応 | |
| C-24 | 低 | `dialog/Information.java:104, 111, 279, 294` | ダイアログ `show()` の二重呼び出し。`ACTION_VIEW` で `ActivityNotFoundException` 未捕捉。 | 未対応 | |
| C-25 | 低 | `ImageActivity.java:1018, 4446` | 再利用される `Message` に対する `mReadTimerMsg == msg` の同一性比較。`msg.what` で比較する。 | 未対応 | |
| C-26 | 低 | `ImageActivity.java:4757, 4764` | `saveHistory` が `mFileList[mCurrentPage]` を範囲チェックなしで参照。読込中の `onStop` で例外の可能性。 | 未対応 | |
| C-27 | 低 | `ImageManager.java:241`, `ThumbnailView.java:103`, `ImageActivity.java:1686-1712`, `ImageManager.java:5043` | `mLock = this` で InputStream 自身をロックに使い外部から synchronized。`setThumb` のデコードと `deleteShareCache` の disk IO が UI スレッド。 | 未対応 | |
| C-28 | 低 | `jni/comitton/callImage.cpp:275-283`, `ImageCommon.cpp:331-333, 656-690, 760-765, 828-907`, `callUnrar.cpp:132-150` | キャッシュスロット選択の競合、`SetBitmap` が stride 無視、`CheckImageType` の長さ未検証、`SetBuff` 失敗時の部分ブロック残留、`rarWrite` の offset/size 未検証と不要なコピーバック。 | 未対応 | |
| C-29 | 低 | `textview/TextManager.java:3119-5298`, `ImageActivity.java` (`onTouch` 792 行, `handleMessage` 355 行), `ImageManager.java` (`ImageScaling` 582 行), `SmbFileAccess.java:456-621` | 巨大メソッド (`analyzeComment` 約 2180 行) と、UI スレッド判定で同じ本体を 2 回書いた約 300 行の重複。テーブル駆動化・共通ヘルパー化。 | 未対応 | |
| C-30 | 低 | `TextActivity.java:522-527, 638-655, 2988-2999`, `TextManager.java:2109-2114, 6335-6362`, `ExpandActivity.java:306-318`, `ImageActivity.java:841-884`, `LocalFileAccess.java:45`, `SafFileAccess.java:546-558`, `common/Logcat.java` | コメントアウト残骸、常に真の分岐、未使用の計算結果、`new File(uri)` の捨て文、同一 Javadoc を約 60 回繰り返す 1150 行のログラッパー。 | 未対応 | |

---

## 推奨する着手順

1. A-01, A-05, A-06, A-07, A-11, A-12 は数行の修正で済むのでまとめて対応する。
2. A-02 / A-03 / A-04 (SMB 認証情報と SMB1) をひとつの作業単位にする。既存キーの移行が必要。
3. B-01 / B-08 / B-09 (Activity 終了時の後始末) と B-02 (再描画ループ) で リークとバッテリー消費の大半を止める。
4. B-03 / B-04 / B-05 (Executor 共通化と UI スレッドのブロック解消) は規模が大きいので別作業にする。
5. B-14 / B-15 (設定 XML の一本化) も別作業にする。
6. C-20 の CI を早めに入れておくと、以降の再発を機械的に防げる。
