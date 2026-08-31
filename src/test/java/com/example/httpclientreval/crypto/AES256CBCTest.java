package com.example.httpclientreval.crypto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AES256CBCTest {

    // Llave de PRUEBA de exactamente 32 caracteres ASCII (32 bytes UTF-8). No es un secreto real.
    private static final String LLAVE_32_BYTES = "12345678901234567890123456789012";

    @Test
    void roundTripEncryptDecrypt_devuelveElMismoJson() throws Exception {
        String jsonOriginal = "{\"headerMensaje\":{\"a\":1},\"BodyMensaje\":{\"b\":\"texto\"}}";

        String cifrado = AES256CBC.encryptWithRandomIV(jsonOriginal, LLAVE_32_BYTES);
        String descifrado = AES256CBC.decryptWithPrependedIV(cifrado, LLAVE_32_BYTES);

        assertEquals(jsonOriginal, descifrado);
    }

    @Test
    void cadaCifradoUsaUnIVDistinto() throws Exception {
        String json = "{\"x\":1}";

        String cifrado1 = AES256CBC.encryptWithRandomIV(json, LLAVE_32_BYTES);
        String cifrado2 = AES256CBC.encryptWithRandomIV(json, LLAVE_32_BYTES);

        // Mismo texto plano, misma llave, pero IV aleatorio -> salidas distintas.
        assertNotEquals(cifrado1, cifrado2);
    }

    @Test
    void llaveConLongitudIncorrecta_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> AES256CBC.validarLlave("muy-corta"));
    }

    @Test
    void llaveNull_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> AES256CBC.validarLlave(null));
    }
}
