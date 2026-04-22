package com.prompt.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class AesUtil {

    @Value("${jwt.secret}")
    private String secretKey;

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String KEY_ALGORITHM = "AES";

    public String encrypt(String plainText) throws Exception {
        byte[] keyBytes = deriveKey(secretKey);
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, KEY_ALGORITHM);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        byte[] iv = new byte[16];
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
        byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    public String decrypt(String encryptedText) throws Exception {
        byte[] keyBytes = deriveKey(secretKey);
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, KEY_ALGORITHM);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        byte[] iv = new byte[16];
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
        byte[] decoded = Base64.getDecoder().decode(encryptedText);
        byte[] decrypted = cipher.doFinal(decoded);
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    private byte[] deriveKey(String secret) {
        byte[] key = secret.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[32];
        System.arraycopy(key, 0, result, 0, Math.min(key.length, 32));
        return result;
    }
}
