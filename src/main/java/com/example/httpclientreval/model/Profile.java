package com.example.httpclientreval.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Perfil reutilizable: llave AES + valores por defecto del header del sobre,
 * y opcionalmente los valores por defecto del _BodyMensaje/_headerMensaje
 * propios de una transacción (bodyMensajeDefault/headerMensajeDefault). Un
 * mismo entorno con 11 transacciones distintas se modela como 11 perfiles
 * (misma llaveAes, distinto bodyMensajeDefault cada uno).
 *
 * Se cargan desde profiles.json (ver profiles.example.json como plantilla);
 * profiles.json está en .gitignore para que ningún secreto real quede
 * commiteado. Si un perfil no trae bodyMensajeDefault/headerMensajeDefault,
 * se usan los defaults genéricos de MensajeNegocio.
 */
public class Profile {

    @SerializedName("nombre")
    public String nombre;

    @SerializedName("llaveAes")
    public String llaveAes;

    @SerializedName("idClienteDefault")
    public int idClienteDefault;

    @SerializedName("idTransaccionDefault")
    public int idTransaccionDefault;

    @SerializedName("ipClienteDefault")
    public String ipClienteDefault;

    @SerializedName("wsseUsername")
    public String wsseUsername;

    @SerializedName("wssePassword")
    public String wssePassword;

    @SerializedName("bodyMensajeDefault")
    public MensajeNegocio.BodyMensaje bodyMensajeDefault;

    @SerializedName("headerMensajeDefault")
    public MensajeNegocio.HeaderMensaje headerMensajeDefault;

    public static List<Profile> loadAll(Path archivo) throws IOException {
        if (!Files.exists(archivo)) {
            throw new IllegalStateException(
                    "No se encontró " + archivo.toAbsolutePath() + ".\n"
                            + "Copia profiles.example.json a profiles.json (en la raíz del proyecto) "
                            + "y completa ahí tu(s) llave(s) AES real(es). profiles.json está en "
                            + ".gitignore, así que nunca se commitea.");
        }

        String contenido = Files.readString(archivo);
        Type tipoLista = new TypeToken<List<Profile>>() {
        }.getType();
        List<Profile> perfiles = new Gson().fromJson(contenido, tipoLista);

        if (perfiles == null || perfiles.isEmpty()) {
            throw new IllegalStateException(archivo.toAbsolutePath() + " no tiene perfiles definidos.");
        }
        return perfiles;
    }

    /**
     * Sobreescribe profiles.json con la lista completa (usado al registrar, renombrar, eliminar o
     * duplicar un perfil desde la UI). Escribe primero en un archivo temporal en el mismo directorio
     * y lo renombra al final: si el proceso se interrumpe a mitad de camino (falla de disco, cierre
     * forzado), profiles.json se queda con el contenido anterior completo, nunca a medio escribir.
     */
    public static void saveAll(Path archivo, List<Profile> perfiles) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(perfiles);

        Path directorio = archivo.toAbsolutePath().getParent();
        Path temporal = Files.createTempFile(directorio, archivo.getFileName().toString(), ".tmp");
        try {
            Files.writeString(temporal, json);
            try {
                // ATOMIC_MOVE: en el mismo volumen, el sistema operativo hace el reemplazo como una
                // sola operación (rename), así que nunca queda un profiles.json a medias.
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

    public static final String GRUPO_SIN_NOMBRE = "Otros";
    private static final String SEPARADOR_GRUPO = " - ";

    /** Grupo de la transacción: lo que va antes del primer " - " del nombre (ej. "Recaudos"), o "Otros" si no hay. */
    public String grupo() {
        String n = nombre == null ? "" : nombre.trim();
        int i = n.indexOf(SEPARADOR_GRUPO);
        return i > 0 ? n.substring(0, i).trim() : GRUPO_SIN_NOMBRE;
    }

    /** Nombre de la transacción dentro de su grupo: lo que va después del primer " - ", o el nombre completo si no hay. */
    public String detalle() {
        String n = nombre == null ? "" : nombre.trim();
        int i = n.indexOf(SEPARADOR_GRUPO);
        return i > 0 ? n.substring(i + SEPARADOR_GRUPO.length()).trim() : n;
    }

    /**
     * Copia independiente de este perfil (incluidos bodyMensajeDefault/headerMensajeDefault: no comparten
     * referencia con el original, así que editar uno no afecta al otro). Va por Gson en vez de copiar
     * campo a campo para no tener que tocar este método si el perfil gana un campo nuevo.
     */
    public Profile copia() {
        Gson gson = new Gson();
        return gson.fromJson(gson.toJson(this), Profile.class);
    }

    @Override
    public String toString() {
        return nombre;
    }
}
