package com.example.httpclientreval.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CredencialesPerfilesTest {

    private static final String LLAVE_A = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"; // 32 bytes
    private static final String LLAVE_B = "BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB"; // 32 bytes
    private static final String LLAVE_NUEVA = "12345678901234567890123456789012"; // 32 bytes

    private Profile uno;
    private Profile dos;
    private Profile otraLlave;
    private List<Profile> perfiles;

    private static Profile perfil(String nombre, String llave) {
        Profile p = new Profile();
        p.nombre = nombre;
        p.llaveAes = llave;
        p.wsseUsername = "usuario-viejo";
        p.wssePassword = "clave-vieja";
        return p;
    }

    @BeforeEach
    void crearPerfiles() {
        uno = perfil("Recaudos - Uno", LLAVE_A);
        dos = perfil("Recaudos - Dos", LLAVE_A);
        otraLlave = perfil("Otros - Tres", LLAVE_B);
        perfiles = List.of(uno, dos, otraLlave);
    }

    @Test
    void sinAplicarATodos_soloCambiaElPerfilObjetivo() {
        CredencialesPerfiles.Cambio cambio =
                CredencialesPerfiles.aplicar(perfiles, uno, LLAVE_NUEVA, "usuario-nuevo", "clave-nueva", false);

        assertEquals(1, cambio.cantidad());
        assertEquals(LLAVE_NUEVA, uno.llaveAes);
        assertEquals("usuario-nuevo", uno.wsseUsername);
        assertEquals("clave-nueva", uno.wssePassword);
        assertEquals(LLAVE_A, dos.llaveAes);
        assertEquals("usuario-viejo", dos.wsseUsername);
        assertEquals(LLAVE_B, otraLlave.llaveAes);
    }

    @Test
    void aplicandoATodos_cambiaSoloLosQueCompartianLaLlaveAnterior() {
        CredencialesPerfiles.Cambio cambio =
                CredencialesPerfiles.aplicar(perfiles, uno, LLAVE_NUEVA, "usuario-nuevo", "clave-nueva", true);

        assertEquals(2, cambio.cantidad());
        assertEquals(LLAVE_NUEVA, uno.llaveAes);
        assertEquals(LLAVE_NUEVA, dos.llaveAes);
        assertEquals("usuario-nuevo", dos.wsseUsername);
        assertEquals("clave-nueva", dos.wssePassword);
        // El de otra llave no se toca, ni siquiera sus credenciales.
        assertEquals(LLAVE_B, otraLlave.llaveAes);
        assertEquals("usuario-viejo", otraLlave.wsseUsername);
        assertEquals("clave-vieja", otraLlave.wssePassword);
    }

    @Test
    void laLlaveSeRecortaAntesDeValidarla() {
        CredencialesPerfiles.aplicar(perfiles, uno, "  " + LLAVE_NUEVA + "\n", "u", "p", false);

        assertEquals(LLAVE_NUEVA, uno.llaveAes);
    }

    @Test
    void llaveInvalida_lanzaYNoModificaNingunPerfil() {
        assertThrows(IllegalArgumentException.class,
                () -> CredencialesPerfiles.aplicar(perfiles, uno, "muy-corta", "u", "p", true));
        assertThrows(IllegalArgumentException.class,
                () -> CredencialesPerfiles.aplicar(perfiles, uno, null, "u", "p", true));

        assertEquals(LLAVE_A, uno.llaveAes);
        assertEquals(LLAVE_A, dos.llaveAes);
        assertEquals("usuario-viejo", uno.wsseUsername);
    }

    @Test
    void revertir_devuelveTodosLosPerfilesAModificadosASuEstadoAnterior() {
        CredencialesPerfiles.Cambio cambio =
                CredencialesPerfiles.aplicar(perfiles, uno, LLAVE_NUEVA, "usuario-nuevo", "clave-nueva", true);

        cambio.revertir();

        for (Profile p : List.of(uno, dos)) {
            assertEquals(LLAVE_A, p.llaveAes);
            assertEquals("usuario-viejo", p.wsseUsername);
            assertEquals("clave-vieja", p.wssePassword);
        }
    }

    @Test
    void conLaMismaLlave_noIncluyeAlObjetivoNiAOtrasLlaves() {
        assertEquals(List.of(dos), CredencialesPerfiles.conLaMismaLlave(perfiles, uno));
        assertEquals(List.of(), CredencialesPerfiles.conLaMismaLlave(perfiles, otraLlave));
    }

    @Test
    void objetivoSinLlave_noAgrupaATodosLosQueTampocoLaTienen() {
        Profile sinLlave1 = perfil("Sin - Uno", null);
        Profile sinLlave2 = perfil("Sin - Dos", "");
        List<Profile> lista = List.of(sinLlave1, sinLlave2, uno);

        assertEquals(List.of(), CredencialesPerfiles.conLaMismaLlave(lista, sinLlave1));

        CredencialesPerfiles.Cambio cambio =
                CredencialesPerfiles.aplicar(lista, sinLlave1, LLAVE_NUEVA, "u", "p", true);

        assertEquals(1, cambio.cantidad());
        assertEquals(LLAVE_NUEVA, sinLlave1.llaveAes);
        assertEquals("", sinLlave2.llaveAes);
    }

    @Test
    void usuarioYPasswordNulos_quedanVacios() {
        CredencialesPerfiles.aplicar(perfiles, uno, LLAVE_NUEVA, null, null, false);

        assertEquals("", uno.wsseUsername);
        assertEquals("", uno.wssePassword);
    }
}
