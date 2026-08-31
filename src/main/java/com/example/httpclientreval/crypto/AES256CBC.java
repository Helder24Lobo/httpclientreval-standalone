package com.example.httpclientreval.crypto;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * Replica del AES256CBC del proyecto Android original (paquete
 * com.example.httpclientreval.crypto). Única diferencia real: se usa
 * java.util.Base64 en vez de android.util.Base64. El resto (Cipher,
 * SecretKeySpec, IvParameterSpec) es exactamente igual.
 *
 * Formato del paquete cifrado: Base64( IV(16 bytes) + AES-CBC-cipherText ).
 */
public class AES256CBC {

    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final String ALGORITHM = "AES";
    private static final int IV_LENGTH_BYTES = 16;
    private static final int KEY_LENGTH_BYTES = 32; // AES-256

    private AES256CBC() {
    }

    /**
     * Valida que la llave tenga exactamente 32 bytes UTF-8 (AES-256).
     * Lanza IllegalArgumentException con mensaje claro si no.
     */
    public static void validarLlave(String llave) {
        if (llave == null) {
            throw new IllegalArgumentException("La llave AES no puede ser null.");
        }
        int len = llave.getBytes(StandardCharsets.UTF_8).length;
        if (len != KEY_LENGTH_BYTES) {
            throw new IllegalArgumentException(
                    "La llave AES debe tener exactamente 32 bytes UTF-8 (AES-256). "
                            + "Longitud actual: " + len + " bytes.");
        }
    }

    /**
     * Cifra plainText con AES/CBC/PKCS5Padding usando un IV aleatorio de 16 bytes,
     * y devuelve Base64( IV + cipherText ), tal como espera el campo "_mensaje".
     */
    public static String encryptWithRandomIV(String plainText, String llave) throws Exception {
        validarLlave(llave);

        byte[] keyBytes = llave.getBytes(StandardCharsets.UTF_8);
        byte[] iv = new byte[IV_LENGTH_BYTES];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, ALGORITHM);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

        byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        byte[] combined = new byte[IV_LENGTH_BYTES + cipherText.length];
        System.arraycopy(iv, 0, combined, 0, IV_LENGTH_BYTES);
        System.arraycopy(cipherText, 0, combined, IV_LENGTH_BYTES, cipherText.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    /**
     * Descifra un valor Base64 con el formato IV(16 bytes) + cipherText, devolviendo
     * el texto plano (JSON de negocio).
     */
    public static String decryptWithPrependedIV(String base64Data, String llave) throws Exception {
        validarLlave(llave);

        byte[] keyBytes = llave.getBytes(StandardCharsets.UTF_8);
        byte[] combined = Base64.getDecoder().decode(base64Data);

        if (combined.length <= IV_LENGTH_BYTES) {
            throw new IllegalArgumentException(
                    "El dato cifrado es demasiado corto para contener IV (16 bytes) + cipherText.");
        }

        byte[] iv = Arrays.copyOfRange(combined, 0, IV_LENGTH_BYTES);
        byte[] cipherText = Arrays.copyOfRange(combined, IV_LENGTH_BYTES, combined.length);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, ALGORITHM);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

        byte[] plainBytes = cipher.doFinal(cipherText);
        return new String(plainBytes, StandardCharsets.UTF_8);
    }
}
