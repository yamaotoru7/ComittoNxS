# ComittoNxS

<img src="app/src/main/res/drawable-hdpi/comittonxx.png" width="100" align="right" alt="logo">

ComittoNxS は [ComittoNxX](https://github.com/ComittoNxA/ComittoNxX) を元にしたオープンソースの画像ビュワーです.  
対応バージョンは Android 5.0 以上 15 までです (minSdk 21 / targetSdk 35).

- 対応する電子書籍ファイルの形式：  
  PDF、EPUB、TEXT、青空文庫

- 対応する画像ファイルの形式：  
  JPEG、GIF、PNG、WebP、AVIF、HEIF(Android 8.0以上)、JXL

- 対応する圧縮ファイルの形式：  
  ZIP、RAR

- 対応する共有オンラインストレージサービス：
  - Windowsファイル共有(SMB)
  - 外部アプリを介した共有オンラインストレージへのアクセス  
    (一応動作はしますが、組み合わせによっては使い物になりません)
    - ストレージアクセスフレームワーク(SAF)
      - CIFS Documents Provider : [Google Play](https://play.google.com/store/apps/details?id=com.wa2c.android.cifsdocumentsprovider)、[マニュアル](https://github.com/wa2c/cifs-documents-provider/wiki/Manual-ja)  
        SMB、FTP、FTPS、SFTPをサポート.
      - Round Sync : [GitHub](https://github.com/newhinton/Round-Sync)  
        多くのクラウド ストレージ プロバイダーに対応.
    - ファイルピッカー
      - Google ドライブ
      - Microsoft OneDrive
      - その他メーカー純正アプリ等

    アカウントの認証などは外部アプリ側で設定してください.  
    これらの外部アプリは起動時に自動実行やバックグラウンドで停止しないなどの設定を推奨します.

## 注意事項

- 配布ビルドは 64bit (arm64-v8a) のみです. 32bit 端末で使う場合は `abiFilters` を変更してビルドしてください.
- 使用メモリサイズが小さくページ1枚が収まらない場合は、自動的に縮小率を上げて読み込みます (AVIF/JXL を除く). 高解像度のページを等倍で見たい場合は使用メモリサイズを増やしてください.
- SMBの接続情報にドメイン名を入力すると初回接続が遅くなる場合があります.  不要なドメイン名(WORKGROUP等)の使用は避けてください.
- オンラインストレージ上のRAR圧縮形式ファイルの構造解析は時間がかかります.サムネイル作成時にファイル名を解析する場合にはZIP圧縮形式の使用を推奨します.

## 既知の不具合

- ソリッド圧縮された RAR は表示できません (エラーになります).
- パスワード付きの RAR には対応していません.

## ComittoNxX からの変更点

- アプリ名を ComittoNxS に変更 (applicationId は `jp.dip.muracoro.comittonx` のまま).
- 設定画面を AndroidX PreferenceFragmentCompat へ移行. 非推奨の Dialog API (ProgressDialog / showDialog) を置き換え.
- Edge-to-Edge 表示および予測型「戻る」ジェスチャーに対応.
- マイク機能 (RECORD_AUDIO 権限と音声ノイズ操作) を削除.
- Android 10 でのダウンロード不具合、ピンチ/ルーペ表示の座標ずれ、無限スクロールを修正.
- サムネイル系ローダーのメモリリークを対策. ユニットテスト環境を導入.
- 画像読み込みとネイティブキャッシュのメモリ配置を最適化 (詳細は [docs/image-pipeline-optimization.md](docs/image-pipeline-optimization.md)).
- 設定ダイアログが白地に白文字で表示される問題を修正. ダイアログ背景を不透明化.
- SMB のパスワードを Android Keystore の鍵で暗号化して保存. 設定のエクスポートにはパスワードを含めない.
- SMB1 での接続を既定で無効化 (共通設定の「SMB1 での接続を許可する」で有効化できる).
- 他のアプリから共有されたファイル (content://) を開けるように修正.
- 上流 ComittoNxX を参照していた更新確認とダウンロード数表示を停止・削除.
- RAR 展開を書庫ごとに独立させ、壊れたデータやパスワード付きの RAR をエラーとして表示.
- 設定画面の文言を strings.xml に集約し、英語・日本語で既定値が食い違っていた問題を修正.

#### ComittoNxX が [NxD](https://github.com/Kdroidwin/cnxd/tree/cnxd) に加えていた修正点

- PNG形式で表示できないファイルがあったのを修正.
- AVIF形式画像の表示に対応.
- HEIF形式画像の表示に対応.(Android 8.0以上)
- JXL形式画像の表示に対応.
- PDF形式電子書籍の表示に対応.
- EPUB形式電子書籍の本文テキストの表示に対応.

## ダウンロード

このリポジトリからビルドしてご利用ください.  
元になった ComittoNxX の配布物は [Releases](https://github.com/ComittoNxA/ComittoNxX/releases) にあります.

## ビルド

### 必要なもの

- JDK 17 以上 (Gradle 9.3 の要件. ソース互換は Java 21 指定なので JDK 21 か Android Studio 同梱の JBR を推奨)
- Android SDK Platform 35、NDK、CMake 3.22.1 (Android Studio の SDK Manager から導入)
- [Git](https://git-scm.com/)、[Ninja](https://ninja-build.org/)、[Meson](https://mesonbuild.com/)、[pkg-config](https://www.freedesktop.org/wiki/Software/pkg-config/)、[NASM](https://www.nasm.us/)  
  (dav1d のビルドに使用. Windows では Git for Windows 同梱の patch / grep / sed も unrar のビルドで使用)
- ネットワーク接続 (初回ビルド時に dav1d、libavif、libjxl、unrar のソースを CMake FetchContent で取得)

`local.properties` に `sdk.dir` を設定してください (Android Studio で開くと自動生成されます).

### ビルド手順

[Android Studio](https://developer.android.com/studio/install) で開くか、以下のコマンドでビルドします.

```sh
# デバッグ版 → app/build/outputs/apk/debug/ComittoNxS[debug]-<version>.apk
./gradlew assembleDebug

# リリース版 (minify あり) → app/build/outputs/apk/release/ComittoNxS-<version>.apk
./gradlew assembleRelease
```

`JAVA_HOME` が JDK 11 以前を指していると Gradle が起動しません. その場合は JDK 17 以上のパスを指定してください.

```sh
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew assembleRelease
```

ビルド対象 ABI は arm64-v8a のみです.

### 署名について

現状の `app/build.gradle` は release ビルドでも `signingConfigs.debug` (`~/.android/debug.keystore`) で署名します. ローカル端末への上書きインストールを簡単にするための設定で、ストア配布用の署名ではありません.

配布用の鍵で署名する場合は以下の手順で設定します. キーストアと `signingConfigs/` 以下は `.gitignore` で除外されているためリポジトリには入りません.

<details><summary><b>配布用署名の設定手順</b></summary>
<p>

###### 署名の作成

キーストアファイルを作成して `signingConfigs/` に保存します.  
Android Studio の場合は [Build] > [Generate Signed Bundle/APK] から作成します.

###### signingConfigs/release.gradle の作成

`signingConfigs/release.gradle` を作成します. `app/build.gradle` はこのファイルが存在すれば読み込みます.

```gradle
signingConfigs {
    release {
        storePassword '${署名ファイルのパスワード}'
        keyPassword '${鍵のパスワード}'
        storeFile file('${署名のファイル名}')
        keyAlias '${鍵のエイリアス}'
    }
}
```

###### build.gradle の切り替え

`app/build.gradle` の `buildTypes.release` にある `signingConfig signingConfigs.debug` を `signingConfig signingConfigs.release` に変更します.
</p>
</details>

## ドキュメント

- [docs/image-pipeline-optimization.md](docs/image-pipeline-optimization.md) : 画像読み込み・ネイティブキャッシュの構造と最適化内容
- [ISSUES.md](ISSUES.md) : コードベース精査で抽出した課題と対応状況の一覧

## ライセンス

LICENSE ファイルに記載されたライセンスに基づきます.  
内部で使用しているライブラリのライセンスは各ライブラリの指定に従います.  

本ソースは、[ComittoNおよびComittoNxN](https://docs.google.com/open?id=0Bzx6UxEo3Pg0SXNIQVdRVnVqemM)、[ComittoNxM](https://www.axfc.net/u/3792235)、[ComittoNxT](https://www.axfc.net/u/3978158)、[ComittoNxA](https://github.com/ComittoNxA/ComittoNxA/tree/1.65A20)、[ComittoNxAC](https://www.axfc.net/u/4059552)、[ComittoNxD](https://github.com/Kdroidwin/cnxd/tree/cnxd)、[ComittoNxX](https://github.com/ComittoNxA/ComittoNxX) のソースを元にしています.
