package com.example.httpclientreval.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FormatoTamanoTest {

    @Test
    void bytesSeMuestranTalCual() {
        assertEquals("0 B", FormatoTamano.humano(0));
        assertEquals("512 B", FormatoTamano.humano(512));
        assertEquals("1023 B", FormatoTamano.humano(1023));
    }

    @Test
    void kilobytesConUnDecimalSalvoQueSeaRedondo() {
        assertEquals("1 KB", FormatoTamano.humano(1024));
        assertEquals("1.5 KB", FormatoTamano.humano(1536));
        assertEquals("10 KB", FormatoTamano.humano(10240));
    }

    @Test
    void megabytes() {
        assertEquals("1 MB", FormatoTamano.humano(1024L * 1024));
        assertEquals("2.5 MB", FormatoTamano.humano((long) (2.5 * 1024 * 1024)));
    }

    @Test
    void cuentaBytesUtf8NoCaracteres() {
        // "áéí" son 3 caracteres pero 6 bytes en UTF-8 (cada acentuada ocupa 2 bytes).
        assertEquals(6, FormatoTamano.bytesUtf8("áéí"));
        assertEquals(0, FormatoTamano.bytesUtf8(null));
    }
}
