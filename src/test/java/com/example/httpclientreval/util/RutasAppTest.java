package com.example.httpclientreval.util;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RutasAppTest {

    private static Path dir(String so, Map<String, String> env, String home, String sobrescrito) {
        return RutasApp.directorioDatos(so, env::get, home, sobrescrito);
    }

    @Test
    void windowsUsaElPerfilDelUsuarioYNoAppData() {
        assertEquals(Path.of("C:\\Users\\ana", ".httpclientreval"),
                dir("Windows 11", Map.of("APPDATA", "C:\\Users\\ana\\AppData\\Roaming"), "C:\\Users\\ana", null));
    }

    @Test
    void laUbicacionClasicaDeWindowsSigueSiendoAppData() {
        String appData = "C:\\Users\\ana\\AppData\\Roaming";
        assertEquals(Path.of(appData, "httpclientreval"),
                RutasApp.directorioClasico("Windows 11", Map.of("APPDATA", appData)::get, "C:\\Users\\ana"));
        assertEquals(Path.of("C:\\Users\\ana", "AppData", "Roaming", "httpclientreval"),
                RutasApp.directorioClasico("Windows 11", Map.<String, String>of()::get, "C:\\Users\\ana"));
    }

    @Test
    void macUsaApplicationSupport() {
        assertEquals(Path.of("/Users/ana", "Library", "Application Support", "httpclientreval"),
                dir("Mac OS X", Map.of(), "/Users/ana", null));
    }

    @Test
    void linuxRespetaXdgYSinEllaUsaDotConfig() {
        assertEquals(Path.of("/x/cfg", "httpclientreval"),
                dir("Linux", Map.of("XDG_CONFIG_HOME", "/x/cfg"), "/home/ana", null));
        assertEquals(Path.of("/home/ana", ".config", "httpclientreval"),
                dir("Linux", Map.of(), "/home/ana", null));
    }

    @Test
    void laPropiedadDelSistemaTienePrioridadSobreTodo() {
        assertEquals(Path.of("/portatil/datos"),
                dir("Windows 11", Map.of("APPDATA", "C:\\x"), "C:\\Users\\ana", "/portatil/datos"));
    }
}
