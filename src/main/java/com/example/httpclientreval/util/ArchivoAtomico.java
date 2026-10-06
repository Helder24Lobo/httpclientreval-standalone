package com.example.httpclientreval.util;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Escritura de archivos de texto "todo o nada": primero a un temporal en la misma carpeta y luego
 * un renombrado. Si el proceso se interrumpe a mitad de camino (falla de disco, cierre forzado),
 * el archivo se queda con su contenido anterior completo, nunca a medio escribir. Lo usan
 * profiles.json y entornos.json, que guardan llaves y credenciales.
 */
public final class ArchivoAtomico {

    private ArchivoAtomico() {
    }

    public static void escribir(Path archivo, String contenido) throws IOException {
        Path directorio = archivo.toAbsolutePath().getParent();
        // La carpeta de usuario puede no existir todavía (primer arranque).
        Files.createDirectories(directorio);
        Path temporal = Files.createTempFile(directorio, archivo.getFileName().toString(), ".tmp");
        try {
            Files.writeString(temporal, contenido);
            try {
                // ATOMIC_MOVE: en el mismo volumen, el sistema operativo hace el reemplazo como una
                // sola operación (rename), así que nunca queda el archivo a medias.
                Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException noAtomico) {
                // Algunos sistemas de archivos no soportan el movimiento atómico (ej. red, FAT32);
                // se hace el reemplazo igual, aunque ya sin esa garantía.
                Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            // No-op si el move ya tuvo éxito (el temporal ya no existe con ese nombre); limpia el
            // archivo temporal si algo falló antes de llegar al move.
            Files.deleteIfExists(temporal);
        }
    }
}
