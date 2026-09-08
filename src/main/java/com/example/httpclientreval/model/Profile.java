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
 * Perfil reutilizable (llave AES + valores por defecto del header del sobre),
 * para no tener que quemar esos datos en el código cada vez que se cifra o
 * descifra algo. Se cargan desde profiles.json (ver profiles.example.json
 * como plantilla); profiles.json está en .gitignore para que ningún secreto
 * real quede commiteado.
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
}
