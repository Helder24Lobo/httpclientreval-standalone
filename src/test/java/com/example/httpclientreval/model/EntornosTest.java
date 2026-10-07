package com.example.httpclientreval.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntornosTest {

    private static final String LLAVE_PRUEBAS = "12345678901234567890123456789012";
    private static final String LLAVE_PROD = "abcdefghijklmnopqrstuvwxyz012345";

    private static Profile perfilConCredenciales() {
        Profile p = new Profile();
        p.nombre = "Recaudos - Comcel";
        p.llaveAes = LLAVE_PRUEBAS;
        p.wsseUsername = "usuario-pruebas";
        p.wssePassword = "clave-pruebas";
        return p;
    }

    private static Entorno completo(String nombre, String llave) {
        Entorno e = new Entorno(nombre);
        e.url = "https://servicio.ejemplo.com/ws";
        e.llaveAes = llave;
        e.wsseUsername = "u";
        e.wssePassword = "p";
        return e;
    }

    // --- ambientes base ---

    @Test
    void laPrimeraVezCreaPruebasConLasCredencialesDeLosPerfilesYProduccionPendiente(@TempDir Path tmp) {
        Entornos entornos = new Entornos(tmp.resolve("entornos.json"), List.of(perfilConCredenciales()), null);

        assertEquals(2, entornos.lista().size());
        Entorno pruebas = entornos.buscar(Entornos.NOMBRE_PRUEBAS);
        assertEquals(SoapHttpClient.URL_POR_DEFECTO, pruebas.url);
        assertEquals(LLAVE_PRUEBAS, pruebas.llaveAes);
        assertEquals("usuario-pruebas", pruebas.wsseUsername);
        assertTrue(pruebas.listoParaUsar());
        assertFalse(pruebas.produccion);

        Entorno produccion = entornos.buscar(Entornos.NOMBRE_PRODUCCION);
        assertTrue(produccion.produccion);
        assertFalse(produccion.listoParaUsar());
        assertEquals(List.of("la URL del servicio", "la llave AES", "el usuario WSSE", "la contraseña WSSE"),
                produccion.faltantes());

        assertEquals(Entornos.NOMBRE_PRUEBAS, entornos.activo().nombre);
        assertTrue(Files.exists(tmp.resolve("entornos.json")));
    }

    @Test
    void losMarcadoresDeEjemploNoSeTomanComoCredencialesReales(@TempDir Path tmp) {
        Profile ejemplo = perfilConCredenciales();
        ejemplo.llaveAes = "REEMPLAZA_ESTA_LLAVE_POR_32BYTE";

        Entornos entornos = new Entornos(tmp.resolve("entornos.json"), List.of(ejemplo), null);

        assertEquals("", entornos.buscar(Entornos.NOMBRE_PRUEBAS).llaveAes);
    }

    @Test
    void unaConfiguracionPersonalizadaDeLaVersionAnteriorSeConservaComoAmbiente(@TempDir Path tmp) {
        Entorno legado = new Entorno("Personalizado");
        legado.url = "https://mi-servidor.com/ws";
        legado.produccion = true;

        Entornos entornos = new Entornos(tmp.resolve("entornos.json"), List.of(perfilConCredenciales()), legado);

        assertEquals(3, entornos.lista().size());
        Entorno migrado = entornos.buscar("Personalizado");
        assertEquals("https://mi-servidor.com/ws", migrado.url);
        assertTrue(migrado.produccion);
        assertEquals(LLAVE_PRUEBAS, migrado.llaveAes);
        // Estaba en uso y quedó completo: sigue siendo el activo, como antes.
        assertEquals("Personalizado", entornos.activo().nombre);
    }

    // --- persistencia ---

    @Test
    void loGuardadoSeRelee(@TempDir Path tmp) throws Exception {
        Path archivo = tmp.resolve("entornos.json");
        Entornos primero = new Entornos(archivo, List.of(perfilConCredenciales()), null);
        primero.guardar(null, completo("Desarrollo", LLAVE_PROD));
        primero.activar("Desarrollo");

        Entornos segundo = new Entornos(archivo, List.of(), null);

        assertEquals("Desarrollo", segundo.activo().nombre);
        assertEquals(LLAVE_PROD, segundo.buscar("Desarrollo").llaveAes);
        assertEquals(3, segundo.lista().size());
    }

    @Test
    void unArchivoDanadoSeApartaYSeCreanLosBase(@TempDir Path tmp) throws Exception {
        Path archivo = tmp.resolve("entornos.json");
        Files.writeString(archivo, "{ esto no es json");

        Entornos entornos = new Entornos(archivo, List.of(perfilConCredenciales()), null);

        assertEquals(2, entornos.lista().size());
        try (var archivos = Files.list(tmp)) {
            assertTrue(archivos.anyMatch(p -> p.getFileName().toString().startsWith("entornos.json.corrupto-")));
        }
    }

    @Test
    void unArchivoDanadoSeRecuperaDelUltimoRespaldoEnVezDeVolverALosBase(@TempDir Path tmp) throws Exception {
        Path archivo = tmp.resolve("entornos.json");
        Entornos primero = new Entornos(archivo, List.of(perfilConCredenciales()), null);
        primero.guardar(null, completo("Desarrollo", LLAVE_PROD));
        primero.guardar("Desarrollo", completo("Desarrollo", LLAVE_PROD));
        primero.activar("Desarrollo");
        // El archivo se corrompe (corte de luz, disco, edición a mano...).
        Files.writeString(archivo, "{ esto no es json");

        Entornos recuperado = new Entornos(archivo, List.of(), null);

        // No empieza de cero: conserva el ambiente que se había creado, con sus credenciales.
        assertNotNull(recuperado.buscar("Desarrollo"));
        assertEquals(LLAVE_PROD, recuperado.buscar("Desarrollo").llaveAes);
        // Y el archivo vuelve a estar sano, listo para el siguiente arranque.
        assertEquals(recuperado.lista().size(), new Entornos(archivo, List.of(), null).lista().size());
    }

    @Test
    void siNingunRespaldoSirveSeCreanLosBase(@TempDir Path tmp) throws Exception {
        Path archivo = tmp.resolve("entornos.json");
        new Entornos(archivo, List.of(perfilConCredenciales()), null);
        Files.writeString(archivo, "{ roto");
        Files.createDirectories(tmp.resolve("respaldos"));
        Files.writeString(tmp.resolve("respaldos").resolve("entornos.json.bak1"), "tampoco es json");

        Entornos entornos = new Entornos(archivo, List.of(perfilConCredenciales()), null);

        assertEquals(2, entornos.lista().size());
        assertEquals(Entornos.NOMBRE_PRUEBAS, entornos.activo().nombre);
    }

    // --- crear y editar ---

    @Test
    void sePuedeCrearUnAmbienteNuevoIncompletoYCompletarloDespues(@TempDir Path tmp) throws Exception {
        Entornos entornos = new Entornos(tmp.resolve("entornos.json"), List.of(perfilConCredenciales()), null);

        entornos.guardar(null, new Entorno("QA"));
        assertFalse(entornos.buscar("QA").listoParaUsar());

        Entorno completado = completo("QA", LLAVE_PROD);
        entornos.guardar("QA", completado);
        assertTrue(entornos.buscar("QA").listoParaUsar());
        assertEquals(3, entornos.lista().size());
    }

    @Test
    void losNombresSonUnicosSinDistinguirMayusculas(@TempDir Path tmp) throws Exception {
        Entornos entornos = new Entornos(tmp.resolve("entornos.json"), List.of(perfilConCredenciales()), null);

        assertThrows(IllegalArgumentException.class, () -> entornos.guardar(null, new Entorno("pruebas")));
    }

    @Test
    void renombrarElActivoMantieneLaActivacion(@TempDir Path tmp) throws Exception {
        Entornos entornos = new Entornos(tmp.resolve("entornos.json"), List.of(perfilConCredenciales()), null);
        Entorno editado = entornos.activo().copia();
        editado.nombre = "Pruebas internas";

        entornos.guardar(Entornos.NOMBRE_PRUEBAS, editado);

        assertEquals("Pruebas internas", entornos.activo().nombre);
        assertNull(entornos.buscar(Entornos.NOMBRE_PRUEBAS));
    }

    @Test
    void datosInvalidosSeRechazanSinCambiarNada(@TempDir Path tmp) throws Exception {
        Entornos entornos = new Entornos(tmp.resolve("entornos.json"), List.of(perfilConCredenciales()), null);

        Entorno urlMala = completo("X", LLAVE_PROD);
        urlMala.url = "no-es-una-url";
        assertThrows(IllegalArgumentException.class, () -> entornos.guardar(null, urlMala));

        Entorno llaveCorta = completo("Y", "corta");
        assertThrows(IllegalArgumentException.class, () -> entornos.guardar(null, llaveCorta));

        Entorno timeoutFuera = completo("Z", LLAVE_PROD);
        timeoutFuera.timeoutSegundos = 1;
        assertThrows(IllegalArgumentException.class, () -> entornos.guardar(null, timeoutFuera));

        assertEquals(2, entornos.lista().size());
    }

    // --- activar y eliminar ---

    @Test
    void soloSePuedeActivarUnAmbienteCompleto(@TempDir Path tmp) throws Exception {
        Entornos entornos = new Entornos(tmp.resolve("entornos.json"), List.of(perfilConCredenciales()), null);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> entornos.activar(Entornos.NOMBRE_PRODUCCION));
        assertTrue(error.getMessage().contains("la llave AES"), error.getMessage());
        assertEquals(Entornos.NOMBRE_PRUEBAS, entornos.activo().nombre);

        Entorno prod = completo(Entornos.NOMBRE_PRODUCCION, LLAVE_PROD);
        prod.produccion = true;
        entornos.guardar(Entornos.NOMBRE_PRODUCCION, prod);
        entornos.activar(Entornos.NOMBRE_PRODUCCION);

        assertEquals(Entornos.NOMBRE_PRODUCCION, entornos.activo().nombre);
        assertEquals(SoapHttpClient.Ambiente.PRODUCCION, SoapHttpClient.ambienteDe(entornos.activo()));
    }

    @Test
    void elAmbienteActivoNoPuedeQuedarIncompleto(@TempDir Path tmp) throws Exception {
        Entornos entornos = new Entornos(tmp.resolve("entornos.json"), List.of(perfilConCredenciales()), null);
        Entorno vaciado = entornos.activo().copia();
        vaciado.llaveAes = "";

        assertThrows(IllegalArgumentException.class, () -> entornos.guardar(Entornos.NOMBRE_PRUEBAS, vaciado));
        assertTrue(entornos.activo().listoParaUsar());
    }

    @Test
    void noSeEliminaElActivoNiElUltimo(@TempDir Path tmp) throws Exception {
        Entornos entornos = new Entornos(tmp.resolve("entornos.json"), List.of(perfilConCredenciales()), null);

        assertThrows(IllegalArgumentException.class, () -> entornos.eliminar(Entornos.NOMBRE_PRUEBAS));

        entornos.eliminar(Entornos.NOMBRE_PRODUCCION);
        assertEquals(1, entornos.lista().size());
        assertThrows(IllegalArgumentException.class, () -> entornos.eliminar(Entornos.NOMBRE_PRUEBAS));
    }

    // --- clasificación y credenciales efectivas ---

    @Test
    void elAmbienteSeClasificaPorLaMarcaYLaUrl() {
        Entorno pruebas = new Entorno("Pruebas");
        pruebas.url = SoapHttpClient.URL_POR_DEFECTO;
        Entorno desarrollo = new Entorno("Desarrollo");
        desarrollo.url = "https://dev.ejemplo.com/ws";
        Entorno prod = new Entorno("Producción");
        prod.url = "https://prod.ejemplo.com/ws";
        prod.produccion = true;

        assertEquals(SoapHttpClient.Ambiente.PRUEBAS, SoapHttpClient.ambienteDe(pruebas));
        assertEquals(SoapHttpClient.Ambiente.PERSONALIZADO, SoapHttpClient.ambienteDe(desarrollo));
        assertEquals(SoapHttpClient.Ambiente.PRODUCCION, SoapHttpClient.ambienteDe(prod));
    }

    @Test
    void lasCredencialesSalenDelAmbienteYElPerfilSoloEsRespaldo() {
        Profile perfil = perfilConCredenciales();
        Entorno conCredenciales = completo("Producción", LLAVE_PROD);
        Entorno sinCredenciales = new Entorno("Vacío");

        assertEquals(LLAVE_PROD, Credenciales.llave(perfil, conCredenciales));
        assertEquals("u", Credenciales.usuario(perfil, conCredenciales));
        assertEquals("p", Credenciales.password(perfil, conCredenciales));

        assertEquals(LLAVE_PRUEBAS, Credenciales.llave(perfil, sinCredenciales));
        assertEquals("usuario-pruebas", Credenciales.usuario(perfil, sinCredenciales));
    }

    @Test
    void laFirmaCambiaConCualquierDatoQueAfecteElEnvio() {
        Entorno base = completo("A", LLAVE_PRUEBAS);
        String original = base.firma();

        Entorno otraLlave = base.copia();
        otraLlave.llaveAes = LLAVE_PROD;
        Entorno otraUrl = base.copia();
        otraUrl.url = "https://otra.com/ws";

        assertNotNull(original);
        assertEquals(original, base.copia().firma());
        assertFalse(original.equals(otraLlave.firma()));
        assertFalse(original.equals(otraUrl.firma()));
    }
}
