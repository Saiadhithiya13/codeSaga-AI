package com.codesage.security.util;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM token encryption service.
 *
 * <p>Architecture Spec §Security: AES-256 encryption for GitHub tokens.
 * Uses AES-256 in GCM (Galois/Counter Mode) which provides:
 * <ul>
 *   <li>Authenticated encryption (integrity protection)</li>
 *   <li>Randomized IV per encryption (prevents pattern analysis)</li>
 *   <li>256-bit key strength</li>
 * </ul>
 *
 * <p>Encrypted format (all base64): {@code IV:CIPHERTEXT}
 * The IV is prepended to the ciphertext to allow decryption.
 */
@Log4j2
@Service
public class TokenEncryptionService {

    private static final String ALGORITHM      = "AES/GCM/NoPadding";
    private static final String KEY_ALGORITHM  = "AES";
    private static final int    GCM_IV_LENGTH  = 12;   // 96-bit IV recommended for GCM
    private static final int    GCM_TAG_LENGTH = 128;  // 128-bit authentication tag

    private final SecretKey secretKey;

    public TokenEncryptionService(
            @Value("${app.encryption.key}") String base64EncodedKey) {
        byte[] keyBytes = Base64.getDecoder().decode(base64EncodedKey);
        this.secretKey = new SecretKeySpec(keyBytes, KEY_ALGORITHM);
        log.info("TokenEncryptionService initialized with AES-256-GCM");
    }

    /**
     * Encrypts a plain-text token using AES-256-GCM.
     * A new random IV is generated per call.
     *
     * @param plaintext the raw token to encrypt
     * @return base64-encoded {@code IV:CIPHERTEXT} string
     */
    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes());

            String ivBase64         = Base64.getEncoder().encodeToString(iv);
            String ciphertextBase64 = Base64.getEncoder().encodeToString(ciphertext);

            return ivBase64 + ":" + ciphertextBase64;
        } catch (Exception e) {
            log.error("Token encryption failed", e);
            throw new RuntimeException("Failed to encrypt token", e);
        }
    }

    /**
     * Decrypts an AES-256-GCM encrypted token.
     *
     * @param encrypted base64-encoded {@code IV:CIPHERTEXT} string
     * @return the original plain-text token
     */
    public String decrypt(String encrypted) {
        try {
            String[] parts = encrypted.split(":");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid encrypted token format");
            }

            byte[] iv         = Base64.getDecoder().decode(parts[0]);
            byte[] ciphertext = Base64.getDecoder().decode(parts[1]);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            return new String(cipher.doFinal(ciphertext));
        } catch (Exception e) {
            log.error("Token decryption failed", e);
            throw new RuntimeException("Failed to decrypt token", e);
        }
    }

    /**
     * Utility: generates a new random AES-256 key as base64.
     * Use this to generate the {@code APP_ENCRYPTION_KEY} env var.
     *
     * <p>Run once: {@code TokenEncryptionService.generateKey()}
     */
    public static String generateKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(KEY_ALGORITHM);
            keyGen.init(256, new SecureRandom());
            SecretKey key = keyGen.generateKey();
            return Base64.getEncoder().encodeToString(key.getEncoded());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate AES-256 key", e);
        }
    }
}
