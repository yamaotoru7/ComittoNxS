# ComittoNxX 要修正項目リスト

ComittoNxX のコードベースおよび実行環境の精査によって抽出された課題・要修正項目の一覧です。
各項目は重要度およびカテゴリ順に分類されています。

---

## 1. 最新 Android (14/15) 互換性向上

- [ ] **1.1 Edge-to-Edge（全画面表示）対応（Android 15 必須）**
  - **対象ファイル**: `FileSelectActivity.java`, `ImageActivity.java`, `TextActivity.java`
  - **内容**: Target SDK 35 (Android 15) では Edge-to-Edge が強制適用されるが、現在は非推奨の `setSystemUiVisibility` (API 30 非推奨) に依存し `WindowInsets` のハンドリングがない。`WindowInsetsCompat` / `WindowInsetsControllerCompat` を導入し、ステータスバー・ナビゲーションバーやノッチとUI要素の重なりを解消する。

- [ ] **1.2 予測型「戻る」ジェスチャー（Predictive Back）対応**
  - **対象ファイル**: 全画面の `onKeyDown` / `onKeyUp`
  - **内容**: 戻るキーの処理が `KeyEvent.KEYCODE_BACK` のキーイベント監視で行われており、Android 13/14+ のジェスチャーナビゲーション（スワイプで戻る）や Predictive Back と整合しない。AndroidX の `OnBackPressedDispatcher` / `OnBackPressedCallback` へ移行する。

- [ ] **1.3 非推奨 Preference API (`PreferenceActivity`) の刷新**
  - **対象ファイル**: `app/src/main/java/src/comitton/config/` 配下のすべての設定画面
  - **内容**: 全ての設定 Activity が Android 10 (API 29) で非推奨となった `android.preference.PreferenceActivity` を直接継承しており、コンパイル時に大量の警告が発生している。`androidx.preference.PreferenceFragmentCompat` にリプレイスする。

- [ ] **1.4 非推奨ダイアログ API (`ProgressDialog`, `showDialog(int)`) の移行**
  - **対象ファイル**: `FileSelectActivity.java`, `ImageActivity.java`, `TextActivity.java` 等
  - **内容**: Android 3.0 (API 11) 非推奨の `showDialog(int)` / `onCreateDialog(int)` および Android 8.0 非推奨の `ProgressDialog` を `DialogFragment` や現代的なプログレス表示に移行する。

## 2. アーキテクチャ・品質・セキュリティ

- [ ] **2.1 God Activity の分割・責務分離**
  - **対象ファイル**: `FileSelectActivity.java` (4,344行), `ImageActivity.java` (4,918行), `TextActivity.java` (3,159行)
  - **内容**: UI描画、入力処理、ファイルIO、非同期スレッド、ダイアログ制御が巨大な単一クラスに密結合している。ViewModel や専用コントローラークラス等へ責務を順次切り出す。

- [ ] **2.2 自動テスト環境（Unit Test / Instrumentation Test）の導入**
  - **対象ファイル**: `app/src/test/`
  - **内容**: テストコードが皆無なため、文字列パース、書誌情報解析、座標変換等の重要ロジックを対象に JUnit テストを追加し、リファクタリング時のデグレを自動検知できるようにする。

- [ ] **2.3 メモリリーク対策**
  - **対象ファイル**: `ThumbnailLoader.java`, 各種リスナー・スレッド
  - **内容**: バックグラウンドスレッドや非同期タスクが `AppCompatActivity` の強参照を保持し、画面回転時にメモリリークが発生するのを防ぐため、WeakReference や LifecycleAware な構成に変更する。

- [ ] **2.4 SMB 認証情報の安全な保管**
  - **対象ファイル**: `RecordList.java` (`server.dat`)
  - **内容**: 登録サーバーのユーザー名・パスワードが内部ストレージの TSV に平文保存されているため、`EncryptedSharedPreferences` や Keystore を用いた暗号化を検討・適用する。

- [ ] **2.5 `RECORD_AUDIO`（マイク権限）の分離または見直し**
  - **対象ファイル**: `AndroidManifest.xml`, `SetNoiseActivity.java`
  - **内容**: 音声ノイズによるページめくり機能のためにマイク権限が宣言されているが、一般のビュワー利用者にとっては不要かつセキュリティ上の懸念になりやすい。機能のオプトイン化や権限要求の最適化を検討する。
