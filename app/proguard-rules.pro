# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-dontwarn javax.naming.**
-dontwarn org.slf4j.**

# このへんをkeepnamesに変更したらSMBにアクセスできなくなった
-keep class org.bouncycastle.jcajce.provider.** { *; }
-keep class org.bouncycastle.jce.provider.** { *; }

# リリースビルドにはデバッグログを含めない。
-assumenosideeffects public class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
}

# アプリ独自のログ (src.comitton.common.Logcat) の verbose/debug/info もリリースビルドから取り除く.
# 引数の文字列組み立ても一緒に消える (ただしメソッド呼び出しを含む引数は残るので isDebugEnabled で囲むこと).
-assumenosideeffects class src.comitton.common.Logcat {
    public static void v(...);
    public static void d(...);
    public static void i(...);
}
