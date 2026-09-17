# 画像読み込み・メモリ配置の最適化計画

対象: `ImageManager.java`(Java側パイプライン) / `jni/comitton/ImageCommon.cpp`, `ImageScale.cpp`, `callImage.cpp`(ネイティブ側キャッシュ)

## 現状の構造

1. Java側が ZIP/RAR/ディレクトリからエントリを読み、`BitmapFactory`(JPEG/PNG/GIF/WebP) または ネイティブデコーダ(AVIF/JXL) で ARGB_8888 に展開する。
2. 展開結果を JNI `ImageSetBitmap` → `SetBuff` で 512KB ブロック(`BLOCKSIZE`×4B)の配列 `gBuffMng` に行単位で詰め直す。
3. 表示時は `CreateScale` が作業領域 `gSclBuffMng` で 余白カット→回転→分割→縮小→シャープ… を段階実行し、結果を `gBuffMng` に memcpy で戻す。
4. `MyImageView.draw` がフレームごとに `ImageDraw` → `DrawBitmap` でブロックから `mCanvasBitmap`(ARGB_8888) へ全画素コピーし、`canvas.drawBitmap` で描く。

## 実施項目と順序

| # | 項目 | 変更ファイル | 状態 |
|---|------|-------------|------|
| 1 | `SetBuff` / `DrawBitmap` の画素ループを行単位コピーに置換。Skia の RGBA_8888 と `MAKE8888` はバイト配置が同一(R,G,B,A)なので、`stride` を尊重しつつ 1 行を一括転送する。`SetBuff` は alpha を 0xFF に強制する現行仕様を維持する。 | ImageCommon.cpp, callImage.cpp, Image.h | 完了 |
| 2 | サイズ取得と本読み込みのストリーム統合。`LoadImage` でサイズ未確定のページは、本読み込み用に開いたストリーム(`openPageStream`)に `mark` → 境界デコード(`SizeCheckImageFromStream`) → `reset` してからそのまま本デコードに進み、アーカイブの再シーク・エントリ再オープンを省く。キャッシュスレッド側も対象ページの事前 `SizeCheckImage` と `memWriteLock` をやめて `LoadImage` に委ねた(1ページ先の見開き判定用 `chkPage2` の事前取得は残す)。PDF は従来通り。 | ImageManager.java | 完了 |
| 3 | `BitmapFactory.Options.inBitmap` による作業 Bitmap の再利用。ページごとに最大 40MB 級の Bitmap を確保・破棄していたのを、`ImageManager` が 1 枚保持して使い回す(`mLock` 配下でしか使わないため排他は既存で足りる)。サイズ不足時のみ作り直し、`close()` で解放。 | ImageManager.java | 完了 |
| 4 | ブロック索引の導入。`gBuffMng` の線形探索(ページ×ブロック数)をやめ、フリーリスト + ページ/種別/半面ごとのブロック番号リストで O(1) 割当・O(ブロック数) 参照にする。ブロック本体は初回使用時に `malloc` する遅延確保に変更し、`ImageGetFreeSize` はカウンタ参照にする。 | ImageCommon.cpp, ImageScale.cpp, callImage.cpp, Image.h | 完了 |
| 5 | スケール結果の memcpy 廃止。`CopySclBuffMngToBuffMng` で作業ブロックと空きブロックの `Buff` ポインタを交換し、512KB×N のコピーをなくす。 | ImageScale.cpp | 完了 |
| 6 | JNI `ImageSetData` を `GetPrimitiveArrayCritical` に変更し、100KB ごとの二重コピーを 1 回にする。 | callImage.cpp | 完了 |
| 7 | 小物: `LoadImageData` の読み込み用 `byte[100KB]` をフィールド `mReadBuffer` に集約(`mLock` 配下でのみ使うため。サムネイル経路の `GetBitmapNativeMain` は別スレッドから呼ばれ得るので従来のローカル確保のまま)、`GetBitmapNativeMain` / `LoadPdfImageData` の `createScaledBitmap` 後の元 Bitmap 解放、`CheckImageType` の JXL 判定バグ修正(同じバイトを二度比較していた)。 | ImageManager.java, ImageCommon.cpp | 完了 |

## 見送り(理由付き)

- 元画像の 8bit グレースケール保持: キャッシュ効率は上がるが、スケール各段と描画の全経路に画素形式の分岐が入る。効果に対して変更範囲が大きすぎるため今回は行わない。
- `ImageGetSize` / `ImageScale` の `int[]` 受け渡し: 数要素なのでコピーコストは無視できる。
- `DrawScaleBitmap`(ルーペ/ピンチ表示) の再サンプリング: 出力先が画面サイズなのでコストは軽い。書き込みだけ 32bit 一括にした。

## 実装メモ

- ブロック索引: `gPageBlocks[index][BLOCKLIST_KEY(page, type, half)]` にブロック番号を格納順で保持する。読み出し側(`DrawBitmap` / `DrawScaleBitmap` / `SetLinesPtr`)は「1ブロックに `BLOCKSIZE / (width + HOKAN_DOTS)` 行」という詰め方を前提に行番号からブロックを直接引く。`SetBuff` の詰め方を変える場合は両方を合わせること。
- 空きブロックは `gFreeList` のスタックで管理し、`gFreeNum` が空き数。`ImageGetFreeSize` はこの値を返す。
- `CopySclBuffMngToBuffMng` のポインタ交換で、本領域の未確保(`nullptr`)ブロックが作業領域側へ渡ることがある。`NextSclBuff` はその場合に確保する。
- `mDecodeBitmap` は `LoadImageData` 内でしか触らず、`LoadImageData` は常に `mLock` 配下で呼ばれる。`close()` での解放も `mLock` を取ってから行う。

## 実機確認後の追加修正

- **設定ダイアログの白地・白文字** `CustomDialogPreference` が Light テーマのまま `AlertDialog` を生成していたため、アプリ共通の `MyDialog` テーマを指定した。あわせて XML の `@null` 指定が文字列 `"@0"` としてボタン名に出ていた問題を直し、OK ボタンは常に標準文言を使うようにした。AndroidX 標準の `ListPreference` / `EditTextPreference` は自前クラスを通らないため、`MyAppCompat` テーマに `alertDialogTheme` を追加して同じ `MyDialog` を使わせ、`MyDialog` には AppCompat 側が参照する `textColorAlertDialogListItem` を追加した(これが無いと選択肢の文字が見えない)。
- **キャッシュを小さくすると画面が真っ黒** 原因は2つ。(1) 先読みページ用の領域確保で表示中ページの元画像まで追い出していた → `memWriteLock` で表示中ページ(見開き相手を含む)を追い出し対象から外した(`isDisplayPage`)。(2) ページ1枚がキャッシュに収まらない → `limitScaleByMemory` で、スケール画像2枚分を差し引いた残り(見開き時はその半分)に収まるまで `inSampleSize` を 2 倍ずつ引き上げる。AVIF/JXL はネイティブデコーダが縮小率を扱わないため対象外。

## 検証

- `gradlew assembleDebug`(JDK は Android Studio 同梱の JBR を使用)でネイティブ含めビルド成功。`comitton` の各ソースは警告なし。
- 実機検証は未実施。以下を確認すること: ZIP/RAR/フォルダの各形式でページ送り、見開き・半面表示、余白カット・回転・シャープ、ルーペ表示、キャッシュサイズを小さくしたときのページ解放とキャッシュ満杯時の挙動、透過 PNG の表示、AVIF/JXL の読み込み。
