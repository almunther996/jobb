package com.example.test2;

import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptionUtils {

    private static final String AES = "AES";
    private static final String AES_MODE = "AES/CBC/PKCS5Padding";

    // Static key for simplicity – in production, use Android Keystore
    // private static final String SECRET_KEY = "A1B2C3D4E5F6G7H8"; // 16-char = 128-bit



    public static void encryptFile(File inputFile, File outputFile) throws Exception {


        SecretKey secretKey = KeyStoreHelper.getOrCreateSecretKey();
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");

        try {
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        } catch (Exception e) {
            Log.e("ENCRYPT", "Cipher init failed: " + e.getMessage(), e);
            throw e;
        }
        byte[] iv = cipher.getIV();
        try (FileInputStream fis = new FileInputStream(inputFile);
             FileOutputStream fos = new FileOutputStream(outputFile)) {

            fos.write(iv);

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                byte[] enc = cipher.update(buffer, 0, bytesRead);
                if (enc != null) fos.write(enc);
            }

            byte[] finalBytes = cipher.doFinal();
            if (finalBytes != null) fos.write(finalBytes);
        }
    }


    public static byte[] decryptFile(File inputFile) throws Exception {
        try (FileInputStream fis = new FileInputStream(inputFile);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            byte[] iv = new byte[16];
            fis.read(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            SecretKey secretKey = KeyStoreHelper.getOrCreateSecretKey();
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                byte[] dec = cipher.update(buffer, 0, bytesRead);
                if (dec != null) baos.write(dec);
            }

            byte[] finalBytes = cipher.doFinal();
            if (finalBytes != null) baos.write(finalBytes);

            return baos.toByteArray();
        }
    }

}