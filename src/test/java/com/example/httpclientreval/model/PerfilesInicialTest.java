package com.example.httpclientreval.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerfilesInicialTest {

    @Test
    void crearDesdeEjemploCreaLaCarpetaYUnArchivoLegible(@TempDir Path tmp) throws Exception {
        Path destino = tmp.resolve("no-existe-aun").resolve("profiles.json");

        PerfilesInicial.crearDesdeEjemplo(destino);

        List<Profile> perfiles = Profile.loadAll(destino);
        assertFalse(perfiles.isEmpty());
        assertEquals("REEMPLAZA_ESTA_LLAVE_POR_32BYTE", perfiles.get(0).llaveAes);
    }

    @Test
    void apartarCorruptoConservaElContenidoConOtroNombre(@TempDir Path tmp) throws Exception {
        Path archivo = tmp.resolve("profiles.json");
        Files.writeString(archivo, "{ esto no es json");

        Path aparte = PerfilesInicial.apartarCorrupto(archivo);

        assertFalse(Files.exists(archivo));
        assertTrue(aparte.getFileName().toString().startsWith("profiles.json.corrupto-"));
        assertEquals("{ esto no es json", Files.readString(aparte));
    }

    @Test
    void copiarValidaElOrigenAntesDeEscribirElDestino(@TempDir Path tmp) throws Exception {
        Path malo = tmp.resolve("malo.json");
        Files.writeString(malo, "[]");
        Path destino = tmp.resolve("profiles.json");

        assertThrows(IllegalStateException.class, () -> PerfilesInicial.copiar(malo, destino));
        assertFalse(Files.exists(destino));
    }

    @Test
    void copiarTraeLosPerfilesDelOrigen(@TempDir Path tmp) throws Exception {
        Path origen = tmp.resolve("origen.json");
        PerfilesInicial.crearDesdeEjemplo(origen);
        Path destino = tmp.resolve("sub").resolve("profiles.json");

        PerfilesInicial.copiar(origen, destino);

        assertEquals(Profile.loadAll(origen).size(), Profile.loadAll(destino).size());
    }

    private static Profile perfil(String nombre, String llave) {
        Profile p = new Profile();
        p.nombre = nombre;
        p.llaveAes = llave;
        return p;
    }

    @Test
    void combinarSumaLasViejasSinPisarLasNuevas(@TempDir Path tmp) throws Exception {
        Path viejo = tmp.resolve("viejo.json");
        Profile.saveAll(viejo, List.of(perfil("Recaudos - Consulta", "vieja"), perfil("Recaudos - Pago", "vieja")));
        Path actual = tmp.resolve("profiles.json");
        Profile.saveAll(actual, List.of(perfil("Recaudos - Consulta", "editada"), perfil("Retiro OTP", "nueva")));

        int agregados = PerfilesInicial.combinar(viejo, actual);

        List<Profile> resultado = Profile.loadAll(actual);
        assertEquals(1, agregados);
        assertEquals(List.of("Recaudos - Consulta", "Retiro OTP", "Recaudos - Pago"),
                resultado.stream().map(p -> p.nombre).toList());
        assertEquals("editada", resultado.get(0).llaveAes);
    }

    @Test
    void combinarConDestinoDanadoDejaLosPerfilesDelOrigen(@TempDir Path tmp) throws Exception {
        Path viejo = tmp.resolve("viejo.json");
        Profile.saveAll(viejo, List.of(perfil("Recaudos - Consulta", "vieja")));
        Path actual = tmp.resolve("profiles.json");
        Files.writeString(actual, "{ esto no es json");

        PerfilesInicial.combinar(viejo, actual);

        assertEquals(1, Profile.loadAll(actual).size());
    }
}
