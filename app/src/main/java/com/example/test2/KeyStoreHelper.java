package com.example.test2;


import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import java.security.KeyStore;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class KeyStoreHelper {

    private static final String KEY_ALIAS = "my_secure_key";
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";

    public static SecretKey getOrCreateSecretKey() throws Exception {

        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);

        if (keyStore.containsAlias(KEY_ALIAS)) {
            // Key exists, return it
            return ((SecretKey) keyStore.getKey(KEY_ALIAS, null));
        }

        // Key does not exist, generate it
        KeyGenerator keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
        keyGenerator.init(new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
        )
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                .setKeySize(256)
                .build());

        return keyGenerator.generateKey();
    }
}