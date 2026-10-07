package com.example.httpclientreval.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchivoAtomicoTest {

    private static List<String> contenidos(List<Path> rutas) throws Exception {
        List<String> textos = new java.util.ArrayList<>();
        for (Path ruta : rutas) {
            textos.add(Files.readString(ruta));
        }
        return textos;
    }

    @Test
    void laPrimeraEscrituraCreaElArchivoYNoHayNadaQueRespaldar(@TempDir Path tmp) throws Exception {
        Path archivo = tmp.resolve("datos.json");

        ArchivoAtomico.escribir(archivo, "v1");

        assertEquals("v1", Files.readString(archivo));
        assertTrue(ArchivoAtomico.respaldos(archivo).isEmpty());
    }

    @Test
    void alSobreescribirLaVersionAnteriorQuedaComoRespaldo(@TempDir Path tmp) throws Exception {
        Path archivo = tmp.resolve("datos.json");
        ArchivoAtomico.escribir(archivo, "v1");

        ArchivoAtomico.escribir(archivo, "v2");

        assertEquals("v2", Files.readString(archivo));
        assertEquals(List.of("v1"), contenidos(ArchivoAtomico.respaldos(archivo)));
        // Van en una subcarpeta, para no llenar de archivos la carpeta de datos.
        assertEquals(tmp.resolve("respaldos"), ArchivoAtomico.respaldos(archivo).get(0).getParent());
    }

    @Test
    void guardarElMismoContenidoNoDesplazaRespaldosUtiles(@TempDir Path tmp) throws Exception {
        Path archivo = tmp.resolve("datos.json");
        ArchivoAtomico.escribir(archivo, "v1");
        ArchivoAtomico.escribir(archivo, "v2");

        ArchivoAtomico.escribir(archivo, "v2");
        ArchivoAtomico.escribir(archivo, "v2");

        assertEquals(List.of("v1"), contenidos(ArchivoAtomico.respaldos(archivo)));
    }

    @Test
    void rotaConservandoSoloLasUltimasCopiasDelMasRecienteAlMasAntiguo(@TempDir Path tmp) throws Exception {
        Path archivo = tmp.resolve("datos.json");

        for (int version = 1; version <= 8; version++) {
            ArchivoAtomico.escribir(archivo, "v" + version, 3);
        }

        assertEquals("v8", Files.readString(archivo));
        // Las 3 versiones previas, la más reciente primero; v1..v4 ya se descartaron.
        assertEquals(List.of("v7", "v6", "v5"), contenidos(ArchivoAtomico.respaldos(archivo)));
    }

    @Test
    void conLaConfiguracionPorDefectoSeConservanCincoCopias(@TempDir Path tmp) throws Exception {
        Path archivo = tmp.resolve("datos.json");

        for (int version = 1; version <= 10; version++) {
            ArchivoAtomico.escribir(archivo, "v" + version);
        }

        assertEquals(ArchivoAtomico.COPIAS_POR_DEFECTO, ArchivoAtomico.respaldos(archivo).size());
        assertEquals(List.of("v9", "v8", "v7", "v6", "v5"), contenidos(ArchivoAtomico.respaldos(archivo)));
    }

    @Test
    void conCeroCopiasNoSeRespalda(@TempDir Path tmp) throws Exception {
        Path archivo = tmp.resolve("datos.json");
        ArchivoAtomico.escribir(archivo, "v1", 0);

        ArchivoAtomico.escribir(archivo, "v2", 0);

        assertEquals("v2", Files.readString(archivo));
        assertTrue(ArchivoAtomico.respaldos(archivo).isEmpty());
    }

    @Test
    void cadaArchivoTieneSusPropiosRespaldosAunqueCompartanCarpeta(@TempDir Path tmp) throws Exception {
        Path perfiles = tmp.resolve("profiles.json");
        Path entornos = tmp.resolve("entornos.json");
        ArchivoAtomico.escribir(perfiles, "p1");
        ArchivoAtomico.escribir(perfiles, "p2");
        ArchivoAtomico.escribir(entornos, "e1");
        ArchivoAtomico.escribir(entornos, "e2");

        assertEquals(List.of("p1"), contenidos(ArchivoAtomico.respaldos(perfiles)));
        assertEquals(List.of("e1"), contenidos(ArchivoAtomico.respaldos(entornos)));
    }

    @Test
    void siElRespaldoFallaElGuardadoIgualSeHace(@TempDir Path tmp) throws Exception {
        Path archivo = tmp.resolve("datos.json");
        ArchivoAtomico.escribir(archivo, "v1");
        // "respaldos" es un ARCHIVO, no una carpeta: no se puede crear ahí ningún respaldo.
        Files.writeString(tmp.resolve("respaldos"), "estorbo");

        ArchivoAtomico.escribir(archivo, "v2");

        assertEquals("v2", Files.readString(archivo));
    }

    @Test
    void noQuedanTemporalesSobrantes(@TempDir Path tmp) throws Exception {
        Path archivo = tmp.resolve("datos.json");
        ArchivoAtomico.escribir(archivo, "v1");
        ArchivoAtomico.escribir(archivo, "v2");

        try (var archivos = Files.list(tmp)) {
            assertTrue(archivos.noneMatch(p -> p.getFileName().toString().endsWith(".tmp")));
        }
    }
}
