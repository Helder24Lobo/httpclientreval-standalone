package com.example.httpclientreval.model;

import com.example.httpclientreval.util.ArchivoAtomico;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Perfil reutilizable: llave AES + valores por defecto del header del sobre,
 * y opcionalmente los valores por defecto del _BodyMensaje/_headerMensaje
 * propios de una transacción (bodyMensajeDefault/headerMensajeDefault). Un
 * mismo entorno con 11 transacciones distintas se modela como 11 perfiles
 * (misma llaveAes, distinto bodyMensajeDefault cada uno).
 *
 * Se cargan desde profiles.json, en la carpeta de datos del usuario (ver RutasApp);
 * así ningún secreto real queda junto al código ni se commitea por error. Ver
 * profiles.example.json como plantilla. Si un perfil no trae bodyMensajeDefault/headerMensajeDefault,
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

    /**
     * Lee la lista de perfiles de {@code archivo}. Lanza IllegalStateException si no existe o no
     * trae ningún perfil, y JsonSyntaxException si el JSON está dañado (el arranque de la app
     * captura todo eso y ofrece crear/restaurar el archivo, ver ArranqueDePerfiles).
     */
    public static List<Profile> loadAll(Path archivo) throws IOException {
        if (!Files.exists(archivo)) {
            throw new IllegalStateException("No se encontró " + archivo.toAbsolutePath() + ".");
        }
        return fromJson(Files.readString(archivo), archivo.toAbsolutePath().toString());
    }

    /** Parsea una lista de perfiles desde texto JSON; {@code origen} solo sirve para los mensajes de error. */
    public static List<Profile> fromJson(String json, String origen) {
        Type tipoLista = new TypeToken<List<Profile>>() {
        }.getType();
        List<Profile> perfiles = new Gson().fromJson(json, tipoLista);

        if (perfiles == null || perfiles.isEmpty()) {
            throw new IllegalStateException(origen + " no tiene perfiles definidos.");
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
        ArchivoAtomico.escribir(archivo, gson.toJson(perfiles));
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
