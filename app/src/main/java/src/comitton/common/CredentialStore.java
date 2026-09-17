package src.comitton.common;

import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * SharedPreferences に保存するパスワードを Android Keystore の鍵で暗号化する.<br>
 * 保存形式は {@code enc1:} + Base64(IV + 暗号文).<br>
 * 接頭辞のない値は旧バージョンの平文として扱う (読み出し時に透過的に返し、次回保存時に暗号化される).<br>
 * Keystore の AES 鍵は API 23 以降で利用できるため、それ未満では平文のまま保存する.
 */
public final class CredentialStore {
	private static final String PREFIX = "enc1:";
	private static final String KEYSTORE = "AndroidKeyStore";
	private static final String KEY_ALIAS = "comitton_credential";
	private static final int GCM_TAG_BITS = 128;
	private static final int IV_BYTES = 12;

	private CredentialStore() {
	}

	/**
	 * 平文を保存用の文字列に変換する. 暗号化できない環境では平文をそのまま返す.
	 */
	@NonNull
	public static String encrypt(@Nullable String plain) {
		int logLevel = Logcat.LOG_LEVEL_WARN;
		if (plain == null || plain.isEmpty()) {
			return "";
		}
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
			return plain;
		}
		try {
			SecretKey key = getOrCreateKey();
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			byte[] iv = new byte[IV_BYTES];
			new SecureRandom().nextBytes(iv);
			cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
			byte[] body = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
			byte[] out = new byte[iv.length + body.length];
			System.arraycopy(iv, 0, out, 0, iv.length);
			System.arraycopy(body, 0, out, iv.length, body.length);
			return PREFIX + Base64.encodeToString(out, Base64.NO_WRAP);
		} catch (Exception e) {
			Logcat.e(logLevel, "パスワードを暗号化できませんでした.", e);
			return plain;
		}
	}

	/**
	 * 保存用の文字列を平文に戻す. 復号できない場合は空文字を返す.
	 */
	@NonNull
	public static String decrypt(@Nullable String stored) {
		int logLevel = Logcat.LOG_LEVEL_WARN;
		if (stored == null || stored.isEmpty()) {
			return "";
		}
		if (!stored.startsWith(PREFIX)) {
			// 旧バージョンの平文
			return stored;
		}
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
			return "";
		}
		try {
			byte[] in = Base64.decode(stored.substring(PREFIX.length()), Base64.NO_WRAP);
			if (in.length <= IV_BYTES) {
				return "";
			}
			SecretKey key = getOrCreateKey();
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, in, 0, IV_BYTES));
			byte[] body = cipher.doFinal(in, IV_BYTES, in.length - IV_BYTES);
			return new String(body, StandardCharsets.UTF_8);
		} catch (Exception e) {
			Logcat.e(logLevel, "パスワードを復号できませんでした.", e);
			return "";
		}
	}

	/**
	 * 値が暗号化済みかどうか.
	 */
	public static boolean isEncrypted(@Nullable String stored) {
		return stored != null && stored.startsWith(PREFIX);
	}

	@NonNull
	@RequiresApi(Build.VERSION_CODES.M)
	private static synchronized SecretKey getOrCreateKey() throws Exception {
		KeyStore keyStore = KeyStore.getInstance(KEYSTORE);
		keyStore.load(null);
		KeyStore.Entry entry = keyStore.getEntry(KEY_ALIAS, null);
		if (entry instanceof KeyStore.SecretKeyEntry) {
			return ((KeyStore.SecretKeyEntry) entry).getSecretKey();
		}
		KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE);
		generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
				KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
				.setBlockModes(KeyProperties.BLOCK_MODE_GCM)
				.setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
				.setKeySize(256)
				.build());
		return generator.generateKey();
	}
}
