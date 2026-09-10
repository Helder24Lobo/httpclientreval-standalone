package com.example.httpclientreval.model;

import com.google.gson.Gson;
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

    @Override
    public String toString() {
        return nombre;
    }
}
