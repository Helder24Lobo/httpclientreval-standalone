package com.example.httpclientreval.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProfileTest {

    @Test
    void perfilConBodyMensajeDefault_soloSobreescribeLosCamposIndicados(@TempDir Path tempDir) throws Exception {
        String json = "["
                + "{"
                + "\"nombre\":\"Transaccion X\","
                + "\"llaveAes\":\"12345678901234567890123456789012\","
                + "\"idClienteDefault\":7,"
                + "\"idTransaccionDefault\":1,"
                + "\"ipClienteDefault\":\"172.17.0.4\","
                + "\"bodyMensajeDefault\":{\"IdPersona\":\"301\",\"Referencia1\":\"otro-negocio\"}"
                + "}"
                + "]";
        Path archivo = tempDir.resolve("profiles.json");
        Files.writeString(archivo, json);

        List<Profile> perfiles = Profile.loadAll(archivo);
        Profile perfil = perfiles.get(0);

        assertEquals("301", perfil.bodyMensajeDefault.idPersona);
        assertEquals("otro-negocio", perfil.bodyMensajeDefault.referencia1);
        // Los campos no indicados en el JSON conservan el default genérico de BodyMensaje.
        assertEquals("0", perfil.bodyMensajeDefault.referencia12);
        assertEquals("", perfil.bodyMensajeDefault.convenio);
        assertNull(perfil.headerMensajeDefault);
    }

    @Test
    void perfilSinBodyMensajeDefault_quedaNull(@TempDir Path tempDir) throws Exception {
        String json = "["
                + "{"
                + "\"nombre\":\"Sin body custom\","
                + "\"llaveAes\":\"12345678901234567890123456789012\","
                + "\"idClienteDefault\":7,"
                + "\"idTransaccionDefault\":1,"
                + "\"ipClienteDefault\":\"172.17.0.4\""
                + "}"
                + "]";
        Path archivo = tempDir.resolve("profiles.json");
        Files.writeString(archivo, json);

        Profile perfil = Profile.loadAll(archivo).get(0);

        assertNull(perfil.bodyMensajeDefault);
        assertNull(perfil.headerMensajeDefault);
    }
}
