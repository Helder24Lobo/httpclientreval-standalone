package com.example.httpclientreval.model;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Operaciones sobre el archivo de perfiles cuando falta o está dañado al arrancar (sin interfaz:
 * los diálogos viven en ArranqueDePerfiles): crearlo con perfiles de ejemplo, copiarlo desde otro
 * archivo y apartar uno dañado sin borrarlo.
 */
public final class PerfilesInicial {

    /** profiles.example.json, copiado a los recursos por build.gradle (processResources). */
    static final String RECURSO_EJEMPLO = "/profiles.example.json";

    private static final DateTimeFormatter MARCA = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private PerfilesInicial() {
    }

    /** Crea {@code destino} con los perfiles de ejemplo (llave y credenciales con marcadores a reemplazar). */
    public static void crearDesdeEjemplo(Path destino) throws IOException {
        Profile.saveAll(destino, perfilesDeEjemplo());
    }

    /** Valida {@code origen} (que sea un archivo de perfiles legible) y lo guarda como {@code destino}. */
    public static void copiar(Path origen, Path destino) throws IOException {
        Profile.saveAll(destino, Profile.loadAll(origen));
    }

    /** @return {@code true} si {@code archivo} existe y es un archivo de perfiles legible. */
    public static boolean esLegible(Path archivo) {
        try {
            Profile.loadAll(archivo);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Suma a {@code destino} los perfiles de {@code origen} que aún no tiene (se comparan por nombre) y
     * lo guarda. Lo que ya hay en {@code destino} manda: si un nombre está en ambos se conserva el de
     * {@code destino}, así las transacciones creadas o editadas después nunca se pisan con las viejas.
     * Si {@code destino} no existe o no es legible, queda igual que {@code origen}.
     *
     * @return cuántos perfiles de {@code origen} se agregaron.
     */
    public static int combinar(Path origen, Path destino) throws IOException {
        List<Profile> resultado = new ArrayList<>(esLegible(destino) ? Profile.loadAll(destino) : List.of());
        Set<String> nombres = new HashSet<>();
        for (Profile p : resultado) {
            if (p != null) {
                nombres.add(clave(p));
            }
        }
        int agregados = 0;
        for (Profile p : Profile.loadAll(origen)) {
            if (p != null && nombres.add(clave(p))) {
                resultado.add(p);
                agregados++;
            }
        }
        Profile.saveAll(destino, resultado);
        return agregados;
    }

    private static String clave(Profile p) {
        return p == null || p.nombre == null ? "" : p.nombre.trim();
    }

    /**
     * Renombra un archivo de perfiles dañado a {@code <nombre>.corrupto-<fecha>} en la misma carpeta,
     * para poder crear uno nuevo sin destruir lo que hubiera dentro (quizá recuperable a mano).
     */
    public static Path apartarCorrupto(Path archivo) throws IOException {
        Path destino = archivo.resolveSibling(
                archivo.getFileName() + ".corrupto-" + MARCA.format(LocalDateTime.now()));
        return Files.move(archivo, destino);
    }

    public static List<Profile> perfilesDeEjemplo() {
        try (InputStream in = PerfilesInicial.class.getResourceAsStream(RECURSO_EJEMPLO)) {
            if (in != null) {
                return Profile.fromJson(new String(in.readAllBytes(), StandardCharsets.UTF_8), RECURSO_EJEMPLO);
            }
        } catch (IOException | RuntimeException ignorada) {
            // Cae al perfil mínimo de abajo: crear el archivo no debe fallar por no encontrar la plantilla.
        }
        Profile minimo = new Profile();
        minimo.nombre = "Ejemplo - Transaccion";
        minimo.llaveAes = "REEMPLAZA_ESTA_LLAVE_POR_32BYTE";
        minimo.idClienteDefault = 7;
        minimo.idTransaccionDefault = 14;
        minimo.ipClienteDefault = "172.17.0.4";
        minimo.wsseUsername = "REEMPLAZA_CON_USUARIO_WS";
        minimo.wssePassword = "REEMPLAZA_CON_PASSWORD_WS";
        minimo.bodyMensajeDefault = new MensajeNegocio.BodyMensaje();
        minimo.headerMensajeDefault = new MensajeNegocio.HeaderMensaje();
        return List.of(minimo);
    }
}
