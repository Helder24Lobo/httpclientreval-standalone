package com.example.httpclientreval.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
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

    private static Profile conNombre(String nombre) {
        Profile p = new Profile();
        p.nombre = nombre;
        return p;
    }

    @Test
    void grupoYDetalle_seSeparanPorElPrimerGuion() {
        Profile p = conNombre("Compra pines - Consulta paquetes de contenido digital");

        assertEquals("Compra pines", p.grupo());
        assertEquals("Consulta paquetes de contenido digital", p.detalle());
    }

    @Test
    void grupoYDetalle_conVariosGuionesSoloCortaEnElPrimero() {
        Profile p = conNombre("Recaudos - Pago - convenio");

        assertEquals("Recaudos", p.grupo());
        assertEquals("Pago - convenio", p.detalle());
    }

    @Test
    void grupoYDetalle_sinSeparador_vaAOtrosConElNombreCompleto() {
        Profile p = conNombre(" Compra Paquete Operador");

        assertEquals("Otros", p.grupo());
        assertEquals("Compra Paquete Operador", p.detalle());
    }

    @Test
    void saveAll_yLoadAll_hacenRoundTripCompleto(@TempDir Path tempDir) throws Exception {
        Path archivo = tempDir.resolve("profiles.json");

        Profile perfil = new Profile();
        perfil.nombre = "Nueva Transaccion";
        perfil.llaveAes = "12345678901234567890123456789012";
        perfil.idClienteDefault = 26;
        perfil.idTransaccionDefault = 9;
        perfil.ipClienteDefault = "172.17.0.4";
        perfil.wsseUsername = "usuario";
        perfil.wssePassword = "clave";

        MensajeNegocio.BodyMensaje body = new MensajeNegocio.BodyMensaje();
        body.idPersona = "999";
        body.referencia1 = "prueba";
        perfil.bodyMensajeDefault = body;

        MensajeNegocio.HeaderMensaje header = new MensajeNegocio.HeaderMensaje();
        header.noIdentificacionCajero = "CAJ001";
        perfil.headerMensajeDefault = header;

        List<Profile> perfiles = new ArrayList<>();
        perfiles.add(perfil);

        Profile.saveAll(archivo, perfiles);
        List<Profile> releidos = Profile.loadAll(archivo);

        assertEquals(1, releidos.size());
        Profile releido = releidos.get(0);
        assertEquals("Nueva Transaccion", releido.nombre);
        assertEquals("999", releido.bodyMensajeDefault.idPersona);
        assertEquals("prueba", releido.bodyMensajeDefault.referencia1);
        assertEquals("CAJ001", releido.headerMensajeDefault.noIdentificacionCajero);
        assertEquals("usuario", releido.wsseUsername);
    }

    @Test
    void copia_traeLosMismosValoresQueElOriginal() {
        Profile original = new Profile();
        original.nombre = "Recaudos - Comcel";
        original.llaveAes = "12345678901234567890123456789012";
        original.idClienteDefault = 26;
        original.idTransaccionDefault = 9;
        original.ipClienteDefault = "172.17.0.4";
        original.wsseUsername = "usuario";
        original.wssePassword = "clave";
        MensajeNegocio.BodyMensaje body = new MensajeNegocio.BodyMensaje();
        body.idPersona = "999";
        original.bodyMensajeDefault = body;

        Profile copia = original.copia();

        assertEquals(original.nombre, copia.nombre);
        assertEquals(original.llaveAes, copia.llaveAes);
        assertEquals(original.idClienteDefault, copia.idClienteDefault);
        assertEquals(original.wssePassword, copia.wssePassword);
        assertEquals("999", copia.bodyMensajeDefault.idPersona);
    }

    @Test
    void copia_esIndependienteDelOriginal_editarUnaNoTocaLaOtra() {
        Profile original = new Profile();
        original.nombre = "Recaudos - Comcel";
        MensajeNegocio.BodyMensaje body = new MensajeNegocio.BodyMensaje();
        body.idPersona = "999";
        original.bodyMensajeDefault = body;

        Profile copia = original.copia();
        copia.nombre = "Recaudos - Comcel (copia)";
        copia.bodyMensajeDefault.idPersona = "111";

        assertEquals("Recaudos - Comcel", original.nombre);
        assertEquals("999", original.bodyMensajeDefault.idPersona);
        assertNotSame(original.bodyMensajeDefault, copia.bodyMensajeDefault);
    }
}
