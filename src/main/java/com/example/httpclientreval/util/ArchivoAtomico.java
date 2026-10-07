package com.example.httpclientreval.util;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Escritura de archivos de texto "todo o nada" con copias de seguridad rotativas. Lo usan profiles.json y
 * entornos.json, que guardan llaves, credenciales y la configuración de cada transacción: perderlos o
 * dejarlos a medias obliga a reconstruirlos a mano.
 *
 * <ul>
 *   <li><b>Atómica:</b> escribe primero a un temporal en la misma carpeta y luego renombra. Si el proceso se
 *       interrumpe a mitad de camino (falla de disco, cierre forzado), el archivo se queda con su contenido
 *       anterior completo, nunca a medio escribir.</li>
 *   <li><b>Con respaldo:</b> antes de reemplazar un archivo, guarda su versión anterior en la subcarpeta
 *       {@value #CARPETA_RESPALDOS} como {@code <nombre>.bak1} (la más reciente), {@code .bak2}... y descarta la
 *       más antigua al pasar de {@value #COPIAS_POR_DEFECTO} copias. Así un error del usuario (borrar un perfil
 *       por accidente) o un archivo que se corrompió por otra causa se puede deshacer.</li>
 * </ul>
 *
 * El respaldo es de mejor esfuerzo: si no se puede hacer (disco lleno, permisos) se anota en el log y el
 * guardado sigue adelante, porque perder el cambio del usuario es peor que perder la copia vieja. Los respaldos
 * tienen los mismos secretos en texto plano que el archivo original, y la misma protección (la carpeta del
 * usuario).
 */
public final class ArchivoAtomico {

    public static final int COPIAS_POR_DEFECTO = 5;
    public static final String CARPETA_RESPALDOS = "respaldos";

    /** Tope para el parámetro de copias y para buscar respaldos existentes. */
    private static final int MAXIMO_COPIAS = 20;

    private ArchivoAtomico() {
    }

    public static void escribir(Path archivo, String contenido) throws IOException {
        escribir(archivo, contenido, COPIAS_POR_DEFECTO);
    }

    /**
     * @param copias cuántos respaldos conservar (0 desactiva el respaldo); se limita a {@value #MAXIMO_COPIAS}
     */
    public static void escribir(Path archivo, String contenido, int copias) throws IOException {
        Path directorio = archivo.toAbsolutePath().getParent();
        // La carpeta de usuario puede no existir todavía (primer arranque).
        Files.createDirectories(directorio);
        Path temporal = Files.createTempFile(directorio, archivo.getFileName().toString(), ".tmp");
        try {
            Files.writeString(temporal, contenido);
            // Después de escribir bien el temporal y antes de reemplazar: si lo anterior falla, no se rotó nada.
            respaldarAnterior(archivo, contenido, Math.min(copias, MAXIMO_COPIAS));
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

    /**
     * Los respaldos que existen de {@code archivo}, del más reciente al más antiguo. Quien se recupera de un
     * archivo dañado los prueba en este orden y se queda con el primero que pueda leer.
     */
    public static List<Path> respaldos(Path archivo) {
        List<Path> existentes = new ArrayList<>();
        for (int i = 1; i <= MAXIMO_COPIAS; i++) {
            Path respaldo = rutaRespaldo(archivo, i);
            if (Files.isRegularFile(respaldo)) {
                existentes.add(respaldo);
            }
        }
        return existentes;
    }

    private static Path rutaRespaldo(Path archivo, int numero) {
        Path directorio = archivo.toAbsolutePath().getParent();
        return directorio.resolve(CARPETA_RESPALDOS).resolve(archivo.getFileName() + ".bak" + numero);
    }

    /**
     * Guarda la versión actual de {@code archivo} como respaldo 1, empujando los anteriores un lugar (y
     * descartando el último). No hace nada si no hay versión previa, si está vacía o si es idéntica al
     * contenido nuevo (guardar sin cambios no debe desplazar copias útiles por otras iguales).
     */
    private static void respaldarAnterior(Path archivo, String nuevoContenido, int copias) {
        if (copias <= 0 || !Files.isRegularFile(archivo)) {
            return;
        }
        try {
            String actual = Files.readString(archivo);
            if (actual.isBlank() || actual.equals(nuevoContenido)) {
                return;
            }
            Files.createDirectories(rutaRespaldo(archivo, 1).getParent());
            Files.deleteIfExists(rutaRespaldo(archivo, copias));
            for (int i = copias - 1; i >= 1; i--) {
                Path origen = rutaRespaldo(archivo, i);
                if (Files.exists(origen)) {
                    Files.move(origen, rutaRespaldo(archivo, i + 1), StandardCopyOption.REPLACE_EXISTING);
                }
            }
            Files.writeString(rutaRespaldo(archivo, 1), actual);
        } catch (IOException | RuntimeException ex) {
            Registro.advertencia("No se pudo respaldar " + archivo + " antes de guardar; se guarda igual", ex);
        }
    }
}
